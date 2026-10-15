# Hoja de Ruta - Gacelas

## Visión General

Gacelas se desarrollará en fases iterativas, comenzando con un MVP funcional y escalando features según feedback de usuarios reales.

## Capacidad y perfil free

El codigo se prepara para 3.000 sesiones concurrentes. Mientras el proyecto siga en el plan Free de Supabase, el runtime usa el perfil free: 150 conexiones Realtime de las 200 permitidas, un canal vivo por cliente y sin polling de posiciones. Trescientas personas en vivo a la vez no caben en ese plan. La decision esta en [ADR 002](adr/002-capacity-and-free-profile.md).

## Fase 0: Arquitectura e Infraestructura (ACTUAL)

**Duración estimada**: Setup inicial  
**Estado**: Completado

### Objetivos

- [x] Definir arquitectura del sistema
- [x] Seleccionar stack tecnológico (Supabase + React + Kotlin)
- [x] Crear estructura de monorepo
- [x] Documentar diseño, reglas de negocio, modelo de datos
- [x] Documentar seguridad, privacidad, presupuesto de tier gratuito
- [x] Establecer guías de contribución

### Entregables

- [x] Documentación completa en `/docs`
- [x] ADR de decisiones arquitectónicas
- [x] Scaffolding de apps (web y Android)
- [x] README con instrucciones de setup

---

## Fase 1: MVP - Funcionalidad Core (SIGUIENTE)

**Objetivo**: Aplicación funcional mínima para grabar actividades y verlas en mapa.

### 1.1 Backend Setup

**Tareas**:

- [ ] Crear proyecto en Supabase
- [ ] Ejecutar migraciones de base de datos (schema inicial)
- [ ] Habilitar PostGIS para geoespacial
- [ ] Configurar Row Level Security (RLS) policies básicas
- [ ] Crear índices para performance
- [ ] Configurar Supabase Auth (email + password)
- [ ] Crear tablas: `users`, `profiles`, `activities`, `track_points`

**Criterios de aceptación**:

- Base de datos accesible desde apps
- Autenticación funcional
- Queries geoespaciales funcionan correctamente

### 1.2 Web App - Básico

**Features**:

- [ ] Autenticación (login, registro, logout)
- [ ] Página de inicio con mapa
- [ ] Ver listado de actividades propias
- [ ] Ver detalle de actividad en mapa
- [ ] Perfil de usuario básico

**Tecnologías**:

- React 18 + TypeScript
- Vite para build
- Tailwind CSS
- Google Maps JavaScript API
- Supabase Client

**Criterios de aceptación**:

- Usuario puede registrarse y autenticarse
- Usuario puede ver sus actividades en mapa

### 1.3 Android App - Grabación de Actividades

**Features**:

- [ ] Autenticación (login, registro)
- [ ] Pantalla de grabación con mapa
- [ ] Iniciar/pausar/detener actividad
- [ ] Location Service en foreground
- [ ] Guardar track points en lotes (batch)
- [ ] Mostrar métricas en tiempo real (distancia, tiempo, pace)
- [ ] Pantalla de resumen post-actividad
- [ ] Guardar actividad en Supabase

**Tecnologías**:

- Kotlin + Jetpack Compose
- Fused Location Provider
- Google Maps SDK Android
- Supabase Kotlin SDK
- Hilt para DI

**Criterios de aceptación**:

- Usuario puede grabar una actividad completa
- Track points se guardan correctamente
- Métricas calculadas son precisas
- Funciona en background con pantalla apagada

### 1.4 Despliegue

**Tareas**:

- [ ] Deploy web a Vercel/Netlify
- [ ] Configurar domain custom (opcional)
- [ ] APK de Android para testing interno
- [ ] Configurar Google Maps API keys
- [ ] Health check para mantener Supabase activo

**Entregables**:

- Web app pública accesible
- APK descargable para testers
- Guía de instalación

---

## Fase 2: Social y Comunidad

**Objetivo**: Transformar la app en una plataforma social para runners.

### 2.1 Follows y Feed

**Features**:

- [ ] Buscar usuarios por username
- [ ] Enviar/aceptar solicitudes de seguimiento
- [ ] Feed de actividades de seguidos
- [ ] Likes en actividades
- [ ] Comentarios en actividades
- [ ] Notificaciones básicas

**Implementación**:

- Tablas: `follows`, `activity_likes`, `comments`, `notifications`
- RLS policies para privacidad
- Realtime subscriptions para feed actualizado

### 2.2 Perfil Mejorado

**Features**:

- [ ] Perfil público con stats
- [ ] Total de km, actividades, racha
- [ ] Gráficos de progreso semanal/mensual
- [ ] Configuración de privacidad (public/followers/private)
- [ ] Editar avatar y bio

**Implementación**:

- Queries agregadas con `SUM`, `AVG`, `COUNT`
- Supabase Storage para avatares

### 2.3 Web - Vista de Comunidad

**Features**:

- [ ] Feed social responsive
- [ ] Tarjetas de actividad con mini-mapa
- [ ] Perfiles de otros usuarios
- [ ] Lista de seguidores/siguiendo
- [ ] Buscador de usuarios

