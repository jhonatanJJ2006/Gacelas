# Arquitectura de Componentes - Gacelas

## Aplicación Web (React + TypeScript)

### Estructura de Carpetas

```
apps/web/src/
├── assets/          # Imágenes, iconos, map styles
├── components/      # Componentes reutilizables
│   ├── ui/         # Componentes UI básicos
│   ├── map/        # Componentes de mapas
│   ├── activity/   # Relacionados con actividades
│   └── social/     # Feed, perfiles, seguidores
├── features/        # Módulos por feature
│   ├── routes/
│   ├── community/
│   ├── events/
│   └── profile/
├── hooks/           # Custom hooks
├── lib/             # Utilidades, clientes
│   ├── supabase.ts
│   └── maps.ts
├── stores/          # State management (Zustand)
├── types/           # TypeScript types
├── pages/           # Páginas/rutas principales
└── App.tsx
```

### Componentes UI Base (`components/ui/`)

#### `Button.tsx`

```typescript
interface ButtonProps {
  variant: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger'
  size: 'small' | 'medium' | 'large'
  fullWidth?: boolean
  loading?: boolean
  disabled?: boolean
  icon?: React.ReactNode
  onClick?: () => void
  children: React.ReactNode
}
```

**Responsabilidad**: Botón reutilizable con estados de carga, variantes de estilo.

#### `Card.tsx`

```typescript
interface CardProps {
  padding?: 'none' | 'small' | 'medium' | 'large'
  hoverable?: boolean
  onClick?: () => void
  children: React.ReactNode
}
```

**Responsabilidad**: Contenedor con sombra y bordes redondeados.

#### `Avatar.tsx`

```typescript
interface AvatarProps {
  src?: string
  fallback: string // Iniciales
  size: 'small' | 'medium' | 'large'
  status?: 'online' | 'offline' | 'running'
}
```

**Responsabilidad**: Avatar de usuario con indicador de estado.

#### `Badge.tsx`

```typescript
interface BadgeProps {
  variant: 'default' | 'success' | 'warning' | 'danger'
  size: 'small' | 'medium'
  children: React.ReactNode
}
```

**Responsabilidad**: Badge para etiquetas, estados, notificaciones.

#### `Modal.tsx`

```typescript
interface ModalProps {
  isOpen: boolean
  onClose: () => void
  title?: string
  size?: 'small' | 'medium' | 'large' | 'fullscreen'
  children: React.ReactNode
}
```

**Responsabilidad**: Modal con backdrop, animación de entrada/salida.

### Componentes de Mapa (`components/map/`)

#### `MapView.tsx`

```typescript
interface MapViewProps {
  center: { lat: number; lng: number }
  zoom: number
  markers?: Marker[]
  polylines?: Polyline[]
  onMapClick?: (coords: { lat: number; lng: number }) => void
  darkMode?: boolean
}
```

**Responsabilidad**: Wrapper de Google Maps con estilos custom, gestión de controles.

**Hooks utilizados**:

- `useGoogleMaps()`: Inicializa Google Maps API
- `useMapMarkers()`: Gestiona marcadores dinámicos
- `useMapPolylines()`: Dibuja rutas

#### `LiveTrackerMap.tsx`

```typescript
interface LiveTrackerMapProps {
  activityId: string
  followedUserId: string
}
```

**Responsabilidad**: Mapa en tiempo real que escucha Supabase Broadcast y actualiza posición del corredor.

**Hooks utilizados**:

- `useLivePosition(activityId)`: Suscripción a Realtime Broadcast
- `useMapAnimation()`: Anima transición de marcador

#### `RouteDrawer.tsx`

```typescript
interface RouteDrawerProps {
  mode: 'view' | 'edit' | 'draw'
  points: RoutePoint[]
  onPointsChange?: (points: RoutePoint[]) => void
}
```

**Responsabilidad**: Permite dibujar/editar rutas en el mapa arrastrando.

### Componentes de Actividad (`components/activity/`)

#### `ActivityCard.tsx`

```typescript
interface ActivityCardProps {
  activity: Activity
  showActions?: boolean
  compact?: boolean
}
```

