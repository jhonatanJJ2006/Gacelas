import { useEffect, useRef, useState } from 'react'
import type { Coordenada } from '../../lib/mapas'
import { suavizar } from '../../lib/mapaEstilo'

/** Posicion animada: se desliza hacia el objetivo en `ms` (sin saltos entre lecturas GPS). */
export function usePosicionSuave(objetivo: Coordenada | null, ms = 900) {
  const [pos, setPos] = useState(objetivo)
  const desde = useRef(objetivo)
  useEffect(() => {
    if (!objetivo) return
    const inicio = performance.now()
    const de = desde.current ?? objetivo
    let id = 0
    const paso = (t: number) => {
      const k = suavizar((t - inicio) / ms)
      const p = {
        lat: de.lat + (objetivo.lat - de.lat) * k,
        lng: de.lng + (objetivo.lng - de.lng) * k,
      }
      desde.current = p
      setPos(p)
      if (k < 1) id = requestAnimationFrame(paso)
    }
    id = requestAnimationFrame(paso)
    return () => cancelAnimationFrame(id)
  }, [objetivo?.lat, objetivo?.lng, ms]) // eslint-disable-line react-hooks/exhaustive-deps
  return pos
}
