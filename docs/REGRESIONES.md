# Regresiones

Registro de fallos que ya se habian corregido y volvieron. Cada entrada dice que paso, la causa, el arreglo y como evitar que se repita.

## 2026-11-13: v0.9.17 no se instala encima de una app firmada con otra huella

### Que paso

v0.9.17 (APK `runcity-0.9.17-arm64.apk`, 45.295.692 bytes, sha256 `fb35b6dbff99053ca94636b7d981aaf88627790c381a49a4bc8e5fc08218354f`) no se instala en un telefono que ya tiene Runcity firmado con otro certificado. El instalador del sistema solo dice que la app no se instalo.

### Diagnostico

El APK de v0.9.17 esta completo (el zip cierra bien), firmado con el esquema v2, sin `testOnly`, paquete `com.gacelas`, `minSdk` 24, `targetSdk` 35, solo `arm64-v8a`, `versionName` 0.9.17 y `versionCode` 917. `zipalign -c -P 16 4` pasa. Comparado con v0.9.16, el certificado, el esquema, el paquete, los SDK, la ABI y las librerias nativas son los mismos. Solo cambian el `versionCode` (916 a 917) y el dex del rediseno Tablero.

Instalacion real con `adb install`:

- Android 14 (pagina de 4 KB) y Android 15 (pagina de 16 KB): v0.9.16 se instala, y v0.9.17 se instala encima. La app abre.
- La misma prueba partiendo de v0.9.11 falla: `INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.gacelas signatures do not match newer version`.

Huella SHA-1 del certificado de los APK publicados:

| Versiones | SHA-1 |
| --- | --- |
| v0.9.8, v0.9.9, v0.9.10, v0.9.11 | 23:CF:7D:D7:5A:19:B1:54:13:99:38:AD:62:61:DE:AA:BE:DC:23:F9 |
| v0.9.12, v0.9.13, v0.9.14, v0.9.15, v0.9.16, v0.9.17 | F1:A2:7B:EE:15:19:B9:5D:DB:88:AD:77:4F:5E:0E:08:5F:9F:45:D1 |

Las dos huellas estan en `gacelas.firmasGoogle` (login con Google). Android no las trata como la misma app: no hay rotacion de clave (el APK no usa el esquema v3). `scripts/publicar-android.sh` y `apps/android/README.md` pedian el keystore `23:CF` para publicar, y Gradle aceptaba cualquiera de las dos. Por eso la firma volvio a cambiar en v0.9.12.

### Arreglo

- `-Pgacelas.release=true` exige que el `debug.keystore` sea exactamente `gacelas.firmaPublicacion` (`F1:A2:...:D1`), ademas de estar en `gacelas.firmasGoogle`.
- La documentacion de publicacion deja de pedir `23:CF` como keystore de las actualizaciones.

No hace falta otro APK para quien ya tiene v0.9.12 o posterior: v0.9.17 ya instala encima. Quien tenga v0.9.8 a v0.9.11, o un APK de Android Studio firmado con `23:CF`, tiene que desinstalar Runcity una vez (se pierden los datos locales de esa instalacion) e instalar el APK publicado. A partir de ahi las actualizaciones siguen con `F1:A2`.

### Como evitar que se repita

- Publicar solo con el `debug.keystore` de `gacelas.firmaPublicacion`.
- No agregar otra huella a esa propiedad para "que compile": cada huella nueva deja sin actualizacion a quien tenga la anterior.
- A futuro, una firma de release guardada fuera del repositorio, la misma en todas las maquinas. Cambiarla exige el esquema v3 con linaje firmado por la clave anterior, no un keystore nuevo.

## 2026-11-11: las copias de APK en Supabase llenaron el Storage y apkUrl de GitHub no era publico

### Que paso

El plan Free de Supabase llego al 130 % del cupo de Storage (1,295 GB de 1 GB) por 26 APKs acumulados en el bucket `app-releases` (unos 49 MB cada uno). Ademas, desde 0.9.2 `latest.json` apuntaba a `https://github.com/jhonatanJJ2006/Gacelas/releases/download/...`. El repo es privado, esa URL responde 404 sin token y la app no lleva tokens, asi que la descarga in-app no era publica.

### Cambio que lo introdujo

`scripts/publicar-android.sh` (v0.9.2) dejo el APK en GitHub Releases y, si cabia en 50 MiB, subia tambien una copia de respaldo a Supabase sin borrar las anteriores. Las builds arm64 posteriores volvieron a pesar menos de 50 MiB, asi que cada publicacion sumaba otro APK al bucket. `apkUrl` apuntaba a GitHub.

