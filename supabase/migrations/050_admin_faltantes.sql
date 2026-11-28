-- 050: partes del panel de administracion que faltaban por falta de datos (web y movil).
--
--   * Prioridad y SLA de los casos de moderacion: columna content_reports.priority (alta, media,
--     baja; la pone el servidor segun el motivo al crear el reporte) y objetivo de horas por
--     prioridad en app_settings (sla_horas_moderacion, editable en Ajustes). La cola con
--     prioridad y vencimiento sale de admin_cola_reportes_sla(); admin_cola_reportes() no cambia
--     para no romper las apps ya instaladas.
--   * Respuestas rapidas: plantillas de nota para el moderador (moderacion.respuestas_rapidas).
--   * SOS en vivo: admin_sos_alertas() lista las alertas recientes; la posicion solo se devuelve
--     mientras la alerta sigue activa (misma regla que el enlace publico sos_ver).
--   * Ultimo acceso y versiones en uso: la app registra al abrirse su plataforma y version
--     (registrar_uso_app). admin_ultimo_acceso() une ese dato con el ultimo inicio de sesion de
--     auth.users; admin_versiones_en_uso() cuenta usuarios por version.
--   * Versiones publicadas: admin.versiones_app, cargada con los releases reales de GitHub
--     hasta v1.0.1; scripts/publicar-android.sh anade cada version nueva (registrar_version_app).
--
-- Solo agrega objetos: no borra ni modifica filas existentes.
-- Para quitarlo: supabase/rollback/050_admin_faltantes.down.sql. Documentacion: docs/ADMIN.md.

BEGIN;

-- ---------- Prioridad y SLA ----------

ALTER TABLE moderacion.content_reports ADD COLUMN IF NOT EXISTS priority TEXT NOT NULL DEFAULT 'media';
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'content_reports_priority_check') THEN
    ALTER TABLE moderacion.content_reports
      ADD CONSTRAINT content_reports_priority_check CHECK (priority IN ('alta', 'media', 'baja'));
  END IF;
END $$;
COMMENT ON COLUMN moderacion.content_reports.priority IS 'Prioridad del caso (050): alta, media o baja. Al crear el reporte la decide el motivo; el moderador puede cambiarla.';

CREATE OR REPLACE FUNCTION moderacion.prioridad_por_motivo(motivo TEXT)
RETURNS TEXT LANGUAGE sql IMMUTABLE AS $$
  SELECT CASE
    WHEN motivo IN ('acoso', 'peligroso', 'suplantacion') THEN 'alta'
    WHEN motivo = 'spam' THEN 'baja'
    ELSE 'media'
  END
$$;

CREATE OR REPLACE FUNCTION moderacion.poner_prioridad_reporte()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  NEW.priority := moderacion.prioridad_por_motivo(NEW.reason);
  RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS content_reports_prioridad ON moderacion.content_reports;
CREATE TRIGGER content_reports_prioridad BEFORE INSERT ON moderacion.content_reports
  FOR EACH ROW EXECUTE FUNCTION moderacion.poner_prioridad_reporte();

-- Objetivo de respuesta en horas por prioridad. Es configuracion, editable en Ajustes del panel.
INSERT INTO admin.app_settings (key, value)
VALUES ('sla_horas_moderacion', '{"alta": 4, "media": 24, "baja": 72}'::jsonb)
ON CONFLICT (key) DO NOTHING;

CREATE OR REPLACE FUNCTION public.admin_horas_sla(prioridad TEXT)
RETURNS INTEGER LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT COALESCE(
    (SELECT (value ->> prioridad)::INTEGER FROM admin.app_settings WHERE key = 'sla_horas_moderacion'),
    CASE prioridad WHEN 'alta' THEN 4 WHEN 'baja' THEN 72 ELSE 24 END)
$$;

