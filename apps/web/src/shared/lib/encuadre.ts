/**
 * Encuadre de rutas en el mapa: calcula la camara que muestra una ruta completa y la
 * anima con suavidad. Es el helper unico de la web: detalle de ruta, resumen de carrera,
 * eventos en vivo, lista de rutas, etc. Las cuentas son puras (se prueban sin mapa).
 */
import type { Coordenada } from './mapas'

/** Espacio (px) que tapan paneles, listas o tarjetas sobre el mapa, por lado. */
export interface PaddingMapa {
  top: number
  right: number
  bottom: number
  left: number
}

/** Margen visual por defecto entre la ruta y el borde del mapa. */
export const PADDING_BASE = 48
export const DURACION_ENCUADRE_MS = 900
/** Zoom con el que se centra una ruta de un solo punto. */
export const ZOOM_UN_PUNTO = 16
export const ZOOM_MAXIMO_RUTA = 17
export const ZOOM_MINIMO_RUTA = 3
/** Tamano de tesela de Google Maps (Mercator). */
const TESELA = 256

export function normalizarPadding(p?: number | Partial<PaddingMapa>): PaddingMapa {
  if (typeof p === 'number') return { top: p, right: p, bottom: p, left: p }
  return {
    top: p?.top ?? PADDING_BASE,
    right: p?.right ?? PADDING_BASE,
    bottom: p?.bottom ?? PADDING_BASE,
    left: p?.left ?? PADDING_BASE,
  }
}

export interface CamaraObjetivo {
  centro: Coordenada
  zoom: number
}

export interface OpcionesCamara {
  zoomMaximo?: number
  zoomMinimo?: number
  zoomUnPunto?: number
  /** Zoom entero (mapas raster): se redondea hacia abajo para que la ruta siempre entre. */
  zoomEntero?: boolean
}

const aMercator = (p: Coordenada) => {
  const lat = Math.max(-85.0511, Math.min(85.0511, p.lat))
  return {
    x: (p.lng + 180) / 360,
    y: 0.5 - Math.log(Math.tan(Math.PI / 4 + (lat * Math.PI) / 360)) / (2 * Math.PI),
  }
}

const deMercator = (x: number, y: number): Coordenada => ({
  lat: (Math.atan(Math.sinh(Math.PI * (1 - 2 * y))) * 180) / Math.PI,
  lng: x * 360 - 180,
})

/**
 * Camara (centro y zoom) que deja TODA la ruta dentro del area libre del mapa: la vista
 * menos el padding de cada lado. El centro compensa el padding asimetrico, asi la ruta
 * queda centrada en la franja visible (por ejemplo, encima de una hoja inferior).
 * Con un solo punto (o puntos casi iguales) centra con `zoomUnPunto`.
 */
export function camaraParaPuntos(
  puntos: Coordenada[],
  tam: { ancho: number; alto: number },
  padding?: number | Partial<PaddingMapa>,
  opciones: OpcionesCamara = {}
): CamaraObjetivo | null {
  const validos = puntos.filter((p) => Number.isFinite(p.lat) && Number.isFinite(p.lng))
  if (!validos.length) return null
  const zMax = opciones.zoomMaximo ?? ZOOM_MAXIMO_RUTA
  const zMin = opciones.zoomMinimo ?? ZOOM_MINIMO_RUTA
  const pad = normalizarPadding(padding)
  // Si los paneles tapan casi todo el mapa, se reduce el padding para dejar al menos 25 % libre.
  const libreAncho = Math.max(tam.ancho - pad.left - pad.right, tam.ancho * 0.25, 1)
  const libreAlto = Math.max(tam.alto - pad.top - pad.bottom, tam.alto * 0.25, 1)
  const escalaH = (tam.ancho - libreAncho) / Math.max(pad.left + pad.right, 1)
  const escalaV = (tam.alto - libreAlto) / Math.max(pad.top + pad.bottom, 1)
  const izq = pad.left * Math.min(1, escalaH)
  const der = pad.right * Math.min(1, escalaH)
  const arr = pad.top * Math.min(1, escalaV)
  const aba = pad.bottom * Math.min(1, escalaV)

  const m = validos.map(aMercator)
  const minX = Math.min(...m.map((p) => p.x))
  const maxX = Math.max(...m.map((p) => p.x))
  const minY = Math.min(...m.map((p) => p.y))
  const maxY = Math.max(...m.map((p) => p.y))
  const dx = maxX - minX
  const dy = maxY - minY

  // Menos de ~2 m de extension: es un punto.
  const unPunto = Math.max(dx, dy) * TESELA * Math.pow(2, 20) < 8
  let zoom: number
  if (unPunto) zoom = opciones.zoomUnPunto ?? ZOOM_UN_PUNTO
  else {
    const zx = dx > 0 ? Math.log2((tam.ancho - izq - der) / (dx * TESELA)) : Infinity
    const zy = dy > 0 ? Math.log2((tam.alto - arr - aba) / (dy * TESELA)) : Infinity
    zoom = Math.min(zx, zy, zMax)
  }
  zoom = Math.max(zMin, Math.min(zMax, zoom))
  if (opciones.zoomEntero) zoom = Math.floor(zoom)

  // El centro de la ruta va al centro del area libre, que esta desplazada respecto al de la vista.
  const mundo = TESELA * Math.pow(2, zoom)
  const cx = (minX + maxX) / 2 - (izq - der) / 2 / mundo
  const cy = (minY + maxY) / 2 - (arr - aba) / 2 / mundo
  return { centro: deMercator(cx, cy), zoom }
}

