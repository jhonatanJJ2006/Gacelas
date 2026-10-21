import { describe, expect, it } from 'vitest'
import { publicacionDesdeFilaFeed } from '../../data/supabaseRepos'
import { nivelCorredor } from '../logica'

describe('feed social', () => {
  it('nivel del corredor por km', () => {
    expect(nivelCorredor(0)).toBe('Gacela Principiante')
    expect(nivelCorredor(30)).toBe('Gacela Intermedia')
    expect(nivelCorredor(150)).toBe('Gacela Avanzada')
    expect(nivelCorredor(500)).toBe('Gacela Élite')
  })
  it('lee desnivel, fotos, compartidos y ultimo comentario de la RPC', () => {
    const p = publicacionDesdeFilaFeed({
      id: 'a',
      user_id: 'u',
      start_time: '2026-10-21T10:00:00Z',
      distance_meters: 5000,
      duration_seconds: 1500,
      elevation_gain: 31,
      images: ['post-images/u/a/1.jpg'],
      reposts_count: 2,
      reposteada: true,
      autor_km: 42.5,
      ultimo_comentario: { autor: 'Ana', texto: 'Bien' },
    })
    expect(p).toMatchObject({
      desnivelM: 31,
      imagenes: ['post-images/u/a/1.jpg'],
      compartidos: 2,
      compartida: true,
      autorKm: 42.5,
      ultimoComentario: { autor: 'Ana', texto: 'Bien' },
    })
  })
})
