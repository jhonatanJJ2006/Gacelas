-- 040: datos para el rediseno de la web (docs/REDISENO-WEB.md). Idempotente y solo aditiva.
--  1. identidad.user_settings.weekly_goal_km: meta semanal en km (una sola meta en Inicio, Perfil y
--     la barra lateral). La web la sincroniza con ajustes.metaKmSemana.
--  2. eventos.reprogramar_eventos_demo(): los eventos de ejemplo (038 y la semilla 003, sin creador)
--     que ya pasaron se mueven semanas completas hacia adelante, mismo dia y hora. pg_cron lo corre
--     a diario, asi nunca se ven eventos de ejemplo con fechas pasadas.
--  3. public.seguridad_rutas(): resumen de seguridad real por ruta publica (nivel de
--     nivel_seguridad_ruta a esta hora y reportes de corredores a <= 200 m en 7 y 30 dias), con cache
--     diaria en rutas.seguridad_rutas_cache. El puntaje 0-100 solo se calcula si hay datos del ECU 911
--     importados; sin ellos es null y la web muestra el nivel y los reportes, nunca una cifra inventada.
--  4. public.km_grupos(p_semanas): km por grupo y semana (suma de actividades de sus miembros
--     activos). Solo totales agregados, nunca actividades individuales.
--  5. eventos.event_results: resultados de eventos (puesto y tiempo) que carga el organizador.
--  6. public.resumen_seguridad_zona(): totales del ECU 911 del ultimo mes importado y reportes de
--     corredores de los ultimos 7 dias en el canton.

-- 1. Meta semanal en km -----------------------------------------------------------------------
ALTER TABLE identidad.user_settings ADD COLUMN IF NOT EXISTS weekly_goal_km NUMERIC NOT NULL DEFAULT 25;
DO $$ BEGIN
  ALTER TABLE identidad.user_settings ADD CONSTRAINT user_settings_weekly_goal_km_rango
    CHECK (weekly_goal_km BETWEEN 1 AND 500);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
CREATE OR REPLACE VIEW public.user_settings WITH (security_invoker = true) AS
  SELECT user_id, visibility, precise_location, hide_home, home_radius_m, prefs, updated_at,
         weekly_goal_runs, weekly_goal_km
  FROM identidad.user_settings;

-- Copia la meta en km que ya estaba en prefs (web) a la columna nueva.
update identidad.user_settings
   set weekly_goal_km = least(500, greatest(1, round((prefs->>'metaKmSemana')::numeric)))
 where prefs ? 'metaKmSemana'
   and (prefs->>'metaKmSemana') ~ '^[0-9]+(\.[0-9]+)?$'
   and weekly_goal_km is distinct from least(500, greatest(1, round((prefs->>'metaKmSemana')::numeric)));

-- 2. Eventos de ejemplo siempre proximos ------------------------------------------------------
CREATE OR REPLACE FUNCTION eventos.etiqueta_fecha_es(t TIMESTAMPTZ) RETURNS TEXT
LANGUAGE sql IMMUTABLE AS $$
  SELECT (ARRAY['Domingo','Lunes','Martes','Miércoles','Jueves','Viernes','Sábado'])
           [extract(dow FROM t AT TIME ZONE 'America/Guayaquil')::int + 1]
    || ' ' || extract(day FROM t AT TIME ZONE 'America/Guayaquil')::int || ' de '
    || (ARRAY['Enero','Febrero','Marzo','Abril','Mayo','Junio','Julio','Agosto','Septiembre','Octubre',
              'Noviembre','Diciembre'])[extract(month FROM t AT TIME ZONE 'America/Guayaquil')::int]
$$;

CREATE OR REPLACE FUNCTION eventos.reprogramar_eventos_demo() RETURNS INTEGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path = eventos, public AS $$
DECLARE n INTEGER;
BEGIN
  WITH movidos AS (
    UPDATE eventos.events e
    SET starts_at = e.starts_at + make_interval(weeks => ceil(extract(epoch FROM now() - e.starts_at) / (7 * 86400))::int),
        date_label = eventos.etiqueta_fecha_es(
          e.starts_at + make_interval(weeks => ceil(extract(epoch FROM now() - e.starts_at) / (7 * 86400))::int))
    WHERE e.starts_at < now() AND (e.id LIKE 'demo-ev-%' OR e.created_by IS NULL)
    RETURNING 1
  ) SELECT count(*) INTO n FROM movidos;
  RETURN n;
