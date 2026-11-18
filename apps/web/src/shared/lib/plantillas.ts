import { decimal } from './geo'
import {
  formatoRitmo,
  formatoTiempo,
  HANDLE_RUNCITY,
  normalizarHandle,
  proyectar,
  type DatosCompartir,
} from './compartir'
export { normalizarHandle }

/** Plantillas transparentes 9:16 para poner encima de una foto propia. */
export type Plantilla = 'estadisticas' | 'traza' | 'fecha' | 'logro'
export const PLANTILLAS: { id: Plantilla; etiqueta: string }[] = [
  { id: 'estadisticas', etiqueta: 'Estadísticas' },
  { id: 'traza', etiqueta: 'Traza de ruta' },
  { id: 'fecha', etiqueta: 'Día y fecha' },
  { id: 'logro', etiqueta: 'Logro / racha' },
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
  estadisticas: { ancho: 1080, alto: 1920 },
  traza: { ancho: 1080, alto: 1920 },
  fecha: { ancho: 1080, alto: 1920 },
  logro: { ancho: 1080, alto: 1920 },
}

export const NARANJA = '#C084FC'
export const HANDLE_GACELAS = HANDLE_RUNCITY
const FUENTE = '"Inter", "Helvetica Neue", "Segoe UI", Roboto, Arial, sans-serif'

export function textoInsignia(d: DatosCompartir) {
  if (d.recordLabel?.trim()) return `Récord personal · ${d.recordLabel.trim()}`
  return d.rachaDias && d.rachaDias > 0
    ? `Racha de ${d.rachaDias} ${d.rachaDias === 1 ? 'día' : 'días'}`
    : null
}

/** Datos compactos (etiqueta, valor) para las plantillas. */
export function datosPlantilla(d: DatosCompartir): [string, string][] {
  const r: [string, string][] = [['Distancia', `${decimal(d.distanciaKm, 2)} km`]]
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
  estadisticas: { cx: 0.5, cy: 0.5, escala: 1 },
  traza: { cx: 0.5, cy: 0.5, escala: 1 },
  fecha: { cx: 0.5, cy: 0.5, escala: 1 },
  logro: { cx: 0.5, cy: 0.5, escala: 1 },
}

/** Rectangulo donde va la superposicion dentro del lienzo final. */
export function rectColocacion(
  _p: Plantilla,
  c: Colocacion,
  W: number,
  H: number,
  f: FormatoFoto = 'historia'
) {
  const t = FORMATOS_FOTO[f]
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

/** Dibuja una plantilla transparente en el formato de salida seleccionado. */
export function dibujarPlantilla(
  g: G,
  d: DatosCompartir,
  p: Plantilla,
  h: string,
  f: FormatoFoto = 'historia'
) {
  const { ancho: W, alto: H } = FORMATOS_FOTO[f]
  const sx = W / 1080
  const sy = H / 1920
  g.clearRect(0, 0, W, H)
  const t = (s: string, x: number, y: number, px: number, peso = 500, alinear: CanvasTextAlign = 'left') =>
    texto(g, s, x * sx, y * sy, px * sx, peso, alinear)
  const pie = () => {
    marca(g, W / 2, 1770 * sy, 38 * sx, 'center')
    handle(g, normalizarHandle(h), W / 2, 1840 * sy, 30 * sx, 'center')
  }
  const metrica = (titulo: string, valor: string, x: number, y: number) => {
    t(titulo.toUpperCase(), x, y, 26, 600)
    t(valor, x, y + 70, 56, 700)
  }
  if (p === 'estadisticas') {
    t('DISTANCIA', 72, 350, 28, 600)
    t(`${decimal(d.distanciaKm, 2)}`, 72, 560, 172, 800)
    t('KILÓMETROS', 80, 625, 30, 600)
    metrica('Tiempo', formatoTiempo(d.segundos ?? 0), 80, 850)
    metrica('Ritmo / km', formatoRitmo(d.ritmoSegKm), 580, 850)
    const badge = textoInsignia(d)
    if (badge) {
      g.fillStyle = 'rgba(192,132,252,0.22)'
      redondeado(g, 64 * sx, 1130 * sy, W - 128 * sx, 120 * sy, 36 * sx)
      g.fill()
      t(badge, 540, 1210, 38, 700, 'center')
    }
    pie()
  } else if (p === 'traza') {
    linea(g, d, { x: 70 * sx, y: 250 * sy, ancho: W - 140 * sx, alto: 760 * sy }, 12 * sx)
    metrica('Distancia', `${decimal(d.distanciaKm, 2)} km`, 80, 1170)
    metrica('Tiempo', formatoTiempo(d.segundos ?? 0), 570, 1170)
    metrica('Ritmo / km', formatoRitmo(d.ritmoSegKm), 80, 1435)
    metrica('Desnivel', `${Math.round(d.desnivelM ?? 0)} m`, 570, 1435)
    pie()
  } else if (p === 'fecha') {
    const date = new Date(d.fechaMs ?? 0)
    const dia = new Intl.DateTimeFormat('es-EC', { weekday: 'long' }).format(date)
    const fecha = new Intl.DateTimeFormat('es-EC', { day: 'numeric', month: 'long', year: 'numeric' }).format(date)
    t(dia.charAt(0).toUpperCase() + dia.slice(1), 72, 720, 76, 700)
    t(fecha, 72, 900, 54, 400)
    t(`${decimal(d.distanciaKm, 2)} km`, 72, 1160, 64, 700)
    pie()
  } else {
    t(textoInsignia(d) ?? 'CADA CARRERA CUENTA', 72, 600, 58, 800)
    t(`${decimal(d.distanciaKm, 2)} km`, 72, 850, 88, 700)
    if (d.segundos) {
      metrica('Tiempo', formatoTiempo(d.segundos), 80, 1100)
      metrica('Ritmo / km', formatoRitmo(d.ritmoSegKm), 570, 1100)
    }
    pie()
  }
}

const aBlob = (cv: HTMLCanvasElement) =>
  new Promise<Blob>((ok, mal) =>
    cv.toBlob((b) => (b ? ok(b) : mal(new Error('No se pudo generar la imagen'))), 'image/png')
  )

export function lienzoPlantilla(d: DatosCompartir, p: Plantilla, h: string, f: FormatoFoto = 'historia') {
  const { ancho, alto } = FORMATOS_FOTO[f]
  const cv = document.createElement('canvas')
  cv.width = ancho
  cv.height = alto
  dibujarPlantilla(cv.getContext('2d')!, d, p, h, f)
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
  c: Colocacion,
  f: FormatoFoto = 'historia'
) {
  g.clearRect(0, 0, W, H)
  if (foto) {
    const a = ajusteCubrir(foto.width, foto.height, W, H)
    g.drawImage(foto, a.sx, a.sy, a.sw, a.sh, 0, 0, W, H)
  }
  const r = rectColocacion(p, c, W, H, f)
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
  dibujarComposicion(cv.getContext('2d')!, ancho, alto, foto, capa, p, c, f)
  return aBlob(cv)
}
