import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import { prepararApp, renderRuta, PERFIL_PRUEBA } from '../test/utils'
import { MapaGoogle } from '../components/MapaGoogle'
import { trazoDeOpcion } from '../lib/mapas'

vi.mock('@vis.gl/react-google-maps', () => ({
  APIProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Map: ({ children }: { children: React.ReactNode }) => <div data-testid="gmap">{children}</div>,
  useMap: () => null,
}))

describe('Continuar con Google', () => {
  beforeEach(() => prepararApp({ sesion: null }))

  it('sin servidor explica que no esta disponible', async () => {
    renderRuta('/inicio-sesion')
    await userEvent.click(await screen.findByRole('button', { name: /continuar con google/i }))
    expect(await screen.findByRole('alert')).toHaveTextContent(/servidor/)
  })

  it('aparece tambien en bienvenida y registro', async () => {
    renderRuta('/registro')
    expect(await screen.findByRole('button', { name: /continuar con google/i })).toBeInTheDocument()
  })
})

describe('nueva contrasena', () => {
  it('valida y guarda la nueva contrasena', async () => {
    prepararApp({ sesion: PERFIL_PRUEBA })
    renderRuta('/nueva-contrasena')
    await userEvent.type(await screen.findByLabelText('Contraseña nueva'), 'corta')
    await userEvent.click(screen.getByRole('button', { name: /guardar contraseña/i }))
    expect(await screen.findByRole('alert')).toHaveTextContent(/8 caracteres/)
    await userEvent.clear(screen.getByLabelText('Contraseña nueva'))
    await userEvent.type(screen.getByLabelText('Contraseña nueva'), 'gacela456')
    await userEvent.click(screen.getByRole('button', { name: /guardar contraseña/i }))
    await waitFor(() => expect(screen.queryByLabelText('Contraseña nueva')).not.toBeInTheDocument())
  })
})

describe('MapaGoogle', () => {
  it('sin clave muestra el mapa dibujado', () => {
    render(<MapaGoogle puntos={[]} alternativa={<span>dibujado</span>} />)
    expect(screen.getByText('dibujado')).toBeInTheDocument()
  })

  it('con clave usa Google Maps', () => {
    vi.stubEnv('MODE', 'production')
    vi.stubEnv('VITE_GOOGLE_MAPS_API_KEY', 'clave-de-prueba')
    render(<MapaGoogle puntos={trazoDeOpcion('ruta-1')} alternativa={<span>dibujado</span>} />)
    expect(screen.getByTestId('gmap')).toBeInTheDocument()
    vi.unstubAllEnvs()
  })

  it('trazos de rutas de ejemplo', () => {
    expect(trazoDeOpcion('primary').length).toBeGreaterThan(1)
    expect(trazoDeOpcion(null)).toEqual([])
    expect(trazoDeOpcion('zzz')).toEqual([])
  })
})
