import { type CarreraGuardada } from '../../../shared/kernel/tipos'
import { escribir, leer } from '../../../shared/lib/almacenLocal'
import { type CarrerasRepository } from '../domain/puertos'

export class LocalCarrerasRepository implements CarrerasRepository {
  async listar() {
    return leer<CarreraGuardada[]>('carreras', [])
  }
  async guardar(c: CarreraGuardada) {
    return escribir('carreras', [c, ...(await this.listar()).filter((x) => x.id !== c.id)])
  }
  async eliminar(id: string) {
    return escribir(
      'carreras',
      (await this.listar()).filter((x) => x.id !== id)
    )
  }
}
