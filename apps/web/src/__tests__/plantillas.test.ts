import { describe, expect, it } from 'vitest'
import type { DatosCompartir } from '../shared/lib/compartir'
import {
  ajusteCubrir,
  COLOCACION_INICIAL,
  datosPlantilla,
  normalizarHandle,
  rectColocacion,
  TAMANO_PLANTILLA,
} from '../shared/lib/plantillas'

const d: DatosCompartir = {
  titulo: 'x',
  puntos: [],
  distanciaKm: 5.024,
  segundos: 1570,
  ritmoSegKm: 312,
  tipo: 'carrera',
  enlace: '',
}

describe('plantillas', () => {
  it('normaliza el @usuario', () => {
    expect(normalizarHandle('  gacelas running ')).toBe('@gacelasrunning')
    expect(normalizarHandle('@@lola')).toBe('@lola')
    expect(normalizarHandle('   ')).toBe('')
  })
  it('datos compactos de una carrera', () => {
    expect(datosPlantilla(d)).toEqual([
      ['Distancia', '5.02 km'],
      ['Ritmo', `5'12" /km`],
      ['Tiempo', '26:10'],
    ])
    expect(datosPlantilla({ ...d, segundos: undefined, desnivelM: 41.6 })).toEqual([
      ['Distancia', '5.02 km'],
      ['Desnivel', '42 m'],
    ])
  })
  it('cover recorta sin deformar', () => {
    expect(ajusteCubrir(4000, 3000, 1080, 1920)).toEqual({
      sx: 1156.25,
      sy: 0,
      sw: 1687.5,
      sh: 3000,
    })
  })
  it('coloca la superposicion manteniendo la proporcion y limita la escala', () => {
    const r = rectColocacion('minimal', { cx: 0.5, cy: 0.5, escala: 5 }, 1000, 2000)
    expect(r.ancho).toBe(1600)
    expect(r.alto).toBeCloseTo(
      (1600 * TAMANO_PLANTILLA.minimal.alto) / TAMANO_PLANTILLA.minimal.ancho
    )
    expect(COLOCACION_INICIAL.trazo.escala).toBeGreaterThan(0)
  })
})
