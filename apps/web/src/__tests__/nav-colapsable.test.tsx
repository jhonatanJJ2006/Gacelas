import { beforeEach, describe, expect, it } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { NavColapsable, type ItemNav } from '../shared/ui/navegacion/NavColapsable'

const items = (ins?: number): ItemNav[] => [
  { id: 'a', etiqueta: 'Inicio', a: '/x', icono: 'home', grupo: '1', activoEn: (p) => p === '/x' },
  { id: 'b', etiqueta: 'Casos', a: '/x/casos', icono: 'shield', grupo: '1', insignia: ins },
  { id: 'c', etiqueta: 'Ajustes', a: '/x/ajustes', icono: 'tune', grupo: '2' },
]

const montar = (ruta: string, ins?: number, clave = 'test.nav') =>
  render(
    <MemoryRouter initialEntries={[ruta]}>
      <NavColapsable
        items={items(ins)}
        usuario={{ nombre: 'Ana Perez', detalle: 'Admin' }}
        claveAlmacen={clave}
      />
    </MemoryRouter>
  )

describe('NavColapsable', () => {
  beforeEach(() => localStorage.clear())

  it('marca la seccion activa y muestra su globo', () => {
    montar('/x/casos')
    expect(screen.getByTestId('nav-b')).toHaveAttribute('aria-current', 'page')
    expect(screen.getByTestId('nav-a')).not.toHaveAttribute('aria-current')
  })

  it('solo muestra insignia con un conteo real mayor que cero', () => {
    const { unmount } = montar('/x', 0)
    expect(screen.queryByTestId('nav-ins-b')).toBeNull()
    unmount()
    montar('/x', 5)
    expect(screen.getByTestId('nav-ins-b')).toHaveTextContent('5')
  })

  it('persiste el estado colapsado', () => {
    const { unmount } = montar('/x')
    const riel = screen.getByTestId('nav-riel')
    expect(riel).toHaveAttribute('data-expandido', 'false')
    fireEvent.click(screen.getByTestId('nav-alternar'))
    expect(riel).toHaveAttribute('data-expandido', 'true')
    expect(localStorage.getItem('test.nav')).toBe('0')
    unmount()
    montar('/x')
    expect(screen.getByTestId('nav-riel')).toHaveAttribute('data-expandido', 'true')
  })

  it('el buscador Ctrl K abre la paleta y filtra por etiqueta', () => {
    montar('/x')
    fireEvent.keyDown(window, { key: 'k', ctrlKey: true })
    const campo = screen.getByLabelText('Buscar sección')
    fireEvent.change(campo, { target: { value: 'ajus' } })
    expect(screen.getByRole('dialog').textContent).toContain('Ajustes')
    expect(screen.getByRole('dialog').textContent).not.toContain('Casos')
  })
})
