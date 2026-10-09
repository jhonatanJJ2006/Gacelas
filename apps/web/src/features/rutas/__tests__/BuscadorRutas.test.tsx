import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { BuscadorRutas } from '../BuscadorRutas'

describe('BuscadorRutas', () => {
  it('renderiza sin errores', () => {
    render(
      <MemoryRouter>
        <BuscadorRutas />
      </MemoryRouter>
    )

    expect(screen.getByPlaceholderText(/a dónde quieres correr/i)).toBeInTheDocument()
  })

  it('muestra opciones de rutas', () => {
    render(
      <MemoryRouter>
        <BuscadorRutas />
      </MemoryRouter>
    )

    expect(screen.getByText(/jipiro/i)).toBeInTheDocument()
  })
})
