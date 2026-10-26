import { actividadesMock, usuarioPorId } from '../../../mocks/data'
import type { PublicacionRemota } from '../../../shared/kernel/comunidad'
import type { CarreraGuardada, PuntoGps } from '../../../shared/kernel/tipos'
import { tiempoRelativo } from '../../../shared/lib/tiempo'

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

const ritmoTexto = (r: string) => {
  const [mi, se] = r.split(':').map(Number)
  return mi * 60 + se
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

/** Km por autor sumando sus publicaciones visibles en el feed. */
export function kmPorAutor(posts: Publicacion[]): Map<string, number> {
  const m = new Map<string, number>()
  for (const p of posts) {
    const k = p.propia ? 'yo' : (p.autorId ?? p.autor)
    m.set(k, (m.get(k) ?? 0) + p.distanciaKm)
  }
  return m
}
