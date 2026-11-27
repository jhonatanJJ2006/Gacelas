-- 049: sistema unico de logros. Hasta hoy convivian dos: 20 logros (tabla user_achievements,
-- Android) y 12 medallas (calculadas en el telefono o el navegador, sin tabla). Esta migracion
-- deja un solo catalogo de 26 logros, sin duplicados, y un solo sitio donde se guarda lo ganado.
--
--   * logros.catalogo_logros: catalogo unico. La columna medalla_origen dice que medalla
--     antigua corresponde a cada logro (12 medallas: 6 ya eran un logro y 6 pasan a serlo).
--   * user_achievements.code deja de usar un CHECK con la lista escrita a mano y pasa a una
--     clave foranea al catalogo.
--   * logros.logro_merecido valida en el servidor los 26 codigos con carreras verificadas
--     (046 solo validaba 4 y rechazaba los otros 16; como la app sube todos los logros nuevos
--     en una sola peticion, un solo rechazo tiraba el lote entero).
--   * Se recalcula lo ganado por cada usuario con carreras verificadas, sin borrar nada.
--
-- Las medallas nunca se guardaron en la base: lo ganado se recalcula desde las carreras
-- verificadas. No se borra ni se modifica ninguna fila existente de user_achievements.
-- Para quitarlo: supabase/rollback/049_logros_unificados.down.sql
-- Plan de aplicacion y reversion: docs/LOGROS-UNIFICADOS.md

BEGIN;

CREATE TABLE IF NOT EXISTS logros.catalogo_logros (
  code TEXT PRIMARY KEY,
  titulo TEXT NOT NULL,
  descripcion TEXT NOT NULL,
  como_conseguirlo TEXT NOT NULL,
  categoria TEXT NOT NULL CHECK (categoria IN ('distancia', 'constancia', 'velocidad', 'exploracion', 'horario')),
  meta INTEGER NOT NULL CHECK (meta > 0),
  unidad TEXT NOT NULL DEFAULT '',
  medalla_origen TEXT UNIQUE,
  orden INTEGER NOT NULL
);
COMMENT ON TABLE logros.catalogo_logros IS 'Catalogo unico de logros (049). Misma lista que packages/shared-types/logros-catalogo.json.';
COMMENT ON COLUMN logros.catalogo_logros.medalla_origen IS 'Id de la medalla antigua que este logro reemplaza (primera, 5k, 10k, 21k, 42k, centenario, 500k, 1000k, racha7, racha30, desnivel1000, desnivel5000).';

