-- Revierte 043_suspension_efectiva.sql. No toca el candado de suspended_at de 042.

BEGIN;

DO $$
DECLARE r record;
BEGIN
  FOR r IN
    SELECT n.nspname AS esquema, c.relname AS tabla
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE c.relkind = 'r'
      AND n.nspname IN (
        'public', 'identidad', 'legal', 'actividad', 'rutas', 'envivo',
        'seguridad', 'comunidad', 'eventos', 'logros', 'moderacion',
        'avisos', 'admin', 'analitica', 'storage'
      )
  LOOP
    EXECUTE format('DROP TRIGGER IF EXISTS aa_bloquear_suspendido ON %I.%I', r.esquema, r.tabla);
    EXECUTE format('DROP TRIGGER IF EXISTS cerrar_en_vivo_al_suspender ON %I.%I', r.esquema, r.tabla);
  END LOOP;
  IF to_regclass('storage.objects') IS NOT NULL THEN
    EXECUTE 'DROP POLICY IF EXISTS "Suspendido no sube archivos" ON storage.objects';
    EXECUTE 'DROP POLICY IF EXISTS "Suspendido no reemplaza archivos" ON storage.objects';
    EXECUTE 'DROP POLICY IF EXISTS "Suspendido no borra archivos" ON storage.objects';
  END IF;
END $$;

CREATE OR REPLACE FUNCTION public.puede_ver_en_vivo(dueno UUID, audiencia TEXT) RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT dueno = auth.uid()
      OR audiencia = 'public'
      OR EXISTS (SELECT 1 FROM follows WHERE follower_id = auth.uid() AND following_id = dueno);
$$;

DROP POLICY IF EXISTS "Ver corredores visibles" ON public.runner_positions;
CREATE POLICY "Ver corredores visibles" ON public.runner_positions FOR SELECT
  USING (auth.uid() IS NOT NULL AND updated_at > now() - interval '2 minutes'
         AND consentimiento_vigente(user_id, 'ubicacion_visible'));

CREATE OR REPLACE FUNCTION public.eliminar_mi_cuenta() RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, auth AS $$
BEGIN
  IF auth.uid() IS NULL THEN RAISE EXCEPTION 'no autenticado'; END IF;
  DELETE FROM auth.users WHERE id = auth.uid();
END;
$$;
REVOKE ALL ON FUNCTION public.eliminar_mi_cuenta() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.eliminar_mi_cuenta() TO authenticated;

DROP FUNCTION IF EXISTS identidad.cerrar_en_vivo_al_suspender();
DROP FUNCTION IF EXISTS identidad.trg_bloquear_suspendido();
DROP FUNCTION IF EXISTS identidad.es_suspendido(uuid);

NOTIFY pgrst, 'reload schema';

COMMIT;
