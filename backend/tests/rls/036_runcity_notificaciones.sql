-- Prueba de 036: las notificaciones del sistema salen con el remitente Runcity. Todo se revierte al final.
BEGIN;
INSERT INTO auth.users (id, email) VALUES ('00000000-0000-0000-0000-0000000036a1', 'a36@prueba.ec')
ON CONFLICT DO NOTHING;
DO $$
DECLARE a TEXT; m TEXT;
BEGIN
  SELECT actor_name, message INTO a, m FROM avisos.notifications
    WHERE user_id = '00000000-0000-0000-0000-0000000036a1' ORDER BY created_at LIMIT 1;
  IF a IS DISTINCT FROM 'Runcity Loja' THEN RAISE EXCEPTION 'Remitente inesperado: %', a; END IF;
  IF m NOT LIKE 'Bienvenido a Runcity%' THEN RAISE EXCEPTION 'Mensaje inesperado: %', m; END IF;
  IF EXISTS (SELECT 1 FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
             WHERE n.nspname = 'public' AND p.prosrc LIKE '%Gacelas%') THEN
    RAISE EXCEPTION 'Quedan funciones con Gacelas';
  END IF;
END $$;
ROLLBACK;
