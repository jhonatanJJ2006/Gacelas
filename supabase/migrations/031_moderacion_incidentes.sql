-- 031: conecta los incidentes de corredores (030, contexto Seguridad de rutas) con la cola de
-- moderacion generica (023). Idempotente. No cambia 030.
--  - Resumen: tipo, direccion y descripcion del incidente.
--  - Ocultar: el incidente queda rechazado (deja de contar en el indice y en el mapa).
--  - Autor: quien lo reporto, para poder suspenderlo.
--  - Al cerrar el caso: resuelto (se tomaron medidas) => rechazado; descartado (el incidente es
--    valido) => verificado.

CREATE OR REPLACE FUNCTION public.moderacion_resumen_incidente(objetivo TEXT) RETURNS TEXT
LANGUAGE sql STABLE SET search_path = public AS $$
  SELECT left(replace(i.tipo, '_', ' ') || coalesce(' en ' || i.direccion, '')
    || coalesce(': ' || i.descripcion, ''), 140)
  FROM public.incidentes_usuario i WHERE i.id::text = objetivo
$$;
CREATE OR REPLACE FUNCTION public.moderacion_ocultar_incidente(objetivo TEXT) RETURNS VOID
LANGUAGE sql SET search_path = public AS $$
  UPDATE public.incidentes_usuario SET estado = 'rechazado' WHERE id::text = objetivo
$$;
CREATE OR REPLACE FUNCTION public.moderacion_autor_incidente(objetivo TEXT) RETURNS UUID
LANGUAGE sql STABLE SET search_path = public AS $$
  SELECT i.user_id FROM public.incidentes_usuario i WHERE i.id::text = objetivo
$$;
CREATE OR REPLACE FUNCTION public.moderacion_resolver_incidente(objetivo TEXT, resultado TEXT) RETURNS VOID
LANGUAGE sql SET search_path = public AS $$
  UPDATE public.incidentes_usuario
  SET estado = CASE resultado WHEN 'resuelto' THEN 'rechazado' ELSE 'verificado' END
  WHERE id::text = objetivo
$$;
REVOKE ALL ON FUNCTION public.moderacion_resumen_incidente(TEXT) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.moderacion_ocultar_incidente(TEXT) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.moderacion_autor_incidente(TEXT) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.moderacion_resolver_incidente(TEXT, TEXT) FROM PUBLIC;

INSERT INTO public.report_target_types (key, etiqueta, referencia, resumen_fn, ocultar_fn, autor_fn, resolver_fn)
VALUES ('incidente', 'Incidente', 'el incidente', 'public.moderacion_resumen_incidente',
  'public.moderacion_ocultar_incidente', 'public.moderacion_autor_incidente',
  'public.moderacion_resolver_incidente')
ON CONFLICT (key) DO UPDATE SET etiqueta = EXCLUDED.etiqueta, referencia = EXCLUDED.referencia,
  resumen_fn = EXCLUDED.resumen_fn, ocultar_fn = EXCLUDED.ocultar_fn, autor_fn = EXCLUDED.autor_fn,
  resolver_fn = EXCLUDED.resolver_fn;
