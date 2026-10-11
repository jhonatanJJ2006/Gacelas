-- 013: bucket publico con las versiones de la app Android (aviso "Hay una nueva version").
-- La app lee android/latest.json y descarga el APK sin token (el repo de GitHub es privado).
-- Solo se sube con la service_role key (scripts/publicar-android.sh): no hay politicas de escritura.

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('app-releases', 'app-releases', true, 157286400, array['application/vnd.android.package-archive', 'application/json'])
on conflict (id) do update set public = true, file_size_limit = 157286400,
  allowed_mime_types = array['application/vnd.android.package-archive', 'application/json'];
