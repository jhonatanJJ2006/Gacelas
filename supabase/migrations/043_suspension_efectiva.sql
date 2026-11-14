-- 043: una cuenta con profiles.suspended_at deja de poder escribir.
--
-- El PR de 042 (perfiles) ya impide que un no-admin cambie suspended_at. Esta
-- migracion no repite ese candado: asume que 042 se aplica antes (042 y luego
-- 043). Si 043 corriera sola, el bloqueo de abajo igual rechaza cualquier
-- UPDATE mientras el flag siga puesto, asi que el usuario tampoco puede
-- limpiarlo; 042 sigue haciendo falta para que ni siquiera pueda tocar la
-- columna en un UPDATE que mas adelante se permita, y para ocultarla.
--
-- RLS no basta: las RPC SECURITY DEFINER corren como dueno de la tabla y se
-- saltan las politicas. Un trigger BEFORE en cada tabla con RLS si se ejecuta
-- en esas escrituras. El helper es identidad.es_suspendido(uuid): no esta en
-- un esquema expuesto por PostgREST (solo public), para no ofrecer un RPC que
-- revele quien esta suspendido. Lee identidad.profiles como SECURITY DEFINER,
-- asi que sigue viendo la columna despues de que 042 quite el SELECT a
-- anon y authenticated.
--
-- Sigue permitido:
--   - Leer (no hay trigger en SELECT). El propio perfil, sus carreras, ajustes
--     y avisos se consultan igual.
--   - Borrar la cuenta (eliminar_mi_cuenta). Es el borrado que pide la persona
--     y la cascada de auth.users. Durante esa transaccion el trigger deja pasar
--     los DELETE y los SET NULL. No puede borrar publicaciones sueltas: eso
--     seria destruir el contenido denunciado sin cerrar la cuenta.
--   - SOS (seguridad.sos_alertas no lleva el trigger). Es una emergencia fisica,
--     no publica en el feed y el enlace es un uuid. Suspender no debe dejar a
--     alguien sin poder avisar a sus contactos.
--   - La cache de puntajes de rutas (rutas.seguridad_rutas_cache). La rellena
--     seguridad_rutas(), que es una lectura. No guarda contenido del usuario.
--
-- Al pasar suspended_at de NULL a una fecha se cierra su sesion en vivo y se
-- borra su punto del mapa, para que no siga visible hasta que expire el TTL.
--
-- Reversion: supabase/rollback/043_suspension_efectiva.down.sql.

BEGIN;

CREATE OR REPLACE FUNCTION identidad.es_suspendido(uid uuid)
RETURNS boolean
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = identidad
AS $$
BEGIN
  RETURN COALESCE(
    (SELECT p.suspended_at IS NOT NULL FROM identidad.profiles p WHERE p.id = uid),
    false
  );
END;
$$;

COMMENT ON FUNCTION identidad.es_suspendido(uuid) IS
  'True si la cuenta tiene suspended_at. No esta expuesta por PostgREST.';

CREATE OR REPLACE FUNCTION identidad.trg_bloquear_suspendido()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = identidad, public
AS $$
BEGIN
  -- Solo lo activa eliminar_mi_cuenta, en la misma transaccion del DELETE.
  IF coalesce(current_setting('runcity.eliminar_cuenta', true), '') = '1' THEN
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
  END IF;
  -- Cierre de mapa y en vivo al suspender. No abre el resto de tablas.
  IF coalesce(current_setting('runcity.cerrar_por_suspension', true), '') = '1'
     AND TG_TABLE_NAME IN ('live_sessions', 'runner_positions') THEN
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
  END IF;
  IF identidad.es_suspendido(auth.uid()) THEN
    RAISE EXCEPTION 'cuenta suspendida' USING ERRCODE = '42501';
  END IF;
  IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
  RETURN NEW;
END;
$$;

COMMENT ON FUNCTION identidad.trg_bloquear_suspendido() IS
  'Rechaza INSERT, UPDATE y DELETE del usuario autenticado si esta suspendido.';

-- Al suspender, deja de emitir posicion. El actor es el admin (o service_role):
-- el bypass de abajo solo cubre estas dos tablas, por si el admin se suspende
-- a si mismo y el SELECT de es_suspendido ya ve su fila en la misma transaccion.
CREATE OR REPLACE FUNCTION identidad.cerrar_en_vivo_al_suspender()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NEW.suspended_at IS NULL OR OLD.suspended_at IS NOT NULL THEN
    RETURN NULL;
  END IF;
  PERFORM set_config('runcity.cerrar_por_suspension', '1', true);
  UPDATE public.live_sessions
    SET ended_at = now()
    WHERE user_id = NEW.id AND ended_at IS NULL;
  DELETE FROM public.runner_positions WHERE user_id = NEW.id;
  PERFORM set_config('runcity.cerrar_por_suspension', '0', true);
  RETURN NULL;
END;
$$;

COMMENT ON FUNCTION identidad.cerrar_en_vivo_al_suspender() IS
  'Cierra la sesion en vivo y quita el punto del mapa cuando la cuenta pasa a suspendida.';

REVOKE ALL ON FUNCTION identidad.es_suspendido(uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION identidad.trg_bloquear_suspendido() FROM PUBLIC;
REVOKE ALL ON FUNCTION identidad.cerrar_en_vivo_al_suspender() FROM PUBLIC;

DO $$
DECLARE rol text;
BEGIN
  FOREACH rol IN ARRAY ARRAY[
    'anon', 'authenticated', 'service_role', 'authenticator', 'supabase_storage_admin'
  ] LOOP
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = rol) THEN
      EXECUTE format('GRANT EXECUTE ON FUNCTION identidad.es_suspendido(uuid) TO %I', rol);
      EXECUTE format('GRANT EXECUTE ON FUNCTION identidad.trg_bloquear_suspendido() TO %I', rol);
      EXECUTE format('GRANT EXECUTE ON FUNCTION identidad.cerrar_en_vivo_al_suspender() TO %I', rol);
    END IF;
  END LOOP;