**Responsabilidad**: Tarjeta que muestra resumen de actividad (distancia, tiempo, mapa miniatura).

**Subcomponentes**:

- `ActivityStats`: Métricas (km, tiempo, pace)
- `ActivityMap`: Mapa miniatura de la ruta
- `ActivityActions`: Botones (like, comentar, compartir)

#### `ActivityDetail.tsx`

```typescript
interface ActivityDetailProps {
  activityId: string
}
```

**Responsabilidad**: Vista completa de actividad con mapa grande, gráficos, comentarios.

**Hooks utilizados**:

- `useActivity(id)`: Carga actividad y track points
- `useActivityComments(id)`: Carga comentarios

#### `ActivityRecorder.tsx`

```typescript
interface ActivityRecorderProps {
  onActivitySaved: (activity: Activity) => void
}
```

**Responsabilidad**: Pantalla principal durante la grabación de actividad.

**Estado interno**:

- Tiempo transcurrido
- Distancia acumulada
- Ritmo actual
- Estado de grabación (running, paused, stopped)

**Hooks utilizados**:

- `useGeolocation()`: Acceso a ubicación del navegador
- `useActivityRecording()`: Lógica de grabación y batch upload

#### `LiveShareControls.tsx`

```typescript
interface LiveShareControlsProps {
  activityId: string
  isSharing: boolean
  onToggleSharing: (enabled: boolean) => void
}
```

**Responsabilidad**: Controles para iniciar/detener compartición en vivo.

### Componentes Sociales (`components/social/`)

#### `FeedItem.tsx`

```typescript
interface FeedItemProps {
  post: Post
  onLike: () => void
  onComment: () => void
}
```

**Responsabilidad**: Item del feed (puede ser actividad, evento, post de club).

#### `UserProfile.tsx`

```typescript
interface UserProfileProps {
  userId: string
  isOwnProfile: boolean
}
```

**Responsabilidad**: Perfil de usuario con stats, actividades recientes, botón seguir/dejar de seguir.

**Hooks utilizados**:

- `useUser(userId)`: Datos del usuario
- `useUserStats(userId)`: Estadísticas agregadas
- `useFollowStatus(userId)`: Estado de seguimiento

#### `FollowersList.tsx`

```typescript
interface FollowersListProps {
  userId: string
  type: 'followers' | 'following'
}
```

**Responsabilidad**: Lista de seguidores o seguidos con estado online.

### Features (Módulos de Alto Nivel)

#### `features/routes/`

**Componentes principales**:

- `RoutesExplorer`: Mapa con rutas públicas, filtros
- `RouteDetail`: Detalle de ruta con reviews, fotos
- `RouteCreator`: Creador de rutas (desde track o dibujando)

**Hooks**:

- `useNearbyRoutes(location, radius)`: Rutas cercanas
- `useRoute(id)`: Detalle de ruta
- `useSaveRoute(activityId)`: Guardar actividad como ruta

#### `features/community/`

**Componentes principales**:

- `CommunityFeed`: Feed de actividades de seguidos
- `ClubList`: Lista de clubes
- `ClubDetail`: Página de club con miembros, eventos, feed

**Hooks**:

- `useFeed(filter)`: Feed con paginación
- `useClubs(location)`: Clubes cercanos

#### `features/events/`

**Componentes principales**:

- `EventList`: Lista de eventos próximos
- `EventDetail`: Detalle de evento con inscripción
- `EventLiveTracker`: Mapa en vivo durante evento con todos los participantes

**Hooks**:

- `useEvents(filters)`: Eventos con filtros
- `useEventParticipants(eventId)`: Participantes inscritos
- `useLiveEventPositions(eventId)`: Posiciones en vivo de todos

#### `features/profile/`

**Componentes principales**:

- `ProfileView`: Vista principal de perfil
- `StatsOverview`: Resumen de estadísticas (total km, actividades, racha)
- `ActivityHistory`: Historial de actividades con filtros
- `SettingsPanel`: Configuración de privacidad, notificaciones

### Hooks Personalizados (`hooks/`)

#### `useGeolocation()`

