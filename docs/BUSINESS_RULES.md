# Reglas de Negocio - Gacelas

## 1. Privacidad y Visibilidad

### 1.1 Niveles de Privacidad de Perfil

#### `public` (Público)

- Cualquier usuario puede ver el perfil y actividades públicas
- Las solicitudes de seguimiento se aceptan automáticamente
- Las actividades públicas aparecen en búsquedas y feeds globales

#### `followers` (Seguidores)

- Solo seguidores aceptados pueden ver actividades privadas
- Las solicitudes de seguimiento requieren aprobación manual
- El perfil básico (nombre, avatar) es visible para todos
- Las estadísticas agregadas son visibles solo para seguidores

#### `private` (Privado)

- Solo el usuario puede ver sus propias actividades
- Las solicitudes de seguimiento requieren aprobación
- El perfil aparece en búsquedas pero sin detalles
- Útil para usuarios que solo quieren grabar sin compartir

### 1.2 Visibilidad de Actividades

**Regla**: Una actividad puede ser pública o privada independientemente del nivel de privacidad del perfil.

```
SI actividad.is_public = true:
    - Visible para todos (respetando privacidad del usuario)
    - Aparece en feed global
    - Compartible con link

SI actividad.is_public = false:
    - Solo visible para el dueño
    - No aparece en feeds ni búsquedas
```

**Caso especial**: Actividades de cuentas privadas no aparecen en feeds globales aunque `is_public = true`. Solo aparecen en el feed de seguidores aceptados.

### 1.3 Home Zone Fuzzing

**Objetivo**: Proteger la ubicación exacta de casa/trabajo del usuario.

**Regla**:

1. El usuario define su `home_location` y `home_radius_meters` (default: 300m)
2. Cualquier punto GPS dentro del radio de casa se desplaza al borde del círculo
3. El desplazamiento mantiene la dirección original pero oculta la ubicación exacta

**Implementación**:

```
SI distance(track_point, home_location) < home_radius_meters:
    bearing = calculate_bearing(home_location, track_point)
    track_point = move_point(home_location, home_radius_meters, bearing)
```

**Aplicación**:

- Primeros 300m de cualquier actividad
- Últimos 300m de cualquier actividad
- Aplica tanto a actividades públicas como privadas

## 2. Seguimiento en Tiempo Real (Live Tracking)

### 2.1 Inicio de Live Tracking

**Regla**: Live tracking debe ser activado explícitamente por el usuario.

```
Condiciones para iniciar live tracking:
1. Usuario debe tener una actividad en curso
2. Usuario debe tener al menos 1 seguidor (o ser miembro de un club/evento)
3. Usuario debe activar manualmente "Compartir en vivo"
```

**Opciones de compartición**:

- **Todos los seguidores**: Cualquier seguidor aceptado puede ver
- **Seguidores seleccionados**: Solo usuarios específicos
- **Club/Evento específico**: Solo miembros del club o participantes del evento
- **Link temporal**: Link público que expira en 24 horas

### 2.2 Auto-Stop de Live Tracking

**Regla**: El live tracking se detiene automáticamente en los siguientes casos:

1. Usuario termina la actividad
2. Usuario pausa por más de 15 minutos
3. Usuario sale de la app por más de 5 minutos (en Android, si el servicio de ubicación se detiene)
4. Usuario cambia configuración de privacidad a `private`

**Notificación**: El usuario recibe notificación "Live tracking detenido" con razón.

### 2.3 Límites de Live Tracking

**Regla**: Para evitar abuso del sistema:

- Máximo **3 horas** de live tracking continuo
- Máximo **100 espectadores simultáneos** por usuario (límite soft, puede aumentar)
- Si se exceden los límites, se notifica al usuario y se detiene el tracking

## 3. Actividades

### 3.1 Validación de Actividades

**Regla**: Una actividad es válida si cumple:

```
1. duration_seconds >= 60  (mínimo 1 minuto)
2. distance_meters >= 100  (mínimo 100 metros)
3. track_points.count >= 10  (mínimo 10 puntos GPS)
4. start_time < end_time
5. start_time <= NOW()  (no actividades futuras)
```

**Actividades inválidas**:

