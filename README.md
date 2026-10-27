# Runcity

**Aplicación de running con seguimiento en tiempo real para runners ecuatorianos**

Nota: el producto se llama Runcity desde la version 0.8.6. El repositorio, el paquete Android (com.gacelas), los modulos de codigo, el dominio gacelas.vercel.app y el proyecto de Supabase conservan el nombre Gacelas.

Runcity es una plataforma integral para corredores que permite grabar rutas, compartir actividades, unirse a eventos, y seguir a otros corredores en tiempo real usando GPS y Google Maps.

## Características Principales

- **Rutas**: Graba, dibuja, guarda y comparte tus rutas de running
- **Comunidad**: Feed social, sigue a otros corredores, únete a clubes y grupos
- **Eventos**: Carreras grupales, inscripciones, seguimiento en vivo durante eventos
- **Perfil**: Estadísticas personales, historial de actividades
- **Seguimiento en Vivo**: Comparte tu ubicación en tiempo real con streaming eficiente

## Estructura del Proyecto

```
gacelas/
├── apps/
│   ├── web/          # Aplicación web (React + TypeScript + Tailwind)
│   └── android/      # Aplicación Android (Kotlin + Jetpack Compose)
├── packages/
│   └── shared-types/ # Tipos compartidos TypeScript/Kotlin
├── supabase/
│   └── migrations/   # Schema SQL versionado para Supabase CLI
├── backend/          # Variables de entorno y notas del proyecto Supabase
├── docs/             # Documentación extensa
│   ├── adr/          # Architecture Decision Records
│   ├── DESIGN.md
│   ├── SYSTEM.md
│   ├── COMPONENTS.md
│   ├── DATA_MODEL.md
│   ├── BUSINESS_RULES.md
│   ├── SECURITY_PRIVACY.md
│   ├── FREE_TIER_BUDGET.md
│   ├── TESTING.md
│   └── ROADMAP.md
└── .github/
    └── workflows/    # CI/CD
```

## Inicio Rápido

### Prerequisitos

- Node.js 20+ y pnpm
- Android Studio (para desarrollo móvil)
- JDK 17+
- Cuenta de Supabase (gratis)
- API Key de Google Maps

### Configuración

1. **Clonar el repositorio**

   ```bash
   git clone <repo-url>
   cd gacelas
   ```

2. **Instalar dependencias**

   ```bash
   pnpm install
   ```

3. **Configurar variables de entorno**

   Copiar los archivos de ejemplo y completar con tus keys:

   ```bash
   cp apps/web/.env.example apps/web/.env.local
   cp apps/android/local.properties.example apps/android/local.properties
   cp backend/.env.example backend/.env
   ```

4. **Iniciar aplicación web**

   ```bash
   cd apps/web
   pnpm dev
   ```

   O desde la raíz:

   ```bash
   pnpm dev
   ```

5. **Abrir aplicación Android**
   - Abrir `apps/android` en Android Studio
   - Sincronizar Gradle
   - Ejecutar en emulador o dispositivo

### Configuración de Backend (Supabase)

Ver [SYSTEM.md](docs/SYSTEM.md) para instrucciones detalladas de configuración de Supabase.

## Documentación

La documentación completa está en la carpeta `docs/`:

- **[DESIGN.md](docs/DESIGN.md)**: Diseño de producto, UX, sistema de diseño
- **[SYSTEM.md](docs/SYSTEM.md)**: Arquitectura del sistema, diagramas, streaming
- **[COMPONENTS.md](docs/COMPONENTS.md)**: Componentes web y móvil
- **[DATA_MODEL.md](docs/DATA_MODEL.md)**: Modelo de datos, entidades, relaciones
- **[BUSINESS_RULES.md](docs/BUSINESS_RULES.md)**: Reglas de negocio
- **[SECURITY_PRIVACY.md](docs/SECURITY_PRIVACY.md)**: Seguridad y privacidad
- **[FREE_TIER_BUDGET.md](docs/FREE_TIER_BUDGET.md)**: Presupuesto tier gratuito
- **[ROADMAP.md](docs/ROADMAP.md)**: Hoja de ruta del producto
- **[CONTRIBUTING.md](docs/CONTRIBUTING.md)**: Guía de contribución
- **[adr/](docs/adr/)**: Decisiones arquitectónicas, incluida la capacidad y el perfil free

## Stack Tecnológico

- **Frontend Web**: React 18, TypeScript, Tailwind CSS, Vite
- **Mobile**: Kotlin, Jetpack Compose, Android SDK 24+
- **Backend**: Supabase (PostgreSQL + PostGIS + Realtime)
- **Maps**: Google Maps API (Web y Android)
- **Real-time**: Supabase Realtime + Broadcast Channels
- **Autenticación**: Supabase Auth

## Equipo

**[Jhonatan Jara](https://github.com/jhonatanJJ2006)** - Estudiante de Computación, UTPL, Ecuador

## Contribuidores

- **[Sebastian Ludeña](https://github.com/sebastianlude)** ([@sebastianlude](https://github.com/sebastianlude))

## Licencia Licencia

[Definir licencia]

## Enlaces Útiles

- [Documentación de Supabase](https://supabase.com/docs)
- [Google Maps Platform](https://developers.google.com/maps)
- [Guía de Android Location Services](https://developer.android.com/training/location)
