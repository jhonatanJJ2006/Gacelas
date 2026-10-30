-- 037: corrige la distancia de las rutas de ejemplo 1, 2 y 3 de la migracion 003.
-- 003 guardo 5.2, 8.5 y 12.3 km, pero sus trazados miden 1.81, 2.24 y 4.40 km.
-- Android ya usaba los valores medidos (MockData.kt); la web y el backend se alinean a ellos.
-- Solo toca rutas de ejemplo sin autor, no las rutas oficiales (022) ni las de usuarios.
UPDATE rutas.routes SET distance_km = 1.8 WHERE id = '1' AND author_id IS NULL AND distance_km = 5.2;
UPDATE rutas.routes SET distance_km = 2.2 WHERE id = '2' AND author_id IS NULL AND distance_km = 8.5;
UPDATE rutas.routes SET distance_km = 4.4 WHERE id = '3' AND author_id IS NULL AND distance_km = 12.3;
