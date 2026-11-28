-- Prueba de 049: un solo catalogo de logros, con la medalla antigua de cada uno, y el servidor
-- valida los 26 codigos con carreras verificadas. Todo se revierte al final.
BEGIN;

DO $$
DECLARE mapa text; n int;
BEGIN
  SELECT count(*) INTO n FROM logros.catalogo_logros;
  IF n <> 26 THEN RAISE EXCEPTION 'el catalogo deberia tener 26 logros y tiene %', n; END IF;

  -- Las 12 medallas antiguas, cada una en un solo logro.
  SELECT string_agg(medalla_origen || '>' || code, ', ' ORDER BY medalla_origen) INTO mapa
  FROM logros.catalogo_logros WHERE medalla_origen IS NOT NULL;
  IF mapa IS DISTINCT FROM
     '1000k>total_1000, 10k>km_10, 21k>km_21, 42k>km_42, 500k>total_500, 5k>km_5, centenario>total_100, desnivel1000>desnivel_1000, desnivel5000>desnivel_5000, primera>primera_carrera, racha30>racha_30, racha7>racha_7'
  THEN
    RAISE EXCEPTION 'mapeo medalla-logro inesperado: %', mapa;
  END IF;

  -- Sin duplicados: ningun titulo ni descripcion repetidos.
  SELECT count(*) INTO n FROM (SELECT titulo FROM logros.catalogo_logros GROUP BY titulo HAVING count(*) > 1) d;
  IF n <> 0 THEN RAISE EXCEPTION 'titulos repetidos en el catalogo'; END IF;

  -- Los 20 codigos de 048 siguen en el catalogo y el CHECK escrito a mano ya no existe.
  SELECT count(*) INTO n FROM logros.catalogo_logros WHERE code IN (
    'primera_carrera', 'km_5', 'km_10', 'racha_7', 'km_15', 'km_21', 'total_50', 'racha_3', 'racha_30',
    'carreras_10', 'carreras_50', 'semanas_meta_4', 'ritmo_6', 'ritmo_5', 'ritmo_430', 'escalador',
    'explorador', 'madrugador', 'nocturno', 'fin_de_semana');
  IF n <> 20 THEN RAISE EXCEPTION 'faltan codigos de 048 en el catalogo: %', n; END IF;
  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'user_achievements_code_check'
             AND conrelid = 'logros.user_achievements'::regclass) THEN
    RAISE EXCEPTION 'sigue el CHECK de codigos';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'user_achievements_code_fkey'
                 AND conrelid = 'logros.user_achievements'::regclass) THEN
    RAISE EXCEPTION 'falta la clave foranea al catalogo';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_trigger t WHERE t.tgrelid = 'logros.catalogo_logros'::regclass
                 AND t.tgname = 'aa_bloquear_suspendido' AND NOT t.tgisinternal) THEN
    RAISE EXCEPTION 'el catalogo no tiene el trigger de suspension';
  END IF;
END $$;

GRANT USAGE ON SCHEMA public, auth TO authenticated;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA auth TO authenticated;

INSERT INTO auth.users (id, email) VALUES
  ('00000000-0000-0000-0000-0000000049a1', 'a49@prueba.ec');

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000049a1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000049a1","role":"authenticated"}', true);

-- El catalogo se lee por public y no se escribe.
DO $$
DECLARE n int;
BEGIN
  SELECT count(*) INTO n FROM public.catalogo_logros;
  IF n <> 26 THEN RAISE EXCEPTION 'un usuario ve % logros del catalogo, no 26', n; END IF;
  BEGIN
    INSERT INTO public.catalogo_logros (code, titulo, descripcion, como_conseguirlo, categoria, meta, orden)
    VALUES ('falso', 'Falso', 'x', 'x', 'distancia', 1, 99);
    RAISE EXCEPTION 'un usuario pudo escribir en el catalogo';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
  END;
END $$;

-- Mismo recorrido que la prueba de 046: unos 5,5 km a unos 300 s/km, verificados.
INSERT INTO public.activities (
  user_id, client_id, title, is_public, start_time, end_time,
  duration_seconds, distance_meters, avg_pace_seconds_per_km, elevation_gain_meters, splits
) VALUES (
  auth.uid(), 'cli-049', 'Manana', true, now(), now() + interval '1650 seconds',
  1650, 5500, 300, 100, '[]'::jsonb
);
INSERT INTO public.activity_tracks (activity_id, user_id, points)
SELECT a.id, auth.uid(), (
  SELECT jsonb_agg(jsonb_build_object(
    'lat', -4.0 - i * 0.0009,
    'lng', -79.2,
    'tiempoMs', (extract(epoch FROM now()) * 1000 + i * 30000),
    'precisionM', 5,
    'altitudM', 2100 + i * 2
  ) ORDER BY i)
  FROM generate_series(0, 55) AS i
)
FROM public.activities a WHERE a.client_id = 'cli-049';

DO $$
BEGIN
  -- Concedidos por el servidor: antes de 049 los codigos nuevos nunca se concedian.
  IF NOT EXISTS (
    SELECT 1 FROM public.user_achievements
    WHERE user_id = auth.uid() AND code IN ('primera_carrera', 'km_5', 'ritmo_6')
    HAVING count(*) = 3
  ) THEN
    RAISE EXCEPTION 'no se concedieron primera_carrera, km_5 y ritmo_6';
  END IF;
  IF EXISTS (
    SELECT 1 FROM public.user_achievements
    WHERE user_id = auth.uid() AND code IN ('km_10', 'km_15', 'km_42', 'total_100', 'racha_7')
  ) THEN
    RAISE EXCEPTION 'se concedio un logro que la carrera no respalda';
  END IF;
  -- Uno que si respalda se puede subir desde la app (sube el lote entero).
  INSERT INTO public.user_achievements (user_id, code) VALUES (auth.uid(), 'ritmo_6')
  ON CONFLICT DO NOTHING;
  -- Los que no respalda siguen rechazados, tambien los codigos nuevos.
  BEGIN
    INSERT INTO public.user_achievements (user_id, code) VALUES (auth.uid(), 'km_42');
    RAISE EXCEPTION 'acepto el logro km_42 sin carrera';
  EXCEPTION WHEN insufficient_privilege THEN
    IF SQLERRM NOT LIKE '%logro no verificado%' THEN
      RAISE EXCEPTION 'error distinto al rechazar km_42: %', SQLERRM;
    END IF;
  END;
  BEGIN
    INSERT INTO public.user_achievements (user_id, code) VALUES (auth.uid(), 'codigo_inexistente');
    RAISE EXCEPTION 'acepto un codigo fuera del catalogo';
  EXCEPTION WHEN insufficient_privilege THEN NULL;
  END;
END $$;

RESET ROLE;
ROLLBACK;
