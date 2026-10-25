import { describe, expect, it } from 'vitest'
import { RUTAS_LOJA } from '../../contexts/rutas/api'
import { aplicarFiltros, conOficiales, FILTROS_INICIALES } from '../rutas'
import { rutasMock } from '../../mocks/data'
import { rutaDesdeFila } from '../../data/supabaseRepos'

describe('rutas oficiales de Loja', () => {
  it('trae entre 20 y 30 rutas con trazado, categoria y fuente', () => {
    expect(RUTAS_LOJA.length).toBeGreaterThanOrEqual(20)
    expect(RUTAS_LOJA.length).toBeLessThanOrEqual(30)
    for (const r of RUTAS_LOJA) {
      expect(r.id).toMatch(/^loja-/)
      expect(r.oficial).toBe(true)
      expect(r.categoria).toBeTruthy()
      expect(r.fuente).toMatch(/OpenStreetMap/)
      expect((r.puntos ?? []).length).toBeGreaterThan(1)
      expect(r.distanciaKm).toBeGreaterThan(0)
      // Todas quedan alrededor de Loja.
      expect(Math.abs(r.puntos![0].lat + 4.1)).toBeLessThan(0.3)
    }
    expect(new Set(RUTAS_LOJA.map((r) => r.id)).size).toBe(RUTAS_LOJA.length)
  })

  it('conOficiales agrega las que faltan sin duplicar', () => {
    const una = rutasMock.slice(0, 1)
    expect(conOficiales(una)).toHaveLength(1 + RUTAS_LOJA.length)
    expect(conOficiales([...una, RUTAS_LOJA[0]])).toHaveLength(1 + RUTAS_LOJA.length)
  })

  it('filtra por categoria y dificultad', () => {
    const todas = conOficiales(rutasMock)
    const montana = aplicarFiltros(todas, { ...FILTROS_INICIALES, categoria: 'montana' })
    expect(montana.length).toBeGreaterThan(0)
    expect(montana.every((r) => r.categoria === 'montana')).toBe(true)
    const dificiles = aplicarFiltros(todas, {
      ...FILTROS_INICIALES,
      categoria: 'montana',
      dificultad: 'Difícil',
    })
    expect(dificiles.every((r) => r.dificultad === 'Difícil' && r.categoria === 'montana')).toBe(
      true
    )
  })

  it('lee las columnas nuevas de la base', () => {
    const r = rutaDesdeFila({
      id: 'loja-jipiro',
      name: 'Parque Jipiro',
      distance_km: 1.5,
      difficulty: 'Fácil',
      is_official: true,
      category: 'parque',
      surface: 'pavimento',
      source: 'OpenStreetMap',
      license: 'ODbL 1.0',
    })
    expect(r).toMatchObject({ oficial: true, categoria: 'parque', superficie: 'pavimento' })
    expect(r.fuente).toBe('OpenStreetMap (ODbL 1.0)')
  })
})
