/**
 * Sistema de movimiento C2 (equivalente web de MovimientoC2.kt). Mismas duraciones y curvas que
 * Android/iOS; con "reducir movimiento" del sistema no se anima nada.
 */
import { useEffect, useRef, useState, type CSSProperties } from 'react'

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

let forzado: boolean | null = null

/** Fuerza "reducir movimiento" (true/false) o vuelve a leer el sistema (null). Lo usan las pruebas. */
export function forzarMovimientoReducido(valor: boolean | null) {
  forzado = valor
}

export function movimientoReducido(): boolean {
  if (forzado !== null) return forzado
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

/** Estilo para listas escalonadas: se combina con la clase mov-escalon (ver index.css). */
export function estiloEscalon(indice: number): CSSProperties {
  return { '--i': Math.min(Math.max(indice, 0), MOVIMIENTO.escalonesMax) } as CSSProperties
}

/** Desaceleracion suave (easeOutExpo recortada) para las cifras que cuentan. */
export function suavizarConteo(t: number): number {
  const x = Math.min(Math.max(t, 0), 1)
  return x >= 1 ? 1 : 1 - Math.pow(2, -10 * x)
}

/**
 * Cifra que cuenta hacia arriba desde 0 (o desde el valor anterior si cambia) hasta [objetivo].
 * Con "reducir movimiento" devuelve el valor final al instante. Anima con requestAnimationFrame
 * y cancela al desmontar.
 */
export function useConteo(objetivo: number, duracion: number = MOVIMIENTO.conteo): number {
  const [valor, setValor] = useState(() => (movimientoReducido() ? objetivo : 0))
  const desde = useRef(valor)
  useEffect(() => {
    if (movimientoReducido() || typeof requestAnimationFrame !== 'function') {
      desde.current = objetivo
      setValor(objetivo)
      return
    }
    const inicio = desde.current
    const t0 = performance.now()
    let id = 0
    const paso = (ahora: number) => {
      const p = suavizarConteo((ahora - t0) / duracion)
      const v = p >= 1 ? objetivo : inicio + (objetivo - inicio) * p
      desde.current = v
      setValor(v)
      if (p < 1) id = requestAnimationFrame(paso)
    }
    id = requestAnimationFrame(paso)
    return () => cancelAnimationFrame(id)
  }, [objetivo, duracion])
  return valor
}

/**
 * Mantiene montado un elemento [duracion] ms despues de que [abierto] pase a false, para poder
 * animar su salida. Devuelve [montado, cerrando]. Con "reducir movimiento" no espera.
 */
export function useSalidaAnimada(
  abierto: boolean,
  duracion: number = MOVIMIENTO.corto
): [boolean, boolean] {
  const [montado, setMontado] = useState(abierto)
  useEffect(() => {
    if (abierto) {
      setMontado(true)
      return
    }
    if (movimientoReducido()) {
      setMontado(false)
      return
    }
    const id = setTimeout(() => setMontado(false), duracion)
    return () => clearTimeout(id)
  }, [abierto, duracion])
  const visible = abierto || (montado && !movimientoReducido())
  return [visible, !abierto && visible]
}
