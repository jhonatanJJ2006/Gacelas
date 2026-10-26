import { beforeEach, describe, expect, it } from 'vitest'
import type { CarreraGuardada } from '../data/tipos'
import { desbloquearNuevos, logrosCumplidos, logrosDesbloqueados } from '../features/social/logros'

/* Caracterizacion de Logros (F5, docs/ARQUITECTURA-DDD.md): reglas y registro local. */
const carrera = (km: number, fecha: number) => ({ distanciaKm: km, fecha }) as unknown as CarreraGuardada
const ahora = Date.parse('2026-10-25T12:00:00Z')

describe('logros', () => {
  beforeEach(() => localStorage.clear())
  it('sin carreras no cumple ninguno', () => expect(logrosCumplidos([], ahora)).toEqual([]))
  it('una carrera de 10 km cumple primera, 5 y 10', () =>
    expect(logrosCumplidos([carrera(10, ahora)], ahora)).toEqual(['primera_carrera', 'km_5', 'km_10']))
  it('desbloquea solo una vez y lo guarda en el dispositivo', () => {
    const n = desbloquearNuevos([carrera(5, ahora)], ahora).map((l) => l.codigo)
    expect(n).toEqual(['primera_carrera', 'km_5'])
    expect(Object.keys(logrosDesbloqueados())).toEqual(['primera_carrera', 'km_5'])
    expect(desbloquearNuevos([carrera(5, ahora)], ahora)).toEqual([])
  })
})
