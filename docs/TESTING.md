# Estrategia de Pruebas - Gacelas Web

Este documento describe la estrategia de pruebas para la aplicación web de Gacelas, una app de running construida con React, TypeScript, Vite, React Router y Tailwind CSS.

## Resumen

La aplicación cuenta con dos niveles de pruebas:

1. Pruebas unitarias y de integración con Vitest y Testing Library
2. Pruebas end-to-end con Playwright

### Cobertura de Código

El proyecto mantiene los siguientes umbrales mínimos de cobertura:

- Líneas: 70%
- Funciones: 70%
- Branches: 70%
- Statements: 70%

## Pruebas Unitarias y de Integración

### Tecnologías

- Vitest: framework de testing
- Testing Library React: testing de componentes
- jsdom: DOM virtual para testing
- User Event: simulación de interacciones de usuario
- jest-dom: matchers adicionales

### Estructura de Carpetas

```
apps/web/src/
├── components/
│   └── __tests__/          # Tests de componentes compartidos
├── features/
│   ├── acceso/
│   │   └── __tests__/      # Tests de login/registro
│   ├── inicio/
│   │   └── __tests__/      # Tests de home
│   ├── correr/
│   │   └── __tests__/      # Tests de funcionalidad de running
│   └── ...
├── test/
│   └── setup.tsx           # Configuración global de tests
└── __tests__/
    └── App.test.tsx        # Tests del router principal
```

### Ejecutar Pruebas Unitarias

```bash
# Desde la raíz del proyecto
pnpm test

# O desde apps/web
cd apps/web
pnpm test

# Modo watch para desarrollo
pnpm test:watch

# Con reporte de cobertura
pnpm test:coverage
```

### Qué se Prueba

#### Componentes Compartidos

- AppLayout: navegación, header, bottom navigation
- Runner3D: canvas 3D (mockeado en tests)
- Navigation: enlaces y estados activos

#### Pantallas por Feature

Cada feature tiene tests que verifican:

- Renderizado sin errores
- Textos clave visibles
- Elementos interactivos presentes
- Validaciones de formularios
- Estados de la UI

#### Router

- Rutas existen y se renderizan correctamente
- Navegación entre pantallas
- Redirects funcionan
- Rutas no encontradas

### Convenciones de Testing

1. Cada componente complejo debe tener su archivo de test
2. Los mocks de Three.js/React Three Fiber están en `test/setup.tsx`
3. Usar `data-testid` solo cuando no hay otra forma de seleccionar elementos
4. Preferir selectores por rol, texto y labels accesibles
5. Tests de formularios deben verificar validación

### Mocks Globales

El archivo `test/setup.tsx` contiene mocks para:

- `@react-three/fiber`: Canvas mockeado como div con data-testid
- `@react-three/drei`: useGLTF, useAnimations, OrbitControls
- `three`: Group y otros objetos 3D

Estos mocks permiten probar componentes que usan Three.js sin necesidad de un contexto WebGL.

## Pruebas End-to-End

### Tecnología

- Playwright: framework E2E
- Navegadores: Chromium
- Viewports: Mobile (390x844) y Desktop

### Estructura

```
apps/web/e2e/
├── onboarding.spec.ts      # Flujo de registro
├── login.spec.ts           # Inicio de sesión
├── correr.spec.ts          # Funcionalidad de carrera
├── navigation.spec.ts      # Navegación general
├── routes.spec.ts          # Rutas y búsqueda
├── profile.spec.ts         # Perfil y avatar 3D
├── settings.spec.ts        # Ajustes y privacidad
└── notifications.spec.ts   # Notificaciones
```

### Ejecutar Pruebas E2E

```bash
# Instalar navegadores (solo primera vez)
cd apps/web
npx playwright install

# Hacer git lfs pull para obtener el modelo 3D
git lfs pull

# Ejecutar todas las pruebas
pnpm test:e2e

# Con UI interactiva
pnpm test:e2e:ui

# En modo headed (ver navegador)
pnpm test:e2e:headed
```

### Configuración

La configuración de Playwright está en `playwright.config.ts`.

Características:

- WebServer: inicia `vite preview` automáticamente
- Retries: 2 intentos en CI, 0 en local
- Trace: se guarda solo en el primer retry
- Reporter: HTML

### Flujos E2E Cubiertos

1. Onboarding y Registro
   - Navegación desde bienvenida a registro
   - Completar formulario
   - Redirección a permisos o inicio

2. Inicio de Sesión
   - Login exitoso
   - Recuperar contraseña

3. Funcionalidad de Correr
   - Iniciar carrera
   - Usar cronómetro
   - Pausa y reanudación
   - Detener y ver resumen

4. Navegación
   - Bottom nav entre secciones
   - Eventos y detalles
   - Perfil y configuración

5. Rutas
   - Explorar rutas
   - Búsqueda de rutas

6. Perfil
   - Editar información
   - Personalizar avatar 3D (verifica canvas)

