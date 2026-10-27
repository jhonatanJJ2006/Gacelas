import { describe, expect, it } from 'vitest'
import { accionClub, puedeGestionar, validarClub } from '../contexts/eventos/infrastructure/clubes'
import {
  calendarioMes,
  clasificacion,
  posicionesRepeticion,
  textoCupo,
  type CorredorEvento,
} from '../contexts/eventos/infrastructure/eventos'
import type { Evento } from '../mocks/data'

const corredor = (id: string, km: number, s: number, puntos = 0): CorredorEvento => ({
  sesionId: 's' + id,
  usuarioId: id,
  nombre: id,
  distanciaM: km * 1000,
  segundos: s,
  lat: null,
  lng: null,
  oculto: false,
  terminado: false,
  puntos: Array.from({ length: puntos }, (_, i) => ({ lat: i, lng: i })),
})

describe('clubes', () => {
  it('decide el boton segun la membresia', () => {
    expect(accionClub(null, false)).toBe('Unirme')
    expect(accionClub(null, true)).toBe('Solicitar unirme')
    expect(
      accionClub({ usuarioId: 'a', nombre: 'A', rol: 'member', estado: 'pending' }, true)
    ).toBe('Solicitud enviada')
    expect(
      accionClub({ usuarioId: 'a', nombre: 'A', rol: 'member', estado: 'active' }, false)
    ).toBe('Salir del grupo')
    expect(
      accionClub({ usuarioId: 'a', nombre: 'A', rol: 'owner', estado: 'active' }, false)
    ).toBeNull()
  })
  it('permisos y validacion', () => {
    expect(puedeGestionar('admin')).toBe(true)
    expect(puedeGestionar('member')).toBe(false)
    expect(validarClub({ nombre: 'ab', descripcion: '' })).toMatch(/3 letras/)
    expect(validarClub({ nombre: 'Runcity', descripcion: '' })).toBeNull()
  })
})

describe('eventos', () => {
  it('texto del cupo y lista de espera', () => {
    expect(
      textoCupo({ inscritos: 3, enEspera: 0, cupo: null, miEstado: null, miPosicion: null })
    ).toBe('3 inscritos')
    expect(
      textoCupo({ inscritos: 9, enEspera: 0, cupo: 10, miEstado: null, miPosicion: null })
    ).toBe('9/10 · 1 cupo libre')
    expect(
      textoCupo({ inscritos: 10, enEspera: 2, cupo: 10, miEstado: 'waitlist', miPosicion: 2 })
    ).toBe('Lleno · 2 en espera')
  })
  it('arma el calendario de lunes a domingo con los eventos del dia', () => {
    const e = { id: 'x', inicioMs: new Date(2026, 9, 15, 7).getTime() } as Evento
    const semanas = calendarioMes(2026, 9, [e])
    expect(semanas[0][0].fecha.getDay()).toBe(1) // lunes
    expect(semanas.flat().filter((d) => d.delMes)).toHaveLength(31)
    expect(semanas.flat().find((d) => d.fecha.getDate() === 15 && d.delMes)!.eventos).toEqual([e])
  })
  it('clasificacion por distancia y desempate por tiempo', () => {
    const t = clasificacion([corredor('a', 3, 900), corredor('b', 5, 1500), corredor('c', 5, 1400)])
    expect(t.map((x) => x.usuarioId)).toEqual(['c', 'b', 'a'])
  })
  it('repeticion avanza por el trazo de cada corredor', () => {
    const p = posicionesRepeticion([corredor('a', 1, 1, 11), corredor('b', 1, 1, 0)], 0.5)
    expect(p).toEqual([{ usuarioId: 'a', nombre: 'a', lat: 5, lng: 5 }])
  })
})
