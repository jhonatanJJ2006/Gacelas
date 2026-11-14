-- Prueba de 042: anon y otro usuario leen la ficha publica y no la edad, el genero,
-- la bio ni suspended_at. Una cuenta suspendida no edita (043). Al levantarla, el
-- dueno edita esos datos menos la suspension. Un admin la ve y puede suspender.
-- La tabla identidad.profiles no entrega esas columnas.
BEGIN;
GRANT USAGE ON SCHEMA public, auth, identidad TO anon, authenticated;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA auth TO anon, authenticated;

INSERT INTO auth.users (id, email, raw_user_meta_data) VALUES
  ('00000000-0000-0000-0000-0000000042a1', 'ana42@prueba.ec',
   '{"full_name":"Ana Prueba","username":"ana_prueba"}'::jsonb),
  ('00000000-0000-0000-0000-0000000042b1', 'bruno42@prueba.ec',
   '{"full_name":"Bruno Prueba","username":"bruno_prueba"}'::jsonb),
  ('00000000-0000-0000-0000-0000000042c1', 'admin42@prueba.ec',
   '{"full_name":"Admin Prueba","username":"admin_prueba"}'::jsonb)
ON CONFLICT DO NOTHING;
INSERT INTO public.user_roles (user_id, role)
  VALUES ('00000000-0000-0000-0000-0000000042c1', 'admin')
ON CONFLICT DO NOTHING;

-- Sesion de servicio para poder escribir columnas que el dueno de la tabla no
-- reescribe sin JWT (el trigger no mira current_user).
SELECT set_config('request.jwt.claim.role', 'service_role', true);
SELECT set_config('request.jwt.claims', '{"role":"service_role"}', true);
UPDATE identidad.profiles SET
  bio = 'secreto-bio-ana',
  age = '29',
  gender = 'Mujer',
  location = 'Loja, Ecuador',
  avatar_url = 'https://ejemplo.test/ana.png',
  suspended_at = '2026-11-01T12:00:00Z'
WHERE id = '00000000-0000-0000-0000-0000000042a1';
UPDATE identidad.profiles SET bio = 'secreto-bio-bruno', age = '41', gender = 'Hombre'
WHERE id = '00000000-0000-0000-0000-0000000042b1';

SET LOCAL ROLE anon;
SELECT set_config('request.jwt.claim.sub', '', true);
SELECT set_config('request.jwt.claim.role', '', true);
SELECT set_config('request.jwt.claims', '', true);
DO $$
DECLARE fila jsonb;
BEGIN
  SELECT to_jsonb(p) INTO fila FROM public.profiles p
  WHERE id = '00000000-0000-0000-0000-0000000042a1';
  IF fila IS NULL THEN RAISE EXCEPTION 'anon no ve la ficha publica'; END IF;
  IF fila->>'username' IS DISTINCT FROM 'ana_prueba'
     OR fila->>'full_name' IS DISTINCT FROM 'Ana Prueba'
     OR fila->>'avatar_url' IS DISTINCT FROM 'https://ejemplo.test/ana.png'
     OR fila->>'location' IS DISTINCT FROM 'Loja, Ecuador' THEN
    RAISE EXCEPTION 'anon perdio datos publicos: %', fila;
  END IF;
  IF fila->>'bio' IS NOT NULL OR fila->>'age' IS NOT NULL
     OR fila->>'gender' IS NOT NULL OR fila->>'suspended_at' IS NOT NULL THEN
    RAISE EXCEPTION 'anon ve datos sensibles: %', fila;
  END IF;
  IF fila::text LIKE '%secreto-bio-ana%' OR fila::text LIKE '%Mujer%' THEN
    RAISE EXCEPTION 'el secreto de Ana aparece en la fila anon';
  END IF;
  IF (SELECT count(*) FROM public.profiles
      WHERE id = '00000000-0000-0000-0000-0000000042a1' AND bio = 'secreto-bio-ana') <> 0 THEN
    RAISE EXCEPTION 'anon filtro por la bio';
  END IF;
  BEGIN
    PERFORM bio FROM identidad.profiles;
    RAISE EXCEPTION 'anon leyo bio en la tabla';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
  END;
  BEGIN
    PERFORM suspended_at FROM identidad.profiles;
    RAISE EXCEPTION 'anon leyo suspended_at en la tabla';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
  END;
  IF (SELECT username FROM identidad.profiles
      WHERE id = '00000000-0000-0000-0000-0000000042a1') IS DISTINCT FROM 'ana_prueba' THEN
    RAISE EXCEPTION 'anon no puede leer el username en la tabla';
  END IF;
