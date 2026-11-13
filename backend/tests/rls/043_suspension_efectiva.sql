-- Prueba de 043: un usuario suspendido no escribe; uno normal si.
-- Lectura, SOS y eliminar_mi_cuenta siguen abiertos. Todo se revierte al final.
BEGIN;

DO $$
DECLARE faltan text;
BEGIN
  SELECT string_agg(n.nspname || '.' || c.relname, ', ' ORDER BY 1) INTO faltan
  FROM pg_class c
  JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE c.relkind = 'r'
    AND c.relrowsecurity
    AND n.nspname IN (
      'public', 'identidad', 'legal', 'actividad', 'rutas', 'envivo',
      'seguridad', 'comunidad', 'eventos', 'logros', 'moderacion',
      'avisos', 'admin', 'analitica'
    )
    AND NOT EXISTS (SELECT 1 FROM pg_depend d WHERE d.objid = c.oid AND d.deptype = 'e')
    AND (n.nspname || '.' || c.relname) NOT IN ('seguridad.sos_alertas', 'rutas.seguridad_rutas_cache')
    AND NOT EXISTS (
      SELECT 1 FROM pg_trigger t
      WHERE t.tgrelid = c.oid AND t.tgname = 'aa_bloquear_suspendido' AND NOT t.tgisinternal
    );
  IF faltan IS NOT NULL THEN
    RAISE EXCEPTION 'Faltan triggers de suspension: %', faltan;
  END IF;
  IF EXISTS (SELECT 1 FROM pg_trigger t WHERE t.tgrelid = 'seguridad.sos_alertas'::regclass
             AND t.tgname = 'aa_bloquear_suspendido' AND NOT t.tgisinternal) THEN
    RAISE EXCEPTION 'SOS no deberia bloquearse';
  END IF;
  IF EXISTS (SELECT 1 FROM pg_trigger t WHERE t.tgrelid = 'rutas.seguridad_rutas_cache'::regclass
             AND t.tgname = 'aa_bloquear_suspendido' AND NOT t.tgisinternal) THEN
    RAISE EXCEPTION 'La cache de seguridad no deberia bloquearse';
  END IF;
  IF to_regclass('storage.objects') IS NOT NULL
     AND NOT EXISTS (SELECT 1 FROM pg_trigger t WHERE t.tgrelid = 'storage.objects'::regclass
                     AND t.tgname = 'aa_bloquear_suspendido' AND NOT t.tgisinternal) THEN
    RAISE EXCEPTION 'storage.objects sin trigger de suspension';
  END IF;
END $$;

CREATE OR REPLACE PROCEDURE public._prueba_043_debe_bloquear(accion text)
LANGUAGE plpgsql AS $$
BEGIN
  EXECUTE accion;
  RAISE EXCEPTION 'un suspendido pudo escribir: %', accion;
EXCEPTION WHEN insufficient_privilege THEN
  IF SQLERRM NOT LIKE '%cuenta suspendida%' THEN
    RAISE EXCEPTION 'error distinto al bloquear (%): %', accion, SQLERRM;
  END IF;
END;
$$;
GRANT EXECUTE ON PROCEDURE public._prueba_043_debe_bloquear(text) TO authenticated;

GRANT USAGE ON SCHEMA public, auth TO authenticated;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA auth TO authenticated;

INSERT INTO auth.users (id, email) VALUES
  ('00000000-0000-0000-0000-0000000043a1', 'a43@prueba.ec'),
  ('00000000-0000-0000-0000-0000000043b1', 'b43@prueba.ec'),
  ('00000000-0000-0000-0000-0000000043d1', 'd43@prueba.ec');
INSERT INTO public.user_roles (user_id, role) VALUES
  ('00000000-0000-0000-0000-0000000043d1', 'admin');
INSERT INTO public.routes (id, name, distance_km, difficulty, is_public)
  VALUES ('ruta-043', 'Ruta 043', 5, 'Moderado', true);
INSERT INTO storage.buckets (id, name) VALUES ('post-images', 'post-images') ON CONFLICT (id) DO NOTHING;

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000043a1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000043a1","role":"authenticated"}', true);

