-- 046: las metricas de una carrera (distancia, duracion, ritmo, desnivel) dejan de
-- confiarse tal cual las manda el cliente. El recorrido en activity_tracks es la
-- prueba: PostGIS recalcula la distancia y, si hay marcas de tiempo, la duracion.
--
-- Umbrales (los mismos para run, walk, hike, bike y other: el tipo lo elige quien
-- sube la carrera y los rankings no separan por tipo):
--   * Un tramo mas rapido que 12 m/s (43,2 km/h) no suma. Es el tope que ya usa la
--     app para descartar saltos de GPS. El récord de 100 m ronda esa velocidad y
--     no se sostiene en una carrera.
--   * Media maxima de 8 m/s (28,8 km/h, ritmo 2:05/km) si la distancia filtrada
--     llega a 200 m. El récord de 800 m esta en ~7,9 m/s y el de maraton en ~5,7.
--     Por debajo de 200 m se admite hasta 12 m/s (un sprint corto).
--   * Hace falta al menos 100 m filtrados, duracion entre 1 s y 36 h, y dos puntos
--     con tiempo creciente (tiempoMs o t, en milisegundos, como envian web y Android).
--     Sin track la fila se guarda con las cifras del cliente y verificada = false:
--     el historial y el reporte personal las siguen viendo, pero el feed, los km
--     de club y las rachas no. Si el track existe y no da una carrera creible, las
--     cifras publicas quedan en 0.
--   * Desnivel: suma de subidas de altitudM (cada paso, a lo sumo 40 m) o, si el
--     track no trae altitud, el valor del cliente recortado a 0,35 m de desnivel
--     por metro y a 8000 m.
--   * Mas de 20000 puntos: no se verifica (recorrido_excesivo), para no calcular
--     un track enorme dentro del trigger.
--
-- La subida actual no cambia de forma: primero activities (con las cifras del
-- telefono) y despues activity_tracks. El INSERT no se rechaza. Hasta que llega
-- el track la fila queda no verificada; al guardarlo se recalcula. Un segundo
-- upsert con cifras infladas no pisa el resultado. Las calorias no estan en la
-- tabla: la app las estima a partir de los km, asi que siguen a la distancia ya
-- corregida. Los parciales (splits) se dejan como los mando el cliente.
--
-- Historicas: al aplicar, cada carrera con track se recalcula. Las de demostracion
-- (client_id demo-% o id da000000-%) no tienen track; se reconstruye uno repitiendo
-- public_points las vueltas que hagan falta, con tiempos repartidos en la duracion
-- que ya tenian, y se les devuelve el trazado publico. El resto, sin puntos, queda
-- no verificada (no entra en rankings ni en logros) y conserva la cifra original.
-- Lo que mando el cliente queda en metricas_cliente.
--
-- 044 lo usa el PR de sync incremental. Reversion: supabase/rollback/046_metricas_carrera.down.sql.

BEGIN;

-- ---------------------------------------------------------------------------
-- Columnas. La tabla puede estar en public (realtime) o en actividad (CI sin
-- publicacion). La vista del otro esquema se recrea para ver las columnas.
-- ---------------------------------------------------------------------------
DO $mig$
DECLARE
  act regclass;
  esquema text;
BEGIN
  SELECT n.nspname, c.oid::regclass INTO esquema, act
  FROM pg_class c
  JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE c.relname = 'activities' AND c.relkind = 'r'
    AND n.nspname IN ('public', 'actividad');
  IF act IS NULL THEN
    RAISE EXCEPTION 'no esta la tabla activities';
  END IF;

  EXECUTE format('ALTER TABLE %s ADD COLUMN IF NOT EXISTS verificada boolean NOT NULL DEFAULT false', act);
  EXECUTE format('ALTER TABLE %s ADD COLUMN IF NOT EXISTS verificacion text NOT NULL DEFAULT %L', act, 'pendiente');
  EXECUTE format('ALTER TABLE %s ADD COLUMN IF NOT EXISTS metricas_cliente jsonb', act);
  EXECUTE format('ALTER TABLE %s DROP CONSTRAINT IF EXISTS activities_verificacion_valores', act);
  EXECUTE format(
    'ALTER TABLE %s ADD CONSTRAINT activities_verificacion_valores CHECK (verificacion IN (%L, %L, %L, %L, %L, %L))',
    act, 'pendiente', 'verificada', 'distancia_sin_puntos', 'imposible', 'insuficiente', 'recorrido_excesivo');
  EXECUTE format(
    'COMMENT ON COLUMN %s.verificada IS %L',
    act, 'True solo si el servidor recalculo la distancia con el recorrido y la velocidad cabe en una carrera a pie.');
  EXECUTE format(
    'CREATE INDEX IF NOT EXISTS activities_verificadas_idx ON %s (user_id, start_time DESC) WHERE verificada',
    act);

  IF esquema = 'public' THEN
    EXECUTE 'CREATE OR REPLACE VIEW actividad.activities WITH (security_invoker = true) AS SELECT * FROM public.activities';
  ELSE
    EXECUTE 'CREATE OR REPLACE VIEW public.activities WITH (security_invoker = true) AS SELECT * FROM actividad.activities';
  END IF;
