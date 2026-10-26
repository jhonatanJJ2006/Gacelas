import { create } from 'zustand'
import { type Evento } from '../../../mocks/data'
import { type EventosRepository } from '../domain/puertos'
import { LocalEventosRepository } from '../infrastructure/eventosLocal'

let repos: { eventos: EventosRepository } = { eventos: new LocalEventosRepository() }

export function configurarEventos(r: { eventos: EventosRepository }) {
  repos = r
}

export const repositoriosEventos = () => repos

export interface EstadoEventos {
  eventosCreados: Evento[]
  inscritos: string[]
  crearEvento(e: Evento): Promise<void>
  alternarInscripcion(id: string): Promise<void>
}

export const useEventos = create<EstadoEventos>((set) => ({
  eventosCreados: [],
  inscritos: [],
  async crearEvento(e) {
    const eventosCreados = await repos.eventos.crear(e)
    set({ eventosCreados, inscritos: await repos.eventos.inscritos() })
  },
  async alternarInscripcion(id) {
    set({ inscritos: await repos.eventos.alternarInscripcion(id) })
  },
}))
