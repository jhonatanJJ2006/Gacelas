-- 047: fotos de demostracion para las rutas oficiales de Loja y para 4 carreras del feed
-- (pedido de Jhona, 17 nov 2026: ver tarjetas de rutas y publicaciones con imagenes).
--
-- Las fotos son URLs https publicas (picsum.photos, fotos libres de Unsplash) mientras no haya
-- fotos reales de cada ruta. route_photos.path solo aceptaba rutas de storage; ahora tambien
-- acepta https. La politica "Subir mis fotos" sigue exigiendo que path empiece por el uid,
-- asi que un usuario no puede insertar URLs externas.
--
-- Para quitarlo: supabase/rollback/047_fotos_demo.down.sql

ALTER TABLE rutas.route_photos DROP CONSTRAINT IF EXISTS route_photos_path_check;
ALTER TABLE rutas.route_photos ADD CONSTRAINT route_photos_path_check
  CHECK (path ~ '^[0-9a-f-]{36}/[^/]+/[^/]+$' OR path ~ '^https://');

DO $$
BEGIN
  -- Autor: el primer corredor de demostracion de 038 (no puede iniciar sesion).
  IF NOT EXISTS (SELECT 1 FROM auth.users WHERE id = 'de000000-0000-4000-8000-000000000001') THEN
    RETURN;
  END IF;

  -- 4 fotos por cada ruta oficial que todavia no tenga fotos.
  INSERT INTO rutas.route_photos (route_id, user_id, path, created_at)
  SELECT r.id, 'de000000-0000-4000-8000-000000000001'::uuid,
         'https://picsum.photos/seed/' || r.id || '-' || n || '/1200/800',
         now() - make_interval(mins => n)
  FROM rutas.routes r
  CROSS JOIN generate_series(1, 4) AS n
  WHERE r.is_official
    AND NOT EXISTS (SELECT 1 FROM rutas.route_photos p WHERE p.route_id = r.id);

  -- 4 a 6 fotos en 4 carreras publicas del feed (ya tienen trazado y metricas).
  INSERT INTO public.post_images (activity_id, user_id, path, position)
  SELECT a.id, a.user_id, 'https://picsum.photos/seed/' || a.id || '-' || n || '/1200/900', n
  FROM public.activities a
  JOIN (VALUES ('da000000-0000-4000-8000-000000000001'::uuid, 5),
               ('da000000-0000-4000-8000-000000000002'::uuid, 6),
               ('da000000-0000-4000-8000-000000000004'::uuid, 4),
               ('da000000-0000-4000-8000-000000000006'::uuid, 5)) AS t(id, total) ON t.id = a.id
  CROSS JOIN LATERAL generate_series(0, t.total - 1) AS n
  WHERE NOT EXISTS (SELECT 1 FROM public.post_images i WHERE i.activity_id = a.id);
END $$;
