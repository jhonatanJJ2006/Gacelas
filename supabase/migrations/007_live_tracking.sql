-- Fase 3.1: base de datos para seguimiento en vivo.
-- live_sharing_settings: preferencias del dueno (solo el las ve y cambia).
-- live_sessions: una fila por carrera en vivo; la ven el dueno y quien pueda ver al autor
-- segun su audiencia. La posicion viaja por Realtime Broadcast (canal live:session:{id});
-- aqui solo se guarda la ultima posicion publica (ya difuminada cerca de casa por el cliente).

CREATE TABLE IF NOT EXISTS live_sharing_settings (
  user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  enabled BOOLEAN NOT NULL DEFAULT false,
  audience TEXT NOT NULL DEFAULT 'followers' CHECK (audience IN ('followers', 'public')),
  notify_followers BOOLEAN NOT NULL DEFAULT true,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE live_sharing_settings ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Own live settings" ON live_sharing_settings;
CREATE POLICY "Own live settings" ON live_sharing_settings FOR ALL
  USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE TABLE IF NOT EXISTS live_sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  audience TEXT NOT NULL DEFAULT 'followers' CHECK (audience IN ('followers', 'public')),
  started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  ended_at TIMESTAMPTZ,
  last_lat DOUBLE PRECISION,
  last_lng DOUBLE PRECISION,
  last_at TIMESTAMPTZ,
  distance_m DOUBLE PRECISION NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS live_sessions_activas_idx ON live_sessions(user_id) WHERE ended_at IS NULL;
ALTER TABLE live_sessions ENABLE ROW LEVEL SECURITY;

CREATE OR REPLACE FUNCTION public.puede_ver_en_vivo(dueno UUID, audiencia TEXT) RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT dueno = auth.uid()
      OR audiencia = 'public'
      OR EXISTS (SELECT 1 FROM follows WHERE follower_id = auth.uid() AND following_id = dueno);
$$;

DROP POLICY IF EXISTS "Ver sesiones en vivo" ON live_sessions;
CREATE POLICY "Ver sesiones en vivo" ON live_sessions FOR SELECT
  USING (auth.uid() IS NOT NULL AND puede_ver_en_vivo(user_id, audience));
DROP POLICY IF EXISTS "Crear mi sesion" ON live_sessions;
CREATE POLICY "Crear mi sesion" ON live_sessions FOR INSERT WITH CHECK (auth.uid() = user_id);
DROP POLICY IF EXISTS "Actualizar mi sesion" ON live_sessions;
CREATE POLICY "Actualizar mi sesion" ON live_sessions FOR UPDATE
  USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
DROP POLICY IF EXISTS "Borrar mi sesion" ON live_sessions;
CREATE POLICY "Borrar mi sesion" ON live_sessions FOR DELETE USING (auth.uid() = user_id);

-- Al iniciar una sesion se avisa a los seguidores que lo permiten (tipo 'live').
CREATE OR REPLACE FUNCTION public.trg_aviso_en_vivo() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF COALESCE((SELECT notify_followers FROM live_sharing_settings WHERE user_id = NEW.user_id), true) THEN
    INSERT INTO notifications (user_id, type, actor_name, message)
    SELECT f.follower_id, 'live',
           COALESCE((SELECT full_name FROM profiles WHERE id = NEW.user_id), 'Un corredor'),
           'está corriendo ahora. Míralo en vivo'
    FROM follows f WHERE f.following_id = NEW.user_id;
  END IF;
  RETURN NULL;
END;
$$;
DROP TRIGGER IF EXISTS aviso_en_vivo ON live_sessions;
CREATE TRIGGER aviso_en_vivo AFTER INSERT ON live_sessions
  FOR EACH ROW EXECUTE FUNCTION trg_aviso_en_vivo();
