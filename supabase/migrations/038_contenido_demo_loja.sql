-- 038: contenido de demostracion en Loja para que Rutas, Clubes, Grupos, Eventos y el feed
-- tengan informacion visible en web y Android (pedido de Jhona, 12 nov 2026).
--
-- Que crea (todo idempotente, ON CONFLICT DO NOTHING):
--   - 8 corredores de demostracion en auth.users. No pueden iniciar sesion: sin contrasena,
--     banned_until en 2999 y raw_app_meta_data.demo = true. El trigger handle_new_user crea su
--     perfil y sus ajustes.
--   - 3 comunidades y 5 grupos en eventos.clubs, con punto de encuentro, dias y ritmo.
--   - Miembros (eventos.club_members) y publicaciones de grupo (public.club_posts).
--   - 6 eventos futuros (eventos.events) con fecha relativa al dia en que se aplica,
--     enlazados a grupos y a rutas oficiales de Loja (022_rutas_loja).
--   - Carreras publicas (public.activities) con el trazado real de esas rutas, para el feed.
--   - Resenas, me gusta y corridas de ruta (rutas.route_reviews, route_likes, route_runs).
--   - Seguimientos entre los corredores de demostracion (no toca cuentas reales).
--
-- Para quitarlo: supabase/rollback/038_contenido_demo_loja.sql (borra los usuarios de
-- demostracion; todo lo demas cae en cascada).

DO $$
DECLARE
  dias TEXT[] := ARRAY['Domingo','Lunes','Martes','Miércoles','Jueves','Viernes','Sábado'];
  meses TEXT[] := ARRAY['Enero','Febrero','Marzo','Abril','Mayo','Junio','Julio','Agosto',
                        'Septiembre','Octubre','Noviembre','Diciembre'];
  hoy DATE := (now() AT TIME ZONE 'America/Guayaquil')::date;
  u RECORD;
  e RECORD;
  a RECORD;
  inicio TIMESTAMPTZ;
  fecha DATE;
