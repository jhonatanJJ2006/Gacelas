import { describe, expect, it } from 'vitest'
import { duracionTotalSeg, estadoIntervalo, intervaloMetronomoMs } from '../intervalos'
import { ValidacionAuth } from '../validacion'
import { diaSemana, inicioSemana, racha, resumenMes, resumenSemana } from '../estadisticas'
import {
  filtrarEventos,
  filtrarNotificaciones,
  filtrarRutas,
  notificacionesVisibles,
} from '../filtros'
import {
  calidadSenal,
  caloriasEstimadas,
  construirFeed,
  destinoNotificacion,
  filtrarFeed,
  indicacion,
  opcionesBuscador,
  saludo,
  validarEvento,
} from '../logica'
import { eventosMock, ID_USUARIO, notificacionesMock, rutasMock } from '../../mocks/data'
import { AJUSTES_POR_DEFECTO, type CarreraGuardada } from '../../data/tipos'

const ms = (y: number, m: number, d: number, h = 7) => new Date(y, m - 1, d, h).getTime()
const carrera = (inicioMs: number, km = 5, publica = true): CarreraGuardada => ({
  id: `${inicioMs}`,
  titulo: 'c',
  nota: '',
  publica,
  inicioMs,
  segundos: 1500,
  distanciaKm: km,
  desnivelM: 10,
  parciales: [],
  puntos: [],
})

describe('intervalos', () => {
  const c = { trabajoSeg: 60, descansoSeg: 30, rondas: 3, vibracion: true, sonido: true }
  it('duracion sin el ultimo descanso', () => expect(duracionTotalSeg(c)).toBe(240))
  it('fases y rondas', () => {
    expect(estadoIntervalo(c, 0)).toEqual({ fase: 'trabajo', ronda: 1, restanteSeg: 60 })
    expect(estadoIntervalo(c, 60)).toEqual({ fase: 'descanso', ronda: 1, restanteSeg: 30 })
    expect(estadoIntervalo(c, 90).ronda).toBe(2)
    expect(estadoIntervalo(c, 240).fase).toBe('terminado')
    expect(estadoIntervalo({ ...c, rondas: 0 }, 0).fase).toBe('terminado')
  })
  it('metronomo limitado a 40-240 BPM', () => {
    expect(intervaloMetronomoMs(120)).toBe(500)
    expect(intervaloMetronomoMs(1000)).toBe(250)
    expect(intervaloMetronomoMs(1)).toBe(1500)
  })
})

describe('ValidacionAuth', () => {
  it('email', () => {
    expect(ValidacionAuth.email('jhona@utpl.edu.ec')).toBeNull()
    expect(ValidacionAuth.email('')).toMatch(/Ingresa/)
    expect(ValidacionAuth.email('sin-arroba')).toMatch(/no es válido/)
  })
  it('contraseña', () => {
    expect(ValidacionAuth.password('gacela123')).toBeNull()
    expect(ValidacionAuth.password('corta1')).toMatch(/8/)
    expect(ValidacionAuth.password('soloLetras')).toMatch(/letras y números/)
  })
  it('nombre y usuario', () => {
    expect(ValidacionAuth.nombre(' a ')).not.toBeNull()
    expect(ValidacionAuth.usernameDesde('Jhona Pérez!', 'x@y.com')).toBe('jhona_perez')
    expect(ValidacionAuth.usernameDesde('!!', 'xavi@y.com')).toBe('xavi')
  })
})

describe('estadisticas', () => {
  const hoy = ms(2026, 10, 9, 20) // viernes
  it('semana, dias y racha', () => {
    const lista = [
      carrera(ms(2026, 10, 9)),
      carrera(ms(2026, 10, 8)),
      carrera(ms(2026, 10, 7)),
      carrera(ms(2026, 10, 1)),
    ]
    const s = resumenSemana(lista, hoy)
    expect(s.carreras).toBe(3)
    expect(s.km).toBe(15)
    expect([...s.diasCorridos].sort()).toEqual([2, 3, 4])
    expect(s.ritmoSegKm).toBe(300)
    expect(s.racha).toBe(3)
    expect(resumenSemana([], hoy).ritmoSegKm).toBeNull()
  })
  it('racha si hoy no corrio y lunes como inicio de semana', () => {
    expect(racha([carrera(ms(2026, 10, 8))], ms(2026, 10, 9))).toBe(1)
    expect(racha([carrera(ms(2026, 10, 6))], ms(2026, 10, 9))).toBe(0)
    expect(new Date(inicioSemana(hoy)).getDate()).toBe(5)
    expect(diaSemana(ms(2026, 10, 11))).toBe(6)
  })
  it('resumen del mes', () => {
    const r = resumenMes([carrera(ms(2026, 10, 2)), carrera(ms(2026, 9, 30))], hoy)
    expect(r).toEqual({ carreras: 1, km: 5, segundos: 1500, desnivelM: 10 })
  })
})