### Arreglo

- En Storage solo quedan `android/runcity-0.9.11-arm64.apk` y `android/latest.json`. El historial de APKs (incluida la 0.7.0, que no tenia asset en GitHub) esta en GitHub Releases.
- `latest.json` apunta otra vez a la URL publica de Supabase del APK actual.
- `scripts/publicar-android.sh` sube el APK nuevo a Supabase, pone `apkUrl` en esa URL publica, comprueba el sha256 y borra el APK anterior del bucket. GitHub Releases sigue como archivo. Si el APK supera 50 MiB, no se actualiza el manifiesto.
- Se elimino `scripts/publicar-apk-storage.mjs`, que subia el APK sin borrar el anterior y no creaba el release de GitHub.

### Como evitar que se repita

- Publicar Android solo con `scripts/publicar-android.sh`.
- No dejar copias historicas en `app-releases`; el bucket solo debe tener el APK actual y `latest.json`.
- No apuntar `apkUrl` a GitHub mientras el repo sea privado.
- `scripts/verificar-manifiesto.sh` comprueba que `apkUrl` responde con el tamano de `apkBytes`.

## 2026-10-27: cierre forzado nativo al abrir Inicio en Android (v0.9.6)

### Que paso

Con 0.9.5 instalada en un Redmi (emerald, Android 16, HyperOS OS3.0.307.0) la app se cerraba sola. El tombstone muestra un abort desde `libfilament-jni.so` en `utils::TPanic<utils::PreconditionPanic>::panic`: una precondicion de Filament fallo y Filament aborta el proceso (no es una excepcion de Kotlin, no se puede capturar).

### Cambio que lo introdujo

v0.9.5, `Runner3DFilamentTransparente`: Inicio paso a Filament sobre un `TextureView` usando `ModelViewer` de gltfio. `ModelViewer` pone la proyeccion de la camara en cada cambio de tamano con `width / height` sin comprobar el tamano; si el TextureView mide 0 (al medirse, al volver de segundo plano, al cerrarse) el aspecto queda en 0 o infinito y `Camera::setProjection` lanza el PreconditionPanic. Ademas su ciclo de vida (superficie, swapchain y destruccion) no estaba atado al de la vista de Compose. No se pudo reproducir en el telefono, asi que la causa exacta es probable, no confirmada: el panic es de precondicion y estas eran las llamadas sin proteger.

### Arreglo

- `CorredorFilament` reemplaza a `ModelViewer`: motor, escena, camara y cargadores propios, todo en el hilo principal.
- `CicloCorredor`: solo se dibuja con superficie, swapchain y tamano mayor que 0; la proyeccion solo se pone con tamano valido.
- Cierre idempotente y en orden: quitar el callback de cuadros, soltar la superficie, destruir el swapchain, esperar al motor, destruir recursos y al final el motor.
- `SalvaguardaCorredor`: marca persistente con commit() antes de crear el motor, en los primeros 120 cuadros y al destruir. Si la app arranca con la marca puesta, el proceso murio con el 3D activo y esa version usa la imagen de respaldo, sin bucle de cierres. Una version nueva vuelve a intentar el 3D.

### Como evitar que se repita

- `CicloCorredorTest` y `SalvaguardaCorredorTest` cubren el dibujo sin tamano, el cierre doble y el bloqueo tras un cierre.
- No usar `ModelViewer` en vistas que se crean y destruyen con Compose; cualquier llamada a Filament con tamanos de la vista se protege contra 0.
- Un cambio de motor de render se prueba en un telefono real antes de publicar.

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

## 2026-11-12: Al iniciar sesion en la web se iba a otra pagina (v0.9.12)

### Sintoma

Despues de entrar con correo o con Google la web no volvia a Inicio ni a la pantalla que se habia pedido; con Google terminaba en una URL de Vercel con inicio de sesion propio.

### Causa

El `site_url` de Supabase Auth apuntaba a un alias de Vercel protegido por SSO, y la web no recordaba la pantalla pedida antes de mandar al acceso.

### Arreglo

`site_url` pasa a `https://gacelas.vercel.app` (con su lista de redirecciones). `src/shared/lib/destino.ts` guarda la pantalla protegida pedida (sessionStorage) y el acceso vuelve a ella; por defecto a Inicio. Guardar o descartar una carrera vuelve a Inicio con un aviso.