```typescript
interface UseGeolocationOptions {
  enableHighAccuracy: boolean
  timeout: number
  maximumAge: number
}

function useGeolocation(options?: UseGeolocationOptions) {
  const [position, setPosition] = useState<GeolocationPosition | null>(null)
  const [error, setError] = useState<GeolocationPositionError | null>(null)
  // ...
  return { position, error, refetch }
}
```

#### `useLivePosition(activityId: string)`

```typescript
function useLivePosition(activityId: string) {
  const [position, setPosition] = useState<LivePosition | null>(null)

  useEffect(() => {
    const channel = supabase.channel(`live:activity:${activityId}`)
    channel.on('broadcast', { event: 'position_update' }, (payload) => {
      setPosition(payload)
    })
    channel.subscribe()

    return () => {
      channel.unsubscribe()
    }
  }, [activityId])

  return position
}
```

#### `useActivityRecording()`

```typescript
function useActivityRecording() {
  const [isRecording, setIsRecording] = useState(false)
  const [isPaused, setIsPaused] = useState(false)
  const [trackPoints, setTrackPoints] = useState<TrackPoint[]>([])

  const startRecording = () => {
    /* ... */
  }
  const pauseRecording = () => {
    /* ... */
  }
  const resumeRecording = () => {
    /* ... */
  }
  const stopRecording = async () => {
    /* ... */
  }

  return {
    isRecording,
    isPaused,
    trackPoints,
    startRecording,
    pauseRecording,
    resumeRecording,
    stopRecording,
  }
}
```

### Stores (Zustand) (`stores/`)

#### `authStore.ts`

```typescript
interface AuthState {
  user: User | null
  profile: Profile | null
  isLoading: boolean
  signIn: (email: string, password: string) => Promise<void>
  signOut: () => Promise<void>
  updateProfile: (updates: Partial<Profile>) => Promise<void>
}

const useAuthStore = create<AuthState>((set) => ({/* ... */}))
```

#### `mapStore.ts`

```typescript
interface MapState {
  center: { lat: number; lng: number }
  zoom: number
  darkMode: boolean
  setCenter: (center: { lat: number; lng: number }) => void
  setZoom: (zoom: number) => void
  toggleDarkMode: () => void
}

const useMapStore = create<MapState>((set) => ({/* ... */}))
```

---

## Aplicación Android (Kotlin + Jetpack Compose)

### Estructura de Módulos

```
apps/android/app/src/main/java/com/gacelas/
├── core/                    # Lógica compartida
│   ├── data/               # Repositorios, data sources
│   ├── domain/             # Entities, use cases
│   ├── network/            # Supabase client, API
│   └── location/           # Location service
├── feature/                # Features por módulo
│   ├── routes/
│   ├── community/
│   ├── events/
│   ├── profile/
│   └── recording/
├── ui/                     # Componentes UI compartidos
│   ├── components/
│   ├── theme/
│   └── navigation/
└── GacelasApp.kt
```

### Core Modules

#### `core/location/`

##### `LocationService.kt`

```kotlin
@AndroidEntryPoint
class LocationService : Service() {
    @Inject lateinit var locationClient: FusedLocationProviderClient
    @Inject lateinit var repository: ActivityRepository

    private val pointsBuffer = mutableListOf<TrackPoint>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000  // 3 segundos
        ).build()

        locationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                handleNewLocation(location)
            }
        }
    }

    private fun handleNewLocation(location: Location) {
        val point = TrackPoint(
            lat = location.latitude,
            lon = location.longitude,
            elevation = location.altitude,
            timestamp = location.time,
            accuracy = location.accuracy,
            speed = location.speed,
            bearing = location.bearing
        )

        pointsBuffer.add(point)

        // Broadcast a espectadores (cada punto)
        broadcastLivePosition(point)

        // Flush a DB cada 50 puntos
        if (pointsBuffer.size >= 50) {
            flushPointsToDatabase()
        }
    }

    private suspend fun broadcastLivePosition(point: TrackPoint) {
        realtimeChannel?.broadcast(
            event = "position_update",
            payload = point
        )
    }

    private suspend fun flushPointsToDatabase() {
        repository.insertTrackPoints(currentActivityId, pointsBuffer)
        pointsBuffer.clear()
    }
}
```

