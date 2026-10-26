/**
 * Intro de marca "Ruta y destello" (docs/MARCA.md): logica y dibujo, sin React.
 * Misma animacion que assets/marca/intro-prototipo.html?v=A y que IntroAnimada.kt (Android/iOS).
 */

export const DURACION_INTRO_S = 2.8
export const CLAVE_INTRO = 'gacelas.intro.vista'
/** Ausencia minima para repetir la intro al volver a abrir la app: 30 s (igual que Android). */
export const UMBRAL_AUSENCIA_MS = 30_000

type Almacen = Pick<Storage, 'getItem' | 'setItem'>

/**
 * Se muestra cada vez que se abre la app (carga de la pagina o de la PWA), salvo que se haya
 * salido hace menos de UMBRAL_AUSENCIA_MS (recarga o cambio rapido). Nunca con
 * prefers-reduced-motion. CLAVE_INTRO guarda la ultima salida (ms epoch).
 */
export function debeMostrarIntro(
  almacen: Almacen | null,
  reducirMovimiento: boolean,
  ahora: number = Date.now()
): boolean {
  if (reducirMovimiento) return false
  try {
    const v = almacen?.getItem(CLAVE_INTRO)
    if (v == null) return true
    const salida = Number(v)
    return Number.isFinite(salida) && ahora - salida >= UMBRAL_AUSENCIA_MS
  } catch {
    return false
  }
}

/** Guarda el instante de la ultima vez que se vio o se dejo la app. */
export function marcarIntroVista(almacen: Almacen | null, ahora: number = Date.now()) {
  try {
    almacen?.setItem(CLAVE_INTRO, String(ahora))
  } catch {
    /* almacenamiento bloqueado: la intro simplemente no se recuerda */
  }
}

const W = 1080
const H = 1920
const GAZ =
  'M28 60 C33 52 46 49 60 50 L66 49 C69 45 72 40 75 36 L73 27 C72 23 70 20 67 18 C71 19 74 22 76 27 L78 33 C80 33 83 35 86 38 L83 41 L79 41 C77 45 74 50 72 55 C77 56 83 57 90 55 L91 58 C84 61 77 62 69 61 C64 64 58 66 52 66 C45 71 36 75 24 78 L22 75 C31 72 38 68 42 63 C37 63 32 64 28 66 L22 62 Z'

export const cl = (v: number, a = 0, b = 1) => Math.max(a, Math.min(b, v))
export const e = (t: number) => 1 - Math.pow(1 - cl(t), 3)
export const io = (t: number) => {
  const x = cl(t)
  return x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2
}

const RUTA: [number, number][] = Array.from({ length: 201 }, (_, i) => {
  const u = i / 200
  return [140 + u * 800, 1180 - Math.sin(u * Math.PI * 2.2) * 120 - u * 160 + Math.sin(u * 17) * 14]
})

let gacela: Path2D | null = null

/** Dibuja el cuadro del instante t (s) en un lienzo de cualquier tamano (encaja 1080 x 1920). */
export function dibujarIntro(x: CanvasRenderingContext2D, ancho: number, alto: number, t: number) {
  gacela ??= new Path2D(GAZ)
  const k = Math.min(ancho / W, alto / H)
  x.save()
  x.clearRect(0, 0, ancho, alto)
  const g = x.createRadialGradient(ancho / 2, alto * 0.45, 0, ancho / 2, alto * 0.45, 1100 * k)
  g.addColorStop(0, '#1A1030')
  g.addColorStop(0.6, '#0C1322')
  g.addColorStop(1, '#070E1D')
  x.fillStyle = g
  x.fillRect(0, 0, ancho, alto)
  x.translate((ancho - W * k) / 2, (alto - H * k) / 2)
  x.scale(k, k)

  // Ruta luminosa
  const n = Math.floor(e(t / 1.3) * 200)
  x.save()
  x.lineCap = 'round'
  x.lineJoin = 'round'
  x.shadowColor = '#A855F7'
  x.shadowBlur = 40 * k
  x.strokeStyle = '#A855F7'
  x.lineWidth = 14
  x.globalAlpha = 1 - cl((t - 1.4) / 0.4)
  x.beginPath()
  RUTA.slice(0, n + 1).forEach((p, i) => (i ? x.lineTo(p[0], p[1]) : x.moveTo(p[0], p[1])))
  x.stroke()
  if (n > 0 && t < 1.5) {
    const p = RUTA[n]
    x.fillStyle = '#10B981'
    x.shadowColor = '#10B981'
    x.beginPath()
    x.arc(p[0], p[1], 22, 0, 7)
    x.fill()
  }
  x.restore()

  // Destello
  const f = cl(1 - Math.abs(t - 1.45) / 0.25)
  if (f > 0) {
    const d = x.createRadialGradient(W / 2, H * 0.45, 0, W / 2, H * 0.45, 900 * f + 1)
    d.addColorStop(0, `rgba(243,232,255,${f})`)
    d.addColorStop(0.3, `rgba(168,85,247,${f * 0.6})`)
    d.addColorStop(1, 'rgba(0,0,0,0)')
    x.fillStyle = d
    x.fillRect(-W, -H, W * 3, H * 3)
  }

  // Gacela, palabra y zoom hacia dentro
  if (t > 1.35) {
    const kk = e((t - 1.35) / 0.4)
    let s = 9 * (0.7 + 0.3 * kk)
    let al = kk
    if (t > 2.2) {
      const z = io((t - 2.2) / 0.6)
      s *= 1 + z * 14
      al = 1 - z
    }
    x.save()
    x.translate(W / 2, H * 0.45)
    x.scale(s, s)
    x.translate(-56, -48)
    x.globalAlpha = al
    const m = x.createLinearGradient(-400, -400, 400, 400)
    m.addColorStop(0, '#F3E8FF')
    m.addColorStop(0.4, '#C084FC')
    m.addColorStop(0.75, '#A855F7')
    m.addColorStop(1, '#7B1FA2')
    x.fillStyle = m
    x.shadowColor = '#A855F7'
    x.shadowBlur = 30 * k
    x.fill(gacela)
    x.restore()
    x.save()
    x.globalAlpha = al * e((t - 1.6) / 0.4)
    x.fillStyle = '#fff'
    x.font = "700 120px 'Saira Semi Condensed', Impact, sans-serif"
    x.textAlign = 'center'
    ;(x as CanvasRenderingContext2D & { letterSpacing?: string }).letterSpacing =
      `${20 - 10 * kk}px`
    x.fillText('GACELAS', W / 2, H * 0.62)
    x.restore()
  }
  x.restore()
  if (t > 2.5) {
    x.fillStyle = `rgba(12,19,34,${io((t - 2.5) / 0.3)})`
    x.fillRect(0, 0, ancho, alto)
  }
}
