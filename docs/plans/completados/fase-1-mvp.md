# Fase 1: MVP — Completada (oct 2026)

- 1.1 Backend: proyecto Supabase, PostGIS, esquema y RLS (`backend/migrations/001`–`003`), Auth email + Google.
- 1.2 Web: login/registro/salida, Inicio, actividades propias con mapa, perfil. Desplegada en https://gacelas.vercel.app (Vercel, despliegue al hacer push a `main`).
- 1.3 Android: grabación GPS en primer plano, pausa/reanudar, parciales, resumen y guardado en Supabase.
- 1.4 Despliegue: Vercel, claves de Google Maps, APK de pruebas.

Notas: Android usa un contenedor de dependencias propio en lugar de Hilt; los puntos de GPS se guardan en `activity_tracks` (JSON) en lugar de `track_points` fila por fila.
