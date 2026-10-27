# Runcity Android

Aplicación nativa de Runcity (codigo y paquete conservan el nombre Gacelas) para Android, hecha con Kotlin, Jetpack Compose y Material 3. Tiene las mismas 23 pantallas que la app web (`apps/web`) y sigue el diseño exportado de Stitch y `docs/DESIGN.md`.

Por ahora la app funciona con datos de prueba (`data/mock`). El mapa usa MapLibre con teselas de OpenStreetMap y funciona sin red en las zonas descargadas.

## Requisitos

- JDK 17
- Android SDK 35 (compileSdk y targetSdk 35, minSdk 24)
- Git LFS, porque el modelo 3D se guarda en LFS (`git lfs pull`)
- Android Studio Ladybug o superior (opcional)

## Configuración

```bash
cd apps/android
cp local.properties.example local.properties
```

Edita `local.properties` y agrega `sdk.dir` si no usas Android Studio:

```properties
sdk.dir=/ruta/a/android-sdk
SUPABASE_URL=https://tu-proyecto.supabase.co
SUPABASE_ANON_KEY=tu-anon-key
```

Las claves se exponen como `BuildConfig.SUPABASE_URL` y `BuildConfig.SUPABASE_ANON_KEY`. El mapa no necesita clave. `local.properties` no se sube al repositorio.

## Comandos

| Comando                                                                 | Qué hace                                              |
| ----------------------------------------------------------------------- | ----------------------------------------------------- |
| `./gradlew assembleDebug`                                               | Genera `app/build/outputs/apk/debug/app-debug.apk`    |
| `./gradlew installDebug`                                                | Instala la app en el dispositivo o emulador conectado |
| `./gradlew testDebugUnitTest`                                           | Corre las pruebas unitarias (JVM y Robolectric)       |
| `./gradlew lintDebug`                                                   | Corre Android Lint                                    |
| `./gradlew assembleDebug -Pgacelas.abi=arm64-v8a -Pgacelas.minify=true` | APK debug solo arm64 con R8 (unos 18 MB)              |
| `./gradlew recordRoborazziDebug`                                        | Genera capturas PNG de las 23 pantallas               |

Para guardar las capturas en otra carpeta:

```bash
./gradlew recordRoborazziDebug --tests '*ScreenshotTest*' -Pgacelas.shots=/ruta/capturas
```

## Estructura

```text
app/src/main/
  assets/models/gacelas-runner-animated.glb   Corredor 3D con clips Idle, Jog y Run (Git LFS)
  res/font/                                   Inter y Roboto Flex (fuentes variables, OFL)
  java/com/gacelas/
    MainActivity.kt
    navigation/       Destinos (rutas) y GacelasNavHost (Navigation Compose)
    ui/theme/         Colores, tipografía y tema oscuro (tokens de tailwind.config.js)
    ui/components/    Barra superior, barra inferior con botón central Correr, tarjetas, campos, mapa
    ui/three/         Runner3D (SceneView/Filament) y su imagen de reemplazo
    data/mock/        Datos de prueba tipados, equivalentes a apps/web/src/mocks/data.ts
    acceso/           Bienvenida, inicio de sesión, registro, recuperar contraseña
    inicio/           Inicio
    correr/           Correr, carrera en curso (CarreraViewModel), cronómetro, resumen
    rutas/            Lista de rutas, buscador, navegación 3D
    comunidad/        Feed, eventos, detalle de evento
    perfil/           Perfil, editar perfil, avatar 3D
    ajustes/          Ajustes, privacidad, configuración de notificaciones, permisos de ubicación
    notificaciones/   Bandeja de notificaciones
    core/location/    LocationService (servicio en primer plano, pendiente de implementar)
```

Las pantallas de acceso quedan fuera del scaffold principal. Inicio, Rutas, Comunidad, Eventos y Perfil se muestran con la barra superior y la barra inferior.

## Pantallas y rutas

Las rutas son las mismas de la web (`apps/web/src/App.tsx`).

