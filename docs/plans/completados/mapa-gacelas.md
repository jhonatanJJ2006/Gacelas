# Mapa Gacelas (estilo Uber) y navegación 3D (estilo FSD)

## Hecho

- Estilo propio en todas las pantallas con mapa (carrera en curso, navegación, buscador y detalle de ruta, en vivo, evento en vivo y dibujar ruta): tierra azul marino/carbón, vías apagadas, sin POIs ni transporte, etiquetas sutiles. Web: `ESTILO_GACELAS` en `lib/mapaEstilo.ts`. Android: `Mapas.ESTILO_GACELAS` con `MapStyleOptions`.
- Controles propios (acercar, alejar, brújula para volver al norte) y botón Recentrar cuando el usuario mueve el mapa.
- Recorrido con contorno oscuro, brillo naranja, color por ritmo (web), flechas de dirección y pines de inicio y meta. "Estás aquí" con halo y cono de dirección.
- Navegación 3D: mapa vectorial inclinado 65° con el rumbo hacia arriba, cámara detrás del corredor, ruta planificada brillante por delante y el corredor GLB animado en el mapa (Idle, Jog o Run según el ritmo, gira con el rumbo).
  - Web: `WebGLOverlayView` + three.js en el mismo contexto GL (`components/mapa/Corredor3DMapa.tsx`), pixel ratio limitado a 1,5. Si el navegador no puede usar el mapa vectorial se ve el punto 2D.
  - Android: capa SceneView transparente anclada a la posición proyectada del usuario sobre el `GoogleMap` inclinado. En dispositivos sin GLES 3 o con SwiftShader (emulador) se usa el punto 2D.
- Carrera en curso: interruptor 2D/3D. En web, con clave de Maps, el mapa ocupa toda la pantalla y los datos van en una hoja inferior.
- Sin clave de Maps se mantiene el mapa dibujado de siempre.

## Pendiente del usuario

- Crear un Map ID vectorial en Google Cloud con el estilo Gacelas y ponerlo en `VITE_GOOGLE_MAP_ID` (Vercel). Mientras tanto el modo 3D usa `DEMO_MAP_ID` con esquema oscuro (se ven POIs).
