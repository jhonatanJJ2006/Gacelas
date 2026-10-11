import { describe, expect, it } from 'vitest'
import {
  enlaceRuta,
  enlaceWhatsApp,
  metricas,
  proyectar,
  recortarExtremos,
  textoCompartir,
  type DatosCompartir,
} from '../compartir'

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
  it('oculta los primeros y ultimos 250 m de una carrera', () => {
    const r = recortarExtremos(linea, 250)
    expect(r[0].lat).toBeCloseTo(linea[3].lat, 6)
    expect(r[r.length - 1].lat).toBeCloseTo(linea[17].lat, 6)
    expect(recortarExtremos(linea.slice(0, 4), 250)).toEqual([])
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
      ['Distancia', '5.23 km'],
      ['Tiempo', '28:32'],
      ['Ritmo /km', `5'27"`],
      ['Desnivel', '48 m'],
    ])
    expect(textoCompartir(carrera)).toContain('racha de 6 días')
    const wa = enlaceWhatsApp(carrera)
    expect(wa.startsWith('https://wa.me/?text=')).toBe(true)
    expect(decodeURIComponent(wa)).toContain('https://gacelas.vercel.app/r/abc')
  })
})
