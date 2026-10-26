-- Prueba de 033: las tablas movidas a su contexto se siguen usando por public (vistas
-- actualizables con security_invoker), con RLS, RETURNING, upsert y el trigger de alta.
DO $$
DECLARE malas TEXT;
BEGIN
  -- Cada tabla movida tiene en public una vista security_invoker con las mismas columnas.
  SELECT string_agg(p.tabla, ', ') INTO malas FROM kernel.propiedad_tablas p
  WHERE p.esquema_actual <> 'public' AND NOT EXISTS (
    SELECT 1 FROM pg_class v WHERE v.oid = to_regclass('public.' || p.tabla) AND v.relkind = 'v'
      AND v.reloptions @> ARRAY['security_invoker=true']);
  IF malas IS NOT NULL THEN RAISE EXCEPTION 'Sin vista security_invoker en public: %', malas; END IF;
  SELECT string_agg(p.tabla, ', ') INTO malas FROM kernel.propiedad_tablas p
  WHERE p.esquema_actual <> 'public' AND (
    SELECT array_agg(attname::text ORDER BY attnum) FROM pg_attribute
    WHERE attrelid = to_regclass(p.esquema_actual || '.' || p.tabla) AND attnum > 0 AND NOT attisdropped)
    IS DISTINCT FROM (
    SELECT array_agg(attname::text ORDER BY attnum) FROM pg_attribute
    WHERE attrelid = to_regclass('public.' || p.tabla) AND attnum > 0 AND NOT attisdropped);
  IF malas IS NOT NULL THEN RAISE EXCEPTION 'Columnas distintas entre tabla y vista: %', malas; END IF;
  -- Las tablas publicadas en Realtime siguen en public.
  SELECT string_agg(t.tablename, ', ') INTO malas FROM pg_publication_tables t
  WHERE t.pubname = 'supabase_realtime' AND t.schemaname <> 'public';
  IF malas IS NOT NULL THEN RAISE EXCEPTION 'Tablas de Realtime fuera de public: %', malas; END IF;
  -- Toda tabla de contexto tiene RLS.
  SELECT string_agg(c.oid::regclass::text, ', ') INTO malas FROM pg_class c
  JOIN kernel.propiedad_tablas p ON c.oid = to_regclass(p.esquema_actual || '.' || p.tabla)
  WHERE c.relkind = 'r' AND NOT c.relrowsecurity;
  IF malas IS NOT NULL THEN RAISE EXCEPTION 'Tablas sin RLS: %', malas; END IF;
END $$;

BEGIN;
GRANT USAGE ON SCHEMA public, auth TO authenticated;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA auth TO authenticated;
INSERT INTO auth.users (id, email) VALUES
  ('00000000-0000-0000-0000-0000000033a1', 'a33@prueba.ec'),
  ('00000000-0000-0000-0000-0000000033b1', 'b33@prueba.ec')
ON CONFLICT DO NOTHING;
INSERT INTO public.routes (id, name, distance_km, difficulty, is_public)
  VALUES ('ruta-prueba-033', 'Ruta 033', 5, 'Moderado', true) ON CONFLICT DO NOTHING;
INSERT INTO public.notifications (user_id, type, message)
  VALUES ('00000000-0000-0000-0000-0000000033a1', 'event', 'solo para A');

DO $$
BEGIN
  -- handle_new_user escribe en public.profiles (vista) y la fila queda en identidad.profiles.
  IF NOT EXISTS (SELECT 1 FROM identidad.profiles WHERE id = '00000000-0000-0000-0000-0000000033a1') THEN
    RAISE EXCEPTION 'El trigger de alta no creo el perfil en identidad.profiles';
  END IF;
END $$;

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000033a1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000033a1","role":"authenticated"}', true);
DO $$
DECLARE n INT; rid TEXT;
BEGIN
  INSERT INTO public.saved_routes (user_id, route_id)
    VALUES (auth.uid(), 'ruta-prueba-033') RETURNING route_id INTO rid;
  IF rid IS DISTINCT FROM 'ruta-prueba-033' THEN RAISE EXCEPTION 'RETURNING por la vista fallo'; END IF;
  INSERT INTO public.user_settings (user_id, weekly_goal_runs) VALUES (auth.uid(), 4)
    ON CONFLICT (user_id) DO UPDATE SET weekly_goal_runs = EXCLUDED.weekly_goal_runs;
  UPDATE public.user_settings SET weekly_goal_runs = 5 WHERE user_id = auth.uid() RETURNING weekly_goal_runs INTO n;
  IF n IS DISTINCT FROM 5 THEN RAISE EXCEPTION 'UPDATE por la vista fallo'; END IF;
  DELETE FROM public.saved_routes WHERE user_id = auth.uid() AND route_id = 'ruta-prueba-033';
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n <> 1 THEN RAISE EXCEPTION 'DELETE por la vista fallo'; END IF;
  SELECT count(*) INTO n FROM public.notifications WHERE message = 'solo para A';
  IF n <> 1 THEN RAISE EXCEPTION 'A no ve su notificacion'; END IF;
END $$;

SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000033b1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000033b1","role":"authenticated"}', true);
DO $$
DECLARE n INT;
BEGIN
  SELECT count(*) INTO n FROM public.notifications WHERE message = 'solo para A';
  IF n <> 0 THEN RAISE EXCEPTION 'RLS no aplica a traves de la vista: B ve la notificacion de A'; END IF;
  BEGIN
    INSERT INTO public.saved_routes (user_id, route_id) VALUES ('00000000-0000-0000-0000-0000000033a1', 'ruta-prueba-033');
    RAISE EXCEPTION 'RLS no bloqueo una escritura ajena';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
  END;
  SELECT count(*) INTO n FROM public.routes WHERE id = 'ruta-prueba-033';
  IF n <> 1 THEN RAISE EXCEPTION 'Rutas publicas no visibles por la vista'; END IF;
  RAISE NOTICE 'OK 033: tablas por contexto con vistas en public';
END $$;
RESET ROLE;
ROLLBACK;
