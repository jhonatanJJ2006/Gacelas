# Rutas oficiales de Loja

Gacelas incluye 26 rutas preestablecidas de Loja y sus alrededores: parques, senderos, montañas, recorridos urbanos y una pista. Se marcan como oficiales (`routes.is_official = true`) y se muestran con la insignia "Ruta oficial" en Android y en la web.

## Origen de los datos

- Trazados: OpenStreetMap, licencia ODbL 1.0. Atribución obligatoria: "© colaboradores de OpenStreetMap". Cada ruta guarda la fuente y la licencia en las columnas `source` y `license`.
- Cuando no hay una vía OSM continua se calculó el trazado con OSRM (perfil a pie, routing.openstreetmap.de) sobre los mismos datos OSM.
- Desnivel y perfil de elevación: modelo SRTM de 30 m consultado con OpenTopoData.
- Los scripts que generan los datos están en `scripts/rutas-loja/` (`obtener_trazados.py`, `rutas_osrm.py`, `perfil_elevacion.py` y `generar.py`). El resultado queda en `supabase/seed/rutas-loja.geojson` y en la migración 022.

## Migración 022

`supabase/migrations/022_rutas_loja.sql` añade a `routes` las columnas `slug`, `is_official`, `category`, `surface`, `source`, `license`, `elevation_loss_m` y `geom` (PostGIS `GEOGRAPHY(LineString)`), y carga las 26 rutas con trazado, punto de inicio, distancia, desnivel, dificultad, descripción y perfil de elevación.

- Es idempotente: usa identificadores fijos y `ON CONFLICT DO UPDATE`, así que se puede ejecutar varias veces.
- La restricción `routes_official_check` solo permite rutas oficiales sin autor, así que un usuario no puede marcar como oficial una ruta propia.
- Un trigger calcula `geom` a partir del trazo (`path`).
- No se aplica sola: hay que ejecutarla en Supabase con el resto de migraciones.

Las apps también traen las rutas en el código (`RutasLoja.kt` y `apps/web/src/contexts/rutas/infrastructure/rutasLojaOsm.ts`) y las mezclan con las del servidor, así que se ven aunque la migración aún no esté aplicada. Si el servidor devuelve la misma ruta, prevalece la del servidor.

## Lista de rutas

