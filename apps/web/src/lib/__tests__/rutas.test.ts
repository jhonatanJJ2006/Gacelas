import { describe, expect, it } from 'vitest'
import { rutasMock } from '../../mocks/data'
import {
  aplicarFiltros,
  FILTROS_INICIALES,
  longitudKm,
  ordenarPorCercania,
  simplificar,
  trazoParaRuta,
} from '../rutas'

const recta = Array.from({ length: 50 }, (_, i) => ({ lat: -4 + i * 0.0001, lng: -79.2 }))

describe('rutas', () => {
  it('Douglas-Peucker quita los puntos alineados y conserva las esquinas', () => {
    expect(simplificar(recta)).toHaveLength(2)
    const ele = [
      ...recta,
      ...Array.from({ length: 20 }, (_, i) => ({
        lat: recta[49].lat,
        lng: -79.2 + (i + 1) * 0.0001,
      })),
    ]
    const s = simplificar(ele)
    expect(s).toHaveLength(3)
    expect(s[1]).toEqual(recta[49])
    expect(longitudKm(s)).toBeCloseTo(longitudKm(ele), 2)
  })
  it('limita el trazo a 500 puntos', () => {
    const zigzag = Array.from({ length: 3000 }, (_, i) => ({
      lat: -4 + i * 0.00005,
      lng: -79.2 + (i % 2) * 0.001,
    }))
    expect(trazoParaRuta(zigzag).length).toBeLessThanOrEqual(500)
  })
  it('filtra por dificultad, distancia y etiqueta', () => {
    expect(
      aplicarFiltros(rutasMock, { ...FILTROS_INICIALES, dificultad: 'Fácil' }).every(
        (r) => r.dificultad === 'Fácil'
      )
    ).toBe(true)
    expect(
      aplicarFiltros(rutasMock, { ...FILTROS_INICIALES, distancia: 'Más de 10 km' }).every(
        (r) => r.distanciaKm > 10
      )
    ).toBe(true)
    expect(
      aplicarFiltros(rutasMock, { ...FILTROS_INICIALES, etiqueta: 'parque' }).map((r) => r.nombre)
    ).toEqual(['Parque Jipiro'])
  })
  it('ordena por cercania', () => {
    expect(
      ordenarPorCercania(rutasMock, { '3': 1, '1': 4 })
        .map((r) => r.id)
        .slice(0, 2)
    ).toEqual(['3', '1'])
  })
})
