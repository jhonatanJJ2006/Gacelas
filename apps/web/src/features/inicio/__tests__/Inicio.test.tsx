import { describe, it, expect } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { Inicio } from '../Inicio'

describe('Inicio', () => {
  it('renderiza sin errores', () => {
    render(
      <MemoryRouter>
        <Inicio />
      </MemoryRouter>
    )

    expect(screen.getByText(/buenos días/i)).toBeInTheDocument()
  })

  it('muestra la ubicación del usuario', () => {
    render(
      <MemoryRouter>
        <Inicio />
      </MemoryRouter>
    )

    expect(screen.getByText('Loja, Ecuador')).toBeInTheDocument()
  })

  it('muestra el estado del autopilot', () => {
    render(
      <MemoryRouter>
        <Inicio />
      </MemoryRouter>
    )

    expect(screen.getByText('AUTOPILOT ACTIVO')).toBeInTheDocument()
  })

  it('muestra métricas de resumen', () => {
    render(
      <MemoryRouter>
        <Inicio />
      </MemoryRouter>
    )

    expect(screen.getByText('8')).toBeInTheDocument()
    expect(screen.getByText('42')).toBeInTheDocument()
    expect(screen.getByText('5:20')).toBeInTheDocument()
  })

  it('muestra el botón de acción rápida para empezar a correr', () => {
    render(
      <MemoryRouter>
        <Inicio />
      </MemoryRouter>
    )

    const correrButton = screen.getByText('Empezar a correr')
    expect(correrButton).toBeInTheDocument()
    expect(correrButton.closest('a')).toHaveAttribute('href', '/correr')
  })

  it('muestra la sección de semana atlética', () => {
    render(
      <MemoryRouter>
        <Inicio />
      </MemoryRouter>
    )

    expect(screen.getByText('Semana atlética')).toBeInTheDocument()
    expect(screen.getByText('3 de 5 días')).toBeInTheDocument()
  })

  it('muestra el próximo evento', () => {
    render(
      <MemoryRouter>
        <Inicio />
      </MemoryRouter>
    )

    expect(screen.getByText('Próximo encuentro')).toBeInTheDocument()
    expect(screen.getByText('Carrera Parque Jipiro')).toBeInTheDocument()
  })

  it('muestra el contenedor 3D o el fallback de carga', async () => {
    render(
      <MemoryRouter>
        <Inicio />
      </MemoryRouter>
    )

    await waitFor(() => {
      const canvas = screen.queryByTestId('three-canvas')
      const fallback = screen.queryByText('Cargando escena 3D...')
      expect(canvas || fallback).toBeTruthy()
    })
  })
})
