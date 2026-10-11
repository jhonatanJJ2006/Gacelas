-- QA #1: los correos no deben ser legibles por otros usuarios ni por anonimos.
-- El correo vive en auth.users; las apps lo leen de la sesion. Se elimina de profiles.
-- QA #2: el username generado no puede pasar de 20 caracteres (limite de la app).
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $function$
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
  VALUES (NEW.id, 'event', 'Gacelas Loja', 'Bienvenido a Gacelas. Mira los eventos de esta semana');
  RETURN NEW;
END;
$function$;

UPDATE profiles SET username = left(username, 13) || '_' || substr(id::text, 1, 6) WHERE length(username) > 20;

ALTER TABLE profiles DROP COLUMN IF EXISTS email;
