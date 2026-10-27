-- Revierte 036: vuelve a poner Gacelas como remitente en funciones y notificaciones guardadas.
BEGIN;
CREATE OR REPLACE FUNCTION public.handle_new_user()
 RETURNS trigger
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
  nombre TEXT := COALESCE(NULLIF(NEW.raw_user_meta_data->>'full_name', ''), NULLIF(NEW.raw_user_meta_data->>'name', ''), split_part(NEW.email, '@', 1));
  base TEXT := left(lower(regexp_replace(COALESCE(NULLIF(NEW.raw_user_meta_data->>'username', ''), split_part(NEW.email, '@', 1)), '[^a-zA-Z0-9_]', '_', 'g')), 20);
  usuario TEXT := base;
BEGIN
  IF length(usuario) < 3 THEN usuario := rpad(usuario, 3, '_'); base := usuario; END IF;
  IF EXISTS (SELECT 1 FROM profiles WHERE username = usuario) THEN
    usuario := left(base, 13) || '_' || substr(NEW.id::text, 1, 6);
  END IF;
  INSERT INTO profiles (id, username, full_name, avatar_url)
  VALUES (NEW.id, usuario, nombre, NEW.raw_user_meta_data->>'avatar_url')
  ON CONFLICT (id) DO NOTHING;
  INSERT INTO user_settings (user_id) VALUES (NEW.id) ON CONFLICT DO NOTHING;
  INSERT INTO notifications (user_id, type, actor_name, message)
  VALUES (NEW.id, 'event', 'Gacelas Loja', 'Bienvenido a Gacelas. Mira los eventos de esta semana');
  RETURN NEW;
END;
$function$

;

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
    VALUES (uid, 'streak', 'Gacelas Loja', format('Racha de %s días. ¡Sigue así!', r.current_daily));
  END IF;
  IF notificar AND r.current_weekly > COALESCE(antes.current_weekly, 0) AND r.current_weekly >= 1 THEN
    INSERT INTO notifications (user_id, type, actor_name, message)
    VALUES (uid, 'streak', 'Gacelas Loja', format('Cumpliste tu meta semanal: racha de %s %s', r.current_weekly,
      CASE WHEN r.current_weekly = 1 THEN 'semana' ELSE 'semanas' END));
  END IF;
  RETURN r;
END;
$function$

;

CREATE OR REPLACE FUNCTION public.avisar_rachas_en_riesgo()
 RETURNS integer
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
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
$function$

;

CREATE OR REPLACE FUNCTION public.moderar_reporte(reporte uuid, accion text, nota text DEFAULT ''::text)
 RETURNS moderacion.content_reports
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
  r public.content_reports;
  nuevo TEXT;
  tipo_reg public.report_target_types;
  autor UUID;
BEGIN
  IF NOT public.es_moderador() THEN RAISE EXCEPTION 'solo moderadores'; END IF;
  SELECT * INTO r FROM public.content_reports WHERE id = reporte FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'reporte no encontrado'; END IF;
  IF accion NOT IN ('en_revision', 'resuelto', 'descartado', 'reabierto', 'ocultar_contenido', 'suspender_usuario', 'nota') THEN
    RAISE EXCEPTION 'accion no valida';
  END IF;
  IF accion = 'suspender_usuario' AND NOT public.is_admin() THEN
    RAISE EXCEPTION 'solo administradores pueden suspender';
  END IF;

  nuevo := CASE accion
    WHEN 'en_revision' THEN 'en_revision'
    WHEN 'reabierto' THEN 'pendiente'
    WHEN 'descartado' THEN 'descartado'
    WHEN 'nota' THEN r.status
    ELSE 'resuelto' END;

  SELECT * INTO tipo_reg FROM public.report_target_types WHERE key = r.target_type;
  IF accion = 'ocultar_contenido' THEN
    IF tipo_reg.ocultar_fn IS NULL THEN RAISE EXCEPTION 'este tipo no se puede ocultar'; END IF;
    EXECUTE format('SELECT %s($1)', tipo_reg.ocultar_fn) USING r.target_id;
  ELSIF accion = 'suspender_usuario' THEN
    IF tipo_reg.autor_fn IS NULL THEN RAISE EXCEPTION 'este tipo no tiene autor'; END IF;
    EXECUTE format('SELECT %s($1)', tipo_reg.autor_fn) INTO autor USING r.target_id;
    UPDATE public.profiles SET suspended_at = now() WHERE id = autor;
  END IF;

  UPDATE public.content_reports SET
    status = nuevo,
    assigned_to = coalesce(assigned_to, auth.uid()),
    resolution_note = CASE WHEN coalesce(nota, '') <> '' THEN left(nota, 500) ELSE resolution_note END,
    resolved_by = CASE WHEN nuevo IN ('resuelto', 'descartado') THEN auth.uid() ELSE NULL END,
    resolved_at = CASE WHEN nuevo IN ('resuelto', 'descartado') THEN now() ELSE NULL END,
    updated_at = now()
  WHERE id = reporte RETURNING * INTO r;

  INSERT INTO public.content_report_actions (report_id, moderator_id, action, note)
  VALUES (reporte, auth.uid(), accion, left(coalesce(nota, ''), 500));

  IF nuevo IN ('resuelto', 'descartado') AND accion <> 'nota' AND tipo_reg.resolver_fn IS NOT NULL THEN
    EXECUTE format('SELECT %s($1, $2)', tipo_reg.resolver_fn) USING r.target_id, nuevo;
  END IF;

  IF r.legacy_post_report_id IS NOT NULL THEN
    UPDATE public.post_reports SET status = nuevo, resolved_by = r.resolved_by WHERE id = r.legacy_post_report_id;
  END IF;

  IF nuevo IN ('resuelto', 'descartado') AND accion <> 'nota' THEN
    INSERT INTO public.notifications (user_id, type, actor_name, message)
    VALUES (r.reporter_id, 'reporte', 'Moderación Gacelas',
      CASE WHEN nuevo = 'resuelto'
        THEN 'Revisamos tu reporte sobre ' || coalesce(tipo_reg.referencia, 'el contenido') || ' y tomamos medidas. Gracias por avisar.'
        ELSE 'Revisamos tu reporte sobre ' || coalesce(tipo_reg.referencia, 'el contenido') || ' y no encontramos una infracción de las normas.' END);
  END IF;
  RETURN r;
END $function$

;

UPDATE avisos.notifications SET actor_name = 'Gacelas Loja' WHERE actor_name = 'Runcity Loja';
UPDATE avisos.notifications SET actor_name = 'Moderación Gacelas' WHERE actor_name = 'Moderación Runcity';
UPDATE avisos.notifications SET message = replace(message, 'Bienvenido a Runcity', 'Bienvenido a Gacelas')
  WHERE message LIKE '%Bienvenido a Runcity%';
COMMIT;
