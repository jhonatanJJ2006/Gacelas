-- 041: el mapa en vivo no guarda ni entrega la posicion exacta.
--
-- Antes, public.mapa_publicar escribia lat/lng tal cual y la politica
-- "Ver corredores visibles" dejaba leer esa fila a cualquier usuario autenticado
-- con el consentimiento ubicacion_visible vigente. Con el registro abierto, eso
-- permitia seguir a un corredor hasta la puerta de su casa.
--
-- Que cambia, sin tocar el contrato que ya usan Android, iOS (modulo Shared) y la web:
--   * mapa_publicar(lat, lng, corriendo) sigue devolviendo boolean. true = aceptado
--     (tambien si la zona de casa se omite). false = sin consentimiento, y se borra la fila.
--   * runner_positions conserva las mismas columnas. Los clientes siguen haciendo
--     select * y escuchando postgres_changes de public.runner_positions.
--   * Si hide_home esta activo (es el defecto, tambien cuando no hay ajustes), no se
--     publica nada dentro de home_radius_m del inicio de esta salida. El inicio es el
--     primer punto, o el primero tras 30 min sin publicar: la misma idea que live_enviar
--     (008), que usa el punto de salida como casa. El inicio exacto vive solo en
--     envivo.mapa_ancla, sin politicas ni permisos para la API.
--   * Fuera de esa zona la coordenada se redondea a 3 decimales (~110 m), igual que
--     los incidentes publicos. Si el punto ya redondeado cae dentro del radio, tampoco
--     se publica. Con hide_home desactivado se sigue redondeando: el mapa no sirve
--     para señalar una puerta.
--   * Sigue sin haber INSERT/UPDATE para anon ni authenticated. Solo esta funcion,
--     SECURITY DEFINER, escribe. DELETE de la fila propia (dejar de compartir) se mantiene.
--   * Quien puede ver el mapa no cambia: cualquier sesion iniciada ve a quien opto por
--     ubicacion visible en los ultimos 2 minutos. Lo que cambia es que esa fila ya no
--     trae la coordenada exacta ni la zona de casa.
--
-- Las filas ya guardadas se tratan al aplicar: el punto exacto pasa al ancla privada y,
-- con ocultar casa (defecto), desaparecen del mapa hasta la proxima publicacion fuera
-- del radio. No hace falta publicar una version nueva de las apps.
--
-- Reversion: supabase/rollback/041_mapa_vivo_privacidad.down.sql
--   (no recupera coordenadas que esta migracion haya borrado o redondeado).

BEGIN;

DO $$ BEGIN
  IF (SELECT c.relkind FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
      WHERE n.nspname = 'public' AND c.relname = 'runner_positions') IS DISTINCT FROM 'r' THEN
    RAISE EXCEPTION 'public.runner_positions tiene que seguir siendo tabla (realtime), no una vista';
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS envivo.mapa_ancla (
  user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  lat DOUBLE PRECISION NOT NULL CHECK (lat BETWEEN -90 AND 90),
  lng DOUBLE PRECISION NOT NULL CHECK (lng BETWEEN -180 AND 180),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE envivo.mapa_ancla ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON TABLE envivo.mapa_ancla FROM PUBLIC, anon, authenticated;
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'service_role') THEN
    REVOKE ALL ON TABLE envivo.mapa_ancla FROM service_role;
  END IF;
END $$;

-- Misma rejilla que incidentes_publicos: 3 decimales ~ 110 m. No es una RPC.
CREATE OR REPLACE FUNCTION envivo.difuminar_coordenada(p_lat DOUBLE PRECISION, p_lng DOUBLE PRECISION,
  OUT lat DOUBLE PRECISION, OUT lng DOUBLE PRECISION)
LANGUAGE sql IMMUTABLE AS $$
  SELECT round(p_lat::numeric, 3)::float8, round(p_lng::numeric, 3)::float8;
$$;
REVOKE ALL ON FUNCTION envivo.difuminar_coordenada(DOUBLE PRECISION, DOUBLE PRECISION) FROM PUBLIC, anon, authenticated;

-- mapa_ancla no se registra en kernel.propiedad_tablas ni se publica como vista en public.
-- La prueba de 033 exige una vista security_invoker por cada tabla registrada fuera de public,
-- y esa vista la expondria PostgREST. El inicio exacto de la salida no debe tener camino de lectura.

-- Audiencia igual que en 019: el mapa es de la ciudad, no solo de seguidores.
-- La privacidad esta en lo que se escribe, no en esconder el marcador a quien ya entro.
DROP POLICY IF EXISTS "Ver corredores visibles" ON public.runner_positions;
CREATE POLICY "Ver corredores visibles" ON public.runner_positions FOR SELECT
  USING (auth.uid() IS NOT NULL AND updated_at > now() - interval '2 minutes'
         AND consentimiento_vigente(user_id, 'ubicacion_visible'));
DROP POLICY IF EXISTS "Borrar mi posicion" ON public.runner_positions;
CREATE POLICY "Borrar mi posicion" ON public.runner_positions FOR DELETE USING (auth.uid() = user_id);

