-- 035: ruta sugerida para hoy (contexto Rutas). Idempotente. Ver docs/RUTA-SUGERIDA.md y
-- docs/adr/007-ruta-sugerida.md.
--  - rutas.route_suggestions guarda una sugerencia por usuario y por dia de Ecuador
--    (America/Guayaquil). Vista public.route_suggestions (security_invoker) segun ADR 005.
--  - public.ruta_sugerida_hoy(p_lat, p_lng) elige, registra y devuelve la sugerencia del dia.
--    Si ya hay una para hoy devuelve la misma (estable durante el dia).
-- Reglas:
--  1. Segura: nivel_seguridad_ruta (ECU 911 + reportes de corredores) para el dia de la semana y
--     la hora actuales de Ecuador. Segura = indice <= 1.0 (no peor que el promedio del canton:
--     nivel 'bajo' o 'medio' sin superar el promedio). Nunca se sugiere una ruta por encima.
--  2. Sin repetir: se excluyen rutas corridas o sugeridas en los dias de Ecuador hoy-3 .. hoy.
--     Corrida = fila en route_runs, o actividad cuyo inicio queda a <= 300 m del inicio de la ruta
--     y cuya distancia esta entre 70 % y 130 % de la ruta (coincidencia geografica).
--  3. Orden: |distancia - objetivo| / objetivo (objetivo = media de las ultimas 5 actividades,
--     5 km sin historial) + distancia del usuario al inicio de la ruta / 10 km si hay ubicacion.
--     Desempate: md5(id || fecha), que rota entre dias.
--  Si nada cumple 1 y 2 devuelve estado 'sin_ruta' con el motivo; nunca relaja la seguridad.

CREATE TABLE IF NOT EXISTS rutas.route_suggestions (
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  dia DATE NOT NULL,
  route_id TEXT NOT NULL REFERENCES rutas.routes(id) ON DELETE CASCADE,
  nivel TEXT NOT NULL CHECK (nivel IN ('bajo', 'medio')),
  indice NUMERIC NOT NULL,
  objetivo_km NUMERIC NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, dia)
);
CREATE INDEX IF NOT EXISTS route_suggestions_ruta_idx ON rutas.route_suggestions (user_id, route_id, dia);
ALTER TABLE rutas.route_suggestions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Mis sugerencias" ON rutas.route_suggestions;
CREATE POLICY "Mis sugerencias" ON rutas.route_suggestions FOR SELECT USING (user_id = auth.uid());
-- Sin politicas de escritura: solo la funcion ruta_sugerida_hoy (security definer) inserta.
REVOKE ALL ON rutas.route_suggestions FROM anon, authenticated;
GRANT SELECT ON rutas.route_suggestions TO authenticated;
GRANT ALL ON rutas.route_suggestions TO service_role;

CREATE OR REPLACE VIEW public.route_suggestions WITH (security_invoker = true) AS
  SELECT * FROM rutas.route_suggestions;
REVOKE ALL ON public.route_suggestions FROM anon, authenticated;
GRANT SELECT ON public.route_suggestions TO authenticated;

DO $$ BEGIN
  IF to_regclass('kernel.propiedad_tablas') IS NOT NULL THEN
    EXECUTE $q$INSERT INTO kernel.propiedad_tablas (tabla, contexto, esquema_actual)
      VALUES ('route_suggestions', 'rutas', 'rutas') ON CONFLICT DO NOTHING$q$;
  END IF;
EXCEPTION WHEN others THEN NULL;
END $$;

CREATE OR REPLACE FUNCTION public.ruta_sugerida_hoy(
  p_lat DOUBLE PRECISION DEFAULT NULL, p_lng DOUBLE PRECISION DEFAULT NULL
) RETURNS JSONB
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path = public AS $$
DECLARE
  uid UUID := auth.uid();
  local TIMESTAMP := now() AT TIME ZONE 'America/Guayaquil';
  hoy DATE := local::date;
  desde DATE := hoy - 3;
  dow INTEGER := extract(dow FROM local)::int;
  hora INTEGER := extract(hour FROM local)::int;
  objetivo NUMERIC;
  usuario GEOGRAPHY;
  elegida RECORD;
  previa RECORD;
  seguras_ids TEXT[];
  n_seguras INTEGER;
