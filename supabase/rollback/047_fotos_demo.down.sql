-- Revierte 047_fotos_demo.sql: borra las fotos de demostracion y restaura el CHECK original.
DELETE FROM rutas.route_photos WHERE path LIKE 'https://picsum.photos/%';
DELETE FROM comunidad.post_images WHERE path LIKE 'https://picsum.photos/%';
ALTER TABLE rutas.route_photos DROP CONSTRAINT IF EXISTS route_photos_path_check;
ALTER TABLE rutas.route_photos ADD CONSTRAINT route_photos_path_check
  CHECK (path ~ '^[0-9a-f-]{36}/[^/]+/[^/]+$');
