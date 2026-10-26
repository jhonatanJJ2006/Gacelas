-- Prueba de 034: comunidades y grupos de running sobre eventos.clubs (vista public.clubs).
-- Creador como owner, comunidad valida, nombres unicos, limite de creacion, solicitudes,
-- cupo, edicion solo por admins del grupo, ocultar desde la cola de moderacion.
BEGIN;
GRANT USAGE ON SCHEMA public, auth TO authenticated;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA auth TO authenticated;
INSERT INTO auth.users (id, email) VALUES
  ('00000000-0000-0000-0000-0000000034a1', 'a34@prueba.ec'),
  ('00000000-0000-0000-0000-0000000034b1', 'b34@prueba.ec'),
  ('00000000-0000-0000-0000-0000000034c1', 'c34@prueba.ec'),
  ('00000000-0000-0000-0000-0000000034d1', 'd34@prueba.ec')
ON CONFLICT DO NOTHING;
INSERT INTO public.user_roles (user_id, role) VALUES ('00000000-0000-0000-0000-0000000034d1', 'admin')
  ON CONFLICT DO NOTHING;

SET LOCAL ROLE authenticated;
-- A crea una comunidad y un grupo dentro de ella.
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000034a1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000034a1","role":"authenticated"}', true);
DO $$
DECLARE com UUID; gru UUID; rol TEXT;
BEGIN
  INSERT INTO public.clubs (name, description, kind, city, contact, requires_approval, owner_id)
    VALUES ('Runners Prueba 034', 'Comunidad de prueba', 'comunidad', 'Loja', '@runners034', false, auth.uid())
    RETURNING id INTO com;
  INSERT INTO public.clubs (name, kind, community_id, meeting_place, meeting_lat, meeting_lng,
      training_days, training_time, level, pace_s_km, max_members, requires_approval, owner_id, members_count)
    VALUES ('Grupo Prueba 034', 'grupo', com, 'Parque Jipiro', -3.97, -79.20, '{2,4}', '06:00', 'intermedio',
      330, 2, true, auth.uid(), 99)
    RETURNING id INTO gru;
  SELECT role INTO rol FROM public.club_members WHERE club_id = gru AND user_id = auth.uid();
  IF rol IS DISTINCT FROM 'owner' THEN RAISE EXCEPTION 'El creador no quedo como owner'; END IF;
  IF (SELECT members_count FROM public.clubs WHERE id = gru) <> 1 THEN RAISE EXCEPTION 'Contador manipulable'; END IF;
  -- Una comunidad no puede pertenecer a otra; un grupo no puede colgar de un grupo.
  BEGIN
    INSERT INTO public.clubs (name, kind, community_id, owner_id) VALUES ('Sub 034', 'comunidad', com, auth.uid());
    RAISE EXCEPTION 'Comunidad dentro de comunidad aceptada';
  EXCEPTION WHEN check_violation THEN NULL; END;
  BEGIN
    INSERT INTO public.clubs (name, kind, community_id, owner_id) VALUES ('Hijo 034', 'grupo', gru, auth.uid());
    RAISE EXCEPTION 'Grupo dentro de grupo aceptado';
  EXCEPTION WHEN check_violation THEN NULL; END;
  -- Nombre unico por tipo, sin distinguir mayusculas ni espacios.
  BEGIN
    INSERT INTO public.clubs (name, kind, owner_id) VALUES ('  grupo   prueba 034 ', 'grupo', auth.uid());
    RAISE EXCEPTION 'Nombre duplicado aceptado';
  EXCEPTION WHEN unique_violation THEN NULL; END;
  -- Limite de 3 creaciones en 24 horas (ya van 2).
  INSERT INTO public.clubs (name, kind, owner_id) VALUES ('Tercero 034', 'grupo', auth.uid());
  BEGIN
    INSERT INTO public.clubs (name, kind, owner_id) VALUES ('Cuarto 034', 'grupo', auth.uid());
    RAISE EXCEPTION 'Limite diario no aplicado';
  EXCEPTION WHEN check_violation THEN NULL; END;
  -- El tipo no cambia al editar.
  UPDATE public.clubs SET kind = 'comunidad', description = 'Editado' WHERE id = gru;
  IF (SELECT kind FROM public.clubs WHERE id = gru) <> 'grupo' THEN RAISE EXCEPTION 'El tipo cambio'; END IF;
  -- El owner no puede autoocultarse ni desocultarse.
  UPDATE public.clubs SET hidden_at = now() WHERE id = gru;
  IF (SELECT hidden_at FROM public.clubs WHERE id = gru) IS NOT NULL THEN RAISE EXCEPTION 'hidden_at editable'; END IF;
  PERFORM set_config('prueba.grupo', gru::text, true);
