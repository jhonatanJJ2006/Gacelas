# Presupuesto de Tier Gratuito - Gacelas

## Supabase Free Tier: Límites

Documentación oficial: https://supabase.com/pricing

### Límites Absolutos

| Recurso            | Límite Free Tier            | Notas                                    |
| ------------------ | --------------------------- | ---------------------------------------- |
| **Database**       | 500 MB                      | PostgreSQL + PostGIS                     |
| **Bandwidth**      | 5 GB/mes                    | Transferencia de datos salientes         |
| **Storage**        | 2 GB                        | Imágenes, avatares, fotos                |
| **Auth Users**     | 50,000 MAU                  | Usuarios activos por mes                 |
| **Realtime**       | 200 conexiones concurrentes | WebSocket                                |
| **Edge Functions** | 500,000 invocations/mes     | ~16,600/día                              |
| **Compute**        | Shared CPU                  | Proyecto se pausa tras 1 semana inactivo |

### Límites Soft (CPU/Memoria)

Supabase Free Tier **NO tiene límites duros** en:

- Número de queries
- Lecturas/escrituras por segundo
- Número de tablas o filas

**Pero**: Hay límites de CPU compartida. Si consumes mucho CPU continuamente, el proyecto puede ser throttled.

## Cálculo de Uso Estimado

### 1. Database Storage (500 MB)

#### Tabla: `activities`

- **Tamaño por fila**: ~200 bytes
- **1,000 actividades**: 200 KB
- **10,000 actividades**: 2 MB

#### Tabla: `track_points`

- **Tamaño por punto**: ~50 bytes (lat, lon, elevation, timestamp, speed, bearing)
- **1 actividad (1 hora)**: 1,200 puntos × 50 bytes = **60 KB**
- **100 actividades completas**: 6 MB
- **1,000 actividades completas**: 60 MB
- **8,000 actividades completas**: 480 MB Cabe en 500 MB

**Conclusión**: Podemos almacenar ~8,000 actividades completas en el tier gratuito.

#### Otras tablas (profiles, routes, comments, etc.)

- **Estimado**: 20 MB para 1,000 usuarios con datos sociales

**Total estimado para prototipo**:

- 100 usuarios testeando
- 500 actividades totales
- 30 MB de track_points + 5 MB de metadata = **35 MB** Muy por debajo del límite

### 2. Bandwidth (5 GB/mes = ~166 MB/día)

#### Escenarios de Consumo

##### Descargar 1 actividad completa con track_points:

```
Query:
  - 1 fila de activities: 200 bytes
  - 1,200 filas de track_points: 60 KB
Total: ~60 KB por actividad
```

##### Feed con 20 actividades (solo metadata, sin track_points):

```
Query:
  - 20 filas de activities: 4 KB
  - 20 usuarios (profiles): 2 KB
Total: ~6 KB por carga de feed
```

##### Live tracking position (Broadcast):

```
Realtime Broadcast NO consume bandwidth de DB (ventaja clave)
Solo consume conexión Realtime (límite: 200 concurrentes)
```

#### Cálculos Diarios

**Uso ligero** (10 usuarios activos/día):

```
10 usuarios × 20 cargas de feed/día × 6 KB = 1.2 MB
10 usuarios × 2 actividades descargadas completas/día × 60 KB = 1.2 MB
Total: ~2.4 MB/día = 72 MB/mes Muy por debajo de 5 GB
```

**Uso intenso** (100 usuarios activos/día):

```
100 usuarios × 20 cargas de feed/día × 6 KB = 12 MB
100 usuarios × 5 actividades descargadas/día × 60 KB = 30 MB
Total: ~42 MB/día = 1.26 GB/mes Aún dentro del límite
```

**Uso extremo** (200 usuarios muy activos):

```
200 usuarios × 30 cargas de feed/día × 6 KB = 36 MB
200 usuarios × 8 actividades descargadas/día × 60 KB = 96 MB
Total: ~132 MB/día = 3.96 GB/mes Justo dentro del límite
```

**Conclusión**: El bandwidth NO será un problema en fase de prototipo. Solo sería problema con 500+ usuarios muy activos.

### 3. Storage (2 GB para imágenes)

#### Uso Estimado

##### Avatar de usuario:

- Tamaño optimizado: 50 KB (200×200 px, comprimido)
- 1,000 usuarios: 50 MB

##### Foto de actividad:

- Tamaño optimizado: 200 KB (1024×768 px, comprimido)
- 10 fotos por actividad (promedio alto)
- 500 actividades con fotos: 1 GB

**Conclusión**: 2 GB es suficiente para prototipo con cientos de usuarios.