-- Misma cola que admin_cola_reportes (y el mismo control de rol) con prioridad y vencimiento.
CREATE OR REPLACE FUNCTION public.admin_cola_reportes_sla(estado TEXT DEFAULT NULL, tipo TEXT DEFAULT NULL)
RETURNS TABLE (id UUID, target_type TEXT, target_id TEXT, reason TEXT, detail TEXT, status TEXT,
               created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ, reportante TEXT, resumen TEXT,
               total_reportes BIGINT, acciones JSONB, prioridad TEXT, vence_en TIMESTAMPTZ)
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.es_moderador() THEN RAISE EXCEPTION 'solo moderadores'; END IF;
  RETURN QUERY
    SELECT q.id, q.target_type, q.target_id, q.reason, q.detail, q.status, q.created_at, q.updated_at,
           q.reportante, q.resumen, q.total_reportes, q.acciones, c.priority,
           q.created_at + make_interval(hours => public.admin_horas_sla(c.priority))
    FROM public.admin_cola_reportes(estado, tipo) q
    JOIN moderacion.content_reports c ON c.id = q.id;
END $$;

CREATE OR REPLACE FUNCTION public.admin_prioridad_reporte(reporte UUID, prioridad TEXT)
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.es_moderador() THEN RAISE EXCEPTION 'solo moderadores'; END IF;
  IF prioridad NOT IN ('alta', 'media', 'baja') THEN RAISE EXCEPTION 'prioridad invalida'; END IF;
  UPDATE moderacion.content_reports SET priority = prioridad, updated_at = now() WHERE id = reporte;
  IF NOT FOUND THEN RAISE EXCEPTION 'caso no encontrado'; END IF;
END $$;

-- ---------- Respuestas rapidas ----------