/** Interpolacion con arranque y llegada suaves. */
export const suavizarEncuadre = (t: number) => {
  const k = Math.min(Math.max(t, 0), 1)
  return k < 0.5 ? 4 * k * k * k : 1 - Math.pow(-2 * k + 2, 3) / 2
}

/** Punto intermedio de la animacion (centro en Mercator, zoom lineal). */
export function camaraIntermedia(de: CamaraObjetivo, a: CamaraObjetivo, t: number): CamaraObjetivo {
  const k = suavizarEncuadre(t)
  const p = aMercator(de.centro)
  const q = aMercator(a.centro)
  return {
    centro: deMercator(p.x + (q.x - p.x) * k, p.y + (q.y - p.y) * k),
    zoom: de.zoom + (a.zoom - de.zoom) * k,
  }
}

const animaciones = new WeakMap<google.maps.Map, () => void>()

const sinMovimiento = () =>
  typeof window !== 'undefined' &&
  typeof window.matchMedia === 'function' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

export interface OpcionesEncuadre extends OpcionesCamara {
  padding?: number | Partial<PaddingMapa>
  duracionMs?: number
}

/**
 * Anima la camara hasta mostrar la ruta completa (fit bounds animado con padding).
 * Cancela el encuadre anterior de ese mapa. Devuelve una funcion para cancelar.
 * Con `prefers-reduced-motion` la camara salta sin animar.
 */
export function encuadrarRuta(
  mapa: google.maps.Map,
  puntos: Coordenada[],
  opciones: OpcionesEncuadre = {}
): () => void {
  animaciones.get(mapa)?.()
  const div = mapa.getDiv()
  const ancho = div.clientWidth
  const alto = div.clientHeight
  if (!ancho || !alto) {
    // El contenedor aun no tiene tamano: se reintenta cuando el mapa quede listo.
    const l = mapa.addListener('idle', () => {
      l.remove()
      encuadrarRuta(mapa, puntos, opciones)
    })
    return () => l.remove()
  }
  const capacidades = mapa.getMapCapabilities?.() as
    { isFractionalZoomEnabled?: boolean } | undefined
  const fraccionario = capacidades?.isFractionalZoomEnabled === true
  const destino = camaraParaPuntos(puntos, { ancho, alto }, opciones.padding, {
    ...opciones,
    zoomEntero: opciones.zoomEntero ?? !fraccionario,
  })
  if (!destino) return () => {}
  const duracion = sinMovimiento() ? 0 : (opciones.duracionMs ?? DURACION_ENCUADRE_MS)
  const centroActual = mapa.getCenter()
  if (!duracion || !centroActual) {
    mapa.moveCamera({ center: destino.centro, zoom: destino.zoom })
    return () => {}
  }
  const desde: CamaraObjetivo = {
    centro: { lat: centroActual.lat(), lng: centroActual.lng() },
    zoom: mapa.getZoom() ?? destino.zoom,
  }
  let id = 0
  const inicio = performance.now()
  const cancelar = () => {
    cancelAnimationFrame(id)
    arrastre.remove()
    if (animaciones.get(mapa) === cancelar) animaciones.delete(mapa)
  }
  // Si la persona toma el mapa, la camara le obedece.
  const arrastre = mapa.addListener('dragstart', cancelar)
  const paso = (ahora: number) => {
    const t = (ahora - inicio) / duracion
    const c = camaraIntermedia(desde, destino, t)
    mapa.moveCamera({
      center: c.centro,
      zoom: fraccionario || t >= 1 ? c.zoom : Math.round(c.zoom),
    })
    if (t < 1) id = requestAnimationFrame(paso)
    else cancelar()
  }
  animaciones.set(mapa, cancelar)
  id = requestAnimationFrame(paso)
  return cancelar
}

export interface Caja {
  left: number
  top: number
  right: number
  bottom: number
}

/**
 * Padding que dejan los paneles que tapan el mapa (cabecera, hoja inferior, lista lateral).
 * Un panel ancho (60 % o mas del mapa) cuenta arriba o abajo segun su posicion; uno angosto,
 * a la izquierda o a la derecha. `margen` es el aire que se deja junto a cada panel.
 */
export function paddingPorPaneles(mapa: Caja, paneles: Caja[], margen = 24): PaddingMapa {
  const ancho = mapa.right - mapa.left
  const alto = mapa.bottom - mapa.top
  const r: PaddingMapa = { top: 0, right: 0, bottom: 0, left: 0 }
  for (const p of paneles) {
    const w = Math.min(p.right, mapa.right) - Math.max(p.left, mapa.left)
    const h = Math.min(p.bottom, mapa.bottom) - Math.max(p.top, mapa.top)
    if (w <= 0 || h <= 0) continue
    const cx = (p.left + p.right) / 2 - mapa.left
    const cy = (p.top + p.bottom) / 2 - mapa.top
    if (w >= ancho * 0.6)
      if (cy < alto / 2)
        r.top = Math.max(r.top, Math.min(p.bottom, mapa.bottom) - mapa.top + margen)
      else r.bottom = Math.max(r.bottom, mapa.bottom - Math.max(p.top, mapa.top) + margen)
    else if (cx < ancho / 2)
      r.left = Math.max(r.left, Math.min(p.right, mapa.right) - mapa.left + margen)
    else r.right = Math.max(r.right, mapa.right - Math.max(p.left, mapa.left) + margen)
  }
  return {
    top: Math.max(r.top, PADDING_BASE),
    right: Math.max(r.right, PADDING_BASE),
    bottom: Math.max(r.bottom, PADDING_BASE),
    left: Math.max(r.left, PADDING_BASE),
  }
}
