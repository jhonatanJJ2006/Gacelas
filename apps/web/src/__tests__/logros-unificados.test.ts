import { beforeEach, describe, expect, it } from 'vitest'
import {
  desbloquearNuevos,
  estadoLogros,
  logrosCumplidos,
  logrosDesbloqueados,
  logrosUnificados,
  LOGROS,
  progresoLogro,
} from '../contexts/logros/infrastructure/logros'
import type { CarreraGuardada } from '../shared/kernel/tipos'

/* Catalogo unico de logros (antes 4 logros y 6 medallas en la web, 20 logros y 12 medallas en Android). */
const DIA = 86_400_000
const base = new Date(2026, 10, 2, 12).getTime() // lunes 2 de noviembre de 2026, 12:00 local
const ahora = base + 40 * DIA

let n = 0
const carrera = (o: Partial<CarreraGuardada> = {}): CarreraGuardada => ({
  id: `c${n++}`,
  titulo: 'c',
  nota: '',
  publica: false,
  inicioMs: base,
  segundos: 3000,
  distanciaKm: 5,
  desnivelM: 0,
  parciales: [],
  puntos: [],
  ...o,
})

describe('catalogo unico', () => {
  it('tiene 26 logros con codigos y titulos distintos', () => {
    expect(LOGROS).toHaveLength(26)
    expect(new Set(LOGROS.map((l) => l.codigo)).size).toBe(26)
    expect(new Set(LOGROS.map((l) => l.titulo)).size).toBe(26)
  })

  it('las 12 medallas antiguas son logros del catalogo', () => {
    const codigos = new Set(LOGROS.map((l) => l.codigo))
    for (const c of [
      'primera_carrera',
      'km_5',
      'km_10',
      'km_21',
      'km_42',
      'total_100',
      'total_500',
      'total_1000',
      'racha_7',
      'racha_30',
      'desnivel_1000',
      'desnivel_5000',
    ])
      expect(codigos.has(c as never)).toBe(true)
  })
})

describe('reglas de los logros que eran medallas', () => {
  it('distancia en una carrera: media maraton y maraton', () => {
    expect(logrosCumplidos([carrera({ distanciaKm: 21.1 })], ahora)).toContain('km_21')
    expect(logrosCumplidos([carrera({ distanciaKm: 21 })], ahora)).not.toContain('km_21')
    expect(logrosCumplidos([carrera({ distanciaKm: 42.2 })], ahora)).toContain('km_42')
    expect(progresoLogro('km_42', [carrera({ distanciaKm: 42.1 })], ahora)).toBe(41)
  })

  it('acumulados: 50, 100, 500 y 1000 km', () => {
    const muchas = Array.from({ length: 21 }, (_, i) =>
      carrera({ inicioMs: base + i * DIA, distanciaKm: 5 })
    )
    const c = logrosCumplidos(muchas, ahora)
    expect(c).toEqual(expect.arrayContaining(['total_50', 'total_100']))
    expect(c).not.toContain('total_500')
    expect(progresoLogro('total_500', muchas, ahora)).toBe(105)
  })

  it('desnivel acumulado y en una carrera', () => {
    const c = [carrera({ desnivelM: 600 }), carrera({ inicioMs: base + DIA, desnivelM: 450 })]
    const l = logrosCumplidos(c, ahora)
    expect(l).toEqual(expect.arrayContaining(['escalador', 'desnivel_1000']))
    expect(l).not.toContain('desnivel_5000')
  })

  it('racha: usa la mejor racha, no solo la de hoy', () => {
    const siete = Array.from({ length: 7 }, (_, i) => carrera({ inicioMs: base + i * DIA }))
    expect(logrosCumplidos(siete, ahora)).toEqual(expect.arrayContaining(['racha_3', 'racha_7']))
    expect(logrosCumplidos(siete, ahora)).not.toContain('racha_30')
  })
})

describe('reglas de los logros que ya existian en Android', () => {
  it('ritmo, horario y fin de semana', () => {
    const rapida = carrera({ distanciaKm: 5, segundos: 1300 }) // 4:20 por km
    expect(logrosCumplidos([rapida], ahora)).toEqual(
      expect.arrayContaining(['ritmo_6', 'ritmo_5', 'ritmo_430'])
    )
    const tempranas = [0, 1, 2].map((i) =>
      carrera({ inicioMs: new Date(2026, 10, 2 + i, 6).getTime() })
    )
    expect(logrosCumplidos(tempranas, ahora)).toContain('madrugador')
    const noches = [0, 1, 2].map((i) =>
      carrera({ inicioMs: new Date(2026, 10, 2 + i, 20).getTime() })
    )
    expect(logrosCumplidos(noches, ahora)).toContain('nocturno')
    const finde = [7, 8, 14, 15, 21].map((d) =>
      carrera({ inicioMs: new Date(2026, 10, d, 10).getTime() })
    )
    expect(logrosCumplidos(finde, ahora)).toContain('fin_de_semana')
  })

  it('explorador cuenta lugares de salida distintos', () => {
    const punto = (lat: number) => ({ lat, lng: -79.2, tiempoMs: 0, precisionM: 5, altitudM: null })
    const lugares = [-3.99, -4.05, -4.11, -4.17, -4.23].map((lat, i) =>
      carrera({ inicioMs: base + i * DIA, puntos: [punto(lat)] })
    )
    expect(logrosCumplidos(lugares, ahora)).toContain('explorador')
    expect(logrosCumplidos(lugares.slice(0, 4), ahora)).not.toContain('explorador')
  })
})

describe('estado y registro en el dispositivo', () => {
  beforeEach(() => localStorage.clear())

  it('ganado = registrado o cumplido por las carreras; el resto muestra su avance', () => {
    const e = estadoLogros([carrera({ distanciaKm: 8 })], { racha_30: 123 }, ahora)
    const por = Object.fromEntries(e.map((x) => [x.logro.codigo, x]))
    expect(por.km_5.lograda).toBe(true)
    expect(por.km_10).toMatchObject({ lograda: false, progreso: 8 })
    // Registrado antes (sus carreras de otro dispositivo ya no estan aqui): sigue ganado.
    expect(por.racha_30).toMatchObject({ lograda: true, fecha: 123 })
    expect(e).toHaveLength(26)
  })

  it('el primer arranque con el catalogo unico lo marca y guarda lo ya cumplido', () => {
    expect(logrosUnificados()).toBe(false)
    const nuevos = desbloquearNuevos([carrera({ distanciaKm: 11, segundos: 6600 })], ahora)
    expect(nuevos.map((l) => l.codigo)).toEqual(['km_5', 'km_10', 'primera_carrera'])
    expect(logrosUnificados()).toBe(true)
    expect(Object.keys(logrosDesbloqueados()).sort()).toEqual(['km_10', 'km_5', 'primera_carrera'])
    expect(desbloquearNuevos([carrera({ distanciaKm: 11, segundos: 6600 })], ahora)).toEqual([])
  })
})
