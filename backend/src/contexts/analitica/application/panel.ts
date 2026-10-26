import { validarPeriodo, type Estadisticas, type Periodo } from '../domain/estadisticas'
import type { ConsultaAnalitica } from '../domain/puertos'

/** VerPanelAdmin: valida el periodo antes de consultar. */
export async function verPanelAdmin(
  consulta: ConsultaAnalitica,
  p: Periodo
): Promise<Estadisticas> {
  const error = validarPeriodo(p)
  if (error) throw new Error(error)
  return consulta.estadisticas(p.desde, p.hasta)
}

/** ExportarReporte en CSV: una fila por dia de la serie. */
export function exportarSerieCsv(e: Estadisticas): string {
  const filas = e.serie.map((s) => [s.dia, s.nuevos, s.activos, s.carreras, s.km].join(','))
  return ['dia,nuevos,activos,carreras,km', ...filas].join('\n') + '\n'
}
