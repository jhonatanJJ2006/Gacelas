import { formatoRitmo, formatoTiempo, proyectar, type DatosCompartir } from './compartir'

/** Plantillas transparentes (estilo Strava) para poner encima de una foto propia. */
export type Plantilla = 'trazo' | 'trazoDatos' | 'datosGrandes' | 'minimal'
export const PLANTILLAS: { id: Plantilla; etiqueta: string }[] = [
  { id: 'trazoDatos', etiqueta: 'Trazo + datos' },
  { id: 'trazo', etiqueta: 'Solo trazo' },
  { id: 'datosGrandes', etiqueta: 'Datos grandes' },
  { id: 'minimal', etiqueta: 'Minimal' },
]

export type FormatoFoto = 'historia' | 'post45' | 'cuadrado'
export const FORMATOS_FOTO: Record<FormatoFoto, { ancho: number; alto: number; etiqueta: string }> =
  {
    historia: { ancho: 1080, alto: 1920, etiqueta: 'Historia 9:16' },
    post45: { ancho: 1080, alto: 1350, etiqueta: 'Post 4:5' },
    cuadrado: { ancho: 1080, alto: 1080, etiqueta: 'Post 1:1' },
  }

/** Tamano del lienzo transparente de cada plantilla. */
export const TAMANO_PLANTILLA: Record<Plantilla, { ancho: number; alto: number }> = {
  trazo: { ancho: 900, alto: 900 },
  trazoDatos: { ancho: 900, alto: 900 },
  datosGrandes: { ancho: 900, alto: 1000 },
  minimal: { ancho: 1000, alto: 360 },
}

export const NARANJA = '#FF6A13'
export const HANDLE_GACELAS = '@gacelasrunningteam'
const FUENTE = '"Inter", "Helvetica Neue", "Segoe UI", Roboto, Arial, sans-serif'

/** Normaliza el @usuario: sin espacios, con @, maximo 30 caracteres. */
export function normalizarHandle(h: string) {
  const limpio = h.trim().replace(/^@+/, '').replace(/\s+/g, '').slice(0, 30)
  return limpio ? `@${limpio}` : ''
}

/** Datos compactos (etiqueta, valor) para las plantillas. */
export function datosPlantilla(d: DatosCompartir): [string, string][] {
  const r: [string, string][] = [['Distancia', `${d.distanciaKm.toFixed(2)} km`]]
  if (d.segundos)
    r.push(['Ritmo', `${formatoRitmo(d.ritmoSegKm)} /km`], ['Tiempo', formatoTiempo(d.segundos)])
  else if (d.desnivelM !== undefined) r.push(['Desnivel', `${Math.round(d.desnivelM)} m`])
  return r
}

/** Recorte tipo "cover": que parte de la foto usar para llenar W x H sin deformar. */
export function ajusteCubrir(iw: number, ih: number, W: number, H: number) {
  const k = Math.max(W / iw, H / ih)
  const sw = W / k
  const sh = H / k
  return { sx: (iw - sw) / 2, sy: (ih - sh) / 2, sw, sh }
}

/** Posicion de la superposicion: centro normalizado (0..1) y escala relativa al ancho del lienzo. */
export interface Colocacion {
  cx: number
  cy: number
  escala: number
}
export const COLOCACION_INICIAL: Record<Plantilla, Colocacion> = {
  trazo: { cx: 0.5, cy: 0.55, escala: 0.75 },
  trazoDatos: { cx: 0.62, cy: 0.62, escala: 0.62 },
  datosGrandes: { cx: 0.5, cy: 0.5, escala: 0.7 },
  minimal: { cx: 0.5, cy: 0.86, escala: 0.85 },
}

/** Rectangulo donde va la superposicion dentro del lienzo final. */
export function rectColocacion(p: Plantilla, c: Colocacion, W: number, H: number) {
  const t = TAMANO_PLANTILLA[p]
  const ancho = Math.max(0.15, Math.min(1.6, c.escala)) * W
  const alto = (ancho * t.alto) / t.ancho
  return { x: c.cx * W - ancho / 2, y: c.cy * H - alto / 2, ancho, alto }
}

// --- Dibujo -------------------------------------------------------------------------------

type G = CanvasRenderingContext2D

function sombra(g: G, fuerte = false) {
  g.shadowColor = 'rgba(0,0,0,0.45)'
  g.shadowBlur = fuerte ? 14 : 8
  g.shadowOffsetY = 1
}
function sinSombra(g: G) {
  g.shadowColor = 'transparent'
  g.shadowBlur = 0
  g.shadowOffsetY = 0
}
function texto(
  g: G,
  t: string,
  x: number,
  y: number,
  px: number,
  peso = 300,
  alinear: CanvasTextAlign = 'left',
  espaciado = 0
) {
  g.font = `${peso} ${px}px ${FUENTE}`
  g.textAlign = alinear
  g.textBaseline = 'alphabetic'
  g.fillStyle = '#fff'
  ;(g as G & { letterSpacing?: string }).letterSpacing = `${espaciado}px`
  g.fillText(t, x, y)
  ;(g as G & { letterSpacing?: string }).letterSpacing = '0px'
}

function linea(
  g: G,
  d: DatosCompartir,
  caja: { x: number; y: number; ancho: number; alto: number },
  grosor: number
) {
  const pts = proyectar(d.puntos, caja, 0.04)
  if (pts.length < 2) return
  g.lineCap = 'round'
  g.lineJoin = 'round'
  sombra(g, true)
  g.strokeStyle = NARANJA
  g.lineWidth = grosor
  g.beginPath()
  pts.forEach((p, i) => (i ? g.lineTo(p.x, p.y) : g.moveTo(p.x, p.y)))
  g.stroke()
  sinSombra(g)
}

