import { describe, expect, it, vi } from 'vitest'
import { desnivelPositivo, muestrear, perfilElevacion } from '../contexts/rutas/domain/elevacion'

const recta = [
  { lat: -4, lng: -79.2 },
  { lat: -3.99, lng: -79.2 },
]

describe('elevacion', () => {
  it('muestrea puntos equidistantes', () => {
    const m = muestrear(recta, 11)
    expect(m).toHaveLength(11)
    expect(m[5].lat).toBeCloseTo(-3.995, 6)
  })
  it('suma solo subidas y filtra ruido', () => {
    expect(desnivelPositivo([100, 101, 100, 110, 105, 120])).toBe(25)
  })
  it('pide la elevacion a Open-Meteo', async () => {
    const pedir = vi.fn(
      async () => new Response(JSON.stringify({ elevation: Array(60).fill(2100.4) }))
    )
    const h = await perfilElevacion(recta, pedir as unknown as typeof fetch)
    expect(h).toHaveLength(60)
    expect(h[0]).toBe(2100)
    expect(String((pedir.mock.calls as unknown as string[][])[0][0])).toContain(
      'api.open-meteo.com'
    )
  })
})
