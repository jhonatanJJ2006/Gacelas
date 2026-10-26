-- 034: comunidades y grupos de running (contexto Eventos y clubes). Idempotente.
-- Ver docs/adr/006-comunidades-y-grupos.md. Extiende eventos.clubs en vez de crear tablas nuevas:
--  - kind: 'comunidad' (colectivo amplio) o 'grupo' (equipo que entrena junto). Los clubes que ya
--    existian quedan como grupos.
--  - Un grupo puede pertenecer a una comunidad (community_id). Una comunidad no pertenece a nada.
--  - Grupo: punto de encuentro, dias y hora de entrenamiento, nivel, ritmo y cupo opcional.
--  - Comunidad: ciudad, logo y portada opcionales, contacto o redes.
--  - requires_approval sigue siendo "privado / con solicitud de ingreso".
--  - Nombres unicos por tipo sin distinguir mayusculas ni espacios repetidos.
--  - Limite anti spam: 3 comunidades y 5 grupos por usuario, y no mas de 3 creaciones en 24 horas.
--  - hidden_at: los admins de Gacelas los ocultan desde la cola de moderacion (tipos 'comunidad' y
--    'grupo' en report_target_types). Solo el owner y los admins ven uno oculto.
-- Segun ADR 005 la vista public.clubs se recrea con las columnas nuevas al final.

ALTER TABLE eventos.clubs ADD COLUMN IF NOT EXISTS kind TEXT NOT NULL DEFAULT 'grupo';
ALTER TABLE eventos.clubs ADD COLUMN IF NOT EXISTS community_id UUID REFERENCES eventos.clubs(id) ON DELETE SET NULL;
ALTER TABLE eventos.clubs ADD COLUMN IF NOT EXISTS city TEXT NOT NULL DEFAULT 'Loja';
ALTER TABLE eventos.clubs ADD COLUMN IF NOT EXISTS logo_path TEXT;
ALTER TABLE eventos.clubs ADD COLUMN IF NOT EXISTS cover_path TEXT;
ALTER TABLE eventos.clubs ADD COLUMN IF NOT EXISTS contact TEXT NOT NULL DEFAULT '';
ALTER TABLE eventos.clubs ADD COLUMN IF NOT EXISTS meeting_place TEXT NOT NULL DEFAULT '';
ALTER TABLE eventos.clubs ADD COLUMN IF NOT EXISTS meeting_lat DOUBLE PRECISION;
ALTER TABLE eventos.clubs ADD COLUMN IF NOT EXISTS meeting_lng DOUBLE PRECISION;
ALTER TABLE eventos.clubs ADD COLUMN IF NOT EXISTS training_days SMALLINT[] NOT NULL DEFAULT '{}';
ALTER TABLE eventos.clubs ADD COLUMN IF NOT EXISTS training_time TIME;
ALTER TABLE eventos.clubs ADD COLUMN IF NOT EXISTS level TEXT NOT NULL DEFAULT 'todos';
ALTER TABLE eventos.clubs ADD COLUMN IF NOT EXISTS pace_s_km INTEGER;
ALTER TABLE eventos.clubs ADD COLUMN IF NOT EXISTS max_members INTEGER;
ALTER TABLE eventos.clubs ADD COLUMN IF NOT EXISTS hidden_at TIMESTAMPTZ;
ALTER TABLE eventos.clubs ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE eventos.clubs DROP CONSTRAINT IF EXISTS clubs_comunidad_grupo_check;
ALTER TABLE eventos.clubs ADD CONSTRAINT clubs_comunidad_grupo_check CHECK (
  kind IN ('comunidad', 'grupo')
  AND char_length(city) <= 60
  AND char_length(contact) <= 200
  AND char_length(meeting_place) <= 120
  AND (logo_path IS NULL OR char_length(logo_path) BETWEEN 3 AND 500)
  AND (cover_path IS NULL OR char_length(cover_path) BETWEEN 3 AND 500)
  AND (meeting_lat IS NULL OR meeting_lat BETWEEN -90 AND 90)
  AND (meeting_lng IS NULL OR meeting_lng BETWEEN -180 AND 180)
  AND (meeting_lat IS NULL) = (meeting_lng IS NULL)
  AND training_days <@ ARRAY[1, 2, 3, 4, 5, 6, 7]::SMALLINT[]
  AND level IN ('todos', 'principiante', 'intermedio', 'avanzado')
  AND (pace_s_km IS NULL OR pace_s_km BETWEEN 150 AND 900)
  AND (max_members IS NULL OR max_members BETWEEN 2 AND 1000)
  AND (kind = 'grupo' OR community_id IS NULL)
  AND community_id IS DISTINCT FROM id);

CREATE UNIQUE INDEX IF NOT EXISTS clubs_nombre_unico
  ON eventos.clubs (kind, lower(regexp_replace(btrim(name), '\s+', ' ', 'g')));
CREATE INDEX IF NOT EXISTS clubs_comunidad_idx ON eventos.clubs (community_id) WHERE community_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS clubs_owner_idx ON eventos.clubs (owner_id, created_at DESC);

CREATE OR REPLACE VIEW public.clubs WITH (security_invoker = true) AS SELECT * FROM eventos.clubs;

-- Protecciones: contadores, owner, tipo fijo, comunidad valida, oculto solo por admins, limites.
CREATE OR REPLACE FUNCTION public.club_proteger() RETURNS trigger
LANGUAGE plpgsql SET search_path = public AS $$
DECLARE
  es_moderador BOOLEAN := is_admin() OR current_user NOT IN ('authenticated', 'anon');
  limite INTEGER;
