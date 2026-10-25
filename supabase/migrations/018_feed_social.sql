-- 018: feed social estilo Stitch (fotos, compartidos, vista previa de comentarios).
-- post_images: fotos de una carrera publicada (bucket publico post-images/<uid>/<actividad>/<archivo>).
-- post_reposts: compartir una carrera en el propio perfil; contador reposts_count en activities.
-- feed_comunidad devuelve ademas imagenes, compartidos, ultimo comentario y si yo la comparti.

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('post-images', 'post-images', true, 5242880, ARRAY['image/jpeg', 'image/png', 'image/webp'])
ON CONFLICT (id) DO UPDATE SET public = true, file_size_limit = 5242880,
  allowed_mime_types = ARRAY['image/jpeg', 'image/png', 'image/webp'];

DROP POLICY IF EXISTS "Fotos de publicaciones: subir en mi carpeta" ON storage.objects;
CREATE POLICY "Fotos de publicaciones: subir en mi carpeta" ON storage.objects FOR INSERT TO authenticated
  WITH CHECK (bucket_id = 'post-images' AND (storage.foldername(name))[1] = auth.uid()::text);
DROP POLICY IF EXISTS "Fotos de publicaciones: borrar las mias" ON storage.objects;
CREATE POLICY "Fotos de publicaciones: borrar las mias" ON storage.objects FOR DELETE TO authenticated
  USING (bucket_id = 'post-images' AND (storage.foldername(name))[1] = auth.uid()::text);

