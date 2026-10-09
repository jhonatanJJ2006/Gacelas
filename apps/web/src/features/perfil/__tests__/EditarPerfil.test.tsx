import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { EditarPerfil } from '../EditarPerfil'

describe('EditarPerfil', () => {
  it('renderiza sin errores', () => {
    render(
      <MemoryRouter>
        <EditarPerfil />
      </MemoryRouter>
    )

    expect(screen.getByText(/editar perfil/i)).toBeInTheDocument()
  })

  it('muestra campos de formulario', () => {
    render(
      <MemoryRouter>
        <EditarPerfil />
      </MemoryRouter>
    )

    expect(screen.getByLabelText(/nombre completo/i)).toBeInTheDocument()
  })
})