END
$mig$;

CREATE OR REPLACE FUNCTION actividad.a_float(t text)
RETURNS double precision
LANGUAGE plpgsql
IMMUTABLE
AS $$
BEGIN
  IF t IS NULL OR btrim(t) = '' THEN RETURN NULL; END IF;
  RETURN t::double precision;
EXCEPTION WHEN others THEN
  RETURN NULL;
END;
$$;

REVOKE ALL ON FUNCTION actividad.a_float(text) FROM PUBLIC;

-- Distancia y duracion a partir del JSON de puntos (lat/lng y tiempoMs o t).
CREATE OR REPLACE FUNCTION actividad.evaluar_carrera(
  puntos jsonb,
  distancia_cliente integer,
  duracion_cliente integer,
  inicio timestamptz,
  fin timestamptz,
  desnivel_cliente integer
) RETURNS TABLE (
  distance_meters integer,
  duration_seconds integer,
  avg_pace_seconds_per_km integer,
  elevation_gain_meters integer,
  verificada boolean,
  verificacion text,
  gps_metros integer,
  gps_segundos integer
)
LANGUAGE plpgsql
STABLE
SET search_path = public, actividad
AS $$
DECLARE
  rec record;
  plat double precision;
  plng double precision;
  pt double precision;
  palt double precision;
  primer boolean := true;
  seg double precision;
  dt double precision;
  dist double precision := 0;
  desnivel double precision := 0;
  con_alt integer := 0;
  validos integer := 0;
  t_min double precision;
  t_max double precision;
  dur integer := 0;
  media_max double precision;
  motivo text;
  dist_out integer := 0;
  des_out integer := 0;
  ritmo integer;
