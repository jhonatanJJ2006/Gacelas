import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AppLayout } from '../AppLayout';

describe('AppLayout', () => {
  it('renderiza el header con logo y notificaciones', () => {
    render(
      <MemoryRouter initialEntries={['/inicio']}>
        <AppLayout />
      </MemoryRouter>
    );

    expect(screen.getByText('Gacelas Loja')).toBeInTheDocument();
    expect(screen.getByLabelText('Notificaciones')).toBeInTheDocument();
  });

  it('renderiza la navegación inferior con todas las opciones', () => {
    render(
      <MemoryRouter initialEntries={['/inicio']}>
        <AppLayout />
      </MemoryRouter>
    );

    expect(screen.getByText('Inicio')).toBeInTheDocument();
    expect(screen.getByText('Rutas')).toBeInTheDocument();
    expect(screen.getByText('Comunidad')).toBeInTheDocument();
    expect(screen.getByText('Perfil')).toBeInTheDocument();
  });

  it('marca como activa la ruta actual', () => {
    render(
      <MemoryRouter initialEntries={['/inicio']}>
        <AppLayout />
      </MemoryRouter>
    );

    const inicioLink = screen.getByText('Inicio').closest('a');
    expect(inicioLink).toHaveClass('text-primary');
  });

  it('renderiza el botón principal de correr', () => {
    render(
      <MemoryRouter initialEntries={['/inicio']}>
        <AppLayout />
      </MemoryRouter>
    );

    const correrButton = screen.getAllByRole('link').find(
      link => link.getAttribute('href') === '/correr'
    );
    expect(correrButton).toBeInTheDocument();
  });

  it('tiene el badge de notificación visible', () => {
    const { container } = render(
      <MemoryRouter initialEntries={['/inicio']}>
        <AppLayout />
      </MemoryRouter>
    );

    const badge = container.querySelector('.bg-primary-container.ring-2');
    expect(badge).toBeInTheDocument();
  });
});