CREATE TABLE IF NOT EXISTS moderacion.respuestas_rapidas (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  titulo TEXT NOT NULL UNIQUE CHECK (char_length(titulo) BETWEEN 1 AND 60),
  texto TEXT NOT NULL CHECK (char_length(texto) BETWEEN 1 AND 500),
  created_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE moderacion.respuestas_rapidas ENABLE ROW LEVEL SECURITY;
-- Sin politicas: solo se lee y escribe con las funciones de abajo.

INSERT INTO moderacion.respuestas_rapidas (titulo, texto) VALUES
  ('Contenido retirado', 'Revisamos tu reporte y retiramos el contenido porque incumple las normas de la comunidad. Gracias por avisarnos.'),
  ('Sin infracción', 'Revisamos tu reporte y no encontramos una infracción de las normas de la comunidad. Gracias por ayudarnos a cuidar Runcity.'),
  ('Cuenta suspendida', 'Revisamos tu reporte y suspendimos la cuenta por incumplir las normas de la comunidad.'),
  ('En revisión', 'Estamos revisando tu reporte. Te avisaremos cuando el caso se cierre.')
ON CONFLICT (titulo) DO NOTHING;

CREATE OR REPLACE FUNCTION public.admin_respuestas_rapidas()
RETURNS TABLE (id UUID, titulo TEXT, texto TEXT, updated_at TIMESTAMPTZ)
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.es_moderador() THEN RAISE EXCEPTION 'solo moderadores'; END IF;
  RETURN QUERY SELECT r.id, r.titulo, r.texto, r.updated_at FROM moderacion.respuestas_rapidas r ORDER BY r.titulo;
END $$;

CREATE OR REPLACE FUNCTION public.admin_guardar_respuesta_rapida(p_id UUID, p_titulo TEXT, p_texto TEXT)
RETURNS UUID LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  res UUID;
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'solo administradores'; END IF;
  IF p_id IS NULL THEN
    INSERT INTO moderacion.respuestas_rapidas (titulo, texto, created_by)
    VALUES (btrim(p_titulo), btrim(p_texto), auth.uid()) RETURNING id INTO res;
  ELSE
    UPDATE moderacion.respuestas_rapidas SET titulo = btrim(p_titulo), texto = btrim(p_texto), updated_at = now()
    WHERE id = p_id RETURNING id INTO res;
    IF res IS NULL THEN RAISE EXCEPTION 'respuesta no encontrada'; END IF;
  END IF;
  RETURN res;
END $$;

CREATE OR REPLACE FUNCTION public.admin_borrar_respuesta_rapida(p_id UUID)
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'solo administradores'; END IF;
  DELETE FROM moderacion.respuestas_rapidas WHERE id = p_id;
END $$;

-- ---------- SOS en vivo ----------

-- Alertas de las ultimas [horas] horas. Activa = sin terminar y con posicion o inicio en las
-- ultimas 2 h (como sos_ver). La posicion y el trazo solo salen mientras esta activa.
CREATE OR REPLACE FUNCTION public.admin_sos_alertas(horas INTEGER DEFAULT 24)
RETURNS TABLE (id UUID, user_id UUID, nombre TEXT, activa BOOLEAN, started_at TIMESTAMPTZ,
               ended_at TIMESTAMPTZ, last_at TIMESTAMPTZ, lat DOUBLE PRECISION, lng DOUBLE PRECISION,
               puntos INTEGER)
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public, seguridad AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'solo administradores'; END IF;
  RETURN QUERY
    WITH a AS (
      SELECT s.*, (s.ended_at IS NULL AND COALESCE(s.last_at, s.started_at) > now() - interval '2 hours') AS viva
      FROM seguridad.sos_alertas s
      WHERE s.started_at > now() - make_interval(hours => LEAST(GREATEST(horas, 1), 720))
         OR s.ended_at IS NULL
    )
    SELECT a.id, a.user_id,
           COALESCE(NULLIF(p.full_name, ''), NULLIF(p.username, ''), 'Corredor de Runcity'),
           a.viva, a.started_at, a.ended_at, a.last_at,
           CASE WHEN a.viva THEN a.last_lat END, CASE WHEN a.viva THEN a.last_lng END,
           jsonb_array_length(a.points)
    FROM a LEFT JOIN identidad.profiles p ON p.id = a.user_id
    ORDER BY a.viva DESC, a.started_at DESC
    LIMIT 100;
END $$;

-- ---------- Uso de la app: ultimo acceso y versiones en uso ----------

CREATE TABLE IF NOT EXISTS admin.uso_app (
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  plataforma TEXT NOT NULL CHECK (plataforma IN ('android', 'ios', 'web')),
  version_name TEXT NOT NULL DEFAULT '' CHECK (char_length(version_name) <= 40),
  version_code INTEGER,
  visto_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, plataforma)
);
ALTER TABLE admin.uso_app ENABLE ROW LEVEL SECURITY;
-- Sin politicas: escribe registrar_uso_app y leen las funciones admin_*.

CREATE OR REPLACE FUNCTION public.registrar_uso_app(p_plataforma TEXT, p_version_name TEXT DEFAULT '', p_version_code INTEGER DEFAULT NULL)
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF auth.uid() IS NULL THEN RETURN; END IF;
  IF p_plataforma NOT IN ('android', 'ios', 'web') THEN RAISE EXCEPTION 'plataforma invalida'; END IF;
  INSERT INTO admin.uso_app (user_id, plataforma, version_name, version_code, visto_en)
  VALUES (auth.uid(), p_plataforma, left(COALESCE(p_version_name, ''), 40), p_version_code, now())
  ON CONFLICT (user_id, plataforma) DO UPDATE
    SET version_name = EXCLUDED.version_name, version_code = EXCLUDED.version_code, visto_en = now();
END $$;

