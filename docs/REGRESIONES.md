# Regresiones

Registro de fallos que ya se habian corregido y volvieron. Cada entrada dice que paso, la causa, el arreglo y como evitar que se repita.

## 2026-10-27: el corredor 3D de Inicio no se dibuja en Android desde 0.9.1 (v0.9.5)

### Cambio que lo introdujo

v0.9.0 a v0.9.1, `InicioScreen.kt`: `Runner3D(..., sobreFondo = true)`. En `Runner3D.kt` eso hacia `disponible = !sobreFondo && ...`, asi que Inicio dejaba de usar Filament (que en 0.9.0 se veia en el telefono de Jhona, sobre fondo negro) y pasaba siempre al WebView transparente con three.js. Ese WebView solo se habia probado en el emulador; en su telefono no pinta nada. 0.9.3 y 0.9.4 solo agregaron respaldos.

### Arreglo

- `motorCorredor`: con Filament disponible Inicio usa `Runner3DFilamentTransparente`. El WebView queda solo para equipos sin Filament.
- `Runner3DFilamentTransparente`: Filament (ModelViewer) en un `TextureView` con `isOpaque = false`, vista `BlendMode.TRANSLUCENT`, limpieza con alfa 0 y sin skybox. El TextureView se compone como una vista normal: escenario detras, etiquetas y boton delante. El SurfaceView de SceneView perforaba la ventana en negro.
- El GLB se lee de los assets del APK (sin red).
- El respaldo sigue debajo hasta que el TextureView entrega un cuadro con pixeles.

### Como evitar que se repita

- `MotorCorredorTest` falla si Inicio vuelve al WebView teniendo Filament.
- Un cambio de motor de render se prueba en un telefono real, no solo en el emulador.

## 2026-10-27: el avatar sigue sin verse en Inicio y tampoco aparece el respaldo (v0.9.4)

### Que paso

Despues de 0.9.3 el escenario de Inicio seguia vacio, sin corredor y sin icono de respaldo. La captura nueva tiene barra de estado y barra de inicio de iPhone, asi que puede ser la app de iOS.

### Diff que lo introdujo

v0.9.0 a v0.9.1, `InicioScreen.kt`: `Runner3D(..., sobreFondo = true)`. Con eso Inicio dejo de usar Filament (SurfaceView, que en 0.9.0 si se veia sobre fondo negro) y paso a un WebView transparente. En iOS el corredor ya era un render de SceneKit dibujado como imagen de Compose.

### Causa

En las dos plataformas el respaldo dependia de un error que nunca llega:

- Android: el respaldo de 0.9.3 solo aparecia si el proceso del WebView moria o si la pagina no marcaba `listo` (modelo cargado). Un WebView que carga bien pero pinta transparente pasa ese control y deja el hueco vacio.
- iOS: `Runner3DIos` solo muestra el respaldo si el GLB no carga. Si SceneKit devuelve cuadros vacios (modelo fuera de camara o render sin contenido) se dibuja una imagen transparente.

Sin dispositivo no se pudo ver cual de los dos renders pinta vacio en el telefono de Jhona.

### Arreglo

- El respaldo se dibuja debajo del 3D hasta recibir una senal positiva: un cuadro que ya tiene pixeles del corredor.
- Android: `runner3d.js` lee los pixeles despues de dibujar y llama a `GacelasCorredor.primerCuadro()` (JavascriptInterface).
- iOS: `Runner3DIos` revisa el canal alfa de cada cuadro de SceneKit.

### Como evitar que se repita

- Un respaldo nunca debe depender de que algo falle: debe quedarse hasta que el 3D confirme que dibujo.
- Test `Runner3DWebRespaldoTest`: falla con el codigo de 0.9.3 y pasa con 0.9.4.

## 2026-10-26: el avatar 3D no aparece en Inicio (Android, v0.9.1 y v0.9.2)

### Que paso

En Inicio se veia el escenario morado pero no el corredor 3D. No aparecia tampoco el icono de respaldo, asi que la vista 3D existia pero no se dibujaba.

### Causa

En v0.9.1 el escenario C2 recorto todo su contenido con una forma no rectangular (`clip(FormaEscenarioC2)`). El corredor en Android es un `WebView` transparente, y en muchos dispositivos un `WebView` dentro de un recorte de forma no se pinta. El GLB, el HTML y el JS ya venian dentro del APK y se servian desde los assets, asi que la red no tenia nada que ver. No se pudo reproducir en un dispositivo real (el emulador no arranca en el entorno de CI local); es la causa mas probable y por eso tambien se agrego un respaldo.

