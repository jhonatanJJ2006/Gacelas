-- 023: modulo de reportes.
--  1. Estadisticas del panel /admin por rango de fechas (solo is_admin()).
--  2. Reporte personal del corredor (cada usuario ve solo lo suyo).
--  3. Reportes de contenido ampliados: publicaciones, usuarios, comentarios y eventos, con
--     estados, historial de acciones del moderador y aviso al reportante.
-- Idempotente. post_reports (020) se mantiene: lo que llega ahi se copia a content_reports.

-- 3. Reportes de contenido ------------------------------------------------------
-- Tipos de objetivo registrables: la cola es generica. Otro contexto (por ejemplo incidentes)
-- se enchufa con un INSERT en report_target_types y sus funciones de resumen, ocultar y autor,
-- sin tocar content_reports, moderar_reporte ni admin_cola_reportes.
CREATE TABLE IF NOT EXISTS public.report_target_types (
  key TEXT PRIMARY KEY CHECK (key ~ '^[a-z_]{2,30}$'),
  etiqueta TEXT NOT NULL,
  referencia TEXT NOT NULL,
  resumen_fn REGPROC,
  ocultar_fn REGPROC,
  autor_fn REGPROC,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE public.report_target_types ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Tipos de reporte visibles" ON public.report_target_types;
CREATE POLICY "Tipos de reporte visibles" ON public.report_target_types FOR SELECT TO authenticated USING (true);
-- Sin politicas de escritura: solo migraciones o service role.

-- Funciones por tipo (target_id TEXT). Se ejecutan dentro de moderar_reporte y admin_cola_reportes.
CREATE OR REPLACE FUNCTION public.moderacion_resumen_post(objetivo TEXT) RETURNS TEXT
LANGUAGE sql STABLE SET search_path = public AS $$ SELECT a.title FROM public.activities a WHERE a.id::text = objetivo $$;
CREATE OR REPLACE FUNCTION public.moderacion_ocultar_post(objetivo TEXT) RETURNS VOID
LANGUAGE sql SET search_path = public AS $$ DELETE FROM public.activities WHERE id::text = objetivo $$;
CREATE OR REPLACE FUNCTION public.moderacion_autor_post(objetivo TEXT) RETURNS UUID
LANGUAGE sql STABLE SET search_path = public AS $$ SELECT a.user_id FROM public.activities a WHERE a.id::text = objetivo $$;

CREATE OR REPLACE FUNCTION public.moderacion_resumen_comentario(objetivo TEXT) RETURNS TEXT
LANGUAGE sql STABLE SET search_path = public AS $$ SELECT left(c.body, 140) FROM public.post_comments c WHERE c.id::text = objetivo $$;
CREATE OR REPLACE FUNCTION public.moderacion_ocultar_comentario(objetivo TEXT) RETURNS VOID
LANGUAGE sql SET search_path = public AS $$ DELETE FROM public.post_comments WHERE id::text = objetivo $$;
CREATE OR REPLACE FUNCTION public.moderacion_autor_comentario(objetivo TEXT) RETURNS UUID
LANGUAGE sql STABLE SET search_path = public AS $$ SELECT c.user_id FROM public.post_comments c WHERE c.id::text = objetivo $$;

CREATE OR REPLACE FUNCTION public.moderacion_resumen_evento(objetivo TEXT) RETURNS TEXT
LANGUAGE sql STABLE SET search_path = public AS $$ SELECT e.title FROM public.events e WHERE e.id::text = objetivo $$;
CREATE OR REPLACE FUNCTION public.moderacion_ocultar_evento(objetivo TEXT) RETURNS VOID
LANGUAGE sql SET search_path = public AS $$ UPDATE public.events SET published = false WHERE id::text = objetivo $$;
CREATE OR REPLACE FUNCTION public.moderacion_autor_evento(objetivo TEXT) RETURNS UUID
LANGUAGE sql STABLE SET search_path = public AS $$ SELECT e.created_by FROM public.events e WHERE e.id::text = objetivo $$;

CREATE OR REPLACE FUNCTION public.moderacion_resumen_usuario(objetivo TEXT) RETURNS TEXT
LANGUAGE sql STABLE SET search_path = public AS $$ SELECT coalesce(u.full_name, u.username) FROM public.profiles u WHERE u.id::text = objetivo $$;
CREATE OR REPLACE FUNCTION public.moderacion_ocultar_usuario(objetivo TEXT) RETURNS VOID
LANGUAGE sql SET search_path = public AS $$ UPDATE public.profiles SET suspended_at = now() WHERE id::text = objetivo $$;
CREATE OR REPLACE FUNCTION public.moderacion_autor_usuario(objetivo TEXT) RETURNS UUID
LANGUAGE sql STABLE SET search_path = public AS $$ SELECT u.id FROM public.profiles u WHERE u.id::text = objetivo $$;

DO $$
DECLARE t TEXT; f TEXT;
BEGIN
  FOREACH t IN ARRAY ARRAY['post', 'comentario', 'evento', 'usuario'] LOOP
    FOREACH f IN ARRAY ARRAY['resumen', 'ocultar', 'autor'] LOOP
      EXECUTE format('REVOKE ALL ON FUNCTION public.moderacion_%s_%s(TEXT) FROM PUBLIC', f, t);
    END LOOP;
  END LOOP;
END $$;

INSERT INTO public.report_target_types (key, etiqueta, referencia, resumen_fn, ocultar_fn, autor_fn) VALUES
  ('post', 'Publicación', 'la publicación', 'public.moderacion_resumen_post', 'public.moderacion_ocultar_post', 'public.moderacion_autor_post'),
  ('comentario', 'Comentario', 'el comentario', 'public.moderacion_resumen_comentario', 'public.moderacion_ocultar_comentario', 'public.moderacion_autor_comentario'),
  ('evento', 'Evento', 'el evento', 'public.moderacion_resumen_evento', 'public.moderacion_ocultar_evento', 'public.moderacion_autor_evento'),
  ('usuario', 'Usuario', 'la cuenta', 'public.moderacion_resumen_usuario', 'public.moderacion_ocultar_usuario', 'public.moderacion_autor_usuario')
ON CONFLICT (key) DO UPDATE SET etiqueta = EXCLUDED.etiqueta, referencia = EXCLUDED.referencia,
  resumen_fn = EXCLUDED.resumen_fn, ocultar_fn = EXCLUDED.ocultar_fn, autor_fn = EXCLUDED.autor_fn;

CREATE TABLE IF NOT EXISTS public.content_reports (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  target_type TEXT NOT NULL REFERENCES public.report_target_types(key),
  target_id TEXT NOT NULL,
  reporter_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  reason TEXT NOT NULL CHECK (reason IN ('spam', 'ofensivo', 'acoso', 'falso', 'suplantacion', 'peligroso', 'otro')),
  detail TEXT NOT NULL DEFAULT '' CHECK (char_length(detail) <= 500),
  status TEXT NOT NULL DEFAULT 'pendiente' CHECK (status IN ('pendiente', 'en_revision', 'resuelto', 'descartado')),
  assigned_to UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  resolution_note TEXT NOT NULL DEFAULT '' CHECK (char_length(resolution_note) <= 500),
  resolved_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  resolved_at TIMESTAMPTZ,
  legacy_post_report_id UUID UNIQUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (target_type, target_id, reporter_id)
);
-- Si la tabla venia de una version anterior con CHECK fijo, se cambia por la clave foranea.
ALTER TABLE public.content_reports DROP CONSTRAINT IF EXISTS content_reports_target_type_check;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'content_reports_target_type_fkey') THEN
    ALTER TABLE public.content_reports ADD CONSTRAINT content_reports_target_type_fkey
      FOREIGN KEY (target_type) REFERENCES public.report_target_types(key);
  END IF;