END $$;

-- aa_ para correr antes que el resto de BEFORE y rechazar con este error.
DO $$
DECLARE
  r record;
  excluidas text[] := ARRAY['seguridad.sos_alertas', 'rutas.seguridad_rutas_cache'];
BEGIN
  FOR r IN
    SELECT n.nspname AS esquema, c.relname AS tabla
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE c.relkind = 'r'
      AND c.relrowsecurity
      AND n.nspname IN (
        'public', 'identidad', 'legal', 'actividad', 'rutas', 'envivo',
        'seguridad', 'comunidad', 'eventos', 'logros', 'moderacion',
        'avisos', 'admin', 'analitica'
      )
      AND NOT EXISTS (
        SELECT 1 FROM pg_depend d WHERE d.objid = c.oid AND d.deptype = 'e'
      )
      AND (n.nspname || '.' || c.relname) <> ALL (excluidas)
  LOOP
    EXECUTE format('DROP TRIGGER IF EXISTS aa_bloquear_suspendido ON %I.%I', r.esquema, r.tabla);
    EXECUTE format(
      'CREATE TRIGGER aa_bloquear_suspendido BEFORE INSERT OR UPDATE OR DELETE ON %I.%I FOR EACH ROW EXECUTE FUNCTION identidad.trg_bloquear_suspendido()',
      r.esquema, r.tabla
    );
  END LOOP;
END $$;

DO $$
DECLARE tabla regclass := to_regclass('identidad.profiles');
BEGIN
  IF tabla IS NULL THEN
    RAISE EXCEPTION 'identidad.profiles no existe';
  END IF;
  EXECUTE format('DROP TRIGGER IF EXISTS cerrar_en_vivo_al_suspender ON %s', tabla);
  EXECUTE format(
    'CREATE TRIGGER cerrar_en_vivo_al_suspender AFTER UPDATE OF suspended_at ON %s FOR EACH ROW EXECUTE FUNCTION identidad.cerrar_en_vivo_al_suspender()',
    tabla
  );
END $$;

-- Quien ya esta en el mapa deja de verse, aunque la fila no se haya borrado.
CREATE OR REPLACE FUNCTION public.puede_ver_en_vivo(dueno UUID, audiencia TEXT) RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT NOT identidad.es_suspendido(dueno)
     AND (dueno = auth.uid()
      OR audiencia = 'public'
      OR EXISTS (SELECT 1 FROM follows WHERE follower_id = auth.uid() AND following_id = dueno));
$$;

DROP POLICY IF EXISTS "Ver corredores visibles" ON public.runner_positions;
CREATE POLICY "Ver corredores visibles" ON public.runner_positions FOR SELECT
  USING (auth.uid() IS NOT NULL
         AND updated_at > now() - interval '2 minutes'
         AND consentimiento_vigente(user_id, 'ubicacion_visible')
         AND NOT identidad.es_suspendido(user_id));

-- La cascada de auth.users borra los datos. Sin este ajuste el trigger la frenaria.
CREATE OR REPLACE FUNCTION public.eliminar_mi_cuenta() RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, auth AS $$
BEGIN
  IF auth.uid() IS NULL THEN RAISE EXCEPTION 'no autenticado'; END IF;
  PERFORM set_config('runcity.eliminar_cuenta', '1', true);
  DELETE FROM auth.users WHERE id = auth.uid();
END;
$$;
REVOKE ALL ON FUNCTION public.eliminar_mi_cuenta() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.eliminar_mi_cuenta() TO authenticated;

-- Storage no es una tabla de la app: el mismo trigger y, por si se quita,
-- politicas RESTRICTIVE (no cubren SELECT).
DO $$
BEGIN
  IF to_regclass('storage.objects') IS NULL THEN
    RETURN;
  END IF;
  EXECUTE 'DROP TRIGGER IF EXISTS aa_bloquear_suspendido ON storage.objects';
  EXECUTE 'CREATE TRIGGER aa_bloquear_suspendido BEFORE INSERT OR UPDATE OR DELETE ON storage.objects FOR EACH ROW EXECUTE FUNCTION identidad.trg_bloquear_suspendido()';
  EXECUTE 'DROP POLICY IF EXISTS "Suspendido no sube archivos" ON storage.objects';
  EXECUTE 'DROP POLICY IF EXISTS "Suspendido no reemplaza archivos" ON storage.objects';
  EXECUTE 'DROP POLICY IF EXISTS "Suspendido no borra archivos" ON storage.objects';
  EXECUTE $p$CREATE POLICY "Suspendido no sube archivos" ON storage.objects
    AS RESTRICTIVE FOR INSERT TO authenticated
    WITH CHECK (NOT identidad.es_suspendido(auth.uid()))$p$;
  EXECUTE $p$CREATE POLICY "Suspendido no reemplaza archivos" ON storage.objects
    AS RESTRICTIVE FOR UPDATE TO authenticated
    USING (NOT identidad.es_suspendido(auth.uid()))
    WITH CHECK (NOT identidad.es_suspendido(auth.uid()))$p$;
  EXECUTE $p$CREATE POLICY "Suspendido no borra archivos" ON storage.objects
    AS RESTRICTIVE FOR DELETE TO authenticated
    USING (NOT identidad.es_suspendido(auth.uid()))$p$;
END $$;

NOTIFY pgrst, 'reload schema';

COMMIT;
