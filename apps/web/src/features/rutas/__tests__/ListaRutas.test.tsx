import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ListaRutas } from '../ListaRutas';

describe('ListaRutas', () => {
  it('renderiza sin errores', () => {
    render(
      <MemoryRouter>
        <ListaRutas />
      </MemoryRouter>
    );

    expect(screen.getByText('Rutas de Loja')).toBeInTheDocument();
  });

  it('muestra el botón de buscar rutas', () => {
    render(
      <MemoryRouter>
        <ListaRutas />
      </MemoryRouter>
    );

    expect(screen.getByPlaceholderText('Buscar rutas...')).toBeInTheDocument();
  });
});
