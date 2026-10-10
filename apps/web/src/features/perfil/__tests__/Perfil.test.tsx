import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { Perfil } from '../Perfil'

describe('Perfil', () => {
  it('renderiza sin errores', () => {
    render(
      <MemoryRouter>
        <Perfil />
      </MemoryRouter>
    )

    expect(screen.getByText('Jhonatan Runner')).toBeInTheDocument()
  })

  it('muestra el botón de editar perfil', () => {
    render(
      <MemoryRouter>
        <Perfil />
      </MemoryRouter>
    )

    const editarLink = screen
      .getAllByRole('link')
      .find((link) => link.getAttribute('href')?.includes('/perfil/editar'))
    expect(editarLink).toBeInTheDocument()
  })
})
