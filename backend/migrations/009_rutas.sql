-- Fase 4: rutas y exploracion.
-- Rutas creadas desde una actividad (trazo simplificado en el cliente con Douglas-Peucker),
-- etiquetas, punto de salida PostGIS para "rutas cercanas", likes y reseñas con estrellas.

ALTER TABLE routes
  ADD COLUMN IF NOT EXISTS tags TEXT[] NOT NULL DEFAULT '{}',
  ADD COLUMN IF NOT EXISTS start_point GEOGRAPHY(Point, 4326),
  ADD COLUMN IF NOT EXISTS likes_count INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS rating_avg NUMERIC(3,2),
  ADD COLUMN IF NOT EXISTS rating_count INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS source_activity_id TEXT;

ALTER TABLE routes DROP CONSTRAINT IF EXISTS routes_tags_check;
ALTER TABLE routes ADD CONSTRAINT routes_tags_check
  CHECK (tags <@ ARRAY['panoramica','plana','urbana','sendero','montana','parque','rio']::TEXT[]);
ALTER TABLE routes DROP CONSTRAINT IF EXISTS routes_name_check;
ALTER TABLE routes ADD CONSTRAINT routes_name_check CHECK (char_length(name) BETWEEN 3 AND 80);
ALTER TABLE routes DROP CONSTRAINT IF EXISTS routes_path_check;
ALTER TABLE routes ADD CONSTRAINT routes_path_check CHECK (jsonb_array_length(path) <= 500);

-- La salida se calcula en el servidor a partir del trazo; la distancia tambien, salvo en
-- las rutas oficiales (sin autor), cuyo trazo es aproximado.
CREATE OR REPLACE FUNCTION public.route_geo() RETURNS TRIGGER
LANGUAGE plpgsql SET search_path = public AS $$
DECLARE
  linea GEOGRAPHY;
BEGIN
  IF jsonb_array_length(NEW.path) >= 1 THEN
    NEW.start_point := ST_SetSRID(ST_MakePoint((NEW.path->0->>'lng')::float, (NEW.path->0->>'lat')::float), 4326)::geography;
  END IF;
  IF jsonb_array_length(NEW.path) >= 2 AND NEW.author_id IS NOT NULL THEN
    SELECT ST_MakeLine(ST_SetSRID(ST_MakePoint((p->>'lng')::float, (p->>'lat')::float), 4326) ORDER BY i)::geography
      INTO linea FROM jsonb_array_elements(NEW.path) WITH ORDINALITY AS t(p, i);
    NEW.distance_km := round((ST_Length(linea) / 1000)::numeric, 2);
  END IF;
  RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS routes_geo ON routes;
CREATE TRIGGER routes_geo BEFORE INSERT OR UPDATE OF path ON routes FOR EACH ROW EXECUTE FUNCTION route_geo();
UPDATE routes SET path = path; -- calcula start_point de las rutas existentes
UPDATE routes SET distance_km = CASE id WHEN '1' THEN 5.2 WHEN '2' THEN 8.5 WHEN '3' THEN 12.3 END
  WHERE author_id IS NULL AND id IN ('1', '2', '3');
CREATE INDEX IF NOT EXISTS routes_start_idx ON routes USING GIST (start_point);

-- Los contadores solo los cambian los triggers de likes y reseñas (profundidad > 1).
CREATE OR REPLACE FUNCTION public.route_proteger_contadores() RETURNS TRIGGER
LANGUAGE plpgsql SET search_path = public AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    NEW.likes_count := 0; NEW.rating_count := 0; NEW.rating_avg := NULL;
  ELSIF pg_trigger_depth() <= 1 THEN
    NEW.likes_count := OLD.likes_count; NEW.rating_count := OLD.rating_count; NEW.rating_avg := OLD.rating_avg;
  END IF;
  RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS routes_contadores ON routes;
CREATE TRIGGER routes_contadores BEFORE INSERT OR UPDATE ON routes FOR EACH ROW EXECUTE FUNCTION route_proteger_contadores();

