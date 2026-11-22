import { describe, expect, it } from 'vitest'
import { traducir } from './textos'

describe('idioma de la interfaz', () => {
  it('ecuador se queda en espanol', () => {
    expect(traducir('es-EC', 'Inicio')).toBe('Inicio')
  })
  it('ingles traduce la navegacion y los ajustes', () => {
    expect(traducir('en', 'Comunidad')).toBe('Community')
    expect(traducir('en', 'Cerrar sesión')).toBe('Sign out')
  })
  it('un texto sin traduccion no se pierde', () => {
    expect(traducir('en', 'Loja')).toBe('Loja')
  })
})
