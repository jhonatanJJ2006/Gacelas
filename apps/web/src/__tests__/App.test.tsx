import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import App from '../App';

describe('Router', () => {
  it('renderiza la ruta de bienvenida', () => {
    render(
      <MemoryRouter initialEntries={['/bienvenida']}>
        <App />
      </MemoryRouter>
    );

    expect(screen.getByText(/bienvenid/i)).toBeInTheDocument();
  });

  it('redirige la ruta raíz a /inicio', async () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>
    );

    expect(await screen.findByText(/buenos días/i)).toBeInTheDocument();
  });

  it('renderiza la ruta de inicio de sesión', () => {
    render(
      <MemoryRouter initialEntries={['/inicio-sesion']}>
        <App />
      </MemoryRouter>
    );

    expect(screen.getByText(/bienvenido de vuelta/i)).toBeInTheDocument();
  });

  it('renderiza la ruta de registro', () => {
    render(
      <MemoryRouter initialEntries={['/registro']}>
        <App />
      </MemoryRouter>
    );

    expect(screen.getByText(/únete a la manada/i)).toBeInTheDocument();
  });

  it('renderiza la ruta de recuperar contraseña', () => {
    render(
      <MemoryRouter initialEntries={['/recuperar-contrasena']}>
        <App />
      </MemoryRouter>
    );

    expect(screen.getByText(/recuperar contraseña/i)).toBeInTheDocument();
  });

  it('renderiza rutas protegidas con AppLayout', async () => {
    render(
      <MemoryRouter initialEntries={['/inicio']}>
        <App />
      </MemoryRouter>
    );

    expect(await screen.findByText('Gacelas Loja')).toBeInTheDocument();
  });

  it('redirige rutas desconocidas a bienvenida', () => {
    render(
      <MemoryRouter initialEntries={['/ruta-inexistente']}>
        <App />
      </MemoryRouter>
    );

    expect(screen.getByText(/bienvenid/i)).toBeInTheDocument();
  });
});