BEGIN
  IF puntos IS NOT NULL AND jsonb_typeof(puntos) = 'array' AND jsonb_array_length(puntos) > 20000 THEN
    RETURN QUERY SELECT 0, 0, NULL::integer, 0, false, 'recorrido_excesivo'::text, 0, 0;
    RETURN;
  END IF;

  IF puntos IS NOT NULL AND jsonb_typeof(puntos) = 'array' THEN
    FOR rec IN
      SELECT
        actividad.a_float(e.p->>'lat') AS lat,
        COALESCE(actividad.a_float(e.p->>'lng'), actividad.a_float(e.p->>'lon'), actividad.a_float(e.p->>'longitude')) AS lng,
        COALESCE(actividad.a_float(e.p->>'tiempoMs'), actividad.a_float(e.p->>'t')) AS t,
        COALESCE(actividad.a_float(e.p->>'precisionM'), actividad.a_float(e.p->>'accuracy')) AS prec,
        COALESCE(actividad.a_float(e.p->>'altitudM'), actividad.a_float(e.p->>'altitude'), actividad.a_float(e.p->>'ele')) AS alt
      FROM jsonb_array_elements(puntos) WITH ORDINALITY AS e(p, ord)
      ORDER BY e.ord
    LOOP
      IF rec.lat IS NULL OR rec.lng IS NULL
         OR rec.lat NOT BETWEEN -90 AND 90 OR rec.lng NOT BETWEEN -180 AND 180 THEN
        CONTINUE;
      END IF;
      IF rec.prec IS NOT NULL AND rec.prec > 35 THEN
        CONTINUE;
      END IF;
      IF rec.t IS NOT NULL THEN
        IF t_min IS NULL OR rec.t < t_min THEN t_min := rec.t; END IF;
        IF t_max IS NULL OR rec.t > t_max THEN t_max := rec.t; END IF;
      END IF;
      IF primer THEN
        primer := false;
        plat := rec.lat; plng := rec.lng; pt := rec.t; palt := rec.alt;
        validos := 1;
        CONTINUE;
      END IF;
      validos := validos + 1;
      seg := ST_Distance(
        ST_SetSRID(ST_MakePoint(plng, plat), 4326)::geography,
        ST_SetSRID(ST_MakePoint(rec.lng, rec.lat), 4326)::geography);
      IF rec.t IS NOT NULL AND pt IS NOT NULL AND rec.t > pt AND seg >= 2 THEN
        dt := (rec.t - pt) / 1000.0;
        IF dt > 0 AND seg / dt <= 12 THEN
          dist := dist + seg;
          IF palt IS NOT NULL AND rec.alt IS NOT NULL AND rec.alt > palt THEN
            desnivel := desnivel + LEAST(rec.alt - palt, 40);
            con_alt := con_alt + 1;
          END IF;
          plat := rec.lat; plng := rec.lng; pt := rec.t; palt := rec.alt;
        END IF;
      END IF;
    END LOOP;
  END IF;

  IF t_min IS NOT NULL AND t_max > t_min THEN
    dur := round((t_max - t_min) / 1000.0)::integer;
  ELSIF fin IS NOT NULL AND inicio IS NOT NULL AND fin > inicio THEN
    dur := LEAST(extract(epoch FROM (fin - inicio))::integer, 129600);
  ELSE
    dur := COALESCE(duracion_cliente, 0);
  END IF;

  media_max := CASE WHEN dist >= 200 THEN 8.0 ELSE 12.0 END;

  -- Si el track ya demuestra una carrera creible, las cifras del cliente no importan
  -- (un upsert posterior puede mandar negativos o un km inflado).
  IF validos >= 2 AND dist >= 100 AND dur > 0 AND dur <= 129600 AND dist / dur <= media_max THEN
    motivo := 'verificada';
  ELSIF COALESCE(distancia_cliente, 0) < 0 OR COALESCE(duracion_cliente, 0) < 0 OR dur > 129600 THEN
    motivo := 'imposible';
  ELSIF validos < 2 THEN
    IF COALESCE(distancia_cliente, 0) > 0 THEN
      motivo := 'distancia_sin_puntos';
    ELSE
      motivo := 'pendiente';
    END IF;
  ELSE
    motivo := CASE WHEN dist < 100 THEN 'insuficiente' ELSE 'imposible' END;
  END IF;

  IF motivo = 'verificada' THEN
    dist_out := round(dist)::integer;
    IF con_alt > 0 THEN
      des_out := LEAST(round(desnivel)::integer, 8000);
    ELSE
      des_out := LEAST(GREATEST(COALESCE(desnivel_cliente, 0), 0), GREATEST(round(dist * 0.35)::integer, 0), 8000);
    END IF;
    ritmo := round(dur * 1000.0 / dist)::integer;
    RETURN QUERY SELECT dist_out, dur, ritmo, des_out, true, motivo, dist_out, dur;
    RETURN;
  END IF;

  -- Sin recorrido se conservan los numeros del cliente (no negativos): el reporte
  -- personal y las carreras ya guardadas siguen leyendose. No quedan verificadas.
  IF motivo IN ('distancia_sin_puntos', 'pendiente') THEN
    ritmo := CASE
      WHEN COALESCE(distancia_cliente, 0) > 0 AND COALESCE(duracion_cliente, 0) > 0
      THEN round(duracion_cliente * 1000.0 / distancia_cliente)::integer
      ELSE NULL
    END;
    RETURN QUERY SELECT
      GREATEST(COALESCE(distancia_cliente, 0), 0),
      GREATEST(COALESCE(duracion_cliente, 0), 0),
      ritmo,
      GREATEST(COALESCE(desnivel_cliente, 0), 0),
      false,
      motivo,
      round(dist)::integer,
      GREATEST(dur, 0);
    RETURN;
  END IF;

  RETURN QUERY SELECT 0, 0, NULL::integer, 0, false, motivo, round(dist)::integer, GREATEST(dur, 0);
END;
$$;

REVOKE ALL ON FUNCTION actividad.evaluar_carrera(jsonb, integer, integer, timestamptz, timestamptz, integer) FROM PUBLIC;

CREATE OR REPLACE FUNCTION actividad.trg_verificar_carrera()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, actividad
AS $$
DECLARE
  puntos jsonb;
  m record;
  forzar boolean := coalesce(current_setting('actividad.forzar_verificacion', true), '') = '1';
  cambian boolean;
  dist_cli integer;
  dur_cli integer;
  des_cli integer;