| Sección        | Pantalla                  | Ruta                     | Composable                          | Barra inferior |
| -------------- | ------------------------- | ------------------------ | ----------------------------------- | -------------- |
| Acceso         | Bienvenida                | `bienvenida`             | `BienvenidaScreen`                  | No             |
| Acceso         | Inicio de sesión          | `inicio-sesion`          | `InicioSesionScreen`                | No             |
| Acceso         | Registro                  | `registro`               | `RegistroScreen`                    | No             |
| Acceso         | Recuperar contraseña      | `recuperar-contrasena`   | `RecuperarContrasenaScreen`         | No             |
| Acceso         | Permisos de ubicación     | `permisos-ubicacion`     | `PermisosUbicacionScreen`           | No             |
| Inicio         | Inicio                    | `inicio`                 | `InicioScreen`                      | Sí             |
| Rutas          | Lista de rutas            | `rutas`                  | `ListaRutasScreen`                  | Sí             |
| Comunidad      | Feed                      | `comunidad`              | `FeedScreen`                        | Sí             |
| Comunidad      | Eventos                   | `comunidad/eventos`      | `EventosScreen`                     | Sí             |
| Perfil         | Perfil                    | `perfil`                 | `PerfilScreen`                      | Sí             |
| Correr         | Preparar carrera          | `correr`                 | `CorrerScreen`                      | No             |
| Correr         | Carrera en curso          | `correr/en-curso`        | `CarreraEnCursoScreen`              | No             |
| Correr         | Cronómetro                | `correr/cronometro`      | `CronometroScreen`                  | No             |
| Correr         | Resumen post-carrera      | `correr/resumen`         | `ResumenPostCarreraScreen`          | No             |
| Rutas          | Buscador de rutas         | `rutas/buscar`           | `BuscadorRutasScreen`               | No             |
| Rutas          | Navegación 3D             | `rutas/navegacion`       | `NavegacionRutaScreen`              | No             |
| Comunidad      | Detalle de evento         | `comunidad/eventos/{id}` | `DetalleEventoScreen`               | No             |
| Perfil         | Editar perfil             | `perfil/editar`          | `EditarPerfilScreen`                | No             |
| Perfil         | Avatar 3D                 | `perfil/avatar`          | `Avatar3DScreen`                    | No             |
| Ajustes        | Ajustes y preferencias    | `ajustes`                | `AjustesPreferenciasScreen`         | No             |
| Ajustes        | Privacidad y ubicación    | `ajustes/privacidad`     | `PrivacidadUbicacionScreen`         | No             |
| Ajustes        | Notificaciones (ajustes)  | `ajustes/notificaciones` | `ConfiguracionNotificacionesScreen` | No             |
| Notificaciones | Bandeja de notificaciones | `notificaciones`         | `NotificacionesScreen`              | No             |

## Cómo funciona el 3D

- El modelo es una copia de `apps/web/public/models/gacelas-runner-animated.glb` en `app/src/main/assets/models/`. Si cambias el modelo de la web, copia también este archivo.
- `ui/three/Runner3D.kt` usa SceneView para Compose (`io.github.sceneview:sceneview`), que renderiza con Filament.
- El modelo se carga con `autoAnimate = false` y se reproduce un solo clip con `playAnimation`: `Idle` en Inicio y en Avatar 3D (con giro automático), y `Jog` o `Run` en Navegación 3D (se cambia con los botones Trote y Carrera).
- Si el dispositivo no tiene OpenGL ES 3.0, si la app corre en un preview o en Robolectric, o si el GLB no carga, se muestra `Runner3DFallback`, una imagen de reemplazo con el ícono del corredor. Así la pantalla nunca queda vacía.
- El GLB no se comprime dentro del APK (`androidResources.noCompress += "glb"`).

## Funcionalidades

- Acceso: registro, inicio de sesión y recuperación con validación (correo, contraseña de 8 caracteres con letras y números). Es una autenticación local detrás de `AuthRepository` (`data/auth`): las cuentas se guardan en DataStore con la contraseña en SHA-256 y la sesión persiste al cerrar la app. Sin sesión, cualquier ruta protegida vuelve a Bienvenida.
- GPS real: `PermisosUbicacionScreen` pide ubicación y notificaciones en tiempo de ejecución. Al empezar una carrera arranca `LocationService`, un servicio en primer plano con notificación que recibe puntos de `FusedLocationProviderClient` y los pasa a `RunTracker`.
- `RunTracker` (`tracking/`) calcula distancia con Haversine, tiempo sin contar pausas, ritmo actual (ventana de 30 s), ritmo promedio, desnivel y parciales por kilómetro. Descarta puntos con precisión peor que 35 m, saltos menores a 2 m y velocidades mayores a 12 m/s.
- Las carreras se guardan en DataStore (`data/carreras`): resumen post-carrera con título, nota y visibilidad, historial en Perfil, semana atlética y racha en Inicio.
- Cronómetro: Splits con los parciales reales, Intervalos (trabajo, descanso y rondas configurables, con vibración y sonido) y Metrónomo (BPM con tick audible). La configuración se guarda.
- Rutas: pestañas Explorar, Mis rutas y Guardadas, búsqueda, y guardar o quitar rutas (persistente).
- Comunidad: feed con tus carreras públicas, likes, comentarios, compartir y ocultar; eventos con pestañas, inscripción y creación de eventos.
- Ajustes: modo oscuro o claro, unidades métricas o imperiales, visibilidad del perfil, ubicación precisa, protección de hogar y cada tipo de notificación. Todo persiste en DataStore (`data/ajustes`).
- Perfil: editar nombre, usuario, bio, edad, género y foto (selector de fotos del sistema). El avatar 3D guarda su configuración.

