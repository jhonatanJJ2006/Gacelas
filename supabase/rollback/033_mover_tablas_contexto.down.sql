-- Reversion de 033: devuelve a public las tablas movidas y recrea las vistas de lectura de 032.
-- No se aplica automaticamente (no esta en supabase/migrations). Uso: psql -f este_archivo.
BEGIN;
DO $$
DECLARE r RECORD;
BEGIN
  FOR r IN SELECT tabla, esquema_actual FROM kernel.propiedad_tablas WHERE esquema_actual <> 'public' LOOP
    EXECUTE format('DROP VIEW IF EXISTS public.%I', r.tabla);
    EXECUTE format('ALTER TABLE %I.%I SET SCHEMA public', r.esquema_actual, r.tabla);
    EXECUTE format('CREATE OR REPLACE VIEW %I.%I WITH (security_invoker = true) AS SELECT * FROM public.%I',
      r.esquema_actual, r.tabla, r.tabla);
    EXECUTE format('GRANT SELECT ON %I.%I TO service_role', r.esquema_actual, r.tabla);
    UPDATE kernel.propiedad_tablas SET esquema_actual = 'public' WHERE tabla = r.tabla;
  END LOOP;
END $$;
REVOKE USAGE ON SCHEMA identidad, legal, actividad, rutas, envivo, seguridad, comunidad, eventos,
  logros, moderacion, avisos, admin, analitica FROM anon, authenticated;
DROP TABLE IF EXISTS kernel.tablas_en_public;
NOTIFY pgrst, 'reload schema';
COMMIT;
