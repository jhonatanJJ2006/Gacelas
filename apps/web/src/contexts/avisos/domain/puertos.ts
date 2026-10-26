import type { Notificacion } from '../../../mocks/data'

export interface NotificacionesRepository {
  listar(): Promise<Notificacion[]>
  marcarLeidas(ids: string[]): Promise<void>
}
