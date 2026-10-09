import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { AjustesPreferencias } from '../AjustesPreferencias'

describe('AjustesPreferencias', () => {
  it('renderiza sin errores', () => {
    render(
      <MemoryRouter>
        <AjustesPreferencias />
      </MemoryRouter>
    )

    expect(screen.getByText(/ajustes/i)).toBeInTheDocument()
  })

  it('muestra los enlaces de privacidad y notificaciones', () => {
    render(
      <MemoryRouter>
        <AjustesPreferencias />
      </MemoryRouter>
    )

    expect(screen.getAllByText(/privacidad/i).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/notificaciones/i).length).toBeGreaterThan(0)
  })

  it('muestra toggles de configuración', () => {
    const { container } = render(
      <MemoryRouter>
        <AjustesPreferencias />
      </MemoryRouter>
    )

    const toggles = container.querySelectorAll('input[type="checkbox"]')
    expect(toggles.length).toBeGreaterThan(0)
  })

  it('permite interactuar con los toggles', async () => {
    const user = userEvent.setup()
    const { container } = render(
      <MemoryRouter>
        <AjustesPreferencias />
      </MemoryRouter>
    )

    const toggle = container.querySelector('input[type="checkbox"]')
    if (toggle) {
      const initialValue = (toggle as HTMLInputElement).checked
      await user.click(toggle)
      expect((toggle as HTMLInputElement).checked).not.toBe(initialValue)
    }
  })
})
