import { act, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { BotonSOS, useSos } from '../contexts/seguridad'

describe('Boton SOS', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['setTimeout', 'requestAnimationFrame', 'performance'] })
    useSos.setState({ alertaId: null, activando: false, error: null })
  })

  it('un toque corto no activa: solo explica que hay que mantenerlo', () => {
    const activar = vi.fn()
    useSos.setState({ activar })
    render(
      <MemoryRouter>
        <BotonSOS posicion={{ lat: -4, lng: -79.2 }} />
      </MemoryRouter>
    )
    const b = screen.getByTestId('boton-sos')
    fireEvent.pointerDown(b)
    act(() => vi.advanceTimersByTime(200))
    fireEvent.pointerUp(b)
    expect(activar).not.toHaveBeenCalled()
    expect(screen.getByRole('status')).toHaveTextContent('Mantén pulsado 3 s')
  })

  it('mantener 3 segundos activa el SOS y ofrece llamar al 911', () => {
    const activar = vi.fn(async () => 'id-1')
    useSos.setState({ activar })
    render(
      <MemoryRouter>
        <BotonSOS posicion={{ lat: -4, lng: -79.2 }} />
      </MemoryRouter>
    )
    fireEvent.pointerDown(screen.getByTestId('boton-sos'))
    act(() => vi.advanceTimersByTime(3200))
    expect(activar).toHaveBeenCalledWith({ lat: -4, lng: -79.2 })
    expect(screen.getByTestId('llamar-911')).toHaveAttribute('href', 'tel:911')
    vi.useRealTimers()
  })
})
