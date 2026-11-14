-- Prueba de 041: el mapa en vivo no entrega la coordenada exacta ni la zona de casa,
-- y la API no puede leer el ancla ni escribir runner_positions directo.
-- Cualquier sesion iniciada sigue viendo a quien opto por ubicacion visible.
BEGIN;
GRANT USAGE ON SCHEMA public, auth, envivo TO anon, authenticated;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA auth TO anon, authenticated;
GRANT SELECT, DELETE ON TABLE public.runner_positions TO anon, authenticated;

INSERT INTO auth.users (id, email) VALUES
  ('00000000-0000-0000-0000-0000000041a1', 'a41@prueba.ec'),
  ('00000000-0000-0000-0000-0000000041b1', 'b41@prueba.ec'),
  ('00000000-0000-0000-0000-0000000041c1', 'c41@prueba.ec'),
  ('00000000-0000-0000-0000-0000000041d1', 'd41@prueba.ec')
ON CONFLICT DO NOTHING;
UPDATE identidad.user_settings SET hide_home = false, home_radius_m = 300
WHERE user_id = '00000000-0000-0000-0000-0000000041c1';
INSERT INTO legal.consents (user_id, kind, version, granted) VALUES
  ('00000000-0000-0000-0000-0000000041a1', 'ubicacion_visible', '2026-10-25', true),
  ('00000000-0000-0000-0000-0000000041b1', 'ubicacion_visible', '2026-10-25', true),
  ('00000000-0000-0000-0000-0000000041c1', 'ubicacion_visible', '2026-10-25', true);

DO $$ BEGIN
  IF has_table_privilege('authenticated', 'public.runner_positions', 'INSERT')
     OR has_table_privilege('authenticated', 'public.runner_positions', 'UPDATE') THEN
    RAISE EXCEPTION 'authenticated puede escribir runner_positions';
  END IF;
  IF NOT has_table_privilege('authenticated', 'public.runner_positions', 'SELECT')
     OR NOT has_table_privilege('authenticated', 'public.runner_positions', 'DELETE') THEN
    RAISE EXCEPTION 'falta SELECT o DELETE para dejar de compartir y leer el mapa';
  END IF;
  IF has_table_privilege('authenticated', 'envivo.mapa_ancla', 'SELECT')
     OR has_table_privilege('anon', 'envivo.mapa_ancla', 'SELECT') THEN
    RAISE EXCEPTION 'el ancla de casa es legible por la API';
  END IF;
  IF has_function_privilege('anon', 'public.mapa_publicar(double precision, double precision, boolean)', 'EXECUTE') THEN
    RAISE EXCEPTION 'anon puede publicar';
  END IF;
  IF NOT has_function_privilege('authenticated', 'public.mapa_publicar(double precision, double precision, boolean)', 'EXECUTE') THEN
    RAISE EXCEPTION 'authenticated no puede publicar';
  END IF;
END $$;

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000041a1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000041a1","role":"authenticated"}', true);

DO $$
DECLARE
  casa_lat DOUBLE PRECISION := -4.0;
  casa_lng DOUBLE PRECISION := -79.2;
  cerca_lat DOUBLE PRECISION := -3.9992;
  cerca_lng DOUBLE PRECISION := -79.2003;
  lejos_lat DOUBLE PRECISION := -3.980123;
  lejos_lng DOUBLE PRECISION := -79.220456;
  d_cerca DOUBLE PRECISION;
  d_lejos DOUBLE PRECISION;
  publicado BOOLEAN;
  v_lat DOUBLE PRECISION;
  v_lng DOUBLE PRECISION;