-- Antes de suspender, A deja carrera, consentimiento, club, mapa y sesion en vivo.
INSERT INTO public.activities (id, user_id, title, start_time, is_public)
  VALUES ('00000000-0000-0000-0000-0000000043aa', auth.uid(), 'Carrera previa', now(), true);
INSERT INTO public.consents (user_id, kind, version, granted) VALUES
  (auth.uid(), 'ubicacion_visible', '2026-10-25', true),
  (auth.uid(), 'gps_carrera', '2026-10-25', true);
INSERT INTO public.clubs (name, owner_id) VALUES ('Club 043 A', auth.uid());
SELECT public.live_iniciar('public');
SELECT public.mapa_publicar(-3.993, -79.204, true);

-- El admin suspende a A: se cierra el en vivo y desaparece del mapa.
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000043d1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000043d1","role":"authenticated"}', true);
SELECT public.admin_suspender('00000000-0000-0000-0000-0000000043a1', true);

SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000043b1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000043b1","role":"authenticated"}', true);
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM public.live_sessions WHERE user_id = '00000000-0000-0000-0000-0000000043a1') THEN
    RAISE EXCEPTION 'la sesion en vivo de un suspendido sigue visible';
  END IF;
  IF EXISTS (SELECT 1 FROM public.runner_positions WHERE user_id = '00000000-0000-0000-0000-0000000043a1') THEN
    RAISE EXCEPTION 'el punto del mapa de un suspendido sigue visible';
  END IF;
END $$;

RESET ROLE;
DO $$
BEGIN
  IF (SELECT suspended_at FROM identidad.profiles WHERE id = '00000000-0000-0000-0000-0000000043a1') IS NULL THEN
    RAISE EXCEPTION 'admin_suspender no marco la cuenta';
  END IF;
  IF EXISTS (SELECT 1 FROM public.live_sessions
             WHERE user_id = '00000000-0000-0000-0000-0000000043a1' AND ended_at IS NULL) THEN
    RAISE EXCEPTION 'la sesion en vivo no se cerro al suspender';
  END IF;
  IF EXISTS (SELECT 1 FROM public.runner_positions WHERE user_id = '00000000-0000-0000-0000-0000000043a1') THEN
    RAISE EXCEPTION 'el punto del mapa no se borro al suspender';
  END IF;
END $$;

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000043a1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000043a1","role":"authenticated"}', true);

-- Leer lo propio sigue permitido.
DO $$
BEGIN
  IF (SELECT title FROM public.activities WHERE id = '00000000-0000-0000-0000-0000000043aa') IS DISTINCT FROM 'Carrera previa' THEN
    RAISE EXCEPTION 'el suspendido no lee su carrera';
  END IF;
  IF (SELECT id FROM public.profiles WHERE id = auth.uid()) IS NULL THEN
    RAISE EXCEPTION 'el suspendido no lee su perfil';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM public.user_settings WHERE user_id = auth.uid()) THEN
    RAISE EXCEPTION 'el suspendido no lee sus ajustes';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM public.notifications WHERE user_id = auth.uid()) THEN
    RAISE EXCEPTION 'el suspendido no lee sus avisos';
  END IF;
END $$;

