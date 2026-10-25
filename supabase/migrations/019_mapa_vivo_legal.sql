-- Mapa en vivo de corredores y consentimientos legales (LOPDP Ecuador).
-- runner_positions: ultima posicion de cada corredor que opto por "Ubicacion visible".
-- Solo se escribe con mapa_publicar(), que exige el consentimiento vigente; las posiciones
-- con mas de 2 minutos no se devuelven. Realtime (postgres_changes) aplica el mismo RLS.

-- Versiones publicadas de los documentos legales (la vigente es la de mayor published_at).
CREATE TABLE IF NOT EXISTS legal_versions (
  kind TEXT NOT NULL CHECK (kind IN ('terminos', 'privacidad')),
  version TEXT NOT NULL,
  published_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (kind, version)
);
ALTER TABLE legal_versions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Leer versiones legales" ON legal_versions;
CREATE POLICY "Leer versiones legales" ON legal_versions FOR SELECT USING (true);
INSERT INTO legal_versions (kind, version) VALUES ('terminos', '2026-10-25'), ('privacidad', '2026-10-25')
  ON CONFLICT DO NOTHING;

-- Historial de consentimientos: nunca se borra ni se edita; revocar es una fila con granted=false.
CREATE TABLE IF NOT EXISTS consents (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  kind TEXT NOT NULL CHECK (kind IN ('terminos', 'privacidad', 'gps_carrera', 'ubicacion_visible')),
  version TEXT NOT NULL,
  granted BOOLEAN NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS consents_usuario_idx ON consents(user_id, kind, created_at DESC);
ALTER TABLE consents ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Ver mis consentimientos" ON consents;
CREATE POLICY "Ver mis consentimientos" ON consents FOR SELECT USING (auth.uid() = user_id);
DROP POLICY IF EXISTS "Registrar mi consentimiento" ON consents;
CREATE POLICY "Registrar mi consentimiento" ON consents FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE OR REPLACE FUNCTION public.consentimiento_vigente(uid UUID, tipo TEXT) RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT COALESCE((SELECT granted FROM consents WHERE user_id = uid AND kind = tipo
                   ORDER BY created_at DESC LIMIT 1), false);
$$;

CREATE TABLE IF NOT EXISTS runner_positions (
  user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  lat DOUBLE PRECISION NOT NULL CHECK (lat BETWEEN -90 AND 90),
  lng DOUBLE PRECISION NOT NULL CHECK (lng BETWEEN -180 AND 180),
  running BOOLEAN NOT NULL DEFAULT false,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS runner_positions_recientes_idx ON runner_positions(updated_at DESC);
ALTER TABLE runner_positions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Ver corredores visibles" ON runner_positions;
CREATE POLICY "Ver corredores visibles" ON runner_positions FOR SELECT
  USING (auth.uid() IS NOT NULL AND updated_at > now() - interval '2 minutes'
         AND consentimiento_vigente(user_id, 'ubicacion_visible'));
DROP POLICY IF EXISTS "Borrar mi posicion" ON runner_positions;
CREATE POLICY "Borrar mi posicion" ON runner_positions FOR DELETE USING (auth.uid() = user_id);

-- Publica la posicion propia. Throttling en servidor: como maximo una vez cada 10 s.
CREATE OR REPLACE FUNCTION public.mapa_publicar(lat DOUBLE PRECISION, lng DOUBLE PRECISION, corriendo BOOLEAN)
RETURNS BOOLEAN LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE uid UUID := auth.uid();
BEGIN
  IF uid IS NULL THEN RAISE EXCEPTION 'no autenticado'; END IF;
  IF NOT consentimiento_vigente(uid, 'ubicacion_visible') THEN
    DELETE FROM runner_positions WHERE user_id = uid;
    RETURN false;
  END IF;
  INSERT INTO runner_positions AS r (user_id, lat, lng, running, updated_at)
    VALUES (uid, lat, lng, corriendo, now())
  ON CONFLICT (user_id) DO UPDATE SET lat = EXCLUDED.lat, lng = EXCLUDED.lng,
    running = EXCLUDED.running, updated_at = now()
    WHERE r.updated_at < now() - interval '10 seconds' OR r.running <> EXCLUDED.running;
  RETURN true;
END;
$$;

-- Al revocar el consentimiento de ubicacion visible se borra la posicion al instante.
CREATE OR REPLACE FUNCTION public.trg_revocar_ubicacion() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NEW.kind = 'ubicacion_visible' AND NOT NEW.granted THEN
    DELETE FROM runner_positions WHERE user_id = NEW.user_id;
  END IF;
  RETURN NULL;
END;
$$;
DROP TRIGGER IF EXISTS revocar_ubicacion ON consents;
CREATE TRIGGER revocar_ubicacion AFTER INSERT ON consents FOR EACH ROW EXECUTE FUNCTION trg_revocar_ubicacion();

-- Elimina la cuenta del usuario autenticado y, en cascada, todos sus datos.
CREATE OR REPLACE FUNCTION public.eliminar_mi_cuenta() RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, auth AS $$
BEGIN
  IF auth.uid() IS NULL THEN RAISE EXCEPTION 'no autenticado'; END IF;
  DELETE FROM auth.users WHERE id = auth.uid();
END;
$$;
REVOKE ALL ON FUNCTION public.eliminar_mi_cuenta() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.eliminar_mi_cuenta() TO authenticated;

DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime')
     AND NOT EXISTS (SELECT 1 FROM pg_publication_tables WHERE pubname = 'supabase_realtime' AND tablename = 'runner_positions') THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE runner_positions;
  END IF;
END $$;
