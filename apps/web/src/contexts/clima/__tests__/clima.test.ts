import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import {
  aClima,
  aproximar,
  debeRefrescar,
  descripcionClima,
  etiquetaLugar,
  LOJA,
} from '../domain/clima'
import {
  CLAVE_CACHE_CLIMA,
  climaActual,
  leerOpenMeteo,
  leerRespuesta,
  reiniciarIntentosClima,
  urlOpenMeteo,
} from '../infrastructure/climaOpenMeteo'

/** Respuesta real de Open-Meteo (Loja, 1 nov 2026 19:00 hora de Ecuador). */
const FIXTURE = {
  latitude: -3.9718807,
  longitude: -79.22638,
  generationtime_ms: 227.7677059173584,
  utc_offset_seconds: 0,
  timezone: 'GMT',
  timezone_abbreviation: 'GMT',
  elevation: 2074.0,
  current_units: {
    time: 'iso8601',
    interval: 'seconds',
    temperature_2m: '°C',
    weather_code: 'wmo code',
    is_day: '',
  },
  current: {
    time: '2026-11-02T00:00',
    interval: 900,
    temperature_2m: 16.1,
    weather_code: 3,
    is_day: 0,
  },
}

const T0 = 1_800_000_000_000

describe('reglas de clima', () => {
  it('traduce los codigos WMO', () => {
    expect(descripcionClima(0, true)).toBe('despejado')
    expect(descripcionClima(0, false)).toBe('noche despejada')
    expect(descripcionClima(1, true)).toBe('mayormente despejado')
    expect(descripcionClima(2, true)).toBe('parcialmente nublado')
    expect(descripcionClima(3, false)).toBe('nublado')
    expect(descripcionClima(45, true)).toBe('niebla')
    expect(descripcionClima(53, true)).toBe('llovizna')
    expect(descripcionClima(63, true)).toBe('lluvia')
    expect(descripcionClima(81, true)).toBe('chubascos')
    expect(descripcionClima(73, true)).toBe('nieve')
    expect(descripcionClima(95, true)).toBe('tormenta')
    expect(descripcionClima(999, true)).toBe('nublado')
  })

  it('refresca cada 15 min como minimo', () => {
    expect(debeRefrescar(null, T0)).toBe(true)
    expect(debeRefrescar(T0, T0 + 14 * 60_000)).toBe(false)
    expect(debeRefrescar(T0, T0 + 15 * 60_000)).toBe(true)
  })

  it('etiqueta Loja cerca o sin ubicacion, Tu zona lejos', () => {
    expect(etiquetaLugar(null)).toBe('Loja')
    expect(etiquetaLugar({ lat: -4.03, lng: -79.2 })).toBe('Loja')
    expect(etiquetaLugar({ lat: -0.18, lng: -78.47 })).toBe('Tu zona')
  })

  it('aproxima a 2 decimales y redondea la temperatura', () => {
    expect(aproximar(LOJA)).toEqual({ lat: -3.99, lng: -79.2 })
    expect(aClima({ temperatura: 16.5, codigoWmo: 3, esDeDia: true }, 'Loja', 1).temperatura).toBe(
      17
    )
  })
})

describe('repositorio de clima', () => {
  beforeEach(() => {
    localStorage.clear()
    reiniciarIntentosClima()
  })
  afterEach(() => localStorage.clear())

  it('lee la respuesta real', () => {
    expect(leerRespuesta(FIXTURE)).toEqual({ temperatura: 16.1, codigoWmo: 3, esDeDia: false })
    expect(() => leerRespuesta({})).toThrow()
  })

  it('exito sin permiso: Loja, coordenadas aproximadas y cache', async () => {
    let url = ''
    const f = (async (u: string) => {
      url = u
      return new Response(JSON.stringify(FIXTURE))
    }) as unknown as typeof fetch
    const c = await climaActual(T0, {
      ubicacion: async () => null,
      leer: (x) => leerOpenMeteo(x, 5000, f),
    })
    expect(c).toEqual({ temperatura: 16, descripcion: 'nublado', lugar: 'Loja', consultadoMs: T0 })
    expect(url).toBe(urlOpenMeteo({ lat: -3.99, lng: -79.2 }))
    expect(JSON.parse(localStorage.getItem(CLAVE_CACHE_CLIMA)!)).toEqual(c)
  })

  it('error de red devuelve la cache; sin cache devuelve null', async () => {
    const falla = async () => {
      throw new Error('sin red')
    }
    expect(await climaActual(T0, { ubicacion: async () => null, leer: falla })).toBeNull()
    reiniciarIntentosClima()
    const viejo = {
      temperatura: 12,
      descripcion: 'lluvia',
      lugar: 'Loja',
      consultadoMs: T0 - 3_600_000,
    }
    localStorage.setItem(CLAVE_CACHE_CLIMA, JSON.stringify(viejo))
    expect(await climaActual(T0, { ubicacion: async () => null, leer: falla })).toEqual(viejo)
  })

  it('cancela la llamada tras el timeout', async () => {
    const colgado = ((_: string, init?: RequestInit) =>
      new Promise((_ok, mal) =>
        init?.signal?.addEventListener('abort', () => mal(new Error('abort')))
      )) as typeof fetch
    await expect(leerOpenMeteo(LOJA, 10, colgado)).rejects.toThrow('abort')
  })

  it('cache reciente no llama a la red y fuera de Loja dice Tu zona', async () => {
    const reciente = {
      temperatura: 18,
      descripcion: 'despejado',
      lugar: 'Loja',
      consultadoMs: T0 - 60_000,
    }
    localStorage.setItem(CLAVE_CACHE_CLIMA, JSON.stringify(reciente))
    let llamadas = 0
    const leer = async () => {
      llamadas++
      return { temperatura: 20, codigoWmo: 0, esDeDia: true }
    }
    expect(
      await climaActual(T0, { ubicacion: async () => ({ lat: -0.18, lng: -78.47 }), leer })
    ).toEqual(reciente)
    expect(llamadas).toBe(0)
    const nuevo = await climaActual(T0 + 15 * 60_000, {
      ubicacion: async () => ({ lat: -0.18, lng: -78.47 }),
      leer,
    })
    expect(nuevo?.lugar).toBe('Tu zona')
    expect(llamadas).toBe(1)
  })
})