CREATE OR REPLACE FUNCTION public.admin_ultimo_acceso()
RETURNS TABLE (id UUID, ultimo_acceso TIMESTAMPTZ, ultimo_inicio_sesion TIMESTAMPTZ,
               plataforma TEXT, version_name TEXT)
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'solo administradores'; END IF;
  RETURN QUERY
    SELECT u.id,
           CASE WHEN ua.visto_en IS NULL THEN u.last_sign_in_at
                WHEN u.last_sign_in_at IS NULL THEN ua.visto_en
                ELSE GREATEST(u.last_sign_in_at, ua.visto_en) END,
           u.last_sign_in_at, ua.plataforma, ua.version_name
    FROM auth.users u
    LEFT JOIN LATERAL (
      SELECT x.plataforma, x.version_name, x.visto_en FROM admin.uso_app x
      WHERE x.user_id = u.id ORDER BY x.visto_en DESC LIMIT 1
    ) ua ON TRUE;
END $$;

CREATE OR REPLACE FUNCTION public.admin_versiones_en_uso(dias INTEGER DEFAULT 30)
RETURNS TABLE (plataforma TEXT, version_name TEXT, version_code INTEGER, usuarios BIGINT, ultimo TIMESTAMPTZ)
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'solo administradores'; END IF;
  RETURN QUERY
    SELECT x.plataforma, x.version_name, max(x.version_code), count(*), max(x.visto_en)
    FROM admin.uso_app x
    WHERE x.visto_en > now() - make_interval(days => LEAST(GREATEST(dias, 1), 365))
    GROUP BY x.plataforma, x.version_name
    ORDER BY x.plataforma, max(x.version_code) DESC NULLS LAST;
END $$;

-- ---------- Versiones publicadas ----------

CREATE TABLE IF NOT EXISTS admin.versiones_app (
  plataforma TEXT NOT NULL DEFAULT 'android' CHECK (plataforma IN ('android', 'ios')),
  version_name TEXT NOT NULL,
  version_code INTEGER NOT NULL,
  canal TEXT NOT NULL CHECK (canal IN ('estable', 'prerelease')),
  publicada_en TIMESTAMPTZ NOT NULL,
  apk_bytes BIGINT,
  notas TEXT NOT NULL DEFAULT '',
  PRIMARY KEY (plataforma, version_name)
);
ALTER TABLE admin.versiones_app ENABLE ROW LEVEL SECURITY;

