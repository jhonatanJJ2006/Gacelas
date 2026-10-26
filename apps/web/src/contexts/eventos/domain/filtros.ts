import { type Evento } from '../../../mocks/data'

export type PestanaEventos = 'Próximos' | 'Mis eventos' | 'Pasados'

export const PESTANAS_EVENTOS: PestanaEventos[] = ['Próximos', 'Mis eventos', 'Pasados']

export function filtrarEventos(
  eventos: Evento[],
  pestana: PestanaEventos,
  inscritos: string[]
): Evento[] {
  if (pestana === 'Pasados') return eventos.filter((e) => e.pasado)
  if (pestana === 'Mis eventos')
    return eventos.filter((e) => inscritos.includes(e.id) || e.creadoPorUsuario)
  return eventos.filter((e) => !e.pasado)
}