BEGIN
  IF TG_OP = 'UPDATE' AND NOT forzar THEN
    cambian := NEW.distance_meters IS DISTINCT FROM OLD.distance_meters
      OR NEW.duration_seconds IS DISTINCT FROM OLD.duration_seconds
      OR NEW.avg_pace_seconds_per_km IS DISTINCT FROM OLD.avg_pace_seconds_per_km
      OR NEW.elevation_gain_meters IS DISTINCT FROM OLD.elevation_gain_meters
      OR NEW.start_time IS DISTINCT FROM OLD.start_time
      OR NEW.end_time IS DISTINCT FROM OLD.end_time
      OR NEW.activity_type IS DISTINCT FROM OLD.activity_type
      OR NEW.verificada IS DISTINCT FROM OLD.verificada
      OR NEW.verificacion IS DISTINCT FROM OLD.verificacion;
    IF NOT cambian THEN
      NEW.verificada := OLD.verificada;
      NEW.verificacion := OLD.verificacion;
      NEW.metricas_cliente := OLD.metricas_cliente;
      NEW.distance_meters := OLD.distance_meters;
      NEW.duration_seconds := OLD.duration_seconds;
      NEW.avg_pace_seconds_per_km := OLD.avg_pace_seconds_per_km;
      NEW.elevation_gain_meters := OLD.elevation_gain_meters;
      RETURN NEW;
    END IF;
  END IF;

  IF NOT forzar OR NEW.metricas_cliente IS NULL THEN
    NEW.metricas_cliente := jsonb_build_object(
      'distance_meters', NEW.distance_meters,
      'duration_seconds', NEW.duration_seconds,
      'avg_pace_seconds_per_km', NEW.avg_pace_seconds_per_km,
      'elevation_gain_meters', NEW.elevation_gain_meters
    );
  END IF;

  -- En el recalculo el NEW ya puede traer ceros (el insert anterior los puso). La
  -- reclamacion original sigue en metricas_cliente y solo se usa de respaldo
  -- para el desnivel cuando el track no trae altitud.
  dist_cli := COALESCE(actividad.a_float(NEW.metricas_cliente->>'distance_meters')::integer, NEW.distance_meters);
  dur_cli := COALESCE(actividad.a_float(NEW.metricas_cliente->>'duration_seconds')::integer, NEW.duration_seconds);
  des_cli := COALESCE(actividad.a_float(NEW.metricas_cliente->>'elevation_gain_meters')::integer, NEW.elevation_gain_meters);

  SELECT t.points INTO puntos FROM public.activity_tracks t WHERE t.activity_id = NEW.id;
  SELECT * INTO m FROM actividad.evaluar_carrera(
    puntos, dist_cli, dur_cli, NEW.start_time, NEW.end_time, des_cli);

  NEW.distance_meters := m.distance_meters;
  NEW.duration_seconds := m.duration_seconds;
  NEW.avg_pace_seconds_per_km := m.avg_pace_seconds_per_km;
  NEW.elevation_gain_meters := m.elevation_gain_meters;
  NEW.verificada := m.verificada;
  NEW.verificacion := m.verificacion;
  NEW.metricas_cliente := COALESCE(NEW.metricas_cliente, '{}'::jsonb) || jsonb_build_object(
    'gps_metros', m.gps_metros, 'gps_segundos', m.gps_segundos);
  RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION actividad.trg_verificar_recorrido()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, actividad
AS $$
DECLARE
  carrera uuid := COALESCE(NEW.activity_id, OLD.activity_id);
BEGIN
  PERFORM set_config('actividad.forzar_verificacion', '1', true);
  UPDATE public.activities SET updated_at = now() WHERE id = carrera;
  PERFORM set_config('actividad.forzar_verificacion', '0', true);
  RETURN NULL;
END;
$$;

-- Logros: solo si hay una carrera verificada que los respalde. La app puede
-- seguir intentando el insert; si no toca, esta fila no se crea.
CREATE OR REPLACE FUNCTION logros.logro_merecido(uid uuid, codigo text)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, logros
AS $$
  SELECT CASE codigo
    WHEN 'primera_carrera' THEN EXISTS (
      SELECT 1 FROM activities a WHERE a.user_id = uid AND a.verificada)
    WHEN 'km_5' THEN EXISTS (
      SELECT 1 FROM activities a WHERE a.user_id = uid AND a.verificada AND a.distance_meters >= 5000)
    WHEN 'km_10' THEN EXISTS (
      SELECT 1 FROM activities a WHERE a.user_id = uid AND a.verificada AND a.distance_meters >= 10000)
    WHEN 'racha_7' THEN EXISTS (
      SELECT 1 FROM user_streaks s
      WHERE s.user_id = uid AND GREATEST(s.current_daily, s.best_daily) >= 7)
    ELSE false
  END;
$$;

