import { describe, expect, it } from 'vitest'
import { clubDesdeFila } from '../contexts/eventos/infrastructure/clubes'
import { textoCupo, unirEventos } from '../contexts/eventos/infrastructure/eventos'
import type { Evento } from '../mocks/data'

/* Caracterizacion de Eventos y clubes (F6, docs/ARQUITECTURA-DDD.md). */
describe('eventos y clubes', () => {
  it('unirEventos agrega los de ejemplo sin repetir ids', () => {
    const e = (id: string) => ({ id }) as Evento
    expect(unirEventos([e('a')], [e('a'), e('b')]).map((x) => x.id)).toEqual(['a', 'b'])
  })
  it('textoCupo', () => {
    expect(textoCupo({ cupo: null, inscritos: 3, enEspera: 0 } as never)).toBe('3 inscritos')
    expect(textoCupo({ cupo: 4, inscritos: 3, enEspera: 0 } as never)).toBe('3/4 · 1 cupo libre')
    expect(textoCupo({ cupo: 3, inscritos: 3, enEspera: 2 } as never)).toBe('Lleno · 2 en espera')
  })
  it('clubDesdeFila con valores por defecto', () => {
    expect(clubDesdeFila({ id: 5 })).toEqual({
      id: '5',
      tipo: 'grupo',
      nombre: '',
      descripcion: '',
      ubicacion: '',
      ciudad: 'Loja',
      conAprobacion: false,
      miembros: 0,
      ownerId: '',
      comunidadId: null,
      logo: null,
      portada: null,
      contacto: '',
      lugarEncuentro: '',
      punto: null,
      dias: [],
      hora: null,
      nivel: 'todos',
      ritmoSegKm: null,
      cupo: null,
      oculto: false,
    })
  })
})
