import { describe, expect, it } from 'vitest'
import { errorSla, estadoSla, ordenarPorSla, prioridadPorMotivo, slaDesde, textoSla } from './sla'
import type { ReporteAdmin } from './caso'

const caso = (id: string, extra: Partial<ReporteAdmin>) =>
  ({
    id,
    status: 'pendiente',
    created_at: '2026-10-31T00:00:00Z',
    ...extra,
  }) as ReporteAdmin

describe('plazos de moderacion', () => {
  it('la prioridad sale del motivo', () => {
    expect(prioridadPorMotivo('acoso')).toBe('alta')
    expect(prioridadPorMotivo('spam')).toBe('baja')
    expect(prioridadPorMotivo('otro')).toBe('media')
  })

  it('lee los plazos guardados y valida el orden', () => {
    expect(slaDesde({ alta: 2, media: 'x' })).toEqual({ alta: 2, media: 24, baja: 72 })
    expect(errorSla({ alta: 4, media: 24, baja: 72 })).toBeNull()
    expect(errorSla({ alta: 30, media: 24, baja: 72 })).toMatch('plazo más corto')
    expect(errorSla({ alta: 0, media: 24, baja: 72 })).toMatch('entre 1 y 720')
  })

  it('distingue vencido, por vencer y en plazo; ignora casos cerrados', () => {
    const ahora = Date.parse('2026-10-31T12:00:00Z')
    expect(estadoSla(caso('a', { vence_en: '2026-10-31T10:00:00Z' }), ahora)?.tipo).toBe('vencido')
    expect(estadoSla(caso('b', { vence_en: '2026-10-31T12:30:00Z' }), ahora)?.tipo).toBe(
      'por_vencer'
    )
    expect(estadoSla(caso('c', { vence_en: '2026-11-02T00:00:00Z' }), ahora)?.tipo).toBe('en_plazo')
    expect(
      estadoSla(caso('d', { status: 'resuelto', vence_en: '2026-10-31T10:00:00Z' }), ahora)
    ).toBeNull()
    expect(textoSla({ tipo: 'vencido', minutos: -120 })).toMatch(/2 h/)
  })

  it('ordena los abiertos por vencimiento', () => {
    const l = ordenarPorSla([
      caso('tarde', { vence_en: '2026-11-02T00:00:00Z' }),
      caso('pronto', { vence_en: '2026-10-31T01:00:00Z' }),
    ])
    expect(l.map((r) => r.id)).toEqual(['pronto', 'tarde'])
  })
})
