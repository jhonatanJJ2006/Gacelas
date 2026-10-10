import { beforeEach, describe, expect, it } from 'vitest'
import { screen } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import { carreraPrueba, prepararApp, renderRuta } from '../test/utils'
import { useApp } from '../store/app'

describe('inicio', () => {
  it('resumen semanal con carreras reales', async () => {
    prepararApp({ carreras: [carreraPrueba({ distanciaKm: 7.5, inicioMs: Date.now() })] })
    renderRuta('/inicio')
    expect(await screen.findByTestId('km-semana')).toHaveTextContent('7.5')
    expect(screen.getByTestId('racha')).toHaveTextContent('1')
    await userEvent.click(screen.getByRole('link', { name: /empezar a correr/i }))
    expect(await screen.findByText('¿Listo para correr?')).toBeInTheDocument()
  })

  it('sin carreras invita a la primera', async () => {
    prepararApp()
    renderRuta('/inicio')
    expect(await screen.findByTestId('km-semana')).toHaveTextContent('0.0')
  })
})

describe('notificaciones', () => {
  beforeEach(() => prepararApp())

  it('filtra, marca leidas y abre el destino', async () => {
    renderRuta('/notificaciones')
    const total = (await screen.findAllByTestId('notificacion')).length
    expect(total).toBeGreaterThan(1)
    await userEvent.click(screen.getByRole('tab', { name: 'En vivo' }))
    expect(screen.getAllByTestId('notificacion')).toHaveLength(1)
    await userEvent.click(screen.getByRole('tab', { name: 'Todas' }))
    await userEvent.click(screen.getByRole('button', { name: /marcar todas/i }))
    expect(useApp.getState().ajustes.notificacionesLeidas.length).toBe(total)
    await userEvent.click(screen.getByRole('tab', { name: 'Sin leer' }))
    expect(screen.getByText('No hay notificaciones.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('tab', { name: 'Todas' }))
    await userEvent.click(screen.getAllByTestId('notificacion')[0])
    expect(await screen.findByRole('button', { name: /Filtrar publicaciones/ })).toBeInTheDocument()
  })

  it('respeta los ajustes desactivados', async () => {
    prepararApp({ ajustes: { notifEnVivo: false } })
    renderRuta('/notificaciones')
    expect(await screen.findAllByTestId('notificacion')).toHaveLength(4)
    await userEvent.click(screen.getByRole('tab', { name: 'En vivo' }))
    expect(screen.getByText('No hay notificaciones.')).toBeInTheDocument()
  })
})
