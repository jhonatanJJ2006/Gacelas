import { create } from 'zustand'
import { notificacionesMock, type Notificacion } from '../../../mocks/data'
import { type NotificacionesRepository } from '../domain/puertos'

export interface EnlacesAvisos {
  /** Sin repositorio de notificaciones, las leidas se guardan en los ajustes. */
  marcarEnAjustes(ids: string[]): Promise<void>
}

let repos: { notificaciones?: NotificacionesRepository } = {}
let enlaces: EnlacesAvisos = { marcarEnAjustes: async () => undefined }

export function configurarAvisos(
  r: { notificaciones?: NotificacionesRepository },
  e?: EnlacesAvisos
) {
  repos = r
  if (e) enlaces = e
}

export const repositoriosAvisos = () => repos

export interface EstadoAvisos {
  /** Notificaciones de la cuenta (o las de ejemplo sin backend). */
  notificaciones: Notificacion[]
  marcarNotificacionesLeidas(ids: string[]): Promise<void>
}

export const useAvisos = create<EstadoAvisos>((set) => ({
  notificaciones: notificacionesMock,
  async marcarNotificacionesLeidas(ids) {
    set((s) => ({
      notificaciones: s.notificaciones.map((n) => (ids.includes(n.id) ? { ...n, leida: true } : n)),
    }))
    if (repos.notificaciones) return repos.notificaciones.marcarLeidas(ids)
    await enlaces.marcarEnAjustes(ids)
  },
}))
