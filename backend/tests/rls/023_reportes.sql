-- Pruebas de 022 y 023: rutas oficiales, reportes de contenido, estadisticas y reporte personal.
-- Se ejecuta como superusuario sobre una base con las migraciones aplicadas; deja todo como estaba.
BEGIN;
GRANT USAGE ON SCHEMA public, auth TO authenticated;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA auth TO authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO authenticated;

INSERT INTO auth.users (id, email) VALUES
  ('00000000-0000-0000-0000-0000000000a1', 'admin@prueba.ec'),
  ('00000000-0000-0000-0000-0000000000b1', 'ana@prueba.ec'),
  ('00000000-0000-0000-0000-0000000000c1', 'luis@prueba.ec')
ON CONFLICT DO NOTHING;
INSERT INTO public.profiles (id, username, full_name) VALUES
  ('00000000-0000-0000-0000-0000000000a1', 'admin', 'Admin'),
  ('00000000-0000-0000-0000-0000000000b1', 'ana', 'Ana'),
  ('00000000-0000-0000-0000-0000000000c1', 'luis', 'Luis')
ON CONFLICT (id) DO NOTHING;
INSERT INTO public.user_roles VALUES ('00000000-0000-0000-0000-0000000000a1', 'admin', now()) ON CONFLICT DO NOTHING;
INSERT INTO public.activities (id, user_id, title, start_time, distance_meters, duration_seconds, elevation_gain_meters) VALUES
  ('00000000-0000-0000-0000-00000000f001', '00000000-0000-0000-0000-0000000000b1', 'Jipiro', now() - interval '1 day', 5000, 1800, 30),
  ('00000000-0000-0000-0000-00000000f002', '00000000-0000-0000-0000-0000000000b1', 'Villonaco', now(), 10000, 4200, 600),
  ('00000000-0000-0000-0000-00000000f003', '00000000-0000-0000-0000-0000000000c1', 'Pucara', now(), 3000, 1000, 50);

DO $$
BEGIN
  IF (SELECT count(*) FROM public.routes WHERE is_official AND geom IS NOT NULL) < 20 THEN
    RAISE EXCEPTION 'faltan rutas oficiales con geometria';
  END IF;
END $$;

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000000b1', true);

-- Un usuario no puede crear rutas oficiales.
DO $$
BEGIN
  BEGIN
    INSERT INTO public.routes (name, distance_km, difficulty, author_id, is_official)
    VALUES ('Trampa', 1, 'Fácil', auth.uid(), true);
    RAISE EXCEPTION 'se pudo crear una ruta oficial';
  EXCEPTION WHEN check_violation THEN NULL;
  END;
END $$;

-- Ana reporta a Luis, un comentario y un evento; el reporte viejo de post se copia.
INSERT INTO public.content_reports (target_type, target_id, reporter_id, reason, detail)
VALUES ('usuario', '00000000-0000-0000-0000-0000000000c1', auth.uid(), 'acoso', 'mensajes'),
       ('evento', 'ev-1', auth.uid(), 'falso', '');
INSERT INTO public.post_reports (post_id, reporter_id, reason) VALUES ('00000000-0000-0000-0000-00000000f003', auth.uid(), 'spam');

DO $$
BEGIN
  IF (SELECT count(*) FROM public.content_reports) <> 3 THEN RAISE EXCEPTION 'Ana deberia ver sus 3 reportes'; END IF;
  BEGIN
    PERFORM public.admin_estadisticas(current_date - 7, current_date);
    RAISE EXCEPTION 'un usuario comun vio estadisticas';
  EXCEPTION WHEN raise_exception THEN
    IF SQLERRM <> 'solo administradores' THEN RAISE; END IF;
  END;
  IF (public.reporte_personal('semana')->'actual'->>'carreras')::int < 1 THEN
    RAISE EXCEPTION 'reporte personal vacio';
  END IF;
END $$;

-- Luis no ve los reportes de Ana ni su reporte personal.
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000000c1', true);
DO $$
BEGIN
  IF (SELECT count(*) FROM public.content_reports) <> 0 THEN RAISE EXCEPTION 'Luis ve reportes ajenos'; END IF;
  IF (public.reporte_personal('anio')->'actual'->>'metros')::int <> 3000 THEN RAISE EXCEPTION 'Luis ve carreras ajenas'; END IF;
END $$;

