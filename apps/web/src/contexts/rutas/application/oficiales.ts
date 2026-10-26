import type { Ruta } from '../../../mocks/data'
import { mezclarOficiales } from '../domain/catalogo'
import { RUTAS_LOJA } from '../infrastructure/rutasLojaOsm'

/** Rutas oficiales de Loja incluidas en la app (antes de aplicar 022 o sin red). */
export const conOficiales = (rutas: Ruta[]) => mezclarOficiales(rutas, RUTAS_LOJA)
