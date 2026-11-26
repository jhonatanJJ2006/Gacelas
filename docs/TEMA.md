# Tema claro y oscuro

Runcity ofrece tres modos de tema en toda la app, web y Android, en la app de usuario y en el panel de administracion:

| Valor     | Efecto                                                                                                         |
| --------- | -------------------------------------------------------------------------------------------------------------- |
| `claro`   | Superficies lavanda y blanco con texto casi negro.                                                             |
| `oscuro`  | El tema historico (por defecto).                                                                               |
| `sistema` | Sigue `prefers-color-scheme` en la web y `isSystemInDarkTheme()` en Android, y reacciona si el sistema cambia. |

La opcion esta en Ajustes, bloque Tema, como selector de tres botones.

## Persistencia

- Local: `Ajustes.tema` en `localStorage['gacelas:ajustes']` (web) y en el almacenamiento local de Ajustes (Android).
- Remota: si existe perfil en Supabase el valor viaja dentro de `user_settings.prefs` (JSON) con la clave `tema`. No hay migracion SQL. Si el remoto no trae `tema` se conserva el local; un valor invalido se ignora.
- Los dos clientes usan los mismos valores (`claro`, `oscuro`, `sistema`) para que el ajuste sea portable.
- Antes `modoOscuro: boolean`; esa clave ya no se escribe. Un ajuste local viejo sin `tema` cae en `oscuro`.

## Web

- Tailwind usa `darkMode: 'class'`. El documento lleva la clase `dark` (oscuro) o `tema-claro` (claro) en `<html>`.
- Los colores son variables CSS en formato `R G B` (`--c-*` en `src/index.css`) que Tailwind consume como `rgb(var(--c-x) / <alpha-value>)`. Cambiar de tema solo cambia el valor de las variables.
- `index.html` trae un script inline que aplica el tema guardado antes del primer pintado para evitar el parpadeo. `TemaDocumento` (en `App.tsx`) mantiene la clase, `color-scheme` y las metas `theme-color` y `color-scheme`.
- `src/shared/lib/tema.ts` concentra la logica (`resolverTema`, `aplicarTema`, `suscribirTema`, `seguirSistema`). `useTemaResuelto()` (`src/shared/ui/useTema.ts`) da el tema resuelto a los componentes que lo necesitan, por ejemplo el mapa.
- Mapas de Google: `estiloMapa(tema)` y `fondoMapa(tema)` en `mapaEstilo.ts`; el mapa vectorial usa `colorScheme` LIGHT o DARK. Android usa MapLibre con `gacelas-claro.json` y `gacelas-oscuro.json`, elegidos con el tema.

### Tokens

Los nombres Tailwind relevantes (todos cambian con el tema):

- Fondos: `bg-c2-bg`, `bg-c2-card`, `bg-c2-s2`, `bg-surface*`.
- Texto: `text-text-primary`, `text-text-secondary`, `text-c2-muted`.
- Bordes: `border-c2-linea`, `border-border-subtle`.
- Acentos usados como texto o icono: `text-c2-violeta`, `text-c2-menta`, `text-c2-cielo`, `text-alerta`, `text-rosa`, `text-peligro-texto`.
- Rellenos de marca fijos (no cambian): `primary-600/700/800`, `danger`, `on-primary` (blanco).
- Texto sobre relleno de color: `text-on-primary` (blanco) sobre botones y degradados morados; `text-c2-sobre-violeta` sobre `bg-c2-violeta` y `bg-primary-200`; `text-c2-sobre-menta` sobre `bg-c2-menta`.
- Vidrio sobre el mapa: `bg-vidrio` con `text-sobre-vidrio`.

Regla de uso: nunca `text-white` ni un hex suelto para el texto de una superficie del tema; usar `text-text-primary`. Para texto sobre un relleno de color, usar `text-on-primary`.

### Zonas siempre oscuras

La clase `zona-oscura` vuelve a declarar las variables oscuras en un subarbol. Se usa donde el diseno es oscuro por definicion en los dos modos: tarjetas KPI del panel de administracion, el buscador de rutas sobre el mapa y la pagina SOS. Dentro de una zona oscura los tokens se comportan como en el tema oscuro.

### Para el trabajo de scrollbars

Las barras de desplazamiento deben leer estos tokens para seguir el tema:

| Token                     | Oscuro     | Claro       | Uso                                    |
| ------------------------- | ---------- | ----------- | -------------------------------------- |
| `--c-scroll-pista`        | 12 19 34   | 243 243 247 | Fondo de la pista.                     |
| `--c-scroll-pulgar`       | 74 64 110  | 140 134 176 | Pulgar en reposo.                      |
| `--c-scroll-pulgar-hover` | 168 85 247 | 122 32 182  | Pulgar al pasar el cursor o arrastrar. |

Ejemplo: `background: rgb(var(--c-scroll-pulgar))`. No hace falta un selector por tema. En claro, el pulgar en reposo tiene 3.1:1 contra la pista (minimo WCAG 1.4.11 para componentes de interfaz); el valor oscuro es previo y queda a criterio de ese trabajo.

## Android

- `GColors.claro` es un estado de Compose. La raiz (`GacelasRaiz`) lo calcula con `ajustes.tema.esClaro(isSystemInDarkTheme())` y todo lo que lea un token se recompone al cambiarlo. `GacelasTheme` arma el `MaterialTheme` con un esquema claro u oscuro y los colores de `GColors`.
- Los tokens de `GColors` son propiedades con getter: `GColors.TextPrimary`, `GColors.C2Card`, `GColors.Surface*`, etc. cambian con el modo. No guardar un token en un `val` de objeto o de archivo (`val X = GColors.Y` se evalua una sola vez): usar `val X get() = GColors.Y`.
- `GColors.OnPrimary` es el blanco para texto sobre rellenos de marca (botones, degradado `GradienteC2`); `GColors.SobreVioleta` es el texto sobre `C2Violeta` (oscuro en oscuro, blanco en claro).
- `Tablero` (fondos, vidrio y bordes del tablero) tiene su variante clara. Las barras del sistema cambian el color de sus iconos con el tema.
- Los mapas reciben `oscuro = !GColors.claro` por defecto y cambian de estilo en caliente.
- Selector de tema: `SelectorTema` en `AjustesContenido.kt` (etiquetas `tema-claro`, `tema-oscuro`, `tema-sistema`).
- Capturas sin emulador: `TemaClaroOscuroTest` (Robolectric y Roborazzi). Ejemplo: `./gradlew :app:testDebugUnitTest --tests com.gacelas.TemaClaroOscuroTest -Pgacelas.shots=/ruta -Proborazzi.test.record=true`.

## Contraste (WCAG AA) del modo claro

Relaciones de contraste calculadas con la formula de luminancia relativa de WCAG 2.1. El minimo AA es 4.5:1 para texto normal y 3:1 para texto grande y componentes de interfaz.

| Color                       | Sobre fondo (243 243 247) | Sobre tarjeta (255 255 255) | Sobre `c2-s2` (234 234 241) |
| --------------------------- | ------------------------- | --------------------------- | --------------------------- |
| Texto principal (22 18 38)  | 16.52                     | 18.28                       | 15.27                       |
| Texto secundario (79 76 97) | 7.47                      | 8.27                        | 6.91                        |
| Violeta (122 32 182)        | 7.00                      | 7.75                        | 6.47                        |
| Primary (110 29 165)        | 7.97                      | 8.82                        | 7.36                        |
| Menta (9 113 79)            | 5.44                      | 6.02                        | 5.03                        |
| Cielo (18 87 165)           | 6.47                      | 7.16                        | 5.98                        |
| Peligro (167 27 57)         | 6.64                      | 7.35                        | 6.13                        |
| Alerta (149 81 4)           | 5.49                      | 6.08                        | 5.07                        |
| Rosa (156 17 87)            | 7.21                      | 7.98                        | 6.66                        |

Texto blanco sobre rellenos: violeta 7.75, `#7B1FA2` 8.20, menta 6.02 y peligro 7.35. Texto principal sobre el fondo del mapa claro (238 238 244): 15.82.

## Decisiones y limites

- El tema por defecto sigue siendo oscuro; Sistema es opcional.
- El naranja `#FF6A13` del marcador de ubicacion no se toco.
- Los degradados y rellenos de marca (`#7B1FA2` y relacionados) son iguales en los dos modos para conservar la identidad.
- El mapa de Google no se verifico visualmente por falta de clave de Maps en el entorno de desarrollo; el estilo claro se cubre con una prueba de las reglas.
- Al arrancar en Android la intro y el primer cuadro usan el tema oscuro hasta que se lee el ajuste guardado.
- Las pantallas de bienvenida e inicio de sesion de la web conservan su fondo de mapa oscuro decorativo con la tarjeta en el tema elegido.