END $$;

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000042b1', true);
SELECT set_config('request.jwt.claim.role', 'authenticated', true);
SELECT set_config('request.jwt.claims',
  '{"sub":"00000000-0000-0000-0000-0000000042b1","role":"authenticated"}', true);
DO $$
DECLARE pub jsonb; propio jsonb;
BEGIN
  -- Lo que piden mapa, feed, busqueda, clubes y rutas de otra persona.
  SELECT to_jsonb(x) INTO pub FROM (
    SELECT id, full_name, username, avatar_url, location
    FROM public.profiles WHERE id = '00000000-0000-0000-0000-0000000042a1'
  ) x;
  IF pub->>'full_name' IS DISTINCT FROM 'Ana Prueba'
     OR pub->>'username' IS DISTINCT FROM 'ana_prueba'
     OR pub->>'avatar_url' IS DISTINCT FROM 'https://ejemplo.test/ana.png'
     OR pub->>'location' IS DISTINCT FROM 'Loja, Ecuador' THEN
    RAISE EXCEPTION 'la ficha que consumen los clientes cambio: %', pub;
  END IF;
  IF (SELECT bio FROM public.profiles WHERE id = '00000000-0000-0000-0000-0000000042a1') IS NOT NULL
     OR (SELECT age FROM public.profiles WHERE id = '00000000-0000-0000-0000-0000000042a1') IS NOT NULL
     OR (SELECT gender FROM public.profiles WHERE id = '00000000-0000-0000-0000-0000000042a1') IS NOT NULL
     OR (SELECT suspended_at FROM public.profiles WHERE id = '00000000-0000-0000-0000-0000000042a1') IS NOT NULL THEN
    RAISE EXCEPTION 'Bruno ve datos sensibles de Ana';
  END IF;
  IF (SELECT privacy_level::text FROM public.profiles
      WHERE id = '00000000-0000-0000-0000-0000000042a1') IS DISTINCT FROM 'public' THEN
    RAISE EXCEPTION 'privacy_level dejo de verse y corredores_ruta lo necesita';
  END IF;
  SELECT to_jsonb(p) INTO propio FROM public.profiles p
  WHERE id = '00000000-0000-0000-0000-0000000042b1';
  IF propio->>'bio' IS DISTINCT FROM 'secreto-bio-bruno'
     OR propio->>'age' IS DISTINCT FROM '41'
     OR propio->>'gender' IS DISTINCT FROM 'Hombre' THEN
    RAISE EXCEPTION 'Bruno no ve su propio perfil: %', propio;
  END IF;
  UPDATE public.profiles SET bio = 'hack' WHERE id = '00000000-0000-0000-0000-0000000042a1';
  UPDATE public.profiles SET suspended_at = NULL WHERE id = '00000000-0000-0000-0000-0000000042b1';
  BEGIN
    PERFORM age FROM identidad.profiles WHERE id = '00000000-0000-0000-0000-0000000042a1';
    RAISE EXCEPTION 'Bruno leyo age en la tabla';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
  END;
END $$;

RESET ROLE;
DO $$
BEGIN
  IF (SELECT bio FROM identidad.profiles WHERE id = '00000000-0000-0000-0000-0000000042a1')
       IS DISTINCT FROM 'secreto-bio-ana' THEN
    RAISE EXCEPTION 'Bruno pisa la bio de Ana';
  END IF;
  IF (SELECT suspended_at FROM identidad.profiles WHERE id = '00000000-0000-0000-0000-0000000042b1')
       IS NOT NULL THEN
    RAISE EXCEPTION 'Bruno se quito una suspension que no tenia, o escribio la columna';
  END IF;