INSERT INTO logros.catalogo_logros (code, titulo, descripcion, como_conseguirlo, categoria, meta, unidad, medalla_origen, orden) VALUES
  ('km_5', '5 km', 'Corriste 5 km en una carrera.', 'Completa 5 km en una sola carrera.', 'distancia', 5, 'km', '5k', 1),
  ('km_10', '10 km', 'Corriste 10 km en una carrera.', 'Completa 10 km en una sola carrera.', 'distancia', 10, 'km', '10k', 2),
  ('km_15', '15 km', 'Corriste 15 km en una carrera.', 'Completa 15 km en una sola carrera.', 'distancia', 15, 'km', NULL, 3),
  ('km_21', 'Media maratón', 'Corriste 21,1 km en una carrera.', 'Completa los 21,1 km de una media maratón en una sola carrera.', 'distancia', 21, 'km', '21k', 4),
  ('km_42', 'Maratón', 'Corriste 42,2 km en una carrera.', 'Completa los 42,2 km de un maratón en una sola carrera.', 'distancia', 42, 'km', '42k', 5),
  ('total_50', '50 km acumulados', 'Sumaste 50 km entre todas tus carreras.', 'Suma 50 km entre todas tus carreras guardadas.', 'distancia', 50, 'km', NULL, 6),
  ('total_100', 'Centenario', 'Sumaste 100 km entre todas tus carreras.', 'Suma 100 km entre todas tus carreras guardadas.', 'distancia', 100, 'km', 'centenario', 7),
  ('total_500', 'Quinientos', 'Sumaste 500 km entre todas tus carreras.', 'Suma 500 km entre todas tus carreras guardadas.', 'distancia', 500, 'km', '500k', 8),
  ('total_1000', 'Mil kilómetros', 'Sumaste 1000 km entre todas tus carreras.', 'Suma 1000 km entre todas tus carreras guardadas.', 'distancia', 1000, 'km', '1000k', 9),
  ('primera_carrera', 'Primera carrera', 'Grabaste tu primera carrera.', 'Graba y guarda tu primera carrera con el botón Correr.', 'constancia', 1, 'carrera', 'primera', 10),
  ('racha_3', 'Racha de 3 días', 'Corriste 3 días seguidos.', 'Corre tres días seguidos.', 'constancia', 3, 'días', NULL, 11),
  ('racha_7', 'Racha de 7 días', 'Corriste 7 días seguidos.', 'Corre al menos una vez cada día durante 7 días seguidos.', 'constancia', 7, 'días', 'racha7', 12),
  ('racha_30', 'Racha de 30 días', 'Corriste 30 días seguidos.', 'Corre al menos una vez cada día durante un mes.', 'constancia', 30, 'días', 'racha30', 13),
  ('carreras_10', '10 carreras', 'Guardaste 10 carreras.', 'Guarda 10 carreras en tu historial.', 'constancia', 10, 'carreras', NULL, 14),
  ('carreras_50', '50 carreras', 'Guardaste 50 carreras.', 'Guarda 50 carreras en tu historial.', 'constancia', 50, 'carreras', NULL, 15),
  ('semanas_meta_4', 'Mes constante', 'Cumpliste tu meta semanal 4 semanas seguidas.', 'Cumple tu meta de carreras por semana durante 4 semanas seguidas.', 'constancia', 4, 'semanas', NULL, 16),
  ('ritmo_6', 'Bajo 6:00', 'Corriste 3 km o más a menos de 6:00 por km.', 'Corre al menos 3 km con un ritmo medio menor a 6:00 por km.', 'velocidad', 1, '', NULL, 17),
  ('ritmo_5', 'Bajo 5:00', 'Corriste 3 km o más a menos de 5:00 por km.', 'Corre al menos 3 km con un ritmo medio menor a 5:00 por km.', 'velocidad', 1, '', NULL, 18),
  ('ritmo_430', 'Bajo 4:30', 'Corriste 5 km o más a menos de 4:30 por km.', 'Corre al menos 5 km con un ritmo medio menor a 4:30 por km.', 'velocidad', 1, '', NULL, 19),
  ('escalador', 'Escalador', 'Subiste 100 m en una carrera.', 'Acumula 100 m de desnivel positivo en una sola carrera.', 'exploracion', 100, 'm', NULL, 20),
  ('explorador', 'Explorador', 'Empezaste carreras en 5 lugares distintos.', 'Sal a correr desde 5 lugares distintos de la ciudad.', 'exploracion', 5, 'lugares', NULL, 21),
  ('desnivel_1000', 'Andino', 'Acumulaste 1000 m de subida entre todas tus carreras.', 'Acumula 1000 m de desnivel positivo entre tus carreras.', 'exploracion', 1000, 'm', 'desnivel1000', 22),
  ('desnivel_5000', 'Cumbre', 'Acumulaste 5000 m de subida entre todas tus carreras.', 'Acumula 5000 m de desnivel positivo entre tus carreras.', 'exploracion', 5000, 'm', 'desnivel5000', 23),
  ('madrugador', 'Madrugador', 'Saliste a correr 3 veces antes de las 7:00.', 'Empieza 3 carreras antes de las 7:00 de la mañana.', 'horario', 3, 'salidas', NULL, 24),
  ('nocturno', 'Corredor nocturno', 'Saliste a correr 3 veces después de las 19:00.', 'Empieza 3 carreras después de las 19:00.', 'horario', 3, 'salidas', NULL, 25),
  ('fin_de_semana', 'Guerrero de fin de semana', 'Corriste 5 sábados o domingos.', 'Corre 5 veces en sábado o domingo.', 'horario', 5, 'salidas', NULL, 26)
ON CONFLICT (code) DO UPDATE SET
  titulo = EXCLUDED.titulo, descripcion = EXCLUDED.descripcion, como_conseguirlo = EXCLUDED.como_conseguirlo,
  categoria = EXCLUDED.categoria, meta = EXCLUDED.meta, unidad = EXCLUDED.unidad,
  medalla_origen = EXCLUDED.medalla_origen, orden = EXCLUDED.orden;

