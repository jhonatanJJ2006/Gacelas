import { beforeEach, describe, expect, it } from 'vitest'
import {
  estaActiva,
  ritmoPromedio,
  RunTracker,
  tiempoTexto,
  tituloPorHora,
} from '../contexts/actividad/domain/runTracker'
import type { PuntoGps } from '../shared/lib/geo'

/** 0.0001 grados de latitud son unos 11.1 m; un punto cada 4 s. */
const punto = (i: number, precision = 5, alt: number | null = null): PuntoGps => ({
  lat: -4 + i * 0.0001,
  lng: -79.2,
  tiempoMs: i * 4000,
  precisionM: precision,
  altitudM: alt,
})

describe('RunTracker', () => {
  let t = 0
  let tracker: RunTracker
  beforeEach(() => {
    t = 0
    tracker = new RunTracker(
      () => t,
      () => 1_000
    )
  })

  it('acumula distancia, tiempo y ritmo', () => {
    tracker.iniciar()
    for (let i = 0; i <= 10; i++) {
      t = i * 4000
      tracker.registrarPunto(punto(i))
    }
    const e = tracker.estado
    expect(e.fase).toBe('corriendo')
    expect(e.distanciaKm).toBeCloseTo(0.111, 2)
    expect(e.segundos).toBe(40)
    expect(e.ritmoActualSegKm).not.toBeNull()
    expect(ritmoPromedio(e)).toBeGreaterThan(300)
    expect(tiempoTexto(e)).toBe('00:00:40')
    expect(estaActiva(e)).toBe(true)
  })

  it('descarta puntos imprecisos, ruido y saltos imposibles', () => {
    tracker.iniciar()
    tracker.registrarPunto(punto(0))
    t = 4000
    tracker.registrarPunto(punto(1, 80))
    expect(tracker.estado.ultimaPrecisionM).toBe(80)
    t = 5000
    tracker.registrarPunto({
      lat: -3.99,
      lng: -79.2,
      tiempoMs: 5000,
      precisionM: 5,
      altitudM: null,
    })
    t = 6000
    tracker.registrarPunto({
      lat: -4 + 0.00001,
      lng: -79.2,
      tiempoMs: 6000,
      precisionM: 5,
      altitudM: null,
    })
    expect(tracker.estado.distanciaKm).toBe(0)
    expect(tracker.estado.puntos).toHaveLength(1)
  })

  it('la pausa no suma tiempo ni distancia', () => {
    tracker.iniciar()
    tracker.registrarPunto(punto(0))
    t = 10_000
    tracker.pausar()
    t = 60_000
    tracker.registrarPunto(punto(5))
    tracker.actualizarTiempo()
    expect(tracker.estado.segundos).toBe(10)
    expect(tracker.estado.distanciaKm).toBe(0)
    tracker.alternarPausa()
    t = 65_000
    tracker.actualizarTiempo()
    expect(tracker.estado.segundos).toBe(15)
    tracker.alternarPausa()
    expect(tracker.estado.fase).toBe('pausada')
  })

  it('crea un parcial automatico por kilometro', () => {
    tracker.iniciar()
    for (let i = 0; i <= 100; i++) {
      t = i * 4000
      tracker.registrarPunto(punto(i))
    }
    expect(tracker.estado.parciales).toHaveLength(1)
    expect(tracker.estado.parciales[0].manual).toBe(false)
  })

  it('vuelta manual y desnivel positivo', () => {
    tracker.iniciar()
    tracker.registrarPunto(punto(0, 5, 2000))
    t = 4000
    tracker.registrarPunto(punto(1, 5, 2003))
    t = 8000
    tracker.registrarPunto(punto(2, 5, 2001))
    tracker.vuelta()
    tracker.vuelta()
    expect(tracker.estado.desnivelM).toBeCloseTo(3)
    const manuales = tracker.estado.parciales.filter((p) => p.manual)
    expect(manuales).toHaveLength(2)
    expect(manuales[1].distanciaKm).toBeCloseTo(0)
  })

  it('finaliza, convierte a carrera guardada y reinicia', () => {
    const vistos: string[] = []
    const quitar = tracker.suscribir((e) => vistos.push(e.fase))
    tracker.iniciar()
    tracker.registrarPunto(punto(0))
    t = 4000
    tracker.registrarPunto(punto(1))
    expect(tracker.finalizar().fase).toBe('terminada')
    const c = tracker.aCarreraGuardada('  ', 'nota', false, 'x')
    expect(c).toMatchObject({ id: 'x', nota: 'nota', publica: false, inicioMs: 1000 })
    expect(c.titulo).toBe(tituloPorHora(1000))
    expect(c.puntos).toHaveLength(2)
    tracker.reiniciar()
    expect(tracker.estado.fase).toBe('inactiva')
    quitar()
    expect(vistos).toContain('terminada')
  })

  it('ignora puntos y vueltas sin carrera activa', () => {
    tracker.registrarPunto(punto(0))
    tracker.vuelta()
    tracker.pausar()
    tracker.reanudar()
    expect(tracker.estado.puntos).toHaveLength(0)
    expect(tracker.estado.fase).toBe('inactiva')
  })

  it('titulo segun la hora', () => {
    const a = (h: number) => new Date(2026, 9, 9, h).getTime()
    expect(tituloPorHora(a(7))).toBe('Carrera matutina')
    expect(tituloPorHora(a(15))).toBe('Carrera de la tarde')
    expect(tituloPorHora(a(21))).toBe('Carrera nocturna')
  })
})
