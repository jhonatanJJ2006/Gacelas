import { haversineM } from './geo'

export type FormatoCompartir = 'historia' | 'post' | 'sticker'
export interface Punto {
  lat: number
  lng: number
}

export interface DatosCompartir {
  titulo: string
  puntos: Punto[]
  distanciaKm: number
  segundos?: number
  ritmoSegKm?: number | null
  desnivelM?: number
  rachaDias?: number
  /** Texto de la etiqueta superior: "Carrera" o "Ruta". */
  tipo: 'carrera' | 'ruta'
  enlace: string
}

export const TAMANOS: Record<FormatoCompartir, { ancho: number; alto: number }> = {
  historia: { ancho: 1080, alto: 1920 },
  post: { ancho: 1080, alto: 1080 },
  sticker: { ancho: 900, alto: 900 },
}

export const SITIO = 'https://gacelas.vercel.app'
export const enlaceRuta = (id: string) => `${SITIO}/r/${encodeURIComponent(id)}`

/**
 * Privacidad: quita los primeros y ultimos `metros` del trazo de una carrera para no revelar
 * dónde vive el corredor (la misma idea que el "ocultar casa" del en vivo).
 */
export function recortarExtremos(puntos: Punto[], metros = 250): Punto[] {
  if (puntos.length < 3) return []
  const corte = (lista: Punto[]) => {
    let d = 0
    for (let i = 1; i < lista.length; i++) {
      d += haversineM(lista[i - 1].lat, lista[i - 1].lng, lista[i].lat, lista[i].lng)
      if (d >= metros) return i
    }
    return lista.length
  }
  const ini = corte(puntos)
  const fin = puntos.length - 1 - corte([...puntos].reverse())
  return ini <= fin ? puntos.slice(ini, fin + 1) : []
}

/** Proyecta el trazo (mercator simple) dentro de una caja, centrado y manteniendo la proporción. */
export function proyectar(
  puntos: Punto[],
  caja: { x: number; y: number; ancho: number; alto: number },
  margen = 0.08
): { x: number; y: number }[] {
  if (!puntos.length) return []
  const xs = puntos.map((p) => p.lng * Math.cos((puntos[0].lat * Math.PI) / 180))
  const ys = puntos.map((p) => -p.lat)
  const minX = Math.min(...xs)
  const minY = Math.min(...ys)
  const w = Math.max(Math.max(...xs) - minX, 1e-9)
  const h = Math.max(Math.max(...ys) - minY, 1e-9)
  const anchoUtil = caja.ancho * (1 - 2 * margen)
  const altoUtil = caja.alto * (1 - 2 * margen)
  const k = Math.min(anchoUtil / w, altoUtil / h)
  const ox = caja.x + (caja.ancho - w * k) / 2
  const oy = caja.y + (caja.alto - h * k) / 2
  return xs.map((x, i) => ({ x: ox + (x - minX) * k, y: oy + (ys[i] - minY) * k }))
}

const dos = (n: number) => String(n).padStart(2, '0')
export function formatoTiempo(seg: number) {
  const h = Math.floor(seg / 3600)
  const m = Math.floor((seg % 3600) / 60)
  const s = Math.floor(seg % 60)
  return h ? `${h}:${dos(m)}:${dos(s)}` : `${m}:${dos(s)}`
}
export const formatoRitmo = (s?: number | null) =>
  s && isFinite(s) && s > 0 ? `${Math.floor(s / 60)}'${dos(Math.round(s % 60))}"` : '--'

/** Metricas a mostrar (etiqueta, valor) segun lo que haya. */
export function metricas(d: DatosCompartir): [string, string][] {
  const r: [string, string][] = [['Distancia', `${d.distanciaKm.toFixed(2)} km`]]
  if (d.segundos) r.push(['Tiempo', formatoTiempo(d.segundos)])
  if (d.segundos) r.push(['Ritmo /km', formatoRitmo(d.ritmoSegKm)])
  if (d.desnivelM !== undefined) r.push(['Desnivel', `${Math.round(d.desnivelM)} m`])
  return r
}

export function textoCompartir(d: DatosCompartir) {
  const base =
    d.tipo === 'carrera'
      ? `Corrí ${d.distanciaKm.toFixed(2)} km${d.segundos ? ` en ${formatoTiempo(d.segundos)}` : ''} con Gacelas`
      : `Mira esta ruta en Gacelas: ${d.titulo} (${d.distanciaKm.toFixed(1)} km)`
  const racha = d.rachaDias && d.rachaDias > 1 ? ` · racha de ${d.rachaDias} días` : ''
  return `${base}${racha} 🦌🔥`
}

