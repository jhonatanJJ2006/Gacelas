# Opcion Piedra Clara

Segunda direccion visual para Runcity. No reemplaza la direccion vigente (noche violeta, vidrio y Anton). Nada de esta carpeta entra en el APK ni en la web: no hay flavor de Gradle, y ningun `src/main` ni `apps/web` la referencia.

## En que se diferencia

La app de hoy es un tablero nocturno: fondo `#0C1322`, widgets de vidrio morado, titulares en Anton cursiva, dock flotante con CORRER en degradado, y una gacela en salto con estela, ruta GPS punteada y montanas de Loja. La intro dibuja ese contorno en neon y compone RUNCITY letra a letra.

Piedra Clara es un cuaderno de dia, pensado como un plano impreso de Loja:

|            | Direccion vigente                   | Piedra Clara                                      |
| ---------- | ----------------------------------- | ------------------------------------------------- |
| Superficie | Noche violeta, vidrio               | Papel `#F3EBDF`, tarjetas solidas                 |
| Acento     | Morado `#7B1FA2` / `#C084FC`        | Arcilla `#A33C22`, cobre `#C4623A`, rio `#1E6460` |
| Titulares  | Anton, cursiva, mayusculas          | Fraunces, romana                                  |
| Texto      | Archivo                             | Outfit                                            |
| Marca      | Gacela en salto, estela y punto GPS | Gacela de pie, de perfil, cuerno de cobre         |
| Mapa       | Mapa vivo a sangre, de noche        | Plano impreso dentro de una postal                |
| Navegacion | Dock flotante, CORRER a la derecha  | Barra de papel, Correr al centro                  |
| Intro      | Trazo neon y palabra en Anton       | Sello que se asienta y nombre compuesto           |

## Como mirarla

Las pantallas estan en `prototipo/index.html`. Abre el archivo en un navegador: Inicio, Correr, la carrera, el resumen, Rutas y el perfil se recorren con la barra y con los botones (Empezar, Terminar, SOS, 2D/3D, filtros del carrusel).

La intro animada es `prototipo/intro.html`. Carga el JSON por red, asi que hace falta servir la carpeta (`python3 -m http.server` dentro de `design/alt-option`). El video ya exportado esta en `previews/intro.mp4` y `previews/intro.gif`.

## Archivos

| Ruta                                                                                 | Que es                                                                                                                |
| ------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------- |
| `tokens.css`                                                                         | Color y radios                                                                                                        |
| `prototipo/index.html`                                                               | Prototipo de las seis pantallas, en espanol                                                                           |
| `prototipo/intro.html`                                                               | Reproductor de la intro                                                                                               |
| `marca/logo.svg`, `previews/logo.png`                                                | Logo horizontal                                                                                                       |
| `marca/logo-vertical.svg`, `previews/logo-vertical.png`                              | Sello con el nombre                                                                                                   |
| `marca/gacela.svg`                                                                   | Simbolo suelto                                                                                                        |
| `marca/icono.svg`, `previews/icono-512.png`                                          | Icono compuesto, 512 px                                                                                               |
| `marca/icono-fondo.svg`, `marca/icono-primer-plano.svg`, `marca/icono-monocromo.svg` | Capas del icono adaptativo                                                                                            |
| `previews/icono-mascaras.png`                                                        | Cuadrado, circulo, squircle y una tinta                                                                               |
| `android/`                                                                           | VectorDrawable, icono adaptativo y animated-vector. Listos para copiar a un `res/` de un flavor que todavia no existe |
| `motion/intro-sello.json`                                                            | Intro en Lottie, 1080 x 1920, 30 fps, 3 s                                                                             |
| `previews/*.png`                                                                     | Pantallas, logo e icono                                                                                               |
| `previews/intro.mp4`, `intro.gif`, `intro-tira.png`                                  | Intro y tira de contacto                                                                                              |

Las fuentes Fraunces y Outfit estan en `prototipo/fuentes/` bajo la licencia SIL OFL (los textos de licencia van junto a los archivos). El reproductor `prototipo/vendor/lottie.min.js` es lottie-web, licencia MIT.

## Por que no se publica sola

`apps/android` solo empaqueta `src/main` (y los flavors que declare el `build.gradle`). Esta carpeta esta fuera de esos directorios. Para probarla en un telefono habria que declarar un flavor nuevo y copiar `android/` a `src/<flavor>/res/`. Eso no esta hecho, a proposito.
