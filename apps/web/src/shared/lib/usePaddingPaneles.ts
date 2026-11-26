import { useLayoutEffect, useState, type RefObject } from 'react'
import { PADDING_BASE, paddingPorPaneles, type PaddingMapa } from './encuadre'

const IGUAL = (a: PaddingMapa, b: PaddingMapa) =>
  a.top === b.top && a.right === b.right && a.bottom === b.bottom && a.left === b.left

/**
 * Mide cuanto del mapa tapan los paneles (cabecera, hoja inferior, lista) y lo mantiene al
 * dia al cambiar el tamano. Se pasa a `paddingEncuadre` / `EncuadrarRuta` para que la ruta
 * se vea entera en la franja libre.
 */
export function usePaddingPaneles(
  mapa: RefObject<HTMLElement | null>,
  paneles: RefObject<HTMLElement | null>[],
  margen = 24
): PaddingMapa {
  const [padding, setPadding] = useState<PaddingMapa>({
    top: PADDING_BASE,
    right: PADDING_BASE,
    bottom: PADDING_BASE,
    left: PADDING_BASE,
  })
  useLayoutEffect(() => {
    const el = mapa.current
    if (!el) return
    const medir = () => {
      const cajas = paneles.flatMap((r) => (r.current ? [r.current.getBoundingClientRect()] : []))
      const sig = paddingPorPaneles(el.getBoundingClientRect(), cajas, margen)
      setPadding((a) => (IGUAL(a, sig) ? a : sig))
    }
    medir()
    // Sin ResizeObserver (jsdom, navegadores muy viejos) basta con medir al montar y al redimensionar.
    const o = typeof ResizeObserver === 'undefined' ? null : new ResizeObserver(medir)
    o?.observe(el)
    paneles.forEach((r) => r.current && o?.observe(r.current))
    window.addEventListener('resize', medir)
    return () => {
      o?.disconnect()
      window.removeEventListener('resize', medir)
    }
    // Las referencias son estables; solo importa el margen.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [margen])
  return padding
}
