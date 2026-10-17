# Hoja de Ruta - Gacelas

## Visión General

Gacelas se desarrollará en fases iterativas, comenzando con un MVP funcional y escalando features según feedback de usuarios reales.

## Capacidad y perfil free

El codigo se prepara para 3.000 sesiones concurrentes. Mientras el proyecto siga en el plan Free de Supabase, el runtime usa el perfil free: 150 conexiones Realtime de las 200 permitidas, un canal vivo por cliente y sin polling de posiciones. Trescientas personas en vivo a la vez no caben en ese plan. La decision esta en [ADR 002](adr/002-capacity-and-free-profile.md).

## Fases 0, 1 y 2: Completadas

Movidas a [plans/completados](plans/completados/). Ver [plans/README.md](plans/README.md).

---

## Fase 3: Live Tracking en Tiempo Real: Completada

Ver [plans/completados/fase-3-live-tracking.md](plans/completados/fase-3-live-tracking.md).

---

## Fase 4: Rutas y Exploración: Completada

Ver [plans/completados/fase-4-rutas.md](plans/completados/fase-4-rutas.md).

---

## Fase 5: Eventos y Clubes (completada)

**Objetivo**: Facilitar carreras grupales y comunidades organizadas.

### 5.1 Clubes

**Features**:

- [x] Crear club (nombre, descripción, ubicación)
- [x] Unirse a club (público o con aprobación)
- [x] Roles: owner, admin, member
- [x] Feed del club (posts, actividades de miembros)
- [x] Eventos del club

**Implementación**:

- Tablas: `clubs`, `club_members`, `club_posts`
- Permisos por rol

### 5.2 Eventos

**Features**:

- [x] Crear evento (carrera grupal, race, entrenamiento)
- [x] Definir fecha, hora, punto de encuentro
- [x] Ruta planificada (opcional)
- [x] Límite de participantes
- [x] Inscripción y lista de espera
- [x] Calendario de eventos

**Implementación**:

- Tablas: `events`, `event_participants`
- Notificaciones de recordatorio (1 día antes, 1 hora antes)

### 5.3 Live Tracking en Eventos

**Features**:

- [x] Vista de mapa con TODOS los participantes en tiempo real
- [x] Leaderboard en vivo (quién va primero)
- [x] Ver track individual de cada participante
- [x] Replay del evento después de terminar

**Implementación**:

- Broadcast a channel de evento: `live:event:{id}`
- Agregación de posiciones de múltiples usuarios

---

## Fase 6: Análisis y Estadísticas

**Objetivo**: Insights profundos sobre performance.

### 6.1 Métricas Avanzadas

**Features**:

- [ ] Gráfico de pace por km
- [ ] Gráfico de elevación vs distancia
- [ ] Zonas de ritmo cardíaco (si se integra sensor)
- [ ] Comparación de actividades
- [ ] Personal records (PR) automáticos

**Implementación**:

- Calcular en Edge Function o cliente
- Librerías de gráficos: Chart.js / Recharts

### 6.2 Dashboards

**Features**:

- [ ] Dashboard semanal/mensual/anual
- [ ] Total de km, tiempo, elevación
- [ ] Racha actual (días consecutivos)
- [ ] Actividad más rápida, más larga
- [ ] Comparación con seguidores (opcional, con permiso)

### 6.3 Exportación

**Features**:

- [ ] Exportar actividad a GPX
- [ ] Exportar actividad a TCX
- [ ] Importar desde Strava/Garmin (futuro)

---

## Fase 7: Gamificación y Motivación

**Objetivo**: Motivar a usuarios con logros y desafíos.

### 7.1 Achievements

**Features**:

- [ ] Sistema de badges/trofeos
- [ ] Logros por hitos (primera carrera, 10km, 100km total, etc.)
- [ ] Colección de badges en perfil
- [ ] Notificación al desbloquear logro

**Ejemplos de logros**:

- "Primera Carrera" (completar 1 actividad)
- "Madrugador" (correr antes de 6am)
- "Centurión" (100km totales)
- "Constante" (7 días consecutivos)
- "Explorador" (correr en 5 ciudades diferentes)

### 7.2 Leaderboards

**Features**:

- [ ] Ranking semanal/mensual de distancia
- [ ] Ranking por seguidores/club
- [ ] Ranking global (opcional)
- [ ] Filtros por género, edad, ubicación

**Implementación**:

- Queries agregadas con `RANK()` window function
- Caché con TTL de 1 hora

### 7.3 Challenges

**Features**:

- [ ] Desafíos comunitarios ("Corre 50km este mes")
- [ ] Progreso visual (barra de progreso)
- [ ] Invitar amigos a desafíos
- [ ] Reward al completar (badge especial)

---

## Fase 8: Monetización (Opcional, Post-MVP)

**Objetivo**: Sostener el proyecto con ingresos.

