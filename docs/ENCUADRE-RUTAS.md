# Encuadre de rutas en el mapa

Al elegir o abrir una ruta, la camara se anima hasta mostrarla ENTERA, dentro de la franja que no tapan
listas, hojas inferiores ni botones. Hay un helper por plataforma y todas las pantallas lo usan.

## Reglas

- La ruta completa cabe en el area libre: vista menos padding de cada lado.
- El padding refleja lo que tapa de verdad (cabecera, carrusel, tarjeta); si no se puede medir, se usa un valor fijo.
- Un solo punto (o puntos casi iguales): se centra con un zoom razonable (16 en web, 15.5 en Android).
- La animacion dura 900 ms con arranque y llegada suaves. Con "reducir movimiento" (web) o animaciones del sistema
  apagadas (Android) la camara salta sin animar.
- Si la persona arrastra el mapa durante la animacion, la camara le obedece.
- Las posiciones en vivo (corredores) no arrastran la camara: solo se vuelve a encuadrar cuando cambia cuantos hay.

## Web

- Cuentas puras y animacion: `apps/web/src/shared/lib/encuadre.ts` (`camaraParaPuntos`, `encuadrarRuta`).
- Componente dentro de un mapa: `EncuadrarRuta` en `shared/ui/mapa/EncuadrarRuta.tsx`.
- Medir paneles: `usePaddingPaneles` (`shared/lib/usePaddingPaneles.ts`).
- Usado en: buscador de rutas, detalle de ruta, resumen de carrera, lista de rutas en modo mapa (ruta elegida y todas
  las salidas), mapa en vivo y eventos en vivo, edicion de una ruta ya trazada.
- Pruebas: `apps/web/src/__tests__/encuadre.test.ts`.

## Android

- Cuentas comunes: `EncuadreMapa` en `core/ui` (`camaraParaPuntos`, `paddingDp`).
- MapLibre: `MapLibreMap.encuadrarRuta(...)` y `EncuadrarRutaEfecto(...)` en `app/.../ui/components/EncuadreMapa.kt`.
- `RutasTablero` mide cuanto tapan la cabecera y el carrusel y lo avisa con `alMedirZonaLibre`.
- Usado en: lista de rutas (tablero), detalle de ruta y de carrera (`MapaGacelas` con `paddingEncuadre`), vista previa
  del plan de navegacion, evento en vivo, mapa de seguridad, mapa en vivo y salidas agrupadas.
- Pruebas: `core/ui/src/commonTest/.../EncuadreMapaTest.kt`.

## Pendiente

- iOS (`MapaMapLibre.swift`) conserva su encuadre actual.
