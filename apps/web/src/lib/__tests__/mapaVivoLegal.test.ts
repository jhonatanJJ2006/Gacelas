import { beforeEach, describe, expect, it } from 'vitest'
import {
  corredorDesdeFila,
  tocaPublicar,
  visibles,
  type CorredorEnMapa,
} from '../../features/comunidad/domain/mapaVivo'
import {
  historial,
  otorgado,
  registrarConsentimiento,
  requiereAceptarTerminos,
} from '../../contexts/legal/consentimientos'
import { PRIVACIDAD, TERMINOS, VERSIONES_LEGALES } from '../../contexts/legal/textos'

const c = (id: string, hace: number, ahora: number): CorredorEnMapa => ({
  userId: id,
  nombre: id,
  avatarUrl: null,
  lat: -3.99,
  lng: -79.2,
  corriendo: true,
  actualizadoMs: ahora - hace,
})

describe('mapa en vivo', () => {
  it('oculta posiciones de mas de 2 minutos y la propia', () => {
    const ahora = 1_000_000
    const l = [c('a', 10_000, ahora), c('b', 121_000, ahora), c('yo', 1_000, ahora)]
    expect(visibles(l, ahora, 'yo').map((x) => x.userId)).toEqual(['a'])
  })

  it('limita la publicacion', () => {
    expect(tocaPublicar(null, null, 0, false)).toBe(true)
    expect(tocaPublicar(0, true, 10_000, true)).toBe(false)
    expect(tocaPublicar(0, true, 15_000, true)).toBe(true)
    expect(tocaPublicar(0, false, 30_000, false)).toBe(false)
    expect(tocaPublicar(0, false, 60_000, false)).toBe(true)
    expect(tocaPublicar(0, false, 1_000, true)).toBe(true)
  })

  it('lee una fila con su perfil', () => {
    const r = corredorDesdeFila(
      { user_id: 'u1', lat: -3.9, lng: -79.2, running: true, updated_at: '2026-10-25T15:00:00Z' },
      { full_name: 'Ana Torres', avatar_url: null }
    )
    expect(r).toMatchObject({ userId: 'u1', nombre: 'Ana Torres', corriendo: true })
    expect(r.actualizadoMs).toBe(Date.parse('2026-10-25T15:00:00Z'))
  })
})

describe('consentimientos', () => {
  beforeEach(() => localStorage.clear())

  it('guarda historial y la ultima decision manda', async () => {
    await registrarConsentimiento('ubicacion_visible', true, 1)
    await registrarConsentimiento('ubicacion_visible', false, 2)
    expect(historial()).toHaveLength(2)
    expect(otorgado(historial(), 'ubicacion_visible')).toBe(false)
  })

  it('pide reaceptar al cambiar la version', () => {
    expect(requiereAceptarTerminos([])).toBe(true)
    const vieja = { tipo: 'terminos' as const, version: '2020-01-01', otorgado: true, fechaMs: 1 }
    expect(requiereAceptarTerminos([vieja])).toBe(true)
    const actual = { ...vieja, version: VERSIONES_LEGALES.terminos, fechaMs: 2 }
    expect(requiereAceptarTerminos([vieja, actual])).toBe(false)
  })

  it('los textos legales mencionan la LOPDP y la caducidad de 2 minutos', () => {
    expect(TERMINOS.length).toBeGreaterThan(5)
    expect(PRIVACIDAD.some((s) => s.texto.includes('LOPDP'))).toBe(true)
    expect(PRIVACIDAD.some((s) => s.texto.includes('2 minutos'))).toBe(true)
  })
})