CREATE TABLE IF NOT EXISTS public.post_images (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  activity_id UUID NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  -- "bucket/ruta" para poder usar post-images, route-photos o share-images
  path TEXT NOT NULL CHECK (length(path) BETWEEN 3 AND 500),
  position INT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS post_images_activity_idx ON public.post_images(activity_id, position);
ALTER TABLE public.post_images ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Fotos visibles con la carrera" ON public.post_images;
CREATE POLICY "Fotos visibles con la carrera" ON public.post_images FOR SELECT TO authenticated
  USING (EXISTS (SELECT 1 FROM activities a WHERE a.id = activity_id AND (a.is_public OR a.user_id = auth.uid())));
DROP POLICY IF EXISTS "Fotos: agregar a mis carreras" ON public.post_images;
CREATE POLICY "Fotos: agregar a mis carreras" ON public.post_images FOR INSERT TO authenticated
  WITH CHECK (user_id = auth.uid() AND EXISTS (SELECT 1 FROM activities a WHERE a.id = activity_id AND a.user_id = auth.uid()));
DROP POLICY IF EXISTS "Fotos: borrar las mias" ON public.post_images;
CREATE POLICY "Fotos: borrar las mias" ON public.post_images FOR DELETE TO authenticated
  USING (user_id = auth.uid());

CREATE TABLE IF NOT EXISTS public.post_reposts (
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  activity_id UUID NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (user_id, activity_id)
);
CREATE INDEX IF NOT EXISTS post_reposts_activity_idx ON public.post_reposts(activity_id);
ALTER TABLE public.post_reposts ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Compartidos visibles" ON public.post_reposts;
CREATE POLICY "Compartidos visibles" ON public.post_reposts FOR SELECT TO authenticated
  USING (EXISTS (SELECT 1 FROM activities a WHERE a.id = activity_id AND (a.is_public OR a.user_id = auth.uid())));
DROP POLICY IF EXISTS "Compartir como yo" ON public.post_reposts;
CREATE POLICY "Compartir como yo" ON public.post_reposts FOR INSERT TO authenticated
  WITH CHECK (user_id = auth.uid() AND EXISTS (SELECT 1 FROM activities a WHERE a.id = activity_id AND a.is_public));
DROP POLICY IF EXISTS "Dejar de compartir" ON public.post_reposts;
CREATE POLICY "Dejar de compartir" ON public.post_reposts FOR DELETE TO authenticated
  USING (user_id = auth.uid());

ALTER TABLE activities ADD COLUMN IF NOT EXISTS reposts_count INT NOT NULL DEFAULT 0;
CREATE OR REPLACE FUNCTION public.feed_reposts_contador() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE aid UUID := COALESCE(NEW.activity_id, OLD.activity_id);
BEGIN
  UPDATE activities SET reposts_count = (SELECT count(*) FROM post_reposts WHERE activity_id = aid) WHERE id = aid;
  RETURN NULL;
END $$;
DROP TRIGGER IF EXISTS feed_reposts_contador ON public.post_reposts;
CREATE TRIGGER feed_reposts_contador AFTER INSERT OR DELETE ON public.post_reposts
  FOR EACH ROW EXECUTE FUNCTION public.feed_reposts_contador();

DROP FUNCTION IF EXISTS public.feed_comunidad(INT, TIMESTAMPTZ);
CREATE OR REPLACE FUNCTION public.feed_comunidad(p_limite INT DEFAULT 30, p_antes TIMESTAMPTZ DEFAULT NULL)
RETURNS TABLE (
  id UUID, client_id TEXT, user_id UUID, autor TEXT, avatar_url TEXT, title TEXT, start_time TIMESTAMPTZ,
  distance_meters NUMERIC, duration_seconds INT, likes_count INT, comments_count INT,
  public_points JSONB, me_gusta BOOLEAN, sigo BOOLEAN, propia BOOLEAN, elevation_gain INT,
  images JSONB, reposts_count INT, reposteada BOOLEAN, autor_km NUMERIC, ultimo_comentario JSONB
) LANGUAGE sql STABLE SECURITY INVOKER SET search_path = public AS $$
  SELECT a.id, a.client_id, a.user_id,
         COALESCE(NULLIF(p.full_name, ''), NULLIF(p.username, ''), 'Corredor'),
         p.avatar_url, COALESCE(NULLIF(a.title, ''), 'Carrera'), a.start_time,
         a.distance_meters::numeric, a.duration_seconds::int,
         COALESCE(a.likes_count, 0), COALESCE(a.comments_count, 0), a.public_points,
         EXISTS (SELECT 1 FROM post_likes l WHERE l.post_id = a.id::text AND l.user_id = auth.uid()),
         EXISTS (SELECT 1 FROM follows f WHERE f.follower_id = auth.uid() AND f.following_id = a.user_id),
         a.user_id = auth.uid(),
         COALESCE(a.elevation_gain_meters, 0)::int,
         COALESCE((SELECT jsonb_agg(i.path ORDER BY i.position, i.created_at) FROM post_images i WHERE i.activity_id = a.id), '[]'::jsonb),
         COALESCE(a.reposts_count, 0),
         EXISTS (SELECT 1 FROM post_reposts r WHERE r.activity_id = a.id AND r.user_id = auth.uid()),
         (SELECT COALESCE(sum(x.distance_meters), 0) / 1000.0 FROM activities x WHERE x.user_id = a.user_id AND x.is_public)::numeric,
         (SELECT jsonb_build_object('autor', COALESCE(NULLIF(cp.full_name, ''), NULLIF(cp.username, ''), 'Corredor'),
                                    'texto', c.body, 'creado', c.created_at)
            FROM post_comments c LEFT JOIN profiles cp ON cp.id = c.user_id
           WHERE c.post_id = a.id::text ORDER BY c.created_at DESC LIMIT 1)
  FROM activities a
  LEFT JOIN profiles p ON p.id = a.user_id
  WHERE a.is_public AND (p_antes IS NULL OR a.start_time < p_antes)
  ORDER BY a.start_time DESC
  LIMIT LEAST(GREATEST(p_limite, 1), 100)
$$;
GRANT EXECUTE ON FUNCTION public.feed_comunidad(INT, TIMESTAMPTZ) TO authenticated;

DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime')
     AND NOT EXISTS (SELECT 1 FROM pg_publication_tables WHERE pubname = 'supabase_realtime' AND tablename = 'post_reposts') THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.post_reposts;
  END IF;
END $$;
