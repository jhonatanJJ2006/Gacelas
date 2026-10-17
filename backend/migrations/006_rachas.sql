-- Sistema de rachas (zona horaria America/Guayaquil).
-- Racha diaria: dias consecutivos con al menos una actividad guardada. Sigue viva si la
-- ultima actividad fue hoy o ayer (se rompe al pasar un dia completo sin correr).
-- Racha semanal: semanas (lunes a domingo) consecutivas con al menos `weekly_goal_runs`
-- carreras. La semana en curso solo suma cuando ya cumplio la meta; si aun no, la racha
-- sigue viva contando hasta la semana anterior.

ALTER TABLE user_settings ADD COLUMN IF NOT EXISTS weekly_goal_runs INTEGER NOT NULL DEFAULT 3
  CHECK (weekly_goal_runs BETWEEN 1 AND 14);

ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check
  CHECK (type IN ('like', 'comment', 'follow', 'event', 'live', 'streak'));

CREATE TABLE IF NOT EXISTS user_streaks (
  user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  current_daily INTEGER NOT NULL DEFAULT 0,
  best_daily INTEGER NOT NULL DEFAULT 0,
  last_active_date DATE,
  current_weekly INTEGER NOT NULL DEFAULT 0,
  best_weekly INTEGER NOT NULL DEFAULT 0,
  weekly_goal_runs INTEGER NOT NULL DEFAULT 3,
  runs_this_week INTEGER NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE user_streaks ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Own streaks select" ON user_streaks;
CREATE POLICY "Own streaks select" ON user_streaks FOR SELECT USING (auth.uid() = user_id);
-- Sin politicas de escritura: solo las funciones SECURITY DEFINER la modifican.

CREATE OR REPLACE FUNCTION public.recalcular_racha(uid UUID, notificar BOOLEAN DEFAULT false)
RETURNS user_streaks LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $$
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
    VALUES (uid, 'streak', 'Gacelas Loja', format('Racha de %s días. ¡Sigue así!', r.current_daily));
  END IF;
  IF notificar AND r.current_weekly > COALESCE(antes.current_weekly, 0) AND r.current_weekly >= 1 THEN
    INSERT INTO notifications (user_id, type, actor_name, message)
    VALUES (uid, 'streak', 'Gacelas Loja', format('Cumpliste tu meta semanal: racha de %s %s', r.current_weekly,
      CASE WHEN r.current_weekly = 1 THEN 'semana' ELSE 'semanas' END));
  END IF;
  RETURN r;
END;
$$;

REVOKE ALL ON FUNCTION public.recalcular_racha(UUID, BOOLEAN) FROM PUBLIC, anon, authenticated;

-- El usuario puede pedir su propia racha (p. ej. al abrir la app tras cambiar de dia).
CREATE OR REPLACE FUNCTION public.mi_racha()
RETURNS user_streaks LANGUAGE sql SECURITY DEFINER SET search_path TO 'public' AS $$
  SELECT * FROM recalcular_racha(auth.uid(), false) WHERE auth.uid() IS NOT NULL;
$$;
REVOKE ALL ON FUNCTION public.mi_racha() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.mi_racha() TO authenticated;

CREATE OR REPLACE FUNCTION public.trg_racha_actividades()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $$
BEGIN
  -- Al borrar la cuenta, las actividades se borran en cascada: no hay racha que recalcular.
  IF TG_OP = 'DELETE' AND NOT EXISTS (SELECT 1 FROM auth.users WHERE id = OLD.user_id) THEN
    RETURN NULL;
  END IF;
  PERFORM recalcular_racha(COALESCE(NEW.user_id, OLD.user_id), TG_OP = 'INSERT');
  RETURN NULL;
END;
$$;
DROP TRIGGER IF EXISTS racha_actividades ON activities;
CREATE TRIGGER racha_actividades AFTER INSERT OR DELETE ON activities
  FOR EACH ROW EXECUTE FUNCTION trg_racha_actividades();

CREATE OR REPLACE FUNCTION public.trg_racha_meta()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $$
BEGIN
  IF NEW.weekly_goal_runs IS DISTINCT FROM OLD.weekly_goal_runs THEN
    PERFORM recalcular_racha(NEW.user_id, false);
  END IF;
  RETURN NULL;
END;
$$;
DROP TRIGGER IF EXISTS racha_meta ON user_settings;
CREATE TRIGGER racha_meta AFTER UPDATE ON user_settings
  FOR EACH ROW EXECUTE FUNCTION trg_racha_meta();

-- Aviso de racha en riesgo: quien tiene racha de 2+ dias, corrio ayer y aun no hoy.
CREATE OR REPLACE FUNCTION public.avisar_rachas_en_riesgo()
RETURNS INTEGER LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $$
DECLARE
  hoy DATE := (now() AT TIME ZONE 'America/Guayaquil')::date;
  n INTEGER;
BEGIN
  INSERT INTO notifications (user_id, type, actor_name, message)
  SELECT s.user_id, 'streak', 'Gacelas Loja',
         format('Tu racha de %s días está en riesgo: corre hoy para no perderla', s.current_daily)
  FROM user_streaks s
  WHERE s.current_daily >= 2 AND s.last_active_date = hoy - 1
    AND NOT EXISTS (SELECT 1 FROM notifications x WHERE x.user_id = s.user_id AND x.type = 'streak'
      AND x.message LIKE '%en riesgo%' AND (x.created_at AT TIME ZONE 'America/Guayaquil')::date = hoy);
  GET DIAGNOSTICS n = ROW_COUNT;
  RETURN n;
END;
$$;
REVOKE ALL ON FUNCTION public.avisar_rachas_en_riesgo() FROM PUBLIC, anon, authenticated;

-- Todos los dias a las 18:00 de Ecuador (23:00 UTC).
CREATE EXTENSION IF NOT EXISTS pg_cron;
SELECT cron.unschedule('gacelas-rachas-en-riesgo') WHERE EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'gacelas-rachas-en-riesgo');
SELECT cron.schedule('gacelas-rachas-en-riesgo', '0 23 * * *', 'SELECT public.avisar_rachas_en_riesgo()');

-- Rachas iniciales de los usuarios existentes.
SELECT recalcular_racha(id, false) FROM auth.users;
