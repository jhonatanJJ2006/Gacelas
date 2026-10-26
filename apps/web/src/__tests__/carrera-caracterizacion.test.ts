import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  descartarCarrera,
  empezarSeguimiento,
  finalizarCarrera,
  mensajeErrorGps,
  tracker,
} from '../contexts/actividad/application/carreraStore'

/**
 * Caracterizacion del flujo de carrera web (F2, docs/ARQUITECTURA-DDD.md): GPS con
 * watchPosition, reloj de 1 s, errores y fin de carrera. Fija el comportamiento actual.
 */
type Exito = (p: GeolocationPosition) => void
type Fallo = (e: GeolocationPositionError) => void

let exito: Exito | null = null
let fallo: Fallo | null = null
const watch = vi.fn((ok: Exito, err: Fallo, _op?: PositionOptions) => {
  exito = ok
  fallo = err
  return 7
})
const clear = vi.fn()

beforeEach(() => {
  vi.useFakeTimers()
  Object.defineProperty(navigator, 'geolocation', {
    configurable: true,
    value: { watchPosition: watch, clearWatch: clear },
  })
  watch.mockClear()
  clear.mockClear()
})

afterEach(() => {
  descartarCarrera()
  vi.useRealTimers()
})

const pos = (lat: number, t: number, accuracy = 5) =>
  ({
    coords: { latitude: lat, longitude: -79.2, accuracy, altitude: null },
    timestamp: t,
  }) as unknown as GeolocationPosition

describe('carrera web (caracterizacion)', () => {
  it('empieza con GPS preciso, registra puntos y termina limpiando el watch', () => {
    empezarSeguimiento({ precisa: true })
    expect(watch).toHaveBeenCalledTimes(1)
    expect(watch.mock.calls[0][2]).toEqual({
      enableHighAccuracy: true,
      maximumAge: 1000,
      timeout: 20_000,
    })
    expect(tracker.estado.fase).toBe('corriendo')
    exito?.(pos(-4.0, 1000))
    exito?.(pos(-4.00003, 2000))
    expect(tracker.estado.puntos.length).toBeGreaterThanOrEqual(1)
    const fin = finalizarCarrera()
    expect(fin.fase).toBe('terminada')
    expect(clear).toHaveBeenCalledWith(7)
  })

  it('traduce errores del GPS', () => {
    const avisos: string[] = []
    empezarSeguimiento({ precisa: false, onError: (m) => avisos.push(m) })
    fallo?.({ code: 1, PERMISSION_DENIED: 1, TIMEOUT: 3 } as GeolocationPositionError)
    expect(avisos[0]).toContain('Permiso de ubicación denegado')
    expect(
      mensajeErrorGps({ code: 3, PERMISSION_DENIED: 1, TIMEOUT: 3 } as GeolocationPositionError)
    ).toContain('tarda')
    expect(
      mensajeErrorGps({ code: 2, PERMISSION_DENIED: 1, TIMEOUT: 3 } as GeolocationPositionError)
    ).toBe('No se pudo obtener la ubicación.')
  })

  it('sin GPS avisa y la carrera sigue', () => {
    Object.defineProperty(navigator, 'geolocation', { configurable: true, value: undefined })
    delete (navigator as unknown as Record<string, unknown>).geolocation
    const avisos: string[] = []
    empezarSeguimiento({ precisa: true, onError: (m) => avisos.push(m) })
    expect(avisos).toEqual(['Este navegador no tiene GPS.'])
    expect(tracker.estado.fase).toBe('corriendo')
  })

  it('descartar deja la carrera inactiva', () => {
    empezarSeguimiento({ precisa: true })
    descartarCarrera()
    expect(tracker.estado.fase).toBe('inactiva')
    expect(clear).toHaveBeenCalled()
  })
})
