-- Reversion de 035: quita la ruta sugerida del servidor. Uso: psql -f este_archivo.
-- Se pierde el historial de sugerencias. Las apps vuelven al calculo local (fallback).
BEGIN;
DROP FUNCTION IF EXISTS public.ruta_sugerida_hoy(DOUBLE PRECISION, DOUBLE PRECISION);
DROP VIEW IF EXISTS public.route_suggestions;
DROP TABLE IF EXISTS rutas.route_suggestions;
DO $$ BEGIN
  IF to_regclass('kernel.propiedad_tablas') IS NOT NULL THEN
    DELETE FROM kernel.propiedad_tablas WHERE tabla = 'route_suggestions';
  END IF;
END $$;
NOTIFY pgrst, 'reload schema';
COMMIT;
