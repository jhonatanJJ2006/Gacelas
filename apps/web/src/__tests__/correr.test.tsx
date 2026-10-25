import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { act, screen, waitFor, within } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import { instalarGpsFalso, prepararApp, renderRuta } from '../test/utils'
import { useApp } from '../store/app'
import { descartarCarrera, tracker } from '../store/carrera'

describe('grabar una carrera con GPS', () => {
  let gps: ReturnType<typeof instalarGpsFalso>
  beforeEach(() => {
    prepararApp()
    useApp.setState((s) => ({ ajustes: { ...s.ajustes, cuentaRegresivaSeg: 0 } }))
    gps = instalarGpsFalso()
    localStorage.setItem(
      'gacelas:consentimientos',
      JSON.stringify([{ tipo: 'gps_carrera', version: 'v', otorgado: true, fechaMs: 1 }])
    )
  })
  afterEach(() => descartarCarrera())

  it('pide el consentimiento de GPS antes de la primera carrera', async () => {
    localStorage.removeItem('gacelas:consentimientos')
    renderRuta('/correr')
    await userEvent.click(await screen.findByRole('button', { name: 'Comenzar carrera' }))
    expect(screen.getByText('Usar tu ubicación durante la carrera')).toBeInTheDocument()
    expect(gps.geo.watchPosition).not.toHaveBeenCalled()
    await userEvent.click(screen.getByRole('button', { name: 'Aceptar y empezar' }))
    expect(await screen.findByText('En curso')).toBeInTheDocument()
  })

  it('Correr muestra la señal del GPS y empieza la carrera', async () => {
    renderRuta('/correr')
    expect(await screen.findByText('GPS Activado')).toBeInTheDocument()
    expect(screen.getByText(/Señal fuerte/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Comenzar carrera' }))
    expect(await screen.findByText('En curso')).toBeInTheDocument()
    expect(gps.geo.watchPosition).toHaveBeenCalled()
  })

  it('cuenta regresiva de ajustes antes de arrancar el GPS', async () => {
    useApp.setState((s) => ({ ajustes: { ...s.ajustes, cuentaRegresivaSeg: 3 } }))
    renderRuta('/correr')
    await screen.findByText('GPS Activado')
    await userEvent.click(screen.getByRole('button', { name: 'Comenzar carrera' }))
    expect(screen.getByTestId('cuenta-regresiva')).toHaveTextContent('3')
    expect(gps.geo.watchPosition).not.toHaveBeenCalled()
    await userEvent.click(screen.getByRole('button', { name: 'Empezar ya' }))
    expect(await screen.findByText('En curso')).toBeInTheDocument()
  })

  it('Correr avisa si el GPS falla', async () => {
    gps.geo.getCurrentPosition.mockImplementation(
      (_ok: unknown, mal: (e: GeolocationPositionError) => void) =>
        mal({ code: 1, PERMISSION_DENIED: 1, TIMEOUT: 3 } as GeolocationPositionError)
    )
    renderRuta('/correr')
    expect(await screen.findByText('GPS no disponible')).toBeInTheDocument()
    expect(screen.getByText(/denegado/)).toBeInTheDocument()
  })

  it('distancia y ritmo salen de los puntos GPS; pausa, termina y guarda', async () => {
    renderRuta('/correr')
    await userEvent.click(await screen.findByRole('button', { name: 'Comenzar carrera' }))
    await screen.findByText('En curso')
    act(() => {
      for (let i = 0; i <= 20; i++) gps.emitir(-4 + i * 0.0001, -79.2, 1000 + i * 4000)
    })
    expect(screen.getByTestId('distancia')).toHaveTextContent('0.22')
    expect(screen.getByTestId('ritmo')).not.toHaveTextContent('--:--')
    expect(screen.getByTestId('trazado-ruta')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Pausar' }))
    expect(screen.getByText('En pausa')).toBeInTheDocument()
    const pausa = screen.getByRole('dialog', { name: 'Carrera en pausa' })
    await userEvent.click(within(pausa).getByRole('button', { name: 'Reanudar' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Terminar carrera' }))
    await userEvent.click(screen.getByRole('button', { name: 'Terminar y guardar' }))
    expect(await screen.findByText('Carrera completada')).toBeInTheDocument()
    expect(screen.getByTestId('resumen-distancia')).toHaveTextContent('0.22')
    expect(gps.activos()).toBe(0)

    await userEvent.type(screen.getByLabelText('Título de la carrera'), 'Fondo Jipiro')
    await userEvent.click(screen.getByRole('checkbox'))
    await userEvent.click(screen.getByRole('button', { name: /guardar carrera/i }))
    expect(await screen.findByText('Fondo Jipiro')).toBeInTheDocument()
    expect(screen.getByText('Privada')).toBeInTheDocument()
    expect(useApp.getState().carreras).toHaveLength(1)
    expect(JSON.parse(localStorage.getItem('gacelas:carreras')!)[0].titulo).toBe('Fondo Jipiro')
    expect(tracker.estado.fase).toBe('inactiva')
  })

  it('cerrar en curso pide confirmacion y descarta', async () => {
    renderRuta('/correr')
    await userEvent.click(await screen.findByRole('button', { name: 'Comenzar carrera' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Salir de la carrera' }))
    await userEvent.click(screen.getByRole('button', { name: 'Seguir corriendo' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Salir de la carrera' }))
    await userEvent.click(screen.getByRole('button', { name: 'Descartar' }))
    await waitFor(() => expect(tracker.estado.fase).toBe('inactiva'))
    expect(useApp.getState().carreras).toHaveLength(0)
  })

  it('resumen: descartar no guarda; sin carrera terminada vuelve a inicio', async () => {
    renderRuta('/correr')
    await userEvent.click(await screen.findByRole('button', { name: 'Comenzar carrera' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Terminar carrera' }))
    await userEvent.click(screen.getByRole('button', { name: 'Terminar y guardar' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Descartar' }))
    await waitFor(() => expect(tracker.estado.fase).toBe('inactiva'))
    expect(useApp.getState().carreras).toHaveLength(0)
  })

  it('en curso sin carrera activa vuelve a Correr', async () => {
    renderRuta('/correr/en-curso')
    expect(await screen.findByText('¿Listo para correr?')).toBeInTheDocument()
  })
})
