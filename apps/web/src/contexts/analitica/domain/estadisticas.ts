/** Indicadores del panel admin (admin_estadisticas, migracion 023). */
export interface Estadisticas {
  desde: string
  hasta: string
  totales: {
    usuarios_nuevos: number
    usuarios_activos: number
    carreras: number
    km: number
    eventos: number
    inscripciones: number
    reportes: number
    usuarios_total: number
  }
  serie: { dia: string; nuevos: number; activos: number; carreras: number; km: number }[]
  rutas_top: {
    id: string
    nombre: string
    oficial: boolean
    corridas: number
    corredores: number
  }[]
  eventos_top: { id: string; titulo: string; fecha: string; inscritos: number }[]
  retencion: { previos: number; vuelven: number; porcentaje: number }
  reportes: {
    por_estado: Record<string, number>
    por_tipo: Record<string, number>
    por_motivo: Record<string, number>
  }
}