| Ruta                                | Categoría | Distancia | Desnivel + | Dificultad | Superficie | Trazado                                                                                            |
| ----------------------------------- | --------- | --------- | ---------- | ---------- | ---------- | -------------------------------------------------------------------------------------------------- |
| Parque Lineal La Tebaida            | Parque    | 2.98 km   | 44 m       | Fácil      | mixta      | OSRM a pie sobre OSM                                                                               |
| Ruta de la Cascarilla               | Sendero   | 2.09 km   | 38 m       | Fácil      | tierra     | OSRM a pie sobre OSM                                                                               |
| Parque Pucará Podocarpus            | Parque    | 0.91 km   | 56 m       | Fácil      | mixta      | OSRM a pie sobre OSM                                                                               |
| Mirador El Churo                    | Montaña   | 1.30 km   | 48 m       | Fácil      | mixta      | OSRM a pie sobre OSM                                                                               |
| Cerro Villonaco                     | Montaña   | 10.19 km  | 628 m      | Difícil    | tierra     | OSRM a pie sobre OSM                                                                               |
| Zamora Huayco                       | Sendero   | 1.05 km   | 38 m       | Fácil      | mixta      | OSRM a pie sobre OSM                                                                               |
| Vilcabamba: Rumi Wilco              | Sendero   | 1.42 km   | 53 m       | Fácil      | tierra     | OSRM a pie sobre OSM                                                                               |
| Vilcabamba: Mirador San José        | Montaña   | 2.27 km   | 140 m      | Fácil      | mixta      | OSRM a pie sobre OSM                                                                               |
| Sendero Ecológico La Banda          | Sendero   | 4.29 km   | 32 m       | Fácil      | tierra     | OSRM a pie sobre OSM                                                                               |
| Orillas del río Zamora              | Urbana    | 3.88 km   | 20 m       | Fácil      | pavimento  | OSRM a pie sobre OSM                                                                               |
| Orillas del río Malacatos           | Urbana    | 3.93 km   | 35 m       | Fácil      | pavimento  | OSRM a pie sobre OSM                                                                               |
| Centro histórico de Loja            | Urbana    | 1.55 km   | 30 m       | Fácil      | pavimento  | OSRM a pie sobre OSM                                                                               |
| Mirador Las Peñas                   | Urbana    | 1.23 km   | 93 m       | Fácil      | pavimento  | OSRM a pie sobre OSM                                                                               |
| Parque Daniel Álvarez               | Parque    | 1.02 km   | 27 m       | Fácil      | tierra     | OSRM a pie sobre OSM                                                                               |
| Mirador Altavista                   | Montaña   | 1.43 km   | 126 m      | Fácil      | pavimento  | OSRM a pie sobre OSM                                                                               |
| Mirador Piedra Bola                 | Montaña   | 5.33 km   | 353 m      | Moderada   | tierra     | OSRM a pie sobre OSM                                                                               |
| Cerro Uritusinga                    | Montaña   | 7.14 km   | 523 m      | Difícil    | tierra     | OSRM a pie sobre OSM                                                                               |
| Vilcabamba: Sendero Chaupi          | Sendero   | 3.93 km   | 19 m       | Fácil      | tierra     | OSRM a pie sobre OSM                                                                               |
| Malacatos: Sendero Yunanga          | Sendero   | 8.40 km   | 590 m      | Difícil    | tierra     | OSRM a pie sobre OSM                                                                               |
| Parque Jipiro                       | Parque    | 1.47 km   | 24 m       | Fácil      | pavimento  | vías OSM 170924331, 943824894, 943824893, 694942246, 1182173722, 1182173720, 1182173721, 694942244 |
| Podocarpus Cajanuma: Los Miradores  | Montaña   | 4.79 km   | 546 m      | Difícil    | tierra     | vías OSM 137870253, 40298120, 137870273, 137870248, 137870250                                      |
| Podocarpus Cajanuma: Bosque Nublado | Sendero   | 0.66 km   | 48 m       | Fácil      | tierra     | vía OSM 40298122                                                                                   |
| PUEAR: Sendero del Oso              | Sendero   | 3.44 km   | 394 m      | Moderada   | tierra     | vía OSM 946998708                                                                                  |
| Vilcabamba: Cerro Mandango          | Montaña   | 3.40 km   | 435 m      | Moderada   | tierra     | OSRM a pie sobre OSM hasta la vía 297211965, luego vías OSM 297211965 y 280173286                  |
| Sendero Ecológico Malacatos         | Sendero   | 4.14 km   | 7 m        | Fácil      | tierra     | vía OSM 658584534                                                                                  |
| Pista de atletismo UTPL             | Pista     | 0.20 km   | 4 m        | Fácil      | tartan     | vía OSM 650288229                                                                                  |

## Rutas que se dejaron fuera

- Cajanuma: Oso de Anteojos. La vía OSM del sendero no está conectada con los demás senderos del sector y OSRM no podía trazarla sin inventar tramos.
- Sendero Ecológico Malacatos por OSRM. El trazado calculado se desviaba por carreteras; se usó en su lugar la vía OSM directa (658584534).
- Abra de Zamora. No hay un trazado dedicado en OSM.

Nota: el trazado del Cerro Villonaco termina a unos 230 m de la cumbre, en el último punto al que llega una vía en OSM.

## En las apps

- Lista de rutas: filtro por categoría (Parques, Senderos, Montaña, Urbanas, Pista) además de dificultad y distancia, vista de mapa e insignia "Ruta oficial".
- Detalle: categoría, superficie, fuente con atribución y botón "Correr esta ruta".
- Modo vista previa de la web (`?vista-previa`): incluye las rutas oficiales y carreras de ejemplo que empiezan en ellas.

## Actualizar las rutas

1. Edita los identificadores de vías o los puntos en `scripts/rutas-loja/`.
2. Ejecuta los scripts en orden (trazados, OSRM, elevación, generar) con Python 3.
3. Revisa el GeoJSON y la migración generada, y vuelve a ejecutar `backend/tests/rls/023_reportes.sql` sobre una base local.
