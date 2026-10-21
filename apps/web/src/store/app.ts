import { create } from 'zustand'
import {
  repositoriosLocales,
  type PublicacionRemota,
  type ComentarioPublicacion,
  type CorredorPublico,
  type Repositorios,
  type NuevaRuta,
  type ResenaRuta,
  type ExplorarRepository,
} from '../data/repositorios'
import {
  AJUSTES_POR_DEFECTO,
  type Ajustes,
  type CarreraGuardada,
  type Perfil,
  type ResultadoAuth,
} from '../data/tipos'
import {
  notificacionesMock,
  rutasMock,
  type Evento,
  type Notificacion,
  type Ruta,
} from '../mocks/data'
import { obtenerSupabase } from '../lib/supabase'
import { calcularRacha, type Racha } from '../lib/rachas'
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
  publicaciones: PublicacionRemota[]
  /** true si el feed viene de Supabase (sin publicaciones de ejemplo). */
  feedSocial: boolean
  refrescarFeed(): Promise<void>
  alternarMeGustaPost(id: string): Promise<void>
  alternarCompartidaPost(id: string): Promise<void>
  subirFotosPost(id: string, archivos: File[]): Promise<void>
  comentarPost(id: string, texto: string): Promise<void>
  comentariosPost(id: string): Promise<ComentarioPublicacion[]>
  /** Escucha cambios del feed en tiempo real; devuelve la baja. */
  suscribirFeed(): () => void
  /** Racha calculada en Supabase (null sin backend: se calcula en el cliente). */
  rachaServidor: Racha | null
  refrescarRacha(): Promise<void>
  siguiendo: string[]
  /** Corredores que siguen al usuario (Supabase). */
  seguidores: number
  alternarSeguir(id: string): Promise<void>
  buscarCorredores(consulta: string): Promise<CorredorPublico[]>
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
  /** Repositorio de rutas (fotos, corredores); undefined sin servidor. */
  explorar(): ExplorarRepository | undefined
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
  publicaciones: [],
  feedSocial: false,
  async refrescarFeed() {
    if (!repos.comunidad || !get().sesion) return set({ feedSocial: false, publicaciones: [] })
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
    if (!repos.comunidad?.suscribir || !get().sesion) return () => undefined
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
  rachaServidor: null,
  siguiendo: [],
  seguidores: 0,
  rutas: rutasMock,
  rutasMeGusta: [],
  rutasSociales: false,
  rutaActivaId: null,
  elegirRutaActiva: (id) => set({ rutaActivaId: id }),
  explorar: () => repos.explorar,

  async refrescarRutas() {
    if (!repos.explorar) return set({ rutas: rutasMock, rutasSociales: false })
    const [rutas, rutasMeGusta] = await Promise.all([
      repos.explorar.listar().catch(() => null),
      get().sesion ? repos.explorar.meGusta().catch(() => []) : Promise.resolve([]),
    ])
    // Sin rutas en el servidor (o sin red) se muestran las de ejemplo.
    set({ rutas: rutas?.length ? rutas : rutasMock, rutasMeGusta, rutasSociales: rutas !== null })
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
    set((s) => ({ rutas: [...s.rutas, { ...nueva, autorNombre: s.sesion?.nombre }] }))
    return nueva
  },
  rutasCercanas: (lat, lng) => repos.explorar?.cercanas(lat, lng) ?? Promise.resolve({}),
  resenasRuta: (id) => repos.explorar?.resenas(id) ?? Promise.resolve([]),
  async resenarRuta(id, estrellas, comentario) {
    if (!repos.explorar) return
    await repos.explorar.resenar(id, estrellas, comentario)
    await get().refrescarRutas()
  },

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
    const [publicaciones, siguiendo] =
      repos.comunidad && sesion
        ? await Promise.all([
            repos.comunidad.publicaciones().catch(() => []),
            repos.comunidad.siguiendo().catch(() => []),
          ])
        : [[], []]
    const rachaServidor =
      repos.rachas && sesion ? await repos.rachas.leer().catch(() => null) : null
    set({
      rachaServidor,
      publicaciones,
      feedSocial: Boolean(repos.comunidad && sesion),
      siguiendo,
      listo: true,
      sesion,
      ajustes,
      carreras,
      rutasGuardadas,
      eventosCreados,
      inscritos,
      notificaciones,
    })
    void get().refrescarRutas()
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

  buscarCorredores: (consulta) => repos.comunidad?.buscar(consulta) ?? Promise.resolve([]),

  async alternarSeguir(id) {
    if (!repos.comunidad) return
    set({ siguiendo: await repos.comunidad.alternarSeguir(id) })
    void get().refrescarFeed()
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
    if ('metaSemanal' in cambios) await get().refrescarRacha()
  },
  async guardarCarrera(c) {
    set({ carreras: await repos.carreras.guardar(c) })
    const ruta = get().rutaActivaId
    if (ruta) {
      set({ rutaActivaId: null })
      await repos.explorar?.registrarCorrida(ruta, c.id).catch(() => undefined)
    }
    await get().refrescarRacha()
  },
  async eliminarCarrera(id) {
    set({ carreras: await repos.carreras.eliminar(id) })
    await get().refrescarRacha()
  },
  async refrescarRacha() {
    if (!repos.rachas || !get().sesion) return
    const r = await repos.rachas.leer().catch(() => null)
    if (r) set({ rachaServidor: r })
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

/**
 * Racha vigente: la del servidor si esta al dia; si no, la calculada con las carreras
 * locales (modo sin conexion o carreras aun en cola de subida).
 */
export function useRacha(): Racha {
  const carreras = useApp((s) => s.carreras)
  const meta = useApp((s) => s.ajustes.metaSemanal)
  const servidor = useApp((s) => s.rachaServidor)
  const local = calcularRacha(carreras, Date.now(), meta)
  if (!servidor) return local
  return servidor.diaria >= local.diaria && servidor.semanal >= local.semanal ? servidor : local
}
