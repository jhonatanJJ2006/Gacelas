# ADR 006: Comunidades y grupos de running sobre la tabla de clubes

## Estado

Aceptado (26 de octubre de 2026). Migración `034_comunidades_grupos.sql`.

## Contexto

Los corredores quieren registrar comunidades (colectivos abiertos de una ciudad o institución) y grupos de running (quedadas con punto de encuentro, días, hora, nivel y ritmo). Un grupo puede pertenecer a una comunidad. Ya existía `eventos.clubs` con membresías, roles, solicitudes, publicaciones (Realtime) y eventos de club, y producción no tenía filas.

Opciones evaluadas:

1. Tablas nuevas `comunidades` y `grupos` con sus propias membresías.
2. Extender `eventos.clubs` con un tipo y los campos de grupo.

## Decisión

Se reutiliza `eventos.clubs` (opción 2):

- `kind` distingue `comunidad` y `grupo`; no cambia después de crear. `community_id` enlaza un grupo con su comunidad (`ON DELETE SET NULL`); una comunidad no puede tener comunidad.
- Campos nuevos: `city`, `logo_path`, `cover_path`, `contact`, `meeting_place`, `meeting_lat`, `meeting_lng`, `training_days` (1 a 7, ISO), `training_time`, `level`, `pace_s_km` (150 a 900), `max_members` (2 a 1000), `hidden_at` y `updated_at`. Una sola restricción `clubs_comunidad_grupo_check` valida rangos y que los campos de grupo queden vacíos en comunidades.
- `requires_approval` sigue siendo la marca de privado: las solicitudes quedan `pending` hasta que un owner o admin las apruebe.
- Nombres únicos por tipo, sin distinguir mayúsculas ni espacios repetidos (índice `clubs_nombre_unico`).
- El trigger `club_proteger` fija owner, contadores, tipo y fecha de creación, comprueba la comunidad elegida y aplica los límites: 3 comunidades y 5 grupos por usuario, y 3 altas por día. Los administradores de Gacelas no tienen límite.
- `club_miembro_antes` bloquea unirse a un club oculto y aplica el cupo al entrar o al aprobar.
- Moderación: se registran los tipos `comunidad` y `grupo` en `report_target_types`. Ocultar pone `hidden_at`; la política "Clubes visibles" solo muestra los ocultos al owner y a los administradores.
- Logo y portada van al bucket `post-images` bajo la carpeta del usuario (`<uid>/clubes/...`), que ya tiene sus políticas.
- Según el ADR 005, la vista `public.clubs` se recrea con `CREATE OR REPLACE VIEW` para exponer las columnas nuevas al final.

## Consecuencias

- Membresías, roles, solicitudes, publicaciones y eventos de club funcionan igual para comunidades y grupos, sin migrar datos ni tocar Realtime.
- Las apps ya instaladas siguen leyendo `public.clubs`; las filas nuevas toman `kind = 'grupo'` por defecto.
- Un grupo hereda de su comunidad solo el enlace; no hay membresía en cascada.
- La reversión está en `supabase/rollback/034_comunidades_grupos.down.sql`.
- La prueba `backend/tests/rls/034_comunidades_grupos.sql` cubre owner, anidación, nombres únicos, límite diario, campos fijos, solicitudes, permisos de edición y borrado, cupo, denuncia y ocultación por un administrador.
