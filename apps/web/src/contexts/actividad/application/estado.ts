import { create } from 'zustand'
import { type Racha } from '../../../shared/kernel/rachas'
import { type CarreraGuardada, type Perfil } from '../../../shared/kernel/tipos'
import { type CarrerasRepository, type RachasRepository } from '../domain/puertos'
import { LocalCarrerasRepository } from '../infrastructure/carrerasLocal'

export interface RepositoriosActividad {
  carreras: CarrerasRepository
  rachas?: RachasRepository
}

export interface EnlacesActividad {
  sesion(): Perfil | null
  /** Registra la carrera como corrida de la ruta elegida, si la hay. */
  alGuardarCarrera(c: CarreraGuardada): Promise<void>
}

let repos: RepositoriosActividad = { carreras: new LocalCarrerasRepository() }
let enlaces: EnlacesActividad = { sesion: () => null, alGuardarCarrera: async () => undefined }

export function configurarActividad(r: RepositoriosActividad, e?: EnlacesActividad) {
  repos = r
  if (e) enlaces = e
}

export const repositoriosActividad = () => repos

export interface EstadoActividad {
  carreras: CarreraGuardada[]
  /** Racha calculada en Supabase (null sin backend: se calcula en el cliente). */
  rachaServidor: Racha | null
  refrescarRacha(): Promise<void>
  guardarCarrera(c: CarreraGuardada): Promise<void>
  eliminarCarrera(id: string): Promise<void>
}

export const useActividad = create<EstadoActividad>((set, get) => ({
  carreras: [],
  rachaServidor: null,
  async guardarCarrera(c) {
    set({ carreras: await repos.carreras.guardar(c) })
    await enlaces.alGuardarCarrera(c)
    await get().refrescarRacha()
  },
  async eliminarCarrera(id) {
    set({ carreras: await repos.carreras.eliminar(id) })
    await get().refrescarRacha()
  },
  async refrescarRacha() {
    if (!repos.rachas || !enlaces.sesion()) return
    const r = await repos.rachas.leer().catch(() => null)
    if (r) set({ rachaServidor: r })
  },
}))
