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
