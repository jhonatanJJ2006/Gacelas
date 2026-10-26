import { describe, expect, it } from 'vitest'
import { BusEventos, exito, fallo } from '..'

describe('kernel', () => {
  it('crea resultados de exito y fallo', () => {
    expect(exito(3)).toEqual({ ok: true, valor: 3 })
    expect(fallo('x', 'y')).toEqual({ ok: false, error: { codigo: 'x', mensaje: 'y' } })
  })

  it('entrega eventos solo a los suscriptores del nombre y permite cancelar', () => {
    const bus = new BusEventos()
    const recibidos: string[] = []
    const cancelar = bus.suscribir('actividad.CarreraTerminada', (e) => recibidos.push(e.nombre))
    bus.publicar({ nombre: 'actividad.CarreraTerminada', ocurridoEn: '2026-10-25', datos: {} })
    bus.publicar({ nombre: 'otro', ocurridoEn: '2026-10-25', datos: {} })
    cancelar()
    bus.publicar({ nombre: 'actividad.CarreraTerminada', ocurridoEn: '2026-10-25', datos: {} })
    expect(recibidos).toEqual(['actividad.CarreraTerminada'])
  })
})
