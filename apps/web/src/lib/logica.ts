/** Logica de pantallas sin dependencias de React, separada para poder probarla. */
import type { CarreraGuardada, PuntoGps } from '../data/tipos'
import type { PublicacionRemota } from '../data/repositorios'
import {
  actividadesMock,
  rutasMock,
  rutasSugeridasMock,
  usuarioPorId,
  type Notificacion,
} from '../mocks/data'

export interface Publicacion {
  id: string
  autor: string
  /** Id del autor en Supabase (solo publicaciones reales de otros). */
  autorId?: string
  propia: boolean
  titulo: string
  tiempo: string
  distanciaKm: number
  segundos: number
  ritmoSegKm: number | null
  /** Desnivel positivo (m). */
  desnivelM?: number
  imagenes?: string[]
  compartidos?: number
  compartida?: boolean
  autorKm?: number
  ultimoComentario?: { autor: string; texto: string } | null
  likes: number
  comentarios: number
  puntos: PuntoGps[]
  /** true si el usuario actual le dio me gusta (solo feed real). */
  meGusta?: boolean
  /** true si la publicacion existe en Supabase (se puede dar me gusta y comentar). */
  remota?: boolean
  avatar?: string | null
  /** Inicio de la carrera (ordenar el feed). */
  inicioMs?: number
}

export type FiltroFeed = 'Todos' | 'Siguiendo' | 'Comunidad' | 'Mías'

export const tiempoRelativo = (ms: number, ahora: number) => {
  if (!Number.isFinite(ms)) return ''
  const min = Math.max(0, Math.round((ahora - ms) / 60_000))
  if (min < 60) return `hace ${min} min`
  if (min < 1440) return `hace ${Math.round(min / 60)}h`
  return `hace ${Math.round(min / 1440)}d`
}

const ritmoTexto = (r: string) => {
  const [mi, se] = r.split(':').map(Number)
  return mi * 60 + se
}

export interface Opcion {
  id: string
  name: string
  distance: number
  duration: number
  elevation: number
  color: 'primary' | 'secondary'
}

/** Valida el formulario de nuevo evento; devuelve el error o null. */
export function validarEvento(e: {
  titulo: string
  fecha: string
  hora: string
  ubicacion: string
  distanciaKm: string
}): string | null {
  if (e.titulo.trim().length < 3) return 'El título debe tener al menos 3 caracteres'
  if (!e.fecha) return 'Elige la fecha'
  if (!e.hora) return 'Elige la hora'
  if (!e.ubicacion.trim()) return 'Indica el punto de encuentro'
  const km = Number(e.distanciaKm)
  if (!Number.isFinite(km) || km <= 0 || km > 100) return 'La distancia debe estar entre 1 y 100 km'
  return null
}

