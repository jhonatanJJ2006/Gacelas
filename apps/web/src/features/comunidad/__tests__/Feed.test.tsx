import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Feed } from '../Feed';

describe('Feed', () => {
  it('renderiza sin errores', () => {
    render(
      <MemoryRouter>
        <Feed />
      </MemoryRouter>
    );

    expect(screen.getByText(/comunidad/i)).toBeInTheDocument();
  });

  it('muestra el botón de eventos', () => {
    render(
      <MemoryRouter>
        <Feed />
      </MemoryRouter>
    );

    expect(screen.getByText('Eventos')).toBeInTheDocument();
  });
});
