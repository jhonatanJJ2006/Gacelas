-- 044: sincronizacion incremental al arrancar la app.
--
-- Hoy el arranque pide todas las filas de activities, activity_tracks (el GPS),
-- saved_routes, post_likes, post_comments, events, event_rsvps y consents.
-- PostgREST ademas corta en max_rows = 1000 (supabase/config.toml), asi que un
-- historial largo ni siquiera llegaba entero.
--
-- La app guarda una marca de tiempo por usuario y la proxima vez pide solo
-- filas con updated_at (o created_at, en consentimientos) posterior, por paginas.
-- La primera vez, tras cerrar sesion y en la recuperacion manual sigue siendo
-- completa. Los DELETE no tienen fila que subir: el trigger los anota en
-- public.sync_borrados para que el otro dispositivo los quite.
--
-- activities, user_settings y profiles ya tenian updated_at. Aqui se agrega a
-- las tablas que el arranque baja y que no lo tenian, con trigger e indice
-- para el filtro (usuario, marca, clave). activities solo gana el indice:
-- su trigger de 001 ya actualiza la marca.
--
-- Tablas de Realtime (activities, post_likes, post_comments) siguen en public.
-- El resto vive en su esquema desde 033; si public.<tabla> es una vista, se
-- recrea para que la columna nueva se vea por PostgREST (ADR 005).
--
-- Reversion: supabase/rollback/044_sync_incremental.down.sql

BEGIN;

-- Reloj del servidor. La app guarda now() menos un margen, no el reloj del telefono.
CREATE OR REPLACE FUNCTION public.sync_reloj() RETURNS TIMESTAMPTZ
LANGUAGE sql STABLE SECURITY INVOKER SET search_path = public AS $$
  SELECT now();
$$;
REVOKE ALL ON FUNCTION public.sync_reloj() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sync_reloj() TO authenticated;

