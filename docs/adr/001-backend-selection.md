# ADR 001: Selección de Backend y Base de Datos

**Fecha**: 2026-10-07  
**Estado**: Aceptado  
**Decisores**: Jhonatan Jara, Equipo Gacelas  
**Contexto técnico**: Aplicación de running con seguimiento GPS en tiempo real

## Contexto

Gacelas requiere un backend que soporte:

1. **Consultas geoespaciales**: Búsqueda de rutas por proximidad, cálculo de distancias
2. **Escrituras de alta frecuencia**: Puntos GPS cada 3-5 segundos durante actividades
3. **Streaming en tiempo real**: Compartir ubicación en vivo con latencia <1s
4. **SDK para Kotlin**: Integración nativa con Android
5. **Tier gratuito generoso**: Prototipo y testing extensivo sin costos
6. **Escalabilidad**: Camino claro para crecer sin rediseñar

## Opciones Evaluadas

### Opción 1: Firebase (Firestore + Realtime Database)

**Pros:**
- Ecosistema maduro con excelente documentación
- SDK oficial para Kotlin/Android muy completo
- Firebase Realtime Database ideal para streaming (WebSockets)
- Auth, Analytics, Crashlytics integrados
- Hosting gratuito para web

**Contras:**
- **Firestore NO tiene soporte nativo para consultas geoespaciales**: requiere GeoFirestore (librería de terceros)
- **Spark Plan muy limitado**:
  - Firestore: 50k lecturas/día, 20k escrituras/día, 1GB almacenamiento
  - Realtime DB: 100 conexiones simultáneas, 1GB almacenamiento, 10GB descarga/mes
- Una actividad de 1 hora = ~1,200 escrituras → solo 16 actividades/día en free tier
- Costos escalan rápido: $0.06 por 100k lecturas, $0.18 por 100k escrituras
- Vendor lock-in: migrar fuera de Firebase es muy costoso

**Cálculos para tier gratuito:**
- 10 usuarios testeando 1 actividad/día de 1 hora = 12,000 escrituras/día → **excede límite**
- Live tracking con 5 espectadores = +30 lecturas/segundo → límite en minutos

### Opción 2: Convex

**Pros:**
- Backend moderno con funciones reactivas
- Real-time por defecto, muy fácil de usar
- TypeScript en backend y frontend
- Tier gratuito: 1 millón de llamadas a funciones/mes
- Excelente DX (Developer Experience)

**Contras:**
- **Sin soporte nativo para geoespacial**: no hay equivalente a PostGIS
- **Sin SDK oficial para Kotlin**: requiere cliente HTTP custom o usar JavaScript en WebView
- Relativamente nuevo, menos maduro que alternativas
- Comunidad pequeña, menos recursos para troubleshooting
- 1M llamadas/mes = ~33k/día → ~16 actividades de 1 hora con 1,200 puntos c/u

**Cálculos:**
- Geoespacial requeriría calcular distancias en aplicación (ineficiente)
- Android requiere wrapper HTTP custom, perdiendo ventajas de reactivity

### Opción 3: Supabase (PostgreSQL + PostGIS + Realtime) ✅

**Pros:**
- **PostGIS nativo**: consultas geoespaciales optimizadas (ST_Distance, ST_DWithin, índices espaciales)
- **PostgreSQL**: base de datos relacional robusta, madura, sin vendor lock-in
- **Supabase Realtime**: 
  - Broadcast channels para streaming efímero (no persiste en DB)
  - Presence para "quién está online"
  - Postgres Changes para reactividad en datos persistidos
- **SDK Kotlin oficial**: `io.github.jan-tennert.supabase:postgrest-kt`
- **Tier gratuito generoso**:
  - 500 MB base de datos
  - 5 GB bandwidth/mes
  - 2 GB almacenamiento de archivos
  - 50,000 usuarios MAU
  - **Sin límite duro en lecturas/escrituras** (solo CPU)
  - 200 conexiones concurrentes
- **Row Level Security (RLS)**: seguridad granular a nivel de fila
- Auth con providers sociales incluida
- Edge Functions (Deno) para lógica serverless
- Fácil self-hosting si se necesita en el futuro