BEGIN
  IF pg_trigger_depth() > 1 THEN RETURN NEW; END IF;
  IF TG_OP = 'INSERT' THEN
    NEW.members_count := 0;
    IF NOT es_moderador THEN NEW.hidden_at := NULL; END IF;
    IF NOT is_admin() AND current_user IN ('authenticated', 'anon') THEN
      limite := (CASE WHEN NEW.kind = 'comunidad' THEN 3 ELSE 5 END);
      IF (SELECT count(*) FROM eventos.clubs WHERE owner_id = NEW.owner_id AND kind = NEW.kind) >= limite THEN
        RAISE EXCEPTION 'Llegaste al limite de % % que puedes crear', limite,
          (CASE WHEN NEW.kind = 'comunidad' THEN 'comunidades' ELSE 'grupos' END)
          USING ERRCODE = 'check_violation';
      END IF;
      IF (SELECT count(*) FROM eventos.clubs WHERE owner_id = NEW.owner_id
          AND created_at > now() - interval '24 hours') >= 3 THEN
        RAISE EXCEPTION 'Ya creaste 3 comunidades o grupos hoy, intenta mañana'
          USING ERRCODE = 'check_violation';
      END IF;
    END IF;
  ELSE
    NEW.members_count := OLD.members_count;
    NEW.owner_id := OLD.owner_id;
    NEW.kind := OLD.kind;
    NEW.created_at := OLD.created_at;
    NEW.updated_at := now();
    IF NOT es_moderador THEN NEW.hidden_at := OLD.hidden_at; END IF;
  END IF;
  IF NEW.community_id IS NOT NULL AND (TG_OP = 'INSERT' OR NEW.community_id IS DISTINCT FROM OLD.community_id) THEN
    IF NOT EXISTS (SELECT 1 FROM eventos.clubs WHERE id = NEW.community_id AND kind = 'comunidad' AND hidden_at IS NULL) THEN
      RAISE EXCEPTION 'La comunidad elegida no existe' USING ERRCODE = 'check_violation';
    END IF;
  END IF;
  RETURN NEW;
END $$;

-- Altas de miembros: rol y estado los decide el servidor; cupo y ocultos.
CREATE OR REPLACE FUNCTION public.club_miembro_antes() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE c eventos.clubs;
BEGIN
  IF pg_trigger_depth() > 1 THEN RETURN NEW; END IF;
  SELECT * INTO c FROM eventos.clubs WHERE id = NEW.club_id;
  IF TG_OP = 'INSERT' THEN
    IF c.hidden_at IS NOT NULL THEN RAISE EXCEPTION 'Este grupo no está disponible'; END IF;
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
  IF NEW.status = 'active' AND (TG_OP = 'INSERT' OR OLD.status <> 'active') AND c.max_members IS NOT NULL
     AND (SELECT count(*) FROM eventos.club_members WHERE club_id = NEW.club_id AND status = 'active') >= c.max_members THEN
    RAISE EXCEPTION 'El grupo está lleno (cupo de %)', c.max_members USING ERRCODE = 'check_violation';
  END IF;
  RETURN NEW;
END $$;

-- Visibilidad: ocultos solo para su owner y los admins.
DROP POLICY IF EXISTS "Clubes visibles" ON eventos.clubs;
CREATE POLICY "Clubes visibles" ON eventos.clubs FOR SELECT USING (
  hidden_at IS NULL OR owner_id = auth.uid() OR is_admin());

-- Cola de moderacion: comunidades y grupos se pueden reportar y ocultar.
CREATE OR REPLACE FUNCTION public.moderacion_resumen_club(objetivo TEXT) RETURNS TEXT
LANGUAGE sql STABLE SET search_path = public AS $$
  SELECT left(c.name || coalesce(': ' || nullif(c.description, ''), ''), 140)
  FROM eventos.clubs c WHERE c.id::text = objetivo
$$;
CREATE OR REPLACE FUNCTION public.moderacion_ocultar_club(objetivo TEXT) RETURNS VOID
LANGUAGE sql SET search_path = public AS $$
  UPDATE eventos.clubs SET hidden_at = now() WHERE id::text = objetivo
$$;
CREATE OR REPLACE FUNCTION public.moderacion_autor_club(objetivo TEXT) RETURNS UUID
LANGUAGE sql STABLE SET search_path = public AS $$
  SELECT c.owner_id FROM eventos.clubs c WHERE c.id::text = objetivo
$$;
REVOKE ALL ON FUNCTION public.moderacion_resumen_club(TEXT) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.moderacion_ocultar_club(TEXT) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.moderacion_autor_club(TEXT) FROM PUBLIC;

INSERT INTO public.report_target_types (key, etiqueta, referencia, resumen_fn, ocultar_fn, autor_fn) VALUES
  ('comunidad', 'Comunidad', 'la comunidad', 'public.moderacion_resumen_club', 'public.moderacion_ocultar_club', 'public.moderacion_autor_club'),
  ('grupo', 'Grupo de running', 'el grupo', 'public.moderacion_resumen_club', 'public.moderacion_ocultar_club', 'public.moderacion_autor_club')
ON CONFLICT (key) DO UPDATE SET etiqueta = EXCLUDED.etiqueta, referencia = EXCLUDED.referencia,
  resumen_fn = EXCLUDED.resumen_fn, ocultar_fn = EXCLUDED.ocultar_fn, autor_fn = EXCLUDED.autor_fn;