7. Ajustes
   - Configurar privacidad
   - Configurar notificaciones
   - Interactuar con toggles

8. Notificaciones
   - Acceder a bandeja

### Notas sobre el Modelo 3D

El archivo GLB (`/models/gacelas-runner-animated.glb`) está gestionado con Git LFS.

Antes de ejecutar las pruebas E2E que involucren el Runner3D, asegúrate de tener el archivo:

```bash
git lfs pull
```

Si no tienes Git LFS instalado:

```bash
# Ubuntu/Debian
sudo apt-get install git-lfs

# macOS
brew install git-lfs

# Inicializar en el repo
git lfs install
```

## Integración Continua

### Workflow de GitHub Actions

El archivo `.github/workflows/web-tests.yml` ejecuta:

1. Checkout del código con `lfs: true`
2. Setup de Node.js y pnpm
3. Instalación de dependencias con cache
4. Lint del código
5. Pruebas unitarias con cobertura
6. Build de la aplicación
7. Instalación de navegadores Playwright
8. Pruebas E2E
9. Upload del reporte de coverage como artifact
10. Upload del reporte E2E como artifact

### Artifacts

Los reportes quedan disponibles en la pestaña Actions de GitHub:

- `coverage-report`: HTML con cobertura de código
- `playwright-report`: HTML con resultados E2E
- `test-results`: Screenshots y traces de fallos

## Escribir Nuevas Pruebas

### Para Pruebas Unitarias

1. Crear archivo `__tests__/ComponentName.test.tsx` junto al componente
2. Importar testing library y el componente
3. Envolver en MemoryRouter si usa navegación
4. Usar queries accesibles (getByRole, getByLabelText, getByText)
5. Probar comportamiento, no implementación

Ejemplo:

```typescript
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { MiComponente } from '../MiComponente';

describe('MiComponente', () => {
  it('renderiza sin errores', () => {
    render(
      <MemoryRouter>
        <MiComponente />
      </MemoryRouter>
    );

    expect(screen.getByText(/texto clave/i)).toBeInTheDocument();
  });
});
```

### Para Pruebas E2E

1. Crear archivo `.spec.ts` en `e2e/`
2. Usar `test.beforeEach` para setup común (ej. login)
3. Usar selectores semánticos cuando sea posible
4. Verificar navegación y estados visibles
5. Evitar esperas hardcodeadas, usar `waitFor` o expects con timeout

Ejemplo:

```typescript
import { test, expect } from '@playwright/test'

test.describe('Mi Feature', () => {
  test.beforeEach(async ({ page }) => {
    // Setup común
    await page.goto('/inicio-sesion')
    // ...login
  })

  test('hace algo importante', async ({ page }) => {
    await page.getByRole('button', { name: /acción/i }).click()
    await expect(page).toHaveURL(/esperado/)
    await expect(page.getByText(/resultado/i)).toBeVisible()
  })
})
```

## Troubleshooting

### Git LFS

Si las pruebas E2E fallan por archivo GLB no encontrado:

```bash
git lfs install
git lfs pull
```

### Navegadores Playwright

Si fallan por navegadores no instalados:

```bash
cd apps/web
npx playwright install chromium
```

En CI, el workflow ya instala los navegadores.

### Tests Lentos

Si las pruebas unitarias son lentas:

- Verificar que no haya imports innecesarios
- Considerar mocks adicionales
- Usar `vi.mock` para dependencias pesadas

### Cobertura Baja

Si no se alcanza el umbral del 70%:

- Identificar archivos sin cobertura con el reporte HTML
- Agregar tests para casos edge
- Revisar que los mocks no oculten código no probado

### Fallos en CI

Si las pruebas pasan localmente pero fallan en CI:

- Revisar diferencias de timezone o locale
- Verificar que Git LFS esté habilitado en GitHub
- Revisar logs de artifacts para traces y screenshots
- Considerar aumentar timeouts en selectores

## Recursos Adicionales

- Documentación de Vitest: https://vitest.dev/
- Testing Library: https://testing-library.com/react
- Playwright: https://playwright.dev/
- Git LFS: https://git-lfs.com/

## Supabase en las pruebas

Ninguna prueba de CI usa el proyecto real ni necesita secretos:

- Unitarias (Vitest): `src/test/fakeSupabase.ts` es un cliente de Supabase en memoria. `src/data/__tests__/supabaseRepos.test.ts` prueba registro, sesion, carreras con cola sin conexion, rutas, ajustes, likes, comentarios y eventos.
- E2E (Playwright): la web se compila con `vite build --mode e2e` (`.env.e2e`) y `e2e/fakeSupabase.ts` responde las llamadas de Auth y PostgREST con `route()`. Todas las pruebas usan ese servidor falso.
- Android: `FakeBackend.kt` reemplaza a supabase-kt en `RepositoriosRemotosTest`.
- Contra el proyecto real, solo a mano: `cd apps/web && pnpm smoke:supabase` (ver `docs/SUPABASE.md`).
