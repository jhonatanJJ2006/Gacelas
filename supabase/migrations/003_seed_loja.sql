-- 003: rutas y eventos de ejemplo en Loja (mismos id que los datos de ejemplo de las apps)
INSERT INTO routes (id, name, distance_km, difficulty, elevation_m, description, path) VALUES
 ('1','Parque Jipiro',5.2,'Fácil',45,'Ruta panorámica alrededor del Parque Jipiro, ideal para principiantes.',
  '[{"lat":-3.9746,"lng":-79.2017},{"lat":-3.9712,"lng":-79.2001},{"lat":-3.9690,"lng":-79.2030},{"lat":-3.9718,"lng":-79.2058},{"lat":-3.9746,"lng":-79.2017}]'),
 ('2','Río Zamora',8.5,'Moderado',120,'Ruta junto al río con algunas subidas moderadas.',
  '[{"lat":-3.9931,"lng":-79.2005},{"lat":-3.9990,"lng":-79.1985},{"lat":-4.0060,"lng":-79.1970},{"lat":-4.0125,"lng":-79.1950}]'),
 ('3','Cerro Villonaco',12.3,'Difícil',380,'Ruta de montaña desafiante con vistas espectaculares de Loja.',
  '[{"lat":-3.9950,"lng":-79.2250},{"lat":-3.9990,"lng":-79.2400},{"lat":-4.0010,"lng":-79.2550},{"lat":-4.0002,"lng":-79.2640}]')
ON CONFLICT (id) DO NOTHING;

INSERT INTO events (id, title, description, date_label, time_label, starts_at, location, distance_km, base_participants) VALUES
 ('1','Carrera Parque Jipiro','Carrera grupal de 10km por el Parque Jipiro. Todos los niveles bienvenidos.','Sábado 11 de Octubre','6:00 AM','2026-10-11 06:00-05','Parque Jipiro, Loja',10,15),
 ('2','Trail Running Podocarpus','Aventura de trail running en el Parque Nacional Podocarpus.','Domingo 19 de Octubre','7:00 AM','2026-10-19 07:00-05','Entrada Cajanuma',15,8),
 ('3','Entrenamiento Intervalos Villonaco','Series en la base del Cerro Villonaco.','Miércoles 15 de Octubre','6:30 AM','2026-10-15 06:30-05','Base Cerro Villonaco',8,12),
 ('4','Fondo dominical Zamora','Fondo suave de 12 km junto al río Zamora.','Domingo 28 de Septiembre','6:30 AM','2026-09-28 06:30-05','Parque Lineal La Tebaida',12,21)
ON CONFLICT (id) DO NOTHING;
