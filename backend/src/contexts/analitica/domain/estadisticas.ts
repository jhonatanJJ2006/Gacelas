/** Indicadores del panel admin (RPC admin_estadisticas, migracion 023). */
export interface Estadisticas {
  desde: string
  hasta: string
  totales: Record<string, number>
  serie: { dia: string; nuevos: number; activos: number; carreras: number; km: number }[]
}

export interface Periodo {
  desde: string
  hasta: string
}

const FECHA = /^\d{4}-\d{2}-\d{2}$/

/** Valida un periodo de fechas AAAA-MM-DD con desde <= hasta y como mucho 366 dias. */
export function validarPeriodo(p: Periodo): string | null {
  if (!FECHA.test(p.desde) || !FECHA.test(p.hasta)) return 'Fechas con formato AAAA-MM-DD'
  const a = Date.parse(p.desde)
  const b = Date.parse(p.hasta)
  if (Number.isNaN(a) || Number.isNaN(b)) return 'Fecha no valida'
  if (a > b) return 'La fecha inicial es posterior a la final'
  if ((b - a) / 86_400_000 > 366) return 'El periodo no puede superar un año'
  return null
}
