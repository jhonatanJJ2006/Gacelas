import type { Ajustes } from '../data/tipos'
import { notificacionesMock, type Evento, type Notificacion, type Ruta } from '../mocks/data'

export type PestanaRutas = 'Explorar' | 'Mis Rutas' | 'Guardadas'
export const PESTANAS_RUTAS: PestanaRutas[] = ['Explorar', 'Mis Rutas', 'Guardadas']

export function filtrarRutas(
  rutas: Ruta[],
  pestana: PestanaRutas,
  guardadas: string[],
  usuarioId: string,
  consulta = ''
): Ruta[] {
  const base =
    pestana === 'Mis Rutas'
      ? rutas.filter((r) => r.autorId === usuarioId || r.propia)
      : pestana === 'Guardadas'
        ? rutas.filter((r) => guardadas.includes(r.id))
        : rutas
  const q = consulta.trim().toLowerCase()
  return q
    ? base.filter(
        (r) => r.nombre.toLowerCase().includes(q) || r.descripcion.toLowerCase().includes(q)
      )
    : base
}

export type PestanaEventos = 'Próximos' | 'Mis eventos' | 'Pasados'
export const PESTANAS_EVENTOS: PestanaEventos[] = ['Próximos', 'Mis eventos', 'Pasados']

export function filtrarEventos(
  eventos: Evento[],
  pestana: PestanaEventos,
  inscritos: string[]
): Evento[] {
  if (pestana === 'Pasados') return eventos.filter((e) => e.pasado)
  if (pestana === 'Mis eventos')
    return eventos.filter((e) => inscritos.includes(e.id) || e.creadoPorUsuario)
  return eventos.filter((e) => !e.pasado)
}

export type FiltroNotificaciones = 'Todas' | 'Sin leer' | 'En vivo'

export function filtrarNotificaciones(
  lista: Notificacion[],
  filtro: FiltroNotificaciones
): Notificacion[] {
  if (filtro === 'Sin leer') return lista.filter((n) => !n.leida)
  if (filtro === 'En vivo') return lista.filter((n) => n.tipo === 'live')
  return lista
}

const PERMITIDA: Record<Notificacion['tipo'], keyof Ajustes> = {
  like: 'notifLikes',
  comment: 'notifComentarios',
  follow: 'notifSeguidores',
  event: 'notifEventos',
  live: 'notifEnVivo',
  streak: 'notifEventos',
  club: 'notifEventos',
  // El aviso de un reporte resuelto siempre se muestra.
  reporte: 'notifEventos',
}

/** Notificaciones visibles segun los ajustes y las ya leidas. */
export function notificacionesVisibles(
  a: Ajustes,
  lista: Notificacion[] = notificacionesMock
): Notificacion[] {
  return lista
    .filter((n) => a[PERMITIDA[n.tipo]] === true)
    .map((n) => (a.notificacionesLeidas.includes(n.id) ? { ...n, leida: true } : n))
}