-- Releases reales de GitHub (jhonatanJJ2006/Gacelas) hasta v1.0.1, con el tamano del APK publicado
-- (GitHub tiene dos releases v0.9.3 con el mismo APK; se guarda el primero).
INSERT INTO admin.versiones_app (version_name, version_code, canal, publicada_en, apk_bytes) VALUES
  ('0.5.0', 500, 'estable', '2026-10-11T16:19:29Z', 37414929),
  ('0.5.0-pre.1', 500, 'prerelease', '2026-10-11T15:19:44Z', 37300241),
  ('0.5.1', 501, 'prerelease', '2026-10-11T17:06:34Z', 37464081),
  ('0.5.2', 502, 'prerelease', '2026-10-11T18:58:24Z', 37464081),
  ('0.5.3', 503, 'prerelease', '2026-10-11T19:17:41Z', 37464081),
  ('0.5.4', 504, 'prerelease', '2026-10-11T20:07:04Z', 37464081),
  ('0.6.0', 600, 'prerelease', '2026-10-11T21:00:16Z', 49501670),
  ('0.6.1', 601, 'prerelease', '2026-10-11T22:59:21Z', 49502616),
  ('0.6.2', 602, 'prerelease', '2026-10-11T23:12:20Z', 49535492),
  ('0.6.3', 603, 'prerelease', '2026-10-11T23:57:51Z', 49535492),
  ('0.6.4', 604, 'prerelease', '2026-10-12T00:22:21Z', 49551876),
  ('0.6.5', 605, 'prerelease', '2026-10-12T01:21:33Z', 49711353),
  ('0.6.6', 606, 'prerelease', '2026-10-12T01:44:17Z', 49744121),
  ('0.6.7', 607, 'prerelease', '2026-10-12T02:09:08Z', 49760509),
  ('0.6.8', 608, 'prerelease', '2026-10-12T02:35:48Z', 49809657),
  ('0.6.9', 609, 'prerelease', '2026-10-12T15:45:26Z', 49826041),
  ('0.6.10', 610, 'estable', '2026-10-13T13:04:17Z', 50023669),
  ('0.7.0', 700, 'estable', '2026-10-17T17:55:13Z', 50056457),
  ('0.8.0', 800, 'estable', '2026-10-20T17:34:58Z', 34376361),
  ('0.8.1', 801, 'prerelease', '2026-10-26T14:24:45Z', 103805448),
  ('0.8.2', 802, 'prerelease', '2026-10-26T17:38:53Z', 103997072),
  ('0.8.3', 803, 'prerelease', '2026-10-26T18:56:19Z', 103965967),
  ('0.8.4', 804, 'prerelease', '2026-10-27T00:21:31Z', 51525300),
  ('0.8.5', 805, 'prerelease', '2026-10-27T00:40:03Z', 51525300),
  ('0.8.9', 809, 'prerelease', '2026-10-27T02:05:12Z', 51558170),
  ('0.9.0', 900, 'prerelease', '2026-10-27T03:08:03Z', 51574550),
  ('0.9.1', 901, 'prerelease', '2026-10-27T03:59:58Z', 51590934),
  ('0.9.2', 902, 'prerelease', '2026-10-27T04:10:41Z', 51590938),
  ('0.9.3', 903, 'prerelease', '2026-10-27T13:01:59Z', 51590938),
  ('0.9.4', 904, 'prerelease', '2026-10-27T14:00:39Z', 51607514),
  ('0.9.5', 905, 'prerelease', '2026-10-27T14:13:23Z', 51607514),
  ('0.9.6', 906, 'prerelease', '2026-10-27T21:35:35Z', 51623930),
  ('0.9.7', 907, 'estable', '2026-10-31T19:53:52Z', 51623934),
  ('0.9.8', 908, 'estable', '2026-10-31T22:19:02Z', 51771390),
  ('0.9.9', 909, 'estable', '2026-11-01T00:07:56Z', 51754904),
  ('0.9.10', 910, 'estable', '2026-11-02T00:40:54Z', 51787676),
  ('0.9.11', 911, 'estable', '2026-11-05T03:38:40Z', 51820448),
  ('0.9.12', 912, 'prerelease', '2026-11-12T13:37:37Z', 55089826),
  ('0.9.13', 913, 'prerelease', '2026-11-12T14:01:29Z', 52281903),
  ('0.9.14', 914, 'prerelease', '2026-11-13T14:11:33Z', 44951576),
  ('0.9.15', 915, 'prerelease', '2026-11-13T15:39:47Z', 44951576),
  ('0.9.16', 916, 'prerelease', '2026-11-13T16:50:46Z', 45099052),
  ('0.9.17', 917, 'prerelease', '2026-11-13T18:37:12Z', 45295692),
  ('0.9.18', 918, 'prerelease', '2026-11-17T16:10:34Z', 45328896),
  ('0.9.19', 919, 'prerelease', '2026-11-17T22:36:59Z', 45328896),
  ('0.9.20', 920, 'prerelease', '2026-11-18T12:16:42Z', 45969809),
  ('0.9.21', 921, 'prerelease', '2026-11-18T15:58:25Z', 45666913),
  ('0.9.22', 922, 'prerelease', '2026-11-18T16:31:55Z', 45666917),
  ('1.0.0', 10000, 'prerelease', '2026-11-21T23:12:40Z', 45683301),
  ('1.0.1', 10001, 'prerelease', '2026-11-22T00:17:09Z', 39876927)

ON CONFLICT (plataforma, version_name) DO NOTHING;