- Se marcan como `draft` (borrador)
- Solo visibles para el usuario
- Pueden ser editadas o eliminadas sin restricción

### 3.2 Eliminación de Actividades

**Regla**: Al eliminar una actividad:

```
1. Se eliminan todos los track_points (CASCADE)
2. Se eliminan todos los likes y comentarios (CASCADE)
3. Se eliminan todas las referencias en posts (SET NULL)
4. Si la actividad es la fuente de una ruta, la ruta mantiene sus route_points pero pierde la referencia
```

**Soft delete**: No implementado en MVP, pero recomendado para versiones futuras (permite "recuperar actividad").

### 3.3 Edición de Actividades

**Regla**: Un usuario puede editar:

- `title`, `description`: Siempre
- `activity_type`: Solo dentro de 24 horas de creación
- `is_public`: Siempre
- `distance_meters`, `duration_seconds`, etc.: NO editable (calculado automáticamente)

**Histórico de ediciones**: No se guarda en MVP (considerar para futuro).

## 4. Rutas

### 4.1 Creación de Rutas

**Regla**: Una ruta puede ser creada de dos formas:

#### Desde una actividad completada:

```
Condiciones:
1. La actividad debe estar completada (end_time != null)
2. La actividad debe cumplir validaciones (3.1)
3. El usuario es el dueño de la actividad

Proceso:
1. Copiar track_points a route_points (simplificando con Douglas-Peucker)
2. Crear registro en `routes` con referencia a actividad original
3. Calcular métricas: distance, elevation_gain, etc.
```

#### Dibujada manualmente:

```
Condiciones:
1. Mínimo 10 puntos
2. Distancia > 100 metros

Proceso:
1. Usuario dibuja en mapa
2. Sistema calcula distancia y elevación usando API de elevación
3. Crear route_points con sequence ordenado
```

### 4.2 Visibilidad de Rutas

**Regla**: Las rutas son públicas por defecto, pero pueden ser privadas.

```
SI route.is_public = true:
    - Visible en búsquedas y mapa de rutas
    - Cualquier usuario puede guardar la ruta
    - Aparece en perfil del creador

SI route.is_public = false:
    - Solo visible para el creador
    - Puede ser compartida con link privado
    - Útil para rutas personales o en desarrollo
```

### 4.3 Verificación de Rutas

**Regla**: Una ruta puede ser "verificada" por moderadores.

**Criterios de verificación**:

1. La ruta ha sido completada por al menos 5 usuarios diferentes
2. No hay reportes de problemas (ruta peligrosa, no transitable)
3. La ruta tiene descripción detallada y fotos
4. La información de superficie y dificultad es precisa

**Beneficios**:

- Badge de "Ruta Verificada"
- Mayor visibilidad en búsquedas
- Prioridad en recomendaciones

## 5. Comunidad y Follows

### 5.1 Solicitudes de Seguimiento

**Regla**: Flujo de seguimiento varía según privacidad:

```
SI usuario_objetivo.privacy_level = 'public':
    follow.status = 'accepted'  (inmediato)
    Notificación: "X te sigue"

SI usuario_objetivo.privacy_level IN ('followers', 'private'):
    follow.status = 'pending'
    Notificación: "X quiere seguirte"
    Usuario debe aprobar/rechazar manualmente
```

### 5.2 Límites de Seguimiento

**Regla**: Para prevenir spam:

- Máximo **100 solicitudes de seguimiento pendientes** por usuario
- Máximo **50 solicitudes enviadas por día**
- Si usuario rechaza 3 veces seguidas, no se pueden enviar más solicitudes por 30 días

### 5.3 Unfollow

**Regla**: Cualquier usuario puede dejar de seguir en cualquier momento sin notificar al otro usuario.

```
Proceso:
1. Eliminar registro de `follows`
2. No enviar notificación al usuario dejado de seguir
3. Usuario dejado de seguir pierde acceso a actividades privadas inmediatamente
```

## 6. Clubes

### 6.1 Comunidades y grupos de running

**Regla**: Cualquier usuario con sesión puede registrar una comunidad o un grupo de running ([ADR 006](adr/006-comunidades-y-grupos.md)).

