-- 039: boton de SOS (ticket ClickUp 86e3uf097).
-- Una alerta SOS es independiente de "compartir en vivo": su id (uuid) es el enlace que se manda
-- a los contactos de emergencia (https://gacelas.vercel.app/sos/<id>). Quien tenga el enlace ve
-- la ultima posicion mientras la alerta este activa; al terminar (o tras 2 h sin posiciones) el
-- enlace deja de mostrar la ubicacion. Durante un SOS la zona de casa NO se oculta: es una
-- emergencia y la elige el usuario manteniendo pulsado el boton 3 segundos.

CREATE SCHEMA IF NOT EXISTS seguridad;

CREATE TABLE IF NOT EXISTS seguridad.sos_alertas (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  ended_at TIMESTAMPTZ,
  last_lat DOUBLE PRECISION,
  last_lng DOUBLE PRECISION,
  last_at TIMESTAMPTZ,
  points JSONB NOT NULL DEFAULT '[]'::jsonb
);
CREATE INDEX IF NOT EXISTS sos_alertas_usuario_idx ON seguridad.sos_alertas (user_id, started_at DESC);
ALTER TABLE seguridad.sos_alertas ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Ver mis alertas SOS" ON seguridad.sos_alertas;
CREATE POLICY "Ver mis alertas SOS" ON seguridad.sos_alertas FOR SELECT USING (auth.uid() = user_id);
-- Sin politicas de escritura: solo las funciones de abajo escriben.

CREATE OR REPLACE FUNCTION public.sos_iniciar(lat DOUBLE PRECISION DEFAULT NULL, lng DOUBLE PRECISION DEFAULT NULL)
RETURNS UUID LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, seguridad AS $$
DECLARE
  uid UUID := auth.uid();
  nueva UUID;
BEGIN
  IF uid IS NULL THEN RAISE EXCEPTION 'no autenticado'; END IF;
  UPDATE seguridad.sos_alertas SET ended_at = now() WHERE user_id = uid AND ended_at IS NULL;
  INSERT INTO seguridad.sos_alertas (user_id, last_lat, last_lng, last_at, points)
  VALUES (uid, lat, lng, CASE WHEN lat IS NOT NULL THEN now() END,
          CASE WHEN lat IS NOT NULL THEN jsonb_build_array(jsonb_build_object('lat', lat, 'lng', lng)) ELSE '[]'::jsonb END)
  RETURNING id INTO nueva;
  RETURN nueva;
END $$;

CREATE OR REPLACE FUNCTION public.sos_enviar(alerta UUID, lat DOUBLE PRECISION, lng DOUBLE PRECISION)
RETURNS BOOLEAN LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, seguridad AS $$
BEGIN
  IF lat NOT BETWEEN -90 AND 90 OR lng NOT BETWEEN -180 AND 180 THEN RAISE EXCEPTION 'coordenadas invalidas'; END IF;
  UPDATE seguridad.sos_alertas SET last_lat = lat, last_lng = lng, last_at = now(),
    points = CASE WHEN jsonb_array_length(points) >= 720 THEN points - 0 ELSE points END
             || jsonb_build_array(jsonb_build_object('lat', lat, 'lng', lng))
  WHERE id = alerta AND user_id = auth.uid() AND ended_at IS NULL;
  RETURN FOUND;
END $$;

CREATE OR REPLACE FUNCTION public.sos_terminar(alerta UUID)
RETURNS VOID LANGUAGE sql SECURITY DEFINER SET search_path = public, seguridad AS $$
  UPDATE seguridad.sos_alertas SET ended_at = now() WHERE id = alerta AND user_id = auth.uid() AND ended_at IS NULL;
$$;

-- Vista publica de una alerta por su enlace. Sin sesion. Solo devuelve la posicion si sigue activa.
CREATE OR REPLACE FUNCTION public.sos_ver(alerta UUID)
RETURNS TABLE (nombre TEXT, activa BOOLEAN, started_at TIMESTAMPTZ, last_at TIMESTAMPTZ,
               lat DOUBLE PRECISION, lng DOUBLE PRECISION, points JSONB)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, seguridad AS $$
  SELECT COALESCE(NULLIF(p.full_name, ''), NULLIF(p.username, ''), 'Corredor de Runcity'),
         a.ended_at IS NULL AND COALESCE(a.last_at, a.started_at) > now() - interval '2 hours',
         a.started_at, a.last_at,
         CASE WHEN a.ended_at IS NULL THEN a.last_lat END,
         CASE WHEN a.ended_at IS NULL THEN a.last_lng END,
         CASE WHEN a.ended_at IS NULL THEN a.points ELSE '[]'::jsonb END
  FROM seguridad.sos_alertas a
  LEFT JOIN identidad.profiles p ON p.id = a.user_id
  WHERE a.id = alerta
$$;

REVOKE ALL ON FUNCTION public.sos_iniciar(DOUBLE PRECISION, DOUBLE PRECISION) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.sos_enviar(UUID, DOUBLE PRECISION, DOUBLE PRECISION) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.sos_terminar(UUID) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.sos_iniciar(DOUBLE PRECISION, DOUBLE PRECISION) TO authenticated;
GRANT EXECUTE ON FUNCTION public.sos_enviar(UUID, DOUBLE PRECISION, DOUBLE PRECISION) TO authenticated;
GRANT EXECUTE ON FUNCTION public.sos_terminar(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.sos_ver(UUID) TO anon, authenticated;

-- Alertas olvidadas (app cerrada sin terminar): se cierran tras 2 h sin posiciones.
CREATE OR REPLACE FUNCTION public.sos_cerrar_inactivas() RETURNS INTEGER
LANGUAGE sql SECURITY DEFINER SET search_path = public, seguridad AS $$
  WITH c AS (UPDATE seguridad.sos_alertas SET ended_at = now()
    WHERE ended_at IS NULL AND COALESCE(last_at, started_at) < now() - interval '2 hours' RETURNING 1)
  SELECT count(*)::int FROM c;
$$;
REVOKE ALL ON FUNCTION public.sos_cerrar_inactivas() FROM PUBLIC, anon, authenticated;
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
    PERFORM cron.unschedule('gacelas-sos-inactivas') WHERE EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'gacelas-sos-inactivas');
    PERFORM cron.schedule('gacelas-sos-inactivas', '*/15 * * * *', 'SELECT public.sos_cerrar_inactivas()');
  END IF;
END $$;

INSERT INTO kernel.propiedad_tablas (tabla, contexto, esquema_actual)
SELECT 'sos_alertas', 'seguridad', 'seguridad'
WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'kernel' AND table_name = 'propiedad_tablas')
ON CONFLICT DO NOTHING;
