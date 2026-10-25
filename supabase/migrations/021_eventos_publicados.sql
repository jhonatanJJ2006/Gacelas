-- 021: los eventos con published = false solo los ven administradores y su creador.
-- Sustituye la politica de lectura abierta de 002 ("Events readable" USING true).

DROP POLICY IF EXISTS "Events readable" ON public.events;
DROP POLICY IF EXISTS "Eventos publicados visibles" ON public.events;
CREATE POLICY "Eventos publicados visibles" ON public.events FOR SELECT
  USING (
    published
    OR public.is_admin()
    OR (auth.uid() IS NOT NULL AND created_by = auth.uid())
  );

CREATE INDEX IF NOT EXISTS events_published_starts_idx ON public.events (published, starts_at);
