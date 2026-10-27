# Marca: icono e intro animada

Elegidos por Jhona el 26 de octubre de 2026: icono propuesta A "Gacela en ruta" e intro A "Ruta y destello" (estilo Netflix, 2,8 s). El mismo dia la intro paso a ser el video "Ruta y destello v2b" con la palabra RUNCITY (2,2 s).

## Paleta y tipografia

| Uso                                         | Color                 |
| ------------------------------------------- | --------------------- |
| Morado principal                            | `#7B1FA2`             |
| Morado brillante (ruta, brillo)             | `#A855F7`             |
| Morado claro                                | `#C084FC`             |
| Lila                                        | `#E9D5FF` y `#F3E8FF` |
| Fondo de marca (splash, intro, theme-color) | `#0C1322`             |
| Fondo profundo                              | `#070E1D`             |
| Acento GPS                                  | `#10B981`             |

Titulares en Anton cursiva (la palabra RUNCITY de la intro) y textos en Archivo. Desde la version 0.8.4 los titulares Anton llevan 0.04em mas de espaciado entre letras que el diseno original (por ejemplo 0.03em en lugar de -0.01em) y sin tracking negativo, para que se lean mejor. Se probo Saira Semi Condensed en la 0.8.3 y se descarto. Ver tambien docs/PALETA.md.

## Icono

Fuentes en `assets/marca/`: `icono.svg` (completo), `fondo.svg` y `primer_plano.svg` (capas del icono adaptativo, lienzo 108 x 108 con zona segura de 66).

- Fondo: degradado violeta a azul noche, halo morado y montanas de Loja.
- Primer plano: gacela en salto con estelas de velocidad sobre una ruta GPS punteada que termina en un punto verde.
- Monocromo (iconos tematicos de Android 13): gacela, estelas y ruta en una sola tinta.

![Icono adaptativo: circulo, squircle, monocromo y splash](capturas/marca/icono-adaptativo.png)

### Archivos generados

| Plataforma | Archivo                                                                                                                |
| ---------- | ---------------------------------------------------------------------------------------------------------------------- |
| Android    | `res/drawable/ic_launcher_background.xml`, `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml` (VectorDrawable) |
| Android    | `res/mipmap-anydpi-v26/ic_launcher.xml` y `ic_launcher_round.xml` (adaptativo)                                         |
| Android    | `res/mipmap-{mdpi..xxxhdpi}/ic_launcher.png` y `ic_launcher_round.png` (Android 7)                                     |
| Android    | `res/drawable/splash_icono.xml` (icono del SplashScreen)                                                               |
| Play Store | `assets/marca/play_store_512.png`                                                                                      |
| Web        | `public/favicon.svg`, `favicon.ico`, `favicon-32.png`, `apple-touch-icon.png`                                          |
| Web (PWA)  | `public/icon-192.png`, `icon-512.png`, `icon-maskable-512.png` y `manifest.webmanifest`                                |

La notificacion de la carrera usa `ic_launcher_monochrome` como icono pequeno.

### Regenerar

Desde la raiz del repo, con Python 3, Pillow y Playwright (chromium):

```
python3 tools/marca/generar.py
```

Si se cambia el dibujo, editar los SVG de `assets/marca/` y las constantes del script (trazo de la gacela y de la ruta), que convierten los SVG a VectorDrawable. VectorDrawable no tiene filtros: el brillo neon de la ruta se aproxima con un halo semitransparente y la sombra de la gacela con una copia desplazada.

## Intro "Ruta y destello v2b" (video)

Aprobada por Jhona el 26 de octubre de 2026 (reemplaza a la intro A dibujada en tiempo real). Una ruta morada luminosa con un cometa entra directo, sin preambulo, estalla en un destello anamorfico al llegar al centro, revela la gacela con un brillo que la cruza y la palabra RUNCITY en Anton con espaciado amplio que se cierra, y se funde al fondo `#0C1322`. Dura 2,2 s.

Es un video pre-renderizado cuadro por cuadro a 60 fps (motion blur con 4 submuestras por cuadro, easing cuidado, grano leve contra el banding) con `tools/marca/render_intro.py b <carpeta>` (requiere `skia-python`, `numpy`, `pillow` y la fuente Anton). Exportacion, 720 x 1280, sin audio:

