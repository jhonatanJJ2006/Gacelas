# Supabase, Google y Maps en Gacelas

Este documento explica como estan conectadas la app web (`apps/web`) y la app Android (`apps/android`) a Supabase, como se aplican las migraciones y que hay que configurar en Google Cloud.

Proyecto: `yampnicdjlmimeoqmowb` (`https://yampnicdjlmimeoqmowb.supabase.co`, region us-east-1).

## Esquema

Las migraciones estan en `backend/migrations` y se aplican en orden:

| Archivo                  | Contenido                                                                                 |
| ------------------------ | ----------------------------------------------------------------------------------------- |
| `001_initial_schema.sql` | PostGIS, `profiles`, `activities`, `track_points`, RLS basico y triggers de `updated_at`. |
| `002_app_data.sql`       | Datos que guardan las apps, RLS completo, trigger de alta e indices.                      |
| `003_seed_loja.sql`      | Rutas y eventos de ejemplo en Loja (mismos id que los datos de ejemplo de las apps).      |

Tablas del esquema `public`:

| Tabla                          | Para que sirve                                                                                                                      | Clave                              |
| ------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------- |
| `profiles`                     | Perfil (nombre, usuario, bio, ubicacion, edad, genero, foto). Se crea al registrarse.                                               | `id` = `auth.users.id`             |
| `user_settings`                | Ajustes. `visibility`, `hide_home`, `home_radius_m` y `precise_location` son columnas (las usa RLS); el resto va en `prefs` (JSON). | `user_id`                          |
| `follows`                      | Quien sigue a quien. Define la visibilidad "seguidores".                                                                            | `follower_id, following_id`        |
| `activities`                   | Carreras: titulo, nota, tiempos, distancia, ritmo, desnivel, `splits` (JSON) y `public_points`.                                     | `id`, unica `(user_id, client_id)` |
| `activity_tracks`              | Recorrido GPS completo (JSON). Solo lo ve el dueno.                                                                                 | `activity_id`                      |
| `track_points`                 | Puntos en PostGIS (de la migracion 001; reservado para consultas espaciales).                                                       | `id`                               |
| `routes`                       | Rutas publicas y propias, con su trazado (`path`).                                                                                  | `id` (texto)                       |
| `saved_routes`                 | Rutas guardadas por cada usuario.                                                                                                   | `user_id, route_id`                |
| `post_likes` y `post_comments` | Me gusta y comentarios del feed. `post_id` es texto (actividad o publicacion de ejemplo).                                           |                                    |
| `events` y `event_rsvps`       | Eventos (ejemplo y creados por usuarios) e inscripciones.                                                                           | `id` (texto)                       |
| `notifications`                | Avisos del usuario. Las crea el servidor (por ejemplo la bienvenida).                                                               | `id`                               |

`client_id` es el id que genera la app al guardar una carrera; asi la misma carrera subida dos veces (por ejemplo desde la cola sin conexion) no se duplica.

Funciones y triggers:

- `handle_new_user` (trigger `on_auth_user_created` sobre `auth.users`): crea el perfil, los ajustes por defecto (visibilidad "seguidores", ocultar casa con 300 m) y un aviso de bienvenida. Sirve para registro con correo y con Google.
- `fuzz_track` (trigger sobre `activity_tracks`): calcula `activities.public_points` quitando los puntos que estan dentro del radio de casa alrededor del punto de salida, si el usuario tiene activado "ocultar casa".
- `can_view_user(owner)`: dice si el usuario actual puede ver el contenido de `owner` segun su visibilidad (publico, seguidores o privado).
- `event_participants(eid)`: inscritos de un evento (base mas inscripciones), visible para todos.

## RLS

Todas las tablas tienen Row Level Security activo.

- Cada usuario lee y escribe solo sus filas en `user_settings`, `activity_tracks`, `saved_routes`, `event_rsvps`, `notifications`, y crea o borra solo sus `post_likes`, `post_comments`, `routes` y `events`.
- `activities`: el dueno ve todas las suyas. Los demas solo ven las marcadas como publicas y solo si `can_view_user` lo permite (perfil publico, o seguidores que lo siguen). El recorrido completo nunca es publico: los demas ven `public_points`, sin la zona de casa.
- `profiles`: lectura publica (nombre y usuario para el feed); cada uno edita solo el suyo.
- `routes` publicas y `events`: lectura publica (sin sesion tambien).
- `post_likes` y `post_comments`: lectura para usuarios con sesion.

## Aplicar migraciones

Desde esta maquina los puertos de Postgres (5432 y 6543) pueden estar bloqueados, asi que se usa la Management API por HTTPS con un token personal (`SUPABASE_ACCESS_TOKEN` en `backend/.env`):

```bash
scripts/sb-query.sh backend/migrations/002_app_data.sql
echo "select tablename from pg_tables where schemaname = 'public'" | scripts/sb-query.sh -
```

Tambien se puede pegar cada archivo en el SQL Editor del panel de Supabase, en orden. Las migraciones no son idempotentes: aplica cada una una sola vez.

