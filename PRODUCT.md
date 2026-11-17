# Product

<!-- impeccable:product-schema 1 -->

## Platform

adaptive

La app principal es Android (Kotlin, Compose Multiplatform compartido con iOS en `apps/android`). La web (`apps/web`, React + Vite + Tailwind) es un complemento con las mismas pantallas y un shell de escritorio.

## Users

- Runner casual y social (perfil principal): 20 a 35 años, corre 2 a 4 veces por semana, 5 a 10 km por salida. Quiere compartir logros, encontrar compañeros y descubrir rutas.
- Runner competitivo: 25 a 45 años, entrena 5 a 6 días por semana y prepara carreras. Quiere análisis de ritmo, comparación y eventos competitivos.
- Organizador de club o evento: coordina salidas grupales, gestiona su club y sigue a los corredores en vivo durante un evento.

Todos corren en Loja, Ecuador, casi siempre con el teléfono en la mano o en el brazo.

## Product Purpose

Gacelas convierte el running en una actividad compartida: grabar carreras con GPS, descubrir y dibujar rutas locales, seguir a amigos en vivo y unirse a eventos y clubes. El éxito es que un corredor salga a correr más seguido y con más gente.

## Positioning

Una red social de running hecha para Loja: rutas reales de la ciudad (Jipiro, Zamora, Ciudadela Universitaria), la comunidad local de corredores y eventos de clubes de la ciudad, con privacidad de ubicación por defecto.

## Operating Context

- Se usa en movimiento: antes de salir, durante la carrera (pantalla encendida, GPS activo, navegación por la ruta) y después para guardar y compartir.
- La hora favorita es el amanecer.
- La web se usa sobre todo para revisar el historial, planear rutas y gestionar la comunidad desde el escritorio.

## Capabilities and Constraints

- Grabación de carreras con GPS, cronómetro e intervalos; resumen y compartir.
- Rutas: explorar, guardar, dibujar, navegar con el corredor 3D sobre el mapa, uso sin conexión.
- Comunidad: feed estilo red social, eventos, clubes, corredores, en vivo y notificaciones.
- Perfil con rachas diaria y semanal y estadísticas.
- Backend Supabase; plan gratuito con contrato de capacidad (ver `docs/FREE_TIER_BUDGET.md`).
- Principio "Grab and Go": empezar a correr en dos toques como máximo.

## Brand Commitments

- Nombre: Gacelas (Gacelas Loja). Voz en español, cercana y directa.
- El corredor 3D (modelo GLB compartido entre web, Android e iOS) es un elemento propio de la marca.
- Paleta inspirada en el amanecer: naranja como color principal y verde bosque como secundario, sobre un fondo azul noche (ver `docs/DESIGN.md`).
- Sin emojis en documentación ni en commits (ver `AGENTS.md`).

## Evidence on Hand

- Datos de ejemplo en `MockData` (Android) y en los fixtures de la web; no hay testimonios, clientes ni métricas reales publicadas. No inventarlos.
- Capturas de pantalla en `docs/capturas/`.

## Product Principles

1. Correr primero: la acción de empezar a correr siempre está a mano.
2. Local antes que global: rutas, eventos y corredores de Loja primero.
3. Privacidad por defecto: ubicación en vivo apagada y puntos de inicio y fin difuminados.
4. Datos verificables: distancia, ritmo y tiempo exactos, sin adornos.

## Accessibility & Inclusion

Tocar con una mano y en movimiento: botones de al menos 48 px de alto en las acciones de carrera, contraste suficiente al sol y respeto de "reducir movimiento" del sistema.
