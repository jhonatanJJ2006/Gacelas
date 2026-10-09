import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Registro } from '../Registro';

describe('Registro', () => {
  it('renderiza el formulario de registro', async () => {
    render(
      <MemoryRouter>
        <Registro />
      </MemoryRouter>
    );

    expect(screen.getByText(/crea tu cuenta/i)).toBeInTheDocument();
  });

  it('muestra todos los campos del formulario', () => {
    render(
      <MemoryRouter>
        <Registro />
      </MemoryRouter>
    );

    expect(screen.getByLabelText(/nombre completo/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/correo electrónico/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/contraseña/i)).toBeInTheDocument();
  });

  it('muestra el enlace para iniciar sesión', () => {
    render(
      <MemoryRouter>
        <Registro />
      </MemoryRouter>
    );

    expect(screen.getByText(/¿ya tienes cuenta?/i)).toBeInTheDocument();
    const iniciarSesionLink = screen.getByText(/iniciar sesión/i);
    expect(iniciarSesionLink.closest('a')).toHaveAttribute('href', '/inicio-sesion');
  });
});
