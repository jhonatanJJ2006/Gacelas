import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { repositoriosLocales, type Repositorios } from '../app/composicion'
import { cargar, fijarEstado, leerEstado, usarRepositorios } from '../app/estado'
import { useRacha } from '../contexts/actividad/ui/useRacha'
import { conOficiales } from '../contexts/rutas/api'
import { notificacionesMock, rutasMock, type Notificacion } from '../mocks/data'
import type { Racha } from '../shared/kernel/rachas'
import type { CarreraGuardada, Perfil } from '../shared/kernel/tipos'

/*
 * Caracterizacion del estado global de la app antes de dividirlo por contexto: fija como
 * se coordinan sesion, ajustes, carreras, rutas, comunidad y avisos.
 */

const estado = () => leerEstado()
const configurar = (r: Repositorios) => usarRepositorios(r)

const perfil = (email: string): Perfil => ({
  email,
  nombre: 'Ana',
  username: 'ana',
  bio: '',
  ubicacion: '',
  edad: '',
  genero: '',
  foto: null,
})

const racha = (diaria: number, semanal: number): Racha => ({
  diaria,
  mejorDiaria: diaria,
  ultimoDiaMs: null,
  semanal,
  mejorSemanal: semanal,
  metaSemanal: 3,
  carrerasSemana: 0,
})

const carrera = (id: string, inicioMs = Date.now()): CarreraGuardada => ({
  id,
  titulo: 'Carrera',
  nota: '',
  publica: true,
  inicioMs,
  segundos: 600,
  distanciaKm: 2,
  desnivelM: 0,
  parciales: [],
  puntos: [],
})

const publicacion = (id: string) => ({
  id,
  autorId: 'x',
  autor: 'X',
  titulo: 't',
  inicioMs: 1,
  distanciaKm: 1,
  segundos: 60,
  likes: 3,
  comentarios: 0,
  puntos: [],
  compartidos: 1,
})

function conBackend(sesion: Perfil | null = perfil('ana@x.ec')) {
  const base = repositoriosLocales()
  let alCambiar: ((p: Perfil | null) => void) | null = null
  const notif: Notificacion = { ...notificacionesMock[0], id: 'n-real', leida: false }
  const r = {
    ...base,
    auth: {
      ...base.auth,
      sesionActual: vi.fn(async () => sesion),
      iniciarSesion: vi.fn(async (email: string) => ({ ok: true as const, perfil: perfil(email) })),
      alCambiarSesion: vi.fn((cb: (p: Perfil | null) => void) => {
        alCambiar = cb
        return vi.fn()
      }),
      cerrarSesion: base.auth.cerrarSesion.bind(base.auth),
      registrar: base.auth.registrar.bind(base.auth),
      recuperarContrasena: base.auth.recuperarContrasena.bind(base.auth),
      actualizarPerfil: base.auth.actualizarPerfil.bind(base.auth),
      entrarConGoogle: base.auth.entrarConGoogle.bind(base.auth),
      cambiarContrasena: base.auth.cambiarContrasena.bind(base.auth),
    },
    notificaciones: { listar: vi.fn(async () => [notif]), marcarLeidas: vi.fn(async () => {}) },
    comunidad: {
      buscar: vi.fn(async () => []),
      publicaciones: vi.fn(async () => [publicacion('p1')]),
      alternarMeGusta: vi.fn(async () => {}),
      comentarios: vi.fn(async () => []),
      comentar: vi.fn(async () => {}),
      siguiendo: vi.fn(async () => ['s1']),
      alternarSeguir: vi.fn(async () => ['s1', 's2']),
      seguidores: vi.fn(async () => 4),
      suscribir: vi.fn((alCambiar: () => void) => {
        void alCambiar
        return vi.fn()
      }),
    },
    rachas: { leer: vi.fn(async () => racha(2, 1)) },
    explorar: {
      listar: vi.fn(async () => [rutasMock[0]]),
      cercanas: vi.fn(async () => ({})),
      crear: vi.fn(async () => ({ ...rutasMock[0], id: 'nueva' })),
      meGusta: vi.fn(async () => ['r1']),
      alternarMeGusta: vi.fn(async () => {}),
      resenas: vi.fn(async () => []),
      resenar: vi.fn(async () => {}),
      fotos: vi.fn(async () => []),
      subirFoto: vi.fn(async () => {}),
      corredores: vi.fn(async () => ({ lista: [], total: 0 })),
      registrarCorrida: vi.fn(async () => {}),
    },
  }
  return { r, cambiarSesion: (p: Perfil | null) => alCambiar?.(p) }
}