**Responsabilidad**: Servicio en foreground para recolección de ubicación GPS con alta frecuencia.

##### `LocationManager.kt`

```kotlin
@Singleton
class LocationManager @Inject constructor(
    private val locationClient: FusedLocationProviderClient,
    private val context: Context
) {
    fun checkPermissions(): Boolean { /* ... */ }
    fun requestPermissions(activity: Activity) { /* ... */ }

    suspend fun getCurrentLocation(): Location? { /* ... */ }

    fun startTracking(activityId: String) {
        val intent = Intent(context, LocationService::class.java).apply {
            putExtra("ACTIVITY_ID", activityId)
        }
        context.startForegroundService(intent)
    }

    fun stopTracking() {
        context.stopService(Intent(context, LocationService::class.java))
    }
}
```

**Responsabilidad**: Gestión de permisos y ciclo de vida del Location Service.

#### `core/data/`

##### `SupabaseClient.kt`

```kotlin
@Singleton
class SupabaseClient @Inject constructor() {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
        install(Auth)
        install(Realtime)
        install(Storage)
    }
}
```

##### `ActivityRepository.kt`

```kotlin
@Singleton
class ActivityRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val localDatabase: GacelasDatabase
) {
    suspend fun createActivity(activity: Activity): String {
        return supabase.client.from("activities")
            .insert(activity)
            .decodeSingle<Activity>()
            .id
    }

    suspend fun insertTrackPoints(
        activityId: String,
        points: List<TrackPoint>
    ) {
        supabase.client.from("track_points").insert(points)
    }

    suspend fun getActivityWithTrackPoints(id: String): ActivityDetail {
        // Consulta con join o dos queries
    }

    fun observeActivitiesByUser(userId: String): Flow<List<Activity>> {
        return supabase.client.from("activities")
            .select()
            .eq("user_id", userId)
            .asFlow<List<Activity>>()
    }
}
```

##### `RoomDatabase.kt` (Caché offline)

```kotlin
@Database(
    entities = [
        ActivityEntity::class,
        TrackPointEntity::class,
        PendingSyncEntity::class
    ],
    version = 1
)
abstract class GacelasDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun syncDao(): SyncDao
}
```

**Responsabilidad**: Caché local para operaciones offline, sincronización posterior.

#### `core/domain/usecases/`

##### `StartActivityRecordingUseCase.kt`