## Mapas

Los mapas usan MapLibre (`ui/components/MapaLibre.kt` y `MapaGacelas.kt`) con teselas vectoriales de OpenStreetMap servidas por OpenFreeMap, sin clave. Los estilos Gacelas oscuro y claro están en `app/src/main/assets/mapas/`.

- Sin conexión: en Ajustes, Mapa, "Descargar" baja la zona de Loja (zoom 10 a 14, unos pocos MB) con `OfflineManager`. MapLibre usa esas teselas sin red y las amplía para los zooms de navegación.
- Vista 3D: la cámara se inclina 60 grados con el rumbo hacia arriba. El zoom depende de la velocidad (`ZoomNavegacion`): 19,5 hasta 8 km/h y baja a 18,3 a partir de 12 km/h, para que a 5-8 km/h el suelo avance al menos 12 dp por segundo.
- En las pruebas JVM (Robolectric) no hay código nativo, así que se muestra `MapaPlaceholder`, el mapa dibujado con Canvas.

## Actualizaciones y publicar una versión

La app revisa al abrir (como máximo una vez al día) `android/latest.json` en el bucket público `app-releases` de Supabase Storage. Si `versionCode` es mayor que el instalado, muestra "Hay una nueva versión" con las novedades, "Actualizar" (descarga con DownloadManager, verifica SHA-256 e instala con FileProvider, pidiendo permiso para instalar apps desconocidas si hace falta) y "Más tarde" (no vuelve a avisar de esa versión). Con `minVersionCode` la actualización es obligatoria. El repo de GitHub es privado: la app no lee GitHub ni lleva tokens.

`versionCode` se calcula desde `versionName` (0.6.1 -> 601, 1.2.3 -> 10203), así que basta con subir `versionName`.

Para publicar:

1. Sube `versionName` en `app/build.gradle.kts`, haz commit y push a `main`.
2. Comprueba que `local.properties` tenga `SUPABASE_URL`, `SUPABASE_ANON_KEY` y `GOOGLE_WEB_CLIENT_ID`, y que el keystore de depuración sea el de SHA-1 23:CF:...:F9.
3. `SUPABASE_SERVICE_ROLE_KEY=... scripts/publicar-android.sh "Notas cortas"` (agrega `--obligatoria` para forzarla). Compila, prueba, sube el APK y `latest.json`, crea el tag y el pre-release de GitHub.

El bucket se crea con `backend/migrations/013_app_releases.sql`.

## Pruebas

- `DestinosTest`: hay 23 rutas únicas y coinciden con la web.
- `NavGraphTest` (Robolectric): el grafo contiene todas las rutas, "Saltar" lleva a Inicio de sesión, las rutas protegidas exigen sesión y Ajustes abre el diálogo de cerrar sesión.
- `LogicaTest.kt`: `GeoTest` (Haversine, formatos, unidades), `RunTrackerTest` (distancia, filtros de GPS, pausa, parciales, desnivel, guardado), `PlanIntervalosTest`, `ValidacionAuthTest`, `FiltrosTest` (rutas y eventos) y `EstadisticasTest` (semana, racha y mes).
- `RepositoriosTest`: auth, carreras, rutas, ajustes y eventos sobre un DataStore real en una carpeta temporal.
- `MockDataTest`: datos de prueba y filtros de notificaciones.
- `ScreenshotTest` (Roborazzi): una captura por pantalla; solo graba con `recordRoborazziDebug`.

El workflow de Android está en `.github/workflows/android.yml`. Ese workflow compila el APK, corre las pruebas y el lint en cada PR que toque `apps/android`, y publica el APK como artefacto.

## Pendiente

- Conectar Supabase: basta con nuevas implementaciones de `AuthRepository`, `CarrerasRepository`, `RutasRepository`, `EventosRepository` y `AjustesRepository` en `Contenedor` (`GacelasApp.kt`).
- Probar el 3D en dispositivos reales y ajustar la cámara de cada pantalla.