CREATE OR REPLACE FUNCTION public.admin_versiones_publicadas()
RETURNS TABLE (plataforma TEXT, version_name TEXT, version_code INTEGER, canal TEXT,
               publicada_en TIMESTAMPTZ, apk_bytes BIGINT, notas TEXT)
LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF NOT public.is_admin() THEN RAISE EXCEPTION 'solo administradores'; END IF;
  RETURN QUERY
    SELECT v.plataforma, v.version_name, v.version_code, v.canal, v.publicada_en, v.apk_bytes, v.notas
    FROM admin.versiones_app v ORDER BY v.publicada_en DESC LIMIT 60;
END $$;

-- La llama scripts/publicar-android.sh con la service_role key despues de publicar.
CREATE OR REPLACE FUNCTION public.registrar_version_app(p_version_name TEXT, p_version_code INTEGER, p_canal TEXT,
  p_apk_bytes BIGINT DEFAULT NULL, p_notas TEXT DEFAULT '', p_plataforma TEXT DEFAULT 'android')
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  IF COALESCE(auth.role(), '') <> 'service_role' AND NOT public.is_admin() THEN
    RAISE EXCEPTION 'Solo service_role o admin' USING ERRCODE = '42501';
  END IF;
  INSERT INTO admin.versiones_app (plataforma, version_name, version_code, canal, publicada_en, apk_bytes, notas)
  VALUES (p_plataforma, p_version_name, p_version_code, p_canal, now(), p_apk_bytes, left(COALESCE(p_notas, ''), 2000))
  ON CONFLICT (plataforma, version_name) DO UPDATE
    SET version_code = EXCLUDED.version_code, canal = EXCLUDED.canal,
        apk_bytes = COALESCE(EXCLUDED.apk_bytes, admin.versiones_app.apk_bytes),
        notas = CASE WHEN EXCLUDED.notas = '' THEN admin.versiones_app.notas ELSE EXCLUDED.notas END;
END $$;

-- ---------- Permisos ----------

REVOKE ALL ON FUNCTION public.admin_horas_sla(TEXT) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.admin_cola_reportes_sla(TEXT, TEXT) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.admin_prioridad_reporte(UUID, TEXT) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.admin_respuestas_rapidas() FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.admin_guardar_respuesta_rapida(UUID, TEXT, TEXT) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.admin_borrar_respuesta_rapida(UUID) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.admin_sos_alertas(INTEGER) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.registrar_uso_app(TEXT, TEXT, INTEGER) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.admin_ultimo_acceso() FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.admin_versiones_en_uso(INTEGER) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.admin_versiones_publicadas() FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.registrar_version_app(TEXT, INTEGER, TEXT, BIGINT, TEXT, TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.admin_horas_sla(TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_cola_reportes_sla(TEXT, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_prioridad_reporte(UUID, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_respuestas_rapidas() TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_guardar_respuesta_rapida(UUID, TEXT, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_borrar_respuesta_rapida(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_sos_alertas(INTEGER) TO authenticated;
GRANT EXECUTE ON FUNCTION public.registrar_uso_app(TEXT, TEXT, INTEGER) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_ultimo_acceso() TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_versiones_en_uso(INTEGER) TO authenticated;
GRANT EXECUTE ON FUNCTION public.admin_versiones_publicadas() TO authenticated;
GRANT EXECUTE ON FUNCTION public.registrar_version_app(TEXT, INTEGER, TEXT, BIGINT, TEXT, TEXT) TO authenticated, service_role;

INSERT INTO kernel.propiedad_tablas (tabla, contexto, esquema_actual)
SELECT t.tabla, t.contexto, t.esquema
FROM (VALUES ('respuestas_rapidas', 'moderacion', 'moderacion'), ('uso_app', 'admin', 'admin'),
             ('versiones_app', 'admin', 'admin')) AS t(tabla, contexto, esquema)
WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'kernel' AND table_name = 'propiedad_tablas')
ON CONFLICT DO NOTHING;

COMMIT;
