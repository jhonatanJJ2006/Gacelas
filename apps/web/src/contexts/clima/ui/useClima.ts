import { useEffect } from 'react'
import { create } from 'zustand'
import type { ClimaActual } from '../domain/clima'
import {
  climaActual,
  leerCacheClima,
  reiniciarIntentosClima,
} from '../infrastructure/climaOpenMeteo'

export type EstadoClima = (
  | { estado: 'cargando'; clima: null }
  | { estado: 'listo'; clima: ClimaActual }
  | { estado: 'sin-datos'; clima: null }
) & { reintentar: () => void }

interface Almacen {
  clima: ClimaActual | null
  termino: boolean
  enCurso: Promise<void> | null
  pedir(forzar?: boolean): Promise<void>
}

/** Un solo pedido compartido por todos los chips y paneles de clima de la pantalla. */
const useAlmacenClima = create<Almacen>((set, get) => ({
  clima: null,
  termino: false,
  enCurso: null,
  pedir(forzar = false) {
    const actual = get().enCurso
    if (actual && !forzar) return actual
    if (forzar) reiniciarIntentosClima()
    if (!get().clima) set({ clima: leerCacheClima() })
    const p = climaActual(Date.now())
      .then((c) => set((s) => ({ clima: c ?? s.clima, termino: true })))
      .catch(() => set({ termino: true }))
      .finally(() => set({ enCurso: null }))
    set({ enCurso: p, termino: forzar ? false : get().termino })
    return p
  },
}))

/** Clima con estado: cache al instante, refresco al montar (minimo 15 min entre llamadas). */
export function useEstadoClima(): EstadoClima {
  const clima = useAlmacenClima((s) => s.clima)
  const termino = useAlmacenClima((s) => s.termino)
  const pedir = useAlmacenClima((s) => s.pedir)
  useEffect(() => {
    void pedir()
  }, [pedir])
  const reintentar = () => void pedir(true)
  const visible = clima ?? leerCacheClima()
  if (visible) return { estado: 'listo', clima: visible, reintentar }
  return termino
    ? { estado: 'sin-datos', clima: null, reintentar }
    : { estado: 'cargando', clima: null, reintentar }
}

/** Solo el clima (o null mientras carga o si no hay datos). */
export function useClima(): ClimaActual | null {
  return useEstadoClima().clima
}
