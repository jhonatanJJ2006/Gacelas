# Fase 3: Seguimiento en vivo — Completada (11 oct 2026)

## 3.1 Backend

- [x] `backend/migrations/007_live_tracking.sql`: `live_sharing_settings` (solo el dueño) y `live_sessions` con RLS: la ven el dueño, sus seguidores o todos si la audiencia es pública. Aviso `live` a los seguidores al empezar.
- [x] `backend/migrations/008_live_rpc.sql`: las posiciones solo se escriben con `live_iniciar`, `live_enviar` y `live_terminar` (SECURITY DEFINER). La salida queda en `live_private` (sin acceso por API). Las sesiones sin posiciones se cierran solas a los 15 min (pg_cron).
- [x] Realtime: `live_sessions` está en la publicación `supabase_realtime`; los espectadores usan `postgres_changes`, que aplica el mismo RLS. Se eligió esto en lugar de Broadcast porque no hace falta escribir políticas aparte para el canal.

## 3.2 Compartir en vivo

- [x] Web y Android: botón "Compartir en vivo" en la carrera en curso; envía la posición cada 5 s; se cierra al terminar o descartar.
- [x] Audiencia: la de `live_sharing_settings` o, si no hay, según la visibilidad del perfil (si es pública, pública; si no, seguidores).

## 3.3 Ver en vivo

- [x] Web: `/comunidad/en-vivo` (lista) y `/comunidad/en-vivo/:id` (mapa con trazado, distancia, tiempo y ritmo) con Supabase Realtime; aviso "X está corriendo en vivo" en Comunidad. La notificación `live` abre la lista.
- [x] Android: mismas pantallas; consulta cada 5 s (la lista cada 10 s).

## 3.4 Privacidad

- [x] Con "Ocultar ubicación de casa" activo, mientras el corredor está dentro del radio (300 m por defecto) alrededor de la salida, el servidor no publica la posición (`hidden = true`) y los espectadores ven "Ubicación oculta cerca de casa".

## Pruebas

- Web: `src/lib/__tests__/enVivo.test.ts`. Android: `EnVivoTest.kt`.
- Contra Supabase real: script de QA `live.mjs` (carpeta de QA, fuera del repo) (11 comprobaciones: RPC, RLS, difuminado, Realtime a seguidor y no seguidor, aviso, cierre).