CREATE TABLE IF NOT EXISTS route_likes (
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  route_id TEXT NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, route_id)
);
ALTER TABLE route_likes ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Ver likes de rutas visibles" ON route_likes;
CREATE POLICY "Ver likes de rutas visibles" ON route_likes FOR SELECT
  USING (EXISTS (SELECT 1 FROM routes r WHERE r.id = route_id));
DROP POLICY IF EXISTS "Dar like" ON route_likes;
CREATE POLICY "Dar like" ON route_likes FOR INSERT
  WITH CHECK (user_id = auth.uid() AND EXISTS (SELECT 1 FROM routes r WHERE r.id = route_id));
DROP POLICY IF EXISTS "Quitar like" ON route_likes;
CREATE POLICY "Quitar like" ON route_likes FOR DELETE USING (user_id = auth.uid());

CREATE TABLE IF NOT EXISTS route_reviews (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  route_id TEXT NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
  comment TEXT NOT NULL DEFAULT '' CHECK (char_length(comment) <= 500),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (route_id, user_id)
);
ALTER TABLE route_reviews ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Ver resenas de rutas visibles" ON route_reviews;
CREATE POLICY "Ver resenas de rutas visibles" ON route_reviews FOR SELECT
  USING (EXISTS (SELECT 1 FROM routes r WHERE r.id = route_id));
DROP POLICY IF EXISTS "Crear mi resena" ON route_reviews;
CREATE POLICY "Crear mi resena" ON route_reviews FOR INSERT
  WITH CHECK (user_id = auth.uid() AND EXISTS (SELECT 1 FROM routes r WHERE r.id = route_id));
DROP POLICY IF EXISTS "Editar mi resena" ON route_reviews;
CREATE POLICY "Editar mi resena" ON route_reviews FOR UPDATE USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());
DROP POLICY IF EXISTS "Borrar mi resena" ON route_reviews;
CREATE POLICY "Borrar mi resena" ON route_reviews FOR DELETE USING (user_id = auth.uid());

CREATE OR REPLACE FUNCTION public.route_counts() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  rid TEXT := COALESCE(NEW.route_id, OLD.route_id);
BEGIN
  UPDATE routes SET
    likes_count = (SELECT count(*) FROM route_likes WHERE route_id = rid),
    rating_count = (SELECT count(*) FROM route_reviews WHERE route_id = rid),
    rating_avg = (SELECT round(avg(rating)::numeric, 2) FROM route_reviews WHERE route_id = rid)
  WHERE id = rid;
  RETURN NULL;
END;
$$;
DROP TRIGGER IF EXISTS route_likes_count ON route_likes;
CREATE TRIGGER route_likes_count AFTER INSERT OR DELETE ON route_likes FOR EACH ROW EXECUTE FUNCTION route_counts();
DROP TRIGGER IF EXISTS route_reviews_count ON route_reviews;
CREATE TRIGGER route_reviews_count AFTER INSERT OR UPDATE OR DELETE ON route_reviews FOR EACH ROW EXECUTE FUNCTION route_counts();

-- Rutas visibles mas cercanas a un punto (respeta el RLS de routes: SECURITY INVOKER).
CREATE OR REPLACE FUNCTION public.rutas_cercanas(lat DOUBLE PRECISION, lng DOUBLE PRECISION, radio_km DOUBLE PRECISION DEFAULT 50)
RETURNS TABLE (id TEXT, distancia_km DOUBLE PRECISION)
LANGUAGE sql STABLE SECURITY INVOKER SET search_path = public AS $$
  SELECT r.id, ST_Distance(r.start_point, ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography) / 1000
  FROM routes r
  WHERE r.start_point IS NOT NULL
    AND ST_DWithin(r.start_point, ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography, radio_km * 1000)
  ORDER BY 2
  LIMIT 100;
$$;
GRANT EXECUTE ON FUNCTION public.rutas_cercanas(DOUBLE PRECISION, DOUBLE PRECISION, DOUBLE PRECISION) TO anon, authenticated;

UPDATE routes SET tags = '{parque,plana,urbana}' WHERE id = '1' AND tags = '{}';
UPDATE routes SET tags = '{rio,panoramica}' WHERE id = '2' AND tags = '{}';
UPDATE routes SET tags = '{montana,sendero,panoramica}' WHERE id = '3' AND tags = '{}';
