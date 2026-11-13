-- 042: la ficha publica de un perfil sigue siendo legible; edad, genero, bio y suspension no.
--
-- Comprobado sobre el esquema ya migrado (033 mueve profiles a identidad y deja
-- public.profiles como vista security_invoker con las mismas columnas):
--   - 001 crea la politica "Profiles are viewable by everyone" con USING (true).
--     Sigue en identidad.profiles. No filtra filas.
--   - 002 agrega age y gender. 020 agrega suspended_at. bio esta desde 001.
--   - profiles.privacy_level no entra en esa politica. El unico uso es corredores_ruta
--     (010), que compara el valor con 'public'. La visibilidad de carreras la decide
--     user_settings.visibility (can_view_user), no esta columna. Por eso la ficha
--     (id, username, full_name, avatar_url, location, privacy_level, fechas) sigue
--     viendose: mapa, feed, busqueda, clubes y rutas leen esos campos de otras
--     personas. Ocultar la fila entera dejaria los nombres en blanco.
--   - PostgREST solo expone public (supabase/config.toml). Aun asi anon y
--     authenticated tenian SELECT de todas las columnas de identidad.profiles,
--     porque 033 les dio USAGE del esquema y copio los privilegios de la tabla.
--
-- La vista deja de ser un SELECT *: las cuatro columnas sensibles salen de
-- identidad.perfil_sensible, que solo devuelve fila al dueno, a un admin o a
-- service_role. Quien no puede verlas recibe NULL, no un error, para que el
-- select * de Android/iOS (KMP) siga resolviendo el nombre y la foto.
-- La vista no es auto-actualizable; un trigger INSTEAD OF conserva el UPDATE
-- que ya hacen las apps (incluida bio, age y gender del propio perfil) y niega
-- cambiar suspended_at si no es admin o service_role. El alta (handle_new_user)
-- escribe en la tabla: INSERT ... ON CONFLICT no funciona sobre una vista con
-- trigger INSTEAD OF. admin_usuarios lee la tabla para no perder el GROUP BY
-- por la clave primaria ni la suspension.
--
-- Reversion: supabase/rollback/042_perfiles_privacidad.down.sql.

BEGIN;

CREATE OR REPLACE FUNCTION identidad.puede_ver_privado(dueno uuid)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = identidad, public
AS $$
  SELECT dueno IS NOT NULL AND (
    dueno = auth.uid()
    OR public.is_admin()
    OR coalesce(auth.role(), '') = 'service_role'
  );
$$;

CREATE OR REPLACE FUNCTION identidad.perfil_sensible(dueno uuid)
RETURNS TABLE (bio text, age text, gender text, suspended_at timestamptz)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = identidad, public
AS $$
  SELECT p.bio, p.age, p.gender, p.suspended_at
  FROM identidad.profiles p
  WHERE p.id = dueno
    AND identidad.puede_ver_privado(dueno);
$$;

-- Segunda barrera: aunque el trigger de la vista corra como dueno de la tabla,
-- esta fila no acepta edad, genero, bio ni suspension de quien no corresponde.
-- No usa current_user: dentro de un SECURITY DEFINER ese rol es el dueno.
CREATE OR REPLACE FUNCTION identidad.profiles_guardar_privados()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = identidad, public
AS $$
DECLARE
  admin boolean := public.is_admin() OR coalesce(auth.role(), '') = 'service_role';
  dueno boolean := auth.uid() = NEW.id;
BEGIN
  IF NOT admin AND NOT dueno THEN
    IF TG_OP = 'UPDATE' THEN
      NEW.bio := OLD.bio;
      NEW.age := OLD.age;
      NEW.gender := OLD.gender;
      NEW.suspended_at := OLD.suspended_at;
    ELSE
      NEW.bio := NULL;
      NEW.age := '';
      NEW.gender := 'Prefiero no decir';
      NEW.suspended_at := NULL;
    END IF;
  ELSIF NOT admin THEN
    IF TG_OP = 'UPDATE' THEN
      NEW.suspended_at := OLD.suspended_at;
    ELSE
      NEW.suspended_at := NULL;
    END IF;
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS profiles_guardar_privados ON identidad.profiles;
CREATE TRIGGER profiles_guardar_privados
  BEFORE INSERT OR UPDATE ON identidad.profiles
  FOR EACH ROW EXECUTE FUNCTION identidad.profiles_guardar_privados();

CREATE OR REPLACE VIEW public.profiles
WITH (security_invoker = true, security_barrier = true) AS
SELECT
  p.id,
  p.username,
  p.full_name,
  p.avatar_url,
  s.bio,
  p.location,
  p.privacy_level,
  p.created_at,
  p.updated_at,
  s.age,
  s.gender,
  s.suspended_at
