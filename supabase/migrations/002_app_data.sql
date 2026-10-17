-- 002: datos que guardan las apps (perfil extendido, ajustes y privacidad, carreras con GPS y
-- parciales, rutas, comunidad, eventos y notificaciones), RLS, trigger de alta e indices.

-- Perfil extendido
ALTER TABLE profiles
  ADD COLUMN IF NOT EXISTS email TEXT,
  ADD COLUMN IF NOT EXISTS age TEXT DEFAULT '',
  ADD COLUMN IF NOT EXISTS gender TEXT DEFAULT 'Prefiero no decir';
ALTER TABLE profiles ALTER COLUMN location SET DEFAULT 'Loja, Ecuador';

CREATE POLICY "Users can insert own profile" ON profiles FOR INSERT WITH CHECK (auth.uid() = id);

-- Seguidores
CREATE TABLE follows (
  follower_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  following_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (follower_id, following_id),
  CHECK (follower_id <> following_id)
);
CREATE INDEX follows_following_idx ON follows(following_id);
ALTER TABLE follows ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Follows visible to involved users" ON follows FOR SELECT
  USING (auth.uid() IN (follower_id, following_id));
CREATE POLICY "Users follow as themselves" ON follows FOR INSERT WITH CHECK (auth.uid() = follower_id);
CREATE POLICY "Users unfollow as themselves" ON follows FOR DELETE USING (auth.uid() = follower_id);

