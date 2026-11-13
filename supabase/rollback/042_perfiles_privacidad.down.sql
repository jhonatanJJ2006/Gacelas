-- Revierte 042_perfiles_privacidad.sql.
-- La vista vuelve a ser el SELECT * auto-actualizable de 033. Con eso regresan
-- el ON CONFLICT del alta y la lectura de todas las columnas para anon.
BEGIN;

DROP TRIGGER IF EXISTS profiles_escritura ON public.profiles;
DROP TRIGGER IF EXISTS profiles_guardar_privados ON identidad.profiles;

CREATE OR REPLACE VIEW public.profiles
WITH (security_invoker = true, security_barrier = false) AS
  SELECT * FROM identidad.profiles;
ALTER VIEW public.profiles SET (security_invoker = true, security_barrier = false);

DROP FUNCTION IF EXISTS identidad.profiles_escribir();
DROP FUNCTION IF EXISTS identidad.profiles_guardar_privados();
DROP FUNCTION IF EXISTS identidad.perfil_sensible(uuid);
DROP FUNCTION IF EXISTS identidad.puede_ver_privado(uuid);

DO $$
DECLARE rol text;
BEGIN
  FOREACH rol IN ARRAY ARRAY['anon', 'authenticated', 'service_role'] LOOP
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = rol) THEN
      EXECUTE format(
        'GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE identidad.profiles TO %I', rol);
      EXECUTE format(
        'GRANT SELECT, INSERT, UPDATE, DELETE ON public.profiles TO %I', rol);
    END IF;
  END LOOP;
END $$;

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
  VALUES (NEW.id, 'event', 'Runcity Loja', 'Bienvenido a Runcity. Mira los eventos de esta semana');
  RETURN NEW;
END;
$function$;

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

COMMENT ON POLICY "Profiles are viewable by everyone" ON identidad.profiles IS NULL;

NOTIFY pgrst, 'reload schema';

COMMIT;
