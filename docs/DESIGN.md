# Diseño de Producto - Gacelas

## Visión del Producto

Gacelas es una plataforma social para runners ecuatorianos que transforma el running de una actividad solitaria en una experiencia compartida y motivadora. Permite a los corredores grabar sus rutas, seguir a amigos en tiempo real, unirse a eventos grupales, y formar parte de una comunidad activa.

### Valores Principales

- **Motivación social**: Correr es más divertido con otros
- **Transparencia**: Datos de actividad precisos y verificables
- **Seguridad**: Control total sobre quién ve tu ubicación
- **Accesibilidad**: Simple para principiantes, poderoso para avanzados

## Público Objetivo

### Perfil Principal: Runner Casual-Social (70%)

- **Edad**: 20-35 años
- **Experiencia**: Corre 2-4 veces por semana, 5-10 km por sesión
- **Motivación**: Fitness, socializar, explorar nuevas rutas
- **Tecnología**: Smartphone siempre a mano, apps de redes sociales diarias
- **Necesidades**: Compartir logros, encontrar compañeros de running, descubrir rutas

### Perfil Secundario: Runner Competitivo (20%)

- **Edad**: 25-45 años
- **Experiencia**: Entrena 5-6 días/semana, participa en carreras
- **Motivación**: Mejorar tiempos, prepararse para competencias
- **Necesidades**: Análisis detallado, comparación de performance, eventos competitivos

### Perfil Terciario: Organizador de Eventos (10%)

- **Rol**: Líder de club de running, organizador de carreras
- **Necesidades**: Gestionar grupos, coordinar salidas grupales, tracking en vivo de eventos

## Principios de Diseño UX

### 1. "Grab & Go"

El usuario debe poder iniciar una carrera en **máximo 2 taps** desde abrir la app:

```
Open App - [START RUN] - Running
```

Sin menús, sin configuración obligatoria, sin distracciones.

### 2. Feedback Instantáneo

Cada acción tiene respuesta visual inmediata:

- Botón de START: animación de pulso cardíaco
- Compartir ubicación: icono de "broadcasting" animado
- Guardar ruta: confeti de celebración

### 3. Contexto Geográfico

Priorizar contenido local:

- Rutas cerca de ti primero
- Eventos en tu ciudad
- Runners en tu área

### 4. Privacidad por Defecto

- Live tracking: OFF por defecto, requiere activación explícita
- Ubicación de inicio/fin: difuminada por defecto (300m radius)
- Perfil: seguidores deben ser aprobados

### 5. Diseño Mobile-First

90% de los usuarios estarán en móvil durante la actividad. El diseño fluye de móvil a web, no al revés.

## Sistema de Diseño

### Paleta de Colores

Inspirada en el amanecer ecuatoriano (hora favorita para correr) y la agilidad de una gacela.

#### Colores Primarios

```css
--primary-600: #d97706; /* Naranja amanecer - Principal */
--primary-700: #b45309; /* Naranja oscuro - Hover */
--primary-500: #f59e0b; /* Naranja claro - Backgrounds */
--primary-400: #fbbf24; /* Dorado - Acentos */
```

#### Colores Secundarios

```css
--secondary-600: #059669; /* Verde bosque - Success, rutas completadas */
--secondary-700: #047857; /* Verde oscuro - Hover */
--secondary-500: #10b981; /* Verde menta - Highlights */
```

#### Neutros (Dark-friendly)

```css
/* Light mode */
--bg-primary: #ffffff;
--bg-secondary: #f9fafb;
--text-primary: #111827;
--text-secondary: #6b7280;
--border: #e5e7eb;

/* Dark mode */
--bg-primary-dark: #111827;
--bg-secondary-dark: #1f2937;
--text-primary-dark: #f9fafb;
--text-secondary-dark: #9ca3af;
--border-dark: #374151;
```

#### Colores de Estado

```css
--danger: #dc2626; /* Errores, alertas */
--warning: #f59e0b; /* Advertencias */
--success: #059669; /* Confirmaciones */
--info: #3b82f6; /* Información */
```

### Tipografía

#### Fuente Principal: Inter

- **Web**: `font-family: 'Inter', -apple-system, sans-serif;`
- **Android**: System default (Roboto) para consistencia con Material Design
- **Pesos**: 400 (Regular), 600 (Semibold), 700 (Bold)

#### Escala de Tamaños