### 8.1 Plan Premium

**Features incluidas**:

- [ ] Análisis avanzados ilimitados
- [ ] Exportación ilimitada de actividades
- [ ] Almacenamiento extendido (10 GB)
- [ ] Sin ads (si se implementan)
- [ ] Badge de "Supporter"
- [ ] Acceso early a nuevas features

**Precio sugerido**: $3-5/mes o $30-50/año

### 8.2 Integraciones Premium

**Features**:

- [ ] Conectar con Strava
- [ ] Conectar con Garmin Connect
- [ ] Integración con Apple Health
- [ ] Integración con Google Fit

### 8.3 Eventos de Pago

**Features**:

- [ ] Organizadores pueden cobrar inscripción
- [ ] Gacelas toma comisión (10-15%)
- [ ] Integración con pasarela de pago (Stripe)

---

## Fase 9: Escalabilidad y Optimización

**Objetivo**: Preparar la app para escalar a miles de usuarios.

### 9.1 Performance

**Tareas**:

- [ ] Implementar Redis para caché
- [ ] CDN para assets estáticos
- [ ] Optimización de queries lentas
- [ ] Índices adicionales basados en uso real
- [ ] Lazy loading agresivo

### 9.2 Infraestructura

**Opciones**:

- [ ] Upgrade a Supabase Pro ($25/mes)
- [ ] Self-hosting de PostgreSQL + Supabase Realtime
- [ ] Read replicas para queries pesadas

### 9.3 Monitoreo

**Herramientas**:

- [ ] Sentry para error tracking
- [ ] Mixpanel/PostHog para analytics
- [ ] Uptime monitoring (UptimeRobot)
- [ ] Alertas automáticas en Slack/Discord

---

## Fase 10: Expansión Regional

**Objetivo**: Adaptar la app para múltiples países.

### 10.1 Internacionalización

**Features**:

- [ ] Soporte multi-idioma (i18n)
- [ ] Español (Ecuador, España, México)
- [ ] Inglés
- [ ] Portugués (Brasil)
- [ ] Unidades (métrico/imperial)

### 10.2 Localización

**Features**:

- [ ] Rutas populares por ciudad
- [ ] Eventos locales destacados
- [ ] Clubes por región
- [ ] Soporte de zonas horarias

---

## Priorización de Features

### Must-Have para MVP (Fase 1-2)

1. Grabar actividades con GPS
2. Ver actividades en mapa
3. Autenticación
4. Perfil básico
5. Feed social
6. Follows

### Should-Have para Beta (Fase 3-4)

1. Live tracking
2. Rutas compartidas
3. Notificaciones
4. Privacidad avanzada

### Nice-to-Have para v1.0 (Fase 5-7)

1. Clubes y eventos
2. Achievements
3. Análisis avanzados
4. Challenges

### Future (Fase 8-10)

1. Monetización
2. Integraciones externas
3. Internacionalización
4. Escalabilidad avanzada

---

## Métricas de Éxito

### MVP (3 meses)

- [ ] 50 usuarios registrados
- [ ] 200 actividades grabadas
- [ ] 80% retention semanal
- [ ] <5% crash rate

### Beta (6 meses)

- [ ] 500 usuarios registrados
- [ ] 2,000 actividades
- [ ] 100 rutas compartidas
- [ ] 70% retention semanal

### v1.0 (12 meses)

- [ ] 5,000 usuarios registrados
- [ ] 20,000 actividades
- [ ] 500 rutas
- [ ] 50 clubes activos
- [ ] 200 eventos realizados

---

## Cronograma Tentativo

| Fase    | Features Principales            | Duración Estimada      | Estado        |
| ------- | ------------------------------- | ---------------------- | ------------- |
| Fase 0  | Arquitectura e infraestructura  | Setup inicial          | Completo      |
| Fase 1  | MVP - Grabación y visualización | Implementación         | Completo      |
| Fase 2  | Feed social y follows           | Desarrollo incremental | Completo      |
| Fase 3  | Live tracking                   | Desarrollo incremental | Completa      |
| Fase 4  | Rutas y exploración             | Desarrollo incremental | Completa      |
| Fase 5  | Eventos y clubes                | Desarrollo incremental | Pendiente     |
| Fase 6  | Análisis avanzados              | Desarrollo incremental | Pendiente     |
| Fase 7  | Gamificación                    | Desarrollo incremental | Pendiente     |
| Fase 8+ | Monetización y escala           | Futuro                 | Ideas futuras |

**Nota**: La implementación es iterativa. Las fases pueden superponerse o reordenarse según feedback de usuarios reales.

---

## Siguiente Paso Inmediato

**Siguiente paso**: Fase 5, eventos y clubes.

Ver [CONTRIBUTING.md](CONTRIBUTING.md) para comenzar a contribuir al desarrollo.
