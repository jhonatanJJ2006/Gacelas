# Regresiones

Registro de fallos que ya se habian corregido y volvieron. Cada entrada dice que paso, la causa, el arreglo y como evitar que se repita.

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
