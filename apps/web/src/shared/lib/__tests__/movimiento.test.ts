import { describe, expect, it } from 'vitest'
import { MOVIMIENTO, retrasoEscalon } from '../movimiento'

describe('movimiento C2', () => {
  it('duraciones dentro de 150-350 ms', () => {
    for (const d of [MOVIMIENTO.corto, MOVIMIENTO.medio, MOVIMIENTO.largo]) {
      expect(d).toBeGreaterThanOrEqual(150)
      expect(d).toBeLessThanOrEqual(350)
    }
  })
  it('escalonado con tope y sin retraso al reducir movimiento', () => {
    expect(retrasoEscalon(1)).toBe(45)
    expect(retrasoEscalon(40)).toBe(retrasoEscalon(MOVIMIENTO.escalonesMax))
    expect(retrasoEscalon(3, true)).toBe(0)
  })
})
