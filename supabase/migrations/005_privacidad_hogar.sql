-- La zona de casa no debe ser visible para anonimos ni para otros usuarios.
-- La configuracion real (hide_home, home_radius_m) vive en user_settings, que solo ve su dueno
-- (RLS auth.uid() = user_id), y el recorte de public_points la lee de ahi.
-- Las columnas de profiles no se usaban y estaban vacias.
ALTER TABLE profiles DROP COLUMN IF EXISTS home_location;
ALTER TABLE profiles DROP COLUMN IF EXISTS home_radius_meters;
