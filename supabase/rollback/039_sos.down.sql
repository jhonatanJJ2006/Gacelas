-- Revierte 039_sos.sql.
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
    PERFORM cron.unschedule('gacelas-sos-inactivas') WHERE EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'gacelas-sos-inactivas');
  END IF;
END $$;
DROP FUNCTION IF EXISTS public.sos_cerrar_inactivas();
DROP FUNCTION IF EXISTS public.sos_ver(UUID);
DROP FUNCTION IF EXISTS public.sos_terminar(UUID);
DROP FUNCTION IF EXISTS public.sos_enviar(UUID, DOUBLE PRECISION, DOUBLE PRECISION);
DROP FUNCTION IF EXISTS public.sos_iniciar(DOUBLE PRECISION, DOUBLE PRECISION);
DROP TABLE IF EXISTS seguridad.sos_alertas;
DELETE FROM kernel.propiedad_tablas WHERE tabla = 'sos_alertas';
