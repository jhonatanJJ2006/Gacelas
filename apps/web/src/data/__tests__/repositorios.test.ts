import { beforeEach, describe, expect, it } from 'vitest'
import { escribir, hashPassword, leer, repositoriosLocales } from '../repositorios'
import { eventosMock } from '../../mocks/data'

describe('repositorios locales', () => {
  beforeEach(() => localStorage.clear())

  it('auth: registro, sesion, login, perfil y cierre', async () => {
    const { auth } = repositoriosLocales()
    expect(await auth.sesionActual()).toBeNull()
    expect((await auth.registrar('Jhona', 'malo', 'gacela123')).ok).toBe(false)
    const r = await auth.registrar('Jhona Runner', 'Jhona@UTPL.edu.ec', 'gacela123')
    expect(r.ok).toBe(true)
    expect((await auth.sesionActual())?.email).toBe('jhona@utpl.edu.ec')
    expect(localStorage.getItem('gacelas:auth')).not.toContain('gacela123')
    expect((await auth.registrar('Otro', 'jhona@utpl.edu.ec', 'gacela123')).ok).toBe(false)
    await auth.cerrarSesion()
    expect(await auth.sesionActual()).toBeNull()
    expect(await auth.iniciarSesion('jhona@utpl.edu.ec', 'otra12345')).toEqual({
      ok: false,
      error: 'Correo o contraseña incorrectos',
    })
    expect((await auth.iniciarSesion('jhona@utpl.edu.ec', '')).ok).toBe(false)
    expect((await auth.iniciarSesion('nada', 'x')).ok).toBe(false)
    expect((await auth.iniciarSesion('JHONA@utpl.edu.ec', 'gacela123')).ok).toBe(true)
    const p = (await auth.sesionActual())!
    await auth.actualizarPerfil({ ...p, bio: 'Corro en Loja' })
    expect((await auth.sesionActual())?.bio).toBe('Corro en Loja')
    expect((await auth.recuperarContrasena('nadie@x.com')).ok).toBe(true)
    expect((await auth.recuperarContrasena('malo')).ok).toBe(false)
  })

  it('carreras: guardar al inicio, reemplazar y eliminar', async () => {
    const { carreras } = repositoriosLocales()
    const base = {
      nota: '',
      publica: true,
      segundos: 60,
      distanciaKm: 0.2,
      desnivelM: 0,
      parciales: [],
      puntos: [],
    }
    await carreras.guardar({ ...base, id: 'a', titulo: 'A', inicioMs: 1 })
    await carreras.guardar({ ...base, id: 'b', titulo: 'B', inicioMs: 2 })
    expect((await carreras.listar()).map((c) => c.id)).toEqual(['b', 'a'])
    await carreras.guardar({ ...base, id: 'a', titulo: 'A2', inicioMs: 1 })
    expect((await carreras.listar()).map((c) => c.titulo)).toEqual(['A2', 'B'])
    expect((await carreras.eliminar('a')).map((c) => c.id)).toEqual(['b'])
  })

  it('rutas, ajustes y eventos', async () => {
    const { rutas, ajustes, eventos } = repositoriosLocales()
    expect(await rutas.alternarGuardada('1')).toEqual(['1'])
    expect(await rutas.alternarGuardada('1')).toEqual([])
    expect((await ajustes.leer()).modoOscuro).toBe(true)
    await ajustes.actualizar({ unidadesMetricas: false, metronomoBpm: 180 })
    expect(await repositoriosLocales().ajustes.leer()).toMatchObject({
      unidadesMetricas: false,
      metronomoBpm: 180,
      modoOscuro: true,
    })
    await eventos.crear({ ...eventosMock[0], id: 'u1', creadoPorUsuario: true })
    expect((await eventos.creados()).map((e) => e.id)).toEqual(['u1'])
    expect(await eventos.inscritos()).toContain('u1')
    expect(await eventos.alternarInscripcion('2')).toContain('2')
    expect(await eventos.alternarInscripcion('2')).not.toContain('2')
  })

  it('leer tolera JSON roto y hash es estable', async () => {
    localStorage.setItem('gacelas:roto', '{no')
    expect(leer('roto', 7)).toBe(7)
    expect(escribir('x', [1])).toEqual([1])
    expect(await hashPassword('a')).toBe(await hashPassword('a'))
    expect(await hashPassword('a')).not.toBe(await hashPassword('b'))
  })
})
