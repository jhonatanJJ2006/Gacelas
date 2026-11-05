/**
 * Sistema de movimiento C2 (equivalente web de MovimientoC2.kt). Mismas duraciones y curvas que
 * Android/iOS; con "reducir movimiento" del sistema no se anima nada.
 */
import { useEffect, useRef } from 'react'

export const MOVIMIENTO = {
  corto: 150,
  medio: 250,
  largo: 350,
  escalon: 45,
  escalonesMax: 6,
  conteo: 900,
  entrar: 'cubic-bezier(0.2, 0, 0, 1)',
  salir: 'cubic-bezier(0.3, 0, 1, 1)',
} as const

export function movimientoReducido(): boolean {
  return (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  )
}

/** Retraso (ms) del elemento [indice] de una lista escalonada, con tope. */
export function retrasoEscalon(indice: number, reducido = false): number {
  if (reducido) return 0
  return MOVIMIENTO.escalon * Math.min(Math.max(indice, 0), MOVIMIENTO.escalonesMax)
}

/**
 * Fundido de entrada del contenido al cambiar de ruta, sin desmontar la pantalla (no pierde
 * estado) y solo con opacidad para no mover elementos fijos.
 */
export function useEntradaPagina<T extends HTMLElement>(clave: string) {
  const ref = useRef<T>(null)
  useEffect(() => {
    const el = ref.current
    if (!el || movimientoReducido() || typeof el.animate !== 'function') return
    el.animate([{ opacity: 0 }, { opacity: 1 }], {
      duration: MOVIMIENTO.medio,
      easing: MOVIMIENTO.entrar,
    })
  }, [clave])
  return ref
}