ALTER TABLE logros.catalogo_logros ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Ver catalogo de logros" ON logros.catalogo_logros;
CREATE POLICY "Ver catalogo de logros" ON logros.catalogo_logros FOR SELECT TO authenticated USING (true);
REVOKE ALL ON logros.catalogo_logros FROM PUBLIC, anon, authenticated;
GRANT SELECT ON logros.catalogo_logros TO authenticated;
GRANT ALL ON logros.catalogo_logros TO service_role;

-- 043 recorre las tablas con RLS que ya existian; esta se crea despues y necesita el mismo trigger.
DROP TRIGGER IF EXISTS aa_bloquear_suspendido ON logros.catalogo_logros;
CREATE TRIGGER aa_bloquear_suspendido
  BEFORE INSERT OR UPDATE OR DELETE ON logros.catalogo_logros
  FOR EACH ROW EXECUTE FUNCTION identidad.trg_bloquear_suspendido();

CREATE OR REPLACE VIEW public.catalogo_logros WITH (security_invoker = true) AS
  SELECT * FROM logros.catalogo_logros;
REVOKE ALL ON public.catalogo_logros FROM PUBLIC, anon, authenticated;
GRANT SELECT ON public.catalogo_logros TO authenticated;
GRANT ALL ON public.catalogo_logros TO service_role;

INSERT INTO kernel.propiedad_tablas (tabla, contexto, esquema_actual)
  VALUES ('catalogo_logros', 'logros', 'logros')
  ON CONFLICT (tabla) DO UPDATE SET contexto = EXCLUDED.contexto, esquema_actual = EXCLUDED.esquema_actual;

-- Lo ya ganado se conserva: todos los codigos de user_achievements existen en el catalogo
-- (los 20 de 048). Si apareciera uno ajeno, la migracion se detiene en vez de borrarlo.
DO $mig$
DECLARE ajenos TEXT;
BEGIN
  SELECT string_agg(DISTINCT u.code, ', ') INTO ajenos
  FROM logros.user_achievements u
  WHERE NOT EXISTS (SELECT 1 FROM logros.catalogo_logros c WHERE c.code = u.code);
  IF ajenos IS NOT NULL THEN
    RAISE EXCEPTION 'user_achievements tiene codigos fuera del catalogo: %', ajenos;
  END IF;
END
$mig$;

ALTER TABLE logros.user_achievements DROP CONSTRAINT IF EXISTS user_achievements_code_check;
ALTER TABLE logros.user_achievements DROP CONSTRAINT IF EXISTS user_achievements_code_fkey;
ALTER TABLE logros.user_achievements ADD CONSTRAINT user_achievements_code_fkey
  FOREIGN KEY (code) REFERENCES logros.catalogo_logros(code) ON UPDATE CASCADE;

