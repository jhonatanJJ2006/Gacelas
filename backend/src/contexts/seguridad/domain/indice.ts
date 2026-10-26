/* Indice de seguridad por zona y dia (docs/ARQUITECTURA-DDD.md, 3.6). */

export type NivelRiesgo = 'bajo' | 'medio' | 'alto' | 'sin_datos'

export interface DiaSeguridad {
  /** 0 = domingo. */
  dia: number
  indice: number | null
}

export interface SeguridadZona {
  dias: DiaSeguridad[]
  tipos: { servicio: string; subtipo: string; conteo: number }[]
  actualizadoHasta: string | null
  fuente: string
}

export interface IncidentePublico {
  id: string
  tipo: string
  lat: number
  lng: number
  ocurridoMs: number
  confirmaciones: number
  verificado: boolean
}

/** Mismos umbrales que la web y Android. Sin indice nunca se presenta como seguro. */
export function nivelDeIndice(i: number | null | undefined): NivelRiesgo {
  if (i === null || i === undefined || Number.isNaN(i)) return 'sin_datos'
  return i < 0.75 ? 'bajo' : i < 1.5 ? 'medio' : 'alto'
}
