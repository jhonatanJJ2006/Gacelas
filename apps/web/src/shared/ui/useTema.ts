import { useSyncExternalStore } from 'react'
import { suscribirTema, temaActual, type TemaResuelto } from '../lib/tema'

/** Tema resuelto (claro u oscuro) y reactivo: para mapas, graficos y otros colores dibujados en JS. */
export function useTemaResuelto(): TemaResuelto {
  return useSyncExternalStore(suscribirTema, temaActual, () => 'oscuro' as TemaResuelto)
}
