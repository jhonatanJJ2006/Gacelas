-- 012: Fase 5 - clubes, eventos con cupo y lista de espera, recordatorios y en vivo por evento.

-- ===== Clubes =====
CREATE TABLE IF NOT EXISTS public.clubs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL CHECK (char_length(btrim(name)) BETWEEN 3 AND 60),
  description TEXT NOT NULL DEFAULT '' CHECK (char_length(description) <= 500),
  location TEXT NOT NULL DEFAULT '' CHECK (char_length(location) <= 80),
  requires_approval BOOLEAN NOT NULL DEFAULT false,
  owner_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  members_count INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.club_members (
  club_id UUID NOT NULL REFERENCES public.clubs(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  role TEXT NOT NULL DEFAULT 'member' CHECK (role IN ('owner', 'admin', 'member')),
  status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'pending')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (club_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.club_posts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  club_id UUID NOT NULL REFERENCES public.clubs(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  body TEXT NOT NULL CHECK (char_length(btrim(body)) BETWEEN 1 AND 1000),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS club_posts_club_idx ON public.club_posts (club_id, created_at DESC);

-- Rol activo de un usuario en un club (null si no es miembro activo).
CREATE OR REPLACE FUNCTION public.rol_club(club UUID, usuario UUID DEFAULT auth.uid())
RETURNS TEXT LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT role FROM club_members WHERE club_id = club AND user_id = usuario AND status = 'active'
$$;
GRANT EXECUTE ON FUNCTION public.rol_club(UUID, UUID) TO anon, authenticated;

-- Al crear un club, el creador queda como owner.
CREATE OR REPLACE FUNCTION public.club_alta_owner() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  INSERT INTO club_members (club_id, user_id, role, status) VALUES (NEW.id, NEW.owner_id, 'owner', 'active');
  RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS club_alta_owner ON public.clubs;
CREATE TRIGGER club_alta_owner AFTER INSERT ON public.clubs FOR EACH ROW EXECUTE FUNCTION public.club_alta_owner();

-- Proteger contadores y el owner.
CREATE OR REPLACE FUNCTION public.club_proteger() RETURNS trigger
LANGUAGE plpgsql SET search_path = public AS $$
BEGIN
  IF pg_trigger_depth() > 1 THEN RETURN NEW; END IF;
  IF TG_OP = 'INSERT' THEN NEW.members_count := 0;
  ELSE NEW.members_count := OLD.members_count; NEW.owner_id := OLD.owner_id; END IF;
  RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS club_proteger ON public.clubs;
CREATE TRIGGER club_proteger BEFORE INSERT OR UPDATE ON public.clubs FOR EACH ROW EXECUTE FUNCTION public.club_proteger();

-- Altas de miembros: rol y estado los decide el servidor.
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
DROP TRIGGER IF EXISTS club_miembro_antes ON public.club_members;
CREATE TRIGGER club_miembro_antes BEFORE INSERT OR UPDATE ON public.club_members FOR EACH ROW EXECUTE FUNCTION public.club_miembro_antes();

CREATE OR REPLACE FUNCTION public.club_contar() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE cid UUID := COALESCE(NEW.club_id, OLD.club_id);
BEGIN
  UPDATE clubs SET members_count = (SELECT count(*) FROM club_members WHERE club_id = cid AND status = 'active') WHERE id = cid;
  -- Avisar al usuario cuando lo aprueban.
  IF TG_OP = 'UPDATE' AND OLD.status = 'pending' AND NEW.status = 'active' THEN
    INSERT INTO notifications (user_id, type, actor_name, message)
    SELECT NEW.user_id, 'club', name, 'Te aceptaron en el club ' || name FROM clubs WHERE id = cid;
  END IF;
  RETURN NULL;
END $$;
DROP TRIGGER IF EXISTS club_contar ON public.club_members;
CREATE TRIGGER club_contar AFTER INSERT OR UPDATE OR DELETE ON public.club_members FOR EACH ROW EXECUTE FUNCTION public.club_contar();

ALTER TABLE public.clubs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.club_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.club_posts ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Clubes visibles" ON public.clubs;
CREATE POLICY "Clubes visibles" ON public.clubs FOR SELECT USING (true);
DROP POLICY IF EXISTS "Crear club" ON public.clubs;
CREATE POLICY "Crear club" ON public.clubs FOR INSERT TO authenticated WITH CHECK (owner_id = auth.uid());
DROP POLICY IF EXISTS "Editar club" ON public.clubs;
CREATE POLICY "Editar club" ON public.clubs FOR UPDATE TO authenticated USING (rol_club(id) IN ('owner', 'admin'));
DROP POLICY IF EXISTS "Borrar club" ON public.clubs;
CREATE POLICY "Borrar club" ON public.clubs FOR DELETE TO authenticated USING (owner_id = auth.uid());

DROP POLICY IF EXISTS "Ver miembros" ON public.club_members;
CREATE POLICY "Ver miembros" ON public.club_members FOR SELECT USING (
  status = 'active' OR user_id = auth.uid() OR rol_club(club_id) IN ('owner', 'admin'));
DROP POLICY IF EXISTS "Unirme" ON public.club_members;
CREATE POLICY "Unirme" ON public.club_members FOR INSERT TO authenticated WITH CHECK (user_id = auth.uid());
DROP POLICY IF EXISTS "Gestionar miembros" ON public.club_members;
CREATE POLICY "Gestionar miembros" ON public.club_members FOR UPDATE TO authenticated
  USING (rol_club(club_id) IN ('owner', 'admin')) WITH CHECK (rol_club(club_id) IN ('owner', 'admin'));
DROP POLICY IF EXISTS "Salir o expulsar" ON public.club_members;
CREATE POLICY "Salir o expulsar" ON public.club_members FOR DELETE TO authenticated USING (
  role <> 'owner' AND (user_id = auth.uid() OR rol_club(club_id) IN ('owner', 'admin')));

DROP POLICY IF EXISTS "Ver posts del club" ON public.club_posts;
CREATE POLICY "Ver posts del club" ON public.club_posts FOR SELECT USING (
  rol_club(club_id) IS NOT NULL OR NOT (SELECT requires_approval FROM clubs WHERE id = club_id));
DROP POLICY IF EXISTS "Publicar en el club" ON public.club_posts;
CREATE POLICY "Publicar en el club" ON public.club_posts FOR INSERT TO authenticated
  WITH CHECK (user_id = auth.uid() AND rol_club(club_id) IS NOT NULL);
DROP POLICY IF EXISTS "Borrar posts del club" ON public.club_posts;
CREATE POLICY "Borrar posts del club" ON public.club_posts FOR DELETE TO authenticated
  USING (user_id = auth.uid() OR rol_club(club_id) IN ('owner', 'admin'));

-- ===== Eventos =====
ALTER TABLE public.notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE public.notifications ADD CONSTRAINT notifications_type_check
  CHECK (type IN ('like', 'comment', 'follow', 'event', 'live', 'streak', 'club'));

ALTER TABLE public.events ADD COLUMN IF NOT EXISTS club_id UUID REFERENCES public.clubs(id) ON DELETE CASCADE;
ALTER TABLE public.events ADD COLUMN IF NOT EXISTS route_id TEXT REFERENCES public.routes(id) ON DELETE SET NULL;
ALTER TABLE public.events ADD COLUMN IF NOT EXISTS max_participants INTEGER CHECK (max_participants IS NULL OR max_participants BETWEEN 1 AND 5000);
ALTER TABLE public.events ADD COLUMN IF NOT EXISTS kind TEXT NOT NULL DEFAULT 'grupal' CHECK (kind IN ('grupal', 'carrera', 'entrenamiento'));
ALTER TABLE public.events ADD COLUMN IF NOT EXISTS meeting_lat DOUBLE PRECISION;
ALTER TABLE public.events ADD COLUMN IF NOT EXISTS meeting_lng DOUBLE PRECISION;

-- Eventos de club: solo admins/owner.
DROP POLICY IF EXISTS "Own events insert" ON public.events;
CREATE POLICY "Own events insert" ON public.events FOR INSERT TO authenticated WITH CHECK (
  created_by = auth.uid() AND (club_id IS NULL OR rol_club(club_id) IN ('owner', 'admin')));

ALTER TABLE public.event_rsvps ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'going' CHECK (status IN ('going', 'waitlist'));

-- Cupo: si esta lleno, la inscripcion entra a la lista de espera.
CREATE OR REPLACE FUNCTION public.rsvp_cupo() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE cupo INTEGER;
BEGIN
  SELECT max_participants INTO cupo FROM events WHERE id = NEW.event_id FOR UPDATE;
  NEW.status := CASE WHEN cupo IS NOT NULL AND
    (SELECT count(*) FROM event_rsvps WHERE event_id = NEW.event_id AND status = 'going') >= cupo
    THEN 'waitlist' ELSE 'going' END;
  RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS rsvp_cupo ON public.event_rsvps;
CREATE TRIGGER rsvp_cupo BEFORE INSERT ON public.event_rsvps FOR EACH ROW EXECUTE FUNCTION public.rsvp_cupo();

-- Si alguien deja su lugar, sube el primero de la lista de espera y se le avisa.
CREATE OR REPLACE FUNCTION public.rsvp_promover() RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE sig UUID; titulo TEXT;
BEGIN
  IF OLD.status <> 'going' THEN RETURN NULL; END IF;
  SELECT user_id INTO sig FROM event_rsvps WHERE event_id = OLD.event_id AND status = 'waitlist'
    ORDER BY created_at LIMIT 1 FOR UPDATE SKIP LOCKED;
  IF sig IS NOT NULL THEN
    UPDATE event_rsvps SET status = 'going' WHERE event_id = OLD.event_id AND user_id = sig;
    SELECT title INTO titulo FROM events WHERE id = OLD.event_id;
    INSERT INTO notifications (user_id, type, actor_name, message)
      VALUES (sig, 'event', titulo, 'Se liberó un cupo: ya estás inscrito en ' || titulo);
  END IF;
  RETURN NULL;
END $$;
DROP TRIGGER IF EXISTS rsvp_promover ON public.event_rsvps;
CREATE TRIGGER rsvp_promover AFTER DELETE ON public.event_rsvps FOR EACH ROW EXECUTE FUNCTION public.rsvp_promover();

-- Estado de un evento para la UI (inscritos, espera y mi lugar), sin exponer quienes son.
CREATE OR REPLACE FUNCTION public.evento_estado(evento TEXT)
RETURNS TABLE (inscritos INTEGER, en_espera INTEGER, cupo INTEGER, mi_estado TEXT, mi_posicion INTEGER)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT
    (SELECT count(*)::int FROM event_rsvps WHERE event_id = evento AND status = 'going'),
    (SELECT count(*)::int FROM event_rsvps WHERE event_id = evento AND status = 'waitlist'),
    (SELECT max_participants FROM events WHERE id = evento),
    (SELECT status FROM event_rsvps WHERE event_id = evento AND user_id = auth.uid()),
    (SELECT count(*)::int + 1 FROM event_rsvps w, event_rsvps yo
       WHERE yo.event_id = evento AND yo.user_id = auth.uid() AND yo.status = 'waitlist'
         AND w.event_id = evento AND w.status = 'waitlist' AND w.created_at < yo.created_at)
$$;
GRANT EXECUTE ON FUNCTION public.evento_estado(TEXT) TO anon, authenticated;

-- Recordatorios 1 dia y 1 hora antes (cron cada 10 min, sin duplicados).
CREATE TABLE IF NOT EXISTS public.event_reminders (
  event_id TEXT NOT NULL REFERENCES public.events(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  kind TEXT NOT NULL CHECK (kind IN ('dia', 'hora')),
  PRIMARY KEY (event_id, user_id, kind)
);
ALTER TABLE public.event_reminders ENABLE ROW LEVEL SECURITY;

CREATE OR REPLACE FUNCTION public.eventos_recordatorios() RETURNS INTEGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE n INTEGER;
BEGIN
  WITH due AS (
    SELECT r.event_id, r.user_id, e.title,
      CASE WHEN e.starts_at - now() <= interval '1 hour' THEN 'hora' ELSE 'dia' END AS kind
    FROM event_rsvps r JOIN events e ON e.id = r.event_id
    WHERE r.status = 'going' AND e.starts_at > now() AND e.starts_at - now() <= interval '24 hours'
  ), nuevos AS (
    INSERT INTO event_reminders (event_id, user_id, kind)
    SELECT event_id, user_id, kind FROM due ON CONFLICT DO NOTHING
    RETURNING event_id, user_id, kind
  ), avisos AS (
    INSERT INTO notifications (user_id, type, actor_name, message)
    SELECT n.user_id, 'event', d.title,
      CASE n.kind WHEN 'hora' THEN d.title || ' empieza en menos de 1 hora' ELSE d.title || ' es mañana' END
    FROM nuevos n JOIN due d USING (event_id, user_id, kind)
    RETURNING 1
  ) SELECT count(*) INTO n FROM avisos;
  RETURN n;
END $$;
REVOKE ALL ON FUNCTION public.eventos_recordatorios() FROM PUBLIC, anon, authenticated;
SELECT cron.unschedule('gacelas-eventos-recordatorios') WHERE EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'gacelas-eventos-recordatorios');
SELECT cron.schedule('gacelas-eventos-recordatorios', '*/10 * * * *', 'SELECT public.eventos_recordatorios()');

-- ===== En vivo en eventos =====
ALTER TABLE public.live_sessions ADD COLUMN IF NOT EXISTS event_id TEXT REFERENCES public.events(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS live_sessions_event_idx ON public.live_sessions (event_id) WHERE event_id IS NOT NULL;

-- Los inscritos (y el organizador) ven las sesiones del evento; el ocultado de casa sigue aplicando.
DROP POLICY IF EXISTS "Ver sesiones del evento" ON public.live_sessions;
CREATE POLICY "Ver sesiones del evento" ON public.live_sessions FOR SELECT TO authenticated USING (
  event_id IS NOT NULL AND (
    EXISTS (SELECT 1 FROM event_rsvps WHERE event_id = live_sessions.event_id AND user_id = auth.uid() AND status = 'going')
    OR EXISTS (SELECT 1 FROM events WHERE id = live_sessions.event_id AND created_by = auth.uid())));

CREATE OR REPLACE FUNCTION public.live_iniciar_evento(evento TEXT)
RETURNS UUID LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE nueva UUID;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM event_rsvps WHERE event_id = evento AND user_id = auth.uid() AND status = 'going') THEN
    RAISE EXCEPTION 'no estás inscrito en este evento';
  END IF;
  nueva := live_iniciar(NULL);
  UPDATE live_sessions SET event_id = evento WHERE id = nueva;
  RETURN nueva;
END $$;
REVOKE ALL ON FUNCTION public.live_iniciar_evento(TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.live_iniciar_evento(TEXT) TO authenticated;

-- Nombres de los participantes del evento en vivo (solo para quien puede ver el evento en vivo).
CREATE OR REPLACE FUNCTION public.evento_en_vivo(evento TEXT)
RETURNS TABLE (session_id UUID, user_id UUID, nombre TEXT, distance_m DOUBLE PRECISION, elapsed_s INTEGER,
  last_lat DOUBLE PRECISION, last_lng DOUBLE PRECISION, hidden BOOLEAN, ended BOOLEAN, points JSONB)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT s.id, s.user_id, COALESCE(NULLIF(p.full_name, ''), p.username, 'Corredor'), s.distance_m, s.elapsed_s,
    CASE WHEN s.hidden THEN NULL ELSE s.last_lat END, CASE WHEN s.hidden THEN NULL ELSE s.last_lng END,
    s.hidden, s.ended_at IS NOT NULL, s.points
  FROM live_sessions s LEFT JOIN profiles p ON p.id = s.user_id
  WHERE s.event_id = evento AND (
    EXISTS (SELECT 1 FROM event_rsvps WHERE event_id = evento AND user_id = auth.uid() AND status = 'going')
    OR EXISTS (SELECT 1 FROM events WHERE id = evento AND created_by = auth.uid()))
  ORDER BY s.distance_m DESC NULLS LAST
$$;
REVOKE ALL ON FUNCTION public.evento_en_vivo(TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.evento_en_vivo(TEXT) TO authenticated;

-- El cliente no puede pasarse de la lista de espera a inscrito editando su fila.
CREATE OR REPLACE FUNCTION public.rsvp_estado_fijo() RETURNS trigger
LANGUAGE plpgsql SET search_path = public AS $$
BEGIN
  IF pg_trigger_depth() <= 1 THEN NEW.status := OLD.status; NEW.event_id := OLD.event_id; NEW.user_id := OLD.user_id; END IF;
  RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS rsvp_estado_fijo ON public.event_rsvps;
CREATE TRIGGER rsvp_estado_fijo BEFORE UPDATE ON public.event_rsvps FOR EACH ROW EXECUTE FUNCTION public.rsvp_estado_fijo();
