import { describe, expect, it } from 'vitest'
import { destinoValido, guardarDestino, olvidarDestino, tomarDestino, verDestino } from '../destino'

function memoria() {
  const m = new Map<string, string>()
  return {
    getItem: (k: string) => m.get(k) ?? null,
    setItem: (k: string, v: string) => void m.set(k, v),
    removeItem: (k: string) => void m.delete(k),
  }
}

describe('destino tras iniciar sesion', () => {
  it('sin destino guardado vuelve a Inicio', () => {
    expect(tomarDestino(memoria())).toBe('/inicio')
  })
  it('vuelve a la pantalla protegida que se queria abrir y la borra', () => {
    const a = memoria()
    guardarDestino('/comunidad/clubes?tab=grupos', a)
    expect(verDestino(a)).toBe('/comunidad/clubes?tab=grupos')
    expect(tomarDestino(a)).toBe('/comunidad/clubes?tab=grupos')
    expect(tomarDestino(a)).toBe('/inicio')
  })
  it('ignora rutas de acceso, la landing y enlaces externos', () => {
    for (const r of [
      '/',
      '/bienvenida',
      '/inicio-sesion',
      '/registro',
      '/legal/terminos',
      '//malo.com',
      'https://x.com',
    ])
      expect(destinoValido(r)).toBeNull()
  })
  it('al cerrar sesion no recuerda la pantalla en la que estaba', () => {
    const a = memoria()
    olvidarDestino(a)
    guardarDestino('/ajustes', a)
    expect(tomarDestino(a)).toBe('/inicio')
    guardarDestino('/perfil', a)
    expect(tomarDestino(a)).toBe('/perfil')
  })
})