```kotlin
@Singleton
class StartActivityRecordingUseCase @Inject constructor(
    private val repository: ActivityRepository,
    private val locationManager: LocationManager,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(title: String): Result<String> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val activity = Activity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = title,
                startTime = Instant.now(),
                isPublic = false
            )

            val activityId = repository.createActivity(activity)
            locationManager.startTracking(activityId)

            Result.success(activityId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

##### `StopActivityRecordingUseCase.kt`

```kotlin
@Singleton
class StopActivityRecordingUseCase @Inject constructor(
    private val repository: ActivityRepository,
    private val locationManager: LocationManager
) {
    suspend operator fun invoke(activityId: String): Result<ActivitySummary> {
        locationManager.stopTracking()

        // Calcular estadísticas finales
        val summary = repository.calculateActivityStats(activityId)

        // Actualizar actividad en DB
        repository.updateActivity(
            activityId,
            endTime = Instant.now(),
            distance = summary.distance,
            duration = summary.duration,
            avgPace = summary.avgPace
        )

        return Result.success(summary)
    }
}
```

### Feature Modules

#### `feature/recording/`

##### `RecordingScreen.kt`

```kotlin
@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel = hiltViewModel(),
    onFinish: (ActivitySummary) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            RecordingTopBar(
                elapsedTime = uiState.elapsedTime,
                onClose = { /* confirm dialog */ }
            )
        },
        floatingActionButton = {
            RecordingFab(
                isRecording = uiState.isRecording,
                isPaused = uiState.isPaused,
                onPauseResume = viewModel::togglePause,
                onStop = viewModel::stopRecording
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Mapa con ruta en vivo
            LiveTrackingMap(
                trackPoints = uiState.trackPoints,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Panel de métricas
            MetricsPanel(
                distance = uiState.distance,
                pace = uiState.currentPace,
                avgPace = uiState.avgPace,
                elevation = uiState.elevationGain
            )

            // Controles de compartir en vivo
            if (uiState.isRecording) {
                LiveSharingControls(
                    isSharing = uiState.isLiveSharing,
                    onToggle = viewModel::toggleLiveSharing
                )
            }
        }
    }
}
```

##### `RecordingViewModel.kt`

```kotlin
@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val startRecordingUseCase: StartActivityRecordingUseCase,
    private val stopRecordingUseCase: StopActivityRecordingUseCase,
    private val locationManager: LocationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    init {
        observeLocationUpdates()
        startElapsedTimeCounter()
    }

    fun startRecording(title: String) {
        viewModelScope.launch {
            startRecordingUseCase(title)
                .onSuccess { activityId ->
                    _uiState.update {
                        it.copy(
                            activityId = activityId,
                            isRecording = true
                        )
                    }
                }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            stopRecordingUseCase(uiState.value.activityId)
                .onSuccess { summary ->
                    // Navegar a pantalla de resumen
                }
        }
    }

    private fun observeLocationUpdates() {
        // Observar updates de Location Service
    }
}
```

#### `feature/routes/`

##### `RoutesExplorerScreen.kt`

```kotlin
@Composable
fun RoutesExplorerScreen(
    viewModel: RoutesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box {
        // Mapa de fondo con rutas
        MapView(
            routes = uiState.routes,
            onRouteClick = viewModel::selectRoute
        )

        // Panel inferior deslizable
        BottomSheet {
            LazyColumn {
                items(uiState.routes) { route ->
                    RouteCard(
                        route = route,
                        onClick = { viewModel.selectRoute(route.id) }
                    )
                }
            }
        }

        // Filtros flotantes
        FilterChips(
            filters = uiState.filters,
            onFilterChange = viewModel::updateFilters
        )
    }
}
```

### UI Components (`ui/components/`)

#### `MapView.kt`

```kotlin
@Composable
fun MapView(
    center: LatLng,
    zoom: Float = 15f,
    markers: List<MarkerData> = emptyList(),
    polylines: List<PolylineData> = emptyList(),
    onMapClick: (LatLng) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, zoom)
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        onMapClick = onMapClick,
        properties = MapProperties(
            mapType = MapType.NORMAL,
            isMyLocationEnabled = true
        )
    ) {
        markers.forEach { markerData ->
            Marker(
                state = MarkerState(position = markerData.position),
                title = markerData.title
            )
        }

        polylines.forEach { polylineData ->
            Polyline(
                points = polylineData.points,
                color = Color(0xFFD97706),
                width = 12f
            )
        }
    }
}
```

#### `StatsCard.kt`

```kotlin
@Composable
fun StatsCard(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### Navigation

#### `Navigation.kt`

```kotlin
@Composable
fun GacelasNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") { HomeScreen() }
        composable("routes") { RoutesExplorerScreen() }
        composable("community") { CommunityFeedScreen() }
        composable("events") { EventsListScreen() }
        composable("profile") { ProfileScreen() }
        composable("recording") { RecordingScreen() }
        composable("route/{routeId}") {
            RouteDetailScreen(routeId = it.arguments?.getString("routeId")!!)
        }
    }
}
```

## Resumen de Responsabilidades

### Web

- **UI/UX** principal para desktop y tablet
- Exploración de rutas en pantalla grande
- Gestión de perfil y configuración
- Visualización de estadísticas con gráficos

### Android

- **Grabación de actividades** con GPS de alta precisión
- Location Service en foreground
- Notificaciones locales
- Optimización de batería

### Backend (Supabase)

- **Almacenamiento persistente** de actividades y rutas
- **Streaming en tiempo real** vía Realtime Broadcast
- **Autenticación y autorización** con RLS
- **Consultas geoespaciales** con PostGIS

## Próximos Pasos

Ver [ROADMAP.md](ROADMAP.md) para plan de implementación por fases de cada componente.
