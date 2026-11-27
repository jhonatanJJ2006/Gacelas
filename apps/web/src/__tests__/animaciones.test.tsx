import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, render, renderHook, screen } from '@testing-library/react'
import {
  estiloEscalon,
  forzarMovimientoReducido,
  suavizarConteo,
  useConteo,
  useSalidaAnimada,
} from '../shared/lib/movimiento'
import {
  CifraTexto,
  Confeti,
  Contador,
  partirCifra,
  particulasConfeti,
} from '../shared/ui/Animaciones'
import { Dialogo } from '../shared/ui/ui'
import { Anillo, Progreso } from '../shared/ui/Visual'

const relojFalso = () =>
  vi.useFakeTimers({
    toFake: [
      'setTimeout',
      'clearTimeout',
      'Date',
      'performance',
      'requestAnimationFrame',
      'cancelAnimationFrame',
    ],
  })

describe('animaciones C2 (web)', () => {
  afterEach(() => {
    forzarMovimientoReducido(true)
    vi.useRealTimers()
  })

  describe('curva y utilidades', () => {
    it('suavizarConteo va de 0 a 1 y es monotona', () => {
      expect(suavizarConteo(0)).toBeCloseTo(0, 3)
      expect(suavizarConteo(1)).toBe(1)
      expect(suavizarConteo(2)).toBe(1)
      expect(suavizarConteo(-1)).toBeCloseTo(0, 3)
      expect(suavizarConteo(0.5)).toBeGreaterThan(suavizarConteo(0.25))
      expect(suavizarConteo(0.25)).toBeGreaterThan(0.5)
    })

    it('estiloEscalon respeta el tope de 6 escalones', () => {
      expect((estiloEscalon(2) as Record<string, number>)['--i']).toBe(2)
      expect((estiloEscalon(40) as Record<string, number>)['--i']).toBe(6)
      expect((estiloEscalon(-3) as Record<string, number>)['--i']).toBe(0)
    })

    it('partirCifra conserva prefijo, decimales, separador y unidad', () => {
      expect(partirCifra('+12,4 km')).toEqual({
        prefijo: '+',
        numero: 12.4,
        decimales: 1,
        sep: ',',
        sufijo: ' km',
      })
      expect(partirCifra('75%')).toMatchObject({ numero: 75, decimales: 0, sep: '', sufijo: '%' })
      expect(partirCifra('Aun ninguno')).toBeNull()
      expect(partirCifra('1.234')).toBeNull()
    })

    it('las particulas del confeti son deterministas', () => {
      expect(particulasConfeti(10)).toEqual(particulasConfeti(10))
      expect(particulasConfeti(28)).toHaveLength(28)
    })
  })

  describe('con reducir movimiento', () => {
    it('Contador y CifraTexto muestran el valor final al instante', () => {
      render(
        <>
          <Contador valor={42} testId="c" />
          <CifraTexto texto="12,4 km" testId="t" />
        </>
      )
      expect(screen.getByTestId('c')).toHaveTextContent('42')
      expect(screen.getByTestId('t')).toHaveTextContent('12,4 km')
    })

    it('el confeti no se dibuja', () => {
      render(<Confeti />)
      expect(screen.queryByTestId('confeti')).toBeNull()
    })

    it('Dialogo se desmonta al instante al cerrarse', () => {
      const { rerender } = render(
        <Dialogo titulo="Aviso" abierto onCerrar={() => {}}>
          Hola
        </Dialogo>
      )
      expect(screen.getByRole('dialog')).toBeInTheDocument()
      rerender(
        <Dialogo titulo="Aviso" abierto={false} onCerrar={() => {}}>
          Hola
        </Dialogo>
      )
      expect(screen.queryByRole('dialog')).toBeNull()
    })

    it('Anillo y Progreso exponen el valor final', () => {
      render(
        <>
          <Anillo pct={50} testId="anillo" />
          <Progreso pct={30} etiqueta="avance" />
        </>
      )
      expect(screen.getByTestId('anillo')).toHaveTextContent('50%')
      expect(screen.getByRole('progressbar', { name: 'avance' })).toHaveAttribute(
        'aria-valuenow',
        '30'
      )
    })
  })

  describe('con movimiento activo', () => {
    beforeEach(() => {
      forzarMovimientoReducido(false)
    })

    it('useConteo cuenta desde 0 hasta el objetivo', () => {
      relojFalso()
      const { result } = renderHook(() => useConteo(100, 900))
      expect(result.current).toBe(0)
      act(() => {
        vi.advanceTimersByTime(300)
      })
      expect(result.current).toBeGreaterThan(0)
      expect(result.current).toBeLessThan(100)
      act(() => {
        vi.advanceTimersByTime(1000)
      })
      expect(result.current).toBe(100)
    })

    it('CifraTexto anima el numero y termina con el texto exacto', () => {
      relojFalso()
      render(<CifraTexto texto="12,4 km" testId="t" />)
      expect(screen.getByTestId('t')).toHaveTextContent('0,0 km')
      act(() => {
        vi.advanceTimersByTime(1200)
      })
      expect(screen.getByTestId('t')).toHaveTextContent('12,4 km')
    })

    it('el confeti se dibuja y se retira solo', () => {
      relojFalso()
      render(<Confeti n={12} />)
      expect(screen.getByTestId('confeti').children).toHaveLength(12)
      act(() => {
        vi.advanceTimersByTime(1500)
      })
      expect(screen.queryByTestId('confeti')).toBeNull()
    })

    it('useSalidaAnimada mantiene el elemento durante la salida', () => {
      relojFalso()
      const { result, rerender } = renderHook(({ a }) => useSalidaAnimada(a, 150), {
        initialProps: { a: true },
      })
      expect(result.current).toEqual([true, false])
      rerender({ a: false })
      expect(result.current).toEqual([true, true])
      act(() => {
        vi.advanceTimersByTime(200)
      })
      expect(result.current).toEqual([false, false])
    })

    it('Dialogo anima su salida antes de desmontarse', () => {
      relojFalso()
      const { rerender } = render(
        <Dialogo titulo="Aviso" abierto onCerrar={() => {}}>
          Hola
        </Dialogo>
      )
      rerender(
        <Dialogo titulo="Aviso" abierto={false} onCerrar={() => {}}>
          Hola
        </Dialogo>
      )
      expect(screen.getByRole('dialog').className).toContain('mov-hoja-salida')
      act(() => {
        vi.advanceTimersByTime(200)
      })
      expect(screen.queryByRole('dialog')).toBeNull()
    })
  })
})