### Arreglo (v0.9.3)

- La forma solo recorta el fondo y el texto del escenario. El corredor 3D queda dentro de un recorte rectangular (`clipToBounds`).
- `Runner3DWeb` cambia al icono de respaldo si el proceso de render del WebView muere, o si a los 10 s la escena no marca `data-listo="1"`.
- Web: nuevo service worker (`public/sw.js`). Guarda la app, el GLB y las fuentes en la primera visita y los sirve sin red.
- iOS ya empaquetaba el GLB en el bundle (`project.yml`), sin cambios.

### Como evitar que se repita

- No poner `WebView`, `SurfaceView` ni otras vistas nativas dentro de un `clip` con forma no rectangular.
- Prueba e2e `apps/web/e2e/v093.spec.ts`: primera visita con red, luego `setOffline(true)` y recarga; el corredor tiene que quedar listo.

## 2026-10-25: Google se queda cargando tras elegir la cuenta (Android)

### Que paso

En Android, al pulsar "Continuar con Google" y elegir la cuenta, la app se quedaba en la pantalla de carga y no entraba. Ya habia pasado en v0.5.x y se corrigio registrando el cliente OAuth Android de `com.gacelas` con la SHA-1 de la firma y usando el client ID web como `serverClientId`.

### Diagnostico

- Supabase no recibio ninguna peticion de inicio de sesion a la hora del intento (23:46 hora de Ecuador): no hay entradas en los logs de edge ni de auth, ni sesiones nuevas. El fallo ocurre antes de llegar al servidor, en el paso de Google (Credential Manager).
- La web no esta afectada: `/auth/v1/authorize` redirige bien a Google con el cliente web y las Redirect URLs siguen incluyendo `https://gacelas.vercel.app/**`.
- Las migraciones 019 a 032 de esa noche no tocan `auth.users`: el unico trigger sigue siendo `on_auth_user_created` (`handle_new_user`). No son la causa.
- El codigo de Google en Android (`GoogleAcceso.kt`) es igual en v0.6.10 y v0.8.0.
- La firma si cambio:

| Version | SHA-1 de la firma                                           |
| ------- | ----------------------------------------------------------- |
| v0.6.10 | 23:CF:7D:D7:5A:19:B1:54:13:99:38:AD:62:61:DE:AA:BE:DC:23:F9 |
| v0.8.0  | F1:A2:7B:EE:15:19:B9:5D:DB:88:AD:77:4F:5E:0E:08:5F:9F:45:D1 |

El ultimo inicio con Google que funciono fue el 20-oct a las 11:06 (hora de Ecuador), con v0.6.10. v0.8.0 se publico ese mismo dia a las 12:34.

### Causa

El APK de v0.8.0 (commit 876739e, release v0.8.0) se compilo con otro `debug.keystore` (F1:A2...) que no es el registrado en el cliente OAuth Android de Google Cloud (23:CF...). Google no entrega el ID token a una app cuya firma no esta registrada, y en algunos telefonos Credential Manager no devuelve error: se queda esperando. La app no tenia limite de tiempo, asi que la pantalla de carga no se quitaba nunca.

### Arreglo

1. App: `AccesoSocialViewModel` espera al proveedor un maximo de 45 s. Si Google no responde, quita la carga y muestra un error que menciona la firma.
2. Compilacion: con `-Pgacelas.release=true`, Gradle lee la SHA-1 del `debug.keystore` y falla si no esta en `gacelas.firmasGoogle` (`apps/android/gradle.properties`).
3. Google Cloud (lo hace Jhona): en el proyecto del cliente web, crear un cliente OAuth tipo Android con paquete `com.gacelas` y SHA-1 `F1:A2:7B:EE:15:19:B9:5D:DB:88:AD:77:4F:5E:0E:08:5F:9F:45:D1`, sin borrar el de 23:CF. Con eso v0.8.0 ya instalada vuelve a funcionar sin reinstalar.

### Test de regresion

`CuentaTest.googleSinRespuestaMuestraErrorYQuitaLaCarga` simula un selector de Google que nunca responde y comprueba que la carga se quita y aparece el error.

### Como evitarlo

- Publicar siempre con `-Pgacelas.release=true`.
- Si cambia la maquina o el keystore, registrar la nueva SHA-1 en Google Cloud antes de publicar y agregarla a `gacelas.firmasGoogle`.
- A futuro: una firma de release fija guardada fuera del repositorio, para que la SHA-1 no dependa de la maquina.

