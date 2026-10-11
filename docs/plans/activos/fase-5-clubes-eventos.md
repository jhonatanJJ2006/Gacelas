# Fase 5: Clubes y eventos

Objetivo: carreras grupales y comunidades organizadas (ROADMAP, Fase 5).

## Alcance

### 5.1 Clubes

- [x] Crear club (nombre, descripción, ubicación, abierto o con aprobación)
- [x] Unirse (directo o solicitud pendiente que aprueba un admin/owner)
- [x] Roles owner / admin / member; el owner nombra admins; nadie expulsa al owner
- [x] Feed del club (publicaciones de miembros; en clubes con aprobación solo los miembros las ven)
- [x] Eventos del club (solo admins/owner los crean)
- Tablas `clubs`, `club_members`, `club_posts`; permisos por rol en RLS y triggers (`rol_club`).

### 5.2 Eventos

- [x] Tipo (grupal, competencia, entrenamiento), fecha y hora reales (`starts_at`), punto de encuentro
- [x] Ruta planificada opcional (`route_id`)
- [x] Cupo (`max_participants`) con lista de espera automática; al liberarse un cupo sube el primero y se le avisa
- [x] El cliente no puede saltarse la lista de espera (estado fijado por trigger)
- [x] Calendario mensual de eventos (web y Android)
- [x] Recordatorios 1 día y 1 hora antes (`pg_cron` cada 10 min, sin duplicados)

### 5.3 En vivo en eventos

- [x] `live_iniciar_evento`: solo los inscritos pueden correr en vivo asociados al evento
- [x] Mapa con todos los participantes en tiempo real (canal `live:event:{id}` sobre `live_sessions`)
- [x] Clasificación en vivo (distancia, desempate por tiempo)
- [x] Trazo de cada participante y repetición del evento al terminar
- [x] Se mantiene el ocultado de casa (posiciones ocultas no se exponen)

## Migración

`backend/migrations/012_clubes_eventos.sql` (aplicada en el proyecto real).

## Pruebas

- Web: `lib/__tests__/fase5.test.ts` (clubes, cupo, calendario, clasificación, repetición).
- Android: `Fase5Test.kt` (lo mismo + repositorio de clubes con backend falso).
- RLS en producción: 27/27 comprobaciones (script de QA externo `qa/fase5/rls.mjs`).
