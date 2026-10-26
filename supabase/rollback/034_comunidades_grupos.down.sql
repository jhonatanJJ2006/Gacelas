-- Reversion de 034: quita comunidades y grupos y deja eventos.clubs como en la 012/033.
-- No se aplica automaticamente (no esta en supabase/migrations). Uso: psql -f este_archivo.
-- Las filas con kind = 'comunidad' se conservan como clubes normales.
BEGIN;
DELETE FROM public.report_target_types WHERE key IN ('comunidad', 'grupo');
DROP FUNCTION IF EXISTS public.moderacion_resumen_club(TEXT);
DROP FUNCTION IF EXISTS public.moderacion_ocultar_club(TEXT);
DROP FUNCTION IF EXISTS public.moderacion_autor_club(TEXT);

DROP POLICY IF EXISTS "Clubes visibles" ON eventos.clubs;
CREATE POLICY "Clubes visibles" ON eventos.clubs FOR SELECT USING (true);

CREATE OR REPLACE FUNCTION public.club_proteger() RETURNS trigger
LANGUAGE plpgsql SET search_path = public AS $$
BEGIN
  IF pg_trigger_depth() > 1 THEN RETURN NEW; END IF;
  IF TG_OP = 'INSERT' THEN NEW.members_count := 0;
  ELSE NEW.members_count := OLD.members_count; NEW.owner_id := OLD.owner_id; END IF;
  RETURN NEW;
END $$;

CREATE OR REPLACE FUNCTION public.club_miembro_antes() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE c clubs;
BEGIN
  IF pg_trigger_depth() > 1 THEN RETURN NEW; END IF;
  SELECT * INTO c FROM clubs WHERE id = NEW.club_id;
  IF TG_OP = 'INSERT' THEN
    NEW.role := 'member';
    NEW.status := CASE WHEN c.requires_approval THEN 'pending' ELSE 'active' END;
  ELSE
    IF OLD.role = 'owner' OR NEW.role = 'owner' THEN
      IF NEW.role IS DISTINCT FROM OLD.role THEN RAISE EXCEPTION 'el owner no cambia de rol'; END IF;
    END IF;
    IF NEW.role = 'admin' AND OLD.role <> 'admin' AND rol_club(NEW.club_id) <> 'owner' THEN
      RAISE EXCEPTION 'solo el owner nombra admins';
    END IF;
  END IF;
  RETURN NEW;
END $$;

-- La vista congela columnas: hay que recrearla antes de quitarlas.
DROP VIEW IF EXISTS public.clubs;
DROP INDEX IF EXISTS eventos.clubs_nombre_unico;
DROP INDEX IF EXISTS eventos.clubs_comunidad_idx;
DROP INDEX IF EXISTS eventos.clubs_owner_idx;
ALTER TABLE eventos.clubs DROP CONSTRAINT IF EXISTS clubs_comunidad_grupo_check;
ALTER TABLE eventos.clubs
  DROP COLUMN IF EXISTS kind, DROP COLUMN IF EXISTS community_id, DROP COLUMN IF EXISTS city,
  DROP COLUMN IF EXISTS logo_path, DROP COLUMN IF EXISTS cover_path, DROP COLUMN IF EXISTS contact,
  DROP COLUMN IF EXISTS meeting_place, DROP COLUMN IF EXISTS meeting_lat, DROP COLUMN IF EXISTS meeting_lng,
  DROP COLUMN IF EXISTS training_days, DROP COLUMN IF EXISTS training_time, DROP COLUMN IF EXISTS level,
  DROP COLUMN IF EXISTS pace_s_km, DROP COLUMN IF EXISTS max_members, DROP COLUMN IF EXISTS hidden_at,
  DROP COLUMN IF EXISTS updated_at;
CREATE VIEW public.clubs WITH (security_invoker = true) AS SELECT * FROM eventos.clubs;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.clubs TO anon, authenticated, service_role;
COMMIT;
