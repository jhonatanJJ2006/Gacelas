import { type Notificacion, notificacionesMock } from '../../../mocks/data'
import type { Ajustes } from '../../../shared/kernel/tipos'

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
