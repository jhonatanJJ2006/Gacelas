-- 045: 039 registro seguridad.sos_alertas y 040 registro rutas.seguridad_rutas_cache
-- en kernel.propiedad_tablas sin la vista security_invoker en public que exige
-- la prueba de 033 (ADR 005). PostgREST sigue en public; sin la vista el nombre
-- que usarian los clientes no existe.
-- No abre permisos nuevos: SOS se usa por sus funciones y la cache solo la escribe
-- service_role. 044 lo ocupa el PR de sync incremental.

CREATE OR REPLACE VIEW public.sos_alertas
  WITH (security_invoker = true) AS
  SELECT * FROM seguridad.sos_alertas;
REVOKE ALL ON public.sos_alertas FROM PUBLIC, anon, authenticated;

CREATE OR REPLACE VIEW public.seguridad_rutas_cache
  WITH (security_invoker = true) AS
  SELECT * FROM rutas.seguridad_rutas_cache;
REVOKE ALL ON public.seguridad_rutas_cache FROM PUBLIC, anon, authenticated;
GRANT ALL ON public.seguridad_rutas_cache TO service_role;

NOTIFY pgrst, 'reload schema';