FROM identidad.profiles p
LEFT JOIN LATERAL identidad.perfil_sensible(p.id) AS s ON true;

ALTER VIEW public.profiles SET (security_invoker = true, security_barrier = true);

CREATE OR REPLACE FUNCTION identidad.profiles_escribir()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = identidad, public
AS $$
DECLARE
  privilegiado boolean := public.is_admin() OR coalesce(auth.role(), '') = 'service_role';
  fila identidad.profiles%ROWTYPE;
BEGIN
  IF TG_OP = 'DELETE' THEN
    RETURN NULL;
  END IF;

  IF TG_OP = 'INSERT' THEN
    IF auth.uid() IS DISTINCT FROM NEW.id AND NOT privilegiado THEN
      RETURN NULL;
    END IF;
    INSERT INTO identidad.profiles (
      id, username, full_name, avatar_url, bio, location, privacy_level,
      created_at, updated_at, age, gender, suspended_at
    ) VALUES (
      NEW.id,
      NEW.username,
      NEW.full_name,
      NEW.avatar_url,
      NEW.bio,
      COALESCE(NEW.location, 'Loja, Ecuador'),
      COALESCE(NEW.privacy_level, 'public'),
      COALESCE(NEW.created_at, now()),
      COALESCE(NEW.updated_at, now()),
      COALESCE(NEW.age, ''),
      COALESCE(NEW.gender, 'Prefiero no decir'),
      CASE WHEN privilegiado THEN NEW.suspended_at ELSE NULL END
    );
    RETURN NEW;
  END IF;

  IF auth.uid() IS DISTINCT FROM OLD.id AND NOT privilegiado THEN
    RETURN NULL;
  END IF;

  SELECT * INTO fila FROM identidad.profiles WHERE id = OLD.id;
  IF NOT FOUND THEN
    RETURN NULL;
  END IF;
  IF NOT privilegiado THEN
    NEW.suspended_at := fila.suspended_at;
  END IF;
  IF NOT identidad.puede_ver_privado(OLD.id) THEN
    NEW.bio := fila.bio;
    NEW.age := fila.age;
    NEW.gender := fila.gender;
    NEW.suspended_at := fila.suspended_at;
  END IF;

  UPDATE identidad.profiles SET
    username = NEW.username,
    full_name = NEW.full_name,
    avatar_url = NEW.avatar_url,
    bio = NEW.bio,
    location = NEW.location,
    privacy_level = COALESCE(NEW.privacy_level, fila.privacy_level),
    age = NEW.age,
    gender = NEW.gender,
    suspended_at = NEW.suspended_at
  WHERE id = OLD.id;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS profiles_escritura ON public.profiles;
CREATE TRIGGER profiles_escritura
  INSTEAD OF INSERT OR UPDATE OR DELETE ON public.profiles
  FOR EACH ROW EXECUTE FUNCTION identidad.profiles_escribir();

REVOKE ALL ON FUNCTION identidad.puede_ver_privado(uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION identidad.perfil_sensible(uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION identidad.profiles_escribir() FROM PUBLIC;
REVOKE ALL ON FUNCTION identidad.profiles_guardar_privados() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identidad.puede_ver_privado(uuid) TO CURRENT_USER;

DO $$
DECLARE rol text;
BEGIN
  FOREACH rol IN ARRAY ARRAY['anon', 'authenticated'] LOOP
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = rol) THEN
      CONTINUE;
    END IF;
    EXECUTE format('REVOKE INSERT, UPDATE, SELECT ON TABLE identidad.profiles FROM %I', rol);
    EXECUTE format(
      'GRANT SELECT (id, username, full_name, avatar_url, location, privacy_level, created_at, updated_at) ON identidad.profiles TO %I',
      rol);
    EXECUTE format(
      'GRANT UPDATE (username, full_name, avatar_url, location, privacy_level) ON identidad.profiles TO %I',
      rol);
    EXECUTE format(
      'GRANT INSERT (id, username, full_name, avatar_url, location, privacy_level) ON identidad.profiles TO %I',
      rol);
    EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON public.profiles TO %I', rol);
    EXECUTE format('GRANT EXECUTE ON FUNCTION identidad.perfil_sensible(uuid) TO %I', rol);
    EXECUTE format('GRANT EXECUTE ON FUNCTION identidad.profiles_escribir() TO %I', rol);
    EXECUTE format('GRANT EXECUTE ON FUNCTION identidad.profiles_guardar_privados() TO %I', rol);
  END LOOP;
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'service_role') THEN
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE identidad.profiles TO service_role;
    GRANT SELECT, INSERT, UPDATE, DELETE ON public.profiles TO service_role;
    GRANT EXECUTE ON FUNCTION identidad.perfil_sensible(uuid) TO service_role;
    GRANT EXECUTE ON FUNCTION identidad.profiles_escribir() TO service_role;
    GRANT EXECUTE ON FUNCTION identidad.profiles_guardar_privados() TO service_role;
  END IF;