| Archivo                                       | Formato            | Peso   |
| --------------------------------------------- | ------------------ | ------ |
| `apps/android/app/src/main/res/raw/intro.mp4` | H.264 main, CRF 24 | 102 KB |
| `apps/web/public/marca/intro.mp4`             | el mismo MP4       | 102 KB |
| `apps/web/public/marca/intro.webm`            | VP9, CRF 34        | 112 KB |

El primer y el ultimo cuadro son el fondo de marca `#0C1322`, igual que el SplashScreen, `windowBackground` y el theme-color de la web.

![Cuadros clave de la intro v2b con RUNCITY](capturas/marca/intro-v2b-runcity.jpg)

### Por que se cambio la intro A

- Corte entre splash e intro: el splash mostraba la gacela sobre `#0C1322` plano y la intro empezaba con un degradado violeta sin gacela; la gacela volvia recien a los 1,35 s.
- Tirones: la palabra se volvia a medir en cada cuadro de su entrada, el destello creaba un pincel nuevo por cuadro y la app se montaba durante la animacion en el hilo principal.
- Fuente: la intro dependia de la familia de titulares en Compose y, en web, de que la fuente del canvas ya estuviera cargada (si no, se dibujaba con Impact y saltaba a mitad).
- Web: `shadowBlur` en canvas es caro en moviles y la intro solo aparecia cuando ya habia cargado el bundle de React.

Un video se decodifica por hardware y se compone fuera del hilo principal, asi que lo que se ve es exactamente lo aprobado, en cualquier telefono.

### Cuando se muestra (regla comun Android y web)

La intro se ve cada vez que se abre la app:

- arranque en frio (proceso nuevo);
- volver a abrirla despues de salir con atras (la actividad se destruye pero el proceso sigue vivo);
- volver desde la pantalla de inicio o desde recientes con el proceso vivo.

Excepcion: si la app estuvo fuera de primer plano menos de 30 s (`UMBRAL_AUSENCIA_MS`), no se repite. Asi un cambio rapido de app, abrir una notificacion o rotar la pantalla no la vuelve a lanzar. Nunca se muestra con las animaciones del sistema desactivadas (Android: escala 0; web: `prefers-reduced-motion`).

### Android

1. `Theme.Gacelas.Arranque` (SplashScreen API) muestra el fondo `#0C1322` con la gacela. Con intro, `MainActivity` retiene el splash con `setKeepOnScreenCondition` hasta que el video pinta su primer cuadro (`onRenderedFirstFrame`), como mucho 1 s (`IntroArranque.ESPERA_PRIMER_CUADRO_MS`). Al soltarlo, `setOnExitAnimationListener` desvanece la gacela del splash en 180 ms sobre el mismo `#0C1322` con que empieza el video: no hay salto de color ni pantalla en blanco.
2. `MainActivity` decide en `onCreate` (y en cada `onStart` posterior) con `IntroArranque.alEntrar(ahora, escalaAnimaciones)`, y registra la salida en `onStop` con `IntroArranque.alSalir(ahora)`. El reloj es `SystemClock.elapsedRealtime()`. La regla pura es `IntroArranque.debeMostrar(ahora, ultimaSalida, escala)`.
3. `IntroVideo` (`app/.../intro/IntroVideo.kt`) reproduce `res/raw/intro.mp4` con Media3 ExoPlayer en un `PlayerView` a pantalla completa (`RESIZE_MODE_ZOOM`, sin controles, volumen 0, obturador y fondo `#0C1322`). `ControlIntro` garantiza que el aviso de primer cuadro y el fin ocurran una sola vez.
4. La intro termina al llegar a `STATE_ENDED`, con un toque, si el reproductor da error o no se puede crear, si no hay primer cuadro en 1 s (fallback) o por un tope de seguridad de 3,7 s. Siempre suelta el splash.
5. La app (`GacelasRaiz`: sesion, Supabase, NavHost, mapas) se monta debajo cuando el video ya pinto su primer cuadro y el contenedor de dependencias esta listo (`GacelasApp.esperarContenedor()`). La preparacion del mapa sin conexion se aplaza 3 s. Al terminar la intro, la pantalla que corresponda ya esta lista.
6. `app/src/main/baseline-prof.txt` con `androidx.profileinstaller` sigue compilando AOT el arranque y la primera pantalla.

