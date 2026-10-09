import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Eventos } from '../Eventos';

describe('Eventos', () => {
  it('renderiza sin errores', () => {
    render(
      <MemoryRouter>
        <Eventos />
      </MemoryRouter>
    );

    expect(screen.getByText('Eventos')).toBeInTheDocument();
  });
});