```css
--text-xs: 0.75rem; /* 12px - Labels, metadata */
--text-sm: 0.875rem; /* 14px - Body secundario */
--text-base: 1rem; /* 16px - Body principal */
--text-lg: 1.125rem; /* 18px - Subtítulos */
--text-xl: 1.25rem; /* 20px - Títulos de sección */
--text-2xl: 1.5rem; /* 24px - Títulos de página */
--text-3xl: 1.875rem; /* 30px - Métricas destacadas */
--text-4xl: 2.25rem; /* 36px - Números grandes (distancia, tiempo) */
```

### Espaciado

Sistema base-8:

```css
--space-1: 0.25rem; /* 4px */
--space-2: 0.5rem; /* 8px */
--space-3: 0.75rem; /* 12px */
--space-4: 1rem; /* 16px */
--space-6: 1.5rem; /* 24px */
--space-8: 2rem; /* 32px */
--space-12: 3rem; /* 48px */
--space-16: 4rem; /* 64px */
```

### Bordes y Sombras

```css
--radius-sm: 0.375rem; /* 6px - Badges, tags */
--radius-md: 0.5rem; /* 8px - Botones, cards */
--radius-lg: 0.75rem; /* 12px - Modales, panels */
--radius-xl: 1rem; /* 16px - Feature cards */
--radius-full: 9999px; /* Circular - Avatar, FAB */

--shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.05);
--shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.1);
--shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.1);
--shadow-xl: 0 20px 25px -5px rgb(0 0 0 / 0.1);
```

## Componentes Principales

### 1. Botón de Acción Principal (CTA)

```jsx
<Button variant="primary" size="large" fullWidth>
  Comenzar Carrera
</Button>
```

- **Tamaño**: 56px alto (fácil de tocar mientras corres)
- **Estado loading**: Spinner + texto "Iniciando GPS..."
- **Haptic feedback** en móvil

### 2. Tarjeta de Actividad

```
┌─────────────────────────────────┐
│  Juan Pérez • hace 2h        │
│                                  │
│  Carrera Matutina             │
│  Parque La Carolina, Quito   │
│                                  │
│ ┌─────────────────────────────┐ │
│ │  [Mapa miniatura]           │ │
│ └─────────────────────────────┘ │
│                                  │
│ 5.2 km  •  28:34  •  5:30/km   │
│                                  │
│  23   5   2              │
└─────────────────────────────────┘
```

### 3. Mapa Interactivo

- **Estilo**: Custom Google Maps style (oscuro en dark mode)
- **Controles**: Zoom, ubicación, capas (satélite, terreno)
- **Marcadores**:
  - Corredor en vivo: Pin animado con flecha direccional
  - Inicio/fin de ruta: Marcadores verdes/rojos
  - Puntos de interés: Iconos pequeños

### 4. Panel de Métricas en Vivo

```
┌─────────────────┐
│   5.23 km      │ Número grande
│   Distancia     │ Label pequeño
├─────────────────┤
│ 28:34  • 5:28/km│ Métricas secundarias
└─────────────────┘
```

### 5. Lista de Seguidores/Siguiendo

```
┌────────────────────────────────┐
│  Maria López                 │ Punto rojo = corriendo ahora
│    Corriendo: 2.3 km          │
│    [Ver en vivo] [Perfil]    │
├────────────────────────────────┤
│    Carlos Ruiz                │
│    Última carrera: hace 3h    │
│    [Perfil]                   │
└────────────────────────────────┘
```

## Estilos de Mapa Personalizados

### Light Mode Map Style

