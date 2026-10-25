-- 022: rutas oficiales de Loja (parques, senderos, montaña, vías urbanas y pista).
-- Generado por scripts/rutas-loja/generar.py a partir de supabase/seed/rutas-loja.geojson.
-- Trazados (c) colaboradores de OpenStreetMap, licencia ODbL 1.0 (ver docs/RUTAS-LOJA.md).
-- Elevación: SRTM 30 m vía OpenTopoData. Idempotente: se puede ejecutar varias veces.

ALTER TABLE public.routes
  ADD COLUMN IF NOT EXISTS slug TEXT,
  ADD COLUMN IF NOT EXISTS category TEXT,
  ADD COLUMN IF NOT EXISTS surface TEXT,
  ADD COLUMN IF NOT EXISTS is_official BOOLEAN NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS source TEXT,
  ADD COLUMN IF NOT EXISTS license TEXT,
  ADD COLUMN IF NOT EXISTS elevation_loss_m INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS geom GEOGRAPHY(LineString, 4326);

ALTER TABLE public.routes DROP CONSTRAINT IF EXISTS routes_category_check;
ALTER TABLE public.routes ADD CONSTRAINT routes_category_check
  CHECK (category IS NULL OR category IN ('parque', 'sendero', 'montana', 'urbana', 'pista'));
ALTER TABLE public.routes DROP CONSTRAINT IF EXISTS routes_surface_check;
ALTER TABLE public.routes ADD CONSTRAINT routes_surface_check
  CHECK (surface IS NULL OR surface IN ('pavimento', 'tierra', 'mixta', 'tartan'));
-- Solo las rutas sin autor (cargadas por el servidor) pueden ser oficiales.
ALTER TABLE public.routes DROP CONSTRAINT IF EXISTS routes_official_check;
ALTER TABLE public.routes ADD CONSTRAINT routes_official_check CHECK (NOT is_official OR author_id IS NULL);
CREATE UNIQUE INDEX IF NOT EXISTS routes_slug_idx ON public.routes(slug) WHERE slug IS NOT NULL;
CREATE INDEX IF NOT EXISTS routes_geom_idx ON public.routes USING GIST (geom);
CREATE INDEX IF NOT EXISTS routes_official_idx ON public.routes(is_official, category);

-- La linea PostGIS se calcula siempre a partir del trazo (path).
CREATE OR REPLACE FUNCTION public.route_geo() RETURNS TRIGGER
LANGUAGE plpgsql SET search_path = public AS $$
DECLARE
  linea GEOGRAPHY;
BEGIN
  IF jsonb_array_length(NEW.path) >= 1 THEN
    NEW.start_point := ST_SetSRID(ST_MakePoint((NEW.path->0->>'lng')::float, (NEW.path->0->>'lat')::float), 4326)::geography;
  END IF;
  IF jsonb_array_length(NEW.path) >= 2 THEN
    SELECT ST_MakeLine(ST_SetSRID(ST_MakePoint((p->>'lng')::float, (p->>'lat')::float), 4326) ORDER BY i)::geography
      INTO linea FROM jsonb_array_elements(NEW.path) WITH ORDINALITY AS t(p, i);
    NEW.geom := linea;
    IF NEW.author_id IS NOT NULL THEN
      NEW.distance_km := round((ST_Length(linea) / 1000)::numeric, 2);
    END IF;
  END IF;
  RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS routes_geo ON public.routes;
CREATE TRIGGER routes_geo BEFORE INSERT OR UPDATE OF path ON public.routes
  FOR EACH ROW EXECUTE FUNCTION public.route_geo();
UPDATE public.routes SET path = path WHERE geom IS NULL AND jsonb_array_length(path) >= 2;

INSERT INTO public.routes (id, slug, name, distance_km, difficulty, elevation_m, elevation_loss_m, description,
  path, tags, elevation_profile, category, surface, source, license, is_official, origin, author_id, is_public)
SELECT v.id, v.slug, v.name, v.distance_km, v.difficulty, v.elevation_m, v.elevation_loss_m, v.description,
  v.path, v.tags, v.elevation_profile, v.category, v.surface, v.source, v.license, true, 'oficial', NULL, true
FROM (VALUES
