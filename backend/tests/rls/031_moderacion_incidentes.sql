-- Prueba de 031: un reporte sobre un incidente pasa por la cola generica y actualiza su estado.
BEGIN;
GRANT USAGE ON SCHEMA public, auth TO authenticated;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA auth TO authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO authenticated;
INSERT INTO auth.users (id, email) VALUES
  ('00000000-0000-0000-0000-0000000031a1', 'admin31@prueba.ec'),
  ('00000000-0000-0000-0000-0000000031b1', 'autor31@prueba.ec'),
  ('00000000-0000-0000-0000-0000000031c1', 'testigo31@prueba.ec')
ON CONFLICT DO NOTHING;
INSERT INTO public.user_roles VALUES ('00000000-0000-0000-0000-0000000031a1', 'admin', now())
ON CONFLICT DO NOTHING;
ALTER TABLE seguridad.incidentes_usuario DISABLE TRIGGER USER;
INSERT INTO public.incidentes_usuario (id, user_id, tipo, descripcion, geom, direccion) VALUES
  ('00000000-0000-0000-0000-0000000031e1', '00000000-0000-0000-0000-0000000031b1', 'via_mal_estado',
   'Hueco grande', ST_SetSRID(ST_MakePoint(-79.2, -4.0), 4326), 'Av. Universitaria'),
  ('00000000-0000-0000-0000-0000000031e2', '00000000-0000-0000-0000-0000000031b1', 'otro',
   'Prueba', ST_SetSRID(ST_MakePoint(-79.2, -4.0), 4326), NULL);
ALTER TABLE seguridad.incidentes_usuario ENABLE TRIGGER USER;
INSERT INTO public.content_reports (target_type, target_id, reporter_id, reason) VALUES
  ('incidente', '00000000-0000-0000-0000-0000000031e1', '00000000-0000-0000-0000-0000000031c1', 'falso'),
  ('incidente', '00000000-0000-0000-0000-0000000031e2', '00000000-0000-0000-0000-0000000031c1', 'falso');

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000031a1', true);
DO $$
DECLARE r1 UUID; r2 UUID;
BEGIN
  IF (SELECT resumen FROM public.admin_cola_reportes(NULL, 'incidente')
      WHERE target_id = '00000000-0000-0000-0000-0000000031e1') NOT LIKE 'via mal estado en Av. Universitaria%' THEN
    RAISE EXCEPTION 'resumen del incidente incorrecto';
  END IF;
  SELECT id INTO r1 FROM public.content_reports WHERE target_id = '00000000-0000-0000-0000-0000000031e1';
  SELECT id INTO r2 FROM public.content_reports WHERE target_id = '00000000-0000-0000-0000-0000000031e2';
  PERFORM public.moderar_reporte(r1, 'descartado', 'el incidente es real');
  PERFORM public.moderar_reporte(r2, 'ocultar_contenido', 'reporte falso');
END $$;
RESET ROLE;
DO $$
BEGIN
  IF (SELECT estado FROM public.incidentes_usuario WHERE id = '00000000-0000-0000-0000-0000000031e1') <> 'verificado' THEN
    RAISE EXCEPTION 'descartar no verifico el incidente';
  END IF;
  IF (SELECT estado FROM public.incidentes_usuario WHERE id = '00000000-0000-0000-0000-0000000031e2') <> 'rechazado' THEN
    RAISE EXCEPTION 'ocultar no rechazo el incidente';
  END IF;
END $$;
SELECT 'ok 031_moderacion_incidentes' AS resultado;
ROLLBACK;
