import { describe, expect, it } from 'vitest'
import { calcularRacha, mapaCalor, rachaEnRiesgo } from '../rachas'
import type { CarreraGuardada } from '../../data/tipos'

const ahora = new Date(2026, 9, 10, 12).getTime() // sabado 10 oct 2026
const hace = (dias: number) =>
  ({
    id: String(dias),
    titulo: 'c',
    inicioMs: ahora - dias * 86_400_000,
    distanciaKm: 5,
    segundos: 1500,
    puntos: [],
    publica: true,
    desnivelM: 0,
  }) as unknown as CarreraGuardada

describe('rachas', () => {
  it('igual que la funcion SQL: diaria actual, mejor y semanal', () => {
    const r = calcularRacha([0, 1, 2, 4, 5, 6, 7, 8, 9, 10, 11].map(hace), ahora, 3)
    expect(r).toMatchObject({
      diaria: 3,
      mejorDiaria: 8,
      semanal: 2,
      mejorSemanal: 2,
      carrerasSemana: 5,
    })
  })
  it('la racha sigue viva si se corrio ayer y se rompe tras un dia sin correr', () => {
    const viva = calcularRacha([1, 2].map(hace), ahora)
    expect(viva.diaria).toBe(2)
    expect(rachaEnRiesgo(viva, ahora)).toBe(true)
    expect(calcularRacha([2, 3].map(hace), ahora).diaria).toBe(0)
    expect(calcularRacha([], ahora)).toMatchObject({ diaria: 0, mejorDiaria: 0, semanal: 0 })
  })
  it('semana en curso sin meta no rompe la racha semanal', () => {
    // semana anterior (5 a 11 dias atras) cumple meta 3, esta semana solo 1 carrera
    const r = calcularRacha([0, 6, 7, 8].map(hace), ahora, 3)
    expect(r.semanal).toBe(1)
  })
  it('mapa de calor de 12 semanas que termina hoy', () => {
    const m = mapaCalor([hace(0), hace(0), hace(3)], ahora)
    expect(m).toHaveLength(12)
    const celdas = m.flat()
    expect(celdas.filter((c) => c.km > 0).map((c) => c.km)).toEqual([5, 10])
    expect(celdas.filter((c) => c.futuro)).toHaveLength(1) // domingo
  })
})

describe('rachaTrasCarrera', () => {
  it('detecta cuando la carrera de hoy alarga la racha', async () => {
    const { rachaTrasCarrera } = await import('../rachas')
    const { antes, despues } = rachaTrasCarrera([hace(1), hace(2)], { inicioMs: ahora }, 3, ahora)
    expect(antes.diaria).toBe(2)
    expect(despues.diaria).toBe(3)
    const otra = rachaTrasCarrera([hace(0)], { inicioMs: ahora }, 3, ahora)
    expect(otra.despues.diaria).toBe(otra.antes.diaria)
  })
})
