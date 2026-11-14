-- Revierte 046_metricas_carrera.sql.
-- Devuelve a las carreras la cifra que habia mandado el cliente (metricas_cliente),
-- quita los tracks reconstruidos de la demostracion y borra la verificacion.
-- No se aplica solo: psql -v ON_ERROR_STOP=1 -f supabase/rollback/046_metricas_carrera.down.sql

BEGIN;

DO $rb$
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
  IF act IS NOT NULL THEN
    EXECUTE format('DROP TRIGGER IF EXISTS zz_verificar_carrera ON %s', act);
    EXECUTE format('DROP TRIGGER IF EXISTS zz_racha_verificada ON %s', act);
  END IF;
  IF tracks IS NOT NULL THEN
    EXECUTE format('DROP TRIGGER IF EXISTS zz_verificar_recorrido ON %s', tracks);
  END IF;
  IF logros_t IS NOT NULL THEN
    EXECUTE format('DROP TRIGGER IF EXISTS zz_logro_verificado ON %s', logros_t);
  END IF;
END
$rb$;

-- Sin los triggers, se puede devolver la cifra original.
UPDATE public.activities
SET
  distance_meters = CASE WHEN metricas_cliente->>'distance_meters' IS NOT NULL
    THEN (metricas_cliente->>'distance_meters')::integer ELSE distance_meters END,
  duration_seconds = CASE WHEN metricas_cliente->>'duration_seconds' IS NOT NULL
    THEN (metricas_cliente->>'duration_seconds')::integer ELSE duration_seconds END,
  avg_pace_seconds_per_km = CASE WHEN metricas_cliente->>'avg_pace_seconds_per_km' IS NOT NULL
    THEN (metricas_cliente->>'avg_pace_seconds_per_km')::integer ELSE avg_pace_seconds_per_km END,
  elevation_gain_meters = CASE WHEN metricas_cliente->>'elevation_gain_meters' IS NOT NULL
    THEN (metricas_cliente->>'elevation_gain_meters')::integer ELSE elevation_gain_meters END
WHERE metricas_cliente IS NOT NULL;

DELETE FROM public.activity_tracks t
USING public.activities a
WHERE t.activity_id = a.id
  AND (a.client_id LIKE 'demo-%' OR a.id::text LIKE 'da000000-%');

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
    FROM activities WHERE user_id = uid
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
  WHERE user_id = uid AND date_trunc('week', (start_time AT TIME ZONE 'America/Guayaquil')::date)::date = semana_actual;

  WITH semanas AS (
    SELECT date_trunc('week', (start_time AT TIME ZONE 'America/Guayaquil')::date)::date AS w
    FROM activities WHERE user_id = uid
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
$function$

;


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
         a.distance_meters::numeric, a.duration_seconds::int,
         COALESCE(a.likes_count, 0), COALESCE(a.comments_count, 0), a.public_points,
         EXISTS (SELECT 1 FROM post_likes l WHERE l.post_id = a.id::text AND l.user_id = auth.uid()),
         EXISTS (SELECT 1 FROM follows f WHERE f.follower_id = auth.uid() AND f.following_id = a.user_id),
         a.user_id = auth.uid(),
         COALESCE(a.elevation_gain_meters, 0)::int,
         COALESCE((SELECT jsonb_agg(i.path ORDER BY i.position, i.created_at) FROM post_images i WHERE i.activity_id = a.id), '[]'::jsonb),
         COALESCE(a.reposts_count, 0),
         EXISTS (SELECT 1 FROM post_reposts r WHERE r.activity_id = a.id AND r.user_id = auth.uid()),
         (SELECT COALESCE(sum(x.distance_meters), 0) / 1000.0 FROM activities x WHERE x.user_id = a.user_id AND x.is_public)::numeric,
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


DROP FUNCTION IF EXISTS public.km_grupos(integer);
DROP FUNCTION IF EXISTS logros.trg_logro_verificado();
DROP FUNCTION IF EXISTS logros.conceder_verificados(uuid);
DROP FUNCTION IF EXISTS logros.logro_merecido(uuid, text);
DROP FUNCTION IF EXISTS actividad.trg_racha_verificada();
DROP FUNCTION IF EXISTS actividad.trg_verificar_recorrido();
DROP FUNCTION IF EXISTS actividad.trg_verificar_carrera();
DROP FUNCTION IF EXISTS actividad.evaluar_carrera(jsonb, integer, integer, timestamptz, timestamptz, integer);
DROP FUNCTION IF EXISTS actividad.a_float(text);
DROP FUNCTION IF EXISTS actividad.recorrido_demo(jsonb, integer, timestamptz, integer);

DO $rb$
DECLARE esquema text;
BEGIN
  SELECT n.nspname INTO esquema
  FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE c.relname = 'activities' AND c.relkind = 'r' AND n.nspname IN ('public', 'actividad');
  IF esquema IS NULL THEN
    RAISE EXCEPTION 'no esta la tabla activities';
  END IF;
  IF esquema = 'public' THEN
    DROP VIEW IF EXISTS actividad.activities;
    ALTER TABLE public.activities DROP CONSTRAINT IF EXISTS activities_verificacion_valores;
    DROP INDEX IF EXISTS public.activities_verificadas_idx;
    ALTER TABLE public.activities
      DROP COLUMN IF EXISTS verificada,
      DROP COLUMN IF EXISTS verificacion,
      DROP COLUMN IF EXISTS metricas_cliente;
    CREATE VIEW actividad.activities WITH (security_invoker = true) AS SELECT * FROM public.activities;
    GRANT SELECT ON actividad.activities TO service_role;
  ELSE
    DROP VIEW IF EXISTS public.activities;
    ALTER TABLE actividad.activities DROP CONSTRAINT IF EXISTS activities_verificacion_valores;
    DROP INDEX IF EXISTS actividad.activities_verificadas_idx;
    ALTER TABLE actividad.activities
      DROP COLUMN IF EXISTS verificada,
      DROP COLUMN IF EXISTS verificacion,
      DROP COLUMN IF EXISTS metricas_cliente;
    CREATE VIEW public.activities WITH (security_invoker = true) AS SELECT * FROM actividad.activities;
    GRANT SELECT, INSERT, UPDATE, DELETE ON public.activities TO anon, authenticated, service_role;
  END IF;
END
$rb$;

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
    AND a.start_time >= date_trunc('week', now() AT TIME ZONE 'America/Guayaquil') - make_interval(weeks => greatest(1, least(p_semanas, 26)) - 1)
    AND a.distance_meters > 0
  GROUP BY 1, 2
$$;
REVOKE ALL ON FUNCTION public.km_grupos(INTEGER) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.km_grupos(INTEGER) TO authenticated;


SELECT public.recalcular_racha(s.user_id, false)
FROM (
  SELECT user_id FROM public.activities
  UNION
  SELECT user_id FROM public.user_streaks
) s;

NOTIFY pgrst, 'reload schema';

COMMIT;
