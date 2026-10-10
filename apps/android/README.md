# Gacelas Android

Aplicación nativa de Gacelas para Android, hecha con Kotlin, Jetpack Compose y Material 3. Tiene las mismas 23 pantallas que la app web (`apps/web`) y sigue el diseño exportado de Stitch y `docs/DESIGN.md`.

Por ahora la app funciona con datos de prueba (`data/mock`). La conexión con Supabase y Google Maps está pendiente.

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
GOOGLE_MAPS_API_KEY=tu-api-key
SUPABASE_URL=https://tu-proyecto.supabase.co
SUPABASE_ANON_KEY=tu-anon-key
```

Las claves se exponen como `BuildConfig.GOOGLE_MAPS_API_KEY`, `BuildConfig.SUPABASE_URL` y `BuildConfig.SUPABASE_ANON_KEY`, y la de Maps también como meta-data en el manifiesto. `local.properties` no se sube al repositorio.

## Comandos

| Comando                          | Qué hace                                              |
| -------------------------------- | ----------------------------------------------------- |
| `./gradlew assembleDebug`        | Genera `app/build/outputs/apk/debug/app-debug.apk`    |
| `./gradlew installDebug`         | Instala la app en el dispositivo o emulador conectado |
| `./gradlew testDebugUnitTest`    | Corre las pruebas unitarias (JVM y Robolectric)       |
| `./gradlew lintDebug`            | Corre Android Lint                                    |
| `./gradlew recordRoborazziDebug` | Genera capturas PNG de las 23 pantallas               |

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

## Mapas

Las pantallas de Rutas usan `MapaPlaceholder`, un mapa estático dibujado con Canvas que copia la maqueta SVG de la web. Queda pendiente reemplazarlo por Maps Compose con `GOOGLE_MAPS_API_KEY` y el estilo oscuro de `docs/DESIGN.md` (ver los TODO en el código).

## Pruebas

- `DestinosTest`: hay 23 rutas únicas y coinciden con la web.
- `NavGraphTest` (Robolectric): el grafo de navegación contiene todas las rutas y "Saltar" lleva a Inicio.
- `CarreraViewModelTest`: tiempo, distancia, ritmo, pausa y formatos.
- `MockDataTest`: datos de prueba y filtros de notificaciones.
- `ScreenshotTest` (Roborazzi): una captura por pantalla; solo graba con `recordRoborazziDebug`.

El workflow de Android está en `apps/android/ci/android.yml`. Hay que moverlo a `.github/workflows/android.yml`, porque el token que subió este cambio no tiene permiso `workflow`. Ese workflow compila el APK, corre las pruebas y el lint en cada PR que toque `apps/android`, y publica el APK como artefacto.

## Pendiente

- Conectar Supabase (autenticación, actividades, rutas, eventos) y reemplazar `data/mock`.
- Google Maps real en Rutas, Buscador y Navegación.
- GPS real: implementar `LocationService` y pedir el permiso de ubicación en tiempo de ejecución.
- Probar el 3D en dispositivos reales y ajustar la cámara de cada pantalla.
