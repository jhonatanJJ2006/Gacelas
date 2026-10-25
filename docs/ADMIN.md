# Panel de administración

El panel vive en la app web, en la ruta `/admin`. Usa la misma sesión de Supabase que la app y solo deja entrar a cuentas con rol `admin` en la tabla `user_roles` (migración 020). Las políticas RLS y las funciones `admin_*` vuelven a comprobar el rol en el servidor, así que ocultar el panel en el cliente no es la única protección.

## Secciones

- Dashboard: métricas reales con la función `admin_metricas()`.
- Usuarios: lista con `admin_usuarios()`, detalle, suspender o reactivar (`admin_suspender`) y cambiar rol.
- Moderación: reportes de la tabla `post_reports`. Ocultar borra la publicación y marca el reporte como resuelto; Mantener lo marca como descartado.
- Eventos: crear, editar, publicar y despublicar (columna `events.published`).
- Rutas y clubes: listado y eliminación.
- Notificaciones: no disponible hasta tener un servicio push en el servidor.
- Versiones: versión mínima de Android en `app_settings`.
- Ajustes y roles: interruptores de `app_settings`, versión vigente de los Términos (`legal_versions`) y equipo con rol.

## Dar rol de administrador al primer usuario

Ningún usuario puede darse el rol a sí mismo: la política solo permite escribir en `user_roles` a quien ya es administrador. El primero se crea con permisos de servicio:

1. Aplica las migraciones 019 y 020 en el proyecto de Supabase.
2. Crea la cuenta desde la app (registro normal) y anota el correo.
3. En el panel de Supabase, abre SQL Editor y ejecuta:

```sql
INSERT INTO public.user_roles (user_id, role)
SELECT id, 'admin' FROM auth.users WHERE email = 'correo@ejemplo.com'
ON CONFLICT (user_id) DO UPDATE SET role = 'admin';
```

4. Cierra sesión y vuelve a entrar en `/admin`.

Desde ahí, ese administrador puede dar o quitar roles a otras personas en Usuarios.

## Revisar el panel en desarrollo

Con `pnpm dev`, la dirección `/admin?demo` carga datos de ejemplo sin servidor ni rol. Esa opción no existe en la build de producción.
