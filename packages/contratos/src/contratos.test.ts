import { describe, expect, it } from 'vitest'
import { EventoContrato, NOMBRES_EVENTOS } from './index'

const uuid = '00000000-0000-4000-8000-000000000001'

describe('contratos', () => {
  it('valida una CarreraTerminada', () => {
    const r = EventoContrato.safeParse({
      nombre: 'actividad.CarreraTerminada',
      ocurridoEn: '2026-10-25T19:00:00Z',
      datos: { carreraId: uuid, corredorId: uuid, distanciaM: 5000, duracionS: 1500, rutaId: null },
    })
    expect(r.success).toBe(true)
  })

  it('rechaza eventos desconocidos o incompletos', () => {
    expect(
      EventoContrato.safeParse({ nombre: 'x', ocurridoEn: '2026-10-25T19:00:00Z', datos: {} })
        .success
    ).toBe(false)
    expect(
      EventoContrato.safeParse({ nombre: 'eventos.EventoPublicado', ocurridoEn: 'ayer', datos: {} })
        .success
    ).toBe(false)
  })

  it('publica la lista de nombres', () => {
    expect(NOMBRES_EVENTOS).toHaveLength(4)
  })
})
