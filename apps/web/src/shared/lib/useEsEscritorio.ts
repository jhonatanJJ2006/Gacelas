import { useEffect, useState } from 'react'

const CONSULTA = '(min-width: 1024px)'

/** true cuando la ventana tiene ancho de escritorio (>=1024px). */
export function useEsEscritorio() {
  const soporta = typeof window !== 'undefined' && typeof window.matchMedia === 'function'
  const [es, setEs] = useState(() => soporta && window.matchMedia(CONSULTA).matches)
  useEffect(() => {
    if (!soporta) return
    const mq = window.matchMedia(CONSULTA)
    const alCambiar = () => setEs(mq.matches)
    mq.addEventListener?.('change', alCambiar)
    return () => mq.removeEventListener?.('change', alCambiar)
  }, [soporta])
  return es
}

/** true cuando la ventana mide al menos `px` de ancho (por ejemplo 1280 para el panel lateral). */
export function useAnchoMinimo(px: number) {
  const consulta = `(min-width: ${px}px)`
  const soporta = typeof window !== 'undefined' && typeof window.matchMedia === 'function'
  const [es, setEs] = useState(() => soporta && window.matchMedia(consulta).matches)
  useEffect(() => {
    if (!soporta) return
    const mq = window.matchMedia(consulta)
    const alCambiar = () => setEs(mq.matches)
    alCambiar()
    mq.addEventListener?.('change', alCambiar)
    return () => mq.removeEventListener?.('change', alCambiar)
  }, [soporta, consulta])
  return es
}