END $$;

REVOKE ALL ON TABLE identidad.profiles FROM PUBLIC;

-- Alta: la vista ya no admite ON CONFLICT. El cuerpo es el de 036, apuntando a la tabla.
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
  IF EXISTS (SELECT 1 FROM identidad.profiles WHERE username = usuario) THEN
    usuario := left(base, 13) || '_' || substr(NEW.id::text, 1, 6);
  END IF;
  INSERT INTO identidad.profiles (id, username, full_name, avatar_url)
  VALUES (NEW.id, usuario, nombre, NEW.raw_user_meta_data->>'avatar_url')
  ON CONFLICT (id) DO NOTHING;
  INSERT INTO user_settings (user_id) VALUES (NEW.id) ON CONFLICT DO NOTHING;
  INSERT INTO notifications (user_id, type, actor_name, message)
  VALUES (NEW.id, 'event', 'Runcity Loja', 'Bienvenido a Runcity. Mira los eventos de esta semana');
  RETURN NEW;
END;
$function$;

-- El panel lee la tabla (clave primaria real) y no la vista enmascarada.
CREATE OR REPLACE FUNCTION public.admin_usuarios()
RETURNS TABLE (id UUID, username TEXT, full_name TEXT, avatar_url TEXT, created_at TIMESTAMPTZ,
               suspended_at TIMESTAMPTZ, role TEXT, carreras BIGINT, km NUMERIC)
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'solo administradores'; END IF;
  RETURN QUERY
    SELECT p.id, p.username, p.full_name, p.avatar_url, p.created_at, p.suspended_at, r.role,
           count(a.id), coalesce(round(sum(a.distance_meters) / 1000.0, 1), 0)
    FROM identidad.profiles p
    LEFT JOIN public.user_roles r ON r.user_id = p.id
    LEFT JOIN public.activities a ON a.user_id = p.id
    GROUP BY p.id, r.role
    ORDER BY p.created_at DESC;
END $$;

COMMENT ON POLICY "Profiles are viewable by everyone" ON identidad.profiles IS
  'La ficha publica se lista completa. Edad, genero, bio y suspended_at no tienen SELECT para anon ni authenticated: la vista public.profiles los devuelve solo al dueno, a un admin o a service_role.';

COMMENT ON FUNCTION identidad.perfil_sensible(uuid) IS
  'Columnas del perfil que no deben salir con la anon key. Sin fila si quien consulta no es el dueno, admin o service_role.';

DO $$
DECLARE
  tabla text[];
  vista text[];
  rol text;
BEGIN
  SELECT array_agg(attname::text ORDER BY attnum) INTO tabla
  FROM pg_attribute
  WHERE attrelid = 'identidad.profiles'::regclass AND attnum > 0 AND NOT attisdropped;
  SELECT array_agg(attname::text ORDER BY attnum) INTO vista
  FROM pg_attribute
  WHERE attrelid = 'public.profiles'::regclass AND attnum > 0 AND NOT attisdropped;
  IF tabla IS DISTINCT FROM vista THEN
    RAISE EXCEPTION 'public.profiles no coincide con identidad.profiles: % vs %', vista, tabla;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM pg_class
    WHERE oid = 'public.profiles'::regclass
      AND relkind = 'v'
      AND reloptions @> ARRAY['security_invoker=true']
  ) THEN
    RAISE EXCEPTION 'public.profiles perdio security_invoker';
  END IF;
  FOREACH rol IN ARRAY ARRAY['anon', 'authenticated'] LOOP
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = rol)
       AND has_column_privilege(rol, 'identidad.profiles', 'bio', 'SELECT') THEN
      RAISE EXCEPTION '% puede leer identidad.profiles.bio', rol;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = rol)
       AND has_column_privilege(rol, 'identidad.profiles', 'suspended_at', 'SELECT') THEN
      RAISE EXCEPTION '% puede leer identidad.profiles.suspended_at', rol;
    END IF;
  END LOOP;
END $$;

NOTIFY pgrst, 'reload schema';

COMMIT;