CREATE OR REPLACE FUNCTION logros.conceder_verificados(uid uuid)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, logros
AS $$
BEGIN
  IF uid IS NULL THEN RETURN; END IF;
  INSERT INTO public.user_achievements (user_id, code)
  SELECT uid, c
  FROM (VALUES ('primera_carrera'), ('km_5'), ('km_10'), ('racha_7')) AS v(c)
  WHERE logros.logro_merecido(uid, c)
  ON CONFLICT DO NOTHING;
END;
$$;

CREATE OR REPLACE FUNCTION logros.trg_logro_verificado()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, logros
AS $$
BEGIN
  IF NOT logros.logro_merecido(NEW.user_id, NEW.code) THEN
    RAISE EXCEPTION 'logro no verificado' USING ERRCODE = '42501';
  END IF;
  RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION actividad.trg_racha_verificada()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, actividad, logros
AS $$
BEGIN
  IF TG_OP = 'UPDATE' AND NEW.verificada IS NOT DISTINCT FROM OLD.verificada THEN
    RETURN NULL;
  END IF;
  IF coalesce(current_setting('actividad.backfill', true), '') = '1' THEN
    RETURN NULL;
  END IF;
  IF NOT NEW.verificada THEN
    PERFORM public.recalcular_racha(NEW.user_id, false);
    RETURN NULL;
  END IF;
  PERFORM public.recalcular_racha(NEW.user_id, true);
  PERFORM logros.conceder_verificados(NEW.user_id);
  RETURN NULL;
END;
$$;

REVOKE ALL ON FUNCTION actividad.trg_verificar_carrera() FROM PUBLIC;
REVOKE ALL ON FUNCTION actividad.trg_verificar_recorrido() FROM PUBLIC;
REVOKE ALL ON FUNCTION actividad.trg_racha_verificada() FROM PUBLIC;
REVOKE ALL ON FUNCTION logros.logro_merecido(uuid, text) FROM PUBLIC;
REVOKE ALL ON FUNCTION logros.conceder_verificados(uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION logros.trg_logro_verificado() FROM PUBLIC;

DO $mig$
DECLARE rol text;
BEGIN
  FOREACH rol IN ARRAY ARRAY['anon', 'authenticated', 'service_role', 'authenticator'] LOOP
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = rol) THEN
      EXECUTE format('GRANT EXECUTE ON FUNCTION actividad.trg_verificar_carrera() TO %I', rol);
      EXECUTE format('GRANT EXECUTE ON FUNCTION actividad.trg_verificar_recorrido() TO %I', rol);
      EXECUTE format('GRANT EXECUTE ON FUNCTION actividad.trg_racha_verificada() TO %I', rol);
      EXECUTE format('GRANT EXECUTE ON FUNCTION logros.trg_logro_verificado() TO %I', rol);
      EXECUTE format('GRANT EXECUTE ON FUNCTION logros.logro_merecido(uuid, text) TO %I', rol);
    END IF;
  END LOOP;
END
$mig$;

DO $mig$
DECLARE
  act regclass;
  tracks regclass;
  logros_t regclass;
BEGIN
  SELECT c.oid::regclass INTO act
  FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE c.relname = 'activities' AND c.relkind = 'r' AND n.nspname IN ('public', 'actividad');
  SELECT c.oid::regclass INTO tracks
  FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE c.relname = 'activity_tracks' AND c.relkind = 'r' AND n.nspname IN ('public', 'actividad');
  SELECT c.oid::regclass INTO logros_t
  FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE c.relname = 'user_achievements' AND c.relkind = 'r' AND n.nspname IN ('public', 'logros');
  IF act IS NULL OR tracks IS NULL OR logros_t IS NULL THEN
    RAISE EXCEPTION 'faltan activities, activity_tracks o user_achievements';
  END IF;

  EXECUTE format('DROP TRIGGER IF EXISTS zz_verificar_carrera ON %s', act);
  EXECUTE format(
    'CREATE TRIGGER zz_verificar_carrera BEFORE INSERT OR UPDATE ON %s FOR EACH ROW EXECUTE FUNCTION actividad.trg_verificar_carrera()',
    act);
  EXECUTE format('DROP TRIGGER IF EXISTS zz_racha_verificada ON %s', act);
  EXECUTE format(
    'CREATE TRIGGER zz_racha_verificada AFTER INSERT OR UPDATE ON %s FOR EACH ROW EXECUTE FUNCTION actividad.trg_racha_verificada()',
    act);
  EXECUTE format('DROP TRIGGER IF EXISTS zz_verificar_recorrido ON %s', tracks);
  EXECUTE format(
    'CREATE TRIGGER zz_verificar_recorrido AFTER INSERT OR UPDATE OR DELETE ON %s FOR EACH ROW EXECUTE FUNCTION actividad.trg_verificar_recorrido()',
    tracks);
  EXECUTE format('DROP TRIGGER IF EXISTS zz_logro_verificado ON %s', logros_t);
  EXECUTE format(
    'CREATE TRIGGER zz_logro_verificado BEFORE INSERT ON %s FOR EACH ROW EXECUTE FUNCTION logros.trg_logro_verificado()',
    logros_t);
