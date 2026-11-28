import { describe, expect, it, vi } from 'vitest'
import type { DatosCompartir } from '../shared/lib/compartir'
import {
  ajusteCubrir,
  COLOCACION_INICIAL,
  datosPlantilla,
  dibujarPlantilla,
  normalizarHandle,
  PLANTILLAS,
  rectColocacion,
  TAMANO_PLANTILLA,
  textoInsignia,
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
    expect(normalizarHandle('   ')).toBe('@runcity')
  })
  it('solo muestra la insignia cuando hay record o racha', () => {
    expect(textoInsignia(d)).toBeNull()
    expect(textoInsignia({ ...d, recordLabel: '5K' })).toBe('Récord personal · 5K')
    expect(textoInsignia({ ...d, rachaDias: 2 })).toBe('Racha de 2 días')
  })
  it('renderiza las cuatro plantillas con marca y handle', () => {
    const fillText = vi.fn()
    const g = {
      clearRect: vi.fn(),
      fillText,
      fillRect: vi.fn(),
      measureText: vi.fn(() => ({ width: 100 })),
      beginPath: vi.fn(),
      moveTo: vi.fn(),
      lineTo: vi.fn(),
      arcTo: vi.fn(),
      closePath: vi.fn(),
      fill: vi.fn(),
      stroke: vi.fn(),
    } as unknown as CanvasRenderingContext2D
    for (const { id } of PLANTILLAS) {
      dibujarPlantilla(g, d, id, '@corredora')
      expect(fillText).toHaveBeenCalledWith('RUNCITY', expect.any(Number), expect.any(Number))
      expect(fillText).toHaveBeenCalledWith('@corredora', expect.any(Number), expect.any(Number))
      fillText.mockClear()
    }
    expect(PLANTILLAS.map(({ id }) => id)).toEqual(['estadisticas', 'traza', 'fecha', 'logro'])
  })
  it('datos compactos de una carrera', () => {
    expect(datosPlantilla(d)).toEqual([
      ['Distancia', '5,02 km'],
      ['Ritmo', `5'12" /km`],
      ['Tiempo', '26:10'],
    ])
    expect(datosPlantilla({ ...d, segundos: undefined, desnivelM: 41.6 })).toEqual([
      ['Distancia', '5,02 km'],
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
    const r = rectColocacion('estadisticas', { cx: 0.5, cy: 0.5, escala: 5 }, 1000, 2000)
    expect(r.ancho).toBe(1600)
    expect(r.alto).toBeCloseTo((1600 * 1920) / 1080)
    expect(COLOCACION_INICIAL.traza.escala).toBeGreaterThan(0)
    expect(Object.values(TAMANO_PLANTILLA)).toEqual(Array(4).fill({ ancho: 1080, alto: 1920 }))
  })

  it('no escribe 1970 ni 1969 cuando falta la fecha de la carrera', () => {
    const fillText = vi.fn()
    const g = {
      clearRect: vi.fn(),
      fillText,
      fillRect: vi.fn(),
      measureText: vi.fn(() => ({ width: 100 })),
      beginPath: vi.fn(),
      moveTo: vi.fn(),
      lineTo: vi.fn(),
      arcTo: vi.fn(),
      closePath: vi.fn(),
      fill: vi.fn(),
      stroke: vi.fn(),
    } as unknown as CanvasRenderingContext2D
    const respaldo = Date.UTC(2026, 10, 26, 17, 0, 0)
    vi.spyOn(Date, 'now').mockReturnValue(respaldo)
    try {
      dibujarPlantilla(g, { ...d, fechaMs: 0 }, 'fecha', '@corredora')
      dibujarPlantilla(g, d, 'fecha', '@corredora')
      const textos = fillText.mock.calls.map((c) => String(c[0])).join(' | ')
      expect(textos).not.toMatch(/1970|1969/)
      expect(textos).toContain('Jueves')
      expect(textos).toContain('26 de noviembre de 2026')
    } finally {
      vi.restoreAllMocks()
    }
  })
})