-- Ajustes y privacidad (una fila por usuario). Las columnas que usa RLS van aparte del JSON.
CREATE TABLE user_settings (
  user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  visibility privacy_level NOT NULL DEFAULT 'followers',
  precise_location BOOLEAN NOT NULL DEFAULT true,
  hide_home BOOLEAN NOT NULL DEFAULT true,
  home_radius_m INTEGER NOT NULL DEFAULT 300 CHECK (home_radius_m BETWEEN 0 AND 5000),
  prefs JSONB NOT NULL DEFAULT '{}'::jsonb,
  updated_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE user_settings ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Own settings select" ON user_settings FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Own settings insert" ON user_settings FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Own settings update" ON user_settings FOR UPDATE USING (auth.uid() = user_id);
CREATE TRIGGER update_user_settings_updated_at BEFORE UPDATE ON user_settings
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Puede ver contenido del autor segun su visibilidad
CREATE OR REPLACE FUNCTION can_view_user(owner UUID) RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT owner = auth.uid()
      OR COALESCE((SELECT visibility FROM user_settings WHERE user_id = owner), 'followers') = 'public'
      OR (COALESCE((SELECT visibility FROM user_settings WHERE user_id = owner), 'followers') = 'followers'
          AND EXISTS (SELECT 1 FROM follows WHERE follower_id = auth.uid() AND following_id = owner));
$$;

-- Carreras: el recorrido completo es privado (activity_tracks); la version publica
-- (public_points) se calcula quitando los puntos cerca de casa.
ALTER TABLE activities
  ADD COLUMN IF NOT EXISTS client_id TEXT,
  ADD COLUMN IF NOT EXISTS splits JSONB NOT NULL DEFAULT '[]'::jsonb,
  ADD COLUMN IF NOT EXISTS public_points JSONB NOT NULL DEFAULT '[]'::jsonb;
CREATE UNIQUE INDEX activities_user_client_idx ON activities(user_id, client_id);

DROP POLICY "Anyone can view public activities" ON activities;
CREATE POLICY "View public activities allowed by privacy" ON activities FOR SELECT
  USING (is_public AND can_view_user(user_id));

CREATE TABLE activity_tracks (
  activity_id UUID PRIMARY KEY REFERENCES activities(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  points JSONB NOT NULL DEFAULT '[]'::jsonb
);
CREATE INDEX activity_tracks_user_idx ON activity_tracks(user_id);
ALTER TABLE activity_tracks ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Own tracks select" ON activity_tracks FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Own tracks insert" ON activity_tracks FOR INSERT
  WITH CHECK (auth.uid() = user_id AND activity_id IN (SELECT id FROM activities WHERE user_id = auth.uid()));
CREATE POLICY "Own tracks update" ON activity_tracks FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Own tracks delete" ON activity_tracks FOR DELETE USING (auth.uid() = user_id);

-- Difuminado del hogar: se recortan los puntos del inicio y del final que esten dentro del
-- radio alrededor del primer punto (salida desde casa), si el usuario lo activo.
CREATE OR REPLACE FUNCTION fuzz_track() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  s user_settings;
  p0 JSONB;
  radio INTEGER;
BEGIN
  SELECT * INTO s FROM user_settings WHERE user_id = NEW.user_id;
  p0 := NEW.points -> 0;
  IF p0 IS NULL OR (s.user_id IS NOT NULL AND NOT s.hide_home) THEN
    UPDATE activities SET public_points = NEW.points WHERE id = NEW.activity_id;
    RETURN NEW;
  END IF;
  radio := COALESCE(s.home_radius_m, 300);
  UPDATE activities SET public_points = COALESCE((
    SELECT jsonb_agg(p ORDER BY i) FROM jsonb_array_elements(NEW.points) WITH ORDINALITY AS t(p, i)
    WHERE ST_Distance(
      ST_SetSRID(ST_MakePoint((p->>'lng')::float, (p->>'lat')::float), 4326)::geography,
      ST_SetSRID(ST_MakePoint((p0->>'lng')::float, (p0->>'lat')::float), 4326)::geography) > radio
  ), '[]'::jsonb) WHERE id = NEW.activity_id;
  RETURN NEW;
END;
$$;
CREATE TRIGGER activity_tracks_fuzz AFTER INSERT OR UPDATE ON activity_tracks
  FOR EACH ROW EXECUTE FUNCTION fuzz_track();

-- Rutas
CREATE TABLE routes (
  id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
  name TEXT NOT NULL,
  distance_km NUMERIC(6,2) NOT NULL,
  difficulty TEXT NOT NULL CHECK (difficulty IN ('Fácil','Moderado','Difícil')),
  elevation_m INTEGER NOT NULL DEFAULT 0,
  description TEXT NOT NULL DEFAULT '',
  author_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  path JSONB NOT NULL DEFAULT '[]'::jsonb,
  is_public BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX routes_author_idx ON routes(author_id);
ALTER TABLE routes ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public routes readable" ON routes FOR SELECT USING (is_public OR author_id = auth.uid());
CREATE POLICY "Own routes insert" ON routes FOR INSERT WITH CHECK (author_id = auth.uid());
CREATE POLICY "Own routes update" ON routes FOR UPDATE USING (author_id = auth.uid());
CREATE POLICY "Own routes delete" ON routes FOR DELETE USING (author_id = auth.uid());

CREATE TABLE saved_routes (
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  route_id TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (user_id, route_id)
);
ALTER TABLE saved_routes ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Own saved routes" ON saved_routes FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

-- Comunidad: likes y comentarios sobre publicaciones (post_id = id de actividad o de ejemplo)
CREATE TABLE post_likes (
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  post_id TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (user_id, post_id)
);
CREATE INDEX post_likes_post_idx ON post_likes(post_id);
ALTER TABLE post_likes ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Likes readable by signed in" ON post_likes FOR SELECT TO authenticated USING (true);
CREATE POLICY "Own likes insert" ON post_likes FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Own likes delete" ON post_likes FOR DELETE USING (auth.uid() = user_id);

CREATE TABLE post_comments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  post_id TEXT NOT NULL,
  body TEXT NOT NULL CHECK (char_length(body) BETWEEN 1 AND 1000),
  created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX post_comments_post_idx ON post_comments(post_id, created_at);
ALTER TABLE post_comments ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Comments readable by signed in" ON post_comments FOR SELECT TO authenticated USING (true);
CREATE POLICY "Own comments insert" ON post_comments FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Own comments delete" ON post_comments FOR DELETE USING (auth.uid() = user_id);

-- Eventos e inscripciones
CREATE TABLE events (
  id TEXT PRIMARY KEY DEFAULT gen_random_uuid()::text,
  title TEXT NOT NULL,
  description TEXT NOT NULL DEFAULT '',
  date_label TEXT NOT NULL,
  time_label TEXT NOT NULL,
  starts_at TIMESTAMPTZ,
  location TEXT NOT NULL,
  distance_km NUMERIC(6,2) NOT NULL DEFAULT 0,
  base_participants INTEGER NOT NULL DEFAULT 0,
  created_by UUID REFERENCES auth.users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX events_starts_idx ON events(starts_at);
CREATE INDEX events_created_by_idx ON events(created_by);
ALTER TABLE events ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Events readable" ON events FOR SELECT USING (true);
CREATE POLICY "Own events insert" ON events FOR INSERT WITH CHECK (created_by = auth.uid());
CREATE POLICY "Own events update" ON events FOR UPDATE USING (created_by = auth.uid());
CREATE POLICY "Own events delete" ON events FOR DELETE USING (created_by = auth.uid());

CREATE TABLE event_rsvps (
  event_id TEXT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (event_id, user_id)
);
CREATE INDEX event_rsvps_user_idx ON event_rsvps(user_id);
ALTER TABLE event_rsvps ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Own rsvps" ON event_rsvps FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

-- Notificaciones (las crea el servidor; el usuario solo lee y marca como leidas)
CREATE TABLE notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  type TEXT NOT NULL CHECK (type IN ('like','comment','follow','event','live')),
  actor_name TEXT NOT NULL DEFAULT '',
  message TEXT NOT NULL,
  read BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX notifications_user_idx ON notifications(user_id, created_at DESC);
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Own notifications select" ON notifications FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Own notifications update" ON notifications FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Own notifications delete" ON notifications FOR DELETE USING (auth.uid() = user_id);

-- Alta: crea perfil y ajustes al registrarse (email o Google)
CREATE OR REPLACE FUNCTION handle_new_user() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  nombre TEXT := COALESCE(NULLIF(NEW.raw_user_meta_data->>'full_name', ''), NULLIF(NEW.raw_user_meta_data->>'name', ''), split_part(NEW.email, '@', 1));
  base TEXT := lower(regexp_replace(COALESCE(NULLIF(NEW.raw_user_meta_data->>'username', ''), split_part(NEW.email, '@', 1)), '[^a-zA-Z0-9_]', '_', 'g'));
  usuario TEXT := base;
BEGIN
  IF EXISTS (SELECT 1 FROM profiles WHERE username = usuario) THEN
    usuario := base || '_' || substr(NEW.id::text, 1, 6);
  END IF;
  INSERT INTO profiles (id, username, full_name, email, avatar_url)
  VALUES (NEW.id, usuario, nombre, NEW.email, NEW.raw_user_meta_data->>'avatar_url')
  ON CONFLICT (id) DO NOTHING;
  INSERT INTO user_settings (user_id) VALUES (NEW.id) ON CONFLICT DO NOTHING;
  INSERT INTO notifications (user_id, type, actor_name, message)
  VALUES (NEW.id, 'event', 'Gacelas Loja', 'Bienvenido a Gacelas. Mira los eventos de esta semana');
  RETURN NEW;
END;
$$;
CREATE TRIGGER on_auth_user_created AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION handle_new_user();

-- Contador de inscritos visible para todos
CREATE OR REPLACE FUNCTION event_participants(eid TEXT) RETURNS INTEGER
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT (SELECT base_participants FROM events WHERE id = eid) + (SELECT count(*)::int FROM event_rsvps WHERE event_id = eid);
$$;