BEGIN
  IF p_lat IS NOT NULL AND p_lng IS NOT NULL AND p_lat BETWEEN -90 AND 90 AND p_lng BETWEEN -180 AND 180 THEN
    usuario := ST_SetSRID(ST_MakePoint(p_lng, p_lat), 4326)::geography;
  END IF;

  SELECT COALESCE(avg(km), 5) INTO objetivo FROM (
    SELECT a.distance_meters / 1000.0 AS km FROM activities a
    WHERE uid IS NOT NULL AND a.user_id = uid AND a.distance_meters > 0
    ORDER BY a.start_time DESC LIMIT 5) u;
  objetivo := greatest(round(objetivo, 2), 0.5);

  -- Estable durante el dia.
  IF uid IS NOT NULL THEN
    SELECT s.*, r.name, r.description, r.distance_km, r.elevation_m, r.difficulty
      INTO previa FROM rutas.route_suggestions s JOIN rutas.routes r ON r.id = s.route_id
      WHERE s.user_id = uid AND s.dia = hoy;
    IF FOUND THEN
      RETURN jsonb_build_object('estado', 'ok', 'dia', hoy, 'nueva', false,
        'ruta', jsonb_build_object('id', previa.route_id, 'nombre', previa.name, 'descripcion', previa.description,
          'distancia_km', previa.distance_km, 'elevacion_m', previa.elevation_m, 'dificultad', previa.difficulty),
        'nivel', previa.nivel, 'indice', previa.indice, 'objetivo_km', previa.objetivo_km);
    END IF;
  END IF;

  CREATE TEMP TABLE IF NOT EXISTS _cand (id TEXT, name TEXT, description TEXT, distance_km NUMERIC,
    elevation_m INTEGER, difficulty TEXT, inicio GEOGRAPHY, nivel TEXT, indice NUMERIC,
    repetida BOOLEAN, puntaje NUMERIC, giro TEXT) ON COMMIT DROP;
  TRUNCATE _cand;
  INSERT INTO _cand
  SELECT r.id, r.name, r.description, r.distance_km, r.elevation_m, r.difficulty,
         ST_SetSRID(ST_MakePoint((r.path->0->>'lng')::float, (r.path->0->>'lat')::float), 4326)::geography,
         s.nivel, s.indice, false, 0, md5(r.id || hoy::text)
  FROM rutas.routes r
  LEFT JOIN LATERAL (SELECT n.nivel, n.indice FROM nivel_seguridad_ruta(r.id, dow, hora) n WHERE n.tramo = 0) s ON true
  WHERE (r.is_public OR r.author_id = uid) AND jsonb_array_length(r.path) >= 2 AND r.distance_km > 0;

  -- Regla 1: solo seguras (sin dato de seguridad no se considera segura).
  DELETE FROM _cand WHERE indice IS NULL OR indice > 1.0 OR nivel = 'alto';
  SELECT count(*), array_agg(id) INTO n_seguras, seguras_ids FROM _cand;

  -- Regla 2: sin repetir (corridas o sugeridas de hoy-3 a hoy).
  IF uid IS NOT NULL THEN
    UPDATE _cand c SET repetida = true WHERE
      EXISTS (SELECT 1 FROM rutas.route_suggestions s WHERE s.user_id = uid AND s.route_id = c.id
              AND s.dia BETWEEN desde AND hoy)
      OR EXISTS (SELECT 1 FROM rutas.route_runs rr WHERE rr.user_id = uid AND rr.route_id = c.id
              AND (rr.created_at AT TIME ZONE 'America/Guayaquil')::date BETWEEN desde AND hoy)
      OR EXISTS (SELECT 1 FROM activities a WHERE a.user_id = uid
              AND (a.start_time AT TIME ZONE 'America/Guayaquil')::date BETWEEN desde AND hoy
              AND a.started_location IS NOT NULL AND c.inicio IS NOT NULL
              AND ST_DWithin(a.started_location, c.inicio, 300)
              AND a.distance_meters / 1000.0 BETWEEN c.distance_km * 0.7 AND c.distance_km * 1.3);
  END IF;
  DELETE FROM _cand WHERE repetida;

  -- Regla 3: cercania a la distancia objetivo y al usuario; desempate rotativo.
  UPDATE _cand SET puntaje = abs(distance_km - objetivo) / objetivo
    + CASE WHEN usuario IS NOT NULL AND inicio IS NOT NULL THEN ST_Distance(usuario, inicio) / 10000.0 ELSE 0 END;
  SELECT * INTO elegida FROM _cand ORDER BY puntaje, giro LIMIT 1;

  IF NOT FOUND THEN
    RETURN jsonb_build_object('estado', 'sin_ruta', 'dia', hoy, 'objetivo_km', objetivo,
      'motivo', CASE WHEN COALESCE(n_seguras, 0) = 0
        THEN 'Ahora ninguna ruta tiene un nivel de seguridad suficiente para esta hora.'
        ELSE 'Ya corriste o te sugerimos todas las rutas seguras en los últimos 3 días.' END);
  END IF;

  IF uid IS NOT NULL THEN
    INSERT INTO rutas.route_suggestions (user_id, dia, route_id, nivel, indice, objetivo_km)
      VALUES (uid, hoy, elegida.id, elegida.nivel, elegida.indice, objetivo)
      ON CONFLICT (user_id, dia) DO NOTHING;
    -- Si otra llamada simultanea gano, devolver la suya.
    IF NOT FOUND THEN RETURN public.ruta_sugerida_hoy(p_lat, p_lng); END IF;
  END IF;

  RETURN jsonb_build_object('estado', 'ok', 'dia', hoy, 'nueva', true,
    'ruta', jsonb_build_object('id', elegida.id, 'nombre', elegida.name, 'descripcion', elegida.description,
      'distancia_km', elegida.distance_km, 'elevacion_m', elegida.elevation_m, 'dificultad', elegida.difficulty),
    'nivel', elegida.nivel, 'indice', elegida.indice, 'objetivo_km', objetivo,
    'seguras', to_jsonb(COALESCE(seguras_ids, '{}'::text[])));
END;
$$;
REVOKE ALL ON FUNCTION public.ruta_sugerida_hoy(DOUBLE PRECISION, DOUBLE PRECISION) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.ruta_sugerida_hoy(DOUBLE PRECISION, DOUBLE PRECISION) TO anon, authenticated;

NOTIFY pgrst, 'reload schema';
