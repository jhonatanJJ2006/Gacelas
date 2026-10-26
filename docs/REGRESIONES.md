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

| Version | SHA-1 de la firma |
| --- | --- |
| v0.6.10 | 23:CF:7D:D7:5A:19:B1:54:13:99:38:AD:62:61:DE:AA:BE:DC:23:F9 |
| v0.8.0 | F1:A2:7B:EE:15:19:B9:5D:DB:88:AD:77:4F:5E:0E:08:5F:9F:45:D1 |

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