END
$mig$;

-- ---------------------------------------------------------------------------
-- Rankings, rachas, logros, clubes, ruta sugerida y reportes solo cuentan
-- carreras verificadas. Ademas las no verificadas guardan 0 en las cifras,
-- asi que un SELECT directo tampoco suma el km que mando el cliente.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.recalcular_racha(uid uuid, notificar boolean DEFAULT false)
 RETURNS logros.user_streaks
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
  hoy DATE := (now() AT TIME ZONE 'America/Guayaquil')::date;
  semana_actual DATE := date_trunc('week', hoy)::date;
  meta INTEGER;
  antes user_streaks;
  r user_streaks;
  ultimo DATE;
  cur_d INTEGER := 0; best_d INTEGER := 0;
  cur_w INTEGER := 0; best_w INTEGER := 0;
  corridas INTEGER := 0;
BEGIN
  SELECT COALESCE((SELECT weekly_goal_runs FROM user_settings WHERE user_id = uid), 3) INTO meta;
  SELECT * INTO antes FROM user_streaks WHERE user_id = uid;

  WITH dias AS (
    SELECT DISTINCT (start_time AT TIME ZONE 'America/Guayaquil')::date AS d
    FROM activities WHERE user_id = uid AND verificada
  ), islas AS (
    SELECT d, d - (row_number() OVER (ORDER BY d))::int AS g FROM dias
  ), tam AS (
    SELECT g, count(*)::int AS n, max(d) AS fin FROM islas GROUP BY g
  )
  SELECT COALESCE(max(n), 0),
         COALESCE(max(n) FILTER (WHERE fin >= hoy - 1 AND fin = (SELECT max(d) FROM dias)), 0),
         (SELECT max(d) FROM dias)
  INTO best_d, cur_d, ultimo FROM tam;

  SELECT count(*)::int INTO corridas FROM activities
  WHERE user_id = uid AND verificada
    AND date_trunc('week', (start_time AT TIME ZONE 'America/Guayaquil')::date)::date = semana_actual;

  WITH semanas AS (
    SELECT date_trunc('week', (start_time AT TIME ZONE 'America/Guayaquil')::date)::date AS w
    FROM activities WHERE user_id = uid AND verificada
    GROUP BY 1 HAVING count(*) >= meta
  ), islas AS (
    SELECT w, w - (7 * row_number() OVER (ORDER BY w))::int AS g FROM semanas
  ), tam AS (
    SELECT g, count(*)::int AS n, max(w) AS fin FROM islas GROUP BY g
  )
  SELECT COALESCE(max(n), 0),
         COALESCE(max(n) FILTER (WHERE fin >= semana_actual - 7), 0)
  INTO best_w, cur_w FROM tam;

  INSERT INTO user_streaks AS s (user_id, current_daily, best_daily, last_active_date, current_weekly,
    best_weekly, weekly_goal_runs, runs_this_week, updated_at)
  VALUES (uid, cur_d, best_d, ultimo, cur_w, best_w, meta, corridas, now())
  ON CONFLICT (user_id) DO UPDATE SET current_daily = EXCLUDED.current_daily,
    best_daily = EXCLUDED.best_daily, last_active_date = EXCLUDED.last_active_date,
    current_weekly = EXCLUDED.current_weekly, best_weekly = EXCLUDED.best_weekly,
    weekly_goal_runs = EXCLUDED.weekly_goal_runs, runs_this_week = EXCLUDED.runs_this_week,
    updated_at = now()
  RETURNING * INTO r;

  IF notificar AND r.current_daily > COALESCE(antes.current_daily, 0) AND r.current_daily >= 2 THEN
    INSERT INTO notifications (user_id, type, actor_name, message)
    VALUES (uid, 'streak', 'Runcity Loja', format('Racha de %s días. ¡Sigue así!', r.current_daily));
  END IF;
  IF notificar AND r.current_weekly > COALESCE(antes.current_weekly, 0) AND r.current_weekly >= 1 THEN
    INSERT INTO notifications (user_id, type, actor_name, message)
    VALUES (uid, 'streak', 'Runcity Loja', format('Cumpliste tu meta semanal: racha de %s %s', r.current_weekly,
      CASE WHEN r.current_weekly = 1 THEN 'semana' ELSE 'semanas' END));
  END IF;
  RETURN r;