## 2026-10-26: Android no avisa de versiones nuevas (0.8.1 a 0.8.3)

### Que paso

Con v0.8.1, v0.8.2 o v0.8.3 instalada, la app no mostraba el aviso "Hay una nueva version" ni se actualizaba.

### Causa

La app no lee GitHub. Lee el manifiesto publico `app-releases/android/latest.json` en Supabase Storage y compara su `versionCode` con el instalado. Ese manifiesto solo lo actualiza `scripts/publicar-android.sh`. Las versiones 0.8.1, 0.8.2 y 0.8.3 se publicaron solo con `gh release create`, asi que el manifiesto siguio anunciando 0.8.0 (versionCode 800). Una app con 801 o mas ve un servidor mas viejo y no avisa nada. El codigo de comparacion estaba bien.

### Arreglo

1. v0.8.4 se publica con `scripts/publicar-android.sh`, que sube el APK y `latest.json` con versionCode 804.
2. El script crea el release con `--target` igual al commit publicado y al final ejecuta `scripts/verificar-manifiesto.sh`.
3. `scripts/verificar-manifiesto.sh` falla si el manifiesto publico no anuncia el `versionName` de `build.gradle.kts`.

### Test de regresion

`ActualizacionTest.versionInstaladaDetectaLaSiguiente`: una app 0.8.3 (803) que lee un manifiesto 0.8.4 (804) recibe el aviso de version nueva, y una 0.8.4 no lo recibe.

### Como evitarlo

- Publicar versiones de Android solo con `scripts/publicar-android.sh`, nunca con `gh release create` a mano.
- Despues de publicar, `scripts/verificar-manifiesto.sh` debe decir "Manifiesto al dia".

## 2026-10-26: Modales cortados a la mitad (web y Android)

### Sintoma

En Android los modales (crear evento, terminos, terminar carrera, compartir sobre foto, reportar incidente) se cortaban a la mitad en telefonos pequenos o con fuente grande: el boton principal quedaba fuera de pantalla y el contenido no se podia desplazar.

### Causa

- `DialogoC2` (core/ui) tenia un alto maximo fijo de 640 dp y ningun scroll. En una ventana de 640 dp menos barras del sistema, o con `fontScale` 1.3, el contenido y las acciones quedaban fuera.
- Los insets (`navigationBarsPadding`, `imePadding`) estaban dentro de la hoja, asi que el teclado empujaba el contenido sin dejar espacio.
- La hoja de reportar incidente (`IncidentesUi`) no tenia scroll.
- En web, `Dialogo` no tenia alto maximo ni scroll, y se renderizaba dentro de la pantalla actual (si un ancestro crea contexto de apilamiento, el `fixed` queda recortado). Las hojas usaban `vh`, que en moviles incluye la barra del navegador.

### Arreglo

1. Android `DialogoC2`: alto maximo relativo a la ventana (`BoxWithConstraints`, `maxHeight - 24.dp`), insets de barras del sistema y teclado aplicados al contenedor, cuerpo con `verticalScroll` y acciones fijas siempre visibles debajo. Se quitaron los scroll anidados en crear evento y compartir sobre foto.
2. `IncidentesUi`: contenido scrolleable y `imePadding`. El modal de actualizacion ya se compacta por alto (`ModalActualizacionTest` exige que no tenga scroll) y no se toco.
3. Web `Dialogo`, `CompartirDialogo` y `HojaReportarIncidente`: se renderizan con portal en `document.body`, alto maximo en `dvh`, cuerpo scrolleable, acciones fijas y safe areas.

### Test de regresion

- Android: `ModalesTamanosTest` (Roborazzi) abre un modal largo en 360x640 dp, 393x852 dp y 360x640 dp con `fontScale` 1.3 y verifica que el boton principal y el secundario se ven completos.
- Web: `e2e/modales.spec.ts` abre crear evento, cerrar sesion y ayuda en 360x640 y verifica que el boton principal queda dentro de la ventana. Con `QA_CAPTURAS=<carpeta>` guarda capturas en 360x640, 390x844 y 1440x900.

### Como evitarlo

- No usar alturas fijas en modales: siempre relativas a la ventana.
- Todo modal nuevo debe usar `DialogoC2` / `Dialogo` (o seguir el mismo patron: cuerpo scrolleable y acciones fijas).

## 2026-10-26: Pantallas de mapa sin flecha de retroceso y reportar incidente centrado

