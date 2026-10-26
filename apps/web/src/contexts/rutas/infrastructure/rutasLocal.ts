import { alternar, escribir, leer } from '../../../shared/lib/almacenLocal'
import { type RutasRepository } from '../domain/puertos'

export class LocalRutasRepository implements RutasRepository {
  async guardadas() {
    return leer<string[]>('rutas-guardadas', [])
  }
  async alternarGuardada(id: string) {
    return escribir('rutas-guardadas', alternar(await this.guardadas(), id))
  }
}