- Comunidad: nombre, descripción, ciudad o zona, logo y portada opcionales, contacto opcional, pública o con solicitud.
- Grupo: lo mismo más punto de encuentro (texto y punto en el mapa), días y hora, nivel, ritmo en min/km y cupo opcional. Puede pertenecer a una comunidad.
- El creador queda como `owner` (fundador y administrador).
- El nombre es único por tipo, sin distinguir mayúsculas ni espacios repetidos.

**Límites** (trigger `club_proteger`):

- Máximo **3 comunidades** y **5 grupos** por usuario.
- Máximo **3 altas por día** entre comunidades y grupos.
- Los administradores de Gacelas no tienen límite.
- Un grupo con cupo lleno no acepta nuevos miembros ni aprobaciones.

### 6.2 Roles en Clubes

#### `owner` (Dueño)

- Puede editar información del club
- Puede promover/degradar admins
- Puede eliminar el club
- Puede expulsar miembros

#### `admin` (Administrador)

- Puede aprobar solicitudes de membresía
- Puede crear eventos
- Puede publicar en el feed del club
- Puede expulsar miembros (no admins)

#### `member` (Miembro)

- Puede ver contenido del club
- Puede participar en eventos
- Puede comentar posts

### 6.3 Unirse a Clubes

**Regla**:

```
SI club.requires_approval = false:
    Usuario se une inmediatamente (role = 'member')

SI club.requires_approval = true:
    Usuario envía solicitud (status = 'pending')
    Admin/Owner debe aprobar

SI club.hidden_at no es nulo o el cupo está lleno:
    No se puede unir
```

### 6.4 Eliminación de Clubes

**Regla**: Solo el `owner` puede eliminar un club. Owner y admins pueden editarlo; el tipo no cambia. Los administradores de Gacelas pueden ocultarlo desde la cola de moderación.

**Proceso**:

```
1. Verificar que no hay eventos futuros (o cancelarlos)
2. Eliminar todos los posts del club (o reasignar a personal)
3. Eliminar todas las membresías
4. Eliminar el club
```

**Soft delete recomendado**: Marcar como `deleted_at` en lugar de eliminar, para preservar historial.

## 7. Eventos

### 7.1 Creación de Eventos

**Regla**: Eventos pueden ser creados por:

- Cualquier usuario (evento independiente)
- Admin/Owner de un club (evento del club)

**Validaciones**:

```
1. start_time > NOW() (eventos futuros solamente)
2. start_time < NOW() + 90 días (máximo 3 meses adelante)
3. duration_minutes <= 480 (máximo 8 horas)
4. max_participants >= 2 (si se especifica)
```

### 7.2 Inscripción a Eventos

**Regla**:

```
SI evento.is_public = true:
    Cualquier usuario puede inscribirse

SI evento.is_public = false:
    Solo miembros del club (si evento de club) o usuarios invitados
```

**Límite de participantes**:

```
SI evento.max_participants != null:
    SI evento.participants_count >= evento.max_participants:
        Lista de espera (waitlist)
    SINO:
        Inscripción inmediata (status = 'registered')
```

### 7.3 Cancelación de Inscripción

**Regla**: Usuario puede cancelar hasta 1 hora antes del evento.

```
SI evento.start_time - NOW() > 1 hora:
    Cancelación permitida sin penalización

SI evento.start_time - NOW() <= 1 hora:
    Cancelación no permitida (o con penalización de reputación en futuro)
```

### 7.4 Eventos Pasados

**Regla**: Después de `start_time + duration_minutes`:

1. Estado del evento cambia a `completed`
2. Se calculan estadísticas de participación real (quién asistió basado en ubicación)
3. Participantes pueden marcar como "asistí" si tienen actividad en esa ubicación y hora

## 8. Moderación y Reportes

### 8.1 Reportar Contenido

**Regla**: Usuarios pueden reportar:

- Actividades (contenido inapropiado en título/descripción)
- Rutas (información incorrecta, ruta peligrosa)
- Comentarios (spam, acoso)
- Usuarios (comportamiento inapropiado)

**Proceso**:

```
1. Usuario envía reporte con categoría y descripción
2. Sistema marca contenido como "under review"
3. Moderadores revisan (en futuro: automático con ML)
4. Decisión: eliminar, advertir usuario, o rechazar reporte
```

