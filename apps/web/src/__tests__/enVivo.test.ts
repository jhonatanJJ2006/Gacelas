import { describe, expect, it } from 'vitest'
import { aPuntosGps, sesionActiva, sesionDesdeFila } from '../contexts/actividad/api'

const fila = {
  id: 's1',
  user_id: 'u1',
  audience: 'followers',
  started_at: '2026-10-11T05:00:00Z',
  ended_at: null,
  hidden: false,
  last_at: '2026-10-11T05:10:00Z',
  last_lat: -3.99,
  last_lng: -79.2,
  points: [{ lat: -3.99, lng: -79.2 }],
  distance_m: 2350,
  elapsed_s: 600,
}

describe('enVivo', () => {
  it('convierte la fila de live_sessions', () => {
    const s = sesionDesdeFila(fila, 'Ana')
    expect(s).toMatchObject({ nombre: 'Ana', distanciaKm: 2.35, segundos: 600, oculta: false })
    expect(s.posicion).toEqual({ lat: -3.99, lng: -79.2 })
  })
  it('no expone posicion mientras esta oculta cerca de casa', () => {
    const s = sesionDesdeFila({ ...fila, hidden: true })
    expect(s.posicion).toBeNull()
    expect(s.oculta).toBe(true)
  })
  it('una sesion sin posiciones en 15 min ya no esta activa', () => {
    const s = sesionDesdeFila(fila)
    expect(sesionActiva(s, Date.parse('2026-10-11T05:20:00Z'))).toBe(true)
    expect(sesionActiva(s, Date.parse('2026-10-11T05:26:00Z'))).toBe(false)
    expect(sesionActiva(sesionDesdeFila({ ...fila, ended_at: '2026-10-11T05:11:00Z' }))).toBe(false)
  })
  it('puntos para el trazado', () => {
    expect(aPuntosGps([{ lat: 1, lng: 2 }])[0]).toMatchObject({ lat: 1, lng: 2, tiempoMs: 0 })
  })
})
