-- Prueba de 046: el servidor recalcula distancia y duracion con el track y no
-- deja que una cifra inventada entre en rankings, rachas, logros ni km de club.
-- La subida de las apps (carrera y despues el recorrido) sigue pasando. Todo se revierte.
BEGIN;

DO $$
DECLARE mal text; n int;
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_trigger t
    JOIN pg_class c ON c.oid = t.tgrelid
    WHERE t.tgname = 'zz_verificar_carrera' AND c.relname = 'activities' AND NOT t.tgisinternal
  ) THEN
    RAISE EXCEPTION 'falta el trigger de verificacion en activities';
  END IF;
  -- 044 deja update_activities_updated_at. El orden alfabetico hace que la suspension
  -- corte antes y que la marca de la sync se escriba junto con las cifras verificadas.
  IF (
    SELECT string_agg(t.tgname, '>' ORDER BY t.tgname)
    FROM pg_trigger t
    JOIN pg_class c ON c.oid = t.tgrelid
    WHERE c.relname = 'activities' AND NOT t.tgisinternal
      AND t.tgname IN ('aa_bloquear_suspendido', 'update_activities_updated_at', 'zz_verificar_carrera')
  ) IS DISTINCT FROM 'aa_bloquear_suspendido>update_activities_updated_at>zz_verificar_carrera' THEN
    RAISE EXCEPTION 'orden de triggers de activities inesperado';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM pg_trigger t
    JOIN pg_class c ON c.oid = t.tgrelid
    WHERE t.tgname = 'zz_logro_verificado' AND c.relname = 'user_achievements' AND NOT t.tgisinternal
  ) THEN
    RAISE EXCEPTION 'falta el trigger de logros';
  END IF;

  SELECT string_agg(
    id::text || ' ' || verificacion || ' gps=' || coalesce(metricas_cliente->>'gps_metros', '-')
      || ' m=' || coalesce(distance_meters, 0)::text, ', ')
  INTO mal
  FROM public.activities
  WHERE (id::text LIKE 'da000000-%' OR client_id LIKE 'demo-%')
    AND (NOT verificada OR coalesce(distance_meters, 0) <= 0);
  IF mal IS NOT NULL THEN
    RAISE EXCEPTION 'carreras demo sin verificar: %', mal;
  END IF;
  IF EXISTS (SELECT 1 FROM rutas.routes WHERE id LIKE 'loja-%') THEN
    SELECT count(*) INTO n FROM public.activities
    WHERE id::text LIKE 'da000000-%' AND verificada AND distance_meters > 0;
    IF n < 1 THEN
      RAISE EXCEPTION 'no quedo ninguna carrera demo verificada';
    END IF;
  END IF;
END $$;

GRANT USAGE ON SCHEMA public, auth TO authenticated;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA auth TO authenticated;

INSERT INTO auth.users (id, email) VALUES
  ('00000000-0000-0000-0000-0000000046a1', 'a46@prueba.ec');

SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000046a1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000046a1","role":"authenticated"}', true);

-- Misma forma que filaDesdeCarrera: la carrera llega antes que los puntos,
-- con un km y un desnivel inflados. El insert no falla.
INSERT INTO public.activities (
  user_id, client_id, title, description, is_public, start_time, end_time,
  duration_seconds, distance_meters, avg_pace_seconds_per_km, elevation_gain_meters, splits
) VALUES (
  auth.uid(), 'cli-046', 'Manana', 'nota', true, now(), now() + interval '1650 seconds',
  1650, 100000, 30, 99999, '[]'::jsonb
);

DO $$
DECLARE v boolean; m integer; motivo text;
BEGIN
  SELECT verificada, distance_meters, verificacion INTO v, m, motivo
  FROM public.activities WHERE client_id = 'cli-046';
  IF v THEN RAISE EXCEPTION 'quedo verificada antes del track'; END IF;
  IF m <> 100000 OR motivo <> 'distancia_sin_puntos' THEN
    RAISE EXCEPTION 'antes del track: m=% motivo=%', m, motivo;
  END IF;
END $$;

-- ~100 m por paso, 55 pasos, 30 s: unos 5,5 km a ~3,3 m/s, con desnivel real de 2 m por paso.
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
FROM public.activities a WHERE a.client_id = 'cli-046';

DO $$
DECLARE v boolean; m integer; seg integer; ritmo integer; des integer; motivo text;
BEGIN
  SELECT verificada, distance_meters, duration_seconds, avg_pace_seconds_per_km,
         elevation_gain_meters, verificacion
  INTO v, m, seg, ritmo, des, motivo
  FROM public.activities WHERE client_id = 'cli-046';
  IF NOT v OR motivo <> 'verificada' THEN
    RAISE EXCEPTION 'no verifico el track real: % %', motivo, m;
  END IF;
  IF m < 5000 OR m > 6200 THEN RAISE EXCEPTION 'distancia gps inesperada: %', m; END IF;
  IF seg <> 1650 THEN RAISE EXCEPTION 'duracion gps inesperada: %', seg; END IF;
  IF ritmo < 250 OR ritmo > 360 THEN RAISE EXCEPTION 'ritmo inesperado: %', ritmo; END IF;
  IF des < 80 OR des > 140 THEN RAISE EXCEPTION 'uso el desnivel del cliente: %', des; END IF;