-- Borrados que la sync incremental tiene que aplicar. user_id NULL es el
-- catalogo compartido (events): lo ve cualquier sesion. El resto, solo el dueno.
-- No hay politica de escritura: inserta el trigger (security definer).
CREATE TABLE IF NOT EXISTS public.sync_borrados (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
  tabla TEXT NOT NULL,
  clave TEXT NOT NULL,
  deleted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS sync_borrados_usuario_idx ON public.sync_borrados (user_id, deleted_at, id);
CREATE INDEX IF NOT EXISTS sync_borrados_catalogo_idx ON public.sync_borrados (tabla, deleted_at, id) WHERE user_id IS NULL;
ALTER TABLE public.sync_borrados ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.sync_borrados FROM PUBLIC, anon, authenticated;
GRANT SELECT ON public.sync_borrados TO authenticated;
DROP POLICY IF EXISTS "Ver mis borrados y los del catalogo" ON public.sync_borrados;
CREATE POLICY "Ver mis borrados y los del catalogo" ON public.sync_borrados
  FOR SELECT TO authenticated
  USING (user_id = auth.uid() OR (user_id IS NULL AND tabla = 'events'));

CREATE OR REPLACE FUNCTION public.sync_anotar_borrado() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  dueno UUID;
  clave TEXT;
BEGIN
  CASE TG_TABLE_NAME
    WHEN 'activities' THEN
      dueno := OLD.user_id;
      clave := COALESCE(OLD.client_id, OLD.id::text);
    WHEN 'activity_tracks' THEN
      dueno := OLD.user_id;
      SELECT COALESCE(a.client_id, a.id::text) INTO clave FROM public.activities a WHERE a.id = OLD.activity_id;
      clave := COALESCE(clave, OLD.activity_id::text);
    WHEN 'saved_routes' THEN
      dueno := OLD.user_id;
      clave := OLD.route_id;
    WHEN 'post_likes' THEN
      dueno := OLD.user_id;
      clave := OLD.post_id;
    WHEN 'post_comments' THEN
      dueno := OLD.user_id;
      clave := OLD.id::text;
    WHEN 'events' THEN
      dueno := NULL;
      clave := OLD.id;
    WHEN 'event_rsvps' THEN
      dueno := OLD.user_id;
      clave := OLD.event_id;
    ELSE
      RETURN OLD;
  END CASE;
  INSERT INTO public.sync_borrados (user_id, tabla, clave) VALUES (dueno, TG_TABLE_NAME, clave);
  RETURN OLD;
END $$;
REVOKE ALL ON FUNCTION public.sync_anotar_borrado() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.sync_anotar_borrado() TO authenticated, service_role;

-- updated_at + trigger + indice de paginacion, y recrea la vista de public si hace falta.
DO $prep$
DECLARE
  nombre TEXT;
  esquema TEXT;
  kind "char";
  indice TEXT;
  cols TEXT;
  par TEXT[];
  pares TEXT[] := ARRAY[
    ARRAY['activity_tracks', 'user_id, updated_at, activity_id'],
    ARRAY['saved_routes', 'user_id, updated_at, route_id'],
    ARRAY['post_likes', 'user_id, updated_at, post_id'],
    ARRAY['post_comments', 'user_id, updated_at, id'],
    ARRAY['events', 'updated_at, id'],
    ARRAY['event_rsvps', 'user_id, updated_at, event_id']
  ];
BEGIN
  FOREACH par SLICE 1 IN ARRAY pares LOOP
    nombre := par[1];
    cols := par[2];
    SELECT n.nspname INTO esquema
    FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE c.relname = nombre AND c.relkind = 'r'
    LIMIT 1;
    IF esquema IS NULL THEN
      RAISE EXCEPTION '044: no esta la tabla %', nombre;
    END IF;

    EXECUTE format(
      'ALTER TABLE %I.%I ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now()',
      esquema, nombre);
    EXECUTE format('DROP TRIGGER IF EXISTS %I ON %I.%I', nombre || '_updated_at', esquema, nombre);
    EXECUTE format(
      'CREATE TRIGGER %I BEFORE INSERT OR UPDATE ON %I.%I FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column()',
      nombre || '_updated_at', esquema, nombre);

    indice := nombre || '_sync_idx';
    EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (%s)', indice, esquema, nombre, cols);

    EXECUTE format('DROP TRIGGER IF EXISTS %I ON %I.%I', 'sync_borrado_' || nombre, esquema, nombre);
    EXECUTE format(
      'CREATE TRIGGER %I AFTER DELETE ON %I.%I FOR EACH ROW EXECUTE FUNCTION public.sync_anotar_borrado()',
      'sync_borrado_' || nombre, esquema, nombre);

    SELECT c.relkind INTO kind
    FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public' AND c.relname = nombre;
    IF kind = 'v' THEN
      EXECUTE format(
        'CREATE OR REPLACE VIEW public.%I WITH (security_invoker = true) AS SELECT * FROM %I.%I',
        nombre, esquema, nombre);
    END IF;
  END LOOP;
END
$prep$;

-- activities ya tiene updated_at y trigger (001). Indice para el filtro de la app y tumbas.
DO $act$
DECLARE esquema TEXT;
BEGIN
  SELECT n.nspname INTO esquema
  FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE c.relname = 'activities' AND c.relkind = 'r'
  LIMIT 1;
  IF esquema IS NULL THEN RAISE EXCEPTION '044: no esta activities'; END IF;
  EXECUTE format('CREATE INDEX IF NOT EXISTS activities_sync_idx ON %I.activities (user_id, updated_at, id)', esquema);
  EXECUTE format('DROP TRIGGER IF EXISTS sync_borrado_activities ON %I.activities', esquema);
  EXECUTE format(
    'CREATE TRIGGER sync_borrado_activities AFTER DELETE ON %I.activities FOR EACH ROW EXECUTE FUNCTION public.sync_anotar_borrado()',
    esquema);
END
$act$;

-- consents es solo insercion (019): la marca es created_at. Indice para paginar.
DO $cons$
DECLARE esquema TEXT;
BEGIN
  SELECT n.nspname INTO esquema
  FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE c.relname = 'consents' AND c.relkind = 'r'
  LIMIT 1;
  IF esquema IS NOT NULL THEN
    EXECUTE format('CREATE INDEX IF NOT EXISTS consents_sync_idx ON %I.consents (user_id, created_at, id)', esquema);
  END IF;
END
$cons$;

NOTIFY pgrst, 'reload schema';

COMMIT;
