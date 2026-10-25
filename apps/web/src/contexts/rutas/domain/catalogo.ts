import type { CategoriaRuta, Ruta } from '../../../mocks/data'

export const CATEGORIAS_RUTA: [CategoriaRuta, string][] = [
  ['parque', 'Parques'],
  ['sendero', 'Senderos'],
  ['montana', 'Montaña'],
  ['urbana', 'Urbanas'],
  ['pista', 'Pista'],
]

/** Agrega las oficiales que no lleguen del servidor; si llega la misma ruta, prevalece la del servidor. */
export function mezclarOficiales(rutas: Ruta[], oficiales: Ruta[]): Ruta[] {
  const ids = new Set(rutas.map((r) => r.id))
  return [...rutas, ...oficiales.filter((r) => !ids.has(r.id))]
}
