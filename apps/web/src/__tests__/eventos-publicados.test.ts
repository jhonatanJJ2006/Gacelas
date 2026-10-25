import { describe, expect, it } from 'vitest'
import { eventoVisible } from '../data/supabaseRepos'

describe('eventos publicados', () => {
  it('oculta borradores a quien no es su creador', () => {
    expect(eventoVisible({ published: true }, 'u1')).toBe(true)
    expect(eventoVisible({}, 'u1')).toBe(true)
    expect(eventoVisible({ published: false, created_by: 'u2' }, 'u1')).toBe(false)
    expect(eventoVisible({ published: false, created_by: 'u1' }, 'u1')).toBe(true)
  })
})
