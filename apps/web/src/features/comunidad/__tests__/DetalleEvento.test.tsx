import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { DetalleEvento } from '../DetalleEvento'

describe('DetalleEvento', () => {
  it('renderiza sin errores', () => {
    render(
      <MemoryRouter initialEntries={['/comunidad/eventos/1']}>
        <Routes>
          <Route path="/comunidad/eventos/:id" element={<DetalleEvento />} />
        </Routes>
      </MemoryRouter>
    )

    expect(screen.getByText(/carrera parque jipiro/i)).toBeInTheDocument()
  })
})
