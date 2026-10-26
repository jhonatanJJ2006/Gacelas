import { describe, expect, it } from 'vitest'
import {
  consultarSeguridadDeRuta,
  incidentesRecientes,
  sugerirDiaSeguro,
} from '../../../src/contexts/seguridad/application/consultas'
import { nivelDeIndice } from '../../../src/contexts/seguridad/domain/indice'
import {
  incidentesSupabase,
  indiceSupabase,
} from '../../../src/contexts/seguridad/infrastructure/supabase'
import { clienteFalso } from '../doble'

describe('seguridad', () => {
  it('usa los mismos umbrales que las apps y no presenta la falta de datos como seguro', () => {
    expect([0.6, 0.75, 1.49, 1.5].map(nivelDeIndice)).toEqual(['bajo', 'medio', 'medio', 'alto'])
    expect(nivelDeIndice(null)).toBe('sin_datos')
  })

  it('completa los siete dias y sugiere el de menor indice', async () => {
    const repo = {
      zonaDeRuta: async () => ({
        dias: [
          { dia: 1, indice: 1.2 },
          { dia: 4, indice: 0.5 },
        ],
        tipos: [],
        actualizadoHasta: '2026-03',
        fuente: 'SIS ECU 911 (CC-BY)',
      }),
    }
    const s = await consultarSeguridadDeRuta(repo, 'r1')
    expect(s.dias).toHaveLength(7)
    expect(s.dias[0].nivel).toBe('sin_datos')
    expect(s.dias[4].nivel).toBe('bajo')
    expect(sugerirDiaSeguro(s)).toBe(4)
  })

  it('sin zona no sugiere ningun dia', async () => {
    const s = await consultarSeguridadDeRuta({ zonaDeRuta: async () => null }, 'r1')
    expect(s.dias.every((d) => d.nivel === 'sin_datos')).toBe(true)
    expect(sugerirDiaSeguro(s)).toBeNull()
  })

  it('acota el limite de incidentes', async () => {
    const pedidos: number[] = []
    const repo = { recientes: async (n: number) => (pedidos.push(n), []) }
    await incidentesRecientes(repo, 0)
    await incidentesRecientes(repo, 9999)
    expect(pedidos).toEqual([1, 500])
  })

  it('el adaptador llama a la RPC seguridad_zona_ruta y mapea la respuesta', async () => {
    const { sb, llamadas } = clienteFalso({
      data: { dias: [{ dia: 2, indice: 1 }], actualizado_hasta: '2026-01', fuente: 'F' },
      error: null,
    })
    const zona = await indiceSupabase(sb).zonaDeRuta('r9')
    expect(llamadas[0]).toEqual({
      metodo: 'rpc',
      args: ['seguridad_zona_ruta', { p_ruta_id: 'r9' }],
    })
    expect(zona).toEqual({
      dias: [{ dia: 2, indice: 1 }],
      tipos: [],
      actualizadoHasta: '2026-01',
      fuente: 'F',
    })
  })

  it('el adaptador lee incidentes_publicos ordenados por hora', async () => {
    const { sb, llamadas } = clienteFalso({
      data: [
        {
          id: 'i1',
          tipo: 'acoso',
          lat: 1,
          lng: 2,
          ocurrido_hora: '2026-10-01T10:00:00Z',
          confirmaciones: null,
          estado: 'verificado',
        },
      ],
      error: null,
    })
    const lista = await incidentesSupabase(sb).recientes(10)
    expect(llamadas.map((l) => l.metodo)).toEqual(['from', 'select', 'order', 'limit'])
    expect(lista[0]).toMatchObject({ id: 'i1', confirmaciones: 0, verificado: true })
  })

  it('los errores de Supabase se propagan', async () => {
    const { sb } = clienteFalso({ data: null, error: { message: 'sin permiso' } })
    await expect(indiceSupabase(sb).zonaDeRuta('r1')).rejects.toThrow('sin permiso')
  })
})
