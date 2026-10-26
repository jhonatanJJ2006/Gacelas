import { beforeEach, describe, expect, it, vi } from 'vitest'

const estado: { filas: unknown[]; usuario: { id: string } | null } = { filas: [], usuario: null }

vi.mock('../shared/supabase/client', () => {
  const consulta = {
    select: () => consulta,
    eq: () => consulta,
    order: () => consulta,
    limit: () => Promise.resolve({ data: estado.filas, error: null }),
  }
  return {
    obtenerSupabase: () => ({
      auth: { getUser: () => Promise.resolve({ data: { user: estado.usuario } }) },
      from: () => consulta,
    }),
  }
})

import {
  requiereAceptarTerminos,
  terminosRemotos,
} from '../contexts/legal/infrastructure/consentimientos'
import { VERSIONES_LEGALES } from '../contexts/legal/domain/textos'

describe('re-aceptacion basada en consents de Supabase', () => {
  beforeEach(() => {
    estado.filas = []
    estado.usuario = { id: 'u1' }
  })

  it('sin sesion usa el dispositivo (undefined)', async () => {
    estado.usuario = null
    expect(await terminosRemotos()).toBeUndefined()
  })

  it('sin fila en consents pide aceptar', async () => {
    const r = await terminosRemotos()
    expect(r).toBeNull()
    expect(requiereAceptarTerminos(r ? [r] : [])).toBe(true)
  })

  it('version antigua en consents pide aceptar, la vigente no', async () => {
    estado.filas = [{ version: '0.9', granted: true, created_at: '2026-01-01T00:00:00Z' }]
    expect(requiereAceptarTerminos([(await terminosRemotos())!])).toBe(true)
    estado.filas = [
      { version: VERSIONES_LEGALES.terminos, granted: true, created_at: '2026-10-01T00:00:00Z' },
    ]
    expect(requiereAceptarTerminos([(await terminosRemotos())!])).toBe(false)
  })
})
