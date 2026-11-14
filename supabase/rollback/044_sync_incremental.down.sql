-- Reversion de 044_sync_incremental.sql.
-- No se aplica sola (no esta en supabase/migrations). Uso: psql -f este_archivo.
-- Las filas de sync_borrados se pierden: no hay otra copia de los DELETE.

BEGIN;

DROP TRIGGER IF EXISTS sync_borrado_activities ON public.activities;
DROP INDEX IF EXISTS public.activities_sync_idx;

DROP TRIGGER IF EXISTS sync_borrado_post_likes ON public.post_likes;
DROP TRIGGER IF EXISTS post_likes_updated_at ON public.post_likes;
DROP INDEX IF EXISTS public.post_likes_sync_idx;
ALTER TABLE public.post_likes DROP COLUMN IF EXISTS updated_at;

DROP TRIGGER IF EXISTS sync_borrado_post_comments ON public.post_comments;
DROP TRIGGER IF EXISTS post_comments_updated_at ON public.post_comments;
DROP INDEX IF EXISTS public.post_comments_sync_idx;
ALTER TABLE public.post_comments DROP COLUMN IF EXISTS updated_at;

-- Vistas: hay que quitarlas antes de la columna (la vista congela las columnas).
DROP VIEW IF EXISTS public.activity_tracks;
DROP TRIGGER IF EXISTS sync_borrado_activity_tracks ON actividad.activity_tracks;
DROP TRIGGER IF EXISTS activity_tracks_updated_at ON actividad.activity_tracks;
DROP INDEX IF EXISTS actividad.activity_tracks_sync_idx;
ALTER TABLE actividad.activity_tracks DROP COLUMN IF EXISTS updated_at;
CREATE VIEW public.activity_tracks WITH (security_invoker = true) AS SELECT * FROM actividad.activity_tracks;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.activity_tracks TO anon, authenticated, service_role;

DROP VIEW IF EXISTS public.saved_routes;
DROP TRIGGER IF EXISTS sync_borrado_saved_routes ON rutas.saved_routes;
DROP TRIGGER IF EXISTS saved_routes_updated_at ON rutas.saved_routes;
DROP INDEX IF EXISTS rutas.saved_routes_sync_idx;
ALTER TABLE rutas.saved_routes DROP COLUMN IF EXISTS updated_at;
CREATE VIEW public.saved_routes WITH (security_invoker = true) AS SELECT * FROM rutas.saved_routes;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.saved_routes TO anon, authenticated, service_role;

DROP VIEW IF EXISTS public.events;
DROP TRIGGER IF EXISTS sync_borrado_events ON eventos.events;
DROP TRIGGER IF EXISTS events_updated_at ON eventos.events;
DROP INDEX IF EXISTS eventos.events_sync_idx;
ALTER TABLE eventos.events DROP COLUMN IF EXISTS updated_at;
CREATE VIEW public.events WITH (security_invoker = true) AS SELECT * FROM eventos.events;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.events TO anon, authenticated, service_role;

DROP VIEW IF EXISTS public.event_rsvps;
DROP TRIGGER IF EXISTS sync_borrado_event_rsvps ON eventos.event_rsvps;
DROP TRIGGER IF EXISTS event_rsvps_updated_at ON eventos.event_rsvps;
DROP INDEX IF EXISTS eventos.event_rsvps_sync_idx;
ALTER TABLE eventos.event_rsvps DROP COLUMN IF EXISTS updated_at;
CREATE VIEW public.event_rsvps WITH (security_invoker = true) AS SELECT * FROM eventos.event_rsvps;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.event_rsvps TO anon, authenticated, service_role;

DROP INDEX IF EXISTS legal.consents_sync_idx;

DROP TABLE IF EXISTS public.sync_borrados;
DROP FUNCTION IF EXISTS public.sync_anotar_borrado();
DROP FUNCTION IF EXISTS public.sync_reloj();

NOTIFY pgrst, 'reload schema';

COMMIT;
