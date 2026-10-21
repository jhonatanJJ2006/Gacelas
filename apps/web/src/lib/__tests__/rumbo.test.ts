import { describe, expect, it } from 'vitest'
import { combinar, diferencia, pesoGps } from '../rumbo'

describe('rumbo', () => {
  it('diferencia por el camino corto', () => {
    expect(diferencia(350, 10)).toBe(20)
    expect(diferencia(10, 350)).toBe(-20)
  })
  it('peso del GPS segun velocidad', () => {
    expect(pesoGps(null)).toBe(0)
    expect(pesoGps(0.3)).toBe(0)
    expect(pesoGps(2)).toBe(1)
    expect(pesoGps(0.8)).toBeCloseTo(0.5)
  })
  it('combina sensor y GPS como Android', () => {
    expect(combinar(null, 90, 2)).toBe(90)
    expect(combinar(45, null, 2)).toBe(45)
    expect(combinar(0, 90, 0)).toBe(0)
    expect(combinar(0, 90, 3)).toBe(90)
  })
})
