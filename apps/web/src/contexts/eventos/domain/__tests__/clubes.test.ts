import { describe, expect, it } from 'vitest'
import {
  accionClub,
  borradorVacio,
  estaLleno,
  filtrarClubes,
  leerRitmo,
  mensajeErrorClub,
  normalizarNombre,
  textoHorario,
  textoRitmo,
  validarClub,
  type Club,
} from '../clubes'
import { clubDesdeFila, filaDesdeBorrador } from '../../infrastructure/clubes'

const club = (c: Partial<Club>): Club => ({
  ...clubDesdeFila({ id: c.id ?? 'x' }),
  ...c,
})

describe('comunidades y grupos: dominio', () => {
  it('valida nombre, duplicados por tipo y campos del grupo', () => {
    const existentes = [club({ id: 'a', tipo: 'grupo', nombre: 'Gacelas  Jipiro' })]
    const b = { ...borradorVacio('grupo'), nombre: 'gacelas jipiro ' }
    expect(validarClub(b, existentes)).toMatch(/Ya existe un grupo/)
    expect(validarClub(b, existentes, 'a')).toBeNull()
    expect(validarClub({ ...b, tipo: 'comunidad' }, existentes)).toBeNull()
    expect(validarClub({ ...b, nombre: 'Otro', cupo: 1 })).toMatch(/cupo/)
    expect(validarClub({ ...b, nombre: 'Otro', hora: '25:00' })).toMatch(/hora/)
    expect(validarClub({ ...b, nombre: 'Otro', ritmoSegKm: 60 })).toMatch(/ritmo/)
  })
  it('normaliza nombres como el indice unico del servidor', () => {
    expect(normalizarNombre('  Runners   LOJA ')).toBe('runners loja')
  })
  it('textos de horario y ritmo', () => {
    expect(textoHorario([4, 2], '06:00')).toBe('Mar y Jue · 06:00')
    expect(textoHorario([1, 3, 5], null)).toBe('Lun, Mié y Vie')
    expect(textoRitmo(330)).toBe('5:30 min/km')
    expect(leerRitmo('5:30')).toBe(330)
    expect(leerRitmo('cinco')).toBeNull()
  })
  it('accion segun relacion y cupo', () => {
    expect(accionClub(null, true, 'grupo', true)).toBeNull()
    expect(
      accionClub(
        { usuarioId: 'a', nombre: 'A', rol: 'member', estado: 'active' },
        false,
        'comunidad'
      )
    ).toBe('Salir de la comunidad')
    expect(estaLleno({ cupo: 2, miembros: 2 })).toBe(true)
    expect(estaLleno({ cupo: null, miembros: 99 })).toBe(false)
  })
  it('buscador filtra por tipo, texto, nivel y oculta lo moderado', () => {
    const lista = [
      club({ id: '1', tipo: 'grupo', nombre: 'Jipiro 6am', nivel: 'principiante' }),
      club({ id: '2', tipo: 'grupo', nombre: 'Trail Villonaco', nivel: 'avanzado' }),
      club({ id: '3', tipo: 'comunidad', nombre: 'Runners Loja' }),
      club({ id: '4', tipo: 'grupo', nombre: 'Oculto', oculto: true }),
    ]
    expect(filtrarClubes(lista, { tipo: 'grupo' }).map((c) => c.id)).toEqual(['1', '2'])
    expect(filtrarClubes(lista, { tipo: 'grupo', consulta: 'villo' }).map((c) => c.id)).toEqual([
      '2',
    ])
    expect(filtrarClubes(lista, { tipo: 'grupo', nivel: 'principiante' }).map((c) => c.id)).toEqual(
      ['1']
    )
    expect(filtrarClubes(lista, { tipo: 'comunidad' }).map((c) => c.id)).toEqual(['3'])
  })
  it('traduce el error de nombre duplicado', () => {
    expect(
      mensajeErrorClub(
        'duplicate key value violates unique constraint "clubs_nombre_unico"',
        'comunidad'
      )
    ).toMatch(/una comunidad con ese nombre/)
  })
  it('mapea filas y borradores en ambos sentidos', () => {
    const fila = filaDesdeBorrador({
      ...borradorVacio('grupo'),
      nombre: ' Jipiro   6am ',
      dias: [5, 1],
      hora: '06:00',
      punto: { lat: -3.97, lng: -79.2 },
      cupo: 20,
    })
    expect(fila).toMatchObject({
      name: 'Jipiro 6am',
      training_days: [1, 5],
      max_members: 20,
      meeting_lat: -3.97,
    })
    const c = clubDesdeFila({ id: 'g', kind: 'grupo', ...fila, training_time: '06:00:00' })
    expect(c).toMatchObject({
      tipo: 'grupo',
      hora: '06:00',
      dias: [1, 5],
      punto: { lat: -3.97, lng: -79.2 },
    })
    const com = filaDesdeBorrador({
      ...borradorVacio('comunidad'),
      nombre: 'Runners',
      comunidadId: 'x',
      cupo: 9,
    })
    expect(com).toMatchObject({ community_id: null, max_members: null, training_days: [] })
  })
})
