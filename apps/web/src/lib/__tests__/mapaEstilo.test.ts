import { describe, expect, it } from 'vitest'
import {
  animacionPorRitmo,
  colorRitmo,
  deltaAngulo,
  interpolar,
  NARANJA,
  rumbo,
  rumboReciente,
  tramosPorRitmo,
  VERDE,
  desplazar,
  distanciaM,
} from '../mapaEstilo'

describe('mapa Gacelas', () => {
  it('rumbo cardinal', () => {
    expect(rumbo({ lat: 0, lng: 0 }, { lat: 1, lng: 0 })).toBeCloseTo(0)
    expect(rumbo({ lat: 0, lng: 0 }, { lat: 0, lng: 1 })).toBeCloseTo(90)
    expect(rumbo({ lat: 0, lng: 0 }, { lat: -1, lng: 0 })).toBeCloseTo(180)
  })
  it('giro mas corto', () => {
    expect(deltaAngulo(350, 10)).toBe(20)
    expect(deltaAngulo(10, 350)).toBe(-20)
  })
  it('interpola', () =>
    expect(interpolar({ lat: 0, lng: 0 }, { lat: 2, lng: 4 }, 0.5)).toEqual({ lat: 1, lng: 2 }))
  it('color por ritmo', () => {
    expect(colorRitmo(500).toLowerCase()).toBe(NARANJA.toLowerCase())
    expect(colorRitmo(250).toLowerCase()).toBe(VERDE.toLowerCase())
    expect(colorRitmo(null)).toBe(NARANJA)
  })
  it('tramos con color', () => {
    const pts = Array.from({ length: 11 }, (_, i) => ({
      lat: -4 + i * 0.0005,
      lng: -79.2,
      tiempoMs: i * 10000,
    }))
    const t = tramosPorRitmo(pts, 100)
    expect(t.length).toBeGreaterThan(3)
    expect(t[0].camino[t[0].camino.length - 1]).toEqual(t[1].camino[0])
  })
  it('animacion por ritmo', () => {
    expect(animacionPorRitmo(null).clip).toBe('idle')
    expect(animacionPorRitmo(420).clip).toBe('jog')
    expect(animacionPorRitmo(280).clip).toBe('run')
  })
  it('desplaza metros en un rumbo', () => {
    const a = { lat: -3.99, lng: -79.2 }
    const b = desplazar(a, 90, 100)
    expect(distanciaM(a, b)).toBeCloseTo(100, 0)
    expect(rumbo(a, b)).toBeCloseTo(90, 0)
  })
  it('rumbo reciente', () => {
    expect(rumboReciente([{ lat: 0, lng: 0 }])).toBeNull()
    expect(
      rumboReciente([
        { lat: 0, lng: 0 },
        { lat: 0.001, lng: 0 },
      ])
    ).toBeCloseTo(0)
  })
})