END $$;
REVOKE ALL ON FUNCTION eventos.reprogramar_eventos_demo() FROM PUBLIC, anon, authenticated;
SELECT eventos.reprogramar_eventos_demo();
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
    PERFORM cron.unschedule(jobid) FROM cron.job WHERE jobname = 'reprogramar-eventos-demo';
    PERFORM cron.schedule('reprogramar-eventos-demo', '15 5 * * *', 'SELECT eventos.reprogramar_eventos_demo()');
  END IF;
EXCEPTION WHEN others THEN NULL;
END $$;

-- 3. Seguridad por ruta con cache diaria ------------------------------------------------------
CREATE TABLE IF NOT EXISTS rutas.seguridad_rutas_cache (
  route_id TEXT NOT NULL REFERENCES rutas.routes(id) ON DELETE CASCADE,
  dia DATE NOT NULL,
  hora INTEGER NOT NULL,
  nivel TEXT NOT NULL,
  indice NUMERIC NOT NULL,
  reportes_7d INTEGER NOT NULL,
  reportes_30d INTEGER NOT NULL,
  puntaje INTEGER,
  PRIMARY KEY (route_id, dia, hora)
);
ALTER TABLE rutas.seguridad_rutas_cache ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON rutas.seguridad_rutas_cache FROM anon, authenticated;
GRANT ALL ON rutas.seguridad_rutas_cache TO service_role;

CREATE OR REPLACE FUNCTION public.seguridad_rutas()
RETURNS TABLE (route_id TEXT, nivel TEXT, indice NUMERIC, reportes_7d INTEGER, reportes_30d INTEGER, puntaje INTEGER)
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path = public, rutas, seguridad AS $$
DECLARE
  local TIMESTAMP := now() AT TIME ZONE 'America/Guayaquil';
  hoy DATE := local::date;
  -- Bloques de 3 horas: la cache cambia con la franja del dia, no con cada consulta.
  franja INTEGER := (extract(hour FROM local)::int / 3) * 3;
  dow INTEGER := extract(dow FROM local)::int;
  hay_ecu BOOLEAN := EXISTS (SELECT 1 FROM seguridad.incidentes_agregados LIMIT 1);
BEGIN
  INSERT INTO rutas.seguridad_rutas_cache (route_id, dia, hora, nivel, indice, reportes_7d, reportes_30d, puntaje)
  SELECT r.id, hoy, franja,
    COALESCE(s.nivel, 'medio'), COALESCE(s.indice, 1),
    (SELECT count(*) FROM seguridad.incidentes_usuario u
       WHERE u.estado IN ('verificado', 'pendiente') AND u.ocurrido_at > now() - interval '7 days'
         AND r.geom IS NOT NULL AND ST_DWithin(u.geom::geography, r.geom::geography, 200))::int,
    (SELECT count(*) FROM seguridad.incidentes_usuario u
       WHERE u.estado IN ('verificado', 'pendiente') AND u.ocurrido_at > now() - interval '30 days'
         AND r.geom IS NOT NULL AND ST_DWithin(u.geom::geography, r.geom::geography, 200))::int,
    NULL
  FROM rutas.routes r
  LEFT JOIN LATERAL (
    SELECT n.nivel, n.indice FROM public.nivel_seguridad_ruta(r.id, dow, franja + 1) n
    ORDER BY n.indice DESC NULLS LAST LIMIT 1
  ) s ON true
  WHERE r.is_public
    AND NOT EXISTS (SELECT 1 FROM rutas.seguridad_rutas_cache c WHERE c.route_id = r.id AND c.dia = hoy AND c.hora = franja)
  ON CONFLICT DO NOTHING;
  -- Puntaje solo con datos oficiales: 100 menos el exceso sobre el promedio del canton y los reportes.
  IF hay_ecu THEN
    UPDATE rutas.seguridad_rutas_cache c
    SET puntaje = greatest(0, least(100, round(100 - greatest(c.indice - 0.5, 0) * 40 - c.reportes_7d * 8 - c.reportes_30d * 2)))::int
    WHERE c.dia = hoy AND c.hora = franja AND c.puntaje IS NULL;
  END IF;
  DELETE FROM rutas.seguridad_rutas_cache WHERE dia < hoy - 2;
  RETURN QUERY SELECT c.route_id, c.nivel, c.indice, c.reportes_7d, c.reportes_30d, c.puntaje
    FROM rutas.seguridad_rutas_cache c WHERE c.dia = hoy AND c.hora = franja;
