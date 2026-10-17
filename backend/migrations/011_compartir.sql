-- 011: imagenes de vista previa (og:image) para el enlace publico /r/:id de una ruta.
-- Bucket publico; solo el autor de la ruta puede subir/reemplazar rutas/<id>.png.

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('share-images', 'share-images', true, 2097152, array['image/png'])
on conflict (id) do update set public = true, file_size_limit = 2097152, allowed_mime_types = array['image/png'];

create or replace function public.puede_subir_vista_previa(nombre text)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (
    select 1 from public.routes r
    where nombre = 'rutas/' || r.id || '.png' and r.author_id = auth.uid()
  )
$$;
revoke all on function public.puede_subir_vista_previa(text) from public, anon;
grant execute on function public.puede_subir_vista_previa(text) to authenticated;

drop policy if exists "Vista previa: subir la de mi ruta" on storage.objects;
create policy "Vista previa: subir la de mi ruta" on storage.objects for insert to authenticated
  with check (bucket_id = 'share-images' and public.puede_subir_vista_previa(name));

drop policy if exists "Vista previa: reemplazar la de mi ruta" on storage.objects;
create policy "Vista previa: reemplazar la de mi ruta" on storage.objects for update to authenticated
  using (bucket_id = 'share-images' and public.puede_subir_vista_previa(name))
  with check (bucket_id = 'share-images' and public.puede_subir_vista_previa(name));

-- upsert necesita poder leer el objeto existente
drop policy if exists "Vista previa: leer" on storage.objects;
create policy "Vista previa: leer" on storage.objects for select to authenticated
  using (bucket_id = 'share-images' and public.puede_subir_vista_previa(name));
