-- Prueba de 035: ruta sugerida para hoy. Segura, estable en el dia, sin repetir en 3 dias,
-- historial solo legible por su dueno y sin escritura directa. Todo se revierte al final.
BEGIN;
GRANT USAGE ON SCHEMA public, auth TO authenticated;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA auth TO authenticated;
INSERT INTO auth.users (id, email) VALUES
  ('00000000-0000-0000-0000-0000000035a1', 'a35@prueba.ec'),
  ('00000000-0000-0000-0000-0000000035b1', 'b35@prueba.ec')
ON CONFLICT DO NOTHING;
-- Tres rutas lejos del catalogo real: U insegura de 3 km, S1 segura de 3 km, S2 segura de 3.1 km.
INSERT INTO rutas.routes (id, name, distance_km, difficulty, path, is_public) VALUES
  ('p35-u', 'Prueba 035 U', 3.0, 'Fácil', '[{"lat":10.0,"lng":10.0},{"lat":10.01,"lng":10.0}]', true),
  ('p35-s1', 'Prueba 035 S1', 3.0, 'Fácil', '[{"lat":10.0,"lng":10.0},{"lat":10.0,"lng":10.01}]', true),
  ('p35-s2', 'Prueba 035 S2', 3.1, 'Fácil', '[{"lat":10.0,"lng":10.0},{"lat":9.99,"lng":10.0}]', true);
-- Un asalto verificado ahora en medio de U la vuelve insegura para esta hora (indice 1.5).
INSERT INTO public.incidentes_usuario (user_id, tipo, geom, estado)
  VALUES ('00000000-0000-0000-0000-0000000035b1', 'robo_asalto',
          ST_SetSRID(ST_MakePoint(10.0, 10.005), 4326), 'verificado');
-- A corre 3 km de media (hace 10 dias, fuera de la ventana).
INSERT INTO public.activities (user_id, title, start_time, distance_meters)
  SELECT '00000000-0000-0000-0000-0000000035a1', 'Prueba', now() - interval '10 days', 3000
  FROM generate_series(1, 5);

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000035a1","role":"authenticated"}', true);
DO $$
DECLARE r JSONB; r2 JSONB;
BEGIN
  r := public.ruta_sugerida_hoy(10.0, 10.0);
  IF r->>'estado' <> 'ok' THEN RAISE EXCEPTION 'Sin sugerencia: %', r; END IF;
  IF r->'ruta'->>'id' = 'p35-u' THEN RAISE EXCEPTION 'Sugirio una ruta insegura'; END IF;
  IF r->'ruta'->>'id' <> 'p35-s1' THEN RAISE EXCEPTION 'Esperaba S1, salio %', r->'ruta'->>'id'; END IF;
  IF (r->>'objetivo_km')::numeric <> 3 THEN RAISE EXCEPTION 'Objetivo mal calculado: %', r->>'objetivo_km'; END IF;
  r2 := public.ruta_sugerida_hoy(10.0, 10.0);
  IF r2->'ruta'->>'id' <> 'p35-s1' OR (r2->>'nueva')::boolean THEN RAISE EXCEPTION 'No es estable en el dia'; END IF;
  IF (SELECT count(*) FROM public.route_suggestions) <> 1 THEN RAISE EXCEPTION 'Registro duplicado'; END IF;
  BEGIN
    INSERT INTO public.route_suggestions (user_id, dia, route_id, nivel, indice, objetivo_km)
      VALUES (auth.uid(), current_date + 1, 'p35-s2', 'bajo', 0.5, 3);
    RAISE EXCEPTION 'Escritura directa aceptada';
  EXCEPTION WHEN insufficient_privilege THEN NULL; END;
END $$;

-- B no ve el historial de A.
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000035b1","role":"authenticated"}', true);
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM public.route_suggestions WHERE user_id = '00000000-0000-0000-0000-0000000035a1')
  THEN RAISE EXCEPTION 'B ve sugerencias de A'; END IF;
END $$;

-- Al dia siguiente S1 (sugerida ayer) no se repite.
RESET ROLE;
UPDATE rutas.route_suggestions SET dia = dia - 1 WHERE user_id = '00000000-0000-0000-0000-0000000035a1';
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000035a1","role":"authenticated"}', true);
DO $$ DECLARE r JSONB; BEGIN
  r := public.ruta_sugerida_hoy(10.0, 10.0);
  IF r->'ruta'->>'id' IN ('p35-s1', 'p35-u') THEN RAISE EXCEPTION 'Repitio o inseguro: %', r->'ruta'->>'id'; END IF;
END $$;

-- Una carrera de hace 2 dias que coincide geograficamente con S2 tambien la excluye.
RESET ROLE;
DELETE FROM rutas.route_suggestions WHERE user_id = '00000000-0000-0000-0000-0000000035a1';
INSERT INTO public.activities (user_id, title, start_time, distance_meters, started_location)
  VALUES ('00000000-0000-0000-0000-0000000035a1', 'Prueba S2', now() - interval '2 days', 3100,
          ST_SetSRID(ST_MakePoint(10.0001, 10.0001), 4326)::geography);
SET LOCAL ROLE authenticated;
DO $$ DECLARE r JSONB; BEGIN
  r := public.ruta_sugerida_hoy(10.0, 10.0);
  IF r->'ruta'->>'id' IN ('p35-s1', 'p35-s2', 'p35-u') THEN
    RAISE EXCEPTION 'No excluyo la corrida por coincidencia: %', r->'ruta'->>'id'; END IF;
END $$;

-- Hace 4 dias ya no cuenta: S1 vuelve a ser elegible.
RESET ROLE;
DELETE FROM public.activities WHERE title = 'Prueba S2' AND user_id = '00000000-0000-0000-0000-0000000035a1';
UPDATE rutas.route_suggestions SET dia = dia - 4 WHERE user_id = '00000000-0000-0000-0000-0000000035a1';
SET LOCAL ROLE authenticated;
DO $$ DECLARE r JSONB; BEGIN
  r := public.ruta_sugerida_hoy(10.0, 10.0);
  IF r->'ruta'->>'id' <> 'p35-s1' THEN RAISE EXCEPTION 'La ventana de 3 dias no expira: %', r->'ruta'->>'id'; END IF;
END $$;

SELECT 'ok 035_ruta_sugerida' AS resultado;
ROLLBACK;
