import { create } from 'zustand'
import {
  type ComentarioPublicacion,
  type CorredorPublico,
  type PublicacionRemota,
} from '../../../shared/kernel/comunidad'
import { type Perfil } from '../../../shared/kernel/tipos'
import { type ComunidadRepository } from '../domain/puertos'

export interface EnlacesComunidad {
  sesion(): Perfil | null
}

let repos: { comunidad?: ComunidadRepository } = {}
let enlaces: EnlacesComunidad = { sesion: () => null }

export function configurarComunidad(r: { comunidad?: ComunidadRepository }, e?: EnlacesComunidad) {
  repos = r
  if (e) enlaces = e
}

export const repositoriosComunidad = () => repos

export interface EstadoComunidad {
  publicaciones: PublicacionRemota[]
  /** true si el feed viene de Supabase (sin publicaciones de ejemplo). */
  feedSocial: boolean
  siguiendo: string[]
  /** Corredores que siguen al usuario (Supabase). */
  seguidores: number
  refrescarFeed(): Promise<void>
  alternarMeGustaPost(id: string): Promise<void>
  alternarCompartidaPost(id: string): Promise<void>
  subirFotosPost(id: string, archivos: File[]): Promise<void>
  comentarPost(id: string, texto: string): Promise<void>
  comentariosPost(id: string): Promise<ComentarioPublicacion[]>
  /** Escucha cambios del feed en tiempo real; devuelve la baja. */
  suscribirFeed(): () => void
  alternarSeguir(id: string): Promise<void>
  buscarCorredores(consulta: string): Promise<CorredorPublico[]>
}

export const useComunidad = create<EstadoComunidad>((set, get) => ({
  publicaciones: [],
  feedSocial: false,
  siguiendo: [],
  seguidores: 0,
  async refrescarFeed() {
    if (!repos.comunidad || !enlaces.sesion()) return set({ feedSocial: false, publicaciones: [] })
    const [publicaciones, siguiendo, seguidores] = await Promise.all([
      repos.comunidad.publicaciones().catch(() => get().publicaciones),
      repos.comunidad.siguiendo().catch(() => get().siguiendo),
      repos.comunidad.seguidores?.().catch(() => get().seguidores) ?? get().seguidores,
    ])
    set({ publicaciones, siguiendo, seguidores, feedSocial: true })
  },
  async alternarCompartidaPost(id) {
    if (!repos.comunidad?.alternarCompartida) return
    const antes = get().publicaciones
    const p = antes.find((x) => x.id === id)
    if (!p) return
    const activo = !p.compartida
    set({
      publicaciones: antes.map((x) =>
        x.id === id
          ? {
              ...x,
              compartida: activo,
              compartidos: Math.max(0, (x.compartidos ?? 0) + (activo ? 1 : -1)),
            }
          : x
      ),
    })
    try {
      await repos.comunidad.alternarCompartida(id, activo)
    } catch (e) {
      set({ publicaciones: antes })
      throw e
    }
  },
  async subirFotosPost(id, archivos) {
    if (!repos.comunidad?.subirFotos) return
    const urls = await repos.comunidad.subirFotos(id, archivos)
    set({
      publicaciones: get().publicaciones.map((x) =>
        x.id === id ? { ...x, imagenes: [...(x.imagenes ?? []), ...urls] } : x
      ),
    })
  },
  async alternarMeGustaPost(id) {
    if (!repos.comunidad) return
    const antes = get().publicaciones
    const p = antes.find((x) => x.id === id)
    if (!p) return
    const activo = !p.meGusta
    set({
      publicaciones: antes.map((x) =>
        x.id === id ? { ...x, meGusta: activo, likes: Math.max(0, x.likes + (activo ? 1 : -1)) } : x
      ),
    })
    try {
      await repos.comunidad.alternarMeGusta(id, activo)
    } catch (e) {
      set({ publicaciones: antes })
      throw e
    }
  },
  async comentarPost(id, texto) {
    if (!repos.comunidad) return
    await repos.comunidad.comentar(id, texto)
    set({
      publicaciones: get().publicaciones.map((x) =>
        x.id === id ? { ...x, comentarios: x.comentarios + 1 } : x
      ),
    })
  },
  comentariosPost: (id) => repos.comunidad?.comentarios(id) ?? Promise.resolve([]),
  suscribirFeed() {
    if (!repos.comunidad?.suscribir || !enlaces.sesion()) return () => undefined
    let t: ReturnType<typeof setTimeout> | undefined
    const baja = repos.comunidad.suscribir(() => {
      clearTimeout(t)
      t = setTimeout(() => void get().refrescarFeed(), 400)
    })
    return () => {
      clearTimeout(t)
      baja()
    }
  },
  buscarCorredores: (consulta) => repos.comunidad?.buscar(consulta) ?? Promise.resolve([]),
  async alternarSeguir(id) {
    if (!repos.comunidad) return
    set({ siguiendo: await repos.comunidad.alternarSeguir(id) })
    void get().refrescarFeed()
  },
}))
