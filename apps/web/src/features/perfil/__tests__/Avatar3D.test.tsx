import { describe, it, expect } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Avatar3D } from '../Avatar3D';

describe('Avatar3D', () => {
  it('renderiza sin errores', async () => {
    render(
      <MemoryRouter>
        <Avatar3D />
      </MemoryRouter>
    );

    await waitFor(() => {
      const canvas = screen.queryByTestId('three-canvas');
      const fallback = screen.queryByText('Cargando avatar 3D...');
      expect(canvas || fallback).toBeTruthy();
    });
  });
});
