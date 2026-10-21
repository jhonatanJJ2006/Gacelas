import { describe, expect, it } from 'vitest'
import type { CarreraGuardada } from '../../data/tipos'
import {
  historico,
  kmMensual,
  medallas,
  mejorRacha,
  records,
  retoMes,
  totalesSemana,
} from '../estadisticas'
import { haceTiempo } from '../tiempo'

const c = (dia: number, km: number, seg: number, mes = 9): CarreraGuardada =>
  ({
    id: `${mes}-${dia}`,
    titulo: 'x',
    nota: '',
    publica: true,
    inicioMs: new Date(2026, mes, dia, 7).getTime(),
    segundos: seg,
    distanciaKm: km,
    desnivelM: 10,
    parciales: [],
    puntos: [],
  }) as unknown as CarreraGuardada

const ahora = new Date(2026, 9, 21, 12).getTime()
const lista = [c(19, 5, 1500), c(20, 10.5, 3150), c(21, 2, 500), c(5, 21.1, 7600, 7)]

describe('estadisticas de Perfil e Inicio', () => {
  it('totales de la semana en curso', () => {
    const t = totalesSemana(lista, ahora)
    expect(t.carreras).toBe(3)
    expect(t.km).toBeCloseTo(17.5)
    expect(t.desnivelM).toBe(30)
  })
  it('historico con mejor ritmo', () => {
    const h = historico(lista)
    expect(h.carreras).toBe(4)
    expect(h.mejorRitmoSegKm).toBe(250)
  })
  it('km por mes de los ultimos 6 meses', () => {
    const m = kmMensual(lista, ahora)
    expect(m).toHaveLength(6)
    expect(m[5]).toMatchObject({ etiqueta: 'Oct', mes: 10 })
    expect(m[5].km).toBeCloseTo(17.5)
    expect(m[3].km).toBeCloseTo(21.1)
  })
  it('records por distancia', () => {
    const r = records(lista)
    expect(r.map((x) => x.etiqueta)).toEqual(['1K', '5K', '10K', '21K'])
    expect(r[0].segundos).toBe(250)
    expect(r[1].segundos).toBe(1500)
    expect(r[2].segundos).toBe(3000)
    expect(r[3].segundos).not.toBeNull()
  })
  it('medallas y racha', () => {
    expect(mejorRacha(lista)).toBe(3)
    const m = Object.fromEntries(medallas(lista).map((x) => [x.id, x.lograda]))
    expect(m).toMatchObject({ primera: true, '5k': true, '10k': true, '21k': true, racha7: false })
  })
  it('reto del mes', () => {
    const r = retoMes(lista, ahora)
    expect(r.titulo).toBe('Reto Octubre Andino')
    expect(r.diasRestantes).toBe(10)
    expect(r.km).toBeCloseTo(17.5)
  })
  it('hace tiempo', () => {
    expect(haceTiempo(ahora - 35 * 60000, ahora)).toBe('Hace 35 min')
    expect(haceTiempo(ahora - 26 * 3600000, ahora)).toBe('Ayer')
  })
})
