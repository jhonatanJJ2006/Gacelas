-- 016: feed de comunidad conectado a la base.
-- post_id de likes y comentarios pasa a ser siempre el id (uuid en texto) de la actividad.
-- Antes la web guardaba 'r-<uuid>' o 'c-<client_id>' y Android 'carrera-<client_id>', asi
-- los contadores nunca coincidian entre apps. Se normalizan los datos existentes, se
-- mantienen likes_count y comments_count con triggers y se expone una RPC de feed.

-- 1. Normalizar post_id existentes
UPDATE post_likes SET post_id = substr(post_id, 3) WHERE post_id LIKE 'r-%';
UPDATE post_comments SET post_id = substr(post_id, 3) WHERE post_id LIKE 'r-%';
UPDATE post_likes l SET post_id = a.id::text FROM activities a
  WHERE (l.post_id = 'c-' || a.client_id OR l.post_id = 'carrera-' || a.client_id
         OR l.post_id = 'carrera-' || a.id::text)
    AND NOT EXISTS (SELECT 1 FROM post_likes x WHERE x.user_id = l.user_id AND x.post_id = a.id::text);
UPDATE post_comments c SET post_id = a.id::text FROM activities a
  WHERE c.post_id = 'c-' || a.client_id OR c.post_id = 'carrera-' || a.client_id
     OR c.post_id = 'carrera-' || a.id::text;

-- 2. Contadores
CREATE OR REPLACE FUNCTION public.feed_contadores() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE pid TEXT := COALESCE(NEW.post_id, OLD.post_id);
BEGIN
  IF pid ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' THEN
    UPDATE activities SET
      likes_count = (SELECT count(*) FROM post_likes WHERE post_id = pid),
      comments_count = (SELECT count(*) FROM post_comments WHERE post_id = pid)
    WHERE id = pid::uuid;
  END IF;
  RETURN NULL;
END $$;
DROP TRIGGER IF EXISTS feed_likes_contador ON post_likes;
CREATE TRIGGER feed_likes_contador AFTER INSERT OR DELETE ON post_likes
  FOR EACH ROW EXECUTE FUNCTION public.feed_contadores();
DROP TRIGGER IF EXISTS feed_comentarios_contador ON post_comments;
CREATE TRIGGER feed_comentarios_contador AFTER INSERT OR DELETE ON post_comments
  FOR EACH ROW EXECUTE FUNCTION public.feed_contadores();

UPDATE activities a SET
  likes_count = (SELECT count(*) FROM post_likes WHERE post_id = a.id::text),
  comments_count = (SELECT count(*) FROM post_comments WHERE post_id = a.id::text);

-- 3. Feed: carreras publicas que el usuario puede ver (RLS de activities aplica porque la
-- funcion es SECURITY INVOKER), con autor, si le di like y si lo sigo.
DROP FUNCTION IF EXISTS public.feed_comunidad(INT, TIMESTAMPTZ);
CREATE OR REPLACE FUNCTION public.feed_comunidad(p_limite INT DEFAULT 30, p_antes TIMESTAMPTZ DEFAULT NULL)
RETURNS TABLE (
  id UUID, client_id TEXT, user_id UUID, autor TEXT, avatar_url TEXT, title TEXT, start_time TIMESTAMPTZ,
  distance_meters NUMERIC, duration_seconds INT, likes_count INT, comments_count INT,
  public_points JSONB, me_gusta BOOLEAN, sigo BOOLEAN, propia BOOLEAN
) LANGUAGE sql STABLE SECURITY INVOKER SET search_path = public AS $$
  SELECT a.id, a.client_id, a.user_id,
         COALESCE(NULLIF(p.full_name, ''), NULLIF(p.username, ''), 'Corredor'),
         p.avatar_url, COALESCE(NULLIF(a.title, ''), 'Carrera'), a.start_time,
         a.distance_meters::numeric, a.duration_seconds::int,
         COALESCE(a.likes_count, 0), COALESCE(a.comments_count, 0), a.public_points,
         EXISTS (SELECT 1 FROM post_likes l WHERE l.post_id = a.id::text AND l.user_id = auth.uid()),
         EXISTS (SELECT 1 FROM follows f WHERE f.follower_id = auth.uid() AND f.following_id = a.user_id),
         a.user_id = auth.uid()
  FROM activities a
  LEFT JOIN profiles p ON p.id = a.user_id
  WHERE a.is_public AND (p_antes IS NULL OR a.start_time < p_antes)
  ORDER BY a.start_time DESC
  LIMIT LEAST(GREATEST(p_limite, 1), 100)
$$;
GRANT EXECUTE ON FUNCTION public.feed_comunidad(INT, TIMESTAMPTZ) TO authenticated;

-- Comentarios de una publicacion con nombre del autor.
CREATE OR REPLACE FUNCTION public.comentarios_publicacion(p_post TEXT)
RETURNS TABLE (id UUID, user_id UUID, autor TEXT, body TEXT, created_at TIMESTAMPTZ)
LANGUAGE sql STABLE SECURITY INVOKER SET search_path = public AS $$
  SELECT c.id, c.user_id, COALESCE(NULLIF(p.full_name, ''), NULLIF(p.username, ''), 'Corredor'),
         c.body, c.created_at
  FROM post_comments c LEFT JOIN profiles p ON p.id = c.user_id
  WHERE c.post_id = p_post
  ORDER BY c.created_at
$$;
GRANT EXECUTE ON FUNCTION public.comentarios_publicacion(TEXT) TO authenticated;

-- 4. Realtime para refrescar el feed
DO $$
DECLARE t TEXT;
BEGIN
  IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime') THEN
    FOREACH t IN ARRAY ARRAY['activities', 'post_likes', 'post_comments', 'follows', 'club_posts'] LOOP
      IF NOT EXISTS (SELECT 1 FROM pg_publication_tables
                     WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = t) THEN
        EXECUTE format('ALTER PUBLICATION supabase_realtime ADD TABLE public.%I', t);
      END IF;
    END LOOP;
  END IF;
END $$;

-- 5. Participantes de eventos: base + inscritos reales sin contar al usuario actual
-- (la app suma 1 si el usuario esta inscrito). event_rsvps solo deja ver las propias.
CREATE OR REPLACE FUNCTION public.participantes_eventos()
RETURNS TABLE (event_id TEXT, otros INT)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT e.id, e.base_participants + (SELECT count(*)::int FROM event_rsvps r
                                      WHERE r.event_id = e.id AND r.user_id IS DISTINCT FROM auth.uid())
  FROM events e
$$;
GRANT EXECUTE ON FUNCTION public.participantes_eventos() TO authenticated;
