-- 032: fase F7 de DDD, etapa 1. Crea un esquema por contexto acotado y el esquema kernel.
-- Las tablas siguen en public: aqui solo se registra a que contexto pertenece cada una y se
-- publican vistas de lectura (security_invoker, respetan RLS) en el esquema de su contexto.
-- Mover las tablas queda para la etapa 2 (ver docs/ARQUITECTURA-DDD.md). No cambia nada que
-- usen la web ni Android: PostgREST sigue exponiendo solo public.
BEGIN;

CREATE SCHEMA IF NOT EXISTS kernel;
CREATE SCHEMA IF NOT EXISTS identidad;
CREATE SCHEMA IF NOT EXISTS legal;
CREATE SCHEMA IF NOT EXISTS actividad;
CREATE SCHEMA IF NOT EXISTS rutas;
CREATE SCHEMA IF NOT EXISTS envivo;
CREATE SCHEMA IF NOT EXISTS seguridad;
CREATE SCHEMA IF NOT EXISTS comunidad;
CREATE SCHEMA IF NOT EXISTS eventos;
CREATE SCHEMA IF NOT EXISTS logros;
CREATE SCHEMA IF NOT EXISTS moderacion;
CREATE SCHEMA IF NOT EXISTS avisos;
CREATE SCHEMA IF NOT EXISTS admin;
CREATE SCHEMA IF NOT EXISTS analitica;

-- Propiedad de cada tabla por contexto; la usa la prueba backend/tests/rls/032_esquemas_contexto.sql.
CREATE TABLE IF NOT EXISTS kernel.propiedad_tablas (
  tabla TEXT PRIMARY KEY,
  contexto TEXT NOT NULL,
  esquema_actual TEXT NOT NULL DEFAULT 'public'
);
ALTER TABLE kernel.propiedad_tablas ENABLE ROW LEVEL SECURITY;
INSERT INTO kernel.propiedad_tablas (tabla, contexto) VALUES
  ('profiles', 'identidad'),
  ('user_settings', 'identidad'),
  ('legal_versions', 'legal'),
  ('consents', 'legal'),
  ('activities', 'actividad'),
  ('activity_tracks', 'actividad'),
  ('track_points', 'actividad'),
  ('track_batches', 'actividad'),
  ('activity_summaries', 'actividad'),
  ('routes', 'rutas'),
  ('route_reviews', 'rutas'),
  ('route_likes', 'rutas'),
  ('route_photos', 'rutas'),
  ('route_runs', 'rutas'),
  ('saved_routes', 'rutas'),
  ('runner_positions', 'envivo'),
  ('live_sessions', 'envivo'),
  ('live_private', 'envivo'),
  ('live_sharing_settings', 'envivo'),
  ('parroquias', 'seguridad'),
  ('incidentes_agregados', 'seguridad'),
  ('importaciones_ecu911', 'seguridad'),
  ('incidentes_usuario', 'seguridad'),
  ('incidentes_confirmaciones', 'seguridad'),
  ('post_images', 'comunidad'),
  ('post_likes', 'comunidad'),
  ('post_comments', 'comunidad'),
  ('post_reposts', 'comunidad'),
  ('follows', 'comunidad'),
  ('clubs', 'eventos'),
  ('club_members', 'eventos'),
  ('club_posts', 'eventos'),
  ('events', 'eventos'),
  ('event_rsvps', 'eventos'),
  ('event_reminders', 'eventos'),
  ('user_streaks', 'logros'),
  ('user_achievements', 'logros'),
  ('post_reports', 'moderacion'),
  ('report_target_types', 'moderacion'),
  ('content_reports', 'moderacion'),
  ('content_report_actions', 'moderacion'),
  ('notifications', 'avisos'),
  ('user_roles', 'admin'),
  ('app_settings', 'admin'),
  ('app_releases', 'admin')
ON CONFLICT (tabla) DO UPDATE SET contexto = EXCLUDED.contexto;

-- Outbox de eventos de dominio entre contextos. Solo service role (RLS sin politicas).
CREATE TABLE IF NOT EXISTS kernel.eventos_salida (
  id BIGSERIAL PRIMARY KEY,
  contexto TEXT NOT NULL,
  tipo TEXT NOT NULL,
  agregado_id TEXT,
  datos JSONB NOT NULL DEFAULT '{}'::jsonb,
  creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  procesado_en TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS eventos_salida_pendientes ON kernel.eventos_salida (creado_en) WHERE procesado_en IS NULL;
ALTER TABLE kernel.eventos_salida ENABLE ROW LEVEL SECURITY;

-- Vistas de lectura por contexto sobre las tablas de public (solo si la tabla existe).
DO $$
DECLARE r RECORD;
BEGIN
  FOR r IN SELECT p.tabla, p.contexto FROM kernel.propiedad_tablas p
    WHERE to_regclass('public.' || p.tabla) IS NOT NULL LOOP
    EXECUTE format('CREATE OR REPLACE VIEW %I.%I WITH (security_invoker = true) AS SELECT * FROM public.%I',
      r.contexto, r.tabla, r.tabla);
  END LOOP;
END $$;

GRANT USAGE ON SCHEMA kernel, identidad, legal, actividad, rutas, envivo, seguridad, comunidad, eventos, logros, moderacion, avisos, admin, analitica TO service_role;
GRANT SELECT ON ALL TABLES IN SCHEMA identidad, legal, actividad, rutas, envivo, seguridad, comunidad, eventos, logros, moderacion, avisos, admin, analitica TO service_role;
GRANT ALL ON ALL TABLES IN SCHEMA kernel TO service_role;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA kernel TO service_role;

COMMIT;
