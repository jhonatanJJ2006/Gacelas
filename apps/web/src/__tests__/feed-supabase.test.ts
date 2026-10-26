import { describe, expect, it } from 'vitest'
import {
  comentarioDesdeFila,
  publicacionDesdeFilaFeed,
} from '../contexts/comunidad/infrastructure/comunidadSupabase'
import { construirFeed, filtrarFeed } from '../lib/logica'
import type { CarreraGuardada } from '../shared/kernel/tipos'

const ahora = Date.parse('2026-10-20T12:00:00Z')

const fila = (extra: Record<string, unknown> = {}) => ({
  id: '11111111-1111-1111-1111-111111111111',
  client_id: 'local-1',
  user_id: 'u2',
  autor: 'Ana',
  avatar_url: null,
  title: 'Fondo Jipiro',
  start_time: '2026-10-20T11:00:00Z',
  distance_meters: 8400,
  duration_seconds: 2520,
  likes_count: 3,
  comments_count: 2,
  public_points: [{ lat: -4, lng: -79, t: 0 }],
  me_gusta: true,
  sigo: true,
  propia: false,
  ...extra,
})

describe('feed desde la base', () => {
  it('mapea la fila de feed_comunidad', () => {
    expect(publicacionDesdeFilaFeed(fila())).toMatchObject({
      id: '11111111-1111-1111-1111-111111111111',
      autorId: 'u2',
      autor: 'Ana',
      distanciaKm: 8.4,
      segundos: 2520,
      likes: 3,
      comentarios: 2,
      meGusta: true,
      sigo: true,
      propia: false,
      clienteId: 'local-1',
      inicioMs: Date.parse('2026-10-20T11:00:00Z'),
    })
    const vacia = publicacionDesdeFilaFeed(fila({ autor: '', title: '', public_points: null }))
    expect(vacia).toMatchObject({ autor: 'Corredor', titulo: 'Carrera', puntos: [] })
  })

  it('mapea comentarios', () => {
    expect(
      comentarioDesdeFila({
        id: 'c1',
        user_id: 'u3',
        autor: 'Luis',
        body: 'Buen ritmo',
        created_at: '2026-10-20T11:30:00Z',
      })
    ).toMatchObject({ id: 'c1', autorId: 'u3', autor: 'Luis', texto: 'Buen ritmo' })
  })

  it('con backend no mezcla ejemplos, no duplica carreras ya subidas y ordena por fecha', () => {
    const mia = publicacionDesdeFilaFeed(
      fila({
        id: '22222222-2222-2222-2222-222222222222',
        propia: true,
        user_id: 'yo',
        client_id: 'c-sub',
        start_time: '2026-10-20T09:00:00Z',
      })
    )
    const otra = publicacionDesdeFilaFeed(fila())
    const local = (id: string, inicioMs: number) =>
      ({
        id,
        titulo: 'Local',
        inicioMs,
        distanciaKm: 5,
        segundos: 1500,
        puntos: [],
        publica: true,
      }) as unknown as CarreraGuardada
    const feed = construirFeed(
      [local('c-sub', ahora - 3 * 3_600_000), local('sin-subir', ahora - 600_000)],
      'Yo',
      ahora,
      [otra, mia],
      true
    )
    expect(feed.map((p) => p.id)).toEqual([
      'c-sin-subir',
      '11111111-1111-1111-1111-111111111111',
      '22222222-2222-2222-2222-222222222222',
    ])
    expect(feed[2]).toMatchObject({ propia: true, autor: 'Yo', autorId: undefined })
    expect(construirFeed([], 'Yo', ahora, [], true)).toHaveLength(0)
    expect(filtrarFeed(feed, 'Siguiendo', [], ['u2']).map((p) => p.autorId)).toEqual(['u2'])
  })
})
