import { screen, waitFor, within } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { prepararApp, renderRuta } from '../test/utils'

function simularAppInstalada(instalada: boolean) {
  window.matchMedia = vi.fn((consulta: string) => ({
    matches: instalada && consulta === '(display-mode: standalone)',
    media: consulta,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  })) as unknown as typeof window.matchMedia
}

describe('landing publica en /', () => {
  beforeEach(() => prepararApp({ sesion: null }))
  afterEach(() => {
    // jsdom no trae matchMedia: se deja como estaba.
    Reflect.deleteProperty(window, 'matchMedia')
  })

  it('sin sesion, "/" muestra la landing y no redirige a bienvenida', async () => {
    renderRuta('/')
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Corre con la manada de Loja' })
    ).toBeInTheDocument()
    expect(screen.getByTestId('landing')).toBeInTheDocument()
  })

  it('lleva al registro y al inicio de sesion con enlaces, no con modales', async () => {
    renderRuta('/')
    await screen.findByTestId('landing')
    const registro = screen.getAllByRole('link', { name: /crear cuenta/i })
    expect(registro.length).toBeGreaterThanOrEqual(2)
    registro.forEach((a) => expect(a).toHaveAttribute('href', '/registro'))
    screen
      .getAllByRole('link', { name: /iniciar sesión|ya tengo cuenta/i })
      .forEach((a) => expect(a).toHaveAttribute('href', '/inicio-sesion'))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('enlaza los documentos legales desde el pie y desde privacidad', async () => {
    renderRuta('/')
    await screen.findByTestId('landing')
    const pie = within(screen.getByRole('navigation', { name: 'Documentos legales' }))
    expect(pie.getByRole('link', { name: 'Términos y condiciones' })).toHaveAttribute(
      'href',
      '/legal/terminos'
    )
    expect(pie.getByRole('link', { name: 'Política de privacidad' })).toHaveAttribute(
      'href',
      '/legal/privacidad'
    )
    expect(screen.getByRole('link', { name: 'Leer la política de privacidad' })).toHaveAttribute(
      'href',
      '/legal/privacidad'
    )
  })

  it('cada seccion tiene su titulo y la pagina solo tiene un h1', async () => {
    renderRuta('/')
    await screen.findByTestId('landing')
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1)
    for (const titulo of ['Hecho para correr en Loja', 'Tu ubicación es tuya', 'Sal a correr hoy'])
      expect(screen.getByRole('heading', { level: 2, name: titulo })).toBeInTheDocument()
  })

  it('atribuye los trazados de OpenStreetMap y los datos del ECU 911', async () => {
    renderRuta('/')
    await screen.findByTestId('landing')
    expect(screen.getByText(/colaboradores de OpenStreetMap/)).toBeInTheDocument()
    expect(screen.getByText(/ECU 911, licencia CC-BY/)).toBeInTheDocument()
  })

  it('la ilustracion tiene una descripcion accesible', async () => {
    renderRuta('/')
    const escena = await screen.findByRole('img', { name: /ruta GPS/ })
    expect(escena).toHaveAttribute('data-testid', 'landing-escena')
  })

  it('con sesion, "/" lleva a inicio', async () => {
    prepararApp()
    renderRuta('/')
    expect(await screen.findByTestId('inicio-escenario')).toBeInTheDocument()
    expect(screen.queryByTestId('landing')).not.toBeInTheDocument()
  })

  it('la app instalada (PWA) salta la landing y va a bienvenida', async () => {
    simularAppInstalada(true)
    renderRuta('/')
    expect(await screen.findByRole('button', { name: /saltar/i })).toBeInTheDocument()
    expect(screen.queryByTestId('landing')).not.toBeInTheDocument()
  })

  it('en el navegador (no instalada) se queda en la landing', async () => {
    simularAppInstalada(false)
    renderRuta('/')
    expect(await screen.findByTestId('landing')).toBeInTheDocument()
  })

  it('pone el titulo de la pestana y lo devuelve al salir', async () => {
    document.title = 'Runcity'
    const { unmount } = renderRuta('/')
    await screen.findByTestId('landing')
    await waitFor(() => expect(document.title).toBe('Runcity: corre con la manada de Loja'))
    unmount()
    expect(document.title).toBe('Runcity')
  })

  it('crear cuenta lleva al formulario de registro', async () => {
    renderRuta('/')
    const [crear] = await screen.findAllByRole('link', { name: /crear cuenta/i })
    await userEvent.click(crear)
    expect(await screen.findByLabelText('Nombre completo')).toBeInTheDocument()
  })
})