END $$;

-- Un segundo upsert, como la cola offline, no puede volver a inflar las cifras.
INSERT INTO public.activities (
  user_id, client_id, title, start_time, distance_meters, duration_seconds, elevation_gain_meters
) VALUES (
  auth.uid(), 'cli-046', 'Manana', now(), 999999, 10, 99999
)
ON CONFLICT (user_id, client_id) DO UPDATE SET
  distance_meters = EXCLUDED.distance_meters,
  duration_seconds = EXCLUDED.duration_seconds,
  elevation_gain_meters = EXCLUDED.elevation_gain_meters,
  title = EXCLUDED.title;

DO $$
DECLARE m integer; v boolean;
BEGIN
  SELECT distance_meters, verificada INTO m, v FROM public.activities WHERE client_id = 'cli-046';
  IF NOT v OR m < 5000 OR m > 6200 THEN
    RAISE EXCEPTION 'el upsert pisa la distancia verificada: %', m;
  END IF;
END $$;

-- Cifras imposibles sin recorrido: el insert pasa y no cuenta.
INSERT INTO public.activities (
  user_id, client_id, title, start_time, is_public, distance_meters, duration_seconds, verificada
) VALUES (
  auth.uid(), 'falsa-046', 'Falsa', now(), true, 100000, 3600, true
);

INSERT INTO public.activities (
  user_id, client_id, title, start_time, distance_meters, duration_seconds
) VALUES (
  auth.uid(), 'negativa-046', 'Negativa', now(), -20, -30
);

-- Salto de 2 km en 10 s, marcado como bici para intentar saltarse el tope de correr.
INSERT INTO public.activities (
  user_id, client_id, title, start_time, activity_type, is_public, distance_meters, duration_seconds
) VALUES (
  auth.uid(), 'bici-046', 'Bici', now(), 'bike', true, 50000, 600
);
INSERT INTO public.activity_tracks (activity_id, user_id, points)
SELECT a.id, auth.uid(), jsonb_build_array(
  jsonb_build_object('lat', -4.0, 'lng', -79.2, 'tiempoMs', extract(epoch FROM now()) * 1000),
  jsonb_build_object('lat', -4.02, 'lng', -79.2, 'tiempoMs', extract(epoch FROM now()) * 1000 + 10000)
)
FROM public.activities a WHERE a.client_id = 'bici-046';

-- El alta sin metricas (pruebas y humo) sigue entrando.
INSERT INTO public.activities (user_id, title, start_time, is_public, client_id)
VALUES (auth.uid(), 'Vacia', now(), false, 'vacia-046');

DO $$
DECLARE m integer; v boolean; motivo text; reclamo integer;
BEGIN
  SELECT verificada, distance_meters, verificacion,
         (metricas_cliente->>'distance_meters')::integer
  INTO v, m, motivo, reclamo
  FROM public.activities WHERE client_id = 'falsa-046';
  -- La fila guarda lo que mando el cliente; no queda verificada, asi que el feed no la suma.
  IF v OR m <> 100000 OR motivo <> 'distancia_sin_puntos' OR reclamo <> 100000 THEN
    RAISE EXCEPTION 'la falsa quedo mal: v=% m=% motivo=% reclamo=%', v, m, motivo, reclamo;
  END IF;

  SELECT verificacion, distance_meters INTO motivo, m FROM public.activities WHERE client_id = 'negativa-046';
  IF motivo <> 'imposible' OR m <> 0 THEN
    RAISE EXCEPTION 'la negativa no quedo imposible: % %', motivo, m;
  END IF;

  SELECT verificada, distance_meters, verificacion INTO v, m, motivo
  FROM public.activities WHERE client_id = 'bici-046';
  IF v OR m <> 0 THEN
    RAISE EXCEPTION 'el salto en bici quedo verificado: % % %', v, m, motivo;
  END IF;

  SELECT verificacion INTO motivo FROM public.activities WHERE client_id = 'vacia-046';
  IF motivo <> 'pendiente' THEN RAISE EXCEPTION 'la vacia no quedo pendiente: %', motivo; END IF;
END $$;

