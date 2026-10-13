# Arquitectura móvil (KMP, Android, iOS)

Volver a [ARQUITECTURA.md](../ARQUITECTURA.md).

## Módulos

```mermaid
graph TD
    APP[apps/android/app<br/>Android: Activity, servicios, DI] --> SH
    IOS[apps/ios/iosApp<br/>SwiftUI wrapper] -->|framework Shared| SH
    subgraph SH[apps/android/shared - Kotlin Multiplatform]
        CM[commonMain] --> EXP[expect]
        AM[androidMain - actual]
        IM[iosMain - actual]
    end
    SH --> SUPA[supabase-kt: auth, postgrest, storage<br/>ktor okhttp / darwin]
```

- `shared`: lógica, ViewModels y la mayor parte de la UI en Compose Multiplatform. Paquete raíz `com.gacelas`.
- `app` (Android): punto de entrada (`GacelasApp`, `MainActivity`), servicios de tracking y notificaciones, pantallas que aún no se movieron a `shared`.
- `iosApp`: `iOSApp.swift` aloja `MainViewController` de `shared`; `MapaMapLibre.swift` y `AccesoNativo.swift` aportan integración nativa (mapa, inicio de sesión).

## Capas por bounded context (objetivo)

Cada contexto (`carreras`, `rutas`, `cuenta`, `comunidad`) en `shared/src/commonMain/kotlin/com/gacelas/<contexto>/`:

```
<contexto>/
  domain/
    modelo/        entidades y value objects (data class inmutables)
    reglas/        lógica pura: cálculos, validaciones, políticas
    repositorio/   interfaces (puertos) que data implementa
    casosuso/      un caso de uso por acción (clase con operator fun invoke)
  data/
    Supabase<X>Repository.kt   implementación remota
    Local<X>Repository.kt      caché / offline / fake
    dto/                       DTO @Serializable y mapeadores toDomain()/toDto()
  presentation/
    <Pantalla>ViewModel.kt     estado (StateFlow<UiState>) + eventos
    ui/                        Composables comunes del contexto
```

```mermaid
graph LR
    UI[presentation/ui<br/>Composables] --> VM[presentation<br/>ViewModel]
    VM --> UC[domain/casosuso]
    UC --> R[domain/reglas + modelo]
    UC --> P[domain/repositorio<br/>interfaces]
    D[data<br/>Supabase / Local] -.implementa.-> P
    D --> R
    DI[app / iOS: composición] --> D
    DI --> VM
```

### Reglas de dependencias

| Capa                         | Puede depender de                                                                                         | No puede depender de                                                   |
| ---------------------------- | --------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| `domain`                     | Kotlin estándar, `kotlinx.coroutines` (Flow), `kotlinx.datetime`, `com.gacelas.shared` (utilidades puras) | `data`, `presentation`, Compose, supabase-kt, ktor, APIs de plataforma |
| `data`                       | `domain` del mismo contexto, supabase-kt, ktor, serialización, `expect` de plataforma                     | `presentation`, Compose                                                |
| `presentation`               | `domain` del mismo contexto, lifecycle-viewmodel, Compose                                                 | `data` (recibe repositorios por constructor), supabase-kt              |
| `androidMain` / `iosMain`    | cualquier capa de `commonMain`                                                                            | otros source sets                                                      |
| `apps/android/app`, `iosApp` | `shared` completo (punto de composición)                                                                  | contener reglas de negocio                                             |

- La inyección es manual por constructor. El ensamblado (crear `Supabase<X>Repository` y pasarlo a los ViewModels) ocurre en el punto de entrada de cada plataforma. Si crece, evaluar Koin en un ADR.
- Los ViewModels exponen `StateFlow<UiState>` inmutable y reciben acciones como funciones; la UI no llama a repositorios.

### expect/actual

Solo para capacidades de plataforma: sensores (`core/sensores`), fuentes (`ui/theme/Fuentes`), botón atrás (`correr/Atras`), formato decimal (`rutas/Decimal`), imágenes de acceso, mapa (`mapa`, con puente a MapLibre en iOS), utilidades de plataforma (`shared/Plataforma`, `shared/Comun`).

- Archivo `X.kt` en `commonMain` con `expect`; `X.android.kt` y `X.ios.kt` con `actual`.
- Preferir una interfaz en `domain`/`data` implementada por plataforma cuando la capacidad necesita estado o ciclo de vida; usar `expect fun` solo para funciones simples.
- Las vistas nativas iOS (MapLibre) se exponen a Compose mediante un puente (`PuenteMapa.kt`) que el lado Swift registra al arrancar.