-- No puede limpiar la suspension ni ninguna otra escritura.
CALL public._prueba_043_debe_bloquear($$UPDATE public.profiles SET suspended_at = NULL, full_name = 'Libre' WHERE id = auth.uid()$$);
CALL public._prueba_043_debe_bloquear($$UPDATE public.profiles SET full_name = 'Libre' WHERE id = auth.uid()$$);
CALL public._prueba_043_debe_bloquear($$UPDATE public.user_settings SET weekly_goal_runs = 4 WHERE user_id = auth.uid()$$);
CALL public._prueba_043_debe_bloquear($$UPDATE public.notifications SET read = true WHERE user_id = auth.uid()$$);
CALL public._prueba_043_debe_bloquear($$UPDATE public.activities SET title = 'Editada' WHERE id = '00000000-0000-0000-0000-0000000043aa'$$);
CALL public._prueba_043_debe_bloquear($$DELETE FROM public.activities WHERE id = '00000000-0000-0000-0000-0000000043aa'$$);
CALL public._prueba_043_debe_bloquear($$INSERT INTO public.activities (user_id, title, start_time, is_public) VALUES (auth.uid(), 'Otra', now(), true)$$);
CALL public._prueba_043_debe_bloquear($$INSERT INTO public.post_comments (user_id, post_id, body) VALUES (auth.uid(), '00000000-0000-0000-0000-0000000043aa', 'no')$$);
CALL public._prueba_043_debe_bloquear($$INSERT INTO public.post_likes (user_id, post_id) VALUES (auth.uid(), '00000000-0000-0000-0000-0000000043aa')$$);
CALL public._prueba_043_debe_bloquear($$INSERT INTO public.post_reposts (user_id, activity_id) VALUES (auth.uid(), '00000000-0000-0000-0000-0000000043aa')$$);
CALL public._prueba_043_debe_bloquear($$INSERT INTO public.follows (follower_id, following_id) VALUES (auth.uid(), '00000000-0000-0000-0000-0000000043b1')$$);
CALL public._prueba_043_debe_bloquear($$INSERT INTO public.routes (name, distance_km, difficulty, author_id) VALUES ('Ruta de A', 3, 'Fácil', auth.uid())$$);
CALL public._prueba_043_debe_bloquear($$INSERT INTO public.route_likes (route_id, user_id) VALUES ('ruta-043', auth.uid())$$);
CALL public._prueba_043_debe_bloquear($$INSERT INTO public.route_reviews (route_id, user_id, rating) VALUES ('ruta-043', auth.uid(), 5)$$);
CALL public._prueba_043_debe_bloquear($$INSERT INTO public.route_runs (route_id, user_id, activity_id) VALUES ('ruta-043', auth.uid(), '00000000-0000-0000-0000-0000000043aa')$$);
CALL public._prueba_043_debe_bloquear($$INSERT INTO public.saved_routes (user_id, route_id) VALUES (auth.uid(), 'ruta-043')$$);
CALL public._prueba_043_debe_bloquear($$INSERT INTO public.clubs (name, owner_id) VALUES ('Otro club 043', auth.uid())$$);
CALL public._prueba_043_debe_bloquear($$INSERT INTO public.club_posts (club_id, user_id, body) SELECT id, auth.uid(), 'hola' FROM public.clubs WHERE owner_id = auth.uid()$$);
CALL public._prueba_043_debe_bloquear($$INSERT INTO public.events (title, date_label, time_label, location, created_by) VALUES ('Salida', 'sabado', '06:00', 'Parque', auth.uid())$$);
CALL public._prueba_043_debe_bloquear($$INSERT INTO public.content_reports (target_type, target_id, reporter_id, reason) VALUES ('usuario', '00000000-0000-0000-0000-0000000043b1', auth.uid(), 'spam')$$);
CALL public._prueba_043_debe_bloquear($$INSERT INTO public.user_achievements (user_id, code) VALUES (auth.uid(), 'primera_carrera')$$);
CALL public._prueba_043_debe_bloquear($$INSERT INTO public.consents (user_id, kind, version, granted) VALUES (auth.uid(), 'terminos', '2026-10-25', true)$$);
CALL public._prueba_043_debe_bloquear($$INSERT INTO public.incidentes_usuario (user_id, tipo, geom) VALUES (auth.uid(), 'otro', ST_SetSRID(ST_MakePoint(-79.2, -3.99), 4326))$$);
CALL public._prueba_043_debe_bloquear($$SELECT public.live_iniciar('public')$$);
CALL public._prueba_043_debe_bloquear($$SELECT public.mapa_publicar(-3.99, -79.20, false)$$);
CALL public._prueba_043_debe_bloquear($$SELECT public.append_track_batch('00000000-0000-0000-0000-0000000043aa', 'lote-a', '[{"lat":-3.99,"lon":-79.2,"timestamp":"2026-11-13T12:00:00Z"}]'::jsonb)$$);
CALL public._prueba_043_debe_bloquear($$INSERT INTO storage.objects (bucket_id, name, owner) VALUES ('post-images', auth.uid()::text || '/a.jpg', auth.uid())$$);

