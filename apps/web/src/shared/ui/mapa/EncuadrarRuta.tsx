import { useMap } from '@vis.gl/react-google-maps'
import { useEffect, useRef } from 'react'
import { encuadrarRuta, type OpcionesEncuadre, type PaddingMapa } from '../../lib/encuadre'
import type { Coordenada } from '../../lib/mapas'

/**
 * Dentro de un `<Map>`: cada vez que cambia la ruta (o `clave`), la camara se anima hasta
 * mostrarla entera. `padding` descuenta lo que tapan listas, tarjetas o paneles sobre el mapa.
 */
export function EncuadrarRuta({
  puntos,
  padding,
  duracionMs,
  clave,
  activo = true,
  ...opciones
}: {
  puntos: Coordenada[]
  padding?: number | Partial<PaddingMapa>
  duracionMs?: number
  /** Si se pasa, solo su cambio vuelve a encuadrar (los puntos pueden moverse sin mover la camara). */
  clave?: string | number
  activo?: boolean
} & Omit<OpcionesEncuadre, 'padding' | 'duracionMs'>) {
  const mapa = useMap()
  const ultima = useRef<string | null>(null)
  // Con `clave` solo ella decide cuando volver a encuadrar (ideal para posiciones en vivo);
  // sin ella, cambia con el trazo.
  const huella = !puntos.length
    ? ''
    : clave !== undefined
      ? `k|${clave}`
      : `${puntos.length}|${puntos[0].lat},${puntos[0].lng}|${puntos[puntos.length - 1].lat},${puntos[puntos.length - 1].lng}`
  const paddingRef = useRef(padding)
  paddingRef.current = padding
  const puntosRef = useRef(puntos)
  puntosRef.current = puntos
  useEffect(() => {
    if (!mapa || !activo || !huella) return
    // Primer encuadre mas corto: el mapa recien aparece.
    const primera = ultima.current === null
    ultima.current = huella
    return encuadrarRuta(mapa, puntosRef.current, {
      ...opciones,
      padding: paddingRef.current,
      duracionMs: duracionMs ?? (primera ? 700 : undefined),
    })
    // `opciones` y `duracionMs` son estables en cada pantalla; solo la ruta dispara el encuadre.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mapa, activo, huella])
  return null
}