END $$;

-- Ana sigue suspendida: 043 rechaza la escritura y no toca la ficha ni la suspension.
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000042a1', true);
SELECT set_config('request.jwt.claim.role', 'authenticated', true);
SELECT set_config('request.jwt.claims',
  '{"sub":"00000000-0000-0000-0000-0000000042a1","role":"authenticated"}', true);
DO $$
BEGIN
  UPDATE public.profiles SET
    full_name = 'Ana Editada',
    username = 'ana_editada',
    bio = 'bio-nueva',
    location = 'Vilcabamba',
    age = '30',
    gender = 'Mujer',
    avatar_url = 'https://ejemplo.test/ana2.png',
    suspended_at = NULL
  WHERE id = '00000000-0000-0000-0000-0000000042a1';
  RAISE EXCEPTION 'la cuenta suspendida edito su perfil';
EXCEPTION WHEN insufficient_privilege THEN
  IF SQLERRM NOT LIKE '%cuenta suspendida%' THEN RAISE; END IF;
END $$;

RESET ROLE;
DO $$
BEGIN
  IF (SELECT bio FROM identidad.profiles WHERE id = '00000000-0000-0000-0000-0000000042a1')
       IS DISTINCT FROM 'secreto-bio-ana'
     OR (SELECT full_name FROM identidad.profiles WHERE id = '00000000-0000-0000-0000-0000000042a1')
       IS DISTINCT FROM 'Ana Prueba'
     OR (SELECT suspended_at FROM identidad.profiles WHERE id = '00000000-0000-0000-0000-0000000042a1')
       IS DISTINCT FROM '2026-11-01T12:00:00Z' THEN
    RAISE EXCEPTION 'el update bloqueado igual cambio el perfil';
  END IF;
END $$;

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000042b1', true);
SELECT set_config('request.jwt.claim.role', 'authenticated', true);
SELECT set_config('request.jwt.claims',
  '{"sub":"00000000-0000-0000-0000-0000000042b1","role":"authenticated"}', true);
DO $$
BEGIN
  IF (SELECT full_name FROM public.profiles WHERE id = '00000000-0000-0000-0000-0000000042a1')
       IS DISTINCT FROM 'Ana Prueba'
     OR (SELECT username FROM public.profiles WHERE id = '00000000-0000-0000-0000-0000000042a1')
       IS DISTINCT FROM 'ana_prueba' THEN
    RAISE EXCEPTION 'Bruno dejo de ver la ficha publica';
  END IF;
  IF (SELECT bio FROM public.profiles WHERE id = '00000000-0000-0000-0000-0000000042a1') IS NOT NULL
     OR (SELECT age FROM public.profiles WHERE id = '00000000-0000-0000-0000-0000000042a1') IS NOT NULL
     OR (SELECT suspended_at FROM public.profiles WHERE id = '00000000-0000-0000-0000-0000000042a1') IS NOT NULL THEN
    RAISE EXCEPTION 'Bruno ve datos sensibles de Ana';
  END IF;
END $$;

SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000042c1', true);
SELECT set_config('request.jwt.claim.role', 'authenticated', true);
SELECT set_config('request.jwt.claims',
  '{"sub":"00000000-0000-0000-0000-0000000042c1","role":"authenticated"}', true);