DO $$
DECLARE sugerida jsonb;
BEGIN
  BEGIN
    sugerida := public.ruta_sugerida_hoy(NULL, NULL);
  EXCEPTION WHEN insufficient_privilege THEN
    IF SQLERRM NOT LIKE '%cuenta suspendida%' THEN RAISE; END IF;
    sugerida := NULL;
  END;
  IF EXISTS (SELECT 1 FROM rutas.route_suggestions WHERE user_id = auth.uid()) THEN
    RAISE EXCEPTION 'un suspendido guardo una ruta sugerida';
  END IF;
END $$;

DO $$
DECLARE nombre text;
BEGIN
  SELECT full_name INTO nombre FROM public.profiles WHERE id = auth.uid();
  IF nombre = 'Libre' THEN RAISE EXCEPTION 'el perfil del suspendido cambio'; END IF;
  IF (SELECT suspended_at FROM public.profiles WHERE id = auth.uid()) IS NULL THEN
    RAISE EXCEPTION 'el suspendido limpio suspended_at';
  END IF;
  IF (SELECT title FROM public.activities WHERE id = '00000000-0000-0000-0000-0000000043aa') IS DISTINCT FROM 'Carrera previa' THEN
    RAISE EXCEPTION 'la carrera del suspendido cambio';
  END IF;
END $$;

-- SOS de emergencia sigue abierto, y no lo habilita a escribir en el feed.
DO $$
DECLARE alerta uuid;
BEGIN
  alerta := public.sos_iniciar(-3.99, -79.20);
  IF alerta IS NULL THEN RAISE EXCEPTION 'SOS no arranco'; END IF;
  IF NOT public.sos_enviar(alerta, -3.991, -79.205) THEN RAISE EXCEPTION 'SOS no acepto la posicion'; END IF;
  PERFORM public.sos_terminar(alerta);
END $$;
CALL public._prueba_043_debe_bloquear($$INSERT INTO public.post_comments (user_id, post_id, body) VALUES (auth.uid(), '00000000-0000-0000-0000-0000000043aa', 'tras sos')$$);

-- B, sin suspension, hace las mismas escrituras.
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000043b1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000043b1","role":"authenticated"}', true);
INSERT INTO public.activities (id, user_id, title, start_time, is_public)
  VALUES ('00000000-0000-0000-0000-0000000043bb', auth.uid(), 'Carrera de B', now(), true);
INSERT INTO public.post_comments (user_id, post_id, body)
  VALUES (auth.uid(), '00000000-0000-0000-0000-0000000043bb', 'va bien');
INSERT INTO public.post_likes (user_id, post_id)
  VALUES (auth.uid(), '00000000-0000-0000-0000-0000000043aa');
INSERT INTO public.follows (follower_id, following_id)
  VALUES (auth.uid(), '00000000-0000-0000-0000-0000000043a1');
INSERT INTO public.routes (id, name, distance_km, difficulty, author_id)
  VALUES ('ruta-043-b', 'Ruta de B', 4, 'Fácil', auth.uid());
INSERT INTO public.route_likes (route_id, user_id) VALUES ('ruta-043', auth.uid());
INSERT INTO public.route_reviews (route_id, user_id, rating, comment)
  VALUES ('ruta-043', auth.uid(), 4, 'buena');
INSERT INTO public.saved_routes (user_id, route_id) VALUES (auth.uid(), 'ruta-043');
INSERT INTO public.clubs (name, owner_id) VALUES ('Club 043 B', auth.uid());
INSERT INTO public.club_posts (club_id, user_id, body)
  SELECT id, auth.uid(), 'salida del sabado' FROM public.clubs WHERE name = 'Club 043 B';
INSERT INTO public.events (id, title, date_label, time_label, location, created_by)
  VALUES ('evt-043-b', 'Salida B', 'sabado', '06:00', 'Parque', auth.uid());
