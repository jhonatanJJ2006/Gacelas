import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Repositorios } from '../app/composicion'
import { repositoriosSupabase } from '../app/composicion'
import { cargar, usarRepositorios } from '../app/estado'
import { eventoDesdeFila } from '../contexts/eventos/infrastructure/eventosSupabase'
import { useIdentidad } from '../contexts/identidad/application/estado'
import { perfilDesdeFila, traducirError } from '../contexts/identidad/infrastructure/authSupabase'
import type { CarreraGuardada } from '../shared/kernel/tipos'
import { crearFakeSupabase, type FakeSupabase } from '../test/fakeSupabase'

const carrera = (id: string, inicioMs = 1_700_000_000_000): CarreraGuardada => ({
  id,
  titulo: `Carrera ${id}`,
  nota: 'suave',
  publica: true,
  inicioMs,
  segundos: 1500,
  distanciaKm: 5.25,
  desnivelM: 40,
  parciales: [{ numero: 1, distanciaKm: 1, segundos: 300, manual: false }],
  puntos: [{ lat: -3.99, lng: -79.2, tiempoMs: inicioMs, precisionM: 5, altitudM: 2100 }],
})

let fake: FakeSupabase
let repos: Repositorios

async function registrarYEntrar() {
  const r = await repos.auth.registrar('Jhona Runner', 'Jhona@UTPL.edu.ec', 'gacela123')
  expect(r.ok).toBe(true)
}

beforeEach(() => {
  localStorage.clear()
  fake = crearFakeSupabase()
  repos = repositoriosSupabase(fake.cliente)
})

describe('SupabaseAuthRepository', () => {
  it('registra, lee el perfil del trigger y mantiene la sesion', async () => {
    await registrarYEntrar()
    const p = await repos.auth.sesionActual()
    expect(p).toMatchObject({ email: 'jhona@utpl.edu.ec', nombre: 'Jhona Runner' })
    expect(fake.tablas.profiles).toHaveLength(1)
  })

  it('con confirmacion de correo pide revisar el correo', async () => {
    fake.confirmarCorreo = true
    const r = await repos.auth.registrar('Ana', 'ana@test.ec', 'gacela123')
    expect(r).toEqual({ ok: false, error: expect.stringContaining('confirmar') })
  })

  it('valida y traduce errores de inicio de sesion', async () => {
    expect(await repos.auth.iniciarSesion('malo', 'x')).toMatchObject({ ok: false })
    expect(await repos.auth.iniciarSesion('a@b.ec', '')).toMatchObject({
      error: 'Ingresa tu contraseña',
    })
    expect(await repos.auth.iniciarSesion('a@b.ec', 'gacela123')).toEqual({
      ok: false,
      error: 'Correo o contraseña incorrectos',
    })
    await registrarYEntrar()
    await repos.auth.cerrarSesion()
    expect(await repos.auth.sesionActual()).toBeNull()
    const r = await repos.auth.iniciarSesion('jhona@utpl.edu.ec', 'gacela123')
    expect(r.ok).toBe(true)
    expect(await repos.auth.registrar('Otro', 'jhona@utpl.edu.ec', 'gacela123')).toEqual({
      ok: false,
      error: 'Ya existe una cuenta con ese correo',
    })
  })

  it('recupera y cambia la contrasena', async () => {
    expect((await repos.auth.recuperarContrasena('x')).ok).toBe(false)
    expect((await repos.auth.recuperarContrasena('a@b.ec')).ok).toBe(true)
    expect(fake.llamadas).toContain('recover:a@b.ec')
    expect((await repos.auth.cambiarContrasena('nueva1234')).ok).toBe(false)
    await registrarYEntrar()
    expect((await repos.auth.cambiarContrasena('nueva1234')).ok).toBe(true)
    expect(fake.usuarios[0].password).toBe('nueva1234')
  })

  it('actualiza el perfil y abre Google con redireccion a /inicio', async () => {
    await registrarYEntrar()
    const p = (await repos.auth.sesionActual())!
    await repos.auth.actualizarPerfil({ ...p, bio: 'Fondista', ubicacion: 'Vilcabamba' })
    expect(fake.tablas.profiles[0]).toMatchObject({ bio: 'Fondista', location: 'Vilcabamba' })
    expect(await repos.auth.entrarConGoogle()).toBeNull()
    expect(fake.oauth).toMatchObject({
      provider: 'google',
      options: { redirectTo: `${window.location.origin}/inicio` },
    })
  })

  it('si Google no esta activo en el servidor lo explica', async () => {
    const { configSupabase } = await import('../shared/supabase/client')
    const antes = { ...configSupabase }
    configSupabase.url = 'https://x.supabase.co'
    const f = vi.fn(async () => new Response(JSON.stringify({ external: { google: false } })))
    vi.stubGlobal('fetch', f)
    expect(await repos.auth.entrarConGoogle()).toMatch(/no está activado/)
    f.mockImplementation(async () => {
      throw new Error('red')
    })
    expect(await repos.auth.entrarConGoogle()).toBeNull()
    vi.unstubAllGlobals()
    Object.assign(configSupabase, antes)
  })

  it('avisa de cambios de sesion', async () => {
    const cb = vi.fn()
    const quitar = repos.auth.alCambiarSesion!(cb)
    await registrarYEntrar()
    await repos.auth.cerrarSesion()
    await new Promise((r) => setTimeout(r, 10))
    expect(cb).toHaveBeenCalledWith(null)
    expect(cb).toHaveBeenCalledWith(expect.objectContaining({ email: 'jhona@utpl.edu.ec' }))
    quitar()
  })
})

