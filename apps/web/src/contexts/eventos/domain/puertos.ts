import type { Evento } from '../../../mocks/data'

export interface EventosRepository {
  creados(): Promise<Evento[]>
  inscritos(): Promise<string[]>
  crear(e: Evento): Promise<Evento[]>
  alternarInscripcion(id: string): Promise<string[]>
}
