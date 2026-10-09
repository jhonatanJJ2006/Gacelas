import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { RecuperarContrasena } from '../RecuperarContrasena';

describe('RecuperarContrasena', () => {
  it('renderiza el formulario de recuperación', () => {
    render(
      <MemoryRouter>
        <RecuperarContrasena />
      </MemoryRouter>
    );

    expect(screen.getByText(/recuperar contraseña/i)).toBeInTheDocument();
  });

  it('muestra el campo de email', () => {
    render(
      <MemoryRouter>
        <RecuperarContrasena />
      </MemoryRouter>
    );

    expect(screen.getByLabelText(/correo electrónico/i)).toBeInTheDocument();
  });

  it('muestra el botón para enviar enlace', () => {
    render(
      <MemoryRouter>
        <RecuperarContrasena />
      </MemoryRouter>
    );

    expect(screen.getByText(/enviar enlace/i)).toBeInTheDocument();
  });
});
