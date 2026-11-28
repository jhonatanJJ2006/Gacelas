-- Revierte 050_admin_faltantes.sql.
-- Borra las funciones, las tablas nuevas (respuestas rapidas, uso de la app y versiones
-- publicadas, con sus filas), el disparador y la columna priority de content_reports, y el
-- ajuste sla_horas_moderacion. No toca ninguna otra fila. Antes de revertir en produccion,
-- guarda una copia de esas tablas si quieres conservar lo registrado (docs/ADMIN.md).
BEGIN;

DROP FUNCTION IF EXISTS public.registrar_version_app(TEXT, INTEGER, TEXT, BIGINT, TEXT, TEXT);
DROP FUNCTION IF EXISTS public.admin_versiones_publicadas();
DROP FUNCTION IF EXISTS public.admin_versiones_en_uso(INTEGER);
DROP FUNCTION IF EXISTS public.admin_ultimo_acceso();
DROP FUNCTION IF EXISTS public.registrar_uso_app(TEXT, TEXT, INTEGER);
DROP FUNCTION IF EXISTS public.admin_sos_alertas(INTEGER);
DROP FUNCTION IF EXISTS public.admin_borrar_respuesta_rapida(UUID);
DROP FUNCTION IF EXISTS public.admin_guardar_respuesta_rapida(UUID, TEXT, TEXT);
DROP FUNCTION IF EXISTS public.admin_respuestas_rapidas();
DROP FUNCTION IF EXISTS public.admin_prioridad_reporte(UUID, TEXT);
DROP FUNCTION IF EXISTS public.admin_cola_reportes_sla(TEXT, TEXT);
DROP FUNCTION IF EXISTS public.admin_horas_sla(TEXT);

DROP TABLE IF EXISTS admin.versiones_app;
DROP TABLE IF EXISTS admin.uso_app;
DROP TABLE IF EXISTS moderacion.respuestas_rapidas;

DROP TRIGGER IF EXISTS content_reports_prioridad ON moderacion.content_reports;
DROP FUNCTION IF EXISTS moderacion.poner_prioridad_reporte();
DROP FUNCTION IF EXISTS moderacion.prioridad_por_motivo(TEXT);
ALTER TABLE moderacion.content_reports DROP CONSTRAINT IF EXISTS content_reports_priority_check;
ALTER TABLE moderacion.content_reports DROP COLUMN IF EXISTS priority;

DELETE FROM admin.app_settings WHERE key = 'sla_horas_moderacion';

DELETE FROM kernel.propiedad_tablas WHERE tabla IN ('respuestas_rapidas', 'uso_app', 'versiones_app')
  AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'kernel' AND table_name = 'propiedad_tablas');

COMMIT;
