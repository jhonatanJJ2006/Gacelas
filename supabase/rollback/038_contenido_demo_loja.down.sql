-- Quita el contenido de demostracion de 038_contenido_demo_loja.sql.
-- Borrar los usuarios de demostracion elimina en cascada perfiles, ajustes, grupos que son suyos
-- (y sus miembros y publicaciones), carreras, me gusta, comentarios, resenas y seguimientos.
DELETE FROM eventos.events WHERE id LIKE 'demo-ev-%';
DELETE FROM auth.users WHERE raw_app_meta_data->>'demo' = 'true' AND email LIKE '%@demo.runcity.app';
