# ADR 007: ruta sugerida para hoy en el servidor

Estado: aceptado (oct 2026).

## Contexto

La tarjeta "Ruta sugerida para hoy" se calculaba en cada app con la distancia media de las ultimas
carreras y salia casi siempre la misma. Se pidio que sea siempre segura, que no repita rutas
corridas o sugeridas en los ultimos 3 dias y que sea estable durante el dia.

## Decision

- La regla vive en una sola funcion de Supabase, `ruta_sugerida_hoy`, que usa el mismo indice de
  seguridad que "Seguridad de la zona" y guarda un historial por usuario y dia en
  `rutas.route_suggestions` (contexto Rutas, vista en public segun ADR 005).
- Umbral de seguridad: indice <= 1.0 (no peor que el promedio del canton). Si nada califica se
  muestra un estado vacio con el motivo; nunca una ruta por encima del umbral.
- Web y Android guardan una copia pura de la regla solo para el modo sin red.

## Consecuencias

- Una sola fuente de verdad para web, Android e iOS.
- El historial queda en el servidor, asi que la regla de no repetir vale entre dispositivos.
- Detalle en docs/RUTA-SUGERIDA.md.
