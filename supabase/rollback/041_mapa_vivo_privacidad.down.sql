-- Revierte 041_mapa_vivo_privacidad.sql.
-- No recupera posiciones borradas o redondeadas: el mapa vuelve a guardar lat/lng tal cual
-- a partir de la proxima publicacion. Uso: psql -f este_archivo
--   o scripts/sb-query.sh supabase/rollback/041_mapa_vivo_privacidad.down.sql
BEGIN;

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

CREATE OR REPLACE FUNCTION public.trg_revocar_ubicacion() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NEW.kind = 'ubicacion_visible' AND NOT NEW.granted THEN
    DELETE FROM runner_positions WHERE user_id = NEW.user_id;
  END IF;
  RETURN NULL;
END;
$$;

GRANT EXECUTE ON FUNCTION public.mapa_publicar(DOUBLE PRECISION, DOUBLE PRECISION, BOOLEAN) TO PUBLIC;
GRANT INSERT, UPDATE, TRUNCATE ON TABLE public.runner_positions TO anon, authenticated;

DROP FUNCTION IF EXISTS envivo.difuminar_coordenada(DOUBLE PRECISION, DOUBLE PRECISION);
DROP TABLE IF EXISTS envivo.mapa_ancla;
-- Por si una version anterior de esta migracion la hubiera registrado.
DELETE FROM kernel.propiedad_tablas WHERE tabla = 'mapa_ancla';

NOTIFY pgrst, 'reload schema';
COMMIT;
