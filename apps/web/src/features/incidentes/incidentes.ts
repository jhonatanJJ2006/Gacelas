import { obtenerSupabase } from '../../lib/supabase'

/*
 * Incidentes de seguridad: datos abiertos del ECU 911 agregados por parroquia (sin hora ni
 * coordenadas) y reportes de corredores. Ver docs/INCIDENTES-ECU911.md y docs/INCIDENTES.md.
 */

export const TIPOS_INCIDENTE = [
  { id: 'robo_asalto', nombre: 'Robo o asalto', icono: 'report' },
  { id: 'acoso', nombre: 'Acoso', icono: 'front_hand' },
  { id: 'perros_agresivos', nombre: 'Perros agresivos', icono: 'pets' },
  { id: 'iluminacion_deficiente', nombre: 'Iluminación deficiente', icono: 'lightbulb' },
  { id: 'trafico_peligroso', nombre: 'Tráfico peligroso', icono: 'traffic' },
  { id: 'via_mal_estado', nombre: 'Vía en mal estado', icono: 'construction' },
  { id: 'accidente', nombre: 'Accidente', icono: 'car_crash' },
  { id: 'otro', nombre: 'Otro', icono: 'more_horiz' },
] as const
export type TipoIncidente = (typeof TIPOS_INCIDENTE)[number]['id']
export const nombreTipo = (t: string) => TIPOS_INCIDENTE.find((x) => x.id === t)?.nombre ?? t

export type Nivel = 'bajo' | 'medio' | 'alto'
export interface SeguridadZona {
  dias: { dia: number; nivel: Nivel; indice: number }[]
  tipos: { servicio: string; subtipo: string; conteo: number }[]
  actualizadoHasta: string | null
  fuente: string
}
export interface IncidentePublico {
  id: string
  tipo: TipoIncidente
  lat: number
  lng: number
  ocurridoMs: number
  confirmaciones: number
  estado: 'pendiente' | 'verificado'
  fotos: string[]
}

export const DIAS_CORTOS = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb']
export const AVISO_SEGURIDAD =
  'Indicador orientativo con datos agregados del ECU 911 por parroquia (sin hora exacta) y reportes de corredores. Llega con meses de retraso y no garantiza tu seguridad: mantente atento y, ante una emergencia, llama al 911.'
export const MAX_FOTOS = 3
export const MAX_BYTES_FOTO = 3 * 1024 * 1024

export const esVistaPrevia = () =>
  import.meta.env.DEV &&
  typeof window !== 'undefined' &&
  new URLSearchParams(window.location.search).has('vista-previa')

/** Datos de ejemplo (ficticios) para ?vista-previa. */
export const SEGURIDAD_EJEMPLO: SeguridadZona = {
  dias: [0.6, 0.9, 0.8, 0.9, 1.1, 1.6, 1.3].map((indice, dia) => ({
    dia,
    indice,
    nivel: indice < 0.75 ? 'bajo' : indice < 1.5 ? 'medio' : 'alto',
  })),
  tipos: [
    { servicio: 'Tránsito y Movilidad', subtipo: 'Ejemplo: siniestro de tránsito', conteo: 18 },
    { servicio: 'Seguridad Ciudadana', subtipo: 'Ejemplo: alerta de seguridad', conteo: 11 },
    { servicio: 'Gestión Sanitaria', subtipo: 'Ejemplo: atención prehospitalaria', conteo: 7 },
  ],
  actualizadoHasta: '2026-03',
  fuente: 'Ejemplo de vista previa. Fuente real: SIS ECU 911 (CC-BY) y reportes de corredores',
}

export function incidentesEjemplo(ahora = Date.now()): IncidentePublico[] {
  return [
    {
      id: 'vp-i1',
      tipo: 'perros_agresivos',
      lat: -3.994,
      lng: -79.203,
      ocurridoMs: ahora - 2 * 3600e3,
      confirmaciones: 3,
      estado: 'verificado',
      fotos: [],
    },
    {
      id: 'vp-i2',
      tipo: 'iluminacion_deficiente',
      lat: -3.986,
      lng: -79.199,
      ocurridoMs: ahora - 26 * 3600e3,
      confirmaciones: 1,
      estado: 'pendiente',
      fotos: [],
    },
    {
      id: 'vp-i3',
      tipo: 'trafico_peligroso',
      lat: -4.001,
      lng: -79.207,
      ocurridoMs: ahora - 50 * 3600e3,
      confirmaciones: 5,
      estado: 'verificado',
      fotos: [],
    },
  ]
}

export function nivelDeIndice(i: number): Nivel {
  return i < 0.75 ? 'bajo' : i < 1.5 ? 'medio' : 'alto'
}

export function colorNivel(n: Nivel) {
  return n === 'bajo' ? '#68DBA9' : n === 'medio' ? '#F5C451' : '#FF8A7A'
}

export async function seguridadZona(rutaId: string): Promise<SeguridadZona | null> {
  if (esVistaPrevia()) return SEGURIDAD_EJEMPLO
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

export async function incidentesRecientes(): Promise<IncidentePublico[]> {
  if (esVistaPrevia()) return incidentesEjemplo()
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
export async function direccionDe(lat: number, lng: number): Promise<string> {
  const respaldo = `${lat.toFixed(4)}, ${lng.toFixed(4)}`
  if (esVistaPrevia()) return 'Av. Orillas del Zamora, Loja'
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

export async function reportarIncidente(n: NuevoIncidente): Promise<void> {
  if (esVistaPrevia()) return
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

export async function confirmarIncidente(id: string): Promise<void> {
  if (esVistaPrevia()) return
  const sb = obtenerSupabase()
  if (!sb) return
  const { error } = await sb.from('incidentes_confirmaciones').insert({ incidente_id: id })
  if (error && !/duplicate/.test(error.message)) throw new Error('No se pudo confirmar')
}

export function haceTexto(ms: number, ahora = Date.now()) {
  const h = Math.round((ahora - ms) / 3600e3)
  if (h < 1) return 'hace menos de 1 h'
  if (h < 24) return `hace ${h} h`
  return `hace ${Math.round(h / 24)} d`
}
