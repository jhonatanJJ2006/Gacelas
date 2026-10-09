import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { Cronometro } from '../Cronometro'

const mockNavigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

describe('Cronometro', () => {
  it('renderiza sin errores', () => {
    render(
      <MemoryRouter>
        <Cronometro />
      </MemoryRouter>
    )

    expect(screen.getByText('Cronómetro Profesional')).toBeInTheDocument()
  })

  it('muestra el tiempo en formato correcto', () => {
    render(
      <MemoryRouter>
        <Cronometro />
      </MemoryRouter>
    )

    expect(screen.getByText(/29:/)).toBeInTheDocument()
  })

  it('muestra las métricas de distancia y ritmo', () => {
    render(
      <MemoryRouter>
        <Cronometro />
      </MemoryRouter>
    )

    expect(screen.getByText('Distancia Total')).toBeInTheDocument()
    expect(screen.getByText('Ritmo Actual')).toBeInTheDocument()
    expect(screen.getByText('Ritmo Promedio')).toBeInTheDocument()
  })

  it('muestra los controles de pausa y detener', () => {
    render(
      <MemoryRouter>
        <Cronometro />
      </MemoryRouter>
    )

    expect(screen.getByText('Vuelta')).toBeInTheDocument()
    expect(screen.getByText('Fin')).toBeInTheDocument()
  })

  it('muestra las pestañas de splits, intervalos y metrónomo', () => {
    render(
      <MemoryRouter>
        <Cronometro />
      </MemoryRouter>
    )

    expect(screen.getByText('Splits')).toBeInTheDocument()
    expect(screen.getByText('Intervalos')).toBeInTheDocument()
    expect(screen.getByText('Metrónomo')).toBeInTheDocument()
  })

  it('permite cambiar entre tabs', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <Cronometro />
      </MemoryRouter>
    )

    const intervaloTab = screen.getByText('Intervalos')
    await user.click(intervaloTab)

    expect(intervaloTab.closest('button')).toHaveClass('bg-surface-container')
  })

  it('muestra la lista de splits cuando está en esa tab', () => {
    render(
      <MemoryRouter>
        <Cronometro />
      </MemoryRouter>
    )

    expect(screen.getByText('PARCIAL / DISTANCIA')).toBeInTheDocument()
    expect(screen.getByText('RITMO')).toBeInTheDocument()
    expect(screen.getByText('TIEMPO')).toBeInTheDocument()
  })

  it('permite pausar y reanudar', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <Cronometro />
      </MemoryRouter>
    )

    const pauseButton = screen.getByText('pause').closest('button')
    expect(pauseButton).toBeInTheDocument()

    if (pauseButton) {
      await user.click(pauseButton)
      expect(screen.getByText('play_arrow')).toBeInTheDocument()
    }
  })

  it('muestra el estado "En Marcha"', () => {
    render(
      <MemoryRouter>
        <Cronometro />
      </MemoryRouter>
    )

    expect(screen.getByText('En Marcha')).toBeInTheDocument()
  })
})