END $$;

-- Resumen legible del objetivo, segun el tipo registrado.
CREATE OR REPLACE FUNCTION public.moderacion_resumen(tipo TEXT, objetivo TEXT) RETURNS TEXT
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public AS $$
DECLARE f REGPROC; res TEXT;
BEGIN
  SELECT resumen_fn INTO f FROM public.report_target_types WHERE key = tipo;
  IF f IS NULL THEN RETURN NULL; END IF;
  EXECUTE format('SELECT %s($1)', f) INTO res USING objetivo;
  RETURN res;
END $$;
REVOKE ALL ON FUNCTION public.moderacion_resumen(TEXT, TEXT) FROM PUBLIC;

CREATE INDEX IF NOT EXISTS content_reports_status_idx ON public.content_reports(status, created_at DESC);
CREATE INDEX IF NOT EXISTS content_reports_target_idx ON public.content_reports(target_type, target_id);
ALTER TABLE public.content_reports ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Reportar contenido" ON public.content_reports;
CREATE POLICY "Reportar contenido" ON public.content_reports FOR INSERT TO authenticated
  WITH CHECK (reporter_id = auth.uid() AND status = 'pendiente' AND resolved_by IS NULL
    AND NOT (target_type = 'usuario' AND target_id = auth.uid()::text));