**Contras:**
- Curva de aprendizaje SQL/PostgreSQL (vs NoSQL)
- Realtime Broadcast requiere diseño cuidadoso para no sobrecargar
- Free tier tiene timeout de inactividad (proyecto pausa después de 1 semana sin uso)
- Menos integración "out-of-the-box" que Firebase (pero más flexible)

**Cálculos para tier gratuito:**
- **Actividades persistidas**: 1 actividad = 1,200 puntos × 50 bytes = 60 KB
  - 500 MB / 60 KB = **~8,300 actividades** almacenables
- **Bandwidth**: 5 GB/mes = 166 MB/día
  - 1 actividad descargada = 60 KB → **~2,700 descargas/día**
- **Live tracking con Broadcast**: ¡Sin cargo de bandwidth! Los mensajes no se persisten
  - Solo consume CPU/conexiones (200 concurrentes = suficiente para prototipo)
- **Rutas con PostGIS**: Búsqueda por radio `ST_DWithin(location, ST_MakePoint(lon, lat), radius)`
  - Con índice GIST: consultas en <10ms incluso con 10k rutas

### Opción 4: Supabase + Firebase (Híbrido)

**Análisis:**
- Usar Supabase para datos + Firebase Realtime para streaming
- **Rechazado**: Complejidad innecesaria, mantener dos backends
- Supabase Realtime Broadcast es suficiente para el caso de uso

## Decisión

**Seleccionamos Supabase** como backend principal por las siguientes razones:

1. **Geoespacial nativo**: PostGIS resuelve el problema de rutas sin hacks
2. **Tier gratuito óptimo para prototipo**: Límites en bandwidth/storage, NO en operaciones
3. **Arquitectura de streaming eficiente**:
   - Broadcast para posiciones en vivo (efímero, sin persistir)
   - Postgres para track points (persistido, optimizado)
   - Separación clara entre "temporal" y "permanente"
4. **Kotlin SDK oficial**: Primera clase, no wrapper manual
5. **Sin vendor lock-in**: PostgreSQL es estándar, fácil migrar
6. **RLS para privacidad**: Reglas de seguridad a nivel de base de datos
7. **Headroom para testing**: Podemos hacer 100+ actividades de prueba sin preocupaciones

## Consecuencias

### Positivas
- Modelo de datos relacional robusto con integridad referencial
- Consultas geoespaciales optimizadas desde día 1
- Broadcast channels permiten streaming sin consumir cuota de escritura
- Flexibilidad para optimizar consultas SQL conforme crecemos
- Camino claro a self-hosting si se requiere

### Negativas
- Equipo debe aprender SQL y conceptos PostgreSQL
- Realtime requiere manejo de reconexión en apps (no automático como Firebase)
- Proyecto se pausa tras 1 semana de inactividad (debe "despertar" con un ping)

### Mitigaciones
- Documentar queries geoespaciales comunes
- Implementar librería de reconexión automática en apps
- Configurar health check cada 6 días para evitar pausa (simple cron job)

## Arquitectura de Streaming con Supabase

### Para Live Tracking (Efímero):
```typescript
// Cliente envía posición cada 5s vía Broadcast
const channel = supabase.channel('live:activity:123')
await channel.subscribe()
await channel.send({
  type: 'broadcast',
  event: 'position',
  payload: { lat, lon, speed, timestamp }
})

// Espectadores escuchan sin persistir en DB
channel.on('broadcast', { event: 'position' }, (payload) => {
  updateMapMarker(payload)
})
```

### Para Track Points (Persistido):
```typescript
// Cada 30 segundos o al terminar, batch insert
await supabase.from('track_points').insert([
  { activity_id, lat, lon, elevation, timestamp, accuracy },
  // ... más puntos
])
```

**Resultado**: Live tracking usa Broadcast (gratis), track completo usa inserts (optimizado).

## Referencias

- [Supabase Realtime Docs](https://supabase.com/docs/guides/realtime)
- [PostGIS Documentation](https://postgis.net/documentation/)
- [Supabase Kotlin SDK](https://github.com/supabase-community/supabase-kt)
- [Firebase Pricing](https://firebase.google.com/pricing)
- [Convex Pricing](https://www.convex.dev/pricing)

## Notas de Implementación

Ver [SYSTEM.md](../SYSTEM.md) para diagramas detallados de la arquitectura de streaming.
