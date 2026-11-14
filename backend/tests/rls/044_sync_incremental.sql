-- Prueba de 044: updated_at, indice, reloj y que un borrado se anota solo para su dueno
-- (los de events, que son el catalogo, los ve cualquiera).
BEGIN;
GRANT USAGE ON SCHEMA public, auth TO authenticated;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA auth TO authenticated;

INSERT INTO auth.users (id, email) VALUES
  ('00000000-0000-0000-0000-0000000044a1', 'a44@prueba.ec'),
  ('00000000-0000-0000-0000-0000000044b1', 'b44@prueba.ec')
ON CONFLICT DO NOTHING;

DO $$
BEGIN
  IF to_regclass('public.sync_borrados') IS NULL THEN RAISE EXCEPTION 'falta la vista public.sync_borrados'; END IF;
  IF to_regclass('actividad.sync_borrados') IS NULL THEN RAISE EXCEPTION 'falta actividad.sync_borrados'; END IF;
  IF NOT EXISTS (
    SELECT 1 FROM pg_trigger t
    WHERE t.tgrelid = 'actividad.sync_borrados'::regclass AND t.tgname = 'aa_bloquear_suspendido' AND NOT t.tgisinternal
  ) THEN RAISE EXCEPTION 'sync_borrados sin el trigger de suspension de 043'; END IF;
  IF public.sync_reloj() IS NULL THEN RAISE EXCEPTION 'sync_reloj no responde'; END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'activities_sync_idx') THEN
    RAISE EXCEPTION 'falta activities_sync_idx';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'activity_tracks_sync_idx') THEN
    RAISE EXCEPTION 'falta activity_tracks_sync_idx';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'saved_routes' AND column_name = 'updated_at'
  ) THEN RAISE EXCEPTION 'la vista saved_routes no expone updated_at'; END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'events' AND column_name = 'updated_at'
  ) THEN RAISE EXCEPTION 'la vista events no expone updated_at'; END IF;
END $$;

INSERT INTO public.activities (user_id, title, start_time, client_id)
VALUES ('00000000-0000-0000-0000-0000000044a1', 'Vuelta', now(), 'cliente-044');
INSERT INTO public.events (id, title, description, date_label, time_label, location, created_by)
VALUES ('ev-044', 'Salida', '', 'Hoy', '6:00', 'Jipiro', '00000000-0000-0000-0000-0000000044a1');
DELETE FROM public.activities WHERE client_id = 'cliente-044';
DELETE FROM public.events WHERE id = 'ev-044';

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM public.sync_borrados
    WHERE tabla = 'activities' AND clave = 'cliente-044' AND user_id = '00000000-0000-0000-0000-0000000044a1'
  ) THEN RAISE EXCEPTION 'no se anoto el borrado de la carrera'; END IF;
  IF NOT EXISTS (
    SELECT 1 FROM public.sync_borrados WHERE tabla = 'events' AND clave = 'ev-044' AND user_id IS NULL
  ) THEN RAISE EXCEPTION 'no se anoto el borrado del evento'; END IF;
END $$;

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000044b1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000044b1","role":"authenticated"}', true);
DO $$
DECLARE n INT;
BEGIN
  SELECT count(*) INTO n FROM public.sync_borrados WHERE tabla = 'activities' AND clave = 'cliente-044';
  IF n <> 0 THEN RAISE EXCEPTION 'B ve el borrado de la carrera de A'; END IF;
  SELECT count(*) INTO n FROM public.sync_borrados WHERE tabla = 'events' AND clave = 'ev-044';
  IF n <> 1 THEN RAISE EXCEPTION 'B no ve el borrado del catalogo de eventos'; END IF;
  BEGIN
    INSERT INTO public.sync_borrados (tabla, clave) VALUES ('activities', 'trampa');
    RAISE EXCEPTION 'un usuario pudo insertar una tumba';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
  END;
  RAISE NOTICE 'OK 044: sync incremental';
END $$;
ROLLBACK;