BEGIN
  d_cerca := ST_Distance(
    ST_SetSRID(ST_MakePoint(cerca_lng, cerca_lat), 4326)::geography,
    ST_SetSRID(ST_MakePoint(casa_lng, casa_lat), 4326)::geography);
  d_lejos := ST_Distance(
    ST_SetSRID(ST_MakePoint(lejos_lng, lejos_lat), 4326)::geography,
    ST_SetSRID(ST_MakePoint(casa_lng, casa_lat), 4326)::geography);
  IF d_cerca > 300 OR d_lejos <= 300 THEN
    RAISE EXCEPTION 'puntos de prueba mal elegidos (cerca % m, lejos % m)', d_cerca, d_lejos;
  END IF;

  publicado := public.mapa_publicar(casa_lat, casa_lng, false);
  IF NOT publicado THEN RAISE EXCEPTION 'en casa debio aceptarse y omitirse'; END IF;
  IF EXISTS (SELECT 1 FROM public.runner_positions WHERE user_id = auth.uid()) THEN
    RAISE EXCEPTION 'la zona de casa quedo visible';
  END IF;

  publicado := public.mapa_publicar(cerca_lat, cerca_lng, true);
  IF NOT publicado OR EXISTS (SELECT 1 FROM public.runner_positions WHERE user_id = auth.uid()) THEN
    RAISE EXCEPTION 'un punto dentro del radio se publico';
  END IF;

  publicado := public.mapa_publicar(lejos_lat, lejos_lng, true);
  IF NOT publicado THEN RAISE EXCEPTION 'fuera de casa debio publicarse'; END IF;
  SELECT lat, lng INTO v_lat, v_lng FROM public.runner_positions WHERE user_id = auth.uid();
  IF v_lat IS NULL THEN RAISE EXCEPTION 'no hay marcador fuera de casa'; END IF;
  IF abs(v_lat - lejos_lat) < 1e-9 AND abs(v_lng - lejos_lng) < 1e-9 THEN
    RAISE EXCEPTION 'se guardo la coordenada exacta';
  END IF;
  IF abs(v_lat - round(lejos_lat::numeric, 3)::float8) > 1e-8
     OR abs(v_lng - round(lejos_lng::numeric, 3)::float8) > 1e-8 THEN
    RAISE EXCEPTION 'no quedo en la rejilla de 3 decimales: % %', v_lat, v_lng;
  END IF;

  BEGIN
    INSERT INTO public.runner_positions (user_id, lat, lng, running)
    VALUES (auth.uid(), lejos_lat, lejos_lng, true);
    RAISE EXCEPTION 'insert directo aceptado';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
  END;
  BEGIN
    UPDATE public.runner_positions SET lat = lejos_lat WHERE user_id = auth.uid();
    RAISE EXCEPTION 'update directo aceptado';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
  END;
  BEGIN
    PERFORM 1 FROM envivo.mapa_ancla;
    RAISE EXCEPTION 'el corredor leyo su ancla';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
  END;
  BEGIN
    PERFORM public.mapa_publicar(999, 0, true);
    RAISE EXCEPTION 'coordenada invalida aceptada';
  EXCEPTION WHEN raise_exception THEN
    IF SQLERRM <> 'coordenadas invalidas' THEN RAISE; END IF;
  END;
END $$;

-- Otro usuario autenticado ve el marcador difuminado, no el punto exacto ni el ancla.
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000041b1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000041b1","role":"authenticated"}', true);
DO $$
DECLARE v_lat DOUBLE PRECISION;
BEGIN
  SELECT lat INTO v_lat FROM public.runner_positions
  WHERE user_id = '00000000-0000-0000-0000-0000000041a1';
  IF v_lat IS NULL THEN RAISE EXCEPTION 'otro corredor no ve el mapa'; END IF;
  IF abs(v_lat - (-3.980123)) < 1e-9 THEN RAISE EXCEPTION 'otro corredor ve la latitud exacta'; END IF;
  IF (SELECT count(*) FROM public.runner_positions) < 1 THEN RAISE EXCEPTION 'el mapa quedo vacio'; END IF;
  BEGIN
    DELETE FROM public.runner_positions WHERE user_id = '00000000-0000-0000-0000-0000000041a1';
    IF NOT FOUND THEN NULL; END IF;
  END;
  IF NOT EXISTS (SELECT 1 FROM public.runner_positions WHERE user_id = '00000000-0000-0000-0000-0000000041a1') THEN
    RAISE EXCEPTION 'B borro la posicion de A';
  END IF;
  BEGIN
    PERFORM 1 FROM envivo.mapa_ancla;
    RAISE EXCEPTION 'B leyo anclas';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
  END;
END $$;

-- Sin sesion no hay filas, aunque la tabla tenga posiciones frescas.
-- Hay que vaciar el JWT: SET ROLE anon no lo borra, y auth.uid() lo seguiria leyendo.
RESET ROLE;
SELECT set_config('request.jwt.claim.sub', '', true);
SELECT set_config('request.jwt.claims', '', true);
SET LOCAL ROLE anon;
DO $$ BEGIN
  IF (SELECT count(*) FROM public.runner_positions) <> 0 THEN
    RAISE EXCEPTION 'anon ve el mapa en vivo';
  END IF;
END $$;

-- Sin consentimiento no se publica. Con ocultar casa desactivado se ve, pero redondeado.
RESET ROLE;
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000041d1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000041d1","role":"authenticated"}', true);
DO $$ BEGIN
  IF public.mapa_publicar(-4.11, -79.22, true) THEN
    RAISE EXCEPTION 'publico sin consentimiento';
  END IF;
  IF EXISTS (SELECT 1 FROM public.runner_positions WHERE user_id = auth.uid()) THEN
    RAISE EXCEPTION 'quedo una fila sin consentimiento';
  END IF;
END $$;

SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000041c1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000041c1","role":"authenticated"}', true);
DO $$
DECLARE v_lat DOUBLE PRECISION; v_lng DOUBLE PRECISION;
BEGIN
  IF NOT public.mapa_publicar(-4.11119, -79.22228, false) THEN
    RAISE EXCEPTION 'con ocultar casa desactivado debio publicarse en el sitio';
  END IF;
  SELECT lat, lng INTO v_lat, v_lng FROM public.runner_positions WHERE user_id = auth.uid();
  IF abs(v_lat - (-4.11119)) < 1e-9 AND abs(v_lng - (-79.22228)) < 1e-9 THEN
    RAISE EXCEPTION 'sin ocultar casa se guardo el punto exacto';
  END IF;
  IF abs(v_lat - round((-4.11119)::numeric, 3)::float8) > 1e-8
     OR abs(v_lng - round((-79.22228)::numeric, 3)::float8) > 1e-8 THEN
    RAISE EXCEPTION 'sin ocultar casa no se redondeo: % %', v_lat, v_lng;
  END IF;
END $$;

-- El radio configurado se respeta: 5 km vuelve a ocultar el punto lejano.
RESET ROLE;
UPDATE identidad.user_settings SET home_radius_m = 5000
WHERE user_id = '00000000-0000-0000-0000-0000000041a1';
UPDATE public.runner_positions SET updated_at = now() - interval '11 seconds'
WHERE user_id = '00000000-0000-0000-0000-0000000041a1';
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000041a1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000041a1","role":"authenticated"}', true);
DO $$ BEGIN
  IF NOT public.mapa_publicar(-3.980123, -79.220456, true) THEN
    RAISE EXCEPTION 'el radio amplio debio aceptarse';
  END IF;
  IF EXISTS (SELECT 1 FROM public.runner_positions WHERE user_id = auth.uid()) THEN
    RAISE EXCEPTION 'un radio de 5 km no oculto el punto';
  END IF;
END $$;

-- Tras 30 min sin publicar, el proximo punto es un inicio nuevo y vuelve a ocultarse.
RESET ROLE;
UPDATE identidad.user_settings SET home_radius_m = 300, hide_home = true
WHERE user_id = '00000000-0000-0000-0000-0000000041a1';
UPDATE envivo.mapa_ancla SET updated_at = now() - interval '31 minutes'
WHERE user_id = '00000000-0000-0000-0000-0000000041a1';
SET LOCAL ROLE authenticated;
DO $$ BEGIN
  PERFORM public.mapa_publicar(-3.97, -79.19, true);
  IF EXISTS (SELECT 1 FROM public.runner_positions WHERE user_id = auth.uid()) THEN
    RAISE EXCEPTION 'el inicio de una salida nueva quedo visible';
  END IF;
END $$;

-- Revocar el consentimiento borra la posicion publica y el ancla.
RESET ROLE;
DO $$ BEGIN
  IF (SELECT lat FROM envivo.mapa_ancla WHERE user_id = '00000000-0000-0000-0000-0000000041a1') IS NULL THEN
    RAISE EXCEPTION 'no se guardo el inicio exacto en el ancla';
  END IF;
  IF abs((SELECT lat FROM envivo.mapa_ancla WHERE user_id = '00000000-0000-0000-0000-0000000041a1') - (-3.97)) > 1e-6 THEN
    RAISE EXCEPTION 'el ancla no se renovo con la salida nueva';
  END IF;
END $$;
INSERT INTO legal.consents (user_id, kind, version, granted)
VALUES ('00000000-0000-0000-0000-0000000041c1', 'ubicacion_visible', '2026-10-25', false);
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM public.runner_positions WHERE user_id = '00000000-0000-0000-0000-0000000041c1') THEN
    RAISE EXCEPTION 'revocar no borro la posicion';
  END IF;
  IF EXISTS (SELECT 1 FROM envivo.mapa_ancla WHERE user_id = '00000000-0000-0000-0000-0000000041c1') THEN
    RAISE EXCEPTION 'revocar no borro el ancla';
  END IF;
END $$;

-- Dejar de compartir sigue siendo el DELETE de la fila propia.
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000041b1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000041b1","role":"authenticated"}', true);
DO $$ BEGIN
  PERFORM public.mapa_publicar(-3.96, -79.18, true);
END $$;
RESET ROLE;
UPDATE identidad.user_settings SET hide_home = false WHERE user_id = '00000000-0000-0000-0000-0000000041b1';
SET LOCAL ROLE authenticated;
DO $$ BEGIN
  IF NOT public.mapa_publicar(-3.96111, -79.18111, false) THEN
    RAISE EXCEPTION 'B no pudo publicarse';
  END IF;
  DELETE FROM public.runner_positions WHERE user_id = auth.uid();
  IF EXISTS (SELECT 1 FROM public.runner_positions WHERE user_id = auth.uid()) THEN
    RAISE EXCEPTION 'no pudo dejar de compartir';
  END IF;
END $$;

SELECT 'ok 041_mapa_vivo_privacidad' AS resultado;
ROLLBACK;
