import {
  debeMostrarIntro,
  DURACION_INTRO_S,
  ESPERA_PRIMER_CUADRO_MS,
  marcarIntroVista,
} from './intro'

type Almacen = Pick<Storage, 'getItem' | 'setItem'>

function almacenLocal(): Almacen | null {
  try {
    return window.localStorage
  } catch {
    return null
  }
}

const reducirMovimiento = () =>
  typeof window.matchMedia === 'function' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

/**
 * Controla la intro en video de index.html (docs/MARCA.md). La decision inicial la toma el script
 * en linea de index.html; aqui se retira al terminar (ended), al tocar, con una tecla, si el video
 * falla o si no muestra su primer cuadro en ESPERA_PRIMER_CUADRO_MS, y se repite al volver tras
 * mas de 30 s fuera. Devuelve una funcion para soltar los escuchadores.
 */
export function iniciarIntroVideo(
  doc: Document = document,
  almacen: Almacen | null = almacenLocal()
): () => void {
  const el = doc.getElementById('intro')
  const video = el?.querySelector('video') ?? null
  if (!el || !video) return () => {}
  let espera: ReturnType<typeof setTimeout> | undefined
  let tope: ReturnType<typeof setTimeout> | undefined

  const cerrar = () => {
    clearTimeout(espera)
    clearTimeout(tope)
    if (el.hidden) return
    el.hidden = true
    video.pause()
  }
  const vigilar = (desde = 0) => {
    clearTimeout(espera)
    clearTimeout(tope)
    // Fallback: sin primer cuadro en 1 s (sin datos, autoplay bloqueado) la app se muestra igual.
    espera = setTimeout(
      () => {
        if (video.readyState < 2 || video.paused) cerrar()
      },
      Math.max(0, ESPERA_PRIMER_CUADRO_MS - desde)
    )
    // Tope de seguridad por si "ended" nunca llega.
    tope = setTimeout(cerrar, Math.max(0, DURACION_INTRO_S * 1000 + 1500 - desde))
  }
  const mostrar = () => {
    el.hidden = false
    marcarIntroVista(almacen)
    try {
      video.currentTime = 0
    } catch {
      /* sin metadatos aun */
    }
    const p = video.play()
    if (p && typeof p.catch === 'function') p.catch(cerrar)
    vigilar()
  }
  const alCambiar = () => {
    if (doc.visibilityState === 'hidden') {
      cerrar()
      marcarIntroVista(almacen)
    } else if (debeMostrarIntro(almacen, reducirMovimiento())) mostrar()
  }
  const alSalir = () => marcarIntroVista(almacen)

  if (!el.hidden) {
    const p = video.paused ? video.play() : undefined
    if (p && typeof p.catch === 'function') p.catch(cerrar)
    // En la primera apertura los plazos cuentan desde que se empezo a cargar la pagina.
    vigilar(Math.min(performance.now(), ESPERA_PRIMER_CUADRO_MS))
  }
  video.addEventListener('ended', cerrar)
  // Con <source> hijos el error de carga llega a cada <source>, no al <video>: se escucha en captura.
  el.addEventListener('error', cerrar, true)
  el.addEventListener('click', cerrar)
  doc.addEventListener('keydown', cerrar)
  doc.addEventListener('visibilitychange', alCambiar)
  window.addEventListener('pagehide', alSalir)
  return () => {
    clearTimeout(espera)
    clearTimeout(tope)
    video.removeEventListener('ended', cerrar)
    el.removeEventListener('error', cerrar, true)
    el.removeEventListener('click', cerrar)
    doc.removeEventListener('keydown', cerrar)
    doc.removeEventListener('visibilitychange', alCambiar)
    window.removeEventListener('pagehide', alSalir)
  }
}
