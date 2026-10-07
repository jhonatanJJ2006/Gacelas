import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Navigation } from '../Navigation'
import { BrowserRouter } from 'react-router-dom'

describe('Navigation', () => {
  it('should render Gacelas logo', () => {
    render(
      <BrowserRouter>
        <Navigation />
      </BrowserRouter>
    )

    expect(screen.getByText('Gacelas')).toBeDefined()
  })

  it('should render all navigation links', () => {
    render(
      <BrowserRouter>
        <Navigation />
      </BrowserRouter>
    )

    expect(screen.getByText('Inicio')).toBeDefined()
    expect(screen.getByText('Rutas')).toBeDefined()
    expect(screen.getByText('Comunidad')).toBeDefined()
    expect(screen.getByText('Eventos')).toBeDefined()
    expect(screen.getByText('Perfil')).toBeDefined()
  })
})
