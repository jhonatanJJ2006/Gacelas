import { describe, expect, it } from 'vitest'
import { formatearHms, formatearMs, formatearRitmo, haversineM, Unidades } from '../geo'

describe('geo', () => {
  it('haversine: un grado de latitud son unos 111 km', () => {
    expect(haversineM(0, 0, 1, 0)).toBeGreaterThan(111_100)
    expect(haversineM(0, 0, 1, 0)).toBeLessThan(111_300)
    expect(haversineM(-4, -79.2, -4, -79.2)).toBe(0)
  })
  it('formatos de tiempo y ritmo', () => {
    expect(formatearRitmo(330)).toBe('5:30')
    expect(formatearRitmo(null)).toBe('--:--')
    expect(formatearRitmo(0)).toBe('--:--')
    expect(formatearRitmo(4000)).toBe('--:--')
    expect(formatearHms(3665)).toBe('01:01:05')
    expect(formatearMs(125)).toBe('2:05')
  })
  it('unidades metricas e imperiales', () => {
    expect(Unidades.texto(1.609344, false)).toBe('1.00')
    expect(Unidades.texto(5, true, 1)).toBe('5.0')
    expect(Unidades.etiquetaDistancia(false)).toBe('mi')
    expect(Unidades.ritmo(300, true)).toBe(300)
    expect(Unidades.ritmo(300, false)).toBe(482)
    expect(Unidades.ritmo(null, false)).toBeNull()
    expect(Unidades.etiquetaRitmo(false)).toBe('/mi')
    expect(Unidades.etiquetaRitmo(true)).toBe('/km')
  })
})
