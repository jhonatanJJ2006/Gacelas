# ADR 005: Tablas en el esquema de su contexto con vistas de compatibilidad en public

## Estado

Aceptado (26 de octubre de 2026). Cierra la fase F7 de [ARQUITECTURA-DDD.md](../ARQUITECTURA-DDD.md) y aplica la decisión D3 del [ADR 004](004-arquitectura-ddd.md).

## Contexto

La migración 032 creó un esquema por contexto acotado y registró en `kernel.propiedad_tablas` a qué contexto pertenece cada tabla, pero las tablas seguían en `public`. Para moverlas había tres riesgos:

- Las versiones ya instaladas de la app (Android v0.8.0 y la web publicada) consultan `public.<tabla>` por PostgREST, que solo expone `public`.
- Ocho tablas están en la publicación `supabase_realtime`. `postgres_changes` no emite eventos sobre vistas y los clientes instalados se suscriben a `public.<tabla>`.
- Varias funciones, triggers y políticas de Storage nombran `public.<tabla>` en su código.

## Decisión

- La migración `033_mover_tablas_contexto.sql` hace `ALTER TABLE public.<tabla> SET SCHEMA <contexto>` y crea en `public` una vista simple con el mismo nombre: `CREATE VIEW public.<tabla> WITH (security_invoker = true) AS SELECT * FROM <contexto>.<tabla>`, con los mismos privilegios que tenía la tabla.
- Las vistas son actualizables automáticamente. INSERT, UPDATE, DELETE, RETURNING y upsert (`ON CONFLICT`) llegan a la tabla y RLS se evalúa con el rol de quien consulta. Índices, restricciones, claves foráneas, secuencias, triggers y políticas viajan con la tabla.
- Las funciones con `public.<tabla>` o con `search_path = public` resuelven ahora la vista y siguen funcionando. Esto incluye `handle_new_user`, las RPC y las tareas de pg_cron. Las políticas de Storage guardan referencias por OID, así que siguen apuntando a la tabla movida.
- `anon`, `authenticated` y `service_role` reciben `USAGE` en los esquemas de contexto, que lo necesitan para las vistas `security_invoker`. PostgREST sigue exponiendo solo `public`.
- **Excepción Realtime:** las tablas de la publicación `supabase_realtime` se quedan en `public` y quedan registradas con su motivo en `kernel.tablas_en_public`. Son `activities`, `follows`, `post_likes`, `post_comments`, `post_reposts`, `live_sessions`, `club_posts` y `runner_positions`. Moverlas y agregar los esquemas nuevos a la publicación rompería las suscripciones de la app instalada.
- La reversión está en `supabase/rollback/033_mover_tablas_contexto.down.sql`. Devuelve las tablas a `public` y recrea las vistas de lectura de la 032.

## Consecuencias

- Las vistas de `public` congelan la lista de columnas. Una migración que agregue o quite columnas en una tabla movida debe recrear su vista en `public`, con `CREATE OR REPLACE VIEW` si solo agrega columnas al final.
- `TRUNCATE`, `ALTER TABLE` y `DISABLE TRIGGER` deben apuntar a `<contexto>.<tabla>`, no a la vista.
- Para mover las tablas de Realtime hay que publicar una versión de la app que escuche `<contexto>.<tabla>` (o use Broadcast), agregar el esquema a `supabase_realtime` y esperar a que las versiones viejas dejen de usarse.
- Las vistas de compatibilidad se pueden quitar cuando los clientes consulten los esquemas de contexto directamente. Eso exige exponerlos en PostgREST.
- La prueba `backend/tests/rls/033_mover_tablas_contexto.sql` verifica las vistas, las columnas, la excepción de Realtime, RLS por la vista y el trigger de alta.
