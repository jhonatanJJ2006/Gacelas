# Fase 4: Rutas y exploración

Estado: **Completada** (octubre 2026). El plan empezó en `activos/` y se movió aquí al terminar.

## Alcance hecho

### 4.1 Creación de rutas

- Publicar una carrera como ruta pública desde el resumen (web y Android): nombre, descripción, dificultad y etiquetas (web).
- Trazo simplificado en el cliente con Douglas-Peucker (`apps/web/src/lib/rutas.ts`, `apps/android/.../data/rutas/Explorar.kt`), máximo 500 puntos.
- Distancia y punto de salida calculados en el servidor con PostGIS (trigger `route_geo`). Las rutas oficiales (sin autor) mantienen su distancia declarada.
- Etiquetas: panorámica, plana, urbana, sendero, montaña, parque, río (CHECK en la tabla).

### 4.2 Exploración

- Las rutas salen de Supabase (si el servidor no responde o no hay rutas, se usan las de ejemplo).
- Filtros por dificultad, distancia y etiqueta; "Cerca de mí" usa la RPC `rutas_cercanas` (PostGIS, `SECURITY INVOKER`, respeta el RLS) y ordena por cercanía.
- Vista de detalle (`/rutas/:id` en la web, `rutas/detalle/{id}` en Android) con mapa del trazo, datos, etiquetas, autor y "Correr esta ruta" (abre el buscador con la ruta).

### 4.3 Social en rutas

- Me gusta (`route_likes`) y reseñas de 1 a 5 estrellas con comentario (`route_reviews`, una por persona y ruta).
- Contadores `likes_count`, `rating_avg` y `rating_count` mantenidos por triggers; el cliente no puede escribirlos.

## Migración

`backend/migrations/009_rutas.sql`, aplicada al proyecto real.

## Pruebas

- Web: `src/lib/__tests__/rutas.test.ts` (Douglas-Peucker, filtros, cercanía) y flujo lista, detalle, buscador en `src/__tests__/rutas.test.tsx`.
- Android: `ExplorarTest` (Douglas-Peucker, filtros, filas, me gusta y reseñas con `FakeBackend`).
- Probado en producción y en un emulador Android 14.

## Queda para después

- Elevación con una API externa y gráfica de elevación (el desnivel viene del GPS de la carrera).
- Dibujar una ruta a mano en el mapa.
- Fotos de rutas y "quién ha corrido esta ruta".
- Clusters en el mapa de exploración.

## Extras (segunda entrega)

- Perfil de elevación (Open-Meteo, 60 muestras) guardado en `routes.elevation_profile` y gráfica en el detalle (SVG en web, Canvas en Android). Rutas semilla rellenadas.
- Dibujar ruta a mano en el mapa (`/rutas/crear`, `rutas/crear` en Android), con deshacer/borrar, dificultad y etiquetas.
- Fotos de rutas: bucket `route-photos` (público, 5 MB, solo imágenes, cada usuario sube en su carpeta) + tabla `route_photos` con RLS.
- "Quién la ha corrido": `route_runs` + RPC `corredores_ruta` (solo perfiles públicos, propios o seguidos; el total sí es global).
- Agrupación de marcadores en el mapa de rutas (`@googlemaps/markerclusterer` en web, `maps-compose-utils` en Android).
- Selector de etiquetas al publicar una carrera como ruta en Android.
- Migración: `backend/migrations/010_rutas_extra.sql`.