export const enlaceWhatsApp = (d: DatosCompartir) =>
  `https://wa.me/?text=${encodeURIComponent(`${textoCompartir(d)}\n${d.enlace}`)}`

function cargar(src: string): Promise<HTMLImageElement | null> {
  return new Promise((ok) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => ok(img)
    img.onerror = () => ok(null)
    img.src = src
  })
}

/** Dibuja la imagen de marca de Gacelas en un canvas y la devuelve como PNG. */
export async function generarImagen(d: DatosCompartir, formato: FormatoCompartir): Promise<Blob> {
  const { ancho: W, alto: H } = TAMANOS[formato]
  const cv = document.createElement('canvas')
  cv.width = W
  cv.height = H
  const g = cv.getContext('2d')!
  const [logo, corredor] = await Promise.all([
    cargar('/marca/logo-compartir.png'),
    formato === 'sticker' ? Promise.resolve(null) : cargar('/models/gacelas-runner-poster.png'),
  ])
  await document.fonts?.ready
  const fuente = (peso: number, px: number) =>
    `${peso} ${px}px Inter, "Segoe UI", Roboto, sans-serif`
  const naranja = '#A855F7'

  if (formato === 'sticker') {
    // Fondo transparente: solo trazo + metricas en una tarjeta redondeada.
    g.fillStyle = 'rgba(11,18,32,0.82)'
    redondeado(g, 0, 0, W, H, 64)
    g.fill()
    trazo(g, proyectar(d.puntos, { x: 60, y: 140, ancho: W - 120, alto: 470 }), naranja, 14)
    cabecera(g, logo, 60, 50, fuente, 44)
    const m = metricas(d).slice(0, 3)
    m.forEach(([e, v], i) => {
      const x = 60 + (i * (W - 120)) / m.length
      g.fillStyle = '#fff'
      g.font = fuente(800, 56)
      g.fillText(v, x, 720)
      g.fillStyle = '#94a3b8'
      g.font = fuente(600, 30)
      g.fillText(e.toUpperCase(), x, 770)
    })
    if (d.rachaDias && d.rachaDias > 0) {
      g.fillStyle = naranja
      g.font = fuente(800, 40)
      g.fillText(`🔥 ${d.rachaDias} días de racha`, 60, 850)
    }
    return aBlob(cv)
  }

  // Fondo: degradado oscuro y una cuadricula de "calles" como mapa estilizado.
  const fondo = g.createLinearGradient(0, 0, 0, H)
  fondo.addColorStop(0, '#0b1220')
  fondo.addColorStop(1, '#111827')
  g.fillStyle = fondo
  g.fillRect(0, 0, W, H)
  const historia = formato === 'historia'
  const mapa = historia
    ? { x: 60, y: 260, ancho: W - 120, alto: 900 }
    : { x: 60, y: 170, ancho: 620, alto: 560 }
  g.save()
  redondeado(g, mapa.x, mapa.y, mapa.ancho, mapa.alto, 40)
  g.clip()
  g.fillStyle = '#0f1a2e'
  g.fillRect(mapa.x, mapa.y, mapa.ancho, mapa.alto)
  g.strokeStyle = '#1f2a3d'
  g.lineWidth = 6
  for (let i = -mapa.alto; i < mapa.ancho; i += 90) {
    g.beginPath()
    g.moveTo(mapa.x + i, mapa.y)
    g.lineTo(mapa.x + i + mapa.alto * 0.45, mapa.y + mapa.alto)
    g.stroke()
  }
  for (let j = 40; j < mapa.alto; j += 120) {
    g.beginPath()
    g.moveTo(mapa.x, mapa.y + j)
    g.lineTo(mapa.x + mapa.ancho, mapa.y + j + 30)
    g.stroke()
  }
  g.fillStyle = 'rgba(6,78,59,0.55)'
  g.beginPath()
  g.ellipse(mapa.x + mapa.ancho * 0.75, mapa.y + mapa.alto * 0.3, 120, 80, 0.4, 0, Math.PI * 2)
  g.fill()
  trazo(g, proyectar(d.puntos, mapa, 0.12), naranja, historia ? 16 : 12)
  g.restore()

  if (corredor) {
    const alto = historia ? 520 : 580
    const ancho = (corredor.width / corredor.height) * alto
    g.drawImage(
      corredor,
      historia ? W - ancho - 20 : W - ancho + 30,
      historia ? 730 : 120,
      ancho,
      alto
    )
  }
  cabecera(g, logo, 60, historia ? 110 : 50, fuente, historia ? 60 : 48)
  g.fillStyle = '#fff'
  g.font = fuente(800, historia ? 64 : 50)
  g.fillText(recortar(g, d.titulo, W - 120), 60, historia ? 220 : 145)

  const m = metricas(d)
  const filaY = historia ? 1260 : 820
  const col = (W - 120) / Math.min(m.length, historia ? 2 : 4)
  m.forEach(([e, v], i) => {
    const fila = historia ? Math.floor(i / 2) : 0
    const x = 60 + (historia ? i % 2 : i) * col
    const y = filaY + fila * 200
    g.fillStyle = i === 0 ? naranja : '#fff'
    g.font = fuente(800, historia ? 92 : 50)
    g.fillText(v, x, y + (historia ? 80 : 50))
    g.fillStyle = '#94a3b8'
    g.font = fuente(600, historia ? 34 : 26)
    g.fillText(e.toUpperCase(), x, y + (historia ? 130 : 90))
  })
  const pieY = historia ? 1760 : 1010
  if (d.rachaDias && d.rachaDias > 0) {
    g.fillStyle = 'rgba(245,158,11,0.16)'
    redondeado(g, 60, pieY - (historia ? 70 : 52), historia ? 520 : 400, historia ? 96 : 70, 48)
    g.fill()
    g.fillStyle = naranja
    g.font = fuente(800, historia ? 44 : 34)
    g.fillText(
      `🔥 ${d.rachaDias} ${d.rachaDias === 1 ? 'día' : 'días'} de racha`,
      90,
      pieY - (historia ? 7 : 6)
    )
  }
  g.fillStyle = '#94a3b8'
  g.font = fuente(600, historia ? 32 : 26)
  g.textAlign = 'right'
  g.fillText('gacelas.vercel.app', W - 60, pieY - 10)
  g.textAlign = 'left'
  return aBlob(cv)
}

