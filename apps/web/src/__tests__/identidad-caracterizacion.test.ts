import { describe, expect, it } from 'vitest'
import type { User } from '@supabase/supabase-js'
import { perfilDesdeFila, traducirError } from '../data/supabaseRepos'

/* Caracterizacion de Identidad (F3, docs/ARQUITECTURA-DDD.md): fija el comportamiento antes de mover codigo. */
describe('identidad: traducirError', () => {
  it.each([
    ['Invalid login credentials', 'Correo o contraseña incorrectos'],
    ['User already registered', 'Ya existe una cuenta con ese correo'],
    ['Email not confirmed', 'Confirma tu correo antes de iniciar sesión'],
    ['For security purposes, wait', 'Demasiados intentos. Espera un momento y vuelve a probar'],
    ['Failed to fetch', 'Sin conexión con el servidor'],
    ['Password should be longer', 'La contraseña no es válida'],
    ['otra cosa', 'No se pudo completar. Intenta de nuevo'],
  ])('%s', (m, esperado) => expect(traducirError(m)).toBe(esperado))
})

describe('identidad: perfilDesdeFila', () => {
  const user = { email: 'Ana@Correo.COM', user_metadata: { name: 'Ana Pérez' } } as unknown as User
  it('sin fila usa los metadatos y valores por defecto', () => {
    const p = perfilDesdeFila(null, user)
    expect(p.email).toBe('ana@correo.com')
    expect(p.nombre).toBe('Ana Pérez')
    expect(p.ubicacion).toBe('Loja, Ecuador')
    expect(p.genero).toBe('Prefiero no decir')
    expect(p.foto).toBeNull()
    expect(p.username.length).toBeGreaterThan(0)
  })
  it('con fila prefiere los datos del perfil', () => {
    const p = perfilDesdeFila(
      { full_name: 'Ana', username: 'ana1', location: 'Quito', avatar_url: 'x.png' },
      user
    )
    expect(p).toMatchObject({ nombre: 'Ana', username: 'ana1', ubicacion: 'Quito', foto: 'x.png' })
  })
})
