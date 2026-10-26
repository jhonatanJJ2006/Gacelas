// Fachada publica del contexto Rutas (rutas oficiales de Loja).
import type { Ruta } from '../../mocks/data'
import { mezclarOficiales } from './domain/catalogo'
import { RUTAS_LOJA } from './infrastructure/rutasLojaOsm'

export { CATEGORIAS_RUTA, mezclarOficiales } from './domain/catalogo'
export { RUTAS_LOJA }

/** Rutas oficiales de Loja incluidas en la app (antes de aplicar 022 o sin red). */
export const conOficiales = (rutas: Ruta[]) => mezclarOficiales(rutas, RUTAS_LOJA)
export * from './domain/rutas'
export * from './domain/elevacion'
