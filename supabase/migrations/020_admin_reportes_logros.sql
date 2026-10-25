-- 020: roles de administracion, reportes de publicaciones, logros y ajustes de la app.
-- Todo con RLS. Solo quien tenga fila 'admin' en user_roles puede usar el panel /admin.

-- Roles ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.user_roles (
  user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  role TEXT NOT NULL CHECK (role IN ('admin', 'moderador')),
  granted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;

-- SECURITY DEFINER para que las politicas puedan consultar user_roles sin recursion.
CREATE OR REPLACE FUNCTION public.is_admin(uid UUID DEFAULT auth.uid())
RETURNS BOOLEAN LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT EXISTS (SELECT 1 FROM public.user_roles WHERE user_id = uid AND role = 'admin')
$$;
CREATE OR REPLACE FUNCTION public.es_moderador(uid UUID DEFAULT auth.uid())
RETURNS BOOLEAN LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT EXISTS (SELECT 1 FROM public.user_roles WHERE user_id = uid AND role IN ('admin', 'moderador'))
$$;
GRANT EXECUTE ON FUNCTION public.is_admin(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.es_moderador(UUID) TO authenticated;

DROP POLICY IF EXISTS "Ver mi rol o todos si admin" ON public.user_roles;
CREATE POLICY "Ver mi rol o todos si admin" ON public.user_roles FOR SELECT TO authenticated
  USING (user_id = auth.uid() OR public.is_admin());
DROP POLICY IF EXISTS "Admin gestiona roles" ON public.user_roles;
CREATE POLICY "Admin gestiona roles" ON public.user_roles FOR ALL TO authenticated
  USING (public.is_admin()) WITH CHECK (public.is_admin());

-- Suspension de cuentas -----------------------------------------------------
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMPTZ;

CREATE OR REPLACE FUNCTION public.admin_suspender(uid UUID, suspender BOOLEAN)
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'solo administradores'; END IF;
  UPDATE public.profiles SET suspended_at = CASE WHEN suspender THEN now() ELSE NULL END WHERE id = uid;
END $$;
GRANT EXECUTE ON FUNCTION public.admin_suspender(UUID, BOOLEAN) TO authenticated;

-- Listado de usuarios para el panel (los perfiles privados no son visibles por RLS normal).
CREATE OR REPLACE FUNCTION public.admin_usuarios()
RETURNS TABLE (id UUID, username TEXT, full_name TEXT, avatar_url TEXT, created_at TIMESTAMPTZ,
               suspended_at TIMESTAMPTZ, role TEXT, carreras BIGINT, km NUMERIC)
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'solo administradores'; END IF;
  RETURN QUERY
    SELECT p.id, p.username, p.full_name, p.avatar_url, p.created_at, p.suspended_at, r.role,
           count(a.id), coalesce(round(sum(a.distance_meters) / 1000.0, 1), 0)
    FROM public.profiles p
    LEFT JOIN public.user_roles r ON r.user_id = p.id
    LEFT JOIN public.activities a ON a.user_id = p.id
    GROUP BY p.id, r.role
    ORDER BY p.created_at DESC;
END $$;
GRANT EXECUTE ON FUNCTION public.admin_usuarios() TO authenticated;

-- Reportes de publicaciones -------------------------------------------------
CREATE TABLE IF NOT EXISTS public.post_reports (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  post_id TEXT NOT NULL,
  reporter_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  reason TEXT NOT NULL CHECK (reason IN ('spam', 'ofensivo', 'acoso', 'falso', 'otro')),
  detail TEXT NOT NULL DEFAULT '' CHECK (char_length(detail) <= 500),
  status TEXT NOT NULL DEFAULT 'pendiente' CHECK (status IN ('pendiente', 'resuelto', 'descartado')),
  resolved_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (post_id, reporter_id)
);
CREATE INDEX IF NOT EXISTS post_reports_status_idx ON public.post_reports(status, created_at DESC);
ALTER TABLE public.post_reports ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Reportar publicacion" ON public.post_reports;
CREATE POLICY "Reportar publicacion" ON public.post_reports FOR INSERT TO authenticated
  WITH CHECK (reporter_id = auth.uid() AND status = 'pendiente');
DROP POLICY IF EXISTS "Ver mis reportes o todos si modera" ON public.post_reports;
CREATE POLICY "Ver mis reportes o todos si modera" ON public.post_reports FOR SELECT TO authenticated
  USING (reporter_id = auth.uid() OR public.es_moderador());
DROP POLICY IF EXISTS "Moderar reportes" ON public.post_reports;
CREATE POLICY "Moderar reportes" ON public.post_reports FOR UPDATE TO authenticated
  USING (public.es_moderador()) WITH CHECK (public.es_moderador());

-- Moderacion: admin/moderador puede borrar una publicacion reportada.
DROP POLICY IF EXISTS "Moderador borra publicaciones" ON public.activities;
CREATE POLICY "Moderador borra publicaciones" ON public.activities FOR DELETE TO authenticated
  USING (public.es_moderador());

-- Logros --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.user_achievements (
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  code TEXT NOT NULL CHECK (code IN ('primera_carrera', 'km_5', 'km_10', 'racha_7')),
  unlocked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, code)
);
ALTER TABLE public.user_achievements ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Ver logros" ON public.user_achievements;
CREATE POLICY "Ver logros" ON public.user_achievements FOR SELECT TO authenticated USING (true);
DROP POLICY IF EXISTS "Desbloquear mis logros" ON public.user_achievements;
CREATE POLICY "Desbloquear mis logros" ON public.user_achievements FOR INSERT TO authenticated
  WITH CHECK (user_id = auth.uid());

-- Ajustes de la app ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.app_settings (
  key TEXT PRIMARY KEY,
  value JSONB NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE public.app_settings ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Leer ajustes de la app" ON public.app_settings;
CREATE POLICY "Leer ajustes de la app" ON public.app_settings FOR SELECT USING (true);
DROP POLICY IF EXISTS "Admin edita ajustes" ON public.app_settings;
CREATE POLICY "Admin edita ajustes" ON public.app_settings FOR ALL TO authenticated
  USING (public.is_admin()) WITH CHECK (public.is_admin());
INSERT INTO public.app_settings (key, value) VALUES
  ('version_minima_android', '"1.0.0"'), ('registro_abierto', 'true'), ('mantenimiento', 'false')
ON CONFLICT (key) DO NOTHING;

-- Publicar una nueva version de los Terminos (las apps la piden de nuevo al cambiar).
DROP POLICY IF EXISTS "Admin publica versiones legales" ON public.legal_versions;
CREATE POLICY "Admin publica versiones legales" ON public.legal_versions FOR ALL TO authenticated
  USING (public.is_admin()) WITH CHECK (public.is_admin());

-- Eventos, rutas y clubes desde el panel ------------------------------------
ALTER TABLE public.events ADD COLUMN IF NOT EXISTS published BOOLEAN NOT NULL DEFAULT true;
DROP POLICY IF EXISTS "Admin gestiona eventos" ON public.events;
CREATE POLICY "Admin gestiona eventos" ON public.events FOR ALL TO authenticated
  USING (public.is_admin()) WITH CHECK (public.is_admin());
DROP POLICY IF EXISTS "Admin gestiona rutas" ON public.routes;
CREATE POLICY "Admin gestiona rutas" ON public.routes FOR ALL TO authenticated
  USING (public.is_admin()) WITH CHECK (public.is_admin());
DROP POLICY IF EXISTS "Admin gestiona clubes" ON public.clubs;
CREATE POLICY "Admin gestiona clubes" ON public.clubs FOR ALL TO authenticated
  USING (public.is_admin()) WITH CHECK (public.is_admin());

-- Metricas del tablero ------------------------------------------------------
CREATE OR REPLACE FUNCTION public.admin_metricas()
RETURNS JSONB LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'solo administradores'; END IF;
  RETURN jsonb_build_object(
    'usuarios', (SELECT count(*) FROM public.profiles),
    'usuarios_7d', (SELECT count(*) FROM public.profiles WHERE created_at > now() - interval '7 days'),
    'carreras_7d', (SELECT count(*) FROM public.activities WHERE start_time > now() - interval '7 days'),
    'km_7d', (SELECT coalesce(round(sum(distance_meters) / 1000.0, 1), 0) FROM public.activities WHERE start_time > now() - interval '7 days'),
    'reportes_pendientes', (SELECT count(*) FROM public.post_reports WHERE status = 'pendiente'),
    'eventos_proximos', (SELECT count(*) FROM public.events WHERE starts_at > now()),
    'clubes', (SELECT count(*) FROM public.clubs),
    'rutas', (SELECT count(*) FROM public.routes)
  );
END $$;
GRANT EXECUTE ON FUNCTION public.admin_metricas() TO authenticated;
