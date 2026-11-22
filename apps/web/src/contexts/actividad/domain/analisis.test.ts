import { describe, expect, it } from 'vitest'
import { carreraATcx, ladoComparacion, nombreTcx, parcialesPorKm, serieElevacion } from './analisis'
import type { Parcial } from '../../../shared/kernel/tipos'
import type { PuntoGps } from '../../../shared/lib/geo'

const p = (numero: number, manual = false): Parcial => ({
  numero,
  distanciaKm: manual ? 0.4 : 1,
  segundos: 300 + numero,
  manual,
})

const punto = (i: number, alt: number | null): PuntoGps => ({
  lat: -4,
  lng: -79.2 + i * 0.001,
  tiempoMs: 1_700_000_000_000 + i * 1000,
  precisionM: 5,
  altitudM: alt,
})

describe('analisis de una carrera', () => {
  it('el grafico de ritmo usa solo los kilometros automaticos', () => {
    expect(parcialesPorKm([p(1), p(2, true), p(3)]).map((x) => x.numero)).toEqual([1, 3])
  })

  it('la elevacion queda contra la distancia, no contra el orden del GPS', () => {
    const serie = serieElevacion(
      [punto(0, 2100), punto(1, null), punto(2, 2140), punto(3, 2120)],
      3
    )
    expect(serie).toHaveLength(3)
    expect(serie[0]).toMatchObject({ distanciaKm: 0, altitudM: 2100 })
    expect(serie[2].altitudM).toBeCloseTo(2120)
    expect(serie[2].distanciaKm).toBeGreaterThan(serie[1].distanciaKm)
  })

  it('el TCX lleva el deporte, el tiempo y escapa el titulo', () => {
    const xml = carreraATcx({
      titulo: 'Río & <parque>',
      inicioMs: Date.UTC(2026, 9, 21, 11, 0, 0),
      segundos: 400,
      distanciaKm: 1.2,
      puntos: [punto(0, 2100), punto(1, 2110)],
    })
    expect(xml).toContain('Sport="Running"')
    expect(xml).toContain('<TotalTimeSeconds>400</TotalTimeSeconds>')
    expect(xml).toContain('<DistanceMeters>1200.0</DistanceMeters>')
    expect(xml).toContain('Río &amp; &lt;parque&gt;')
    expect(xml).toContain('<AltitudeMeters>2100.0</AltitudeMeters>')
    expect(nombreTcx('Río & parque', Date.UTC(2026, 9, 21))).toMatch(/\.tcx$/)
  })

  it('compara distancia, tiempo, ritmo y desnivel', () => {
    const a = ladoComparacion({
      id: 'a',
      titulo: 'A',
      distanciaKm: 10,
      segundos: 3000,
      desnivelM: 40,
    })
    const b = ladoComparacion({
      id: 'b',
      titulo: 'B',
      distanciaKm: 5,
      segundos: 1800,
      desnivelM: 80,
    })
    expect(a.ritmoSegKm).toBe(300)
    expect(b.ritmoSegKm).toBe(360)
    expect(a.desnivelM - b.desnivelM).toBe(-40)
  })
})