Prueba de humo contra el proyecto real (crea un usuario de prueba, prueba RLS y lo borra):

```bash
cd apps/web && pnpm smoke:supabase
```

## Variables de entorno

| Archivo                         | Variable                                                                                        | Uso                                                                                       |
| ------------------------------- | ----------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| `apps/web/.env.local`           | `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`                                                   | Cliente supabase-js. Sin ellas la web guarda todo en el navegador.                        |
| `apps/web/.env.local`           | `VITE_GOOGLE_MAPS_API_KEY`                                                                      | Google Maps en buscador de rutas, navegacion y carrera. Sin ella se usa el mapa dibujado. |
| `apps/web/.env.local`           | `VITE_GOOGLE_OAUTH_CLIENT_ID`                                                                   | Referencia; el login con Google va por Supabase.                                          |
| `apps/android/local.properties` | `SUPABASE_URL`, `SUPABASE_ANON_KEY`                                                             | Cliente supabase-kt (BuildConfig). Sin ellas la app funciona solo en el telefono.         |
| `apps/android/local.properties` | `GOOGLE_WEB_CLIENT_ID`                                                                          | Credential Manager pide el ID token para este cliente web.                                |
| `apps/android/local.properties` | (ninguna)                                                                                       | Android usa MapLibre con OpenStreetMap, sin clave.                                        |
| `backend/.env`                  | `SUPABASE_ACCESS_TOKEN`, `DATABASE_URL`, `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET` | Migraciones y configuracion; nunca van en las apps.                                       |

Ninguno de estos archivos se sube al repositorio. Las plantillas sin valores son `apps/web/.env.example`, `apps/android/local.properties.example` y `backend/.env.example`. Las pruebas e2e usan `apps/web/.env.e2e`, que apunta a un Supabase falso.

## Como funcionan las apps

- Web: `src/data/supabaseRepos.ts` implementa las mismas interfaces que los repositorios locales. Las carreras se guardan primero en el navegador y entran a una cola (`gacelas:carreras-pendientes`) que se sube cuando hay conexion. Las rutas protegidas usan la sesion de Supabase (supabase-js la guarda y la renueva).
- Android: `data/remoto` tiene `SupabaseBackend` (supabase-kt: auth y postgrest) y los repositorios remotos. DataStore es la cache: se escribe primero en el telefono y luego en el servidor; al abrir sesion se descarga todo. Las carreras sin conexion quedan en `carreras_pendientes`.
- Realtime no se usa todavia.

## Login con Google

### 1. Pantalla de consentimiento

En Google Cloud (proyecto `active-landing-511100-c8`), Google Auth Platform: tipo Externa, nombre "Gacelas" y tu correo. Mientras este en modo de prueba, solo pueden entrar los usuarios de prueba que agregues ahi.

### 2. Cliente web (ya creado)

Tipo "Aplicacion web":

- URI de redireccionamiento autorizado: `https://yampnicdjlmimeoqmowb.supabase.co/auth/v1/callback`
- Origenes de JavaScript: `http://localhost:5173` y la URL donde publiques la web.

### 3. Activar Google en Supabase

Authentication, Sign In / Providers, Google: activar, pegar el Client ID y el Client Secret del cliente web y guardar. En "Authorized Client IDs" deja tambien el Client ID web (Android lo usa para el ID token).

Authentication, URL Configuration:

- Site URL: la URL publica de la web (por ahora `http://localhost:5173`).
- Redirect URLs: `http://localhost:5173/**` y `https://TU-DOMINIO/**`. La web vuelve a `/inicio` despues de Google y a `/nueva-contrasena` desde el correo de recuperacion.

### 4. Cliente Android

En el mismo proyecto de Google Cloud crea un cliente OAuth de tipo Android:

- Nombre del paquete: `com.gacelas`
- Huella SHA-1 del certificado de firma.

La huella del keystore de depuracion se obtiene asi:

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

El APK de depuracion compilado en la maquina de Grok Bot esta firmado con la huella:

```
SHA-1: F1:A2:7B:EE:15:19:B9:5D:DB:88:AD:77:4F:5E:0E:08:5F:9F:45:D1
```

Cada computadora tiene su propio keystore de depuracion. Si compilas en tu PC, agrega tambien la SHA-1 de tu `debug.keystore`, y la de la firma de release (o la de Play App Signing) antes de publicar.

El cliente Android no lleva secreto ni se configura en Supabase: la app pide el ID token con `GOOGLE_WEB_CLIENT_ID` (el cliente web) y Google lo emite solo si el paquete y la SHA-1 coinciden con un cliente Android del mismo proyecto.

## Google Maps

La clave "Gacelas Maps" necesita Maps JavaScript API, Maps SDK for Android y Geocoding API. Conviene restringirla: por referente HTTP para la web y por paquete `com.gacelas` mas SHA-1 para Android (o usar dos claves). El estilo oscuro sigue `docs/DESIGN.md` (carreteras #374151, parques #064E3B, agua #1E3A8A y trazado naranja #F59E0B).
