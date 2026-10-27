import { describe, expect, it } from 'vitest'
import {
  arrastrar,
  avanzar,
  empezarArrastre,
  GIRO_AUTO,
  INERCIA_MAX,
  nuevoGiro,
  soltar,
} from '../giroArrastre'

describe('giroArrastre', () => {
  it('gira solo sin arrastre', () => {
    const g = avanzar(nuevoGiro(), 1)
    expect(g.angulo).toBeCloseTo(GIRO_AUTO)
  })

  it('el arrastre pausa el giro automatico y suma el desplazamiento', () => {
    let g = empezarArrastre(nuevoGiro())
    g = arrastrar(g, 100)
    g = avanzar(g, 1)
    expect(g.angulo).toBeCloseTo(40)
    g = arrastrar(g, -200)
    expect(g.angulo).toBeCloseTo(320)
  })

  it('la inercia se frena y vuelve el giro automatico', () => {
    let g = soltar(empezarArrastre(nuevoGiro()), 500)
    expect(g.inercia).toBeCloseTo(200)
    for (let i = 0; i < 300; i++) g = avanzar(g, 1 / 60)
    expect(g.inercia).toBe(0)
    const antes = g.angulo
    g = avanzar(g, 1)
    expect((g.angulo - antes + 360) % 360).toBeCloseTo(GIRO_AUTO)
  })

  it('limita la inercia', () => {
    expect(soltar(nuevoGiro(), 1e6).inercia).toBe(INERCIA_MAX)
    expect(soltar(nuevoGiro(), -1e6).inercia).toBe(-INERCIA_MAX)
  })
})