END $$;

-- B pide unirse (grupo con solicitud), no puede editar; C tambien pide.
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000034b1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000034b1","role":"authenticated"}', true);
DO $$
DECLARE gru UUID := current_setting('prueba.grupo')::uuid; n INT;
BEGIN
  INSERT INTO public.club_members (club_id, user_id, role, status) VALUES (gru, auth.uid(), 'admin', 'active');
  IF (SELECT status FROM public.club_members WHERE club_id = gru AND user_id = auth.uid()) <> 'pending' THEN
    RAISE EXCEPTION 'La solicitud no quedo pendiente';
  END IF;
  UPDATE public.clubs SET name = 'Hackeado 034' WHERE id = gru;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n <> 0 THEN RAISE EXCEPTION 'Un no admin edito el grupo'; END IF;
  DELETE FROM public.clubs WHERE id = gru;
  GET DIAGNOSTICS n = ROW_COUNT;
  IF n <> 0 THEN RAISE EXCEPTION 'Un no admin borro el grupo'; END IF;
  INSERT INTO public.content_reports (target_type, target_id, reporter_id, reason)
    VALUES ('grupo', gru::text, auth.uid(), 'spam');
END $$;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000034c1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000034c1","role":"authenticated"}', true);
INSERT INTO public.club_members (club_id, user_id) VALUES (current_setting('prueba.grupo')::uuid, auth.uid());

-- A aprueba a B (cupo 2 se llena) y ya no puede aprobar a C.
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000034a1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000034a1","role":"authenticated"}', true);
DO $$
DECLARE gru UUID := current_setting('prueba.grupo')::uuid;
BEGIN
  UPDATE public.club_members SET status = 'active' WHERE club_id = gru AND user_id = '00000000-0000-0000-0000-0000000034b1';
  BEGIN
    UPDATE public.club_members SET status = 'active' WHERE club_id = gru AND user_id = '00000000-0000-0000-0000-0000000034c1';
    RAISE EXCEPTION 'Cupo no aplicado';
  EXCEPTION WHEN check_violation THEN NULL; END;
END $$;

-- El admin de Gacelas oculta el grupo desde la cola de moderacion.
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000034d1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000034d1","role":"authenticated"}', true);
SELECT public.moderar_reporte(
  (SELECT id FROM public.content_reports WHERE target_type = 'grupo' AND target_id = current_setting('prueba.grupo')),
  'ocultar_contenido', 'prueba');

-- C ya no lo ve ni puede unirse; A (owner) si lo ve.
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000034c1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000034c1","role":"authenticated"}', true);
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM public.clubs WHERE id = current_setting('prueba.grupo')::uuid) THEN
    RAISE EXCEPTION 'Un grupo oculto sigue visible';
  END IF;
END $$;
SELECT set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-0000000034a1', true);
SELECT set_config('request.jwt.claims', '{"sub":"00000000-0000-0000-0000-0000000034a1","role":"authenticated"}', true);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM public.clubs WHERE id = current_setting('prueba.grupo')::uuid AND hidden_at IS NOT NULL) THEN
    RAISE EXCEPTION 'El owner no ve su grupo oculto';
  END IF;
  RAISE NOTICE 'OK 034: comunidades y grupos';
END $$;
RESET ROLE;
ROLLBACK;