-- Reglas del servidor para los 26 codigos. Solo cuentan carreras verificadas (046). La zona
-- horaria es la de recalcular_racha. Los umbrales son los mismos que usan web y Android.
CREATE OR REPLACE FUNCTION logros.logro_merecido(uid uuid, codigo text)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, extensions, logros
AS $$
  WITH v AS (
    SELECT a.distance_meters AS m,
           a.duration_seconds AS s,
           coalesce(a.elevation_gain_meters, 0) AS d,
           a.start_time AT TIME ZONE 'America/Guayaquil' AS local_ts,
           a.started_location AS lugar
    FROM activities a
    WHERE a.user_id = uid AND a.verificada
  ), r AS (
    SELECT count(*) AS n,
           coalesce(max(m), 0) AS max_m,
           coalesce(sum(m), 0) AS sum_m,
           coalesce(sum(d), 0) AS sum_d,
           coalesce(max(d), 0) AS max_d,
           coalesce(bool_or(m >= 3000 AND s > 0 AND s * 1000.0 / m < 360), false) AS ritmo6,
           coalesce(bool_or(m >= 3000 AND s > 0 AND s * 1000.0 / m < 300), false) AS ritmo5,
           coalesce(bool_or(m >= 5000 AND s > 0 AND s * 1000.0 / m < 270), false) AS ritmo430,
           count(*) FILTER (WHERE extract(hour FROM local_ts) < 7) AS madrugadas,
           count(*) FILTER (WHERE extract(hour FROM local_ts) >= 19) AS noches,
           count(*) FILTER (WHERE extract(isodow FROM local_ts) >= 6) AS finde
    FROM v
  )
  SELECT CASE codigo
    WHEN 'primera_carrera' THEN r.n >= 1
    WHEN 'km_5' THEN r.max_m >= 5000
    WHEN 'km_10' THEN r.max_m >= 10000
    WHEN 'km_15' THEN r.max_m >= 15000
    WHEN 'km_21' THEN r.max_m >= 21097
    WHEN 'km_42' THEN r.max_m >= 42195
    WHEN 'total_50' THEN r.sum_m >= 50000
    WHEN 'total_100' THEN r.sum_m >= 100000
    WHEN 'total_500' THEN r.sum_m >= 500000
    WHEN 'total_1000' THEN r.sum_m >= 1000000
    WHEN 'carreras_10' THEN r.n >= 10
    WHEN 'carreras_50' THEN r.n >= 50
    WHEN 'racha_3' THEN coalesce((SELECT GREATEST(s.current_daily, s.best_daily) FROM user_streaks s WHERE s.user_id = uid), 0) >= 3
    WHEN 'racha_7' THEN coalesce((SELECT GREATEST(s.current_daily, s.best_daily) FROM user_streaks s WHERE s.user_id = uid), 0) >= 7
    WHEN 'racha_30' THEN coalesce((SELECT GREATEST(s.current_daily, s.best_daily) FROM user_streaks s WHERE s.user_id = uid), 0) >= 30
    WHEN 'semanas_meta_4' THEN coalesce((SELECT s.best_weekly FROM user_streaks s WHERE s.user_id = uid), 0) >= 4
    WHEN 'ritmo_6' THEN r.ritmo6
    WHEN 'ritmo_5' THEN r.ritmo5
    WHEN 'ritmo_430' THEN r.ritmo430
    WHEN 'escalador' THEN r.max_d >= 100
    WHEN 'desnivel_1000' THEN r.sum_d >= 1000
    WHEN 'desnivel_5000' THEN r.sum_d >= 5000
    WHEN 'explorador' THEN (
      SELECT count(*) FROM (
        SELECT DISTINCT trunc(ST_Y(v.lugar::geometry) * 100) AS la, trunc(ST_X(v.lugar::geometry) * 100) AS lo
        FROM v WHERE v.lugar IS NOT NULL
      ) z
    ) >= 5
    WHEN 'madrugador' THEN r.madrugadas >= 3
    WHEN 'nocturno' THEN r.noches >= 3
    WHEN 'fin_de_semana' THEN r.finde >= 5
    ELSE false
  END
  FROM r;
$$;

CREATE OR REPLACE FUNCTION logros.conceder_verificados(uid uuid)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, logros
AS $$
BEGIN
  IF uid IS NULL THEN RETURN; END IF;
  INSERT INTO public.user_achievements (user_id, code)
  SELECT uid, c.code
  FROM logros.catalogo_logros c
  WHERE NOT EXISTS (SELECT 1 FROM public.user_achievements u WHERE u.user_id = uid AND u.code = c.code)
    AND logros.logro_merecido(uid, c.code)
  ON CONFLICT DO NOTHING;
END;
$$;

REVOKE ALL ON FUNCTION logros.logro_merecido(uuid, text) FROM PUBLIC;
REVOKE ALL ON FUNCTION logros.conceder_verificados(uuid) FROM PUBLIC;
DO $mig$
DECLARE rol text;
BEGIN
  FOREACH rol IN ARRAY ARRAY['anon', 'authenticated', 'service_role', 'authenticator'] LOOP
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = rol) THEN
      EXECUTE format('GRANT EXECUTE ON FUNCTION logros.logro_merecido(uuid, text) TO %I', rol);
    END IF;
  END LOOP;
END
$mig$;

-- Recalculo de lo ganado: cada usuario con carreras verificadas recibe los logros del
-- catalogo que le corresponden (incluidas las 6 medallas que ahora son logros). No quita nada.
SELECT logros.conceder_verificados(s.user_id)
FROM (SELECT DISTINCT user_id FROM public.activities WHERE verificada) s;

NOTIFY pgrst, 'reload schema';

COMMIT;
