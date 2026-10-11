import { create } from 'zustand'
import { repositoriosLocales, type Repositorios } from '../data/repositorios'
import {
  AJUSTES_POR_DEFECTO,
  type Ajustes,
  type CarreraGuardada,
  type Perfil,
  type ResultadoAuth,
} from '../data/tipos'
import { notificacionesMock, type Evento, type Notificacion } from '../mocks/data'
import { obtenerSupabase } from '../lib/supabase'
import { repositoriosSupabase } from '../data/supabaseRepos'

/** Con Supabase configurado se usa el servidor; si no, todo queda en el dispositivo. */
export function repositoriosPorDefecto(): Repositorios {
  const sb = obtenerSupabase()
  return sb ? repositoriosSupabase(sb) : repositoriosLocales()
}

let repos: Repositorios = repositoriosPorDefecto()
let dejarDeEscuchar: (() => void) | null = null

/** Permite cambiar la implementacion de los repositorios (pruebas o Supabase). */
export function usarRepositorios(r: Repositorios) {
  repos = r
}

export interface EstadoApp {
  listo: boolean
  sesion: Perfil | null
  ajustes: Ajustes
  carreras: CarreraGuardada[]
  rutasGuardadas: string[]
  eventosCreados: Evento[]
  inscritos: string[]
  /** Notificaciones de la cuenta (o las de ejemplo sin backend). */
  notificaciones: Notificacion[]
  marcarNotificacionesLeidas(ids: string[]): Promise<void>
  cargar(): Promise<void>
  iniciarSesion(email: string, password: string): Promise<ResultadoAuth>
  registrar(nombre: string, email: string, password: string): Promise<ResultadoAuth>
  recuperarContrasena(email: string): Promise<ResultadoAuth>
  actualizarPerfil(p: Perfil): Promise<void>
  cerrarSesion(): Promise<void>
  entrarConGoogle(): Promise<string | null>
  cambiarContrasena(password: string): Promise<ResultadoAuth>
  actualizarAjustes(c: Partial<Ajustes> | ((a: Ajustes) => Partial<Ajustes>)): Promise<void>
  guardarCarrera(c: CarreraGuardada): Promise<void>
  eliminarCarrera(id: string): Promise<void>
  alternarRutaGuardada(id: string): Promise<void>
  crearEvento(e: Evento): Promise<void>
  alternarInscripcion(id: string): Promise<void>
}

export const useApp = create<EstadoApp>((set, get) => ({
  listo: false,
  sesion: null,
  ajustes: AJUSTES_POR_DEFECTO,
  carreras: [],
  rutasGuardadas: [],
  eventosCreados: [],
  inscritos: [],
  notificaciones: notificacionesMock,

  async cargar() {
    const [sesion, ajustes, carreras, rutasGuardadas, eventosCreados, inscritos] =
      await Promise.all([
        repos.auth.sesionActual(),
        repos.ajustes.leer(),
        repos.carreras.listar(),
        repos.rutas.guardadas(),
        repos.eventos.creados(),
        repos.eventos.inscritos(),
      ])
    const notificaciones =
      repos.notificaciones && sesion
        ? await repos.notificaciones.listar().catch(() => [])
        : notificacionesMock
    set({
      listo: true,
      sesion,
      ajustes,
      carreras,
      rutasGuardadas,
      eventosCreados,
      inscritos,
      notificaciones,
    })
    dejarDeEscuchar?.()
    dejarDeEscuchar =
      repos.auth.alCambiarSesion?.((perfil) => {
        const antes = get().sesion?.email ?? null
        if (perfil?.email === antes && perfil) return set({ sesion: perfil })
        // Otra cuenta o sesion cerrada: se recargan los datos de esa cuenta.
        if (!perfil) return set({ sesion: null })
        void get().cargar()
      }) ?? null
  },

  async marcarNotificacionesLeidas(ids) {
    set((s) => ({
      notificaciones: s.notificaciones.map((n) => (ids.includes(n.id) ? { ...n, leida: true } : n)),
    }))
    if (repos.notificaciones) return repos.notificaciones.marcarLeidas(ids)
    await get().actualizarAjustes((a) => ({
      notificacionesLeidas: Array.from(new Set([...a.notificacionesLeidas, ...ids])),
    }))
  },

  async iniciarSesion(email, password) {
    const r = await repos.auth.iniciarSesion(email, password)
    if (r.ok) {
      set({ sesion: r.perfil })
      await get().cargar()
    }
    return r
  },
  async registrar(nombre, email, password) {
    const r = await repos.auth.registrar(nombre, email, password)
    if (r.ok) set({ sesion: r.perfil })
    return r
  },
  recuperarContrasena: (email) => repos.auth.recuperarContrasena(email),
  async actualizarPerfil(p) {
    set({ sesion: await repos.auth.actualizarPerfil(p) })
  },
  async cerrarSesion() {
    await repos.auth.cerrarSesion()
    set({ sesion: null })
  },

  entrarConGoogle: () => repos.auth.entrarConGoogle(),
  async cambiarContrasena(password) {
    const r = await repos.auth.cambiarContrasena(password)
    if (r.ok) set({ sesion: r.perfil })
    return r
  },

  async actualizarAjustes(c) {
    const cambios = typeof c === 'function' ? c(get().ajustes) : c
    set({ ajustes: { ...get().ajustes, ...cambios } })
    set({ ajustes: await repos.ajustes.actualizar(cambios) })
  },
  async guardarCarrera(c) {
    set({ carreras: await repos.carreras.guardar(c) })
  },
  async eliminarCarrera(id) {
    set({ carreras: await repos.carreras.eliminar(id) })
  },
  async alternarRutaGuardada(id) {
    set({ rutasGuardadas: await repos.rutas.alternarGuardada(id) })
  },
  async crearEvento(e) {
    const eventosCreados = await repos.eventos.crear(e)
    set({ eventosCreados, inscritos: await repos.eventos.inscritos() })
  },
  async alternarInscripcion(id) {
    set({ inscritos: await repos.eventos.alternarInscripcion(id) })
  },
}))
