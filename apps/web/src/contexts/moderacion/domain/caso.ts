import type { TipoObjetivo } from './denuncia'

/** Prioridad de un caso (050): la decide el motivo y el moderador puede cambiarla. */
export type Prioridad = 'alta' | 'media' | 'baja'

export type EstadoReporte = 'pendiente' | 'en_revision' | 'resuelto' | 'descartado'
export type AccionModeracion =
  | 'en_revision'
  | 'resuelto'
  | 'descartado'
  | 'reabierto'
  | 'ocultar_contenido'
  | 'suspender_usuario'
  | 'nota'
export interface AccionReporte {
  accion: AccionModeracion
  nota: string
  fecha: string
  moderador: string
}
/** Caso de la cola generica (public.admin_cola_reportes, migracion 023). */
export interface ReporteAdmin {
  id: string
  target_type: TipoObjetivo
  target_id: string
  /** Compatibilidad con la cola anterior (solo publicaciones). */
  post_id?: string
  reason: string
  detail: string
  status: EstadoReporte
  created_at: string
  reportante: string
  resumen: string
  total_reportes: number
  acciones: AccionReporte[]
  /** Prioridad y vencimiento del plazo (admin_cola_reportes_sla, migracion 050). */
  prioridad?: Prioridad
  vence_en?: string | null
}