## Convenciones de nombres

- Código y carpetas en español, salvo términos técnicos fijados por el patrón: `domain`, `data`, `presentation`, `Repository`, `ViewModel`.
- Paquetes en minúsculas sin guiones: `com.gacelas.rutas.domain.casosuso`.
- Repositorios: interfaz `RutasRepository`, implementaciones `SupabaseRutasRepository`, `LocalRutasRepository`.
- Casos de uso: verbo + objeto (`ObtenerCarrera`, `EliminarCarrera`, `MarcarVuelta`), un archivo por caso de uso o agrupados en `CasosUso<Contexto>.kt` si son triviales.
- ViewModels: `<Pantalla>ViewModel`; estado `<Pantalla>UiState`.
- Composables de pantalla: `<Pantalla>Contenido` (sin estado, recibe UiState y lambdas).
- Tests en `commonTest/kotlin/com/gacelas/<contexto>/` con el mismo paquete que el código probado; `domain` se prueba sin fakes de red.

## Estado actual

| Contexto    | domain                                                              | data                                                         | presentation                                   | Comentario                                                            |
| ----------- | ------------------------------------------------------------------- | ------------------------------------------------------------ | ---------------------------------------------- | --------------------------------------------------------------------- |
| `carreras`  | Sí (`modelo`, `reglas`, `repositorio`, `casosuso`, `SesionCarrera`) | Sí (`Supabase`/`Local`)                                      | Sí (`ViewModelsCarreras.kt`)                   | Migrado (PR de Clean Architecture 1-2)                                |
| `rutas`     | Sí                                                                  | Sí (incluye offline, trazos, elevación)                      | Sí (`ViewModelsRutas.kt`)                      | Migrado (#60); `ListaRutasContenido.kt` sigue en la raíz del contexto |
| `cuenta`    | No                                                                  | En `data/auth`, `data/ajustes`, `data/notificaciones`        | En `acceso/`, `ajustes/`, `perfil/`            | Pendiente                                                             |
| `comunidad` | No                                                                  | En `data/clubes` (`Fase5.kt`), `data/eventos`, `data/envivo` | En `apps/android/app/.../comunidad` y `envivo` | Pendiente                                                             |

Otras carpetas de `commonMain` hoy: `correr`, `tracking`, `mapa`, `inicio`, `navigation`, `ui` (tema, componentes, 3D), `core/sensores`, `actualizacion`, `data/remoto` (cliente Supabase compartido), `data/mock`.

Desviaciones detectadas:

- `carreras/domain/SesionCarrera.kt` importa `com.gacelas.tracking.formatearHms` (formato de presentación) desde `domain`. Mover el formateo a `presentation` o la función a `shared`.
- `data/clubes/Fase5.kt` y `data/mock` usan nombres por fase/temporales en lugar de por contexto.
- Varias pantallas de `apps/android/app` (`comunidad`, `envivo`, `rutas/DetalleRuta.kt`, `perfil/DetalleCarrera.kt`) tienen ViewModels y lógica propia de Android que iOS no comparte.

## Qué falta para el objetivo

1. Contexto `cuenta`: crear `cuenta/{domain,data,presentation}`; mover `data/auth`, `data/ajustes`, `data/notificaciones` a `cuenta/data` y `acceso`, `ajustes`, `perfil` a `cuenta/presentation/ui`.
2. Contexto `comunidad`: idem con `data/clubes`, `data/eventos`, `data/envivo` y las pantallas de `apps/android/app/.../comunidad`.
3. Mover `ListaRutasContenido.kt` a `rutas/presentation/ui` y las pantallas de carrera (`correr`) a `carreras/presentation/ui`.
4. Corregir la dependencia `domain -> tracking` de `SesionCarrera`.
5. Reducir `apps/android/app` a composición, servicios Android y permisos; mover pantallas a `shared` para que iOS las reciba.
6. Añadir una comprobación automática de capas (por ejemplo un test con Konsist, o un script `rg` en CI que falle si un archivo de `*/domain/*` importa `io.github.jan.supabase`, `io.ktor`, `androidx.compose` o `*.data.*`).
7. Tests de `domain` y ViewModels para `cuenta` y `comunidad` en `commonTest`.