iOS todavia no muestra la intro. Para hacerlo, `MainViewController` puede reproducir el mismo MP4 con `AVPlayerLayer` y la misma regla de `IntroArranque` (codigo comun en `core/ui`).

`core/ui/.../ui/intro/Marca.kt` conserva la paleta y la silueta de la gacela que usa el icono dibujado en la app.

#### Causa del fallo de la v0.8.1 (corregido en la v0.8.2)

- Solo salia la primera vez: la regla usaba una bandera por proceso (`mostradaEnProceso`). Al salir con atras Android conserva el proceso, asi que al reabrir la bandera seguia activa.
- Pantalla oscura un buen rato: la intro se componia en el mismo cuadro que toda la app. Ademas `windowBackground` (`#111827`) no coincidia con el splash (`#0C1322`).

### Web

La intro es un `<video autoplay muted playsinline preload="auto">` en `index.html`, fuera de React, dentro de `#intro` (fondo `#0C1322`, `object-fit: cover`, `data-testid="intro-animada"`), con WebM VP9 y MP4 de respaldo (`preload="auto"`). Se ve antes de que cargue el bundle de la app.

- Un script en linea en `index.html` aplica la misma regla que `debeMostrarIntro` (`src/shared/lib/intro.ts`): se muestra al abrir salvo si se salio hace menos de 30 s, y nunca con `prefers-reduced-motion: reduce`. `localStorage`, clave `gacelas.intro.vista`, guarda el instante de la ultima salida.
- `src/shared/lib/introVideo.ts` (`iniciarIntroVideo`, cargado por su propia entrada diminuta `src/app/introArranque.ts`, antes y sin esperar a `main.tsx`) la oculta al terminar (`ended`), con clic, toque o cualquier tecla, si el video falla, si no hay primer cuadro en 1 s (fallback, por ejemplo autoplay bloqueado) o por un tope de 3,7 s; y la repite al volver a la pestana tras 30 s o mas fuera (`visibilitychange`, `pagehide`).
- La app carga debajo mientras tanto.

## Logo dentro de la app

El logo anterior (cuadro naranja con el corredor de Material o el simbolo `sprint`, y el PNG naranja `82a3eb8346b4.png` / `compartir_logo.png`) se reemplazo por el icono A en todas las pantallas:

- Android: `IconoGacelas` y `MarcaGacelasA` (core/ui `components/IconoGacelas.kt`), dibujados en Compose con la misma gacela que la intro. Se usan en `MarcaGacelas`, la cabecera de acceso (`MarcaC4`: bienvenida, inicio de sesion, registro), la cabecera del modal de actualizacion, el dialogo Acerca de e `InicioComun` (iOS).
- Web: `IconoGacelas` y `LogoGacelas` (`src/shared/ui/LogoGacelas.tsx`, usa `/favicon.svg`) en bienvenida, inicio de sesion y registro.
- Imagenes de compartir: `compartir_logo.png` (Android) y `public/marca/logo-compartir.png` (web), generados desde el icono A.
- Tarjeta Open Graph `public/og-default.png` (1200 x 630): icono A con la palabra GACELAS.
- Las plantillas de correo (`supabase/templates`) no llevan logo grafico.

Todo sale de `python3 tools/marca/generar.py`. Capturas antes y despues: `/workspace/gacelas/marca/implementado/logo-interno/`.

## Pruebas

| Prueba                             | Que verifica                                                                                                                                    |
| ---------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| `core/ui` `IntroArranqueTest`      | arranque en frio, regla de 30 s, ciclo salir y volver, animaciones desactivadas, saltar, duracion, espera de 1 s                                |
| `app` `IntroMarcaTest` (Roborazzi) | `ControlIntro` (un solo fin, suelta el splash), fallback sin primer cuadro, salto con toque, fondo igual al splash, capturas                    |
| web `introVideo.test.ts`           | regla de 30 s, fin por `ended`, salto con clic y tecla, fallback sin primer cuadro y por error, tope de seguridad                               |
| web `e2e/intro.spec.ts`            | aparece sobre `#0C1322`, se salta, no reaparece al recargar enseguida, termina sola, fallback sin video, movimiento reducido, manifest e iconos |

Los demas e2e dan la intro por vista (`e2e/ayuda.ts`). Capturas de Roborazzi: `./gradlew :app:testDebugUnitTest --tests '*IntroMarca*' -Proborazzi.test.record=true -Pgacelas.shots=<carpeta>`.