CREATE OR REPLACE FUNCTION public.mapa_publicar(lat DOUBLE PRECISION, lng DOUBLE PRECISION, corriendo BOOLEAN)
RETURNS BOOLEAN LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, envivo, identidad AS $$
DECLARE
  uid UUID := auth.uid();
  ocultar BOOLEAN;
  radio INTEGER;
  ancla_lat DOUBLE PRECISION;
  ancla_lng DOUBLE PRECISION;
  ancla_at TIMESTAMPTZ;
  lat_out DOUBLE PRECISION;
  lng_out DOUBLE PRECISION;
BEGIN
  IF uid IS NULL THEN RAISE EXCEPTION 'no autenticado'; END IF;
  IF lat IS NULL OR lng IS NULL OR lat NOT BETWEEN -90 AND 90 OR lng NOT BETWEEN -180 AND 180 THEN
    RAISE EXCEPTION 'coordenadas invalidas';
  END IF;
  IF NOT consentimiento_vigente(uid, 'ubicacion_visible') THEN
    DELETE FROM public.runner_positions WHERE user_id = uid;
    DELETE FROM envivo.mapa_ancla WHERE user_id = uid;
    RETURN false;
  END IF;

  SELECT s.hide_home, s.home_radius_m INTO ocultar, radio
  FROM identidad.user_settings s WHERE s.user_id = uid;
  ocultar := COALESCE(ocultar, true);
  radio := COALESCE(radio, 300);

  SELECT a.lat, a.lng, a.updated_at INTO ancla_lat, ancla_lng, ancla_at
  FROM envivo.mapa_ancla a WHERE a.user_id = uid;
  IF ancla_at IS NULL OR ancla_at < now() - interval '30 minutes' THEN
    INSERT INTO envivo.mapa_ancla (user_id, lat, lng, updated_at)
    VALUES (uid, lat, lng, now())
    ON CONFLICT (user_id) DO UPDATE SET lat = EXCLUDED.lat, lng = EXCLUDED.lng, updated_at = now();
    ancla_lat := lat;
    ancla_lng := lng;
  ELSE
    UPDATE envivo.mapa_ancla SET updated_at = now() WHERE user_id = uid;
  END IF;

  IF ocultar AND ST_Distance(
       ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography,
       ST_SetSRID(ST_MakePoint(ancla_lng, ancla_lat), 4326)::geography) <= radio THEN
    DELETE FROM public.runner_positions WHERE user_id = uid;
    RETURN true;
  END IF;

  SELECT d.lat, d.lng INTO lat_out, lng_out FROM envivo.difuminar_coordenada(lat, lng) AS d;
  IF ocultar AND ST_Distance(
       ST_SetSRID(ST_MakePoint(lng_out, lat_out), 4326)::geography,
       ST_SetSRID(ST_MakePoint(ancla_lng, ancla_lat), 4326)::geography) <= radio THEN
    DELETE FROM public.runner_positions WHERE user_id = uid;
    RETURN true;
  END IF;

  INSERT INTO public.runner_positions AS r (user_id, lat, lng, running, updated_at)
  VALUES (uid, lat_out, lng_out, COALESCE(corriendo, false), now())
  ON CONFLICT (user_id) DO UPDATE SET lat = EXCLUDED.lat, lng = EXCLUDED.lng,
    running = EXCLUDED.running, updated_at = now()
  WHERE r.updated_at < now() - interval '10 seconds' OR r.running IS DISTINCT FROM EXCLUDED.running;
  RETURN true;
END;
$$;

REVOKE ALL ON FUNCTION public.mapa_publicar(DOUBLE PRECISION, DOUBLE PRECISION, BOOLEAN) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.mapa_publicar(DOUBLE PRECISION, DOUBLE PRECISION, BOOLEAN) TO authenticated;
REVOKE INSERT, UPDATE, TRUNCATE ON TABLE public.runner_positions FROM PUBLIC, anon, authenticated;

CREATE OR REPLACE FUNCTION public.trg_revocar_ubicacion() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, envivo AS $$
BEGIN
  IF NEW.kind = 'ubicacion_visible' AND NOT NEW.granted THEN
    DELETE FROM public.runner_positions WHERE user_id = NEW.user_id;
    DELETE FROM envivo.mapa_ancla WHERE user_id = NEW.user_id;
  END IF;
  RETURN NULL;
END;
$$;

-- Filas que ya estaban en el mapa: el exacto queda solo en el ancla.
INSERT INTO envivo.mapa_ancla (user_id, lat, lng)
SELECT rp.user_id, rp.lat, rp.lng FROM public.runner_positions rp
ON CONFLICT (user_id) DO NOTHING;
DELETE FROM public.runner_positions rp
WHERE COALESCE((SELECT s.hide_home FROM identidad.user_settings s WHERE s.user_id = rp.user_id), true);
WITH calc AS (
  SELECT rp.user_id, x.lat, x.lng
  FROM public.runner_positions rp
  CROSS JOIN LATERAL envivo.difuminar_coordenada(rp.lat, rp.lng) AS x
)
UPDATE public.runner_positions AS rp
SET lat = calc.lat, lng = calc.lng
FROM calc
WHERE rp.user_id = calc.user_id;

COMMENT ON TABLE envivo.mapa_ancla IS
  'Inicio exacto de la salida del mapa en vivo. No lo lee la API; solo mapa_publicar.';
COMMENT ON TABLE public.runner_positions IS
  'Ultima posicion publica del mapa en vivo, ya redondeada a ~110 m y sin la zona de casa.';

NOTIFY pgrst, 'reload schema';

COMMIT;
