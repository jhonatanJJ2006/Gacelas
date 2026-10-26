import { create } from 'zustand'
import {
  AJUSTES_POR_DEFECTO,
  type Ajustes,
  type Perfil,
  type ResultadoAuth,
} from '../../../shared/kernel/tipos'
import { type AjustesRepository, type AuthRepository } from '../domain/puertos'
import { LocalAjustesRepository, LocalAuthRepository } from '../infrastructure/local'

/** Repositorios de identidad; la raiz de composicion los reemplaza al arrancar. */
export interface RepositoriosIdentidad {
  auth: AuthRepository
  ajustes: AjustesRepository
}

/** Efectos en otros contextos que coordina la raiz de composicion. */
export interface EnlacesIdentidad {
  /** Tras iniciar sesion se recargan los datos de la cuenta. */
  recargar(): Promise<void>
  /** Al cambiar la meta semanal se refresca la racha. */
  alCambiarMeta(): Promise<void>
}

let repos: RepositoriosIdentidad = {
  auth: new LocalAuthRepository(),
  ajustes: new LocalAjustesRepository(),
}
let enlaces: EnlacesIdentidad = {
  recargar: async () => undefined,
  alCambiarMeta: async () => undefined,
}

export function configurarIdentidad(r: RepositoriosIdentidad, e?: EnlacesIdentidad) {
  repos = r
  if (e) enlaces = e
}

export const repositoriosIdentidad = () => repos

export interface EstadoIdentidad {
  listo: boolean
  sesion: Perfil | null
  ajustes: Ajustes
  iniciarSesion(email: string, password: string): Promise<ResultadoAuth>
  registrar(nombre: string, email: string, password: string): Promise<ResultadoAuth>
  recuperarContrasena(email: string): Promise<ResultadoAuth>
  actualizarPerfil(p: Perfil): Promise<void>
  cerrarSesion(): Promise<void>
  entrarConGoogle(): Promise<string | null>
  cambiarContrasena(password: string): Promise<ResultadoAuth>
  actualizarAjustes(c: Partial<Ajustes> | ((a: Ajustes) => Partial<Ajustes>)): Promise<void>
}

export const useIdentidad = create<EstadoIdentidad>((set, get) => ({
  listo: false,
  sesion: null,
  ajustes: AJUSTES_POR_DEFECTO,
  async iniciarSesion(email, password) {
    const r = await repos.auth.iniciarSesion(email, password)
    if (r.ok) {
      set({ sesion: r.perfil })
      await enlaces.recargar()
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
    if ('metaSemanal' in cambios) await enlaces.alCambiarMeta()
  },
}))