### 4. Edge Functions (500,000 invocations/mes = ~16,600/día)

#### Funciones Previstas

1. **calculate-activity-stats** (al finalizar actividad):
   - 50 actividades/día = 50 invocations

2. **send-notification** (varias notificaciones):
   - 200 notificaciones/día = 200 invocations

3. **compress-track-points** (job diario):
   - 1 invocation/día

**Total estimado**: 251 invocations/día = **~7,500/mes** Muy por debajo de 500k

### 5. Realtime Connections (200 concurrentes)

#### Escenarios

##### Live tracking:

- 1 corredor compartiendo + 10 espectadores = 11 conexiones
- 10 corredores simultáneos con 10 espectadores c/u = 110 conexiones (suficiente)

##### Feed en tiempo real:

- 50 usuarios viendo feed simultáneamente = 50 conexiones

**Total concurrente en hora pico**: ~160 conexiones Dentro del límite

**Conclusión**: 200 conexiones concurrentes es suficiente para prototipo.

## Estrategias de Optimización

### 1. Compresión de Track Points (Douglas-Peucker)

El algoritmo Douglas-Peucker reduce el número de puntos en una polilínea manteniendo su forma.

```typescript
function simplifyTrackPoints(
  points: TrackPoint[],
  tolerance: number = 0.0001 // ~11 metros
): TrackPoint[] {
  // Implementación de Douglas-Peucker
  // Reduce 1,200 puntos a ~300 puntos sin perder forma visual
  return simplifiedPoints
}
```

**Beneficio**: Reducir 75% de storage sin afectar visualización en mapa.

**Aplicación**:

- Después de 6 meses de antigüedad
- O cuando DB supere 400 MB (80% del límite)

### 2. Lazy Loading de Track Points

```typescript
// NO cargar track_points por defecto
const { data: activity } = await supabase
  .from('activities')
  .select('id, title, distance_meters, duration_seconds, ...') // SIN track_points
  .eq('id', activityId)
  .single()

// Solo cargar cuando usuario hace click en "Ver ruta completa"
const { data: trackPoints } = await supabase
  .from('track_points')
  .select('*')
  .eq('activity_id', activityId)
  .order('timestamp')
```

**Beneficio**: Reducir bandwidth en 90% para vistas de lista.

### 3. Paginación Agresiva

```typescript
// Feed: Cargar solo 10 actividades por página (no 50)
const { data } = await supabase
  .from('activities')
  .select('*')
  .range(page * 10, (page + 1) * 10 - 1)
  .order('start_time', { ascending: false })
```

### 4. Cache en Cliente

**Web**: LocalStorage para datos no sensibles

```typescript
// Cachear rutas populares por 24h
const cachedRoutes = localStorage.getItem('routes_cache')
if (cachedRoutes && !isExpired(cachedRoutes.timestamp)) {
  return JSON.parse(cachedRoutes.data)
}
```

**Android**: Room Database

```kotlin
// Cachear actividades descargadas
@Entity
data class CachedActivity(
    @PrimaryKey val id: String,
    val data: String,  // JSON serializado
    val cachedAt: Long
)
```

### 5. Limpieza de Datos Viejos

**Job mensual** (puede ser manual en MVP):

```sql
-- Eliminar notificaciones mayores a 90 días
DELETE FROM notifications
WHERE created_at < NOW() - INTERVAL '90 days';

-- Comprimir track_points de actividades mayores a 6 meses
-- (implementar con Edge Function)
```

## Monitoreo de Uso

### Dashboard de Supabase

Revisar diariamente en `https://app.supabase.com/project/[PROJECT_ID]`:

1. **Database**:
   - Database size: Mantener <450 MB (90% del límite)
   - Tamaño por tabla: Identificar cuál crece más rápido

2. **Bandwidth**:
   - Daily egress: Mantener <150 MB/día
   - Tendencia mensual: Proyectar si se acerca a 5 GB

3. **API**:
   - Requests per second: Normal <10 rps en prototipo
   - Error rate: Mantener <1%

4. **Realtime**:
   - Peak concurrent connections: Mantener <180

### Alertas Recomendadas

Configurar alertas (email o Slack webhook) cuando:

- Database size > 450 MB (90%)
- Bandwidth mensual > 4.5 GB (90%)
- Realtime connections > 180 (90%)

## Plan de Contingencia

### Si se Acerca al Límite de Database (500 MB)

1. **Inmediato** (día 1-7):
   - Ejecutar compresión de track_points en actividades viejas
   - Eliminar notificaciones antiguas
   - Revisar si hay datos duplicados o spam

