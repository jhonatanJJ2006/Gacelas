import { configurarActividad, useActividad } from '../contexts/actividad/api'
import { configurarAvisos, useAvisos } from '../contexts/avisos/api'
import { configurarComunidad, useComunidad } from '../contexts/comunidad/api'
import { configurarEventos, useEventos } from '../contexts/eventos/api'
import { configurarIdentidad, useIdentidad } from '../contexts/identidad/api'
import { configurarRutas, useRutas } from '../contexts/rutas/api'
import { notificacionesMock } from '../mocks/data'
import { repositoriosPorDefecto, type Repositorios } from './composicion'

/*
 * Coordina los stores de cada contexto: les entrega los repositorios, carga los datos de la
 * cuenta y conecta los efectos que cruzan contextos (sesion, racha, corrida de ruta).
 */

let repos: Repositorios = repositoriosPorDefecto()
let dejarDeEscuchar: (() => void) | null = null

const sesion = () => useIdentidad.getState().sesion

/** Permite cambiar la implementacion de los repositorios (pruebas o Supabase). */
export function usarRepositorios(r: Repositorios) {
  repos = r
  configurarIdentidad(
    { auth: r.auth, ajustes: r.ajustes },
    { recargar: cargar, alCambiarMeta: () => useActividad.getState().refrescarRacha() }
  )
  configurarActividad(
    { carreras: r.carreras, rachas: r.rachas },
    { sesion, alGuardarCarrera: (c) => useRutas.getState().registrarCorridaActiva(c.id) }
  )
  configurarRutas({ rutas: r.rutas, explorar: r.explorar }, { sesion })
  configurarEventos({ eventos: r.eventos })
  configurarComunidad({ comunidad: r.comunidad }, { sesion })
  configurarAvisos(
    { notificaciones: r.notificaciones },
    {
      marcarEnAjustes: (ids) =>
        useIdentidad.getState().actualizarAjustes((a) => ({
          notificacionesLeidas: Array.from(new Set([...a.notificacionesLeidas, ...ids])),
        })),
    }
  )
}

usarRepositorios(repos)

/** Carga los datos de la cuenta en todos los contextos y escucha cambios de sesion. */
export async function cargar(): Promise<void> {
  const [sesionActual, ajustes, carreras, rutasGuardadas, eventosCreados, inscritos] =
    await Promise.all([
      repos.auth.sesionActual(),
      repos.ajustes.leer(),
      repos.carreras.listar(),
      repos.rutas.guardadas(),
      repos.eventos.creados(),
      repos.eventos.inscritos(),
    ])
  const notificaciones =
    repos.notificaciones && sesionActual
      ? await repos.notificaciones.listar().catch(() => [])
      : notificacionesMock
  const [publicaciones, siguiendo] =
    repos.comunidad && sesionActual
      ? await Promise.all([
          repos.comunidad.publicaciones().catch(() => []),
          repos.comunidad.siguiendo().catch(() => []),
        ])
      : [[], []]
  const rachaServidor =
    repos.rachas && sesionActual ? await repos.rachas.leer().catch(() => null) : null
  useActividad.setState({ rachaServidor, carreras })
  useComunidad.setState({
    publicaciones,
    feedSocial: Boolean(repos.comunidad && sesionActual),
    siguiendo,
  })
  useRutas.setState({ rutasGuardadas })
  useEventos.setState({ eventosCreados, inscritos })
  useAvisos.setState({ notificaciones })
  useIdentidad.setState({ listo: true, sesion: sesionActual, ajustes })
  void useRutas.getState().refrescarRutas()
  dejarDeEscuchar?.()
  dejarDeEscuchar =
    repos.auth.alCambiarSesion?.((perfil) => {
      const antes = sesion()?.email ?? null
      if (perfil?.email === antes && perfil) return useIdentidad.setState({ sesion: perfil })
      // Otra cuenta o sesion cerrada: se recargan los datos de esa cuenta.
      if (!perfil) return useIdentidad.setState({ sesion: null })
      void cargar()
    }) ?? null
}

const stores = [useIdentidad, useActividad, useRutas, useEventos, useComunidad, useAvisos] as const

/** Estado combinado de todos los contextos (pruebas y vista previa). */
export function leerEstado() {
  return {
    ...useIdentidad.getState(),
    ...useActividad.getState(),
    ...useRutas.getState(),
    ...useEventos.getState(),
    ...useComunidad.getState(),
    ...useAvisos.getState(),
  }
}

export type EstadoCombinado = ReturnType<typeof leerEstado>

/** Reparte un cambio de estado entre los stores que tienen cada clave. */
export function fijarEstado(
  cambios: Partial<EstadoCombinado> | ((s: EstadoCombinado) => Partial<EstadoCombinado>)
) {
  const c = typeof cambios === 'function' ? cambios(leerEstado()) : cambios
  for (const store of stores) {
    const propio = store.getState() as object
    const parte = Object.fromEntries(Object.entries(c).filter(([k]) => k in propio))
    if (Object.keys(parte).length) store.setState(parte as never)
  }
}

/** Escucha cambios en cualquiera de los stores. */
export function suscribirEstado(fn: (s: EstadoCombinado) => void): () => void {
  const bajas = stores.map((s) => s.subscribe(() => fn(leerEstado())))
  return () => bajas.forEach((b) => b())
}
