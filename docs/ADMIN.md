# Panel de administración

El panel vive en la app web, en la ruta `/admin`. Usa la misma sesión de Supabase que la app y solo deja entrar a cuentas con rol `admin` en la tabla `user_roles` (migración 020). Las políticas RLS y las funciones `admin_*` vuelven a comprobar el rol en el servidor, así que ocultar el panel en el cliente no es la única protección.

## Diseño Escenario y navegación

El panel usa la propuesta 1 "Escenario" (oscuro, con tokens de tema para el modo claro) y el navbar B colapsable.

- Web: riel de 96 px con globo de etiqueta en la sección activa y insignias sobre el icono; se expande a 248 px con etiquetas, avatar y buscador (Ctrl K). El estado se guarda en `localStorage` (`runcity.admin.nav.colapsado`). Con menos de 1024 px pasa a una barra inferior con burbuja morada que salta a la sección activa. Respeta "reducir movimiento".
- Móvil: la misma barra con burbuja; seis destinos (Resumen, Moderación, Usuarios, Rutas, Reportes, Ajustes). El moderador solo ve Moderación y no tiene barra.
- Componente genérico: `apps/web/src/shared/ui/navegacion/NavColapsable.tsx` y `apps/android/core/ui/.../navegacion/NavColapsable.kt`. Quien lo usa pasa sus items (ruta, etiqueta, icono, insignia opcional). La insignia solo se pinta con un conteo real mayor que cero; en el panel, Moderación muestra los casos abiertos (web, `admin_metricas`) o pendientes (móvil, `admin_cola_reportes`).
- Sin fuente de datos: SOS, posición de corredores en vivo en el móvil, prioridad y SLA, respuestas rápidas, último acceso y estado de importación ECU 911 muestran "fuente pendiente" o no se muestran. Los valores de ejemplo solo aparecen con `?demo` en desarrollo y están rotulados.
- "Asignarme el siguiente caso" toma el pendiente más antiguo con la acción `en_revision` de `moderar_reporte()` (el servidor registra al moderador); no hay cambios de esquema.

## Secciones

- Resumen (antes Dashboard): métricas reales con la función `admin_metricas()`.
- Usuarios: lista con `admin_usuarios()`, detalle, suspender o reactivar (`admin_suspender`) y cambiar rol.
- Estadísticas: gráficos por rango de fechas con `admin_estadisticas()`, exportables a CSV y PDF (ver `docs/REPORTES.md`).
- Moderación: cola de reportes de publicaciones, usuarios, comentarios y eventos (`admin_cola_reportes()`). Pestañas por estado (pendiente, en revisión, resuelto, descartado), filtro por tipo, nota del moderador e historial de acciones. Las acciones (revisar, ocultar o suspender, descartar, reabrir, solo nota) pasan por `moderar_reporte()` y avisan a quien reportó al cerrar el caso.
- Eventos: crear, editar, publicar y despublicar (columna `events.published`).
- Rutas y clubes: listado y eliminación.
- Notificaciones: no disponible hasta tener un servicio push en el servidor.
- Versiones: versión mínima de Android en `app_settings`.
- Ajustes y roles: interruptores de `app_settings`, versión vigente de los Términos (`legal_versions`) y equipo con rol.

## Dar rol de administrador al primer usuario

Ningún usuario puede darse el rol a sí mismo: la política solo permite escribir en `user_roles` a quien ya es administrador. El primero se crea con permisos de servicio:

1. Aplica las migraciones 019 y 020 en el proyecto de Supabase (y 022 y 023 para rutas oficiales, estadísticas y reportes ampliados).
2. Crea la cuenta desde la app (registro normal) y anota el correo.
3. En el panel de Supabase, abre SQL Editor y ejecuta:

```sql
INSERT INTO public.user_roles (user_id, role)
SELECT id, 'admin' FROM auth.users WHERE email = 'correo@ejemplo.com'
ON CONFLICT (user_id) DO UPDATE SET role = 'admin';
```

4. Cierra sesión y vuelve a entrar en `/admin`.

Desde ahí, ese administrador puede dar o quitar roles a otras personas en Usuarios.

## Panel en la app móvil (Android e iOS)

El mismo panel existe en la app móvil, con código compartido en el contexto `contextos/administracion` (dominio, casos de uso, repositorio Supabase y pantallas Compose Multiplatform), así que Android e iOS usan las mismas pantallas.

- Entrada: en Ajustes aparece la sección "Administración" con la fila "Panel de administración" solo si la cuenta tiene rol. El rol se consulta en el servidor (`is_admin()` y después `es_moderador()`) cada vez que se abre Ajustes, sin caché; si la consulta falla, la entrada no aparece.
- Ruta: `ajustes/admin` en Android y la pantalla `Admin` en iOS. El panel vuelve a consultar el rol al abrirse; sin rol muestra "Acceso denegado".
- Roles: el administrador ve todas las secciones; el moderador solo ve Moderación, igual que en el servidor.
- Secciones: Resumen (acción rápida para atender el siguiente caso), Usuarios (filtros Todos y Suspendidos), Moderación, Reportes (rango de fechas, sin exportar), Rutas y eventos (pestañas Rutas, Eventos y Clubes; los eventos se crean, editan, publican y despublican con fecha y hora como texto `aaaa-mm-ddThh:mm`) y Ajustes (versión mínima de Android editable; el resto de `app_settings` en solo lectura).
- Estados: cada sección tiene carga, vacío y error con botón Reintentar.
- Sin funciones nuevas: usa las mismas RPC y tablas que la web, y la seguridad real sigue en RLS y en las funciones `admin_*`.
- No incluye: notificaciones, versión de los Términos ni gestión del equipo con rol (siguen en la web).
- Modo demo (sin Supabase): la entrada no aparece.

Capturas en `docs/capturas/admin-movil/`, generadas con Roborazzi:

```
./gradlew :app:testDebugUnitTest --tests '*AdminMovil*' -Proborazzi.test.record=true -Pgacelas.shots=<ruta absoluta a docs/capturas>
```

## Estado en producción

El 25 de octubre de 2026 se aplicaron en el proyecto las migraciones 019 a 023, 030, 031 y 032, con respaldo previo, y quedaron registradas en `supabase_migrations.schema_migrations`. La cuenta 2jhonatanjara2@gmail.com tiene rol `admin`.

## Revisar el panel en desarrollo

Con `pnpm dev`, la dirección `/admin?demo` carga datos de ejemplo sin servidor ni rol, incluidas estadísticas (`/admin/estadisticas?demo`) y reportes de varios tipos. Esa opción no existe en la build de producción.
