# Guía de Contribución - Gacelas

Gracias por tu interés en contribuir a Gacelas. Este documento contiene las guías y convenciones que debemos seguir para mantener un código base limpio y consistente.

## Tabla de Contenidos

1. [Configuración del Entorno](#configuración-del-entorno)
2. [Estructura del Proyecto](#estructura-del-proyecto)
3. [Flujo de Trabajo Git](#flujo-de-trabajo-git)
4. [Convenciones de Código](#convenciones-de-código)
5. [Testing](#testing)
6. [Pull Requests](#pull-requests)
7. [Comunicación](#comunicación)

## Configuración del Entorno

### Prerequisitos

- **Node.js**: 18.0.0 o superior
- **npm** o **pnpm**: Última versión
- **Android Studio**: Para desarrollo móvil
- **JDK**: 17 o superior
- **Git**: Control de versiones
- **Supabase CLI** (opcional): Para desarrollo local de backend

### Setup Inicial

1. **Clonar el repositorio**

   ```bash
   git clone https://github.com/[usuario]/gacelas.git
   cd gacelas
   ```

2. **Instalar dependencias**

   ```bash
   pnpm install
   ```

3. **Configurar variables de entorno**

   Copiar archivos de ejemplo:

   ```bash
   cp apps/web/.env.example apps/web/.env.local
   cp apps/android/local.properties.example apps/android/local.properties
   cp backend/.env.example backend/.env
   ```

   Completar con tus keys:
   - **Supabase**: Obtener de https://app.supabase.com
   - **Google Maps**: Obtener de https://console.cloud.google.com

4. **Iniciar aplicación web**

   ```bash
   pnpm dev
   ```

5. **Abrir aplicación Android**
   - Abrir `apps/android` en Android Studio
   - Sync Gradle
   - Ejecutar en emulador o dispositivo

## Estructura del Proyecto

```
gacelas/
├── apps/
│   ├── web/              # React + TypeScript + Tailwind
│   └── android/          # Kotlin + Jetpack Compose
├── packages/
│   └── shared-types/     # Tipos compartidos
├── backend/
│   ├── migrations/       # Migraciones SQL
│   └── functions/        # Supabase Edge Functions
├── docs/                 # Documentación
├── .github/
│   └── workflows/        # CI/CD
└── package.json
```

### Responsabilidades por Carpeta

- **apps/web**: Frontend web, solo navegador
- **apps/android**: App móvil nativa Android
- **packages/shared-types**: Tipos TypeScript compartidos entre web y backend
- **backend/migrations**: Schema SQL, ejecución secuencial
- **backend/functions**: Lógica serverless en Deno
- **docs**: Documentación en Markdown

## Flujo de Trabajo Git

### Branches

Usamos **Git Flow** simplificado:

- `main`: Código en producción, siempre estable
- `develop`: Rama de desarrollo, integración continua
- `feature/nombre-feature`: Features nuevas
- `fix/nombre-bug`: Corrección de bugs
- `docs/tema`: Actualizaciones de documentación

### Crear un Branch

```bash
# Feature
git checkout develop
git pull origin develop
git checkout -b feature/live-tracking

# Bug fix
git checkout -b fix/map-not-loading

# Docs
git checkout -b docs/update-readme
```

### Commits

Seguimos **Conventional Commits**: https://www.conventionalcommits.org/

**Formato**:

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types**:

- `feat`: Nueva feature
- `fix`: Corrección de bug
- `docs`: Cambios en documentación
- `style`: Formato, punto y coma faltante, etc (no afecta lógica)
- `refactor`: Refactorización de código
- `perf`: Mejora de performance
- `test`: Agregar o corregir tests
- `chore`: Tareas de mantenimiento (deps, config)

**Ejemplos**:

```bash
# Feature nueva
git commit -m "feat(web): add live tracking map component"

# Bug fix
git commit -m "fix(android): location service not stopping on activity end"

# Documentación
git commit -m "docs: update setup instructions in README"

# Refactor
git commit -m "refactor(web): extract map logic into custom hook"

# Con scope y body
git commit -m "feat(backend): add route simplification function

Implement Douglas-Peucker algorithm to reduce track points
without losing visual fidelity. Reduces storage by ~75%.

Closes #42"
```

**Scopes comunes**:

- `web`: Aplicación web
- `android`: Aplicación Android
- `backend`: Backend/base de datos
- `shared`: Código compartido
- `ci`: CI/CD
- `deps`: Dependencias

### Pull Requests

Ver sección [Pull Requests](#pull-requests) más abajo.

## Convenciones de Código

### General

- **Idioma**: Código en **inglés** (variables, funciones, comentarios)
- **Documentación**: En **español**
- **Indentación**: 2 espacios (no tabs)
- **Line endings**: LF (Unix), no CRLF
- **Trailing commas**: Siempre en objetos/arrays multi-línea
- **Quotes**: Single quotes `'` en JavaScript/TypeScript

### TypeScript/JavaScript (Web)

**Estilo**: Seguir guía de [Airbnb JavaScript Style Guide](https://github.com/airbnb/javascript)

**Naming Conventions**:

```typescript
// Variables y funciones: camelCase
const userName = 'Juan'
function getUserProfile() {}

// Constantes: UPPER_SNAKE_CASE
const MAX_RETRY_COUNT = 3

// Interfaces y Types: PascalCase
interface UserProfile {}
type ActivityType = 'run' | 'walk'

// Componentes React: PascalCase
function ActivityCard() {}

// Archivos: kebab-case para archivos, PascalCase para componentes
// ✅ activity-card.tsx (archivo), ActivityCard (componente)
// ❌ ActivityCard.tsx (evitar)
```

**Imports**: Ordenar por categorías

```typescript
// 1. External libraries
import React from 'react'
import { useQuery } from '@tanstack/react-query'

// 2. Internal modules
import { useAuth } from '@/hooks/useAuth'
import { supabase } from '@/lib/supabase'

// 3. Components
import { Button } from '@/components/ui/button'

// 4. Types
import type { Activity } from '@/types'

// 5. Relative imports
import { calculateDistance } from './utils'
```

**Functions**: Preferir arrow functions

```typescript
// ✅ Preferido
const calculatePace = (distance: number, duration: number): number => {
  return duration / distance
}

// ❌ Evitar
function calculatePace(distance: number, duration: number): number {
  return duration / distance
}

// Excepción: Componentes React (ambos estilos OK)
export function ActivityCard() {}
export const ActivityCard = () => {}
```

**Types vs Interfaces**:

```typescript
// Preferir `interface` para objetos y contracts
interface UserProfile {
  id: string
  username: string
}

// Preferir `type` para unions, aliases, intersections
type ActivityType = 'run' | 'walk' | 'hike'
type OptionalUser = User | null
```

**Async/Await**: Preferir sobre Promises

```typescript
// ✅ Preferido
async function fetchActivity(id: string) {
  try {
    const { data, error } = await supabase.from('activities').select('*').eq('id', id).single()

    if (error) throw error
    return data
  } catch (error) {
    console.error('Error fetching activity:', error)
    throw error
  }
}

// ❌ Evitar
function fetchActivity(id: string) {
  return supabase
    .from('activities')
    .select('*')
    .eq('id', id)
    .single()
    .then(({ data, error }) => {
      if (error) throw error
      return data
    })
}
```

### Kotlin (Android)

**Estilo**: Seguir [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)

**Naming Conventions**:

```kotlin
// Variables y funciones: camelCase
val userName = "Juan"
fun getUserProfile() { }

// Constantes: UPPER_SNAKE_CASE (en companion object)
companion object {
    const val MAX_RETRY_COUNT = 3
}

// Classes e Interfaces: PascalCase
class ActivityRepository
interface LocationProvider

// Packages: lowercase sin guion bajo
package com.gacelas.feature.recording

// Archivos: PascalCase, mismo nombre que clase principal
// ActivityRepository.kt
```

**Null Safety**: Aprovechar sistema de tipos de Kotlin

```kotlin
// ✅ Preferido: Usar tipos nullable con safe calls
val userName: String? = user?.profile?.username

// ✅ Elvis operator para defaults
val displayName = userName ?: "Usuario Anónimo"

// ✅ Null checks con let
user?.let {
    updateProfile(it)
}

// ❌ Evitar !! (force unwrap) a menos que estés 100% seguro
val userName = user!!.username  // Puede crashear
```

**Data Classes**: Para models simples

```kotlin
data class Activity(
    val id: String,
    val userId: String,
    val title: String,
    val distanceMeters: Int,
    val durationSeconds: Int
)
```

**Coroutines**: Para operaciones async

```kotlin
// ViewModel
viewModelScope.launch {
    try {
        val activity = repository.getActivity(activityId)
        _uiState.update { it.copy(activity = activity) }
    } catch (e: Exception) {
        _uiState.update { it.copy(error = e.message) }
    }
}

// Repository
suspend fun getActivity(id: String): Activity {
    return withContext(Dispatchers.IO) {
        supabase.from("activities")
            .select()
            .eq("id", id)
            .decodeSingle<Activity>()
    }
}
```

### SQL (Migrations)

**Naming Conventions**:

```sql
-- Tablas: plural, snake_case
CREATE TABLE activities ( );
CREATE TABLE track_points ( );

-- Columnas: snake_case
user_id, created_at, is_public

-- Índices: <tabla>_<columnas>_idx
CREATE INDEX activities_user_id_idx ON activities(user_id);

-- Foreign keys: <tabla>_<columna>_fkey
ALTER TABLE activities ADD CONSTRAINT activities_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id);

-- Policies: descriptivo en inglés, quotes
CREATE POLICY "Users can view own activities"
ON activities FOR SELECT
USING (user_id = auth.uid());
```

**Orden de statements**:

```sql
-- 1. CREATE TABLE
-- 2. ALTER TABLE (constraints, defaults)
-- 3. CREATE INDEX
-- 4. ENABLE ROW LEVEL SECURITY
-- 5. CREATE POLICY
```

## Testing

### Web (React)

**Framework**: Vitest + React Testing Library

**Ubicación**: `__tests__` junto al archivo testeado

```
components/
├── activity-card.tsx
└── __tests__/
    └── activity-card.test.tsx
```

**Naming**: `<archivo>.test.tsx`

**Ejemplo**:

```typescript
import { render, screen } from '@testing-library/react'
import { ActivityCard } from '../activity-card'

describe('ActivityCard', () => {
  it('should render activity title', () => {
    const activity = {
      id: '1',
      title: 'Morning Run',
      distanceMeters: 5000,
      durationSeconds: 1800
    }

    render(<ActivityCard activity={activity} />)

    expect(screen.getByText('Morning Run')).toBeInTheDocument()
  })

  it('should display distance in km', () => {
    const activity = {
      id: '1',
      title: 'Run',
      distanceMeters: 5000,
      durationSeconds: 1800
    }

    render(<ActivityCard activity={activity} />)

    expect(screen.getByText('5.0 km')).toBeInTheDocument()
  })
})
```

**Ejecutar tests**:

```bash
pnpm test:unit  # Tests unitarios
pnpm test:watch # Watch mode con Vitest
```

### Android (Kotlin)

**Framework**: JUnit 4 + MockK

**Ubicación**: `src/test/java/` para unit tests, `src/androidTest/java/` para UI tests

**Naming**: `<Clase>Test.kt`

**Ejemplo**:

```kotlin
class ActivityRepositoryTest {

    @Test
    fun `getActivity returns activity when found`() = runTest {
        // Given
        val activityId = "test-id"
        val expected = Activity(id = activityId, title = "Test Run")

        // When
        val result = repository.getActivity(activityId)

        // Then
        assertEquals(expected, result)
    }

    @Test
    fun `getActivity throws exception when not found`() = runTest {
        // Given
        val activityId = "non-existent"

        // When & Then
        assertThrows<NotFoundException> {
            repository.getActivity(activityId)
        }
    }
}
```

**Ejecutar tests**:

```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # UI tests (requiere emulador)
```

## Pull Requests

### Antes de Crear PR

1. ✅ Tu código compila sin errores
2. ✅ Todos los tests pasan
3. ✅ Has probado manualmente los cambios
4. ✅ El código sigue las convenciones de estilo
5. ✅ Has actualizado documentación si es necesario
6. ✅ Commit messages son descriptivos

### Crear PR

1. Push tu branch:

   ```bash
   git push origin feature/live-tracking
   ```

2. Ir a GitHub y crear Pull Request

3. **Template de PR**:
   ```markdown
   ## Descripción

   Breve descripción de qué hace este PR.

   ## Tipo de Cambio

   - [ ] Bug fix
   - [ ] Nueva feature
   - [ ] Breaking change
   - [ ] Documentación

   ## ¿Cómo se ha testeado?

   Describe los tests que ejecutaste.

   ## Checklist

   - [ ] Mi código sigue las guías de estilo
   - [ ] He realizado self-review
   - [ ] He comentado código complejo
   - [ ] He actualizado documentación
   - [ ] Mis cambios no generan warnings
   - [ ] He agregado tests
   - [ ] Tests nuevos y existentes pasan

   ## Screenshots (si aplica)

   ![Screenshot](url)
   ```

### Code Review

**Como autor**:

- Responde a comentarios de forma constructiva
- Haz los cambios solicitados en commits separados
- Resuelve conversaciones cuando estén completadas

**Como reviewer**:

- Sé respetuoso y constructivo
- Explica el "por qué" de tus sugerencias
- Aprueba cuando estés satisfecho
- Usa labels: `suggestion`, `nitpick`, `blocking`

### Merge

- **Squash and Merge** preferido para features pequeñas
- **Merge Commit** para features grandes con historia relevante
- Eliminar branch después de merge

## Comunicación

### Issues

**Crear issue**: https://github.com/[usuario]/gacelas/issues/new

**Template de Bug**:

```markdown
## Descripción del Bug

Descripción clara del problema.

## Pasos para Reproducir

1. Ir a '...'
2. Click en '...'
3. Ver error

## Comportamiento Esperado

Qué debería pasar.

## Screenshots

![Screenshot](url)

## Entorno

- OS: [e.g. Android 13, macOS 14]
- Navegador: [e.g. Chrome 120]
- Versión de la app: [e.g. 0.1.0]
```

**Template de Feature Request**:

```markdown
## Feature Solicitada

Descripción clara de la feature.

## Caso de Uso

¿Por qué es necesaria? ¿Qué problema resuelve?

## Solución Propuesta

¿Cómo se vería la implementación?

## Alternativas Consideradas

¿Hay otras formas de resolver esto?
```

### Contacto

- **GitHub Issues**: Para bugs y features
- **GitHub Discussions**: Para preguntas y discusiones generales
- **Email**: jhonatan.jara@ejemplo.com (para privacidad/seguridad)

## Licencia

Al contribuir, aceptas que tus contribuciones serán licenciadas bajo la misma licencia del proyecto.

---

## ¡Gracias por Contribuir! 🙌

Tu tiempo y esfuerzo son muy apreciados. Juntos estamos construyendo la mejor app de running para Ecuador. 🏃‍♀️🏃‍♂️