- Sintoma: en Carrera, Navegacion de ruta y Mapa en vivo el boton "Reportar incidente" aparecia centrado o a lo ancho bajo el mapa, y algunas pantallas de mapa no tenian forma visible de volver.
- Causa: cada pantalla dibujaba su propio boton de reporte (pildora ancha) y la cabecera de Carrera y Navegacion no incluia accion de retroceso.
- Arreglo: componentes compartidos BotonReportarMapa y BotonVolverMapa (web: shared/ui/BotonesMapa.tsx; Android: core/ui BotonesMapa.kt). Reportar es un boton lateral de 42 px al borde derecho, color C2 de alerta, icono de advertencia y etiqueta accesible. Volver va arriba a la izquierda respetando las areas seguras; vuelve a la pantalla anterior o a Inicio. En Carrera activa usa el comportamiento existente (web: la carrera sigue en segundo plano; Android: onAtras con confirmacion). En Navegacion de ruta Android se guarda la carrera igual que al cerrar.
- Pruebas: apps/web/e2e/mapa.spec.ts y apps/android/app/src/test/java/com/gacelas/MapaBotonesTest.kt.

## 2026-10-26: Modales cortados o con botones bajo la barra de navegacion (v0.9.1)

- Sintoma: en algunos telefonos "¿Descartar la carrera?" y otras hojas no quedaban pegadas al borde inferior; parte del contenido y el ultimo boton quedaban bajo la barra de navegacion.
- Causa: la ventana de `Dialog` se posiciona mal en esos equipos aunque se use `decorFitsSystemWindows = false`.
- Arreglo: las hojas C2 (`DialogoC2`, `HojaC2`, incidentes, aviso de actualizacion) se dibujan en la ventana de la actividad mediante `AnfitrionHojasC2` (core/ui HojasC2.kt), montado en MainActivity. Sin anfitrion (previews, iOS, pruebas aisladas) se usa `Dialog` como respaldo. `CargandoGoogle` sigue como dialogo centrado.
- Pruebas: `ModalesBordeTest` comprueba en 360x640, 393x873 y fuente 1.3 que no hay ventana de dialogo, que la hoja toca el borde inferior y que el ultimo boton se ve completo (acerca, descartar, compartir en vivo y una hoja larga).

## 2026-10-26: Fondo negro en la tarjeta del avatar de Inicio (v0.9.1)

- Sintoma: la escena del avatar en Inicio se veia sobre negro en vez del escenario morado del diseno C2.
- Causa: el SurfaceView de Filament con `setZOrderMediaOverlay` perfora la ventana y se ve negro detras.
- Arreglo: Inicio usa `Runner3D(sobreFondo = true)`, que dibuja el avatar en la vista transparente, sobre `EscenarioInicioC2` (degradado 160 grados, halo morado, rayas a 115 grados, brillo del piso y texto CORRE LOJA). Web usa la clase `.escenario-c2`.
- Pruebas: `InicioC2Test` (Roborazzi) y `e2e/v091.spec.ts`.

## 2026-10-26: El APK dependia del limite de 50 MiB de Supabase Storage (v0.9.2)

- Sintoma: el APK de 0.9.1 pesaba 49,2 MiB y quedaban unos 0,8 MiB antes de que Supabase Storage (plan gratuito, 50 MiB por archivo) rechazara la subida y no se pudiera publicar.
- Arreglo: `scripts/publicar-android.sh` sube el APK como asset del release de GitHub (`runcity-X.Y.Z-arm64.apk`, limite 2 GB), descarga ese asset con `curl -L` y compara sha256, y solo despues sube a Supabase `android/latest.json` con `apkUrl` apuntando a GitHub. Si el APK cabe, deja ademas una copia de respaldo en Supabase que las apps no usan. Aviso blando si pasa de 150 MiB.
- Compatibilidad: 0.9.1 y anteriores leen el mismo `latest.json` con los mismos campos. `ReglasActualizacion.aviso` acepta cualquier `apkUrl` https, el parser ignora campos desconocidos, DownloadManager sigue la redireccion 302 a release-assets.githubusercontent.com y la app compara el sha256 al terminar. No hizo falta cambiar la app.
- Pruebas: `ActualizacionTest.manifiestoConApkEnGithubReleases` y `scripts/verificar-manifiesto.sh`, que ahora comprueba que `apkUrl` responde con el tamano de `apkBytes` (`VERIFICAR_SHA=1` compara tambien el sha256).
