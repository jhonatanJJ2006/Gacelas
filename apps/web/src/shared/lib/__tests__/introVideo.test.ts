import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  CLAVE_INTRO,
  debeMostrarIntro,
  DURACION_INTRO_S,
  ESPERA_PRIMER_CUADRO_MS,
  marcarIntroVista,
  UMBRAL_AUSENCIA_MS,
} from '../intro'
import { iniciarIntroVideo } from '../introVideo'

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
  it('se muestra al abrir y se repite solo tras 30 s fuera', () => {
    const a = almacen()
    expect(debeMostrarIntro(a, false, 1_000)).toBe(true)
    marcarIntroVista(a, 1_000)
    expect(a.getItem(CLAVE_INTRO)).toBe('1000')
    expect(debeMostrarIntro(a, false, 1_000 + UMBRAL_AUSENCIA_MS - 1)).toBe(false)
    expect(debeMostrarIntro(a, false, 1_000 + UMBRAL_AUSENCIA_MS)).toBe(true)
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
  it('dura como maximo unos 3 s y espera el primer cuadro 1 s', () => {
    expect(DURACION_INTRO_S).toBeLessThanOrEqual(3)
    expect(ESPERA_PRIMER_CUADRO_MS).toBe(1000)
  })
})

function montar(visible = true) {
  document.body.innerHTML =
    '<div id="intro" data-testid="intro-animada"><video muted playsinline></video></div>'
  const el = document.getElementById('intro')!
  el.hidden = !visible
  const video = el.querySelector('video')!
  const play = vi.fn().mockResolvedValue(undefined)
  Object.defineProperty(video, 'play', { value: play, configurable: true })
  Object.defineProperty(video, 'pause', { value: vi.fn(), configurable: true })
  return { el, video, play }
}

describe('intro en video', () => {
  let soltar = () => {}
  beforeEach(() => {
    vi.useFakeTimers()
    movimientoReducido(false)
  })
  afterEach(() => {
    soltar()
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('se retira al terminar el video', () => {
    const { el, video } = montar()
    Object.defineProperty(video, 'readyState', { value: 4 })
    Object.defineProperty(video, 'paused', { value: false })
    soltar = iniciarIntroVideo(document, almacen())
    vi.advanceTimersByTime(ESPERA_PRIMER_CUADRO_MS + 10)
    expect(el.hidden).toBe(false)
    video.dispatchEvent(new Event('ended'))
    expect(el.hidden).toBe(true)
  })

  it('se salta con un toque o una tecla', () => {
    const { el } = montar()
    soltar = iniciarIntroVideo(document, almacen())
    el.click()
    expect(el.hidden).toBe(true)
    soltar()
    const otra = montar()
    soltar = iniciarIntroVideo(document, almacen())
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    expect(otra.el.hidden).toBe(true)
  })

  it('fallback: se cierra si no hay primer cuadro en 1 s o si el video falla', () => {
    const { el } = montar()
    soltar = iniciarIntroVideo(document, almacen())
    expect(el.hidden).toBe(false)
    vi.advanceTimersByTime(ESPERA_PRIMER_CUADRO_MS + 10)
    expect(el.hidden).toBe(true)
    soltar()
    const otra = montar()
    soltar = iniciarIntroVideo(document, almacen())
    otra.video.dispatchEvent(new Event('error'))
    expect(otra.el.hidden).toBe(true)
  })

  it('tope de seguridad aunque ended no llegue', () => {
    const { el, video } = montar()
    Object.defineProperty(video, 'readyState', { value: 4 })
    Object.defineProperty(video, 'paused', { value: false })
    soltar = iniciarIntroVideo(document, almacen())
    vi.advanceTimersByTime(DURACION_INTRO_S * 1000 + 1600)
    expect(el.hidden).toBe(true)
  })

  it('si index.html la oculto (vista hace poco o movimiento reducido) no la reproduce', () => {
    const { el, play } = montar(false)
    soltar = iniciarIntroVideo(document, almacen())
    expect(el.hidden).toBe(true)
    expect(play).not.toHaveBeenCalled()
  })
})
