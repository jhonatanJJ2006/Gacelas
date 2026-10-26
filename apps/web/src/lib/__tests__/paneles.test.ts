import { describe, expect, it } from 'vitest'
import { inicioSemana, kmPorSemana, rankingSemanal, recordsPersonales } from '../paneles'
import type { CarreraGuardada } from '../../shared/kernel/tipos'

const ahora = new Date(2026, 9, 10, 12).getTime() // sabado
const carrera = (dias: number, km: number, seg: number) =>
  ({
    id: `${dias}-${km}`,
    titulo: 'c',
    inicioMs: ahora - dias * 86_400_000,
    distanciaKm: km,
    segundos: seg,
    puntos: [],
    publica: true,
    desnivelM: 0,
  }) as unknown as CarreraGuardada

describe('paneles de escritorio', () => {
  it('ranking semanal ordena por km e incluye al usuario', () => {
    const r = rankingSemanal(
      [carrera(1, 5, 1500), carrera(10, 20, 6000)],
      'Yo',
      [
        {
          id: 'a',
          autorId: 'u2',
          autor: 'Ana',
          titulo: 't',
          inicioMs: ahora - 3_600_000,
          distanciaKm: 8,
          segundos: 2400,
          likes: 0,
          comentarios: 0,
          puntos: [],
        },
      ],
      ahora
    )
    expect(r.map((f) => [f.nombre, f.km])).toEqual([
      ['Ana', 8],
      ['Yo', 5],
    ])
  })
  it('km por semana y records', () => {
    expect(new Date(inicioSemana(ahora)).getDay()).toBe(1)
    const s = kmPorSemana([carrera(1, 5, 1500), carrera(8, 3, 900)], ahora)
    expect(s.at(-1)!.km).toBe(5)
    expect(s.at(-2)!.km).toBe(3)
    const rec = recordsPersonales([carrera(1, 5, 1500), carrera(8, 3, 840)])
    expect(rec.masLarga!.distanciaKm).toBe(5)
    expect(rec.mejorRitmoSegKm).toBe(280)
    expect(rec.totalSegundos).toBe(2340)
  })
})
