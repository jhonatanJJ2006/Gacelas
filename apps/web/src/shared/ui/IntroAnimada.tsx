import { useEffect, useRef, useState } from 'react'
import { debeMostrarIntro, dibujarIntro, DURACION_INTRO_S, marcarIntroVista } from '../lib/intro'

function almacenSesion(): Storage | null {
  try {
    return window.sessionStorage
  } catch {
    return null
  }
}

const reducirMovimiento = () =>
  typeof window.matchMedia === 'function' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

/**
 * Intro de marca a pantalla completa (docs/MARCA.md). Se muestra una vez por sesion al cargar la
 * app, encima de la app que ya carga debajo; se salta con clic, toque o cualquier tecla.
 */
export function IntroAnimada() {
  const [visible, setVisible] = useState(() =>
    debeMostrarIntro(almacenSesion(), reducirMovimiento())
  )
  const lienzo = useRef<HTMLCanvasElement>(null)

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
      aria-label="Intro de Gacelas, toca para saltar"
      data-testid="intro-animada"
      onClick={() => setVisible(false)}
      className="fixed inset-0 z-[9999] cursor-pointer bg-[#0C1322]"
    >
      <canvas ref={lienzo} className="block h-full w-full" />
    </div>
  )
}
