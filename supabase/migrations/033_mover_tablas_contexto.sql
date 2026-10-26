-- 033: fase F7 de DDD, etapa 2. Mueve cada tabla al esquema de su contexto acotado y deja en
-- public una vista actualizable con el mismo nombre y columnas (security_invoker = true), para
-- que PostgREST, la web y las versiones de Android ya instaladas sigan leyendo y escribiendo
-- igual. RLS, triggers, FKs, indices y secuencias viajan con la tabla. Ver docs/adr/0003.
--
-- Excepcion: las tablas de la publicacion supabase_realtime se quedan en public, porque
-- postgres_changes no emite eventos sobre vistas y los clientes instalados se suscriben a
-- public.<tabla>. Tambien se queda cualquier tabla que no figure en kernel.propiedad_tablas.
BEGIN;

CREATE TABLE IF NOT EXISTS kernel.tablas_en_public (
  tabla TEXT PRIMARY KEY,
  motivo TEXT NOT NULL
);
ALTER TABLE kernel.tablas_en_public ENABLE ROW LEVEL SECURITY;

DO $$
DECLARE
  r RECORD;
  a RECORD;
  motivo TEXT;
BEGIN
  FOR r IN SELECT p.tabla, p.contexto FROM kernel.propiedad_tablas p
    WHERE to_regclass('public.' || p.tabla) IS NOT NULL
      AND (SELECT relkind FROM pg_class WHERE oid = to_regclass('public.' || p.tabla)) IN ('r', 'p')
    ORDER BY p.tabla LOOP

    motivo := NULL;
    IF EXISTS (SELECT 1 FROM pg_publication_tables
               WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = r.tabla) THEN
      motivo := 'Publicada en supabase_realtime: postgres_changes no funciona sobre vistas y los clientes instalados escuchan public.' || r.tabla;
    END IF;

    IF motivo IS NOT NULL THEN
      INSERT INTO kernel.tablas_en_public (tabla, motivo) VALUES (r.tabla, motivo)
        ON CONFLICT (tabla) DO UPDATE SET motivo = EXCLUDED.motivo;
      CONTINUE;
    END IF;

    -- La vista de lectura de la etapa 1 ocupa el nombre en el esquema destino.
    EXECUTE format('DROP VIEW IF EXISTS %I.%I', r.contexto, r.tabla);
    EXECUTE format('ALTER TABLE public.%I SET SCHEMA %I', r.tabla, r.contexto);
    EXECUTE format('CREATE VIEW public.%I WITH (security_invoker = true) AS SELECT * FROM %I.%I',
      r.tabla, r.contexto, r.tabla);
    -- Mismos privilegios que tenia la tabla.
    FOR a IN SELECT g.grantee::regrole::text AS rol, g.privilege_type AS priv
      FROM pg_class c, aclexplode(c.relacl) g
      WHERE c.oid = format('%I.%I', r.contexto, r.tabla)::regclass AND g.grantee <> 0
        AND g.privilege_type IN ('SELECT', 'INSERT', 'UPDATE', 'DELETE', 'TRUNCATE', 'REFERENCES', 'TRIGGER') LOOP
      EXECUTE format('GRANT %s ON public.%I TO %I', a.priv, r.tabla, a.rol);
    END LOOP;
    UPDATE kernel.propiedad_tablas SET esquema_actual = r.contexto WHERE tabla = r.tabla;
    DELETE FROM kernel.tablas_en_public WHERE tabla = r.tabla;
  END LOOP;
END $$;

-- Las vistas de public son security_invoker: el rol que consulta necesita USAGE en el esquema
-- de la tabla. Esto no expone los esquemas por la API (PostgREST sigue exponiendo solo public).
GRANT USAGE ON SCHEMA identidad, legal, actividad, rutas, envivo, seguridad, comunidad, eventos,
  logros, moderacion, avisos, admin, analitica TO anon, authenticated, service_role;

NOTIFY pgrst, 'reload schema';

COMMIT;
