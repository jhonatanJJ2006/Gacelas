import { useEffect, useRef, useState } from 'react'
import { debeMostrarIntro, dibujarIntro, DURACION_INTRO_S, marcarIntroVista } from '../lib/intro'

function almacenSesion(): Storage | null {
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
 * Intro de marca a pantalla completa (docs/MARCA.md). Se muestra al abrir la app y al volver tras
 * mas de 30 s fuera, encima de la app que ya carga debajo; se salta con clic, toque o cualquier tecla.
 */
export function IntroAnimada() {
  const [visible, setVisible] = useState(() =>
    debeMostrarIntro(almacenSesion(), reducirMovimiento())
  )
  const lienzo = useRef<HTMLCanvasElement>(null)

  // La ultima salida cuenta para la regla de 30 s; al volver tras una ausencia larga se repite.
  useEffect(() => {
    const alCambiar = () => {
      if (document.visibilityState === 'hidden') marcarIntroVista(almacenSesion())
      else if (debeMostrarIntro(almacenSesion(), reducirMovimiento())) setVisible(true)
    }
    const alSalir = () => marcarIntroVista(almacenSesion())
    document.addEventListener('visibilitychange', alCambiar)
    window.addEventListener('pagehide', alSalir)
    return () => {
      document.removeEventListener('visibilitychange', alCambiar)
      window.removeEventListener('pagehide', alSalir)
    }
  }, [])

  useEffect(() => {
    if (!visible) return
    marcarIntroVista(almacenSesion())
    const c = lienzo.current
    const ctx = c?.getContext('2d') ?? null
    let raf = 0
    let inicio: number | null = null
    const cerrar = () => setVisible(false)
    const cuadro = (ahora: number) => {
      inicio ??= ahora
      const t = (ahora - inicio) / 1000
      if (c && ctx) {
        // Hasta 1,5x: el brillo (shadowBlur) es caro y a mas resolucion no se nota.
        const dpr = Math.min(window.devicePixelRatio || 1, 1.5)
        const w = Math.round(c.clientWidth * dpr)
        const h = Math.round(c.clientHeight * dpr)
        if (c.width !== w || c.height !== h) {
          c.width = w
          c.height = h
        }
        dibujarIntro(ctx, w, h, Math.min(t, DURACION_INTRO_S))
      }
      if (t >= DURACION_INTRO_S) cerrar()
      else raf = requestAnimationFrame(cuadro)
    }
    raf = requestAnimationFrame(cuadro)
    window.addEventListener('keydown', cerrar)
    return () => {
      cancelAnimationFrame(raf)
      window.removeEventListener('keydown', cerrar)
    }
  }, [visible])

  if (!visible) return null
  return (
    <div
      role="button"
      tabIndex={-1}
      aria-label="Intro de Runcity, toca para saltar"
      data-testid="intro-animada"
      onClick={() => setVisible(false)}
      className="fixed inset-0 z-[9999] cursor-pointer bg-[#0C1322]"
    >
      <canvas ref={lienzo} className="block h-full w-full" />
    </div>
  )
}
