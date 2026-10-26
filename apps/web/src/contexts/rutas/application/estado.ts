import { create } from 'zustand'
import { rutasMock, type Ruta } from '../../../mocks/data'
import { type Perfil } from '../../../shared/kernel/tipos'
import {
  type ExplorarRepository,
  type NuevaRuta,
  type ResenaRuta,
  type RutasRepository,
} from '../domain/puertos'
import { LocalRutasRepository } from '../infrastructure/rutasLocal'
import { conOficiales } from './oficiales'

export interface RepositoriosRutas {
  rutas: RutasRepository
  /** Rutas de la comunidad; sin este repositorio se usan las de ejemplo. */
  explorar?: ExplorarRepository
}

export interface EnlacesRutas {
  sesion(): Perfil | null
}

let repos: RepositoriosRutas = { rutas: new LocalRutasRepository() }
let enlaces: EnlacesRutas = { sesion: () => null }

export function configurarRutas(r: RepositoriosRutas, e?: EnlacesRutas) {
  repos = r
  if (e) enlaces = e
}

export const repositoriosRutas = () => repos

export interface EstadoRutas {
  rutasGuardadas: string[]
  alternarRutaGuardada(id: string): Promise<void>
  /** Rutas de la comunidad (Supabase) o las de ejemplo. */
  rutas: Ruta[]
  rutasMeGusta: string[]
  /** true si hay servidor de rutas (se puede publicar, dar me gusta y reseñar). */
  rutasSociales: boolean
  refrescarRutas(): Promise<void>
  alternarMeGustaRuta(id: string): Promise<void>
  publicarRuta(r: NuevaRuta): Promise<Ruta>
  rutasCercanas(lat: number, lng: number): Promise<Record<string, number>>
  resenasRuta(id: string): Promise<ResenaRuta[]>
  resenarRuta(id: string, estrellas: number, comentario: string): Promise<void>
  /** Ruta elegida con "Correr esta ruta": al guardar la carrera se registra como corrida. */
  rutaActivaId: string | null
  elegirRutaActiva(id: string | null): void
  /** Registra la carrera como corrida de la ruta activa y la deja de elegir. */
  registrarCorridaActiva(carreraId: string): Promise<void>
  /** Repositorio de rutas (fotos, corredores); undefined sin servidor. */
  explorar(): ExplorarRepository | undefined
}

export const useRutas = create<EstadoRutas>((set, get) => ({
  rutasGuardadas: [],
  rutas: conOficiales(rutasMock),
  rutasMeGusta: [],
  rutasSociales: false,
  rutaActivaId: null,
  elegirRutaActiva: (id) => set({ rutaActivaId: id }),
  explorar: () => repos.explorar,
  async refrescarRutas() {
    if (!repos.explorar) return set({ rutas: conOficiales(rutasMock), rutasSociales: false })
    const [rutas, rutasMeGusta] = await Promise.all([
      repos.explorar.listar().catch(() => null),
      enlaces.sesion() ? repos.explorar.meGusta().catch(() => []) : Promise.resolve([]),
    ])
    // Sin rutas en el servidor (o sin red) se muestran las de ejemplo.
    set({
      rutas: conOficiales(rutas?.length ? rutas : rutasMock),
      rutasMeGusta,
      rutasSociales: rutas !== null,
    })
  },
  async alternarMeGustaRuta(id) {
    if (!repos.explorar) return
    const activo = !get().rutasMeGusta.includes(id)
    const ajustar = (d: number) =>
      set((s) => ({
        rutasMeGusta: d > 0 ? [...s.rutasMeGusta, id] : s.rutasMeGusta.filter((x) => x !== id),
        rutas: s.rutas.map((r) =>
          r.id === id ? { ...r, meGusta: Math.max(0, (r.meGusta ?? 0) + d) } : r
        ),
      }))
    ajustar(activo ? 1 : -1)
    try {
      await repos.explorar.alternarMeGusta(id, activo)
    } catch (e) {
      ajustar(activo ? -1 : 1)
      throw e
    }
  },
  async publicarRuta(r) {
    if (!repos.explorar) throw new Error('Publicar rutas necesita conexión con el servidor')
    const nueva = await repos.explorar.crear(r)
    const autorNombre = enlaces.sesion()?.nombre
    set((s) => ({ rutas: [...s.rutas, { ...nueva, autorNombre }] }))
    return nueva
  },
  rutasCercanas: (lat, lng) => repos.explorar?.cercanas(lat, lng) ?? Promise.resolve({}),
  resenasRuta: (id) => repos.explorar?.resenas(id) ?? Promise.resolve([]),
  async resenarRuta(id, estrellas, comentario) {
    if (!repos.explorar) return
    await repos.explorar.resenar(id, estrellas, comentario)
    await get().refrescarRutas()
  },
  async registrarCorridaActiva(carreraId) {
    const ruta = get().rutaActivaId
    if (!ruta) return
    set({ rutaActivaId: null })
    await repos.explorar?.registrarCorrida(ruta, carreraId).catch(() => undefined)
  },
  async alternarRutaGuardada(id) {
    set({ rutasGuardadas: await repos.rutas.alternarGuardada(id) })
  },
}))
