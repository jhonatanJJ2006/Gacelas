import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { Notificaciones } from '../Notificaciones'

describe('Notificaciones', () => {
  it('renderiza sin errores', () => {
    render(
      <MemoryRouter>
        <Notificaciones />
      </MemoryRouter>
    )

    expect(screen.getByText(/notificaciones/i)).toBeInTheDocument()
  })
})