2. **Corto plazo** (día 7-30):
   - Implementar compresión automática en Edge Function
   - Advertir usuarios de actividades próximas a ser comprimidas
   - Ofrecer opción de "exportar datos" antes de comprimir

3. **Mediano plazo** (mes 1-3):
   - Evaluar upgrade a Supabase Pro ($25/mes) - 8 GB database
   - O migrar a self-hosted PostgreSQL

### Si se Acerca al Límite de Bandwidth (5 GB/mes)

1. **Inmediato**:
   - Activar lazy loading de track_points en toda la app
   - Reducir tamaño de páginas en feeds (10 items en lugar de 20)
   - Comprimir responses con gzip (Supabase lo hace automáticamente)

2. **Corto plazo**:
   - Implementar cache agresivo en cliente
   - Reducir frecuencia de polling/refreshes

3. **Mediano plazo**:
   - CDN para assets estáticos (Cloudflare gratis)
   - Supabase Pro: 50 GB bandwidth

### Si se Excede Inactividad (Proyecto se Pausa)

**Problema**: Supabase pausa proyectos tras 1 semana sin actividad.

**Solución**: Health check automático cada 6 días

```typescript
// GitHub Actions Workflow: .github/workflows/keep-alive.yml
name: Supabase Keep Alive
on:
  schedule:
    - cron: '0 0 */6 * *'  # Cada 6 días
jobs:
  ping:
    runs-on: ubuntu-latest
    steps:
      - name: Ping Supabase
        run: |
          curl -X GET "https://[PROJECT_ID].supabase.co/rest/v1/profiles?limit=1" \
            -H "apikey: ${{ secrets.SUPABASE_ANON_KEY }}"
```

## Estimación de Uso por Sesión de Testing

### Sesión Típica de Testing (1 usuario, 1 hora)

1. **Login**: 1 KB
2. **Cargar feed (3 veces)**: 18 KB
3. **Grabar 1 actividad**:
   - Metadata: 200 bytes
   - 1,200 track_points: 60 KB
   - Total: 60.2 KB
4. **Ver 3 actividades de otros**: 180 KB
5. **Dar 10 likes**: 1 KB
6. **Ver 1 ruta popular**: 40 KB
7. **Actualizar perfil**: 1 KB

**Total por sesión**: ~300 KB

**100 sesiones de testing**: 30 MB Muy manejable

### Sesión Intensiva (Stress Testing)

1. Crear 10 actividades consecutivas: 600 KB
2. Ver 50 actividades completas: 3 MB
3. Live tracking por 1 hora: 0 KB (Broadcast no usa bandwidth)
4. Subir 5 fotos: 1 MB

**Total**: ~4.6 MB por usuario en stress test

**20 usuarios en stress test**: 92 MB/día Aún dentro del límite diario

## Recomendaciones Finales

### Para Mantener en Free Tier el Mayor Tiempo Posible:

1. **Implementar lazy loading** de track_points desde día 1
2. **Usar Realtime Broadcast** para live tracking (no persiste)
3. **Comprimir imágenes** antes de subir (client-side)
4. **Paginar todo**: feeds, listas, comentarios
5. **Cachear datos** que no cambian frecuentemente
6. **Health check** cada 6 días para evitar pausa
7. **Monitorear uso** en dashboard semanalmente

### Cuándo Considerar Upgrade:

- **Database > 450 MB** de manera sostenida
- **Bandwidth > 4 GB/mes** (80% del límite)
- **100+ usuarios activos diarios** de manera consistente
- **Necesitas mejor performance** (CPU dedicado)

**Costo de Upgrade**: Supabase Pro $25/mes

- 8 GB database
- 50 GB bandwidth
- Conexiones dedicadas
- Backups diarios

## Proyección de Crecimiento

| Métrica             | Semana 1 | Mes 1  | Mes 3  | Mes 6  | Límite  |
| ------------------- | -------- | ------ | ------ | ------ | ------- |
| Usuarios totales    | 10       | 50     | 200    | 500    | 50k MAU |
| Actividades totales | 50       | 300    | 1,500  | 4,000  | ~8k     |
| Database size       | 5 MB     | 25 MB  | 120 MB | 320 MB | 500 MB  |
| Bandwidth/mes       | 100 MB   | 500 MB | 2 GB   | 4.5 GB | 5 GB    |
| Storage             | 50 MB    | 200 MB | 800 MB | 1.8 GB | 2 GB    |

**Conclusión**: El free tier es suficiente para **6 meses de crecimiento orgánico** con hasta 500 usuarios.

---

Ver [SYSTEM.md](SYSTEM.md) para arquitectura que implementa estas optimizaciones.
