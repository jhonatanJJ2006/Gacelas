import { obtenerSupabase } from '../../../shared/supabase/client'
import {
  MAX_BYTES_FOTO,
  MAX_FOTOS,
  type IncidentePublico,
  type SeguridadZona,
  type TipoIncidente,
} from '../domain/incidentes'

/* Adaptadores de Supabase y Nominatim del contexto Seguridad de rutas. */

export async function seguridadZonaRemoto(rutaId: string): Promise<SeguridadZona | null> {
  const sb = obtenerSupabase()
  if (!sb) return null
  const { data, error } = await sb.rpc('seguridad_zona_ruta', { p_ruta_id: rutaId })
  if (error || !data) return null
  const d = data as {
    dias: SeguridadZona['dias']
    tipos: SeguridadZona['tipos']
    actualizado_hasta: string | null
    fuente: string
  }
  return {
    dias: d.dias ?? [],
    tipos: d.tipos ?? [],
    actualizadoHasta: d.actualizado_hasta,
    fuente: d.fuente,
  }
}

export async function incidentesRecientesRemoto(): Promise<IncidentePublico[]> {
  const sb = obtenerSupabase()
  if (!sb) return []
  const { data } = await sb
    .from('incidentes_publicos')
    .select('id,tipo,lat,lng,ocurrido_hora,confirmaciones,estado,fotos')
    .order('ocurrido_hora', { ascending: false })
    .limit(100)
  return (data ?? []).map((r) => ({
    id: r.id,
    tipo: r.tipo,
    lat: r.lat,
    lng: r.lng,
    ocurridoMs: new Date(r.ocurrido_hora).getTime(),
    confirmaciones: r.confirmaciones,
    estado: r.estado,
    fotos: r.fotos ?? [],
  }))
}

/** Direccion aproximada por geocodificacion inversa (OpenStreetMap Nominatim). */
export async function direccionDeRemoto(lat: number, lng: number): Promise<string> {
  const respaldo = `${lat.toFixed(4)}, ${lng.toFixed(4)}`
  try {
    const r = await fetch(
      `https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=17&accept-language=es&lat=${lat}&lon=${lng}`
    )
    if (!r.ok) return respaldo
    const j = (await r.json()) as { address?: Record<string, string>; display_name?: string }
    const a = j.address ?? {}
    const calle = a.road ?? a.pedestrian ?? a.path ?? a.footway
    const zona = a.city ?? a.town ?? a.village ?? ''
    return calle
      ? [calle, zona].filter(Boolean).join(', ')
      : (j.display_name ?? respaldo).slice(0, 200)
  } catch {
    return respaldo
  }
}

/** Reencodifica la imagen en un canvas: elimina EXIF (incluido GPS) y limita el tamano. */
export async function limpiarFoto(archivo: Blob, ladoMax = 1600): Promise<Blob> {
  const bmp = await createImageBitmap(archivo)
  const escala = Math.min(1, ladoMax / Math.max(bmp.width, bmp.height))
  const canvas = document.createElement('canvas')
  canvas.width = Math.round(bmp.width * escala)
  canvas.height = Math.round(bmp.height * escala)
  canvas.getContext('2d')!.drawImage(bmp, 0, 0, canvas.width, canvas.height)
  const blob = await new Promise<Blob | null>((ok) => canvas.toBlob(ok, 'image/jpeg', 0.82))
  if (!blob || blob.size > MAX_BYTES_FOTO) throw new Error('La foto es demasiado grande')
  return blob
}

export interface NuevoIncidente {
  tipo: TipoIncidente
  descripcion?: string
  lat: number
  lng: number
  direccion: string
  ocurridoMs: number
  fotos: Blob[]
}

export async function reportarIncidenteRemoto(n: NuevoIncidente): Promise<void> {
  const sb = obtenerSupabase()
  if (!sb) throw new Error('Sin conexión con el servidor')
  if (n.fotos.length > MAX_FOTOS) throw new Error(`Máximo ${MAX_FOTOS} fotos`)
  const carpeta = crypto.randomUUID()
  const rutas: string[] = []
  for (const [i, f] of n.fotos.entries()) {
    const limpia = await limpiarFoto(f)
    const ruta = `${carpeta}/${i}.jpg`
    const { error } = await sb.storage
      .from('incidentes')
      .upload(ruta, limpia, { contentType: 'image/jpeg' })
    if (error) throw new Error('No se pudo subir la foto')
    rutas.push(ruta)
  }
  const { error } = await sb.from('incidentes_usuario').insert({
    tipo: n.tipo,
    descripcion: n.descripcion?.trim() || null,
    geom: `SRID=4326;POINT(${n.lng} ${n.lat})`,
    direccion: n.direccion.slice(0, 200),
    ocurrido_at: new Date(n.ocurridoMs).toISOString(),
    fotos: rutas,
  })
  if (error) {
    if (rutas.length) await sb.storage.from('incidentes').remove(rutas)
    if (/Limite de reportes/.test(error.message))
      throw new Error('Llegaste al límite de reportes. Intenta más tarde.')
    if (/row-level security/.test(error.message))
      throw new Error('Activa el permiso de ubicación (GPS en carrera) para reportar.')
    throw new Error('No se pudo enviar el reporte')
  }
}

export async function confirmarIncidenteRemoto(id: string): Promise<void> {
  const sb = obtenerSupabase()
  if (!sb) return
  const { error } = await sb.from('incidentes_confirmaciones').insert({ incidente_id: id })
  if (error && !/duplicate/.test(error.message)) throw new Error('No se pudo confirmar')
}
