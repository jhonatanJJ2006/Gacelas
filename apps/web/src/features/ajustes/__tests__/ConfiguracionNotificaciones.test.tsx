import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ConfiguracionNotificaciones } from '../ConfiguracionNotificaciones';

describe('ConfiguracionNotificaciones', () => {
  it('renderiza sin errores', () => {
    render(
      <MemoryRouter>
        <ConfiguracionNotificaciones />
      </MemoryRouter>
    );

    expect(screen.getByText('Notificaciones')).toBeInTheDocument();
  });

  it('muestra filtros de notificaciones', () => {
    const { container } = render(
      <MemoryRouter>
        <ConfiguracionNotificaciones />
      </MemoryRouter>
    );

    const toggles = container.querySelectorAll('input[type="checkbox"]');
    expect(toggles.length).toBeGreaterThan(0);
  });
});