DROP POLICY IF EXISTS "Ver mis reportes de contenido o todos si modera" ON public.content_reports;
CREATE POLICY "Ver mis reportes de contenido o todos si modera" ON public.content_reports FOR SELECT TO authenticated
  USING (reporter_id = auth.uid() OR public.es_moderador());
DROP POLICY IF EXISTS "Moderar reportes de contenido" ON public.content_reports;
CREATE POLICY "Moderar reportes de contenido" ON public.content_reports FOR UPDATE TO authenticated
  USING (public.es_moderador()) WITH CHECK (public.es_moderador());

CREATE TABLE IF NOT EXISTS public.content_report_actions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  report_id UUID NOT NULL REFERENCES public.content_reports(id) ON DELETE CASCADE,
  moderator_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  action TEXT NOT NULL CHECK (action IN ('en_revision', 'resuelto', 'descartado', 'reabierto',
    'ocultar_contenido', 'suspender_usuario', 'nota')),
  note TEXT NOT NULL DEFAULT '' CHECK (char_length(note) <= 500),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS content_report_actions_report_idx ON public.content_report_actions(report_id, created_at);
ALTER TABLE public.content_report_actions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Moderadores ven el historial" ON public.content_report_actions;
CREATE POLICY "Moderadores ven el historial" ON public.content_report_actions FOR SELECT TO authenticated
  USING (public.es_moderador());
-- Las acciones solo se escriben con moderar_reporte() (SECURITY DEFINER).

-- post_reports admite tambien 'en_revision' y sigue funcionando para las apps ya publicadas.
ALTER TABLE public.post_reports DROP CONSTRAINT IF EXISTS post_reports_status_check;
ALTER TABLE public.post_reports ADD CONSTRAINT post_reports_status_check
  CHECK (status IN ('pendiente', 'en_revision', 'resuelto', 'descartado'));

CREATE OR REPLACE FUNCTION public.copiar_post_report() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  INSERT INTO public.content_reports (target_type, target_id, reporter_id, reason, detail, status,
    resolved_by, created_at, legacy_post_report_id)
  VALUES ('post', NEW.post_id, NEW.reporter_id, NEW.reason, NEW.detail, NEW.status, NEW.resolved_by,
    NEW.created_at, NEW.id)
  ON CONFLICT DO NOTHING;
  RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS post_reports_copiar ON public.post_reports;
CREATE TRIGGER post_reports_copiar AFTER INSERT ON public.post_reports
  FOR EACH ROW EXECUTE FUNCTION public.copiar_post_report();

-- Reportes viejos de publicaciones.
INSERT INTO public.content_reports (target_type, target_id, reporter_id, reason, detail, status,
  resolved_by, created_at, legacy_post_report_id)
SELECT 'post', pr.post_id, pr.reporter_id, pr.reason, pr.detail, pr.status, pr.resolved_by, pr.created_at, pr.id
FROM public.post_reports pr
ON CONFLICT DO NOTHING;

-- Aviso al reportante.
ALTER TABLE public.notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE public.notifications ADD CONSTRAINT notifications_type_check
  CHECK (type IN ('like', 'comment', 'follow', 'event', 'live', 'streak', 'club', 'reporte'));

CREATE OR REPLACE FUNCTION public.moderar_reporte(reporte UUID, accion TEXT, nota TEXT DEFAULT '')
RETURNS public.content_reports
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
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
END $$;
GRANT EXECUTE ON FUNCTION public.moderar_reporte(UUID, TEXT, TEXT) TO authenticated;