END;
$function$;

DROP FUNCTION IF EXISTS public.feed_comunidad(INT, TIMESTAMPTZ);
CREATE OR REPLACE FUNCTION public.feed_comunidad(p_limite INT DEFAULT 30, p_antes TIMESTAMPTZ DEFAULT NULL)
RETURNS TABLE (
  id UUID, client_id TEXT, user_id UUID, autor TEXT, avatar_url TEXT, title TEXT, start_time TIMESTAMPTZ,
  distance_meters NUMERIC, duration_seconds INT, likes_count INT, comments_count INT,
  public_points JSONB, me_gusta BOOLEAN, sigo BOOLEAN, propia BOOLEAN, elevation_gain INT,
  images JSONB, reposts_count INT, reposteada BOOLEAN, autor_km NUMERIC, ultimo_comentario JSONB
) LANGUAGE sql STABLE SECURITY INVOKER SET search_path = public AS $$
  SELECT a.id, a.client_id, a.user_id,
         COALESCE(NULLIF(p.full_name, ''), NULLIF(p.username, ''), 'Corredor'),
         p.avatar_url, COALESCE(NULLIF(a.title, ''), 'Carrera'), a.start_time,
         CASE WHEN a.verificada THEN a.distance_meters ELSE 0 END::numeric,
         CASE WHEN a.verificada THEN a.duration_seconds ELSE 0 END::int,
         COALESCE(a.likes_count, 0), COALESCE(a.comments_count, 0), a.public_points,
         EXISTS (SELECT 1 FROM post_likes l WHERE l.post_id = a.id::text AND l.user_id = auth.uid()),
         EXISTS (SELECT 1 FROM follows f WHERE f.follower_id = auth.uid() AND f.following_id = a.user_id),
         a.user_id = auth.uid(),
         CASE WHEN a.verificada THEN COALESCE(a.elevation_gain_meters, 0) ELSE 0 END::int,
         COALESCE((SELECT jsonb_agg(i.path ORDER BY i.position, i.created_at) FROM post_images i WHERE i.activity_id = a.id), '[]'::jsonb),
         COALESCE(a.reposts_count, 0),
         EXISTS (SELECT 1 FROM post_reposts r WHERE r.activity_id = a.id AND r.user_id = auth.uid()),
         (SELECT COALESCE(sum(x.distance_meters), 0) / 1000.0 FROM activities x
           WHERE x.user_id = a.user_id AND x.is_public AND x.verificada)::numeric,
         (SELECT jsonb_build_object('autor', COALESCE(NULLIF(cp.full_name, ''), NULLIF(cp.username, ''), 'Corredor'),
                                    'texto', c.body, 'creado', c.created_at)
            FROM post_comments c LEFT JOIN profiles cp ON cp.id = c.user_id
           WHERE c.post_id = a.id::text ORDER BY c.created_at DESC LIMIT 1)
  FROM activities a
  LEFT JOIN profiles p ON p.id = a.user_id
  WHERE a.is_public AND (p_antes IS NULL OR a.start_time < p_antes)
  ORDER BY a.start_time DESC
  LIMIT LEAST(GREATEST(p_limite, 1), 100)
$$;
GRANT EXECUTE ON FUNCTION public.feed_comunidad(INT, TIMESTAMPTZ) TO authenticated;

CREATE OR REPLACE FUNCTION public.km_grupos(p_semanas INTEGER DEFAULT 7)
RETURNS TABLE (club_id UUID, semana DATE, km NUMERIC, corredores INTEGER)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, eventos, actividad AS $$
  SELECT m.club_id,
         date_trunc('week', a.start_time AT TIME ZONE 'America/Guayaquil')::date AS semana,
         round(sum(a.distance_meters) / 1000.0, 1) AS km,
         count(DISTINCT a.user_id)::int AS corredores
  FROM eventos.club_members m
  JOIN eventos.clubs c ON c.id = m.club_id AND c.hidden_at IS NULL
  JOIN actividad.activities a ON a.user_id = m.user_id
  WHERE m.status = 'active'
    AND a.verificada
    AND a.start_time >= date_trunc('week', now() AT TIME ZONE 'America/Guayaquil') - make_interval(weeks => greatest(1, least(p_semanas, 26)) - 1)
    AND a.distance_meters > 0
  GROUP BY 1, 2
$$;
REVOKE ALL ON FUNCTION public.km_grupos(INTEGER) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.km_grupos(INTEGER) TO authenticated;

