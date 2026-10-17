import { obtenerSupabase } from './supabase'

/** Guarda la imagen de post como vista previa (og:image) del enlace publico de la ruta. */
export async function subirVistaPrevia(rutaId: string, imagen: Blob) {
  const sb = obtenerSupabase()
  if (!sb) return
  await sb.storage
    .from('share-images')
    .upload(`rutas/${rutaId}.png`, imagen, { contentType: 'image/png', upsert: true })
    .catch(() => undefined)
}
