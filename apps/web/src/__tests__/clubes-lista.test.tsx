import { render, screen, within } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { clubes, type Club } from '../contexts/eventos/api'
import { Clubes } from '../contexts/eventos/ui/Clubes'
import { prepararApp } from '../test/utils'

const base: Club = {
  id: 'g1',
  tipo: 'grupo',
  nombre: 'Gacelas del Jipiro',
  descripcion: '',
  ubicacion: '',
  ciudad: 'Loja',
  conAprobacion: false,
  miembros: 23,
  ownerId: 'x',
  comunidadId: null,
  logo: null,
  portada: null,
  contacto: '',
  lugarEncuentro: 'Parque Jipiro',
  punto: null,
  dias: [2, 6],
  hora: '06:00',
  nivel: 'principiante',
  ritmoSegKm: 390,
  cupo: 30,
  oculto: false,
}
const privado: Club = {
  ...base,
  id: 'g2',
  nombre: 'Trail Villonaco',
  conAprobacion: true,
  nivel: 'avanzado',
}

const ver = () =>
  render(
    <MemoryRouter>
      <Clubes />
    </MemoryRouter>
  )

describe('lista de clubes', () => {
  beforeEach(() => {
    prepararApp()
    vi.spyOn(clubes, 'disponible').mockReturnValue(true)
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('vacío ilustrado con Crear grupo', async () => {
    vi.spyOn(clubes, 'listar').mockResolvedValue({ todos: [], mios: new Set() })
    ver()
    expect(await screen.findByText('Aún no hay grupos')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /crear grupo/i })).toBeInTheDocument()
  })

  it('error con Reintentar vuelve a cargar', async () => {
    const listar = vi
      .spyOn(clubes, 'listar')
      .mockRejectedValueOnce(new Error('red'))
      .mockResolvedValue({ todos: [base], mios: new Set() })
    ver()
    await userEvent.click(await screen.findByRole('button', { name: 'Reintentar' }))
    expect(await screen.findByText('Gacelas del Jipiro')).toBeInTheDocument()
    expect(listar).toHaveBeenCalledTimes(2)
  })

  it('tarjetas, Mis grupos, búsqueda con limpiar y Unirme', async () => {
    vi.spyOn(clubes, 'listar').mockResolvedValue({ todos: [base, privado], mios: new Set(['g1']) })
    const unirse = vi.spyOn(clubes, 'unirse').mockResolvedValue()
    ver()
    expect(screen.getByTestId('cargando-clubes')).toBeInTheDocument()
    const mis = await screen.findByRole('region', { name: 'Mis grupos' })
    expect(within(mis).getByText('Gacelas del Jipiro')).toBeInTheDocument()
    expect(within(mis).getByText('Miembro')).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: 'Grupos · 2' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Solicitar' }))
    expect(unirse).toHaveBeenCalledWith('g2')
    expect(await screen.findByText('Solicitud enviada')).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Buscar grupos'), 'zzz')
    expect(screen.getByText('Sin resultados')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Limpiar búsqueda' }))
    expect(screen.getByText('Trail Villonaco')).toBeInTheDocument()
  })
})
