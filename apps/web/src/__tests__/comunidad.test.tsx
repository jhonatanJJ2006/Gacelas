import { beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, within } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import { carreraPrueba, prepararApp, renderRuta } from '../test/utils'
import { useApp } from '../store/app'

describe('comunidad', () => {
  beforeEach(() =>
    prepararApp({
      carreras: [
        carreraPrueba({ titulo: 'Mi fondo', publica: true }),
        carreraPrueba({ id: 'c2', titulo: 'Secreta', publica: false }),
      ],
    })
  )

  it('feed con mis carreras publicas, likes, comentarios y ocultar', async () => {
    renderRuta('/comunidad')
    expect(await screen.findByText(/Mi fondo/)).toBeInTheDocument()
    expect(screen.queryByText(/Secreta/)).not.toBeInTheDocument()
    expect(screen.getAllByTestId('publicacion')).toHaveLength(3)

    const maria = screen.getByText('María López').closest('article')!
    await userEvent.click(within(maria).getByRole('button', { name: 'Me gusta' }))
    expect(within(maria).getByRole('button', { name: 'Quitar me gusta' })).toHaveTextContent('24')
    expect(useApp.getState().ajustes.likes).toEqual(['a-1'])

    await userEvent.click(within(maria).getByRole('button', { name: 'Comentar' }))
    const dialogo = screen.getByRole('dialog')
    expect(within(dialogo).getByRole('button', { name: 'Publicar' })).toBeDisabled()
    await userEvent.type(within(dialogo).getByLabelText('Comentario'), 'Gran ritmo')
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Publicar' }))
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Cerrar' }))
    expect(within(maria).getByText('Gran ritmo')).toBeInTheDocument()
    expect(within(maria).getByRole('button', { name: 'Comentar' })).toHaveTextContent('6')

    await userEvent.click(within(maria).getByRole('button', { name: 'Más opciones' }))
    await userEvent.click(screen.getByRole('menuitem', { name: 'Ocultar publicación' }))
    expect(screen.queryByText('María López')).not.toBeInTheDocument()
    expect(useApp.getState().ajustes.ocultas).toEqual(['a-1'])
  })

  it('filtro del feed y compartir', async () => {
    const writeText = vi.fn(() => Promise.resolve())
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true })
    renderRuta('/comunidad')
    await userEvent.click(await screen.findByRole('button', { name: 'Filtrar publicaciones' }))
    await userEvent.click(screen.getByRole('menuitemradio', { name: 'Mías' }))
    expect(screen.getAllByTestId('publicacion')).toHaveLength(1)
    await userEvent.click(screen.getByRole('button', { name: 'Compartir' }))
    expect(writeText).toHaveBeenCalledWith(expect.stringContaining('Jhona Runner corrió 5.00 km'))
    expect(await screen.findByText('Enlace copiado')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Filtrar publicaciones' }))
    await userEvent.click(screen.getByRole('menuitemradio', { name: 'Comunidad' }))
    expect(screen.getAllByTestId('publicacion')).toHaveLength(2)
    await userEvent.click(screen.getByRole('button', { name: 'Eventos' }))
    expect(await screen.findByRole('tab', { name: 'Próximos' })).toBeInTheDocument()
  })

  it('eventos: pestañas, inscribirse y crear evento', async () => {
    renderRuta('/comunidad/eventos')
    expect(await screen.findAllByTestId('evento')).toHaveLength(3)
    await userEvent.click(screen.getByRole('tab', { name: 'Pasados' }))
    expect(screen.getByText('Fondo dominical Zamora')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Inscribirse' })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('tab', { name: 'Próximos' }))
    const trail = screen.getByText('Trail Running Podocarpus').closest('article')!
    await userEvent.click(within(trail).getByRole('button', { name: 'Inscribirse' }))
    await userEvent.click(
      within(screen.getByRole('dialog', { name: 'Unirse al evento' })).getByRole('button', {
        name: 'Inscribirme',
      })
    )
    expect(within(trail).getByText('9 participantes')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('tab', { name: 'Mis eventos' }))
    expect(screen.getAllByTestId('evento')).toHaveLength(1)

    await userEvent.click(screen.getByRole('button', { name: 'Crear evento' }))
    const d = screen.getByRole('dialog', { name: 'Crear evento' })
    await userEvent.click(within(d).getByRole('button', { name: 'Crear' }))
    expect(within(d).getByRole('alert')).toHaveTextContent(/título/)
    await userEvent.type(within(d).getByLabelText('Título del evento'), 'Fondo UTPL')
    await userEvent.type(within(d).getByLabelText('Fecha'), '2026-11-01')
    await userEvent.type(within(d).getByLabelText('Hora'), '06:30')
    await userEvent.type(within(d).getByLabelText('Punto de encuentro'), 'Campus UTPL')
    await userEvent.click(within(d).getByRole('button', { name: 'Crear' }))
    expect(await screen.findByText('Fondo UTPL')).toBeInTheDocument()
    expect(screen.getByText('Organizas tú')).toBeInTheDocument()
    expect(JSON.parse(localStorage.getItem('gacelas:eventos-creados')!)[0].titulo).toBe(
      'Fondo UTPL'
    )
  })

  it('detalle de evento: inscripcion, compartir y evento inexistente', async () => {
    Object.defineProperty(navigator, 'share', {
      value: vi.fn(() => Promise.resolve()),
      configurable: true,
    })
    renderRuta('/comunidad/eventos/1')
    expect(await screen.findByText('Carrera Parque Jipiro')).toBeInTheDocument()
    expect(screen.getByTestId('participantes')).toHaveTextContent('15')
    await userEvent.click(screen.getByRole('button', { name: /inscribirme al evento/i }))
    expect(screen.getByTestId('participantes')).toHaveTextContent('16')
    expect(useApp.getState().inscritos).toEqual(['1'])
    await userEvent.click(screen.getByRole('button', { name: /cancelar inscripción/i }))
    expect(screen.getByTestId('participantes')).toHaveTextContent('15')
    await userEvent.click(screen.getByRole('button', { name: 'Compartir evento' }))
    expect(navigator.share).toHaveBeenCalled()
    Object.defineProperty(navigator, 'share', { value: undefined, configurable: true })
  })

  it('evento inexistente vuelve a la lista', async () => {
    renderRuta('/comunidad/eventos/999')
    expect(await screen.findByRole('tab', { name: 'Próximos' })).toBeInTheDocument()
  })
})