-- ---------------------------------------------------------------------------
-- Historicas. Las de demostracion recuperan un track a partir del trazado
-- publico (la semilla no guardo activity_tracks) para poder verificarlas con
-- la misma regla. Despues se les devuelve ese trazado: fuzz_track lo habria
-- recortado por la zona de casa.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION actividad.recorrido_demo(camino jsonb, metros integer, inicio timestamptz, duracion_s integer)
RETURNS jsonb
LANGUAGE sql
STABLE
SET search_path = public, actividad
AS $$
  WITH base AS (
    SELECT ord::int AS ord,
           actividad.a_float(p->>'lat') AS lat,
           COALESCE(actividad.a_float(p->>'lng'), actividad.a_float(p->>'lon')) AS lng
    FROM jsonb_array_elements(COALESCE(camino, '[]'::jsonb)) WITH ORDINALITY AS e(p, ord)
  ), limpio AS (
    SELECT row_number() OVER (ORDER BY ord) AS i, lat, lng FROM base
    WHERE lat BETWEEN -90 AND 90 AND lng BETWEEN -180 AND 180
  ), una AS (
    SELECT i, lat, lng,
           coalesce(ST_Distance(
             ST_SetSRID(ST_MakePoint(lag(lng) OVER (ORDER BY i), lag(lat) OVER (ORDER BY i)), 4326)::geography,
             ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography), 0) AS seg
    FROM limpio
  ), laps AS (
    SELECT least(12, greatest(1, round(
      coalesce(metros, 0)::double precision / GREATEST(sum(seg), 1.0))::int)) AS v
    FROM una
  ), repetido AS (
    SELECT ((gs - 1) * (SELECT count(*) FROM una) + u.i) AS seq, u.lat, u.lng
    FROM una u
    JOIN laps l ON (SELECT count(*) FROM limpio) >= 2
    JOIN generate_series(1, l.v) AS gs ON true
  ), tramos AS (
    SELECT seq, lat, lng,
           coalesce(ST_Distance(
             ST_SetSRID(ST_MakePoint(lag(lng) OVER (ORDER BY seq), lag(lat) OVER (ORDER BY seq)), 4326)::geography,
             ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography), 0) AS seg
    FROM repetido
  ), acum AS (
    SELECT seq, lat, lng, sum(seg) OVER (ORDER BY seq) AS acc, sum(seg) OVER () AS total FROM tramos
  )
  SELECT COALESCE(jsonb_agg(jsonb_build_object(
      'lat', lat, 'lng', lng, 'precisionM', 8,
      'tiempoMs', round(extract(epoch FROM inicio) * 1000
        + CASE WHEN total > 0 THEN acc / total ELSE 0 END * duracion_s * 1000)) ORDER BY seq), '[]'::jsonb)
  FROM acum
  WHERE inicio IS NOT NULL AND COALESCE(duracion_s, 0) > 0
$$;

REVOKE ALL ON FUNCTION actividad.recorrido_demo(jsonb, integer, timestamptz, integer) FROM PUBLIC;

SELECT set_config('actividad.backfill', '1', true);

CREATE TEMP TABLE _demo_046 ON COMMIT DROP AS
SELECT id, public_points
FROM public.activities
WHERE client_id LIKE 'demo-%' OR id::text LIKE 'da000000-%';

INSERT INTO public.activity_tracks (activity_id, user_id, points)
SELECT a.id, a.user_id, d.pts
FROM public.activities a
CROSS JOIN LATERAL (
  SELECT actividad.recorrido_demo(a.public_points, a.distance_meters, a.start_time, a.duration_seconds) AS pts
) d
WHERE a.id IN (SELECT id FROM _demo_046)
  AND NOT EXISTS (SELECT 1 FROM public.activity_tracks t WHERE t.activity_id = a.id)
  AND jsonb_typeof(d.pts) = 'array'
  AND jsonb_array_length(d.pts) >= 2;

SELECT set_config('actividad.forzar_verificacion', '1', true);
UPDATE public.activities SET updated_at = now();
SELECT set_config('actividad.forzar_verificacion', '0', true);

UPDATE public.activities a
SET public_points = d.public_points
FROM _demo_046 d
WHERE a.id = d.id;

SELECT public.recalcular_racha(s.user_id, false)
FROM (
  SELECT user_id FROM public.activities
  UNION
  SELECT user_id FROM public.user_streaks
) s;

SELECT logros.conceder_verificados(s.user_id)
FROM (SELECT DISTINCT user_id FROM public.activities WHERE verificada) s;

DROP FUNCTION actividad.recorrido_demo(jsonb, integer, timestamptz, integer);

NOTIFY pgrst, 'reload schema';

COMMIT;