DO $$
DECLARE susp timestamptz;
BEGIN
  IF (SELECT bio FROM public.profiles WHERE id = '00000000-0000-0000-0000-0000000042a1')
       IS DISTINCT FROM 'secreto-bio-ana' THEN
    RAISE EXCEPTION 'el admin no ve la bio';
  END IF;
  IF (SELECT suspended_at FROM public.profiles WHERE id = '00000000-0000-0000-0000-0000000042a1') IS NULL THEN
    RAISE EXCEPTION 'el admin no ve la suspension';
  END IF;
  PERFORM public.admin_suspender('00000000-0000-0000-0000-0000000042b1', true);
  SELECT u.suspended_at INTO susp FROM public.admin_usuarios() u
  WHERE u.id = '00000000-0000-0000-0000-0000000042b1';
  IF susp IS NULL THEN RAISE EXCEPTION 'admin_usuarios no ve la suspension'; END IF;
  PERFORM public.admin_suspender('00000000-0000-0000-0000-0000000042a1', false);
END $$;

SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000042a1', true);
SELECT set_config('request.jwt.claim.role', 'authenticated', true);
SELECT set_config('request.jwt.claims',
  '{"sub":"00000000-0000-0000-0000-0000000042a1","role":"authenticated"}', true);
UPDATE public.profiles SET
  full_name = 'Ana Editada',
  username = 'ana_editada',
  bio = 'bio-nueva',
  location = 'Vilcabamba',
  age = '30',
  gender = 'Mujer',
  avatar_url = 'https://ejemplo.test/ana2.png',
  suspended_at = '2020-01-01T00:00:00Z'
WHERE id = '00000000-0000-0000-0000-0000000042a1';
DO $$
BEGIN
  IF (SELECT suspended_at FROM public.profiles WHERE id = '00000000-0000-0000-0000-0000000042b1') IS NOT NULL THEN
    RAISE EXCEPTION 'Ana ve la suspension de Bruno';
  END IF;
  IF (SELECT bio FROM public.profiles WHERE id = auth.uid()) IS DISTINCT FROM 'bio-nueva'
     OR (SELECT age FROM public.profiles WHERE id = auth.uid()) IS DISTINCT FROM '30'
     OR (SELECT full_name FROM public.profiles WHERE id = auth.uid()) IS DISTINCT FROM 'Ana Editada'
     OR (SELECT username FROM public.profiles WHERE id = auth.uid()) IS DISTINCT FROM 'ana_editada'
     OR (SELECT location FROM public.profiles WHERE id = auth.uid()) IS DISTINCT FROM 'Vilcabamba'
     OR (SELECT avatar_url FROM public.profiles WHERE id = auth.uid())
       IS DISTINCT FROM 'https://ejemplo.test/ana2.png'
     OR (SELECT suspended_at FROM public.profiles WHERE id = auth.uid()) IS NOT NULL THEN
    RAISE EXCEPTION 'el dueno no guardo la ficha, o pudo fijar la suspension';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM pg_class
    WHERE oid = 'public.profiles'::regclass AND relkind = 'v'
      AND reloptions @> ARRAY['security_invoker=true']
  ) THEN
    RAISE EXCEPTION 'la vista dejo de ser security_invoker';
  END IF;
END $$;

SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000042b1', true);
SELECT set_config('request.jwt.claim.role', 'authenticated', true);
SELECT set_config('request.jwt.claims',
  '{"sub":"00000000-0000-0000-0000-0000000042b1","role":"authenticated"}', true);
DO $$
BEGIN
  IF (SELECT full_name FROM public.profiles WHERE id = '00000000-0000-0000-0000-0000000042a1')
       IS DISTINCT FROM 'Ana Editada'
     OR (SELECT location FROM public.profiles WHERE id = '00000000-0000-0000-0000-0000000042a1')
       IS DISTINCT FROM 'Vilcabamba' THEN
    RAISE EXCEPTION 'Bruno no ve la ficha actualizada';
  END IF;
  IF (SELECT bio FROM public.profiles WHERE id = '00000000-0000-0000-0000-0000000042a1') IS NOT NULL
     OR (SELECT age FROM public.profiles WHERE id = '00000000-0000-0000-0000-0000000042a1') IS NOT NULL THEN
    RAISE EXCEPTION 'Bruno ve la bio o la edad nueva de Ana';
  END IF;
END $$;

RESET ROLE;
ROLLBACK;
