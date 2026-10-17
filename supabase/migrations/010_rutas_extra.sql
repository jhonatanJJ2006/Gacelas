-- Fase 4 (resto): perfil de elevacion, rutas dibujadas a mano, fotos y "quien la ha corrido".

-- Perfil de elevacion: alturas en metros a lo largo del trazo (lo calcula el cliente con Open-Meteo).
ALTER TABLE routes ADD COLUMN IF NOT EXISTS elevation_profile JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE routes ADD COLUMN IF NOT EXISTS origin TEXT NOT NULL DEFAULT 'actividad';
ALTER TABLE routes DROP CONSTRAINT IF EXISTS routes_profile_check;
ALTER TABLE routes ADD CONSTRAINT routes_profile_check CHECK (jsonb_array_length(elevation_profile) <= 200);
ALTER TABLE routes DROP CONSTRAINT IF EXISTS routes_origin_check;
ALTER TABLE routes ADD CONSTRAINT routes_origin_check CHECK (origin IN ('actividad', 'dibujada', 'oficial'));
UPDATE routes SET origin = 'oficial' WHERE author_id IS NULL;

-- Fotos: archivos en el bucket publico route-photos/<uid>/<ruta>/<archivo>.
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('route-photos', 'route-photos', true, 5242880, ARRAY['image/jpeg', 'image/png', 'image/webp'])
ON CONFLICT (id) DO UPDATE SET public = true, file_size_limit = 5242880,
  allowed_mime_types = ARRAY['image/jpeg', 'image/png', 'image/webp'];

DROP POLICY IF EXISTS "Fotos de rutas: subir en mi carpeta" ON storage.objects;
CREATE POLICY "Fotos de rutas: subir en mi carpeta" ON storage.objects FOR INSERT TO authenticated
  WITH CHECK (bucket_id = 'route-photos' AND (storage.foldername(name))[1] = auth.uid()::text);
DROP POLICY IF EXISTS "Fotos de rutas: borrar las mias" ON storage.objects;
CREATE POLICY "Fotos de rutas: borrar las mias" ON storage.objects FOR DELETE TO authenticated
  USING (bucket_id = 'route-photos' AND (storage.foldername(name))[1] = auth.uid()::text);

CREATE TABLE IF NOT EXISTS route_photos (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  route_id TEXT NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  path TEXT NOT NULL CHECK (path ~ '^[0-9a-f-]{36}/[^/]+/[^/]+$'),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS route_photos_route_idx ON route_photos(route_id, created_at DESC);
ALTER TABLE route_photos ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Ver fotos de rutas visibles" ON route_photos;
CREATE POLICY "Ver fotos de rutas visibles" ON route_photos FOR SELECT
  USING (EXISTS (SELECT 1 FROM routes r WHERE r.id = route_id));
DROP POLICY IF EXISTS "Subir mis fotos" ON route_photos;
CREATE POLICY "Subir mis fotos" ON route_photos FOR INSERT
  WITH CHECK (user_id = auth.uid() AND split_part(path, '/', 1) = auth.uid()::text
    AND EXISTS (SELECT 1 FROM routes r WHERE r.id = route_id));
DROP POLICY IF EXISTS "Borrar mis fotos" ON route_photos;
CREATE POLICY "Borrar mis fotos" ON route_photos FOR DELETE USING (user_id = auth.uid());

-- Quien ha corrido cada ruta: se registra al guardar una carrera iniciada con "Correr esta ruta".
CREATE TABLE IF NOT EXISTS route_runs (
  route_id TEXT NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  activity_id TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (route_id, activity_id)
);
CREATE INDEX IF NOT EXISTS route_runs_route_idx ON route_runs(route_id);
ALTER TABLE route_runs ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Mis corridas de rutas" ON route_runs;
CREATE POLICY "Mis corridas de rutas" ON route_runs FOR SELECT USING (user_id = auth.uid());
DROP POLICY IF EXISTS "Registrar mi corrida" ON route_runs;
CREATE POLICY "Registrar mi corrida" ON route_runs FOR INSERT
  WITH CHECK (user_id = auth.uid() AND EXISTS (SELECT 1 FROM routes r WHERE r.id = route_id));

-- Lista publica: solo perfiles publicos, la propia cuenta y a quienes sigo.
CREATE OR REPLACE FUNCTION public.corredores_ruta(ruta TEXT)
RETURNS TABLE (user_id UUID, nombre TEXT, usuario TEXT, veces BIGINT, ultima TIMESTAMPTZ, total BIGINT)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  WITH visibles AS (SELECT 1 FROM routes r WHERE r.id = ruta AND (r.is_public OR r.author_id = auth.uid()))
  SELECT rr.user_id, p.full_name, p.username, count(*), max(rr.created_at),
         (SELECT count(DISTINCT x.user_id) FROM route_runs x WHERE x.route_id = ruta)
  FROM route_runs rr JOIN profiles p ON p.id = rr.user_id
  WHERE rr.route_id = ruta AND EXISTS (SELECT 1 FROM visibles)
    AND (p.privacy_level = 'public' OR rr.user_id = auth.uid()
         OR EXISTS (SELECT 1 FROM follows f WHERE f.follower_id = auth.uid() AND f.following_id = rr.user_id))
  GROUP BY rr.user_id, p.full_name, p.username
  ORDER BY max(rr.created_at) DESC
  LIMIT 50;
$$;
REVOKE ALL ON FUNCTION public.corredores_ruta(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.corredores_ruta(TEXT) TO anon, authenticated;
