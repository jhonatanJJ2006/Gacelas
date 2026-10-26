import type { Estadisticas } from './estadisticas'

/** ConsultaAnalitica: lecturas de solo lectura (RPC o vistas). */
export interface ConsultaAnalitica {
  estadisticas(desde: string, hasta: string): Promise<Estadisticas>
}
