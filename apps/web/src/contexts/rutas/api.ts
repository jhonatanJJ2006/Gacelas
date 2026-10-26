// Fachada publica del contexto Rutas (rutas oficiales de Loja).
import { RUTAS_LOJA } from './infrastructure/rutasLojaOsm'

export * from './application/estado'
export { conOficiales } from './application/oficiales'
export { CATEGORIAS_RUTA, mezclarOficiales } from './domain/catalogo'
export * from './domain/elevacion'
export type * from './domain/puertos'
export * from './domain/rutas'
export { LocalRutasRepository } from './infrastructure/rutasLocal'
export { SupabaseExplorarRepository, SupabaseRutasRepository } from './infrastructure/rutasSupabase'
export * from './infrastructure/vistaPrevia'
export { RUTAS_LOJA }
