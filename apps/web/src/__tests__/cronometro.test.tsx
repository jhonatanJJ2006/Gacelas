import { act, screen } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { descartarCarrera, tracker } from '../contexts/actividad/application/carreraStore'
import { useApp } from '../store/app'
import { instalarGpsFalso, prepararApp, renderRuta } from '../test/utils'

const osciladores: { frecuencia: number }[] = []
class AudioContextFalso {
  state = 'running'
  currentTime = 0
  destination = {}
  resume = vi.fn()
  createOscillator() {
    const o = {
      frequency: { value: 0 },
      type: '',
      connect: (n: unknown) => n,
      start: () => osciladores.push({ frecuencia: o.frequency.value }),
      stop: vi.fn(),
    }
    return o
  }
  createGain() {
    return {
      gain: { setValueAtTime: vi.fn(), exponentialRampToValueAtTime: vi.fn() },
      connect: (n: unknown) => n,
    }
  }
}

describe('cronometro', () => {
  beforeEach(() => {
    prepararApp()
    instalarGpsFalso()
    osciladores.length = 0
    Object.defineProperty(window, 'AudioContext', { value: AudioContextFalso, configurable: true })
    Object.defineProperty(navigator, 'vibrate', { value: vi.fn(), configurable: true })
  })
  afterEach(() => {
    descartarCarrera()
    vi.useRealTimers()
  })

  it('inicia la carrera, marca vueltas y termina en el resumen', async () => {
    renderRuta('/correr/cronometro')
    expect(await screen.findByText('Listo')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /vuelta/i })).toBeDisabled()
    await userEvent.click(screen.getByRole('button', { name: 'Iniciar' }))
    expect(screen.getByText('En marcha')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /vuelta/i }))
    expect(screen.getByText('Vuelta 1')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Pausar' }))
    expect(screen.getByText('En pausa')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /fin/i }))
    expect(await screen.findByText('Carrera completada')).toBeInTheDocument()
  })

  it('modo pacer compara con el objetivo y se puede ajustar', async () => {
    renderRuta('/correr/cronometro')
    await userEvent.click(await screen.findByRole('button', { name: /ritmo pacer/i }))
    expect(screen.getByText(/Esperando ritmo/)).toBeInTheDocument()
    expect(screen.getByText('5:00 /km')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Objetivo más rápido' }))
    expect(screen.getByText('4:55 /km')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Objetivo más lento' }))
    await userEvent.click(screen.getByRole('button', { name: 'Objetivo más lento' }))
    expect(screen.getByText('5:05 /km')).toBeInTheDocument()
  })

  it('intervalos: configura, guarda y cambia de fase con aviso', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    renderRuta('/correr/cronometro')
    await user.click(await screen.findByRole('button', { name: /fartlek/i }))
    expect(screen.getByTestId('panel-intervalos')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Menos Trabajo' }))
    await user.click(screen.getByRole('button', { name: 'Menos Rondas' }))
    await user.click(screen.getByRole('switch', { name: 'Vibración' }))
    expect(useApp.getState().ajustes.intervalos).toMatchObject({
      trabajoSeg: 50,
      rondas: 5,
      vibracion: false,
    })
    expect(JSON.parse(localStorage.getItem('gacelas:ajustes')!).intervalos.trabajoSeg).toBe(50)
    for (let i = 0; i < 4; i++)
      await user.click(screen.getByRole('button', { name: 'Menos Trabajo' }))
    for (let i = 0; i < 3; i++)
      await user.click(screen.getByRole('button', { name: 'Menos Descanso' }))
    for (let i = 0; i < 3; i++)
      await user.click(screen.getByRole('button', { name: 'Menos Rondas' }))
    // Trabajo 10 s, descanso 0 s, 2 rondas
    await user.click(screen.getByRole('button', { name: 'Iniciar intervalos' }))
    expect(screen.getByText('Trabajo')).toBeInTheDocument()
    const antes = osciladores.length
    await act(async () => {
      vi.advanceTimersByTime(11_000)
    })
    expect(screen.getByText('Ronda 2 de 2')).toBeInTheDocument()
    expect(osciladores.length).toBeGreaterThan(antes)
    await act(async () => {
      vi.advanceTimersByTime(11_000)
    })
    expect(screen.getByRole('button', { name: 'Iniciar intervalos' })).toBeInTheDocument()
  })

  it('metronomo suena al BPM elegido y guarda el BPM', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    renderRuta('/correr/cronometro')
    await user.click(await screen.findByRole('tab', { name: /metrónomo/i }))
    expect(screen.getByTestId('bpm')).toHaveTextContent('168')
    await user.click(screen.getByRole('button', { name: '180' }))
    expect(screen.getByTestId('bpm')).toHaveTextContent('180')
    expect(useApp.getState().ajustes.metronomoBpm).toBe(180)
    await user.click(screen.getByRole('button', { name: 'Iniciar metrónomo' }))
    await act(async () => {
      vi.advanceTimersByTime(1000)
    })
    expect(osciladores.length).toBeGreaterThanOrEqual(3)
    expect(osciladores[0].frecuencia).toBe(1320)
    await user.click(screen.getByRole('button', { name: 'Detener metrónomo' }))
    const n = osciladores.length
    await act(async () => {
      vi.advanceTimersByTime(1000)
    })
    expect(osciladores.length).toBe(n)
  })

  it('splits muestra los parciales reales', async () => {
    tracker.iniciar()
    tracker.vuelta()
    renderRuta('/correr/cronometro')
    expect(await screen.findByText('Vuelta 1')).toBeInTheDocument()
  })
})
