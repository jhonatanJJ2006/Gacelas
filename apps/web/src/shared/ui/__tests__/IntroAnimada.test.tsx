import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  CLAVE_INTRO,
  debeMostrarIntro,
  DURACION_INTRO_S,
  e,
  io,
  marcarIntroVista,
} from '../../lib/intro'
import { IntroAnimada } from '../IntroAnimada'

function almacen(): Storage {
  const m = new Map<string, string>()
  return {
    getItem: (k: string) => m.get(k) ?? null,
    setItem: (k: string, v: string) => void m.set(k, v),
  } as Storage
}

function movimientoReducido(si: boolean) {
  window.matchMedia = vi
    .fn()
    .mockReturnValue({ matches: si }) as unknown as typeof window.matchMedia
}

describe('logica de la intro', () => {
  it('se muestra una vez por sesion', () => {
    const a = almacen()
    expect(debeMostrarIntro(a, false)).toBe(true)
    marcarIntroVista(a)
    expect(a.getItem(CLAVE_INTRO)).toBe('1')
    expect(debeMostrarIntro(a, false)).toBe(false)
  })
  it('se omite con prefers-reduced-motion', () => {
    expect(debeMostrarIntro(almacen(), true)).toBe(false)
  })
  it('no falla si el almacenamiento esta bloqueado', () => {
    const roto = {
      getItem: () => {
        throw new Error('x')
      },
      setItem: () => {
        throw new Error('x')
      },
    } as unknown as Storage
    expect(debeMostrarIntro(roto, false)).toBe(false)
    expect(() => marcarIntroVista(roto)).not.toThrow()
  })
  it('dura como maximo unos 3 s y usa las curvas del prototipo', () => {
    expect(DURACION_INTRO_S).toBeLessThanOrEqual(3)
    expect(e(0)).toBe(0)
    expect(e(1)).toBe(1)
    expect(io(0.5)).toBe(0.5)
  })
})

describe('<IntroAnimada />', () => {
  beforeEach(() => {
    sessionStorage.clear()
    HTMLCanvasElement.prototype.getContext = vi.fn().mockReturnValue(null) as never
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('aparece, se salta con un clic y no reaparece en la misma sesion', () => {
    movimientoReducido(false)
    const { unmount } = render(<IntroAnimada />)
    fireEvent.click(screen.getByTestId('intro-animada'))
    expect(screen.queryByTestId('intro-animada')).toBeNull()
    unmount()
    render(<IntroAnimada />)
    expect(screen.queryByTestId('intro-animada')).toBeNull()
  })

  it('se salta con una tecla', () => {
    movimientoReducido(false)
    render(<IntroAnimada />)
    act(() => {
      fireEvent.keyDown(window, { key: 'Escape' })
    })
    expect(screen.queryByTestId('intro-animada')).toBeNull()
  })

  it('no aparece con movimiento reducido', () => {
    movimientoReducido(true)
    render(<IntroAnimada />)
    expect(screen.queryByTestId('intro-animada')).toBeNull()
  })
})
