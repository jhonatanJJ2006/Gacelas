-- Fase 3: seguimiento en vivo (continuacion de 007).
-- Los espectadores se suscriben con Supabase Realtime (postgres_changes) a su fila de
-- live_sessions; Realtime aplica el RLS de 007, asi que solo reciben cambios quienes
-- pueden ver la sesion. Las posiciones solo se escriben con live_enviar(), que guarda el
-- punto de salida en una tabla privada y oculta la posicion mientras el corredor este
-- dentro del radio de casa (si tiene activado ocultar casa).

ALTER TABLE live_sessions
  ADD COLUMN IF NOT EXISTS points JSONB NOT NULL DEFAULT '[]'::jsonb,
  ADD COLUMN IF NOT EXISTS hidden BOOLEAN NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS elapsed_s INTEGER NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS live_private (
  session_id UUID PRIMARY KEY REFERENCES live_sessions(id) ON DELETE CASCADE,
  start_lat DOUBLE PRECISION NOT NULL,
  start_lng DOUBLE PRECISION NOT NULL
);
ALTER TABLE live_private ENABLE ROW LEVEL SECURITY; -- sin politicas: nadie la lee por la API

-- Las escrituras directas ya no se permiten: todo pasa por las funciones.
DROP POLICY IF EXISTS "Crear mi sesion" ON live_sessions;
DROP POLICY IF EXISTS "Actualizar mi sesion" ON live_sessions;

CREATE OR REPLACE FUNCTION public.live_iniciar(audiencia TEXT DEFAULT NULL)
RETURNS UUID LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  uid UUID := auth.uid();
  aud TEXT;
  nueva UUID;
BEGIN
  IF uid IS NULL THEN RAISE EXCEPTION 'no autenticado'; END IF;
  aud := COALESCE(audiencia, (SELECT audience FROM live_sharing_settings WHERE user_id = uid),
    CASE WHEN (SELECT visibility FROM user_settings WHERE user_id = uid) = 'public' THEN 'public' ELSE 'followers' END);
  IF aud NOT IN ('followers', 'public') THEN RAISE EXCEPTION 'audiencia invalida'; END IF;
  UPDATE live_sessions SET ended_at = now() WHERE user_id = uid AND ended_at IS NULL;
  INSERT INTO live_sessions (user_id, audience) VALUES (uid, aud) RETURNING id INTO nueva;
  RETURN nueva;
END;
$$;

CREATE OR REPLACE FUNCTION public.live_enviar(sesion UUID, lat DOUBLE PRECISION, lng DOUBLE PRECISION,
  distancia_m DOUBLE PRECISION, segundos INTEGER)
RETURNS BOOLEAN LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  uid UUID := auth.uid();
  s user_settings;
  inicio live_private;
  oculto BOOLEAN := false;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM live_sessions WHERE id = sesion AND user_id = uid AND ended_at IS NULL) THEN
    RAISE EXCEPTION 'sesion no valida';
  END IF;
  IF lat NOT BETWEEN -90 AND 90 OR lng NOT BETWEEN -180 AND 180 THEN RAISE EXCEPTION 'coordenadas invalidas'; END IF;
  INSERT INTO live_private (session_id, start_lat, start_lng) VALUES (sesion, lat, lng)
    ON CONFLICT (session_id) DO NOTHING;
  SELECT * INTO inicio FROM live_private WHERE session_id = sesion;
  SELECT * INTO s FROM user_settings WHERE user_id = uid;
  IF s.user_id IS NULL OR s.hide_home THEN
    oculto := ST_Distance(
      ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography,
      ST_SetSRID(ST_MakePoint(inicio.start_lng, inicio.start_lat), 4326)::geography
    ) <= COALESCE(s.home_radius_m, 300);
  END IF;
  IF oculto THEN
    UPDATE live_sessions SET hidden = true, distance_m = distancia_m, elapsed_s = segundos, last_at = now()
      WHERE id = sesion;
  ELSE
    UPDATE live_sessions SET hidden = false, last_lat = lat, last_lng = lng, last_at = now(),
      distance_m = distancia_m, elapsed_s = segundos,
      points = CASE WHEN jsonb_array_length(points) >= 600 THEN points - 0 ELSE points END
               || jsonb_build_array(jsonb_build_object('lat', lat, 'lng', lng))
      WHERE id = sesion;
  END IF;
  RETURN NOT oculto;
END;
$$;

CREATE OR REPLACE FUNCTION public.live_terminar(sesion UUID)
RETURNS VOID LANGUAGE sql SECURITY DEFINER SET search_path = public AS $$
  UPDATE live_sessions SET ended_at = now() WHERE id = sesion AND user_id = auth.uid() AND ended_at IS NULL;
$$;

REVOKE ALL ON FUNCTION public.live_iniciar(TEXT) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.live_enviar(UUID, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, INTEGER) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.live_terminar(UUID) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.live_iniciar(TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.live_enviar(UUID, DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION, INTEGER) TO authenticated;
GRANT EXECUTE ON FUNCTION public.live_terminar(UUID) TO authenticated;

-- Sesiones olvidadas (app cerrada sin terminar): se cierran tras 15 min sin posiciones.
CREATE OR REPLACE FUNCTION public.live_cerrar_inactivas() RETURNS INTEGER
LANGUAGE sql SECURITY DEFINER SET search_path = public AS $$
  WITH c AS (UPDATE live_sessions SET ended_at = now()
    WHERE ended_at IS NULL AND COALESCE(last_at, started_at) < now() - interval '15 minutes' RETURNING 1)
  SELECT count(*)::int FROM c;
$$;
REVOKE ALL ON FUNCTION public.live_cerrar_inactivas() FROM PUBLIC, anon, authenticated;
SELECT cron.unschedule('gacelas-live-inactivas') WHERE EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'gacelas-live-inactivas');
SELECT cron.schedule('gacelas-live-inactivas', '*/5 * * * *', 'SELECT public.live_cerrar_inactivas()');

-- Realtime: emitir cambios de live_sessions (filtrados por RLS para cada suscriptor).
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_publication_tables WHERE pubname = 'supabase_realtime' AND tablename = 'live_sessions') THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE live_sessions;
  END IF;
END $$;