**Límites**: Máximo 10 reportes por usuario por día (prevenir abuso).

### 8.2 Penalizaciones

**Regla**: Usuarios con múltiples infracciones:

```
1ra infracción: Advertencia
2da infracción: Suspensión de 7 días (no puede publicar)
3ra infracción: Suspensión de 30 días
4ta infracción: Ban permanente
```

**Infracciones graves** (acoso, contenido explícito): Ban inmediato.

## 9. Límites y Cuotas

### 9.1 Límites de API

**Regla**: Para proteger el sistema en free tier:

- Máximo **10 actividades iniciadas por día** por usuario
- Máximo **5 rutas creadas por día** por usuario
- Máximo **100 comentarios por día** por usuario
- Máximo **500 likes por día** por usuario

**Rate limiting**: 60 requests por minuto por usuario.

### 9.2 Retención de Datos

**Regla**: En free tier:

- **Actividades**: Todas se mantienen indefinidamente
- **Track points**: Se comprimen después de 6 meses (algoritmo Douglas-Peucker más agresivo)
- **Notificaciones**: Se eliminan después de 90 días
- **Comentarios**: Se mantienen indefinidamente

**Advertencia**: Si el usuario se acerca a límites de almacenamiento, se le notifica y puede:

1. Eliminar actividades antiguas manualmente
2. Aceptar compresión adicional de track points
3. Upgrade a plan de pago (futuro)

## 10. Notificaciones

### 10.1 Tipos de Notificaciones

**Regla**: Usuario recibe notificación cuando:

1. Alguien lo sigue (`follow_accepted` si ya era seguidor)
2. Alguien le envía solicitud de seguimiento (`follow_request`)
3. Alguien da like a su actividad (`activity_like`, agrupado cada 10 likes)
4. Alguien comenta su actividad (`activity_comment`)
5. Lo invitan a un evento (`event_invite`)
6. Lo invitan a un club (`club_invite`)
7. Un seguido inicia live tracking (`live_tracking_started`)

### 10.2 Preferencias de Notificaciones

**Regla**: Usuario puede configurar:

- **Push notifications** (móvil): ON/OFF por tipo
- **Email notifications**: ON/OFF por tipo
- **In-app notifications**: Siempre ON

**Frecuencia de agrupación**:

- Likes: agrupar cada 10 o cada 6 horas
- Comentarios: uno por uno
- Follows: uno por uno

### 10.3 Notificaciones de Live Tracking

**Regla especial**: Para evitar spam:

- Máximo 1 notificación de "X está corriendo" por usuario por día
- Solo se notifica si el usuario ha interactuado con ese corredor en los últimos 7 días
- Usuario puede desactivar notificaciones de live tracking globalmente

## 11. Integridad de Datos

### 11.1 Cascade Rules

**Regla**: Borrados en cascada:

```
Usuario eliminado:
  - Eliminar profile (CASCADE)
  - Eliminar actividades (CASCADE)
    - Eliminar track_points (CASCADE)
    - Eliminar likes/comentarios (CASCADE)
  - Eliminar rutas (SET creator TO null, mantener route_points)
  - Eliminar follows (CASCADE)
  - Eliminar club_members (CASCADE)
  - Eliminar notificaciones (CASCADE)
```

### 11.2 Consistencia de Contadores

**Regla**: Los campos `*_count` (likes_count, comments_count, etc.) deben actualizarse mediante **triggers de base de datos**:

```sql
CREATE TRIGGER increment_likes_count
AFTER INSERT ON activity_likes
FOR EACH ROW
EXECUTE FUNCTION increment_activity_likes_count();
```

**Recalculo periódico**: Job diario que verifica consistencia y corrige discrepancias.

## 12. Futuras Reglas (Post-MVP)

- **Achievements y badges**: Desbloquear logros por hitos (100km, 10 actividades, etc.)
- **Leaderboards**: Rankings semanales/mensuales de distancia, elevación
- **Challenges**: Desafíos comunitarios (ej: "Corre 50km esta semana")
- **Strava integration**: Importar actividades desde Strava
- **Payment & subscriptions**: Plan premium con más almacenamiento y features

---

Ver [SECURITY_PRIVACY.md](SECURITY_PRIVACY.md) para implementación técnica de estas reglas.