INSERT INTO public.event_rsvps (event_id, user_id) VALUES ('evt-043-b', auth.uid());
INSERT INTO public.content_reports (target_type, target_id, reporter_id, reason)
  VALUES ('usuario', '00000000-0000-0000-0000-0000000043a1', auth.uid(), 'spam');
INSERT INTO public.user_achievements (user_id, code) VALUES (auth.uid(), 'primera_carrera');
INSERT INTO public.consents (user_id, kind, version, granted)
  VALUES (auth.uid(), 'ubicacion_visible', '2026-10-25', true);
SELECT public.mapa_publicar(-3.99, -79.20, true);
SELECT public.live_iniciar('followers');
SELECT public.append_track_batch(
  '00000000-0000-0000-0000-0000000043bb', 'lote-b',
  '[{"lat":-3.99,"lon":-79.2,"timestamp":"2026-11-13T12:00:00Z"}]'::jsonb);
UPDATE public.user_settings SET weekly_goal_runs = 4 WHERE user_id = auth.uid();
UPDATE public.notifications SET read = true WHERE user_id = auth.uid();
UPDATE public.profiles SET full_name = 'Corredor B' WHERE id = auth.uid();
INSERT INTO storage.objects (bucket_id, name, owner)
  VALUES ('post-images', auth.uid()::text || '/b.jpg', auth.uid());

DO $$
BEGIN
  IF (SELECT full_name FROM public.profiles WHERE id = auth.uid()) IS DISTINCT FROM 'Corredor B' THEN
    RAISE EXCEPTION 'B no pudo actualizar su perfil';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM public.runner_positions WHERE user_id = auth.uid()) THEN
    RAISE EXCEPTION 'B no publico su posicion';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM public.live_sessions WHERE user_id = auth.uid() AND ended_at IS NULL) THEN
    RAISE EXCEPTION 'B no abrio sesion en vivo';
  END IF;
  IF (SELECT public.append_track_batch(
        '00000000-0000-0000-0000-0000000043bb', 'lote-b',
        '[{"lat":-3.99,"lon":-79.2,"timestamp":"2026-11-13T12:00:00Z"}]'::jsonb)) <> 0 THEN
    RAISE EXCEPTION 'el lote repetido no debio reinsertar puntos';
  END IF;
END $$;

-- Reactivar devuelve la escritura. Volver a suspender la quita.
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000043d1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000043d1","role":"authenticated"}', true);
SELECT public.admin_suspender('00000000-0000-0000-0000-0000000043a1', false);

SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000043a1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000043a1","role":"authenticated"}', true);
INSERT INTO public.post_comments (user_id, post_id, body)
  VALUES (auth.uid(), '00000000-0000-0000-0000-0000000043aa', 'ya puedo');

SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000043d1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000043d1","role":"authenticated"}', true);
SELECT public.admin_suspender('00000000-0000-0000-0000-0000000043a1', true);

SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000043a1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000043a1","role":"authenticated"}', true);
CALL public._prueba_043_debe_bloquear($$INSERT INTO public.post_comments (user_id, post_id, body) VALUES (auth.uid(), '00000000-0000-0000-0000-0000000043aa', 'otra vez no')$$);

-- Borrar la cuenta sigue permitido y no se lleva los datos de B.
SELECT public.eliminar_mi_cuenta();

RESET ROLE;
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM auth.users WHERE id = '00000000-0000-0000-0000-0000000043a1') THEN
    RAISE EXCEPTION 'la cuenta suspendida no se borro';
  END IF;
  IF EXISTS (SELECT 1 FROM public.activities WHERE id = '00000000-0000-0000-0000-0000000043aa') THEN
    RAISE EXCEPTION 'la carrera debio caer con la cuenta';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM public.activities WHERE id = '00000000-0000-0000-0000-0000000043bb') THEN
    RAISE EXCEPTION 'se borro la carrera de un usuario que no estaba suspendido';
  END IF;
  IF (SELECT full_name FROM public.profiles WHERE id = '00000000-0000-0000-0000-0000000043b1') IS DISTINCT FROM 'Corredor B' THEN
    RAISE EXCEPTION 'el perfil de B no quedo';
  END IF;
END $$;

ROLLBACK;
