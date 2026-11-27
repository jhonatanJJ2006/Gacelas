import { describe, expect, it, vi } from 'vitest'
import {
  copiarImagen,
  enlaceRuta,
  enlaceWhatsApp,
  fechaPlantillaMs,
  fechaValidaMs,
  metricas,
  proyectar,
  recortarExtremos,
  textoCompartir,
  textoFechaPlantilla,
  type DatosCompartir,
} from '../shared/lib/compartir'

const linea = Array.from({ length: 21 }, (_, i) => ({ lat: -4 + i * 0.0009, lng: -79.2 })) // ~100 m entre puntos

const carrera: DatosCompartir = {
  tipo: 'carrera',
  titulo: 'Carrera matutina',
  puntos: linea,
  distanciaKm: 5.234,
  segundos: 1712,
  ritmoSegKm: 327,
  desnivelM: 48,
  rachaDias: 6,
  enlace: enlaceRuta('abc'),
}

describe('compartir', () => {
  it('oculta los puntos dentro de 300 m de la salida y llegada', () => {
    const r = recortarExtremos(linea)
    expect(r[0].lat).toBeCloseTo(linea[3].lat, 6)
    expect(r[r.length - 1].lat).toBeCloseTo(linea[17].lat, 6)
    expect(recortarExtremos(linea.slice(0, 4))).toEqual([])
  })
  it('usa distancia geografica al extremo en vez de distancia acumulada', () => {
    const ruta = [
      { lat: 0, lng: 0 },
      { lat: 0, lng: 0.001 },
      { lat: 0.001, lng: 0.001 },
      { lat: 0.001, lng: 0 },
      { lat: 0.002, lng: 0 },
      { lat: 0.003, lng: 0 },
      { lat: 0.004, lng: 0 },
      { lat: 0.005, lng: 0 },
      { lat: 0.006, lng: 0 },
      { lat: 0.007, lng: 0 },
      { lat: 0.008, lng: 0 },
      { lat: 0.009, lng: 0 },
      { lat: 0.01, lng: 0 },
      { lat: 0.011, lng: 0 },
      { lat: 0.012, lng: 0 },
    ]
    const r = recortarExtremos(ruta)
    expect(r[0]).toBe(ruta[5])
    expect(r[r.length - 1]).toBe(ruta[11])
  })
  it('proyecta el trazo dentro de la caja', () => {
    const caja = { x: 60, y: 100, ancho: 960, alto: 900 }
    const p = proyectar(linea, caja)
    p.forEach(({ x, y }) => {
      expect(x).toBeGreaterThanOrEqual(caja.x)
      expect(x).toBeLessThanOrEqual(caja.x + caja.ancho)
      expect(y).toBeGreaterThanOrEqual(caja.y)
      expect(y).toBeLessThanOrEqual(caja.y + caja.alto)
    })
    expect(p[0].y).toBeGreaterThan(p[20].y) // el norte queda arriba
  })
  it('arma metricas, texto y enlace de WhatsApp', () => {
    expect(metricas(carrera)).toEqual([
      ['Distancia', '5,23 km'],
      ['Tiempo', '28:32'],
      ['Ritmo /km', `5'27"`],
      ['Desnivel', '48 m'],
    ])
    expect(textoCompartir(carrera)).toContain('racha de 6 días')
    const wa = enlaceWhatsApp(carrera)
    expect(wa.startsWith('https://wa.me/?text=')).toBe(true)
    expect(decodeURIComponent(wa)).toContain('https://gacelas.vercel.app/r/abc')
  })
  it('copia PNG al portapapeles cuando el navegador lo permite', async () => {
    const anterior = navigator.clipboard
    const escribir = vi.fn().mockResolvedValue(undefined)
    class ClipboardItemMock {}
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { write: escribir },
    })
    vi.stubGlobal('ClipboardItem', ClipboardItemMock)
    try {
      expect(await copiarImagen(new Blob(['png'], { type: 'image/png' }))).toBe(true)
      expect(escribir).toHaveBeenCalledOnce()
    } finally {
      vi.unstubAllGlobals()
      Object.defineProperty(navigator, 'clipboard', { configurable: true, value: anterior })
    }
  })

  it('nunca usa la epoca Unix si la fecha falta o es invalida', () => {
    const respaldo = Date.UTC(2026, 10, 26, 17, 0, 0)
    expect(fechaValidaMs(undefined)).toBeNull()
    expect(fechaValidaMs(null)).toBeNull()
    expect(fechaValidaMs(0)).toBeNull()
    expect(fechaValidaMs(NaN)).toBeNull()
    expect(fechaValidaMs(-1)).toBeNull()
    expect(fechaPlantillaMs(0, respaldo)).toBe(respaldo)
    expect(fechaPlantillaMs(undefined, respaldo)).toBe(respaldo)
    const { dia, fecha } = textoFechaPlantilla(0, respaldo)
    expect(dia).toBe('Jueves')
    expect(fecha).toBe('26 de noviembre de 2026')
    expect(fecha).not.toMatch(/1970|1969/)
  })
  it('acepta una fecha valida en milisegundos o en segundos Unix', () => {
    const ms = 1_700_000_000_000
    expect(fechaValidaMs(ms)).toBe(ms)
    expect(fechaValidaMs(1_700_000_000)).toBe(ms)
    const { dia, fecha } = textoFechaPlantilla(1_700_000_000)
    expect(dia).toBe('Martes')
    expect(fecha).toBe('14 de noviembre de 2023')
  })

  it('usa descarga como fallback si ClipboardItem no está disponible', async () => {
    vi.stubGlobal('ClipboardItem', undefined)
    try {
      expect(await copiarImagen(new Blob(['png'], { type: 'image/png' }))).toBe(false)
    } finally {
      vi.unstubAllGlobals()
    }
  })
})