END $$;
REVOKE ALL ON FUNCTION public.seguridad_rutas() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.seguridad_rutas() TO authenticated;

-- 4. Km por grupo y semana --------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.km_grupos(p_semanas INTEGER DEFAULT 7)
RETURNS TABLE (club_id UUID, semana DATE, km NUMERIC, corredores INTEGER)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, eventos, actividad AS $$
  SELECT m.club_id,
         date_trunc('week', a.start_time AT TIME ZONE 'America/Guayaquil')::date AS semana,
         round(sum(a.distance_meters) / 1000.0, 1) AS km,
         count(DISTINCT a.user_id)::int AS corredores
  FROM eventos.club_members m
  JOIN eventos.clubs c ON c.id = m.club_id AND c.hidden_at IS NULL
  JOIN actividad.activities a ON a.user_id = m.user_id
  WHERE m.status = 'active'
    AND a.start_time >= date_trunc('week', now() AT TIME ZONE 'America/Guayaquil') - make_interval(weeks => greatest(1, least(p_semanas, 26)) - 1)
    AND a.distance_meters > 0
  GROUP BY 1, 2
$$;
REVOKE ALL ON FUNCTION public.km_grupos(INTEGER) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.km_grupos(INTEGER) TO authenticated;

-- 5. Resultados de eventos --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS eventos.event_results (
  event_id TEXT NOT NULL REFERENCES eventos.events(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  position INTEGER CHECK (position > 0),
  time_seconds INTEGER CHECK (time_seconds > 0),
  distance_km NUMERIC,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (event_id, user_id)
);
ALTER TABLE eventos.event_results ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Ver resultados de eventos publicados" ON eventos.event_results;
CREATE POLICY "Ver resultados de eventos publicados" ON eventos.event_results FOR SELECT
  USING (EXISTS (SELECT 1 FROM eventos.events e WHERE e.id = event_id AND (e.published OR e.created_by = auth.uid())));
DROP POLICY IF EXISTS "El organizador carga resultados" ON eventos.event_results;
CREATE POLICY "El organizador carga resultados" ON eventos.event_results FOR ALL
  USING (EXISTS (SELECT 1 FROM eventos.events e WHERE e.id = event_id AND e.created_by = auth.uid()))
  WITH CHECK (EXISTS (SELECT 1 FROM eventos.events e WHERE e.id = event_id AND e.created_by = auth.uid()));
GRANT SELECT, INSERT, UPDATE, DELETE ON eventos.event_results TO authenticated;
CREATE OR REPLACE VIEW public.event_results WITH (security_invoker = true) AS SELECT * FROM eventos.event_results;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.event_results TO authenticated;
DO $$ BEGIN
  IF to_regclass('kernel.propiedad_tablas') IS NOT NULL THEN
    EXECUTE $q$INSERT INTO kernel.propiedad_tablas (tabla, contexto, esquema_actual) VALUES
      ('event_results', 'eventos', 'eventos'), ('seguridad_rutas_cache', 'rutas', 'rutas') ON CONFLICT DO NOTHING$q$;
  END IF;
EXCEPTION WHEN others THEN NULL;
END $$;

-- 6. Seguridad en la zona ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.resumen_seguridad_zona()
RETURNS JSONB
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, seguridad AS $$
  WITH ultimo AS (SELECT max(make_date(anio, mes, 1)) AS mes FROM seguridad.incidentes_agregados)
  SELECT jsonb_build_object(
    'mes_ecu', (SELECT to_char(mes, 'YYYY-MM') FROM ultimo),
    'ecu', COALESCE((
      SELECT jsonb_agg(jsonb_build_object('servicio', servicio, 'conteo', total) ORDER BY total DESC)
      FROM (SELECT a.servicio, sum(a.conteo)::int AS total FROM seguridad.incidentes_agregados a, ultimo
            WHERE make_date(a.anio, a.mes, 1) = ultimo.mes GROUP BY a.servicio) t), '[]'::jsonb),
    'reportes_7d', (SELECT count(*) FROM seguridad.incidentes_usuario u
                    WHERE u.estado IN ('verificado', 'pendiente') AND u.ocurrido_at > now() - interval '7 days')
  )
$$;
REVOKE ALL ON FUNCTION public.resumen_seguridad_zona() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.resumen_seguridad_zona() TO authenticated;
