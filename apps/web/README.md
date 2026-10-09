# Gacelas Web

Aplicación web de Gacelas construida con React + TypeScript + Vite + Tailwind CSS.

## Estructura del proyecto

```
src/
├── features/           # Pantallas organizadas por sección
│   ├── acceso/        # Bienvenida, login, registro, recuperar contraseña
│   ├── inicio/        # Pantalla de inicio con runner 3D
│   ├── correr/        # Carrera en curso, cronómetro, resumen
│   ├── rutas/         # Lista de rutas, buscador, navegación
│   ├── comunidad/     # Feed social, eventos, detalle de evento
│   ├── perfil/        # Perfil del usuario, editar, avatar 3D
│   └── ajustes/       # Ajustes, privacidad, notificaciones, permisos
├── components/        # Componentes compartidos
│   ├── AppLayout.tsx  # Layout principal con header y bottom nav
│   └── Runner3D.tsx   # Componente 3D del corredor animado
├── mocks/             # Datos mock para desarrollo
└── assets/            # Recursos estáticos
```

## Tecnologías

- React 18
- TypeScript
- Vite
- Tailwind CSS (con tokens de Material 3 de Stitch)
- React Router DOM
- Three.js + React Three Fiber + Drei (para escenas 3D)
- Material Symbols (iconos)

## Fuentes

- Inter (400, 500, 600, 700) - textos
- Roboto Flex (600, 700) - títulos

## Tokens de diseño

El archivo `tailwind.config.js` contiene todos los tokens extraídos de Google Stitch:

- Colores primarios: naranja amanecer (primary-600: #D97706)
- Colores secundarios: verde bosque (secondary-600: #059669)
- Paleta Material 3 completa (surface, on-surface, containers, etc.)
- Espaciado base-8
- Tipografía con escalas definidas
- Bordes y sombras

## Modelo 3D

El componente `Runner3D` carga el modelo `/models/gacelas-runner-animated.glb` con animaciones:

- Idle: posición de reposo
- Jog: trote ligero
- Run: carrera rápida

El modelo se carga de forma lazy y se usa en:

- Pantalla de inicio (Idle con auto-rotate)
- Pantalla de avatar 3D (Idle editable)
- Navegación 3D (Jog/Run según velocidad)

## Rutas de la aplicación

### Acceso (sin layout) - 5 rutas

- `/bienvenida` - Bienvenida (ID: b61edc1cb0ac4f79bd27f40a92624e8e)
- `/inicio-sesion` - InicioSesion (ID: 889e64826d154d7ab885855f489a2814)
- `/registro` - Registro (ID: c7a11b43c2ad4877a6bff9cc902f8e41)
- `/recuperar-contrasena` - RecuperarContrasena (ID: 677928b62b2d49a38cca263c8641fa5f)
- `/permisos-ubicacion` - PermisosUbicacion (ID: 31b3ed996ba749d88231fd674b8e9f24)

### Principales (con layout: header + bottom nav) - 5 rutas

- `/inicio` - Inicio con Runner 3D (ID: 3cda309d10e040d1bf07f99558aa001c)
- `/rutas` - ListaRutas (ID: 0c202fc9cfe74c1994ce1ece0010cf00)
- `/comunidad` - Feed social (ID: 673a5ce87e3a497bb1afdbece8d1d114)
- `/comunidad/eventos` - Eventos (ID: 3ac8ac9d8ef245ec92f72566b8681c03)
- `/perfil` - Perfil (ID: 4c002bb50db14d36a0aae56adde917f7)

### Correr (sin layout) - 4 rutas

- `/correr` - Preparación de carrera
- `/correr/en-curso` - CarreraEnCurso (ID: 7525f3ca1f474ca78ab04628d314879f)
- `/correr/cronometro` - Cronómetro ampliado (ID: 441fa480937c401499997b738aa65cd3)
- `/correr/resumen` - ResumenPostCarrera (ID: 2e0b3dcc47ac4e9997f174bdaedeff34)

### Rutas (sin layout) - 2 rutas

- `/rutas/buscar` - Buscador estilo Uber (ID: 70441a3c2dbc4eb7a8aa75c321d60bb2)
- `/rutas/navegacion` - Navegación 3D con Runner (ID: 1f20f211c8b547209350ae8d8f384bfa)

### Comunidad (sin layout) - 1 ruta

- `/comunidad/eventos/:id` - DetalleEvento (ID: 40e6040a070c41a7889fb349e89a226e)

### Perfil (sin layout) - 2 rutas

- `/perfil/editar` - EditarPerfil (ID: 75bd965b782646f889f62747a0715b72)
- `/perfil/avatar` - Avatar3D (ID: df69e2fc40494da7901024d901382056)

### Ajustes (sin layout) - 3 rutas

- `/ajustes` - AjustesPreferencias (ID: c8e26916e4bf48399016e6ed751d025d)
- `/ajustes/privacidad` - PrivacidadUbicacion (ID: 1cfbd65e8a9e43398d84c1954f3cc230)
- `/ajustes/notificaciones` - ConfiguracionNotificaciones (ID: 085a47e52df74e26bb6bceb746c138fb)

### Notificaciones (sin layout) - 1 ruta

- `/notificaciones` - Notificaciones (bandeja)

**Total: 23 rutas implementadas**

## Tabla de pantallas

| Sección | Pantalla | Ruta | ID Stitch |
|---------|----------|------|-----------|
| Acceso | Bienvenida | /bienvenida | b61edc1cb0ac4f79bd27f40a92624e8e |
| Acceso | Inicio de sesión | /inicio-sesion | 889e64826d154d7ab885855f489a2814 |
| Acceso | Registro | /registro | c7a11b43c2ad4877a6bff9cc902f8e41 |
| Acceso | Recuperar contraseña | /recuperar-contrasena | 677928b62b2d49a38cca263c8641fa5f |
| Inicio | Inicio | /inicio | 3cda309d10e040d1bf07f99558aa001c |
| Correr | Carrera en curso | /correr/en-curso | 7525f3ca1f474ca78ab04628d314879f |
| Correr | Resumen post-carrera | /correr/resumen | 2e0b3dcc47ac4e9997f174bdaedeff34 |
| Rutas | Lista de rutas | /rutas | 0c202fc9cfe74c1994ce1ece0010cf00 |
| Comunidad | Feed | /comunidad | 673a5ce87e3a497bb1afdbece8d1d114 |
| Perfil | Perfil | /perfil | 4c002bb50db14d36a0aae56adde917f7 |
| Ajustes | Ajustes y preferencias | /ajustes | c8e26916e4bf48399016e6ed751d025d |
| Ajustes | Permisos de ubicación | /permisos-ubicacion | 31b3ed996ba749d88231fd674b8e9f24 |

## Pantallas no portadas

Las siguientes pantallas de Stitch no fueron implementadas en esta primera versión:

- Cronómetro ampliado (441fa480937c401499997b738aa65cd3)
- Modal de cronómetro (ce3a57477b9d4f298539c8bb1e36ee28)
- Buscador de rutas (70441a3c2dbc4eb7a8aa75c321d60bb2)
- Navegación 3D (1f20f211c8b547209350ae8d8f384bfa)
- Eventos (3ac8ac9d8ef245ec92f72566b8681c03)
- Detalle de evento (40e6040a070c41a7889fb349e89a226e)
- Editar perfil (75bd965b782646f889f62747a0715b72)
- Avatar 3D (df69e2fc40494da7901024d901382056)
- Privacidad y ubicación (1cfbd65e8a9e43398d84c1954f3cc230)
- Notificaciones (085a47e52df74e26bb6bceb746c138fb)

## Pendientes

- Integración con Supabase (autenticación y base de datos)
- Integración con Google Maps para rutas y navegación
- Implementación completa del tracking GPS
- Conexión de datos mock con API real
- Pantallas adicionales (cronómetro ampliado, navegación 3D, eventos, etc.)
- Tests unitarios y de integración
- Optimización de carga del modelo 3D
- PWA para instalación en dispositivos móviles

## Notas sobre diseño

Las pantallas fueron portadas fielmente desde las maquetas de Google Stitch, usando:

- Material Symbols para iconos (en lugar de Heroicons del DESIGN.md)
- Tailwind CSS con configuración inline del export de Stitch
- Mobile-first responsive design
- Dark mode como predeterminado

## Desarrollo

```bash
pnpm install
pnpm dev
```

## Build

```bash
pnpm build
```

## Lint

```bash
pnpm lint
```
