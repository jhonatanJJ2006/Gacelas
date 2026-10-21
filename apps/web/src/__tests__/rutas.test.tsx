import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, screen, within } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import { instalarGpsFalso, prepararApp, renderRuta } from '../test/utils'
import { useApp } from '../store/app'
import { descartarCarrera, tracker } from '../store/carrera'

describe('rutas', () => {
  beforeEach(() => prepararApp())
  afterEach(() => descartarCarrera())

  it('pestañas, busqueda y guardar rutas', async () => {
    renderRuta('/rutas')
    expect(await screen.findByText('Parque Jipiro')).toBeInTheDocument()
    expect(screen.getAllByRole('article')).toHaveLength(4)
    await userEvent.click(screen.getByRole('tab', { name: 'Mis Rutas' }))
    expect(screen.getAllByRole('article')).toHaveLength(1)
    expect(screen.getByText('Campus UTPL - San Cayetano')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('tab', { name: 'Guardadas (0)' }))
    expect(screen.getByTestId('rutas-vacio')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('tab', { name: 'Explorar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Guardar Río Zamora' }))
    expect(screen.getByRole('tab', { name: 'Guardadas (1)' })).toBeInTheDocument()
    expect(JSON.parse(localStorage.getItem('gacelas:rutas-guardadas')!)).toEqual(['2'])
    await userEvent.click(screen.getByRole('tab', { name: 'Guardadas (1)' }))
    expect(screen.getAllByRole('article')).toHaveLength(1)
    await userEvent.click(screen.getByRole('button', { name: 'Quitar Río Zamora de guardadas' }))
    expect(screen.queryAllByRole('article')).toHaveLength(0)
    await userEvent.click(screen.getByRole('tab', { name: 'Explorar' }))
    await userEvent.type(screen.getByLabelText('Buscar rutas'), 'villonaco')
    expect(screen.getAllByRole('article')).toHaveLength(1)
  })

  it('muestra millas si las unidades son imperiales', async () => {
    prepararApp({ ajustes: { unidadesMetricas: false } })
    renderRuta('/rutas')
    const tarjeta = (await screen.findByText('Parque Jipiro')).closest('article')!
    expect(within(tarjeta).getByText(/3\.2 mi/)).toBeInTheDocument()
  })

  it('abrir una ruta la preselecciona en el buscador y navega', async () => {
    const gps = instalarGpsFalso()
    renderRuta('/rutas')
    await userEvent.click(await screen.findByRole('link', { name: 'Abrir Cerro Villonaco' }))
    await screen.findByTestId('correr-ruta', {}, { timeout: 4000 })
    await userEvent.click(screen.getByTestId('correr-ruta'))
    const opcion = await screen.findByRole('button', { name: /Cerro Villonaco/, pressed: true })
    expect(opcion).toBeInTheDocument()
    expect(screen.getByText('Ruta seleccionada')).toBeInTheDocument()
    await userEvent.type(screen.getByPlaceholderText('¿A dónde quieres correr?'), 'zzzz')
    expect(screen.getByRole('button', { name: /comenzar navegación/i })).toBeEnabled()
    await userEvent.clear(screen.getByPlaceholderText('¿A dónde quieres correr?'))
    await userEvent.click(
      screen.getByRole('button', { name: /Ruta Universitaria Directa/, pressed: false })
    )
    await userEvent.click(screen.getByRole('button', { name: /comenzar navegación/i }))
    expect(await screen.findByText(/siguiente giro en/i)).toBeInTheDocument()
    expect(screen.getByText('Ruta Universitaria Directa')).toBeInTheDocument()
    expect(gps.geo.watchPosition).toHaveBeenCalled()
    expect(screen.getByTestId('giro-metros')).toHaveTextContent('500 metros')
    act(() => {
      for (let i = 0; i <= 10; i++) gps.emitir(-4 + i * 0.0001, -79.2, 1000 + i * 4000)
    })
    expect(screen.getByTestId('giro-metros')).toHaveTextContent('389 metros')
    await userEvent.click(screen.getByRole('button', { name: 'Carrera' }))
    expect(screen.getByRole('button', { name: 'Carrera' })).toHaveAttribute('aria-pressed', 'true')
    await userEvent.click(screen.getByRole('button', { name: /ver cronómetro/i }))
    expect(await screen.findByText('Cronómetro Profesional')).toBeInTheDocument()
  })

  it('cerrar la navegacion sin tiempo vuelve a rutas', async () => {
    instalarGpsFalso()
    renderRuta('/rutas/navegacion')
    await userEvent.click(await screen.findByRole('button', { name: 'Terminar navegación' }))
    expect(await screen.findByText('Rutas de Loja')).toBeInTheDocument()
  })

  it('cerrar la navegacion con tiempo lleva al resumen', async () => {
    instalarGpsFalso()
    let ahora = 1000
    const reloj = vi.spyOn(performance, 'now').mockImplementation(() => ahora)
    renderRuta('/rutas/navegacion')
    await screen.findByRole('button', { name: 'Terminar navegación' })
    act(() => {
      ahora = 6000
      tracker.actualizarTiempo()
    })
    reloj.mockRestore()
    await userEvent.click(screen.getByRole('button', { name: 'Terminar navegación' }))
    expect(await screen.findByText('Carrera completada')).toBeInTheDocument()
    expect(useApp.getState().carreras).toHaveLength(0)
  })
})