describe('SupabaseCarrerasRepository', () => {
  it('sin sesion usa la copia local', async () => {
    await repos.carreras.guardar(carrera('a'))
    expect(await repos.carreras.listar()).toHaveLength(1)
  })

  it('sube la carrera con sus puntos y la vuelve a leer', async () => {
    await registrarYEntrar()
    await repos.carreras.guardar(carrera('a'))
    expect(fake.tablas.activities[0]).toMatchObject({ client_id: 'a', distance_meters: 5250 })
    expect(fake.tablas.activity_tracks).toHaveLength(1)
    localStorage.clear()
    const lista = await repos.carreras.listar()
    expect(lista[0]).toMatchObject({ id: 'a', distanciaKm: 5.25, nota: 'suave' })
    expect(lista[0].puntos).toHaveLength(1)
    await repos.carreras.eliminar('a')
    expect(fake.tablas.activities).toHaveLength(0)
  })

  it('sin red encola y sincroniza despues', async () => {
    await registrarYEntrar()
    fake.sinRed = true
    await repos.carreras.guardar(carrera('b'))
    expect(await repos.carreras.listar()).toHaveLength(1)
    expect(JSON.parse(localStorage.getItem('gacelas:carreras-pendientes')!)).toHaveLength(1)
    fake.sinRed = false
    const lista = await repos.carreras.listar()
    expect(lista.map((c) => c.id)).toEqual(['b'])
    expect(fake.tablas.activities).toHaveLength(1)
    expect(JSON.parse(localStorage.getItem('gacelas:carreras-pendientes')!)).toHaveLength(0)
  })
})

describe('Rutas, ajustes y eventos', () => {
  it('guarda rutas por usuario', async () => {
    expect(await repos.rutas.alternarGuardada('2')).toEqual(['2'])
    await registrarYEntrar()
    expect(await repos.rutas.alternarGuardada('1')).toEqual(['1'])
    expect(await repos.rutas.alternarGuardada('1')).toEqual([])
  })

  it('ajustes, privacidad, likes y comentarios', async () => {
    await registrarYEntrar()
    await repos.ajustes.actualizar({ visibilidad: 'publico', radioHogarM: 500, metronomoBpm: 172 })
    expect(fake.tablas.user_settings[0]).toMatchObject({ visibility: 'public', home_radius_m: 500 })
    await repos.ajustes.actualizar({ likes: ['1', '2'] })
    await repos.ajustes.actualizar({ likes: ['2'] })
    await repos.ajustes.actualizar({ comentarios: { '2': ['Bien!'] } })
    localStorage.clear()
    const a = await repos.ajustes.leer()
    expect(a).toMatchObject({
      visibilidad: 'publico',
      radioHogarM: 500,
      metronomoBpm: 172,
      likes: ['2'],
    })
    expect(a.comentarios).toEqual({ '2': ['Bien!'] })
    fake.sinRed = true
    expect((await repos.ajustes.leer()).visibilidad).toBe('publico')
  })

  it('crea eventos e inscripciones', async () => {
    expect(await repos.eventos.inscritos()).toEqual([])
    await registrarYEntrar()
    const ev = {
      id: 'ev1',
      titulo: 'Fondo',
      descripcion: '',
      fecha: 'Sábado',
      hora: '6:00 AM',
      ubicacion: 'Jipiro',
      participantes: 0,
      distanciaKm: 10,
      pasado: false,
      creadoPorUsuario: true,
    }
    const creados = await repos.eventos.crear(ev)
    expect(creados[0]).toMatchObject({ id: 'ev1', titulo: 'Fondo', creadoPorUsuario: true })
    expect(await repos.eventos.inscritos()).toEqual(['ev1'])
    expect(await repos.eventos.alternarInscripcion('ev1')).toEqual([])
    expect(await repos.eventos.alternarInscripcion('1')).toEqual(['1'])
  })
})

describe('utilidades', () => {
  it('traduce mensajes y mapea filas', () => {
    expect(traducirError('Email not confirmed')).toMatch(/Confirma/)
    expect(traducirError('rate limit exceeded')).toMatch(/Demasiados/)
    expect(traducirError('Failed to fetch')).toMatch(/conexión/)
    expect(traducirError('raro')).toMatch(/Intenta/)
    expect(eventoDesdeFila({ id: '9', title: 't', starts_at: '2000-01-01T00:00:00Z' }).pasado).toBe(
      true
    )
    expect(
      perfilDesdeFila(null, { email: 'X@Y.ec', user_metadata: { name: 'Equis' } } as never).nombre
    ).toBe('Equis')
  })

  it('el store usa los repositorios de Supabase y entra con Google', async () => {
    usarRepositorios(repos)
    await cargar()
    expect(await useIdentidad.getState().entrarConGoogle()).toBeNull()
    const r = await useIdentidad.getState().registrar('Jhona Runner', 'j@utpl.edu.ec', 'gacela123')
    expect(r.ok).toBe(true)
    expect((await useIdentidad.getState().cambiarContrasena('otra12345')).ok).toBe(true)
  })
})

describe('notificaciones', () => {
  it('convierte filas de Supabase', async () => {
    const { notificacionDesdeFila } = await import('../contexts/avisos/api')
    const ahora = Date.parse('2026-10-10T12:00:00Z')
    const n = notificacionDesdeFila(
      {
        id: 'x',
        type: 'raro',
        actor_name: 'Gacelas Loja',
        message: 'Hola',
        read: false,
        created_at: '2026-10-10T11:00:00Z',
      },
      ahora
    )
    expect(n).toEqual({
      id: 'x',
      tipo: 'event',
      usuario: 'Gacelas Loja',
      mensaje: 'Hola',
      tiempo: 'hace 1h',
      leida: false,
    })
  })
})
