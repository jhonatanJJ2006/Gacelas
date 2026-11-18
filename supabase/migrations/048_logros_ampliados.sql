-- 048: amplia los codigos de logros (pantalla de logros de la app movil, 17 nov 2026).
-- La app sube a user_achievements cada logro nuevo; el CHECK de 020 solo aceptaba 4 codigos
-- y rechazaba el lote entero.
-- Para quitarlo: supabase/rollback/048_logros_ampliados.down.sql

ALTER TABLE public.user_achievements DROP CONSTRAINT IF EXISTS user_achievements_code_check;
ALTER TABLE public.user_achievements ADD CONSTRAINT user_achievements_code_check CHECK (code IN (
  'primera_carrera', 'km_5', 'km_10', 'racha_7',
  'km_15', 'km_21', 'total_50', 'racha_3', 'racha_30', 'carreras_10', 'carreras_50', 'semanas_meta_4',
  'ritmo_6', 'ritmo_5', 'ritmo_430', 'escalador', 'explorador', 'madrugador', 'nocturno', 'fin_de_semana'
));