BEGIN
  -- ===== Corredores de demostracion =====
  FOR u IN SELECT * FROM (VALUES
    ('de000000-0000-4000-8000-000000000001'::uuid, 'maria.loja',    'María Fernanda Ojeda', 'Fondista. Domingos largos por La Tebaida y el Zamora.'),
    ('de000000-0000-4000-8000-000000000002'::uuid, 'carlos.trail',  'Carlos Jaramillo',     'Trail en Podocarpus y Villonaco. Siempre subiendo.'),
    ('de000000-0000-4000-8000-000000000003'::uuid, 'ana.utpl',      'Ana Lucía Torres',     'Estudiante UTPL. Series en la pista y 10K los sábados.'),
    ('de000000-0000-4000-8000-000000000004'::uuid, 'diego.jipiro',  'Diego Armijos',        'Entreno al amanecer en Jipiro. Ritmo tranquilo, constancia.'),
    ('de000000-0000-4000-8000-000000000005'::uuid, 'valeria.runs',  'Valeria Celi',         'Preparando la media maratón de Loja.'),
    ('de000000-0000-4000-8000-000000000006'::uuid, 'jorge.vilca',   'Jorge Ludeña',         'Vilcabamba y Malacatos. Corro donde hay montaña.'),
    ('de000000-0000-4000-8000-000000000007'::uuid, 'sofia.pace',    'Sofía Castillo',       'Coach de principiantes. Primer 5K sin caminar.'),
    ('de000000-0000-4000-8000-000000000008'::uuid, 'andres.night',  'Andrés Riofrío',       'Salidas nocturnas por el centro histórico.')
  ) AS t(id, usuario, nombre, bio) LOOP
    INSERT INTO auth.users (id, instance_id, aud, role, email, encrypted_password, banned_until,
                            raw_app_meta_data, raw_user_meta_data, created_at, updated_at)
    VALUES (u.id, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated',
            u.usuario || '@demo.runcity.app', '', '2999-01-01'::timestamptz,
            jsonb_build_object('provider', 'email', 'providers', jsonb_build_array('email'), 'demo', true),
            jsonb_build_object('full_name', u.nombre, 'username', replace(u.usuario, '.', '_')),
            now() - interval '40 days', now())
    ON CONFLICT (id) DO NOTHING;
    UPDATE identidad.profiles SET bio = u.bio, location = 'Loja, Ecuador' WHERE id = u.id AND COALESCE(bio, '') = '';
    UPDATE identidad.user_settings SET visibility = 'public' WHERE user_id = u.id;
  END LOOP;

  -- ===== Comunidades y grupos =====
  INSERT INTO eventos.clubs (id, name, description, location, requires_approval, owner_id, kind, community_id, city,
                             contact, meeting_place, meeting_lat, meeting_lng, training_days, training_time, level, pace_s_km, max_members)
  VALUES
    ('c1000000-0000-4000-8000-000000000001', 'Loja Runners', 'La comunidad abierta de corredores de Loja. Salidas grupales toda la semana, de 5K a media maratón.', 'Loja', false,
     'de000000-0000-4000-8000-000000000001', 'comunidad', NULL, 'Loja', 'Instagram @lojarunners', 'Parque Jipiro, entrada principal', -3.968705, -79.207122, '{2,4,6}', '06:00', 'todos', NULL, NULL),
    ('c1000000-0000-4000-8000-000000000002', 'Trail Podocarpus', 'Corredores de montaña del sur. Rutas por Cajanuma, Villonaco, Uritusinga y Vilcabamba.', 'Loja', false,
     'de000000-0000-4000-8000-000000000002', 'comunidad', NULL, 'Loja', 'WhatsApp del grupo en el perfil', 'Base del Cerro Villonaco', -3.994531, -79.240236, '{6,7}', '06:30', 'intermedio', NULL, NULL),
    ('c1000000-0000-4000-8000-000000000003', 'Corredores UTPL', 'Comunidad universitaria: series en la pista, fondos y carreras interfacultades.', 'Loja', false,
     'de000000-0000-4000-8000-000000000003', 'comunidad', NULL, 'Loja', 'deportes@utpl.edu.ec', 'Pista de atletismo UTPL', -3.985144, -79.19853, '{1,3,5}', '17:30', 'todos', NULL, NULL),
    ('c1000000-0000-4000-8000-000000000004', 'Madrugadores Jipiro', 'Rodaje suave antes del trabajo. Ritmo conversacional, nadie se queda atrás.', 'Loja', false,
     'de000000-0000-4000-8000-000000000004', 'grupo', 'c1000000-0000-4000-8000-000000000001', 'Loja', '', 'Parque Jipiro, junto al lago', -3.968705, -79.207122, '{1,3,5}', '05:45', 'principiante', 420, 40),
    ('c1000000-0000-4000-8000-000000000005', 'Fondo dominical La Tebaida', 'Fondos de 12 a 21 km por el Parque Lineal La Tebaida y las orillas del Zamora.', 'Loja', false,
     'de000000-0000-4000-8000-000000000005', 'grupo', 'c1000000-0000-4000-8000-000000000001', 'Loja', '', 'Parque Lineal La Tebaida, puente principal', -4.012278, -79.204655, '{7}', '06:30', 'intermedio', 345, 60),
    ('c1000000-0000-4000-8000-000000000006', 'Cumbres Villonaco', 'Subidas y bajadas técnicas al Villonaco. Trae agua y cortavientos.', 'Loja', true,
     'de000000-0000-4000-8000-000000000002', 'grupo', 'c1000000-0000-4000-8000-000000000002', 'Loja', '', 'Base del Cerro Villonaco', -3.994531, -79.240236, '{6}', '06:00', 'avanzado', 390, 25),
    ('c1000000-0000-4000-8000-000000000007', 'Series en la pista', 'Intervalos de 400 y 1000 m con calentamiento guiado. Martes y jueves.', 'Loja', false,
     'de000000-0000-4000-8000-000000000003', 'grupo', 'c1000000-0000-4000-8000-000000000003', 'Loja', '', 'Pista de atletismo UTPL', -3.985144, -79.19853, '{2,4}', '17:30', 'intermedio', 300, 30),
    ('c1000000-0000-4000-8000-000000000008', 'Mi primer 5K', 'Plan de 8 semanas para pasar de caminar a correr 5 km. Grupo de principiantes.', 'Loja', false,
     'de000000-0000-4000-8000-000000000007', 'grupo', 'c1000000-0000-4000-8000-000000000001', 'Loja', '', 'Parque Daniel Álvarez', -4.018021, -79.210489, '{2,4,6}', '07:00', 'principiante', 480, 50)
  ON CONFLICT (id) DO NOTHING;

  INSERT INTO eventos.club_members (club_id, user_id)
  SELECT c.id::uuid, m.id::uuid FROM (VALUES
    ('c1000000-0000-4000-8000-000000000001', ARRAY[2,3,4,5,6,7,8]),
    ('c1000000-0000-4000-8000-000000000002', ARRAY[1,5,6]),
    ('c1000000-0000-4000-8000-000000000003', ARRAY[5,7,8]),
    ('c1000000-0000-4000-8000-000000000004', ARRAY[1,7,8]),
    ('c1000000-0000-4000-8000-000000000005', ARRAY[1,2,4,6]),
    ('c1000000-0000-4000-8000-000000000006', ARRAY[6]),
    ('c1000000-0000-4000-8000-000000000007', ARRAY[5,8]),
    ('c1000000-0000-4000-8000-000000000008', ARRAY[4,8,1])
  ) AS c(id, nums)
  CROSS JOIN LATERAL unnest(c.nums) AS n
  CROSS JOIN LATERAL (SELECT ('de000000-0000-4000-8000-00000000000' || n)::text AS id) m
  ON CONFLICT DO NOTHING;

  INSERT INTO public.club_posts (id, club_id, user_id, body, created_at) VALUES
    ('c2000000-0000-4000-8000-000000000001', 'c1000000-0000-4000-8000-000000000001', 'de000000-0000-4000-8000-000000000001', 'Este sábado salimos 6:00 desde Jipiro. 8 km a ritmo suave, luego café en la entrada.', now() - interval '20 hours'),
    ('c2000000-0000-4000-8000-000000000002', 'c1000000-0000-4000-8000-000000000001', 'de000000-0000-4000-8000-000000000004', 'Gracias a todos por el rodaje de hoy. Fuimos 23. La próxima semana probamos la ruta del Zamora.', now() - interval '3 days'),
    ('c2000000-0000-4000-8000-000000000003', 'c1000000-0000-4000-8000-000000000002', 'de000000-0000-4000-8000-000000000002', 'Cajanuma está con barro después de las lluvias. Zapatilla de trail obligatoria el domingo.', now() - interval '1 day'),
    ('c2000000-0000-4000-8000-000000000004', 'c1000000-0000-4000-8000-000000000002', 'de000000-0000-4000-8000-000000000006', 'Subí el Mandango ayer: 3,4 km y casi 500 m de desnivel. Vistas increíbles de Vilcabamba.', now() - interval '2 days'),
    ('c2000000-0000-4000-8000-000000000005', 'c1000000-0000-4000-8000-000000000003', 'de000000-0000-4000-8000-000000000003', 'Martes y jueves series en la pista a las 17:30. Traigan reloj o la app con el cronómetro de intervalos.', now() - interval '5 hours'),
    ('c2000000-0000-4000-8000-000000000006', 'c1000000-0000-4000-8000-000000000005', 'de000000-0000-4000-8000-000000000005', 'Domingo fondo de 16 km. Punto de hidratación en el km 8, junto al puente.', now() - interval '30 hours'),
    ('c2000000-0000-4000-8000-000000000007', 'c1000000-0000-4000-8000-000000000008', 'de000000-0000-4000-8000-000000000007', 'Semana 3 del plan: 6 bloques de 3 minutos corriendo y 1 caminando. ¡Vamos que se puede!', now() - interval '10 hours'),
    ('c2000000-0000-4000-8000-000000000008', 'c1000000-0000-4000-8000-000000000004', 'de000000-0000-4000-8000-000000000008', 'Mañana llueve según el pronóstico, igual salimos. Lleven rompevientos.', now() - interval '14 hours'),
    ('c2000000-0000-4000-8000-000000000009', 'c1000000-0000-4000-8000-000000000006', 'de000000-0000-4000-8000-000000000002', 'Solicitudes abiertas para la subida del sábado. Nivel avanzado: 10 km con 800 m de desnivel.', now() - interval '4 days'),
    ('c2000000-0000-4000-8000-000000000010', 'c1000000-0000-4000-8000-000000000007', 'de000000-0000-4000-8000-000000000003', 'Hoy: 8 x 400 m con 200 m de trote. Calentamiento 15 minutos.', now() - interval '2 hours')
  ON CONFLICT (id) DO NOTHING;

  -- ===== Eventos futuros (relativos a la fecha de aplicacion) =====
  FOR e IN SELECT * FROM (VALUES
    ('demo-ev-1', 'Rodaje grupal 8K Jipiro', 'Salida grupal a ritmo conversacional por el Parque Jipiro y la avenida Orillas del Zamora.', 6, 0, '06:00'::time, 'Parque Jipiro, Loja', 8.0, 18, 'c1000000-0000-4000-8000-000000000004', 'loja-jipiro', 60, 'grupal', -3.968705, -79.207122),
    ('demo-ev-2', 'Fondo dominical 16K La Tebaida', 'Fondo largo junto al río. Grupos por ritmo de 5:30 a 7:00 min/km.', 0, 0, '06:30'::time, 'Parque Lineal La Tebaida', 16.0, 25, 'c1000000-0000-4000-8000-000000000005', 'loja-la-tebaida', 80, 'entrenamiento', -4.012278, -79.204655),
    ('demo-ev-3', 'Subida al Villonaco', 'Trail de montaña con 800 m de desnivel. Nivel avanzado, cupo limitado.', 6, 1, '06:00'::time, 'Base Cerro Villonaco', 10.2, 9, 'c1000000-0000-4000-8000-000000000006', 'loja-villonaco', 25, 'entrenamiento', -3.994531, -79.240236),
    ('demo-ev-4', 'Series 8 x 400 en la pista', 'Intervalos en la pista de la UTPL con calentamiento y estiramiento guiados.', 2, 0, '17:30'::time, 'Pista de atletismo UTPL', 6.0, 14, 'c1000000-0000-4000-8000-000000000007', 'loja-pista-utpl', 40, 'entrenamiento', -3.985144, -79.19853),
    ('demo-ev-5', 'Carrera 10K Centro Histórico', 'Recorrido urbano por el centro histórico y la Puerta de la Ciudad. Abierta a todos.', 0, 1, '07:00'::time, 'Puerta de la Ciudad, Loja', 10.0, 64, 'c1000000-0000-4000-8000-000000000001', 'loja-centro-historico', 300, 'carrera', -3.989800, -79.204200),
    ('demo-ev-6', 'Trail Cajanuma Los Miradores', 'Sendero de bosque nublado en el Parque Nacional Podocarpus. Llevar impermeable.', 6, 2, '07:00'::time, 'Entrada Cajanuma, Podocarpus', 4.8, 12, 'c1000000-0000-4000-8000-000000000002', 'loja-cajanuma-miradores', 30, 'grupal', -4.115281, -79.164674)
  ) AS t(id, titulo, descripcion, dia_semana, semana, hora, lugar, km, base, club, ruta, cupo, tipo, lat, lng) LOOP
    -- Proximo dia de la semana pedido (0 = domingo), nunca hoy, mas las semanas indicadas.
    fecha := hoy + (((e.dia_semana - extract(dow FROM hoy)::int + 6) % 7) + 1) + 7 * e.semana;
    inicio := (fecha + e.hora) AT TIME ZONE 'America/Guayaquil';
    INSERT INTO eventos.events (id, title, description, date_label, time_label, starts_at, location, distance_km,
                                base_participants, created_by, club_id, route_id, max_participants, kind, meeting_lat, meeting_lng, published)
    VALUES (e.id, e.titulo, e.descripcion,
            dias[extract(dow FROM fecha)::int + 1] || ' ' || extract(day FROM fecha)::int || ' de ' || meses[extract(month FROM fecha)::int],
            to_char(e.hora, 'FMHH12:MI') || CASE WHEN e.hora < '12:00' THEN ' AM' ELSE ' PM' END,
            inicio, e.lugar, e.km, e.base,
            (SELECT owner_id FROM eventos.clubs WHERE id = e.club::uuid), e.club::uuid,
            CASE WHEN EXISTS (SELECT 1 FROM rutas.routes WHERE id = e.ruta) THEN e.ruta END,
            e.cupo, e.tipo, e.lat, e.lng, true)
    ON CONFLICT (id) DO NOTHING;
  END LOOP;

  INSERT INTO eventos.event_rsvps (event_id, user_id)
  SELECT ev, ('de000000-0000-4000-8000-00000000000' || n)::uuid
  FROM (VALUES ('demo-ev-1', ARRAY[1,4,7,8]), ('demo-ev-2', ARRAY[1,2,5,6]), ('demo-ev-3', ARRAY[2,6]),
               ('demo-ev-4', ARRAY[3,5,8]), ('demo-ev-5', ARRAY[1,2,3,4,5,6,7,8]), ('demo-ev-6', ARRAY[2,5,6])) AS t(ev, nums)
  CROSS JOIN LATERAL unnest(t.nums) AS n
  WHERE EXISTS (SELECT 1 FROM eventos.events WHERE id = t.ev)
  ON CONFLICT DO NOTHING;

  -- ===== Carreras publicas con trazado real (feed) =====
  FOR a IN SELECT * FROM (VALUES
    ('da000000-0000-4000-8000-000000000001'::uuid, 1, 'loja-la-tebaida',        'Fondo suave por La Tebaida',     1, '06:40'::time, 2, 368),
    ('da000000-0000-4000-8000-000000000002'::uuid, 2, 'loja-villonaco',         'Villonaco hasta la cima',        2, '06:10'::time, 1, 455),
    ('da000000-0000-4000-8000-000000000003'::uuid, 3, 'loja-pista-utpl',        'Series 10 x 400',                0, '17:45'::time, 25, 262),
    ('da000000-0000-4000-8000-000000000004'::uuid, 4, 'loja-jipiro',            'Amanecer en Jipiro',             1, '05:55'::time, 4, 402),
    ('da000000-0000-4000-8000-000000000005'::uuid, 5, 'loja-orillas-zamora',    'Tempo por el Zamora',            3, '06:20'::time, 3, 318),
    ('da000000-0000-4000-8000-000000000006'::uuid, 6, 'loja-mandango',          'Cerro Mandango con niebla',      2, '07:05'::time, 1, 520),
    ('da000000-0000-4000-8000-000000000007'::uuid, 7, 'loja-daniel-alvarez',    'Semana 3 del plan 5K',           0, '07:10'::time, 4, 465),
    ('da000000-0000-4000-8000-000000000008'::uuid, 8, 'loja-centro-historico',  'Nocturna por el centro',         1, '19:30'::time, 4, 350),
    ('da000000-0000-4000-8000-000000000009'::uuid, 1, 'loja-orillas-malacatos', 'Regenerativo junto al Malacatos', 4, '06:30'::time, 2, 390),
    ('da000000-0000-4000-8000-000000000010'::uuid, 2, 'loja-cajanuma-miradores','Cajanuma: Los Miradores',        5, '07:00'::time, 1, 540),
    ('da000000-0000-4000-8000-000000000011'::uuid, 5, 'loja-la-banda',          'Sendero La Banda',               6, '06:15'::time, 2, 377),
    ('da000000-0000-4000-8000-000000000012'::uuid, 6, 'loja-uritusinga',        'Uritusinga, ida y vuelta',       7, '06:00'::time, 1, 498),
    ('da000000-0000-4000-8000-000000000013'::uuid, 3, 'loja-altavista',         'Cuestas en Altavista',           3, '17:20'::time, 5, 333),
    ('da000000-0000-4000-8000-000000000014'::uuid, 4, 'loja-pucara',            'Rodaje Pucará Podocarpus',       5, '06:00'::time, 5, 410),
    ('da000000-0000-4000-8000-000000000015'::uuid, 7, 'loja-jipiro',            'Primeros 3 km sin parar',        6, '07:00'::time, 2, 470),
    ('da000000-0000-4000-8000-000000000016'::uuid, 8, 'loja-las-penas',         'Mirador Las Peñas de noche',     8, '20:00'::time, 4, 345)
  ) AS t(id, autor, ruta, titulo, hace_dias, hora, vueltas, ritmo) LOOP
    CONTINUE WHEN NOT EXISTS (SELECT 1 FROM rutas.routes WHERE id = a.ruta);
    inicio := ((hoy - a.hace_dias) + a.hora) AT TIME ZONE 'America/Guayaquil';
    INSERT INTO public.activities (id, user_id, title, activity_type, start_time, end_time, distance_meters, duration_seconds,
                                   avg_pace_seconds_per_km, elevation_gain_meters, is_public, client_id, public_points, splits)
    SELECT a.id, ('de000000-0000-4000-8000-00000000000' || a.autor)::uuid, a.titulo, 'run', inicio,
           inicio + make_interval(secs => round(r.distance_km * a.vueltas * a.ritmo)::int),
           round(r.distance_km * a.vueltas * 1000)::int, round(r.distance_km * a.vueltas * a.ritmo)::int, a.ritmo,
           COALESCE(r.elevation_m, 0) * a.vueltas, true, 'demo-' || a.id::text, r.path,
           (SELECT COALESCE(jsonb_agg(jsonb_build_object('numero', k, 'segundos', a.ritmo + ((k * 7) % 21) - 10, 'distanciaKm', 1, 'manual', false)), '[]'::jsonb)
              FROM generate_series(1, floor(r.distance_km * a.vueltas)::int) AS k)
    FROM rutas.routes r WHERE r.id = a.ruta
    ON CONFLICT (id) DO NOTHING;
  END LOOP;

  INSERT INTO public.post_likes (user_id, post_id)
  SELECT ('de000000-0000-4000-8000-00000000000' || l)::uuid, p::text
  FROM (VALUES ('da000000-0000-4000-8000-000000000001'::uuid, ARRAY[2,4,5,7]), ('da000000-0000-4000-8000-000000000002'::uuid, ARRAY[1,6]),
               ('da000000-0000-4000-8000-000000000003'::uuid, ARRAY[5,8]), ('da000000-0000-4000-8000-000000000004'::uuid, ARRAY[1,7,8]),
               ('da000000-0000-4000-8000-000000000006'::uuid, ARRAY[2,5]), ('da000000-0000-4000-8000-000000000007'::uuid, ARRAY[1,3,4,5,8])) AS t(p, ls)
  CROSS JOIN LATERAL unnest(t.ls) AS l
  WHERE EXISTS (SELECT 1 FROM public.activities WHERE id = t.p)
  ON CONFLICT DO NOTHING;

  INSERT INTO public.post_comments (user_id, post_id, body, created_at)
  SELECT ('de000000-0000-4000-8000-00000000000' || c.autor)::uuid, c.post::text, c.texto, now() - make_interval(hours => c.horas)
  FROM (VALUES (5, 'da000000-0000-4000-8000-000000000001'::uuid, 'Qué buen ritmo para un fondo. ¿El domingo repetimos?', 20),
               (1, 'da000000-0000-4000-8000-000000000002'::uuid, 'Esa subida es brutal. Felicidades.', 30),
               (8, 'da000000-0000-4000-8000-000000000007'::uuid, '¡Vas muy bien con el plan!', 9),
               (2, 'da000000-0000-4000-8000-000000000006'::uuid, 'El Mandango con niebla es otra cosa.', 40)) AS c(autor, post, texto, horas)
  WHERE EXISTS (SELECT 1 FROM public.activities WHERE id = c.post)
    AND NOT EXISTS (SELECT 1 FROM public.post_comments pc WHERE pc.post_id = c.post::text AND pc.body = c.texto);

  UPDATE public.activities x SET
    likes_count = (SELECT count(*) FROM public.post_likes l WHERE l.post_id = x.id::text),
    comments_count = (SELECT count(*) FROM public.post_comments c WHERE c.post_id = x.id::text)
  WHERE x.id::text LIKE 'da000000-%';

  -- ===== Metricas de rutas: resenas, me gusta y corridas =====
  INSERT INTO rutas.route_reviews (route_id, user_id, rating, comment, created_at)
  SELECT r.ruta, ('de000000-0000-4000-8000-00000000000' || r.autor)::uuid, r.nota, r.texto, now() - make_interval(days => r.dias)
  FROM (VALUES ('loja-jipiro', 4, 5, 'Plana y segura al amanecer. Ideal para rodajes suaves.', 3),
               ('loja-jipiro', 7, 4, 'Buena para empezar. Hay mucha gente los fines de semana.', 6),
               ('loja-la-tebaida', 1, 5, 'La mejor para fondos largos sin tráfico.', 2),
               ('loja-la-tebaida', 5, 4, 'Algunos tramos de tierra después de la lluvia.', 8),
               ('loja-villonaco', 2, 5, 'Subida exigente, vistas espectaculares. Llevar cortavientos.', 1),
               ('loja-villonaco', 6, 4, 'Mucho viento arriba. Vale la pena.', 9),
               ('loja-centro-historico', 8, 4, 'Bonita de noche, ojo con los cruces.', 4),
               ('loja-orillas-zamora', 5, 5, 'Perfecta para tempos. Bien iluminada.', 3),
               ('loja-mandango', 6, 5, 'Técnica y empinada. Vilcabamba desde arriba no tiene precio.', 2),
               ('loja-pista-utpl', 3, 5, 'Tartán en buen estado. Abierta en la tarde.', 1),
               ('loja-cajanuma-miradores', 2, 4, 'Mucho barro en temporada de lluvia.', 5),
               ('loja-daniel-alvarez', 7, 5, 'Sombra y circuito corto, perfecto para principiantes.', 4)) AS r(ruta, autor, nota, texto, dias)
  WHERE EXISTS (SELECT 1 FROM rutas.routes WHERE id = r.ruta)
    AND NOT EXISTS (SELECT 1 FROM rutas.route_reviews x WHERE x.route_id = r.ruta AND x.user_id = ('de000000-0000-4000-8000-00000000000' || r.autor)::uuid);

  INSERT INTO rutas.route_likes (route_id, user_id)
  SELECT ruta, ('de000000-0000-4000-8000-00000000000' || n)::uuid
  FROM (VALUES ('loja-jipiro', ARRAY[1,3,4,7,8]), ('loja-la-tebaida', ARRAY[1,2,5]), ('loja-villonaco', ARRAY[2,6,5]),
               ('loja-orillas-zamora', ARRAY[5,4]), ('loja-mandango', ARRAY[6,2]), ('loja-centro-historico', ARRAY[8,3])) AS t(ruta, nums)
  CROSS JOIN LATERAL unnest(t.nums) AS n
  WHERE EXISTS (SELECT 1 FROM rutas.routes WHERE id = t.ruta)
  ON CONFLICT DO NOTHING;

  INSERT INTO rutas.route_runs (route_id, user_id, activity_id, created_at)
  SELECT x.client_id_ruta, x.user_id, x.id::text, x.start_time
  FROM (SELECT a2.id, a2.user_id, a2.start_time, r.id AS client_id_ruta
          FROM public.activities a2 JOIN rutas.routes r ON r.path = a2.public_points
         WHERE a2.id::text LIKE 'da000000-%') x
  ON CONFLICT DO NOTHING;

  -- ===== Seguimientos entre corredores de demostracion =====
  INSERT INTO public.follows (follower_id, following_id)
  SELECT ('de000000-0000-4000-8000-00000000000' || f)::uuid, ('de000000-0000-4000-8000-00000000000' || s)::uuid
  FROM generate_series(1, 8) f CROSS JOIN generate_series(1, 8) s
  WHERE f <> s AND (f + s) % 3 <> 0
  ON CONFLICT DO NOTHING;
END $$;
