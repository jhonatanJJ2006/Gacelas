import { describe, expect, it } from 'vitest'
import { nuevoRecord } from '../estadisticas'
import { etiquetaOpcion, type Opcion } from '../logica'
import type { CarreraGuardada } from '../../data/tipos'

const c = (distanciaKm: number, segundos: number) =>
  ({ id: `${distanciaKm}-${segundos}`, distanciaKm, segundos, inicioMs: 0 }) as CarreraGuardada

describe('resumen y buscador', () => {
  it('nuevoRecord detecta la distancia mas larga mejorada', () => {
    expect(nuevoRecord([], { distanciaKm: 5.2, segundos: 1500 })?.etiqueta).toBe('5K')
    expect(nuevoRecord([c(5, 1400)], { distanciaKm: 5, segundos: 1500 })).toBeNull()
  })

  it('etiquetaOpcion marca optima, plano y trail', () => {
    const o = (id: string, distance: number, elevation: number) =>
      ({ id, name: id, distance, duration: 30, elevation, color: 'secondary' }) as Opcion
    const todas = [o('a', 5, 50), o('b', 7, 10), o('c', 9, 300)]
    expect(todas.map((x) => etiquetaOpcion(x, todas))).toEqual(['Óptima', 'Plano', 'Trail'])
  })
})
