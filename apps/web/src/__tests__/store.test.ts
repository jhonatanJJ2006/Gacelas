import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { repositoriosLocales } from '../app/composicion'
import { usarRepositorios, useApp } from '../store/app'
import {
  descartarCarrera,
  empezarSeguimiento,
  finalizarCarrera,
  mensajeErrorGps,
  tracker,
} from '../store/carrera'
import { instalarGpsFalso } from '../test/utils'

describe('store de la app', () => {
  beforeEach(() => {
    localStorage.clear()
    usarRepositorios(repositoriosLocales())
  })

  it('carga, registra, guarda y cierra sesion', async () => {
    await useApp.getState().cargar()
    expect(useApp.getState()).toMatchObject({ listo: true, sesion: null })
    const r = await useApp.getState().registrar('Jhona', 'jhona@utpl.edu.ec', 'gacela123')
    expect(r.ok).toBe(true)
    expect(useApp.getState().sesion?.nombre).toBe('Jhona')
    await useApp.getState().actualizarAjustes({ unidadesMetricas: false })
    await useApp.getState().actualizarAjustes((a) => ({ metronomoBpm: a.metronomoBpm + 2 }))
    expect(useApp.getState().ajustes).toMatchObject({ unidadesMetricas: false, metronomoBpm: 170 })
    await useApp.getState().alternarRutaGuardada('1')
    await useApp.getState().alternarInscripcion('2')
    await useApp.getState().crearEvento({
      id: 'u',
      titulo: 't',
      descripcion: '',
      fecha: '',
      hora: '',
      ubicacion: '',
      participantes: 0,
      distanciaKm: 5,
      pasado: false,
      creadoPorUsuario: true,
    })
    expect(useApp.getState().inscritos.sort()).toEqual(['2', 'u'])
    await useApp.getState().cerrarSesion()
    expect(useApp.getState().sesion).toBeNull()
    expect((await useApp.getState().iniciarSesion('jhona@utpl.edu.ec', 'mala1234')).ok).toBe(false)
    await useApp.getState().iniciarSesion('jhona@utpl.edu.ec', 'gacela123')
    await useApp.getState().cargar()
    expect(useApp.getState()).toMatchObject({
      rutasGuardadas: ['1'],
      ajustes: { unidadesMetricas: false },
    })
    expect(useApp.getState().sesion?.email).toBe('jhona@utpl.edu.ec')
    expect((await useApp.getState().recuperarContrasena('x@y.com')).ok).toBe(true)
  })
})

describe('seguimiento GPS', () => {
  afterEach(() => descartarCarrera())

  it('usa watchPosition, suma puntos, pide wake lock y limpia al terminar', async () => {
    const gps = instalarGpsFalso()
    const liberar = vi.fn(() => Promise.resolve())
    const request = vi.fn(() => Promise.resolve({ release: liberar, addEventListener: vi.fn() }))
    Object.defineProperty(navigator, 'wakeLock', { value: { request }, configurable: true })
    empezarSeguimiento({ precisa: true })
    expect(gps.geo.watchPosition).toHaveBeenCalledWith(
      expect.any(Function),
      expect.any(Function),
      expect.objectContaining({ enableHighAccuracy: true })
    )
    expect(request).toHaveBeenCalledWith('screen')
    gps.emitir(-4, -79.2, 0)
    gps.emitir(-4.0001, -79.2, 4000)
    expect(tracker.estado.distanciaKm).toBeGreaterThan(0.01)
    await new Promise((r) => setTimeout(r, 0))
    const e = finalizarCarrera()
    expect(e.fase).toBe('terminada')
    expect(gps.activos()).toBe(0)
    expect(liberar).toHaveBeenCalled()
  })

  it('avisa los errores del GPS', () => {
    const gps = instalarGpsFalso()
    const onError = vi.fn()
    empezarSeguimiento({ precisa: false, onError })
    gps.fallar(1)
    expect(onError).toHaveBeenCalledWith(expect.stringMatching(/denegado/))
    const err = (code: number) =>
      ({ code, PERMISSION_DENIED: 1, TIMEOUT: 3 }) as GeolocationPositionError
    expect(mensajeErrorGps(err(3))).toMatch(/tarda/)
    expect(mensajeErrorGps(err(2))).toMatch(/No se pudo/)
  })
})
