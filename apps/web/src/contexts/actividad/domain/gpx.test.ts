import { describe, expect, it } from 'vitest'
import { carreraAGpx, nombreGpx } from './gpx'

const punto = {
  lat: -3.99,
  lng: -79.2,
  tiempoMs: Date.parse('2026-11-21T12:00:00.000Z'),
  precisionM: 5,
  altitudM: 2100,
}

describe('carreraAGpx', () => {
  it('escribe el trazo con altura y hora, y escapa el titulo', () => {
    const xml = carreraAGpx({
      titulo: 'Salida & meta <Loja>',
      inicioMs: punto.tiempoMs,
      puntos: [punto, { ...punto, lat: -4, altitudM: null, tiempoMs: punto.tiempoMs + 60_000 }],
    })
    expect(xml).toContain('Salida &amp; meta &lt;Loja&gt;')
    expect(xml).toContain(
      '<trkpt lat="-3.99" lon="-79.2"><ele>2100</ele><time>2026-11-21T12:00:00.000Z</time></trkpt>'
    )
    expect(xml).toContain(
      '<trkpt lat="-4" lon="-79.2"><time>2026-11-21T12:01:00.000Z</time></trkpt>'
    )
    expect(xml).not.toContain('<ele></ele>')
  })

  it('una carrera sin puntos sigue siendo un GPX valido', () => {
    const xml = carreraAGpx({ titulo: 'Vacia', inicioMs: NaN, puntos: [] })
    expect(xml).toContain('<trkseg></trkseg>')
    expect(xml).not.toContain('<time>')
  })
})

describe('nombreGpx', () => {
  it('deja un nombre de archivo seguro', () => {
    expect(nombreGpx('Carrera en el río!')).toBe('carrera-en-el-rio.gpx')
    expect(nombreGpx('???')).toBe('carrera.gpx')
  })
})