-- El admin modera.
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000000a1', true);
DO $$
DECLARE r UUID;
BEGIN
  IF (SELECT count(*) FROM public.admin_cola_reportes()) <> 3 THEN RAISE EXCEPTION 'cola incompleta'; END IF;
  SELECT id INTO r FROM public.content_reports WHERE target_type = 'post';
  PERFORM public.moderar_reporte(r, 'en_revision', 'mirando');
  PERFORM public.moderar_reporte(r, 'ocultar_contenido', 'spam confirmado');
  IF (SELECT status FROM public.post_reports) <> 'resuelto' THEN RAISE EXCEPTION 'post_reports no se sincronizo'; END IF;
  IF EXISTS (SELECT 1 FROM public.activities WHERE id = '00000000-0000-0000-0000-00000000f003') THEN
    RAISE EXCEPTION 'no se oculto la publicacion';
  END IF;
  IF (SELECT count(*) FROM public.content_report_actions WHERE report_id = r) <> 2 THEN RAISE EXCEPTION 'historial incompleto'; END IF;
  IF (public.admin_estadisticas(current_date - 7, current_date)->'totales'->>'carreras')::int < 2 THEN
    RAISE EXCEPTION 'estadisticas sin carreras';
  END IF;
END $$;
RESET ROLE;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM public.notifications WHERE user_id = '00000000-0000-0000-0000-0000000000b1' AND type = 'reporte') THEN
    RAISE EXCEPTION 'no se aviso al reportante';
  END IF;
END $$;
-- Un contexto nuevo se registra como tipo de objetivo sin cambiar la cola.
CREATE TABLE public.prueba_incidentes (id TEXT PRIMARY KEY, titulo TEXT, oculto BOOLEAN DEFAULT false);
INSERT INTO public.prueba_incidentes VALUES ('i1', 'Bache en la via');
CREATE FUNCTION public.prueba_resumen_incidente(o TEXT) RETURNS TEXT LANGUAGE sql AS $$ SELECT titulo FROM public.prueba_incidentes WHERE id = o $$;
CREATE FUNCTION public.prueba_ocultar_incidente(o TEXT) RETURNS VOID LANGUAGE sql AS $$ UPDATE public.prueba_incidentes SET oculto = true WHERE id = o $$;
INSERT INTO public.report_target_types (key, etiqueta, referencia, resumen_fn, ocultar_fn)
VALUES ('incidente', 'Incidente', 'el incidente', 'public.prueba_resumen_incidente', 'public.prueba_ocultar_incidente');
DO $$
BEGIN
  BEGIN
    INSERT INTO public.content_reports (target_type, target_id, reporter_id, reason)
    VALUES ('desconocido', 'x', '00000000-0000-0000-0000-0000000000b1', 'spam');
    RAISE EXCEPTION 'acepto un tipo no registrado';
  EXCEPTION WHEN foreign_key_violation THEN NULL;
  END;
END $$;
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000000b1', true);
INSERT INTO public.content_reports (target_type, target_id, reporter_id, reason)
VALUES ('incidente', 'i1', '00000000-0000-0000-0000-0000000000b1', 'falso');
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000000a1', true);
DO $$
DECLARE r UUID;
BEGIN
  IF (SELECT resumen FROM public.admin_cola_reportes(NULL, 'incidente') LIMIT 1) <> 'Bache en la via' THEN
    RAISE EXCEPTION 'la cola no resume el tipo nuevo';
  END IF;
  SELECT id INTO r FROM public.content_reports WHERE target_type = 'incidente';
  PERFORM public.moderar_reporte(r, 'ocultar_contenido', '');
  BEGIN
    PERFORM public.moderar_reporte(r, 'suspender_usuario', '');
    RAISE EXCEPTION 'suspendio sin autor';
  EXCEPTION WHEN raise_exception THEN
    IF SQLERRM <> 'este tipo no tiene autor' THEN RAISE; END IF;
  END;
END $$;
RESET ROLE;
DO $$
BEGIN
  IF NOT (SELECT oculto FROM public.prueba_incidentes WHERE id = 'i1') THEN RAISE EXCEPTION 'no se oculto el incidente'; END IF;
  IF NOT EXISTS (SELECT 1 FROM public.notifications WHERE type = 'reporte' AND message LIKE '%el incidente%') THEN
    RAISE EXCEPTION 'aviso sin referencia del tipo';
  END IF;
END $$;
SELECT 'ok 023_reportes' AS resultado;
ROLLBACK;
