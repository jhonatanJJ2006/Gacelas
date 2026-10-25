import type { Estadisticas } from '../domain/estadisticas'

/** ConsultaAnalitica: RPC de solo lectura. */
export interface ConsultaAnalitica {
  estadisticas(desde: string, hasta: string): Promise<Estadisticas>
}