---

## Fase 3: Live Tracking en Tiempo Real

**Objetivo**: Compartir ubicación en vivo durante actividades.

### 3.1 Backend - Realtime

**Features**:

- [ ] Supabase Realtime Broadcast setup
- [ ] Tabla `live_sharing_settings`
- [ ] Edge Function para verificar permisos de visualización
- [ ] Channels por actividad: `live:activity:{id}`

### 3.2 Android - Compartir en Vivo

**Features**:

- [ ] Botón "Compartir en vivo" durante actividad
- [ ] Selector de audiencia (seguidores, club, link público)
- [ ] Broadcast de posición cada 5 segundos
- [ ] Indicador visual de "broadcasting"
- [ ] Auto-stop al terminar actividad

### 3.3 Web & Android - Ver en Vivo

**Features**:

- [ ] Notificación "X está corriendo ahora"
- [ ] Vista de mapa en vivo con posición actualizada
- [ ] Métricas en tiempo real del corredor
- [ ] Animación suave de transición de marcador

### 3.4 Privacidad

**Features**:

- [ ] Home zone fuzzing (primeros/últimos 300m)
- [ ] Configurar ubicación de casa en perfil
- [ ] Permisos granulares de visualización

---

## Fase 4: Rutas y Exploración

**Objetivo**: Descubrir y compartir rutas populares.

### 4.1 Creación de Rutas

**Features**:

- [ ] Guardar actividad como ruta pública
- [ ] Dibujar ruta manualmente en mapa
- [ ] Asignar nombre, descripción, dificultad
- [ ] Tags (scenic, flat, urban, trail, etc.)
- [ ] Calcular elevación con API externa

**Implementación**:

- Tablas: `routes`, `route_points`
- Douglas-Peucker para simplificar puntos
- PostGIS para cálculos de distancia

### 4.2 Exploración de Rutas

**Features**:

- [ ] Mapa con rutas cercanas (clusters)
- [ ] Filtros: distancia, dificultad, tipo
- [ ] Búsqueda por ubicación o ciudad
- [ ] Vista de detalle con elevación gráfica
- [ ] Guardar ruta para más tarde
- [ ] Botón "Correr esta ruta" (inicia actividad con ruta cargada)

### 4.3 Social en Rutas

**Features**:

- [ ] Dar like a rutas
- [ ] Comentarios y reviews
- [ ] Rating (1-5 estrellas)
- [ ] Fotos de la ruta
- [ ] Ver quién ha corrido esta ruta

---

## Fase 5: Eventos y Clubes

**Objetivo**: Facilitar carreras grupales y comunidades organizadas.

### 5.1 Clubes

**Features**:

- [ ] Crear club (nombre, descripción, ubicación)
- [ ] Unirse a club (público o con aprobación)
- [ ] Roles: owner, admin, member
- [ ] Feed del club (posts, actividades de miembros)
- [ ] Eventos del club

**Implementación**:

- Tablas: `clubs`, `club_members`, `club_posts`
- Permisos por rol

### 5.2 Eventos

**Features**:

- [ ] Crear evento (carrera grupal, race, entrenamiento)
- [ ] Definir fecha, hora, punto de encuentro
- [ ] Ruta planificada (opcional)
- [ ] Límite de participantes
- [ ] Inscripción y lista de espera
- [ ] Calendario de eventos

**Implementación**:

- Tablas: `events`, `event_participants`
- Notificaciones de recordatorio (1 día antes, 1 hora antes)

### 5.3 Live Tracking en Eventos

**Features**:

- [ ] Vista de mapa con TODOS los participantes en tiempo real
- [ ] Leaderboard en vivo (quién va primero)
- [ ] Ver track individual de cada participante
- [ ] Replay del evento después de terminar

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

| Fase    | Features Principales            | Duración Estimada      | Estado              |
| ------- | ------------------------------- | ---------------------- | ------------------- |
| Fase 0  | Arquitectura e infraestructura  | Setup inicial          | Completo            |
| Fase 1  | MVP - Grabación y visualización | Implementación         | Siguiente           |
| Fase 2  | Feed social y follows           | Desarrollo incremental | Pendiente Pendiente |
| Fase 3  | Live tracking                   | Desarrollo incremental | Pendiente Pendiente |
| Fase 4  | Rutas y exploración             | Desarrollo incremental | Pendiente Pendiente |
| Fase 5  | Eventos y clubes                | Desarrollo incremental | Pendiente Pendiente |
| Fase 6  | Análisis avanzados              | Desarrollo incremental | Pendiente Pendiente |
| Fase 7  | Gamificación                    | Desarrollo incremental | Pendiente Pendiente |
| Fase 8+ | Monetización y escala           | Futuro                 | Ideas futuras       |

**Nota**: La implementación es iterativa. Las fases pueden superponerse o reordenarse según feedback de usuarios reales.

---

## Siguiente Paso Inmediato

**Siguiente paso**: Comenzar Fase 1.1: Configurar proyecto en Supabase y ejecutar migraciones.

Ver [CONTRIBUTING.md](CONTRIBUTING.md) para comenzar a contribuir al desarrollo.