-- Cola de moderacion con un resumen del contenido reportado.
CREATE OR REPLACE FUNCTION public.admin_cola_reportes(estado TEXT DEFAULT NULL, tipo TEXT DEFAULT NULL)
RETURNS TABLE (id UUID, target_type TEXT, target_id TEXT, reason TEXT, detail TEXT, status TEXT,
               created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ, reportante TEXT, resumen TEXT,
               total_reportes BIGINT, acciones JSONB)
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.es_moderador() THEN RAISE EXCEPTION 'solo moderadores'; END IF;
  RETURN QUERY
    SELECT c.id, c.target_type, c.target_id, c.reason, c.detail, c.status, c.created_at, c.updated_at,
      coalesce(p.full_name, p.username, ''),
      coalesce(public.moderacion_resumen(c.target_type, c.target_id), '(contenido eliminado)'),
      (SELECT count(*) FROM public.content_reports x WHERE x.target_type = c.target_type AND x.target_id = c.target_id),
      coalesce((SELECT jsonb_agg(jsonb_build_object('accion', h.action, 'nota', h.note, 'fecha', h.created_at,
        'moderador', coalesce(m.full_name, m.username, '')) ORDER BY h.created_at)
        FROM public.content_report_actions h LEFT JOIN public.profiles m ON m.id = h.moderator_id
        WHERE h.report_id = c.id), '[]'::jsonb)
    FROM public.content_reports c
    LEFT JOIN public.profiles p ON p.id = c.reporter_id
    WHERE (estado IS NULL OR c.status = estado) AND (tipo IS NULL OR c.target_type = tipo)
    ORDER BY (c.status IN ('pendiente', 'en_revision')) DESC, c.created_at DESC
    LIMIT 300;
END $$;
GRANT EXECUTE ON FUNCTION public.admin_cola_reportes(TEXT, TEXT) TO authenticated;

-- 1. Estadisticas del panel -----------------------------------------------------
CREATE OR REPLACE FUNCTION public.admin_estadisticas(desde DATE, hasta DATE)
RETURNS JSONB LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public AS $$
DECLARE
  dias INTEGER;
  ini TIMESTAMPTZ;
  fin TIMESTAMPTZ;
  ini_prev TIMESTAMPTZ;
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'solo administradores'; END IF;
  IF desde IS NULL OR hasta IS NULL OR hasta < desde THEN RAISE EXCEPTION 'rango no valido'; END IF;
  IF hasta - desde > 731 THEN RAISE EXCEPTION 'el rango maximo es de dos años'; END IF;
  dias := hasta - desde + 1;
  ini := desde::timestamptz;
  fin := (hasta + 1)::timestamptz;
  ini_prev := ini - make_interval(days => dias);
  RETURN jsonb_build_object(
    'desde', desde, 'hasta', hasta,
    'totales', jsonb_build_object(
      'usuarios_nuevos', (SELECT count(*) FROM profiles WHERE created_at >= ini AND created_at < fin),
      'usuarios_activos', (SELECT count(DISTINCT user_id) FROM activities WHERE start_time >= ini AND start_time < fin),
      'carreras', (SELECT count(*) FROM activities WHERE start_time >= ini AND start_time < fin),
      'km', (SELECT coalesce(round(sum(distance_meters) / 1000.0, 1), 0) FROM activities WHERE start_time >= ini AND start_time < fin),
      'eventos', (SELECT count(*) FROM events WHERE starts_at >= ini AND starts_at < fin),
      'inscripciones', (SELECT count(*) FROM event_rsvps WHERE created_at >= ini AND created_at < fin),
      'reportes', (SELECT count(*) FROM content_reports WHERE created_at >= ini AND created_at < fin),
      'usuarios_total', (SELECT count(*) FROM profiles)
    ),
    'serie', (SELECT coalesce(jsonb_agg(jsonb_build_object(
        'dia', d::date,
        'nuevos', (SELECT count(*) FROM profiles p WHERE p.created_at >= d AND p.created_at < d + interval '1 day'),
        'activos', (SELECT count(DISTINCT a.user_id) FROM activities a WHERE a.start_time >= d AND a.start_time < d + interval '1 day'),
        'carreras', (SELECT count(*) FROM activities a WHERE a.start_time >= d AND a.start_time < d + interval '1 day'),
        'km', (SELECT coalesce(round(sum(a.distance_meters) / 1000.0, 1), 0) FROM activities a WHERE a.start_time >= d AND a.start_time < d + interval '1 day')
      ) ORDER BY d), '[]'::jsonb)
      FROM generate_series(ini, fin - interval '1 day', interval '1 day') AS d),
    'rutas_top', (SELECT coalesce(jsonb_agg(x ORDER BY x->>'corridas' DESC), '[]'::jsonb) FROM (
        SELECT jsonb_build_object('id', r.id, 'nombre', r.name, 'oficial', coalesce(r.is_official, false),
          'corridas', count(*), 'corredores', count(DISTINCT rr.user_id)) AS x
        FROM route_runs rr JOIN routes r ON r.id = rr.route_id
        WHERE rr.created_at >= ini AND rr.created_at < fin
        GROUP BY r.id, r.name, r.is_official ORDER BY count(*) DESC LIMIT 10) t),
    'eventos_top', (SELECT coalesce(jsonb_agg(x), '[]'::jsonb) FROM (
        SELECT jsonb_build_object('id', e.id, 'titulo', e.title, 'fecha', e.starts_at,
          'inscritos', (SELECT count(*) FROM event_rsvps v WHERE v.event_id = e.id)) AS x
        FROM events e WHERE e.starts_at >= ini AND e.starts_at < fin
        ORDER BY (SELECT count(*) FROM event_rsvps v WHERE v.event_id = e.id) DESC LIMIT 10) t),
    -- Retencion: corredores activos en el periodo anterior (misma duracion) que volvieron a correr en este.
    'retencion', (WITH previos AS (
        SELECT DISTINCT user_id FROM activities WHERE start_time >= ini_prev AND start_time < ini),
      vuelven AS (SELECT DISTINCT a.user_id FROM activities a JOIN previos p USING (user_id)
        WHERE a.start_time >= ini AND a.start_time < fin)
      SELECT jsonb_build_object('previos', (SELECT count(*) FROM previos), 'vuelven', (SELECT count(*) FROM vuelven),
        'porcentaje', CASE WHEN (SELECT count(*) FROM previos) = 0 THEN 0
          ELSE round(100.0 * (SELECT count(*) FROM vuelven) / (SELECT count(*) FROM previos), 1) END)),
    'reportes', jsonb_build_object(
      'por_estado', (SELECT coalesce(jsonb_object_agg(status, n), '{}'::jsonb) FROM (
          SELECT status, count(*) n FROM content_reports WHERE created_at >= ini AND created_at < fin GROUP BY status) t),
      'por_tipo', (SELECT coalesce(jsonb_object_agg(target_type, n), '{}'::jsonb) FROM (
          SELECT target_type, count(*) n FROM content_reports WHERE created_at >= ini AND created_at < fin GROUP BY target_type) t),
      'por_motivo', (SELECT coalesce(jsonb_object_agg(reason, n), '{}'::jsonb) FROM (
          SELECT reason, count(*) n FROM content_reports WHERE created_at >= ini AND created_at < fin GROUP BY reason) t)
    )
  );
