import { type Ruta } from '../../../mocks/data'

export type PestanaRutas = 'Explorar' | 'Mis Rutas' | 'Guardadas'

export const PESTANAS_RUTAS: PestanaRutas[] = ['Explorar', 'Mis Rutas', 'Guardadas']

export function filtrarRutas(
  rutas: Ruta[],
  pestana: PestanaRutas,
  guardadas: string[],
  usuarioId: string,
  consulta = ''
): Ruta[] {
  const base =
    pestana === 'Mis Rutas'
      ? rutas.filter((r) => r.autorId === usuarioId || r.propia)
      : pestana === 'Guardadas'
        ? rutas.filter((r) => guardadas.includes(r.id))
        : rutas
  const q = consulta.trim().toLowerCase()
  return q
    ? base.filter(
        (r) => r.nombre.toLowerCase().includes(q) || r.descripcion.toLowerCase().includes(q)
      )
    : base
}