/** Marca pequena "RUNCITY" (texto espaciado, sin fondo). */
function marca(g: G, x: number, y: number, px: number, alinear: CanvasTextAlign = 'left') {
  sombra(g)
  texto(g, 'RUNCITY', x, y, px, 700, alinear, px * 0.35)
  sinSombra(g)
}
function handle(
  g: G,
  h: string,
  x: number,
  y: number,
  px: number,
  alinear: CanvasTextAlign = 'left'
) {
  if (!h) return
  sombra(g)
  g.font = `300 ${px}px ${FUENTE}`
  texto(g, h, x, y, px, 300, alinear)
  const w = g.measureText(h).width
  const x0 = alinear === 'center' ? x - w / 2 : alinear === 'right' ? x - w : x
  g.fillRect(x0, y + px * 0.22, w, Math.max(1.5, px * 0.05))
  sinSombra(g)
}

/** Dibuja la plantilla transparente en un contexto de TAMANO_PLANTILLA[p]. */
export function dibujarPlantilla(g: G, d: DatosCompartir, p: Plantilla, h: string) {
  const { ancho: W, alto: H } = TAMANO_PLANTILLA[p]
  g.clearRect(0, 0, W, H)
  const datos = datosPlantilla(d)
  if (p === 'trazo') {
    linea(g, d, { x: 40, y: 30, ancho: W - 80, alto: H - 150 }, 11)
    marca(g, W / 2, H - 70, 26, 'center')
    handle(g, h, W / 2, H - 22, 26, 'center')
  } else if (p === 'trazoDatos') {
    marca(g, 20, 48, 26)
    linea(g, d, { x: 10, y: 80, ancho: W * 0.62, alto: H - 200 }, 9)
    sombra(g)
    let y = 210
    for (const [k, v] of datos) {
      texto(g, k.toUpperCase(), W * 0.7, y, 20, 400, 'left', 3)
      texto(g, v, W * 0.7, y + 46, 40, 300)
      y += 130
    }
    sinSombra(g)
    handle(g, h, W - 20, H - 30, 28, 'right')
  } else if (p === 'datosGrandes') {
    sombra(g)
    const [dist, ...resto] = datos
    texto(g, dist[1].replace(' km', ''), W / 2, 230, 210, 200, 'center', -4)
    texto(g, 'KILÓMETROS', W / 2, 290, 26, 400, 'center', 8)
    resto.forEach(([k, v], i) => {
      const x = resto.length === 1 ? W / 2 : W * (0.27 + i * 0.46)
      texto(g, v, x, 400, 52, 300, 'center')
      texto(g, k.toUpperCase(), x, 440, 20, 400, 'center', 4)
    })
    sinSombra(g)
    linea(g, d, { x: W * 0.18, y: 480, ancho: W * 0.64, alto: 380 }, 8)
    marca(g, W / 2, H - 70, 24, 'center')
    handle(g, h, W / 2, H - 24, 24, 'center')
  } else {
    linea(g, d, { x: 10, y: 20, ancho: 200, alto: H - 40 }, 6)
    sombra(g)
    texto(g, datos.map((x) => x[1]).join('   ·   '), 250, H / 2 + 6, 42, 300)
    sinSombra(g)
    marca(g, 250, H / 2 - 70, 22)
    handle(g, h, 250, H / 2 + 80, 24)
  }
}

const aBlob = (cv: HTMLCanvasElement) =>
  new Promise<Blob>((ok, mal) =>
    cv.toBlob((b) => (b ? ok(b) : mal(new Error('No se pudo generar la imagen'))), 'image/png')
  )

export function lienzoPlantilla(d: DatosCompartir, p: Plantilla, h: string) {
  const { ancho, alto } = TAMANO_PLANTILLA[p]
  const cv = document.createElement('canvas')
  cv.width = ancho
  cv.height = alto
  dibujarPlantilla(cv.getContext('2d')!, d, p, h)
  return cv
}

/** PNG transparente de la plantilla (para usar como sticker). */
export const generarSuperposicion = (d: DatosCompartir, p: Plantilla, h: string) =>
  aBlob(lienzoPlantilla(d, p, h))

/** Dibuja la foto (cover) y encima la plantilla colocada. Sin foto queda transparente. */
export function dibujarComposicion(
  g: G,
  W: number,
  H: number,
  foto: (CanvasImageSource & { width: number; height: number }) | null,
  capa: HTMLCanvasElement,
  p: Plantilla,
  c: Colocacion
) {
  g.clearRect(0, 0, W, H)
  if (foto) {
    const a = ajusteCubrir(foto.width, foto.height, W, H)
    g.drawImage(foto, a.sx, a.sy, a.sw, a.sh, 0, 0, W, H)
  }
  const r = rectColocacion(p, c, W, H)
  g.drawImage(capa, r.x, r.y, r.ancho, r.alto)
}

export async function exportarComposicion(
  foto: HTMLImageElement | null,
  capa: HTMLCanvasElement,
  p: Plantilla,
  c: Colocacion,
  f: FormatoFoto
) {
  const { ancho, alto } = FORMATOS_FOTO[f]
  const cv = document.createElement('canvas')
  cv.width = ancho
  cv.height = alto
  dibujarComposicion(cv.getContext('2d')!, ancho, alto, foto, capa, p, c)
  return aBlob(cv)
}