END $$;
GRANT EXECUTE ON FUNCTION public.admin_estadisticas(DATE, DATE) TO authenticated;

-- El tablero tambien cuenta los reportes nuevos (todos los tipos).
CREATE OR REPLACE FUNCTION public.admin_metricas()
RETURNS JSONB LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'solo administradores'; END IF;
  RETURN jsonb_build_object(
    'usuarios', (SELECT count(*) FROM public.profiles),
    'usuarios_7d', (SELECT count(*) FROM public.profiles WHERE created_at > now() - interval '7 days'),
    'carreras_7d', (SELECT count(*) FROM public.activities WHERE start_time > now() - interval '7 days'),
    'km_7d', (SELECT coalesce(round(sum(distance_meters) / 1000.0, 1), 0) FROM public.activities WHERE start_time > now() - interval '7 days'),
    'reportes_pendientes', (SELECT count(*) FROM public.content_reports WHERE status IN ('pendiente', 'en_revision')),
    'eventos_proximos', (SELECT count(*) FROM public.events WHERE starts_at > now()),
    'clubes', (SELECT count(*) FROM public.clubs),
    'rutas', (SELECT count(*) FROM public.routes)
  );
END $$;
GRANT EXECUTE ON FUNCTION public.admin_metricas() TO authenticated;

-- 2. Reporte personal -----------------------------------------------------------
-- SECURITY INVOKER y filtro por auth.uid(): cada persona solo obtiene sus propias carreras.
CREATE OR REPLACE FUNCTION public.reporte_personal(periodo TEXT, referencia DATE DEFAULT current_date)
RETURNS JSONB LANGUAGE plpgsql STABLE SECURITY INVOKER SET search_path = public AS $$
DECLARE
  yo UUID := auth.uid();
  paso INTERVAL;
  ini TIMESTAMPTZ;
  fin TIMESTAMPTZ;
  ini_prev TIMESTAMPTZ;
  unidad TEXT;