function cabecera(
  g: CanvasRenderingContext2D,
  logo: HTMLImageElement | null,
  x: number,
  y: number,
  fuente: (p: number, px: number) => string,
  px: number
) {
  if (logo) g.drawImage(logo, x - 10, y - px * 1.1, px * 1.6, px * 1.6)
  g.fillStyle = '#fff'
  g.font = fuente(900, px)
  g.fillText('GACELAS', x + px * 1.6, y + px * 0.25)
}

function trazo(
  g: CanvasRenderingContext2D,
  pts: { x: number; y: number }[],
  color: string,
  ancho: number
) {
  if (pts.length < 2) return
  g.lineCap = 'round'
  g.lineJoin = 'round'
  g.shadowColor = color
  g.shadowBlur = 24
  g.strokeStyle = color
  g.lineWidth = ancho
  g.beginPath()
  pts.forEach((p, i) => (i ? g.lineTo(p.x, p.y) : g.moveTo(p.x, p.y)))
  g.stroke()
  g.shadowBlur = 0
  const punto = (p: { x: number; y: number }, c: string) => {
    g.fillStyle = c
    g.beginPath()
    g.arc(p.x, p.y, ancho * 1.3, 0, Math.PI * 2)
    g.fill()
    g.strokeStyle = '#fff'
    g.lineWidth = 5
    g.stroke()
  }
  punto(pts[0], '#10B981')
  punto(pts[pts.length - 1], '#EF4444')
}

function redondeado(
  g: CanvasRenderingContext2D,
  x: number,
  y: number,
  w: number,
  h: number,
  r: number
) {
  g.beginPath()
  g.moveTo(x + r, y)
  g.arcTo(x + w, y, x + w, y + h, r)
  g.arcTo(x + w, y + h, x, y + h, r)
  g.arcTo(x, y + h, x, y, r)
  g.arcTo(x, y, x + w, y, r)
  g.closePath()
}

function recortar(g: CanvasRenderingContext2D, t: string, max: number) {
  if (g.measureText(t).width <= max) return t
  while (t.length > 1 && g.measureText(t + '…').width > max) t = t.slice(0, -1)
  return t + '…'
}

const aBlob = (cv: HTMLCanvasElement) =>
  new Promise<Blob>((ok, mal) =>
    cv.toBlob((b) => (b ? ok(b) : mal(new Error('No se pudo generar la imagen'))), 'image/png')
  )

/** Comparte con Web Share API nivel 2 (archivos). Devuelve false si el navegador no puede. */
export async function compartirNativo(d: DatosCompartir, imagen: Blob): Promise<boolean> {
  const archivo = new File([imagen], 'gacelas.png', { type: 'image/png' })
  const datos: ShareData = {
    files: [archivo],
    title: d.titulo,
    text: `${textoCompartir(d)}\n${d.enlace}`,
  }
  if (!navigator.canShare?.(datos)) return false
  try {
    await navigator.share(datos)
  } catch (e) {
    if ((e as Error).name !== 'AbortError') return false
  }
  return true
}
