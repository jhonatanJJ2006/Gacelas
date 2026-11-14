-- La imagen supabase/postgres:15.8.1.060 arranca con un esquema de plataforma
-- anterior al de Supabase alojado. Estas diferencias ya rompian sentencias de
-- las migraciones y auth.uid() en las pruebas, pero psql segui adelante.
-- En produccion las columnas y las funciones ya existen: aqui solo se completan
-- si faltan, y auth.uid/role/email aceptan tanto request.jwt.claim.* como
-- request.jwt.claims (lo que fijan las pruebas y PostgREST).

ALTER TABLE storage.buckets ADD COLUMN IF NOT EXISTS public boolean DEFAULT false;
ALTER TABLE storage.buckets ADD COLUMN IF NOT EXISTS file_size_limit bigint;
ALTER TABLE storage.buckets ADD COLUMN IF NOT EXISTS allowed_mime_types text[];

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_proc p
    JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'storage' AND p.proname = 'foldername'
  ) THEN
    CREATE FUNCTION storage.foldername(name text)
    RETURNS text[]
    LANGUAGE plpgsql
    AS $fn$
    DECLARE
      _parts text[];
    BEGIN
      SELECT string_to_array(name, '/') INTO _parts;
      RETURN _parts[1:array_length(_parts, 1) - 1];
    END
    $fn$;
  END IF;
END $$;

ALTER TABLE auth.users ADD COLUMN IF NOT EXISTS banned_until timestamptz;

CREATE OR REPLACE FUNCTION auth.uid()
RETURNS uuid
LANGUAGE sql
STABLE
AS $$
  SELECT coalesce(
    nullif(current_setting('request.jwt.claim.sub', true), ''),
    (nullif(current_setting('request.jwt.claims', true), '')::jsonb ->> 'sub')
  )::uuid
$$;

CREATE OR REPLACE FUNCTION auth.role()
RETURNS text
LANGUAGE sql
STABLE
AS $$
  SELECT coalesce(
    nullif(current_setting('request.jwt.claim.role', true), ''),
    (nullif(current_setting('request.jwt.claims', true), '')::jsonb ->> 'role')
  )::text
$$;

CREATE OR REPLACE FUNCTION auth.email()
RETURNS text
LANGUAGE sql
STABLE
AS $$
  SELECT coalesce(
    nullif(current_setting('request.jwt.claim.email', true), ''),
    (nullif(current_setting('request.jwt.claims', true), '')::jsonb ->> 'email')
  )::text
$$;