/** Une las carreras publicas del usuario con la actividad de la comunidad. */
export function construirFeed(
  carreras: CarreraGuardada[],
  nombre: string,
  ahora: number,
  remotas: PublicacionRemota[] = [],
  social = false
): Publicacion[] {
  const subidas = new Set(remotas.map((r) => r.clienteId).filter(Boolean) as string[])
  const propias: Publicacion[] = carreras
    .filter((c) => c.publica && !subidas.has(c.id))
    .map((c) => ({
      id: `c-${c.id}`,
      autor: nombre,
      propia: true,
      titulo: c.titulo,
      tiempo: tiempoRelativo(c.inicioMs, ahora),
      inicioMs: c.inicioMs,
      distanciaKm: c.distanciaKm,
      segundos: c.segundos,
      ritmoSegKm: c.distanciaKm > 0.01 ? Math.floor(c.segundos / c.distanciaKm) : null,
      desnivelM: c.desnivelM,
      likes: 0,
      comentarios: 0,
      puntos: c.puntos,
    }))
  const reales: Publicacion[] = remotas.map((r) => ({
    id: r.id,
    autor: r.propia ? nombre : r.autor,
    autorId: r.propia ? undefined : r.autorId,
    propia: Boolean(r.propia),
    titulo: r.titulo,
    tiempo: tiempoRelativo(r.inicioMs, ahora),
    inicioMs: r.inicioMs,
    distanciaKm: r.distanciaKm,
    segundos: r.segundos,
    ritmoSegKm: r.distanciaKm > 0.01 ? Math.floor(r.segundos / r.distanciaKm) : null,
    desnivelM: r.desnivelM ?? 0,
    imagenes: r.imagenes ?? [],
    compartidos: r.compartidos ?? 0,
    compartida: Boolean(r.compartida),
    autorKm: r.autorKm,
    ultimoComentario: r.ultimoComentario ?? null,
    likes: r.likes,
    comentarios: r.comentarios,
    puntos: r.puntos,
    meGusta: Boolean(r.meGusta),
    remota: true,
    avatar: r.avatar ?? null,
  }))
  // Con backend (social) el feed es solo de la base: nunca se mezclan publicaciones de ejemplo.
  const comunidad: Publicacion[] =
    social || reales.length
      ? []
      : actividadesMock.map((a, i) => ({
          id: `a-${a.id}`,
          autor: usuarioPorId(a.usuarioId)?.nombre ?? 'Corredor',
          propia: false,
          titulo: a.titulo,
          tiempo: a.fecha,
          inicioMs: ahora - (i + 7) * 86_400_000,
          distanciaKm: a.distanciaKm,
          segundos: a.duracionSegundos,
          ritmoSegKm: ritmoTexto(a.ritmo),
          likes: a.likes,
          comentarios: a.comentarios,
          puntos: [],
        }))
  return [...propias, ...reales, ...comunidad].sort((x, y) => (y.inicioMs ?? 0) - (x.inicioMs ?? 0))
}

export function filtrarFeed(
  lista: Publicacion[],
  filtro: FiltroFeed,
  ocultas: string[],
  siguiendo: string[] = []
) {
  return lista
    .filter((p) => !ocultas.includes(p.id))
    .filter((p) =>
      filtro === 'Mías'
        ? p.propia
        : filtro === 'Comunidad'
          ? !p.propia
          : filtro === 'Siguiendo'
            ? Boolean(p.autorId && siguiendo.includes(p.autorId))
            : true
    )
}

/** Estimacion simple: alrededor de 1 kcal por kg y km, con 70 kg de referencia. */
export const caloriasEstimadas = (km: number, pesoKg = 70) => Math.round(km * pesoKg)

export function calidadSenal(precisionM: number) {
  if (precisionM <= 10) return 'Señal fuerte'
  if (precisionM <= 30) return 'Señal media'
  return 'Señal débil'
}

export function saludo(hora: number) {
  if (hora < 12) return 'Buenos días'
  if (hora < 19) return 'Buenas tardes'
  return 'Buenas noches'
}

/** Pantalla a la que lleva cada tipo de notificacion. */
export function destinoNotificacion(n: Notificacion): string {
  switch (n.tipo) {
    case 'event':
      return '/comunidad/eventos/1'
    case 'follow':
      return '/perfil'
    case 'live':
      return '/comunidad/en-vivo'
    case 'club':
      return '/comunidad/clubes'
    default:
      return '/comunidad'
  }
}

/** Reduce la foto a 256 px como maximo para que quepa en localStorage. */
export async function leerFoto(archivo: File, lado = 256): Promise<string> {
  const url = await new Promise<string>((ok, mal) => {
    const lector = new FileReader()
    lector.onload = () => ok(String(lector.result))
    lector.onerror = () => mal(lector.error)
    lector.readAsDataURL(archivo)
  })
  try {
    const img = await new Promise<HTMLImageElement>((ok, mal) => {
      const i = new Image()
      i.onload = () => ok(i)
      i.onerror = mal
      i.src = url
    })
    const escala = Math.min(1, lado / Math.max(img.width, img.height))
    const canvas = document.createElement('canvas')
    canvas.width = Math.round(img.width * escala)
    canvas.height = Math.round(img.height * escala)
    const ctx = canvas.getContext('2d')
    if (!ctx) return url
    ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
    return canvas.toDataURL('image/jpeg', 0.85)
  } catch {
    return url
  }
}

