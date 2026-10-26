import { describe, expect, it } from 'vitest'
import { rutaDesdeFila } from '../contexts/rutas/infrastructure/rutasSupabase'

/* Caracterizacion de Rutas (F4, docs/ARQUITECTURA-DDD.md): valores por defecto del mapeo de filas. */
describe('rutas: rutaDesdeFila con fila minima', () => {
  it('rellena los valores por defecto', () => {
    const r = rutaDesdeFila({ id: 7, difficulty: 'Extrema' })
    expect(r).toMatchObject({
      id: '7',
      nombre: '',
      distanciaKm: 0,
      dificultad: 'Moderado',
      autorId: 'gacelas',
      etiquetas: [],
      puntos: [],
      valoracion: null,
      origen: 'actividad',
      oficial: false,
      fuente: undefined,
    })
  })
  it('compone la fuente con la licencia por defecto', () => {
    expect(rutaDesdeFila({ id: 1, source: 'OSM' }).fuente).toBe('OSM (ODbL 1.0)')
  })
})