describe('filtros', () => {
  it('rutas por pestaña y busqueda', () => {
    expect(filtrarRutas(rutasMock, 'Explorar', [], ID_USUARIO)).toHaveLength(4)
    expect(filtrarRutas(rutasMock, 'Mis Rutas', [], ID_USUARIO).map((r) => r.id)).toEqual(['4'])
    expect(filtrarRutas(rutasMock, 'Guardadas', ['2'], ID_USUARIO).map((r) => r.id)).toEqual(['2'])
    expect(
      filtrarRutas(rutasMock, 'Explorar', [], ID_USUARIO, 'villonaco').map((r) => r.id)
    ).toEqual(['3'])
    expect(filtrarRutas(rutasMock, 'Explorar', [], ID_USUARIO, 'principiantes')).toHaveLength(1)
  })
  it('eventos por pestaña', () => {
    expect(filtrarEventos(eventosMock, 'Próximos', []).every((e) => !e.pasado)).toBe(true)
    expect(filtrarEventos(eventosMock, 'Pasados', []).map((e) => e.id)).toEqual(['4'])
    expect(filtrarEventos(eventosMock, 'Mis eventos', ['2']).map((e) => e.id)).toEqual(['2'])
    expect(
      filtrarEventos([{ ...eventosMock[0], id: 'u', creadoPorUsuario: true }], 'Mis eventos', [])
    ).toHaveLength(1)
  })
  it('notificaciones segun ajustes y leidas', () => {
    expect(filtrarNotificaciones(notificacionesMock, 'Todas')).toHaveLength(5)
    expect(filtrarNotificaciones(notificacionesMock, 'Sin leer')).toHaveLength(2)
    expect(
      filtrarNotificaciones(notificacionesMock, 'En vivo').every((n) => n.tipo === 'live')
    ).toBe(true)
    const sinLikes = notificacionesVisibles({ ...AJUSTES_POR_DEFECTO, notifLikes: false })
    expect(sinLikes.some((n) => n.tipo === 'like')).toBe(false)
    const leidas = notificacionesVisibles({
      ...AJUSTES_POR_DEFECTO,
      notificacionesLeidas: ['1', '2'],
    })
    expect(leidas.every((n) => n.leida)).toBe(true)
  })
})

describe('logica de pantallas', () => {
  it('feed: une carreras publicas y comunidad, filtra y oculta', () => {
    const feed = construirFeed(
      [carrera(Date.now() - 3_600_000), carrera(1, 3, false)],
      'Jhona',
      Date.now()
    )
    expect(feed.filter((p) => p.propia)).toHaveLength(1)
    expect(feed[0]).toMatchObject({ autor: 'Jhona', tiempo: 'hace 1h', ritmoSegKm: 300 })
    expect(filtrarFeed(feed, 'Mías', [])).toHaveLength(1)
    expect(filtrarFeed(feed, 'Comunidad', []).every((p) => !p.propia)).toBe(true)
    expect(filtrarFeed(feed, 'Todos', ['a-1']).some((p) => p.id === 'a-1')).toBe(false)
    expect(construirFeed([carrera(Date.now() - 120_000)], 'J', Date.now())[0].tiempo).toBe(
      'hace 2 min'
    )
    expect(construirFeed([carrera(Date.now() - 2 * 86_400_000)], 'J', Date.now())[0].tiempo).toBe(
      'hace 2d'
    )
  })
  it('buscador: sugeridas por defecto, busca rutas de Loja y respeta la preferida', () => {
    expect(opcionesBuscador('', null).map((o) => o.id)).toEqual(['primary', 'alt'])
    expect(opcionesBuscador('jipiro', null).map((o) => o.id)).toContain('ruta-1')
    expect(opcionesBuscador('', '3')[0].id).toBe('ruta-3')
    expect(opcionesBuscador('zzz', null)).toHaveLength(0)
  })
  it('indicaciones de navegacion', () => {
    expect(indicacion(0)).toEqual({ metros: 500, texto: 'Sigue recto por la Av. Universitaria' })
    expect(indicacion(1).metros).toBe(200)
    expect(indicacion(10).texto).toMatch(/Llegaste/)
  })
  it('validar evento', () => {
    const ok = {
      titulo: 'Fondo',
      fecha: '2026-11-01',
      hora: '06:00',
      ubicacion: 'Jipiro',
      distanciaKm: '10',
    }
    expect(validarEvento(ok)).toBeNull()
    expect(validarEvento({ ...ok, titulo: 'a' })).toMatch(/título/)
    expect(validarEvento({ ...ok, fecha: '' })).toMatch(/fecha/)
    expect(validarEvento({ ...ok, hora: '' })).toMatch(/hora/)
    expect(validarEvento({ ...ok, ubicacion: ' ' })).toMatch(/encuentro/)
    expect(validarEvento({ ...ok, distanciaKm: '0' })).toMatch(/distancia/)
  })
  it('otros', () => {
    expect(calidadSenal(5)).toBe('Señal fuerte')
    expect(calidadSenal(20)).toBe('Señal media')
    expect(calidadSenal(50)).toBe('Señal débil')
    expect(caloriasEstimadas(5)).toBe(350)
    expect(saludo(8)).toBe('Buenos días')
    expect(saludo(15)).toBe('Buenas tardes')
    expect(saludo(21)).toBe('Buenas noches')
    expect(destinoNotificacion(notificacionesMock[4])).toBe('/comunidad/eventos/1')
    expect(destinoNotificacion(notificacionesMock[3])).toBe('/perfil')
    expect(destinoNotificacion(notificacionesMock[0])).toBe('/comunidad/en-vivo')
  })
})

describe('feed con publicaciones reales', () => {
  it('usa las reales y no mezcla las de ejemplo', () => {
    const ahora = Date.parse('2026-10-10T12:00:00Z')
    const feed = construirFeed([], 'Yo', ahora, [
      {
        id: 'x',
        autorId: 'u2',
        autor: 'Ana',
        titulo: 'Fondo',
        inicioMs: ahora - 3_600_000,
        distanciaKm: 5,
        segundos: 1500,
        likes: 1,
        comentarios: 0,
        puntos: [],
      },
    ])
    expect(feed).toHaveLength(1)
    expect(feed[0]).toMatchObject({ id: 'r-x', autorId: 'u2', tiempo: 'hace 1h', ritmoSegKm: 300 })
  })
})
