import type { Evento } from '../../../mocks/data'
import { alternar, escribir, leer } from '../../../shared/lib/almacenLocal'
import { type EventosRepository } from '../domain/puertos'

export class LocalEventosRepository implements EventosRepository {
  async creados() {
    return leer<Evento[]>('eventos-creados', [])
  }
  async inscritos() {
    return leer<string[]>('eventos-inscritos', [])
  }
  async crear(e: Evento) {
    escribir('eventos-inscritos', Array.from(new Set([...(await this.inscritos()), e.id])))
    return escribir('eventos-creados', [e, ...(await this.creados())])
  }
  async alternarInscripcion(id: string) {
    return escribir('eventos-inscritos', alternar(await this.inscritos(), id))
  }
}