/** Rutas sugeridas mas las rutas de Loja, filtradas por el texto buscado. */
export function opcionesBuscador(consulta: string, rutaPreferida: string | null): Opcion[] {
  const sugeridas: Opcion[] = rutasSugeridasMock.map((r) => ({
    id: r.id,
    name: r.nombre,
    distance: r.distanciaKm,
    duration: r.minutos,
    elevation: r.elevacionM,
    color: r.principal ? 'primary' : 'secondary',
  }))
  const deLoja: Opcion[] = rutasMock.map((r) => ({
    id: `ruta-${r.id}`,
    name: r.nombre,
    distance: r.distanciaKm,
    // Estimacion a 5:20 /km.
    duration: Math.round(r.distanciaKm * 5.33),
    elevation: r.elevacionM,
    color: 'secondary',
  }))
  const q = consulta.trim().toLowerCase()
  const todas = [...sugeridas, ...deLoja]
  const filtradas = q ? todas.filter((o) => o.name.toLowerCase().includes(q)) : sugeridas
  const preferida = rutaPreferida ? todas.find((o) => o.id === `ruta-${rutaPreferida}`) : undefined
  return preferida && !filtradas.includes(preferida) ? [preferida, ...filtradas] : filtradas
}

/** Etiqueta de una alternativa: Óptima (la más corta), Plano (menos desnivel) o Trail (más desnivel). */
export function etiquetaOpcion(o: Opcion, todas: Opcion[]): 'Óptima' | 'Plano' | 'Trail' | null {
  if (todas.length < 2) return null
  const min = (f: (x: Opcion) => number) => todas.reduce((a, b) => (f(b) < f(a) ? b : a))
  if (min((x) => x.distance) === o) return 'Óptima'
  if (min((x) => x.elevation) === o) return 'Plano'
  if (min((x) => -x.elevation) === o) return 'Trail'
  return null
}

/** Indicaciones simplificadas segun la distancia recorrida (sin mapas todavia). */
export function indicacion(distanciaKm: number): { metros: number; texto: string } {
  const pasos = [
    { km: 0.5, texto: 'Sigue recto por la Av. Universitaria' },
    { km: 1.2, texto: 'Gira a la derecha en Av. Universitaria' },
    { km: 2.5, texto: 'Continúa junto al río Zamora' },
    { km: 4.0, texto: 'Gira a la izquierda hacia el Parque Jipiro' },
    { km: 6.0, texto: 'Rodea el lago del Parque Jipiro' },
  ]
  const siguiente = pasos.find((p) => p.km > distanciaKm)
  if (!siguiente) return { metros: 0, texto: 'Llegaste al final de la ruta' }
  return { metros: Math.round((siguiente.km - distanciaKm) * 1000), texto: siguiente.texto }
}

/** Nivel del corredor segun km acumulados (misma regla que Android NivelCorredor). */
export function nivelCorredor(km: number): string {
  if (km >= 500) return 'Gacela Élite'
  if (km >= 150) return 'Gacela Avanzada'
  if (km >= 30) return 'Gacela Intermedia'
  return 'Gacela Principiante'
}

/** Km por autor sumando sus publicaciones visibles en el feed. */
export function kmPorAutor(posts: Publicacion[]): Map<string, number> {
  const m = new Map<string, number>()
  for (const p of posts) {
    const k = p.propia ? 'yo' : (p.autorId ?? p.autor)
    m.set(k, (m.get(k) ?? 0) + p.distanciaKm)
  }
  return m
}