BEGIN
  IF yo IS NULL THEN RAISE EXCEPTION 'sin sesion'; END IF;
  IF periodo NOT IN ('semana', 'mes', 'anio') THEN RAISE EXCEPTION 'periodo no valido'; END IF;
  unidad := CASE periodo WHEN 'semana' THEN 'week' WHEN 'mes' THEN 'month' ELSE 'year' END;
  paso := CASE periodo WHEN 'semana' THEN interval '1 week' WHEN 'mes' THEN interval '1 month' ELSE interval '1 year' END;
  ini := date_trunc(unidad, referencia::timestamptz);
  fin := ini + paso;
  ini_prev := ini - paso;
  RETURN (
    WITH mias AS (
      SELECT a.* FROM activities a WHERE a.user_id = yo AND a.start_time >= ini_prev AND a.start_time < fin
    ), actual AS (SELECT * FROM mias WHERE start_time >= ini), previo AS (SELECT * FROM mias WHERE start_time < ini),
    resumen AS (
      SELECT 'actual' AS cual, count(*) carreras, coalesce(sum(distance_meters), 0) metros,
        coalesce(sum(duration_seconds), 0) segundos, coalesce(sum(elevation_gain_meters), 0) desnivel FROM actual
      UNION ALL
      SELECT 'previo', count(*), coalesce(sum(distance_meters), 0), coalesce(sum(duration_seconds), 0),
        coalesce(sum(elevation_gain_meters), 0) FROM previo
    ), dias AS (
      SELECT DISTINCT (start_time AT TIME ZONE 'America/Guayaquil')::date d FROM activities
      WHERE user_id = yo AND start_time < fin
    ), racha AS (
      SELECT coalesce(max(n), 0) mejor FROM (
        SELECT count(*) n FROM (SELECT d, d - (row_number() OVER (ORDER BY d))::int g FROM dias
          WHERE d >= ini::date) x GROUP BY g) y
    )
    SELECT jsonb_build_object(
      'periodo', periodo, 'desde', ini::date, 'hasta', (fin - interval '1 day')::date,
      'actual', (SELECT to_jsonb(r) - 'cual' FROM resumen r WHERE cual = 'actual'),
      'previo', (SELECT to_jsonb(r) - 'cual' FROM resumen r WHERE cual = 'previo'),
      'ritmo_medio_seg_km', (SELECT CASE WHEN sum(distance_meters) > 0
        THEN round(sum(duration_seconds) * 1000.0 / sum(distance_meters)) END FROM actual),
      'mejores', jsonb_build_object(
        'mas_larga_m', (SELECT max(distance_meters) FROM actual),
        'mejor_ritmo_seg_km', (SELECT min(round(duration_seconds * 1000.0 / distance_meters)) FROM actual WHERE distance_meters >= 1000),
        'mas_desnivel_m', (SELECT max(elevation_gain_meters) FROM actual)),
      'racha_dias', (SELECT mejor FROM racha),
      'rutas_favoritas', (SELECT coalesce(jsonb_agg(x), '[]'::jsonb) FROM (
        SELECT jsonb_build_object('id', r.id, 'nombre', r.name, 'veces', count(*)) x
        FROM route_runs rr JOIN routes r ON r.id = rr.route_id
        WHERE rr.user_id = yo AND rr.created_at >= ini AND rr.created_at < fin
        GROUP BY r.id, r.name ORDER BY count(*) DESC LIMIT 5) t),
      'serie', (SELECT coalesce(jsonb_agg(jsonb_build_object('inicio', b::date,
          'metros', (SELECT coalesce(sum(distance_meters), 0) FROM actual a
            WHERE a.start_time >= b AND a.start_time < b + CASE periodo WHEN 'anio' THEN interval '1 month' ELSE interval '1 day' END)) ORDER BY b), '[]'::jsonb)
        FROM generate_series(ini, fin - interval '1 second',
          CASE periodo WHEN 'anio' THEN interval '1 month' ELSE interval '1 day' END) b)
    )
  );
END $$;
GRANT EXECUTE ON FUNCTION public.reporte_personal(TEXT, DATE) TO authenticated;
