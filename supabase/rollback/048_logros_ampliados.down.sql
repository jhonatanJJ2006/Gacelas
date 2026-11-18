-- Revierte 048_logros_ampliados.sql: borra los logros nuevos y restaura el CHECK de 020.
DELETE FROM logros.user_achievements WHERE code NOT IN ('primera_carrera', 'km_5', 'km_10', 'racha_7');
ALTER TABLE logros.user_achievements DROP CONSTRAINT IF EXISTS user_achievements_code_check;
ALTER TABLE logros.user_achievements ADD CONSTRAINT user_achievements_code_check
  CHECK (code IN ('primera_carrera', 'km_5', 'km_10', 'racha_7'));
