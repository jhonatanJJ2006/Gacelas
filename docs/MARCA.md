# Marca: icono e intro animada

Elegidos por Jhona el 26 de octubre de 2026: icono propuesta A "Gacela en ruta" e intro A "Ruta y destello" (estilo Netflix, 2,8 s).

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

Titulares en Anton (la palabra GACELAS de la intro) y textos en Archivo. Ver tambien docs/PALETA.md.

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

## Intro animada "Ruta y destello"

Una ruta morada luminosa se dibuja con un punto verde en la punta (0 a 1,3 s), estalla en un destello (1,45 s), se convierte en la gacela con la palabra GACELAS, hace zoom hacia dentro (2,2 s) y se funde al fondo de la app (2,8 s). Lienzo de diseno de 1080 x 1920 encajado en la pantalla. El prototipo original esta en `assets/marca/intro-prototipo.html?v=A`.

![Cuadros 0,6 / 1,2 / 1,45 / 1,9 / 2,4 s: MP4 aprobado, web y Android](capturas/marca/intro-mp4-web-android.jpg)

### Cuando se muestra (regla comun Android y web)

La intro se ve cada vez que se abre la app:

- arranque en frio (proceso nuevo);
- volver a abrirla despues de salir con atras (la actividad se destruye pero el proceso sigue vivo);
- volver desde la pantalla de inicio o desde recientes con el proceso vivo.

Excepcion: si la app estuvo fuera de primer plano menos de 30 s (`UMBRAL_AUSENCIA_MS`), no se repite. Asi un cambio rapido de app, abrir una notificacion o rotar la pantalla no la vuelve a lanzar. Nunca se muestra con las animaciones del sistema desactivadas (Android: escala 0; web: `prefers-reduced-motion`).

### Android (y iOS via KMP)

1. `Theme.Gacelas.Arranque` (SplashScreen API) muestra el fondo `#0C1322` con la gacela mientras arranca el proceso. No usa `setKeepOnScreenCondition`: se cierra en el primer cuadro. `Theme.Gacelas` usa el mismo `#0C1322` como `windowBackground`, y la intro empieza con ese color, asi el paso del splash a la intro es continuo.
2. `MainActivity` decide en `onCreate` (y en cada `onStart` posterior) con `IntroArranque.alEntrar(ahora, escalaAnimaciones)`, y registra la salida en `onStop` con `IntroArranque.alSalir(ahora)`. El reloj es `SystemClock.elapsedRealtime()`. La regla pura es `IntroArranque.debeMostrar(ahora, ultimaSalida, escala)`.
3. Con intro, la app (`GacelasRaiz`: sesion, Supabase, NavHost, mapas) se monta despues de los dos primeros cuadros de la intro y cuando el contenedor de dependencias ya esta construido en segundo plano (`GacelasApp.esperarContenedor()`). La preparacion del mapa sin conexion se aplaza 3 s. Al terminar la intro, la pantalla que corresponda ya esta lista debajo.
4. `IntroAnimada` (core/ui, `commonMain`) dibuja en un `Canvas`: el tiempo se lee solo en la fase de dibujo (sin recomposiciones por cuadro), la ruta, los trazos, los pinceles y el texto medido se reutilizan entre cuadros (sin asignaciones en el bucle de dibujo) y el brillo se imita con trazos semitransparentes (sin blur).
5. Dura 2,8 s de reloj (no se estira con la escala de animaciones) y un toque la salta.
6. `app/src/main/baseline-prof.txt` con `androidx.profileinstaller`: el arranque, la intro y la primera pantalla se compilan AOT al instalar, sin esperar al JIT.

El codigo de la intro es comun (`core/ui/src/commonMain/.../ui/intro/`), listo para usarse en `MainViewController` de iOS; por ahora iOS no la muestra.

#### Causa del fallo de la v0.8.1 (corregido en la v0.8.2)

- Solo salia la primera vez: la regla usaba una bandera por proceso (`mostradaEnProceso`). Al salir con atras Android conserva el proceso, asi que al reabrir la bandera seguia activa.
- Pantalla oscura un buen rato: la intro se componia en el mismo cuadro que toda la app. El primer cuadro esperaba a `GacelasRaiz`, que pedia el contenedor de dependencias (Supabase) mientras otro hilo lo estaba construyendo, y a la preparacion del mapa sin conexion en `onCreate`. Ademas `windowBackground` (`#111827`) no coincidia con el splash (`#0C1322`).

### Web

`src/shared/ui/IntroAnimada.tsx` (montada en `main.tsx`) y `src/shared/lib/intro.ts` (logica y dibujo en canvas 2D, misma animacion):

- Misma regla: se muestra al abrir la app y al volver a la pestana tras 30 s o mas fuera. `localStorage`, clave `gacelas.intro.vista`, guarda el instante de la ultima salida (`visibilitychange` y `pagehide`).
- Se omite con `prefers-reduced-motion: reduce`.
- Se salta con clic, toque o cualquier tecla. La app carga debajo mientras tanto.
- El lienzo usa como maximo 1,5x de densidad para que el brillo no sea pesado.

## Logo dentro de la app

El logo anterior (cuadro naranja con el corredor de Material o el simbolo `sprint`, y el PNG naranja `82a3eb8346b4.png` / `compartir_logo.png`) se reemplazo por el icono A en todas las pantallas:

- Android: `IconoGacelas` y `MarcaGacelasA` (core/ui `components/IconoGacelas.kt`), dibujados en Compose con la misma gacela que la intro. Se usan en `MarcaGacelas`, la cabecera de acceso (`MarcaC4`: bienvenida, inicio de sesion, registro), la cabecera del modal de actualizacion, el dialogo Acerca de e `InicioComun` (iOS).
- Web: `IconoGacelas` y `LogoGacelas` (`src/shared/ui/LogoGacelas.tsx`, usa `/favicon.svg`) en bienvenida, inicio de sesion y registro.
- Imagenes de compartir: `compartir_logo.png` (Android) y `public/marca/logo-compartir.png` (web), generados desde el icono A.
- Tarjeta Open Graph `public/og-default.png` (1200 x 630): icono A con la palabra GACELAS.
- Las plantillas de correo (`supabase/templates`) no llevan logo grafico.

Todo sale de `python3 tools/marca/generar.py`. Capturas antes y despues: `/workspace/gacelas/marca/implementado/logo-interno/`.

## Pruebas

| Prueba                             | Que verifica                                                                                                |
| ---------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| `core/ui` `IntroArranqueTest`      | arranque en frio, regla de 30 s, ciclo salir y volver, animaciones desactivadas, saltar, duracion           |
| `app` `IntroMarcaTest` (Roborazzi) | cuadros clave de la intro, fin por tiempo, salto con toque, capturas del icono adaptativo                   |
| web `IntroAnimada.test.tsx`        | regla de 30 s, salto con clic y tecla, movimiento reducido                                                  |
| web `e2e/intro.spec.ts`            | aparece, se salta, no reaparece al recargar enseguida, termina sola, movimiento reducido, manifest e iconos |

Los demas e2e dan la intro por vista (`e2e/ayuda.ts`). Capturas de Roborazzi: `./gradlew :app:testDebugUnitTest --tests '*IntroMarca*' -Proborazzi.test.record=true -Pgacelas.shots=<carpeta>`.
