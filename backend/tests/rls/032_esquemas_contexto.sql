-- Prueba de 032: cada tabla de public tiene contexto asignado, RLS activa y vista en su esquema.
DO $$
DECLARE faltan TEXT; sin_rls TEXT; sin_vista TEXT;
BEGIN
  SELECT string_agg(c.relname, ', ') INTO faltan FROM pg_class c
  WHERE c.relnamespace = 'public'::regnamespace AND c.relkind = 'r' AND c.relname <> 'spatial_ref_sys'
    AND NOT EXISTS (SELECT 1 FROM kernel.propiedad_tablas p WHERE p.tabla = c.relname);
  IF faltan IS NOT NULL THEN RAISE EXCEPTION 'Tablas sin contexto: %', faltan; END IF;
  SELECT string_agg(p.tabla, ', ') INTO sin_rls FROM kernel.propiedad_tablas p
  JOIN pg_class c ON c.oid = to_regclass(p.esquema_actual || '.' || p.tabla) WHERE NOT c.relrowsecurity;
  IF sin_rls IS NOT NULL THEN RAISE EXCEPTION 'Tablas sin RLS: %', sin_rls; END IF;
  SELECT string_agg(p.tabla, ', ') INTO sin_vista FROM kernel.propiedad_tablas p
  WHERE to_regclass(p.esquema_actual || '.' || p.tabla) IS NOT NULL
    AND to_regclass(p.contexto || '.' || p.tabla) IS NULL;
  IF sin_vista IS NOT NULL THEN RAISE EXCEPTION 'Tablas sin vista de contexto: %', sin_vista; END IF;
  RAISE NOTICE 'OK 032: esquemas por contexto';
END $$;
