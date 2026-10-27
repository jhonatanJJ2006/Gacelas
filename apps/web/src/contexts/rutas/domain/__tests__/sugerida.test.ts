import { describe, expect, it } from 'vitest'
import type { Ruta } from '../../../../mocks/data'
import type { CarreraGuardada } from '../../../../shared/kernel/tipos'
import { diaEcuador, distanciaObjetivo, elegirRutaSugerida, ventanaSinRepetir } from '../sugerida'

const ruta = (id: string, km: number, lat = -4): Ruta => ({
  id,
  nombre: id,
  distanciaKm: km,
  dificultad: 'Fácil',
  elevacionM: 0,
  descripcion: '',
  autorId: '',
  puntos: [
    { lat, lng: -79.2 },
    { lat: lat + 0.01, lng: -79.2 },
  ],
})
const carrera = (km: number, inicioMs: number, lat = -4): CarreraGuardada => ({
  id: String(inicioMs),
  titulo: '',
  nota: '',
  publica: false,
  inicioMs,
  segundos: 600,
  distanciaKm: km,
  desnivelM: 0,
  parciales: [],
  puntos: [{ lat, lng: -79.2, t: inicioMs } as never],
})
// 26 oct 2026, 08:00 en Ecuador.
const AHORA = Date.UTC(2026, 9, 26, 13)
const DIA = 86_400_000
const rutas = [ruta('a', 3), ruta('b', 3.2, -3.9), ruta('insegura', 3), ruta('c', 8, -3.8)]
const seguras = new Set(['a', 'b', 'c'])
const historicas = Array.from({ length: 5 }, (_, i) => carrera(3, AHORA - (10 + i) * DIA, -3.5))

describe('ruta sugerida para hoy', () => {
  it('usa el dia de Ecuador', () => {
    expect(diaEcuador(Date.UTC(2026, 9, 27, 3))).toBe('2026-10-26')
    expect(ventanaSinRepetir('2026-10-26')).toEqual([
      '2026-10-26',
      '2026-10-25',
      '2026-10-24',
      '2026-10-23',
    ])
  })

  it('objetivo es la media de las ultimas 5 o 5 km', () => {
    expect(distanciaObjetivo([])).toBe(5)
    expect(distanciaObjetivo(historicas)).toBe(3)
  })

  it('nunca sugiere una ruta insegura', () => {
    const r = elegirRutaSugerida({
      rutas,
      carreras: historicas,
      seguras,
      historial: [],
      ahoraMs: AHORA,
    })
    expect(r.estado).toBe('ok')
    if (r.estado === 'ok') expect(r.ruta.id).toBe('a')
    const nada = elegirRutaSugerida({
      rutas,
      carreras: [],
      seguras: new Set(),
      historial: [],
      ahoraMs: AHORA,
    })
    expect(nada.estado).toBe('sin_ruta')
  })

  it('es estable durante el dia', () => {
    const r = elegirRutaSugerida({
      rutas,
      carreras: historicas,
      seguras,
      historial: [{ dia: '2026-10-26', rutaId: 'c' }],
      ahoraMs: AHORA,
    })
    expect(r.estado === 'ok' && r.ruta.id).toBe('c')
  })

  it('no repite rutas sugeridas o corridas en los ultimos 3 dias', () => {
    const r = elegirRutaSugerida({
      rutas,
      carreras: [...historicas, carrera(3.1, AHORA - 2 * DIA, -3.9)],
      seguras,
      historial: [{ dia: '2026-10-23', rutaId: 'a' }],
      ahoraMs: AHORA,
    })
    expect(r.estado === 'ok' && r.ruta.id).toBe('c')
  })

  it('hace 4 dias ya no cuenta', () => {
    const r = elegirRutaSugerida({
      rutas,
      carreras: historicas,
      seguras,
      historial: [{ dia: '2026-10-22', rutaId: 'a' }],
      ahoraMs: AHORA,
    })
    expect(r.estado === 'ok' && r.ruta.id).toBe('a')
  })

  it('si todas las seguras se repitieron lo dice', () => {
    const r = elegirRutaSugerida({
      rutas,
      carreras: historicas,
      seguras: new Set(['a']),
      historial: [{ dia: '2026-10-25', rutaId: 'a' }],
      ahoraMs: AHORA,
    })
    expect(r.estado).toBe('sin_ruta')
  })

  it('a igual distancia prefiere la mas cercana al usuario', () => {
    const r = elegirRutaSugerida({
      rutas: [ruta('lejos', 3, -3.5), ruta('cerca', 3, -4)],
      carreras: historicas,
      seguras: new Set(['lejos', 'cerca']),
      historial: [],
      ahoraMs: AHORA,
      ubicacion: { lat: -4, lng: -79.2 },
    })
    expect(r.estado === 'ok' && r.ruta.id).toBe('cerca')
  })
})
