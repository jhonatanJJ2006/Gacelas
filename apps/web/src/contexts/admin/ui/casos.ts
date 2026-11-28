import type { Tono } from './kit'
import type { EstadoReporte, ReporteAdmin } from './fuente'

/* Reglas de presentacion de la cola de moderacion (sin dependencias de React). */

export const ESTADOS: [EstadoReporte, string][] = [
  ['pendiente', 'Pendiente'],
  ['en_revision', 'En revisión'],
  ['resuelto', 'Resuelto'],
  ['descartado', 'Descartado'],
]

export const MOTIVOS: Record<string, string> = {
  spam: 'Spam o publicidad',
  ofensivo: 'Contenido ofensivo',
  acoso: 'Acoso',
  falso: 'Carrera falsa',
  suplantacion: 'Suplantación',
  peligroso: 'Peligroso',
  otro: 'Otro',
}

export const tonoEstado = (e: EstadoReporte): Tono =>
  ({ pendiente: 'warn', en_revision: 'info', resuelto: 'ok', descartado: 'neu' })[e] as Tono

export const nombreEstado = (e: string) => ESTADOS.find(([s]) => s === e)?.[1] ?? e

/** Identificador corto y legible de un caso (los primeros caracteres del id). */
export const idCorto = (r: ReporteAdmin) => `R-${r.id.replace(/-/g, '').slice(0, 4).toUpperCase()}`

/** El caso pendiente mas antiguo: es el siguiente a atender. */
export function siguienteCaso(cola: ReporteAdmin[]): ReporteAdmin | null {
  return (
    cola
      .filter((r) => r.status === 'pendiente')
      .sort((a, b) => new Date(a.created_at).getTime() - new Date(b.created_at).getTime())[0] ??
    null
  )
}

export const estaAbierto = (r: ReporteAdmin) =>
  r.status === 'pendiente' || r.status === 'en_revision'
