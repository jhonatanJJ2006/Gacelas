import { useEffect, useState } from 'react'
import type { ClimaActual } from '../domain/clima'
import { climaActual, leerCacheClima } from '../infrastructure/climaOpenMeteo'

/** Clima del chip de Inicio: cache al instante y refresco al montar (minimo 15 min entre llamadas). */
export function useClima(): ClimaActual | null {
  const [clima, setClima] = useState<ClimaActual | null>(() => leerCacheClima())
  useEffect(() => {
    let vivo = true
    void climaActual(Date.now()).then((c) => {
      if (vivo && c) setClima(c)
    })
    return () => {
      vivo = false
    }
  }, [])
  return clima
}
