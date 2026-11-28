import type { Prioridad, ReporteAdmin } from './caso'

/** Prioridad de un caso (content_reports.priority, migracion 050). */
export const PRIORIDADES: [Prioridad, string][] = [
  ['alta', 'Alta'],
  ['media', 'Media'],
  ['baja', 'Baja'],
]

/** Misma regla que moderacion.prioridad_por_motivo (050). */
export function prioridadPorMotivo(motivo: string): Prioridad {
  if (motivo === 'acoso' || motivo === 'peligroso' || motivo === 'suplantacion') return 'alta'
  if (motivo === 'spam') return 'baja'
  return 'media'
}

/** Horas objetivo por prioridad (app_settings.sla_horas_moderacion). */
export type SlaHoras = Record<Prioridad, number>
export const CLAVE_SLA = 'sla_horas_moderacion'
export const SLA_POR_DEFECTO: SlaHoras = { alta: 4, media: 24, baja: 72 }

export function slaDesde(valor: unknown): SlaHoras {
  const o = (valor && typeof valor === 'object' ? valor : {}) as Partial<Record<Prioridad, unknown>>
  const h = (k: Prioridad) =>
    typeof o[k] === 'number' && Number.isInteger(o[k]) ? (o[k] as number) : SLA_POR_DEFECTO[k]
  return { alta: h('alta'), media: h('media'), baja: h('baja') }
}

/** Valida los plazos: entre 1 y 720 h y la alta con el plazo mas corto. Devuelve el error o null. */
export function errorSla(s: SlaHoras): string | null {
  if (![s.alta, s.media, s.baja].every((v) => Number.isInteger(v) && v >= 1 && v <= 720))
    return 'Usa entre 1 y 720 horas'
  if (!(s.alta <= s.media && s.media <= s.baja))
    return 'La prioridad alta debe tener el plazo más corto'
  return null
}

export type EstadoSla = { tipo: 'en_plazo' | 'por_vencer' | 'vencido'; minutos: number }

/**
 * Plazo de un caso abierto: vencido si ya paso; por vencer si queda menos de una cuarta parte del
 * plazo (o menos de una hora). Igual que estadoSla del panel movil.
 */
export function estadoSla(r: ReporteAdmin, ahora = Date.now()): EstadoSla | null {
  if (!(r.status === 'pendiente' || r.status === 'en_revision') || !r.vence_en) return null
  const vence = Date.parse(r.vence_en)
  if (Number.isNaN(vence)) return null
  const restante = Math.trunc((vence - ahora) / 60_000)
  if (restante < 0) return { tipo: 'vencido', minutos: restante }
  const creado = Date.parse(r.created_at)
  const total = Number.isNaN(creado) ? 0 : Math.trunc((vence - creado) / 60_000)
  const margen = Math.max(60, Math.trunc(total / 4))
  return { tipo: restante <= margen ? 'por_vencer' : 'en_plazo', minutos: restante }
}

export function duracionCorta(minutos: number): string {
  const m = Math.abs(minutos)
  if (m < 60) return `${m} min`
  if (m < 60 * 48) return `${Math.trunc(m / 60)} h`
  return `${Math.trunc(m / (60 * 24))} d`
}

export function textoSla(e: EstadoSla): string {
  return e.tipo === 'vencido'
    ? `Vencido hace ${duracionCorta(e.minutos)}`
    : `Vence en ${duracionCorta(e.minutos)}`
}

const ORDEN: Record<Prioridad, number> = { alta: 0, media: 1, baja: 2 }

/** Abiertos primero, por vencimiento y luego prioridad; los cerrados mantienen su orden. */
export function ordenarPorSla(lista: ReporteAdmin[]): ReporteAdmin[] {
  const abierto = (r: ReporteAdmin) => r.status === 'pendiente' || r.status === 'en_revision'
  return lista
    .map((r, i) => ({ r, i }))
    .sort((a, b) => {
      const aa = abierto(a.r) ? 0 : 1
      const bb = abierto(b.r) ? 0 : 1
      if (aa !== bb) return aa - bb
      if (aa === 1) return a.i - b.i
      const va = a.r.vence_en ?? '9999'
      const vb = b.r.vence_en ?? '9999'
      if (va !== vb) return va < vb ? -1 : 1
      const pa = ORDEN[a.r.prioridad ?? 'media']
      const pb = ORDEN[b.r.prioridad ?? 'media']
      return pa !== pb ? pa - pb : a.i - b.i
    })
    .map((x) => x.r)
}
