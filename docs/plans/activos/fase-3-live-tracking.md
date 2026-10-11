# Fase 3: Seguimiento en vivo — En curso (desde 10 oct 2026)

## Hecho

- [x] 3.1 Base de datos (`backend/migrations/007_live_tracking.sql`, aplicada):
  - `live_sharing_settings` (solo el dueño), `live_sessions` (las ve el dueño, sus seguidores o todos si la audiencia es pública).
  - Aviso `live` a los seguidores al iniciar una sesión.
  - Verificado: un seguidor ve la sesión y recibe el aviso; quien no sigue no la ve; nadie crea sesiones a nombre de otro.

## Siguiente

- [ ] 3.1 Canal Realtime Broadcast `live:session:{id}` con autorización por `puede_ver_en_vivo` (Realtime Authorization).
- [ ] 3.2 Android: botón "Compartir en vivo", selector de audiencia, envío de posición cada 5 s (difuminada cerca de casa), cierre automático al terminar.
- [ ] 3.3 Web y Android: vista "en vivo" con mapa, marcador animado y métricas; enlace desde la notificación.
- [ ] 3.4 Privacidad: reutilizar la zona de casa de `user_settings` para no emitir los primeros/últimos 300 m.
