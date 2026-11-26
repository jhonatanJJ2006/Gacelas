import { describe, expect, it } from 'vitest'
import {
  camaraIntermedia,
  camaraParaPuntos,
  normalizarPadding,
  paddingPorPaneles,
  PADDING_BASE,
  suavizarEncuadre,
  ZOOM_UN_PUNTO,
} from '../shared/lib/encuadre'
import type { Coordenada } from '../shared/lib/mapas'

const TESELA = 256
const mercator = (p: Coordenada) => ({
  x: (p.lng + 180) / 360,
  y: 0.5 - Math.log(Math.tan(Math.PI / 4 + (p.lat * Math.PI) / 360)) / (2 * Math.PI),
})
/** Pixel de la pantalla donde cae un punto con la camara dada. */
const aPantalla = (
  p: Coordenada,
  cam: { centro: Coordenada; zoom: number },
  tam: { ancho: number; alto: number }
) => {
  const mundo = TESELA * Math.pow(2, cam.zoom)
  const a = mercator(p)
  const c = mercator(cam.centro)
  return { x: tam.ancho / 2 + (a.x - c.x) * mundo, y: tam.alto / 2 + (a.y - c.y) * mundo }
}

const LOJA: Coordenada[] = [
  { lat: -3.9746, lng: -79.2017 },
  { lat: -3.9845, lng: -79.2035 },
  { lat: -3.9931, lng: -79.2005 },
  { lat: -4.0045, lng: -79.1958 },
]
const TAM = { ancho: 800, alto: 600 }

describe('camaraParaPuntos', () => {
  it('sin puntos validos no hay camara', () => {
    expect(camaraParaPuntos([], TAM)).toBeNull()
    expect(camaraParaPuntos([{ lat: NaN, lng: 0 }], TAM)).toBeNull()
  })

  it('un solo punto: centra con zoom razonable', () => {
    const c = camaraParaPuntos([LOJA[0]], TAM)!
    expect(c.zoom).toBe(ZOOM_UN_PUNTO)
    expect(c.centro.lat).toBeCloseTo(LOJA[0].lat, 6)
    expect(c.centro.lng).toBeCloseTo(LOJA[0].lng, 6)
  })

  it('puntos repetidos cuentan como un punto', () => {
    const c = camaraParaPuntos([LOJA[0], LOJA[0], LOJA[0]], TAM, undefined, { zoomUnPunto: 14 })!
    expect(c.zoom).toBe(14)
  })

  it('toda la ruta cae dentro del area libre', () => {
    const padding = { top: 120, right: 40, bottom: 260, left: 40 }
    const c = camaraParaPuntos(LOJA, TAM, padding)!
    for (const p of LOJA) {
      const px = aPantalla(p, c, TAM)
      expect(px.x).toBeGreaterThanOrEqual(padding.left - 0.5)
      expect(px.x).toBeLessThanOrEqual(TAM.ancho - padding.right + 0.5)
      expect(px.y).toBeGreaterThanOrEqual(padding.top - 0.5)
      expect(px.y).toBeLessThanOrEqual(TAM.alto - padding.bottom + 0.5)
    }
  })

  it('con una hoja inferior grande la ruta sube y se aleja', () => {
    const libre = camaraParaPuntos(LOJA, TAM, 48)!
    const conHoja = camaraParaPuntos(LOJA, TAM, { bottom: 300 })!
    expect(conHoja.zoom).toBeLessThanOrEqual(libre.zoom)
    // El centro de la camara baja (mas al sur) para que la ruta quede arriba, sobre la hoja.
    expect(conHoja.centro.lat).toBeLessThan(libre.centro.lat)
  })

  it('el zoom es el mayor que aun deja entrar la ruta (aprovecha la franja libre)', () => {
    const c = camaraParaPuntos(LOJA, TAM, 48)!
    const xs = LOJA.map((p) => aPantalla(p, c, TAM))
    const ancho = Math.max(...xs.map((p) => p.x)) - Math.min(...xs.map((p) => p.x))
    const alto = Math.max(...xs.map((p) => p.y)) - Math.min(...xs.map((p) => p.y))
    const llenaAncho = Math.abs(ancho - (TAM.ancho - 96)) < 1
    const llenaAlto = Math.abs(alto - (TAM.alto - 96)) < 1
    expect(llenaAncho || llenaAlto).toBe(true)
  })

  it('zoom entero para mapas raster: nunca recorta la ruta', () => {
    const fino = camaraParaPuntos(LOJA, TAM, 48)!
    const entero = camaraParaPuntos(LOJA, TAM, 48, { zoomEntero: true })!
    expect(Number.isInteger(entero.zoom)).toBe(true)
    expect(entero.zoom).toBeLessThanOrEqual(fino.zoom)
    expect(entero.zoom).toBeGreaterThan(fino.zoom - 1)
  })

  it('respeta el zoom maximo con rutas diminutas', () => {
    const corta = [
      { lat: -3.99, lng: -79.2 },
      { lat: -3.99003, lng: -79.2 },
    ]
    expect(camaraParaPuntos(corta, TAM, 48, { zoomMaximo: 16 })!.zoom).toBe(16)
  })

  it('paneles que tapan casi todo no rompen el calculo', () => {
    const c = camaraParaPuntos(LOJA, TAM, { top: 500, bottom: 500 })!
    expect(Number.isFinite(c.zoom)).toBe(true)
    expect(Number.isFinite(c.centro.lat)).toBe(true)
  })
})

