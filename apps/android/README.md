# Gacelas Android App

Aplicación móvil nativa para Android construida con Kotlin y Jetpack Compose.

## Requisitos

- Android Studio Hedgehog (2023.1.1) o superior
- JDK 17
- Android SDK 34
- Dispositivo o emulador con Android 7.0 (API 24) o superior

## Configuración

1. **Abrir en Android Studio**:
   - File - Open - Seleccionar carpeta `apps/android`

2. **Configurar local.properties**:

   ```bash
   cp local.properties.example local.properties
   ```

   Editar `local.properties` con tus keys:

   ```properties
   GOOGLE_MAPS_API_KEY=tu-api-key-aqui
   SUPABASE_URL=https://tu-proyecto.supabase.co
   SUPABASE_ANON_KEY=tu-anon-key-aqui
   ```

3. **Sync Gradle**:
   - Esperar a que Gradle descargue dependencias

4. **Ejecutar**:
   - Run - Run 'app'
   - Seleccionar dispositivo o emulador

## Arquitectura

```
app/src/main/java/com/gacelas/
├── core/                    # Lógica compartida
│   ├── data/               # Repositorios
│   ├── domain/             # Entities, use cases
│   ├── network/            # Supabase client
│   └── location/           # Location service
├── feature/                # Features por módulo
│   ├── recording/          # Grabación de actividades
│   ├── routes/
│   ├── community/
│   ├── events/
│   └── profile/
└── ui/                     # UI compartido
    ├── components/
    ├── theme/
    └── navigation/
```

## Stack Tecnológico

- **Lenguaje**: Kotlin 2.0
- **UI**: Jetpack Compose + Material 3
- **Arquitectura**: MVVM + Clean Architecture
- **DI**: Hilt (Dagger)
- **Navigation**: Compose Navigation
- **Networking**: Ktor Client
- **Backend**: Supabase Kotlin SDK
- **Maps**: Google Maps SDK + Maps Compose
- **Location**: Fused Location Provider
- **Database Local**: Room

## Estado de Desarrollo

Este es el scaffold inicial. Las funcionalidades se implementarán según [../../docs/ROADMAP.md](../../docs/ROADMAP.md).

### Fase 1 (MVP)

- [ ] Autenticación con Supabase
- [ ] Location Service para grabación GPS
- [ ] Pantalla de grabación con mapa
- [ ] Guardar track points en batches
- [ ] Pantalla de resumen post-actividad

### Fase 2 (Social)

- [ ] Feed de actividades
- [ ] Perfil de usuario
- [ ] Follows

### Fase 3 (Live Tracking)

- [ ] Broadcast de posición en tiempo real
- [ ] Ver corredores en vivo

## Permisos

La app requiere los siguientes permisos (ver `AndroidManifest.xml`):

- `ACCESS_FINE_LOCATION`: GPS de alta precisión
- `ACCESS_COARSE_LOCATION`: Ubicación aproximada (fallback)
- `FOREGROUND_SERVICE`: Servicio en primer plano durante grabación
- `FOREGROUND_SERVICE_LOCATION`: Tipo específico de foreground service
- `POST_NOTIFICATIONS`: Notificaciones (Android 13+)

## Testing

```bash
# Unit tests
./gradlew test

# UI tests (requiere emulador/dispositivo)
./gradlew connectedAndroidTest
```

## Build

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requiere signing config)
./gradlew assembleRelease
```

APK generado en: `app/build/outputs/apk/`

## Documentación

Ver documentación completa del proyecto en [../../docs/](../../docs/)
