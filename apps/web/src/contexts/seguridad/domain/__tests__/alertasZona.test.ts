import { describe, expect, it } from 'vitest'
import { alertasNuevas, distanciaM, mensajeAlerta } from '../alertasZona'
import type { IncidentePublico } from '../incidentes'

const base: IncidentePublico = {
  id: 'a',
  tipo: 'robo_asalto',
  lat: -3.9931,
  lng: -79.2042,
  ocurridoMs: Date.parse('2026-11-12T10:00:00Z'),
  confirmaciones: 0,
  estado: 'pendiente',
  fotos: [],
}
const ahora = Date.parse('2026-11-13T10:00:00Z')

describe('alertas de zona', () => {
  it('mide distancias cortas con precision de metros', () => {
    expect(
      Math.round(
        distanciaM({ lat: -3.9931, lng: -79.2042 }, { lat: -3.9931 + 0.0009, lng: -79.2042 })
      )
    ).toBe(100)
  })
  it('avisa un reporte a menos de 200 m una sola vez', () => {
    const cerca = { lat: -3.9931 + 0.0013, lng: -79.2042 }
    const r = alertasNuevas(cerca, [base], new Set(), ahora)
    expect(r).toHaveLength(1)
    expect(r[0].metros).toBeLessThanOrEqual(200)
    expect(alertasNuevas(cerca, [base], new Set(['a']), ahora)).toHaveLength(0)
  })
  it('ignora reportes lejanos o de mas de 7 dias', () => {
    expect(alertasNuevas({ lat: -3.99, lng: -79.21 }, [base], new Set(), ahora)).toHaveLength(0)
    const viejo = { ...base, ocurridoMs: ahora - 8 * 24 * 3600 * 1000 }
    expect(alertasNuevas({ lat: base.lat, lng: base.lng }, [viejo], new Set(), ahora)).toHaveLength(
      0
    )
  })
  it('arma el mensaje con el tipo de reporte', () => {
    expect(mensajeAlerta(base, 150)).toBe('Atención: reporte de robo o asalto a 150 m')
  })
})