const esperar = () => new Promise((ok) => setTimeout(ok, 0))

describe('caracterizacion del estado de la app', () => {
  beforeEach(() => {
    localStorage.clear()
    fijarEstado({
      sesion: null,
      carreras: [],
      publicaciones: [],
      siguiendo: [],
      seguidores: 0,
      rutaActivaId: null,
      rachaServidor: null,
      notificaciones: notificacionesMock,
    })
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('sin backend ni sesion usa los datos de ejemplo', async () => {
    configurar(repositoriosLocales())
    await cargar()
    await esperar()
    expect(estado()).toMatchObject({
      listo: true,
      sesion: null,
      notificaciones: notificacionesMock,
      publicaciones: [],
      feedSocial: false,
      rachaServidor: null,
      rutasSociales: false,
    })
    expect(estado().rutas).toEqual(conOficiales(rutasMock))
    expect(estado().explorar()).toBeUndefined()
  })

  it('con backend y sesion carga notificaciones, feed, racha y rutas del servidor', async () => {
    const { r } = conBackend()
    configurar(r)
    await cargar()
    await esperar()
    expect(estado().notificaciones.map((n) => n.id)).toEqual(['n-real'])
    expect(estado().publicaciones.map((p) => p.id)).toEqual(['p1'])
    expect(estado()).toMatchObject({
      feedSocial: true,
      siguiendo: ['s1'],
      rachaServidor: racha(2, 1),
      rutasMeGusta: ['r1'],
      rutasSociales: true,
    })
    expect(estado().rutas).toEqual(conOficiales([rutasMock[0]]))
  })

  it('reacciona a cambios de sesion externos', async () => {
    const { r, cambiarSesion } = conBackend()
    configurar(r)
    await cargar()
    expect(r.auth.sesionActual).toHaveBeenCalledTimes(1)
    cambiarSesion({ ...perfil('ana@x.ec'), nombre: 'Ana B' })
    expect(estado().sesion?.nombre).toBe('Ana B')
    expect(r.auth.sesionActual).toHaveBeenCalledTimes(1)
    cambiarSesion(perfil('otra@x.ec'))
    await esperar()
    expect(r.auth.sesionActual).toHaveBeenCalledTimes(2)
    cambiarSesion(null)
    expect(estado().sesion).toBeNull()
  })

  it('iniciar sesion recarga los datos de la cuenta', async () => {
    const { r } = conBackend(null)
    configurar(r)
    const res = await estado().iniciarSesion('b@x.ec', 'x')
    expect(res.ok).toBe(true)
    expect(r.auth.sesionActual).toHaveBeenCalledTimes(1)
    expect(estado().listo).toBe(true)
  })

  it('guardar una carrera registra la ruta activa y refresca la racha', async () => {
    const { r } = conBackend()
    configurar(r)
    await cargar()
    r.rachas.leer.mockResolvedValueOnce(racha(5, 3))
    estado().elegirRutaActiva('r9')
    await estado().guardarCarrera(carrera('c1'))
    expect(r.explorar.registrarCorrida).toHaveBeenCalledWith('r9', 'c1')
    expect(estado().rutaActivaId).toBeNull()
    expect(estado().rachaServidor).toEqual(racha(5, 3))
    expect(estado().carreras.map((c) => c.id)).toEqual(['c1'])
    await estado().eliminarCarrera('c1')
    expect(estado().carreras).toEqual([])
    expect(r.rachas.leer).toHaveBeenCalledTimes(3)
  })

  it('cambiar la meta semanal refresca la racha; otros ajustes no', async () => {
    const { r } = conBackend()
    configurar(r)
    await cargar()
    await estado().actualizarAjustes({ unidadesMetricas: false })
    expect(r.rachas.leer).toHaveBeenCalledTimes(1)
    await estado().actualizarAjustes({ metaSemanal: 4 })
    expect(r.rachas.leer).toHaveBeenCalledTimes(2)
    expect(estado().ajustes.metaSemanal).toBe(4)
  })

  it('marcar avisos leidos usa el repositorio o, sin el, los ajustes', async () => {
    const { r } = conBackend()
    configurar(r)
    await cargar()
    await estado().marcarNotificacionesLeidas(['n-real'])
    expect(r.notificaciones.marcarLeidas).toHaveBeenCalledWith(['n-real'])
    expect(estado().notificaciones[0].leida).toBe(true)

    configurar(repositoriosLocales())
    await cargar()
    const id = notificacionesMock[0].id
    await estado().marcarNotificacionesLeidas([id])
    expect(estado().ajustes.notificacionesLeidas).toContain(id)
  })

  it('los me gusta del feed son optimistas y se revierten si fallan', async () => {
    const { r } = conBackend()
    configurar(r)
    await cargar()
    await estado().alternarMeGustaPost('p1')
    expect(estado().publicaciones[0]).toMatchObject({ meGusta: true, likes: 4 })
    r.comunidad.alternarMeGusta.mockRejectedValueOnce(new Error('red'))
    await expect(estado().alternarMeGustaPost('p1')).rejects.toThrow('red')
    expect(estado().publicaciones[0]).toMatchObject({ meGusta: true, likes: 4 })
    await estado().comentarPost('p1', 'hola')
    expect(estado().publicaciones[0].comentarios).toBe(1)
  })

  it('seguir a alguien actualiza la lista y refresca el feed', async () => {
    const { r } = conBackend()
    configurar(r)
    await cargar()
    await estado().alternarSeguir('s2')
    expect(estado().siguiendo).toEqual(['s1', 's2'])
    await esperar()
    expect(estado().seguidores).toBe(4)
    expect(r.comunidad.publicaciones).toHaveBeenCalledTimes(2)
  })

  it('el feed en tiempo real agrupa cambios en 400 ms', async () => {
    const { r } = conBackend()
    configurar(r)
    await cargar()
    vi.useFakeTimers()
    const baja = estado().suscribirFeed()
    const aviso = r.comunidad.suscribir.mock.calls[0][0]
    aviso()
    aviso()
    vi.advanceTimersByTime(399)
    expect(r.comunidad.publicaciones).toHaveBeenCalledTimes(1)
    vi.advanceTimersByTime(1)
    expect(r.comunidad.publicaciones).toHaveBeenCalledTimes(2)
    baja()
  })

  it('publicar rutas necesita servidor y agrega la ruta con su autor', async () => {
    configurar(repositoriosLocales())
    await expect(estado().publicarRuta({} as never)).rejects.toThrow('servidor')
    const { r } = conBackend()
    configurar(r)
    await cargar()
    await esperar()
    const nueva = await estado().publicarRuta({} as never)
    expect(nueva.id).toBe('nueva')
    expect(estado().rutas.find((x) => x.id === 'nueva')).toMatchObject({
      id: 'nueva',
      autorNombre: 'Ana',
    })
    await estado().alternarMeGustaRuta('nueva')
    expect(estado().rutasMeGusta).toContain('nueva')
  })

  it('la racha vigente prefiere la del servidor solo si esta al dia', () => {
    fijarEstado({ carreras: [carrera('a')], rachaServidor: racha(0, 0) })
    const { result, rerender } = renderHook(() => useRacha())
    expect(result.current.diaria).toBe(1)
    act(() => fijarEstado({ rachaServidor: racha(9, 9) }))
    rerender()
    expect(result.current.diaria).toBe(9)
  })
})
