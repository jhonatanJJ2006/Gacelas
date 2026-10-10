import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { Correr } from '../Correr'

describe('Correr', () => {
  it('renderiza sin errores', () => {
    render(
      <MemoryRouter>
        <Correr />
      </MemoryRouter>
    )

    expect(screen.getByText(/listo para correr/i)).toBeInTheDocument()
  })
})
