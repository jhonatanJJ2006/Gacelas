-- 014: las rutas de ejemplo declaraban mas distancia que su trazo (Jipiro 5.2 km con un trazo de 1.8 km,
-- y la navegacion decia "faltan 1.8 km"). Se iguala distance_km al largo real del trazo.
UPDATE routes SET distance_km = 1.8 WHERE id = '1' AND distance_km = 5.2;
UPDATE routes SET distance_km = 2.2 WHERE id = '2' AND distance_km = 8.5;
UPDATE routes SET distance_km = 4.4 WHERE id = '3' AND distance_km = 12.3;