### Test de regresion

`src/shared/lib/__tests__/destino.test.ts`, `e2e/v0912.spec.ts` (vuelve a la pantalla pedida, descartar vuelve a Inicio) y `src/__tests__/correr.test.tsx`.

### Como evitarlo

No usar alias de Vercel con proteccion como `site_url`. Toda navegacion tras acceso pasa por `tomarDestino()`.

## 2026-11-12: Se preguntaba en cada carrera si compartir la ubicacion (v0.9.12)

### Sintoma

Al iniciar cada carrera la app volvia a preguntar si otras personas podian ver la ubicacion.

### Causa

La respuesta vivia solo en Ajustes del dispositivo. Al reinstalar la app, o cuando la web guardaba `prefs` con otro formato (por ejemplo `visibilidad` en minusculas), Android descartaba todas las preferencias remotas y la decision volvia a quedar vacia.

### Arreglo

La respuesta se guarda tambien como consentimiento `ubicacion_visible` en `consents`. Antes de preguntar, web y Android buscan esa decision (Android sincroniza los consentimientos del servidor). Android lee `prefs` clave por clave (`ajustesTolerantes`) y ya no pierde todo por un campo distinto. El consentimiento de GPS tambien se busca en el servidor antes de pedirlo. Solo se cambia desde Ajustes, Privacidad.

### Test de regresion

`e2e/v0912.spec.ts` (segunda carrera sin preguntas) y `Version0912Test.prefsEscritasPorLaWebNoBorranLaDecision` en Android.

### Como evitarlo

Toda decision de privacidad que no deba repetirse se guarda en `consents`, no solo en preferencias locales.

## 2026-11-12: El boton 3D/2D parecia invertido (v0.9.12)

### Sintoma

Al tocar 3D el mapa seguia plano, y al tocar 2D parecia 3D.

### Causa

Web: sin Map ID el mapa raster no se inclina, asi que el modo 3D se veia plano, y el boton mostraba el modo de destino ("2D" estando en 3D). Android: la camara solo se inclinaba si ya habia punto GPS y se seguia al corredor; tras arrastrar el mapa el cambio no hacia nada.

### Arreglo

Selector con las dos opciones (2D y 3D) que resalta la que se ve. Web: sin Map ID la capa del mapa se inclina con perspectiva CSS (en GPU) y gira con el rumbo; el corredor 3D va de pie encima. Android: al cambiar de modo se vuelve a seguir y la inclinacion se aplica en el acto.

### Test de regresion

Selector `vista-3d-on` / `vista-2d-on` en web y Android (Roborazzi de la carrera).

### Como evitarlo

Los botones de modo muestran el estado actual, nunca el de destino.

## 2026-11-12: La version 0.9.12 no cabia en el bucket de actualizaciones (APK de 55,1 MB)

### Sintoma

`scripts/publicar-android.sh` creo el release v0.9.12 en GitHub pero no actualizo `android/latest.json`: el APK pesaba 55.089.826 bytes y el bucket `app-releases` admite 52.428.800 (50 MiB). Las apps instaladas no vieron la 0.9.12.

### Causa

La intro paso a ExoPlayer (media3) y a un video de 752 KB. El APK de 0.9.11 ya estaba a 600 KB del limite.

### Arreglo

0.9.13: intro con el `MediaPlayer` del sistema y video de 396 KB (52.281.903 bytes). 0.9.14: el APK publicado pasa por R8 solo para quitar los iconos de `material-icons-extended` que no se usan (`app/proguard-iconos.pro`, activo con `-Pgacelas.release=true`): sin ofuscar, sin optimizar y conservando todas las demas clases, asi que la reflexion de Supabase, Ktor, serializacion, Filament y MapLibre no cambia. Se quitan 11.279 de 11.400 clases de iconos (de 39.645 a 29.456 clases en total) y el dex comprimido baja de 25,06 MB a 17,64 MB. APK publicado: 44,9 MB, con unos 7,5 MB de margen.

### Test de regresion

El script ya corta la publicacion si el APK supera el limite. Comparar las clases del dex con y sin R8 (`dexdump -h`): solo deben faltar clases `androidx/compose/material/icons/**` y lambdas sinteticas.

### Como evitarlo

Antes de agregar una dependencia o un recurso grande, medir el APK con `./gradlew clean assembleDebug -Pgacelas.abi=arm64-v8a -Pgacelas.release=true`. Los builds incrementales reusan el archivo y dejan huecos: medir siempre tras `clean`.
