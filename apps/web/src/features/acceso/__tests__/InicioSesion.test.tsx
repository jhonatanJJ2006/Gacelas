import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { InicioSesion } from '../InicioSesion'

const mockNavigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

describe('InicioSesion', () => {
  it('renderiza el formulario de inicio de sesión', () => {
    render(
      <MemoryRouter>
        <InicioSesion />
      </MemoryRouter>
    )

    expect(screen.getByText('Bienvenido de vuelta')).toBeInTheDocument()
    expect(screen.getByLabelText('Correo electrónico')).toBeInTheDocument()
    expect(screen.getByLabelText('Contraseña')).toBeInTheDocument()
  })

  it('muestra el logo de Gacelas', () => {
    render(
      <MemoryRouter>
        <InicioSesion />
      </MemoryRouter>
    )

    expect(screen.getByText('GACELAS')).toBeInTheDocument()
    expect(screen.getByText('LOJA, ECUADOR')).toBeInTheDocument()
  })

  it('permite ingresar email y contraseña', async () => {
    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <InicioSesion />
      </MemoryRouter>
    )

    const emailInput = screen.getByLabelText('Correo electrónico')
    const passwordInput = screen.getByLabelText('Contraseña')

    await user.type(emailInput, 'test@example.com')
    await user.type(passwordInput, 'password123')

    expect(emailInput).toHaveValue('test@example.com')
    expect(passwordInput).toHaveValue('password123')
  })

  it('muestra el enlace de recuperar contraseña', () => {
    render(
      <MemoryRouter>
        <InicioSesion />
      </MemoryRouter>
    )

    const recuperarLink = screen.getByText('¿Olvidaste tu contraseña?')
    expect(recuperarLink).toBeInTheDocument()
    expect(recuperarLink.closest('a')).toHaveAttribute('href', '/recuperar-contrasena')
  })

  it('muestra el enlace para crear cuenta', () => {
    render(
      <MemoryRouter>
        <InicioSesion />
      </MemoryRouter>
    )

    expect(screen.getByText('¿No tienes cuenta?')).toBeInTheDocument()
    const crearCuentaLink = screen.getByText('Crear cuenta')
    expect(crearCuentaLink.closest('a')).toHaveAttribute('href', '/registro')
  })

  it('tiene campos requeridos', () => {
    render(
      <MemoryRouter>
        <InicioSesion />
      </MemoryRouter>
    )

    const emailInput = screen.getByLabelText('Correo electrónico')
    const passwordInput = screen.getByLabelText('Contraseña')

    expect(emailInput).toBeRequired()
    expect(passwordInput).toBeRequired()
  })

  it('el email debe ser de tipo email', () => {
    render(
      <MemoryRouter>
        <InicioSesion />
      </MemoryRouter>
    )

    const emailInput = screen.getByLabelText('Correo electrónico')
    expect(emailInput).toHaveAttribute('type', 'email')
  })

  it('la contraseña debe ser de tipo password', () => {
    render(
      <MemoryRouter>
        <InicioSesion />
      </MemoryRouter>
    )

    const passwordInput = screen.getByLabelText('Contraseña')
    expect(passwordInput).toHaveAttribute('type', 'password')
  })
})