describe('padding', () => {
  it('numero o parcial', () => {
    expect(normalizarPadding(10)).toEqual({ top: 10, right: 10, bottom: 10, left: 10 })
    expect(normalizarPadding({ bottom: 200 })).toEqual({
      top: PADDING_BASE,
      right: PADDING_BASE,
      bottom: 200,
      left: PADDING_BASE,
    })
  })

  it('paneles: cabecera arriba, hoja abajo y lista lateral', () => {
    const mapa = { left: 0, top: 0, right: 400, bottom: 800 }
    const p = paddingPorPaneles(
      mapa,
      [
        { left: 0, top: 0, right: 400, bottom: 150 },
        { left: 0, top: 560, right: 400, bottom: 800 },
        { left: 280, top: 200, right: 400, bottom: 500 },
      ],
      20
    )
    expect(p.top).toBe(170)
    expect(p.bottom).toBe(260)
    expect(p.right).toBe(140)
    expect(p.left).toBe(PADDING_BASE)
  })

  it('un panel fuera del mapa no cuenta', () => {
    const p = paddingPorPaneles({ left: 0, top: 0, right: 400, bottom: 400 }, [
      { left: 0, top: 500, right: 400, bottom: 700 },
    ])
    expect(p.bottom).toBe(PADDING_BASE)
  })
})

describe('animacion', () => {
  it('arranca y termina exactos', () => {
    const a = { centro: { lat: -3.9, lng: -79.1 }, zoom: 12 }
    const b = { centro: { lat: -4.0, lng: -79.3 }, zoom: 15 }
    const inicio = camaraIntermedia(a, b, 0)
    const fin = camaraIntermedia(a, b, 1)
    expect(inicio.zoom).toBeCloseTo(12)
    expect(inicio.centro.lat).toBeCloseTo(-3.9, 6)
    expect(fin.zoom).toBeCloseTo(15)
    expect(fin.centro.lng).toBeCloseTo(-79.3, 6)
  })

  it('la curva es suave y monotona', () => {
    expect(suavizarEncuadre(0)).toBe(0)
    expect(suavizarEncuadre(1)).toBe(1)
    expect(suavizarEncuadre(0.5)).toBeCloseTo(0.5)
    expect(suavizarEncuadre(0.1)).toBeLessThan(0.1)
    expect(suavizarEncuadre(0.9)).toBeGreaterThan(0.9)
    expect(suavizarEncuadre(-1)).toBe(0)
    expect(suavizarEncuadre(2)).toBe(1)
  })
})