- Carreteras: Gris claro (#E5E7EB)
- Parques: Verde suave (#D1FAE5)
- Agua: Azul claro (#BFDBFE)
- Rutas dibujadas: Naranja (#D97706), 4px grosor

### Dark Mode Map Style

- Carreteras: Gris oscuro (#374151)
- Parques: Verde oscuro (#064E3B)
- Agua: Azul oscuro (#1E3A8A)
- Rutas dibujadas: Naranja brillante (#F59E0B), 4px grosor

Estilos JSON para Google Maps disponibles en: `apps/web/src/assets/map-styles/`

## Flujos de Usuario Principales

### Flujo 1: Iniciar y Grabar Carrera

```
Home - [Comenzar Carrera] - Pedir permisos GPS  -
Pantalla de Carrera en Vivo (mapa + métricas)  -
[Opciones: Pausar | Compartir en vivo | Terminar]  -
Resumen de Actividad - [Guardar | Descartar]
```

### Flujo 2: Compartir Ubicación en Vivo

```
Durante carrera - [Compartir en vivo]  -
Modal: "¿Con quién?" - [Todos los seguidores | Grupo específico]  -
Confirmación - Broadcasting... (indicador visible)  -
[Detener compartir] cuando termine
```

### Flujo 3: Seguir a un Corredor en Vivo

```
Feed - Ver post " Juan está corriendo"  -
[Ver en vivo] - Mapa fullscreen con posición de Juan  -
Métricas en tiempo real (distancia, ritmo, tiempo)
```

### Flujo 4: Explorar Rutas Populares

```
Tab Rutas - Mapa con clusters de rutas  -
[Filtros: Distancia, Dificultad, Tipo]  -
Tap en ruta - Detalle (mapa, stats, reviews)  -
[Guardar | Correr esta ruta | Compartir]
```

### Flujo 5: Unirse a Evento

```
Tab Eventos - Lista de eventos cercanos  -
Tap evento - Detalle (fecha, lugar, participantes)  -
[Inscribirse] - Confirmación  -
El día del evento: Notificación - [Ver tracking en vivo de todos]
```

## Consideraciones de Accesibilidad

- **Contraste**: Todos los textos pasan WCAG AA (4.5:1 mínimo)
- **Touch targets**: Mínimo 44x44px en móvil
- **Labels**: Todos los iconos tienen aria-label
- **Navegación por teclado**: Web totalmente navegable con Tab
- **TalkBack/VoiceOver**: Anuncios de métricas durante carrera

## Animaciones y Transiciones

### Principios

- **Rápidas**: 150-300ms para la mayoría
- **Naturales**: Easing curves suaves (cubic-bezier)
- **Útiles**: Solo animar cuando agrega valor, no por decoración

### Ejemplos

```css
/* Botón hover */
transition: all 150ms cubic-bezier(0.4, 0, 0.2, 1);

/* Modal aparece */
animation: slideUp 300ms cubic-bezier(0.16, 1, 0.3, 1);

/* Número cambia (contador) */
animation: countUp 500ms ease-out;
```

## Responsive Design

### Breakpoints

```css
--mobile: 0-640px; /* Diseño primario */
--tablet: 641-1024px; /* 2 columnas */
--desktop: 1025px+; /* 3 columnas, sidebar */
```

### Layout en Web

- **Mobile**: 1 columna, navegación bottom tabs
- **Tablet**: 2 columnas (sidebar + contenido), navegación top
- **Desktop**: 3 columnas (sidebar + feed + detalle), mapa sidebar fijo

## Iconografía

Usar **Heroicons** (outline para navegación, solid para estados activos):

- Inicio: `HomeIcon`
- Rutas: `MapIcon`
- Comunidad: `UsersIcon`
- Eventos: `CalendarIcon`
- Perfil: `UserIcon`
- Iniciar: `PlayIcon`
- Pausar: `PauseIcon`
- Ubicación: `MapPinIcon`
- Live: `SignalIcon`

## Estados de Carga y Vacíos

### Loading States

- **Skeleton screens** en lugar de spinners genéricos
- Placeholder cards con animación shimmer
- Texto: "Cargando rutas cercanas..."

### Empty States

- Ilustración simple + mensaje motivacional
- CTA claro para siguiente acción
- Ejemplo: "¡Tu primera carrera te espera! "

## Notificaciones y Toasts

```
┌────────────────────────────────┐
│ Carrera guardada exitosamente│
└────────────────────────────────┘
  (arriba) Toast: 3s duración, auto-dismiss

┌────────────────────────────────┐
│  Juan López está corriendo   │
│    [Ver en vivo] [Ignorar]    │
└────────────────────────────────┘
  (arriba) Notificación persistente hasta acción
```

## Micro-interacciones

1. **Pull to refresh**: Lista de actividades con indicador de carga
2. **Swipe actions**: Deslizar actividad - [Editar | Eliminar]
3. **Long press**: Mantener en mapa - Crear punto de interés
4. **Double tap**: Mapa - Dar like rápido a actividad
5. **Shake phone**: Durante carrera - Marcar punto de interés

## Próximos Pasos

Ver [ROADMAP.md](ROADMAP.md) para plan de implementación por fases del sistema de diseño.
