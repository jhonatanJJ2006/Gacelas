-- Feed de comunidad: agrega el desnivel positivo (elevation_gain) de cada carrera.
-- Cambia el tipo de retorno, por eso se elimina y se vuelve a crear la funcion.
DROP FUNCTION IF EXISTS public.feed_comunidad(INT, TIMESTAMPTZ);
CREATE OR REPLACE FUNCTION public.feed_comunidad(p_limite INT DEFAULT 30, p_antes TIMESTAMPTZ DEFAULT NULL)
RETURNS TABLE (
  id UUID, client_id TEXT, user_id UUID, autor TEXT, avatar_url TEXT, title TEXT, start_time TIMESTAMPTZ,
  distance_meters NUMERIC, duration_seconds INT, likes_count INT, comments_count INT,
  public_points JSONB, me_gusta BOOLEAN, sigo BOOLEAN, propia BOOLEAN, elevation_gain INT
) LANGUAGE sql STABLE SECURITY INVOKER SET search_path = public AS $$
  SELECT a.id, a.client_id, a.user_id,
         COALESCE(NULLIF(p.full_name, ''), NULLIF(p.username, ''), 'Corredor'),
         p.avatar_url, COALESCE(NULLIF(a.title, ''), 'Carrera'), a.start_time,
         a.distance_meters::numeric, a.duration_seconds::int,
         COALESCE(a.likes_count, 0), COALESCE(a.comments_count, 0), a.public_points,
         EXISTS (SELECT 1 FROM post_likes l WHERE l.post_id = a.id::text AND l.user_id = auth.uid()),
         EXISTS (SELECT 1 FROM follows f WHERE f.follower_id = auth.uid() AND f.following_id = a.user_id),
         a.user_id = auth.uid(),
         COALESCE(a.elevation_gain_meters, 0)::int
  FROM activities a
  LEFT JOIN profiles p ON p.id = a.user_id
  WHERE a.is_public AND (p_antes IS NULL OR a.start_time < p_antes)
  ORDER BY a.start_time DESC
  LIMIT LEAST(GREATEST(p_limite, 1), 100)
$$;
GRANT EXECUTE ON FUNCTION public.feed_comunidad(INT, TIMESTAMPTZ) TO authenticated;
