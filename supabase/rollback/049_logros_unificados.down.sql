-- Revierte 049_logros_unificados.sql.
-- Quita los 6 logros que antes eran solo medallas (no caben en el CHECK de 048), restaura las
-- reglas de 046 (el servidor vuelve a validar solo 4 codigos), el CHECK de 048 y borra el catalogo.
-- Los logros de los 20 codigos de 048 que se concedieron al aplicar 049 se conservan: eran
-- validos antes y siguen siendo validos. Para volver exactamente al estado previo, restaurar
-- user_achievements desde el respaldo tomado antes de aplicar (docs/LOGROS-UNIFICADOS.md).
BEGIN;

DELETE FROM logros.user_achievements
WHERE code IN ('km_42', 'total_100', 'total_500', 'total_1000', 'desnivel_1000', 'desnivel_5000');

ALTER TABLE logros.user_achievements DROP CONSTRAINT IF EXISTS user_achievements_code_fkey;
ALTER TABLE logros.user_achievements DROP CONSTRAINT IF EXISTS user_achievements_code_check;
ALTER TABLE logros.user_achievements ADD CONSTRAINT user_achievements_code_check CHECK (code IN (
  'primera_carrera', 'km_5', 'km_10', 'racha_7',
  'km_15', 'km_21', 'total_50', 'racha_3', 'racha_30', 'carreras_10', 'carreras_50', 'semanas_meta_4',
  'ritmo_6', 'ritmo_5', 'ritmo_430', 'escalador', 'explorador', 'madrugador', 'nocturno', 'fin_de_semana'
));

CREATE OR REPLACE FUNCTION logros.logro_merecido(uid uuid, codigo text)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, logros
AS $$
  SELECT CASE codigo
    WHEN 'primera_carrera' THEN EXISTS (
      SELECT 1 FROM activities a WHERE a.user_id = uid AND a.verificada)
    WHEN 'km_5' THEN EXISTS (
      SELECT 1 FROM activities a WHERE a.user_id = uid AND a.verificada AND a.distance_meters >= 5000)
    WHEN 'km_10' THEN EXISTS (
      SELECT 1 FROM activities a WHERE a.user_id = uid AND a.verificada AND a.distance_meters >= 10000)
    WHEN 'racha_7' THEN EXISTS (
      SELECT 1 FROM user_streaks s
      WHERE s.user_id = uid AND GREATEST(s.current_daily, s.best_daily) >= 7)
    ELSE false
  END;
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
  SELECT uid, c
  FROM (VALUES ('primera_carrera'), ('km_5'), ('km_10'), ('racha_7')) AS v(c)
  WHERE logros.logro_merecido(uid, c)
  ON CONFLICT DO NOTHING;
END;
$$;

DELETE FROM kernel.propiedad_tablas WHERE tabla = 'catalogo_logros';
DROP VIEW IF EXISTS public.catalogo_logros;
DROP TABLE IF EXISTS logros.catalogo_logros;

NOTIFY pgrst, 'reload schema';

COMMIT;