-- Logros: el de 5 km si, el de 10 km no. La primera carrera la concede el servidor.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM public.user_achievements
    WHERE user_id = auth.uid() AND code IN ('primera_carrera', 'km_5')
    HAVING count(*) = 2
  ) THEN
    RAISE EXCEPTION 'no se concedieron los logros del track real';
  END IF;
  BEGIN
    INSERT INTO public.user_achievements (user_id, code) VALUES (auth.uid(), 'km_10');
    RAISE EXCEPTION 'acepto el logro de 10 km sin carrera';
  EXCEPTION WHEN insufficient_privilege THEN
    IF SQLERRM NOT LIKE '%logro no verificado%' THEN
      RAISE EXCEPTION 'error distinto al rechazar el logro: %', SQLERRM;
    END IF;
  END;
END $$;

-- Rachas: la falsa de ayer no alarga la racha.
INSERT INTO public.activities (
  user_id, client_id, title, start_time, distance_meters, duration_seconds
) VALUES (
  auth.uid(), 'ayer-046', 'Ayer', now() - interval '1 day', 40000, 3600
);

DO $$
DECLARE diaria integer; semana integer;
BEGIN
  SELECT current_daily, runs_this_week INTO diaria, semana
  FROM public.user_streaks WHERE user_id = auth.uid();
  IF diaria <> 1 OR semana <> 1 THEN
    RAISE EXCEPTION 'la racha conto carreras no verificadas: diaria=% semana=%', diaria, semana;
  END IF;
END $$;

INSERT INTO public.clubs (name, owner_id) VALUES ('Club metricas 046', auth.uid());

DO $$
DECLARE km numeric; feed numeric; autor numeric; metros numeric;
BEGIN
  SELECT coalesce(sum(g.km), 0) INTO km
  FROM public.km_grupos(1) g
  JOIN public.clubs c ON c.id = g.club_id
  WHERE c.name = 'Club metricas 046';
  IF km < 5 OR km > 7 THEN
    RAISE EXCEPTION 'km del club inesperados: %', km;
  END IF;

  SELECT f.distance_meters, f.autor_km INTO feed, autor
  FROM public.feed_comunidad(100) f WHERE f.client_id = 'falsa-046';
  IF feed IS NULL OR feed <> 0 OR autor IS NULL OR autor > 7 THEN
    RAISE EXCEPTION 'el feed publico la falsa: distancia=% autor_km=%', feed, autor;
  END IF;

  SELECT f.distance_meters INTO feed FROM public.feed_comunidad(100) f WHERE f.client_id = 'cli-046';
  IF feed IS NULL OR feed < 5000 THEN RAISE EXCEPTION 'el feed oculto la carrera real: %', feed; END IF;

  -- El reporte personal es del dueno y conserva la cifra que envio. El feed, no.
  SELECT (public.reporte_personal('semana')->'actual'->>'metros')::numeric INTO metros;
  IF metros IS NULL OR metros < 100000 THEN
    RAISE EXCEPTION 'el reporte personal perdio la cifra del dueno: %', metros;
  END IF;
END $$;

-- La sync incremental (044) baja activities con updated_at posterior a la marca.
-- Si el recalculo no mueve esa marca, el otro dispositivo se queda con el km inflado.
RESET ROLE;
ALTER TABLE public.activities DISABLE TRIGGER update_activities_updated_at;
UPDATE public.activities SET updated_at = now() - interval '1 day' WHERE client_id = 'cli-046';
ALTER TABLE public.activities ENABLE TRIGGER update_activities_updated_at;
UPDATE public.activity_tracks SET points = points
WHERE activity_id = (SELECT id FROM public.activities WHERE client_id = 'cli-046');

DO $$
DECLARE marca timestamptz; m integer; v boolean;
BEGIN
  SELECT updated_at, distance_meters, verificada INTO marca, m, v
  FROM public.activities WHERE client_id = 'cli-046';
  IF NOT v OR m < 5000 OR m > 6200 THEN
    RAISE EXCEPTION 'el recalculo perdio la distancia verificada: % %', v, m;
  END IF;
  IF marca <= now() - interval '12 hours' THEN
    RAISE EXCEPTION 'el recalculo no movio updated_at (%): la sync no bajaria las cifras', marca;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM public.activities
    WHERE client_id = 'cli-046'
      AND updated_at > now() - interval '12 hours'
      AND verificada
      AND distance_meters > 5000
  ) THEN
    RAISE EXCEPTION 'una sync posterior a la marca no veria la carrera verificada';
  END IF;
END $$;

-- Un like no debe borrar la verificacion.
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000046a1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000046a1","role":"authenticated"}', true);
UPDATE public.activities SET likes_count = likes_count + 1 WHERE client_id = 'cli-046' AND user_id = auth.uid();

DO $$
DECLARE v boolean; m integer;
BEGIN
  SELECT verificada, distance_meters INTO v, m FROM public.activities WHERE client_id = 'cli-046';
  IF NOT v OR m < 5000 THEN RAISE EXCEPTION 'un like borro la verificacion'; END IF;
END $$;

ROLLBACK;
