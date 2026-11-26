import type { PuntoGeo } from './fuente'

/*
 * Esquema de Loja para el escenario del Resumen. Las calles y los rios son decorativos (sin
 * escala); lo que se dibuja encima son posiciones reales proyectadas al recuadro de la ciudad.
 * Los colores salen de los tokens --mapa-* para que cambie con el tema.
 */

const ANCHO = 640
const ALTO = 320
/** Recuadro que cubre la ciudad de Loja y su entorno inmediato. */
export const RECUADRO_LOJA = { norte: -3.93, sur: -4.05, oeste: -79.3, este: -79.11 }

export function proyectar(p: PuntoGeo): { x: number; y: number } | null {
  const { norte, sur, oeste, este } = RECUADRO_LOJA
  if (p.lat > norte || p.lat < sur || p.lng < oeste || p.lng > este) return null
  return {
    x: ((p.lng - oeste) / (este - oeste)) * ANCHO,
    y: ((norte - p.lat) / (norte - sur)) * ALTO,
  }
}

const CALLES_V = [70, 150, 230, 300, 390, 470, 560]
const CALLES_H = [50, 110, 170, 230, 280]

export function MapaEscenario({
  corredores,
  incidentes,
  ruta,
  etiqueta,
}: {
  corredores: PuntoGeo[]
  incidentes: PuntoGeo[]
  /** Trazado a resaltar (por ejemplo, una ruta). */
  ruta?: PuntoGeo[]
  etiqueta: string
}) {
  const vivos = corredores.map(proyectar).filter((p) => p !== null)
  const alertas = incidentes.map(proyectar).filter((p) => p !== null)
  const trazo = (ruta ?? []).map(proyectar).filter((p) => p !== null)
  return (
    <svg
      viewBox={`0 0 ${ANCHO} ${ALTO}`}
      preserveAspectRatio="xMidYMid slice"
      role="img"
      aria-label={etiqueta}
      data-testid="mapa-escenario"
    >
      <rect width={ANCHO} height={ALTO} fill="hsl(var(--mapa-fondo))" />
      <g stroke="hsl(var(--mapa-calle-2))" strokeWidth="5" fill="none">
        {CALLES_V.map((x, i) => (
          <path key={`v${x}`} d={`M${x + (i % 2) * 6} 0 L${x - 14 + (i % 3) * 9} ${ALTO}`} />
        ))}
        {CALLES_H.map((y, i) => (
          <path key={`h${y}`} d={`M0 ${y + (i % 2) * 8} L${ANCHO} ${y - 12 + (i % 3) * 10}`} />
        ))}
      </g>
      <g stroke="hsl(var(--mapa-calle))" strokeWidth="9" fill="none" strokeLinecap="round">
        <path d={`M0 ${ALTO * 0.62} C 160 ${ALTO * 0.5}, 330 ${ALTO * 0.75}, ${ANCHO} ${ALTO * 0.4}`} />
        <path d={`M${ANCHO * 0.34} 0 C ${ANCHO * 0.4} ${ALTO * 0.4}, ${ANCHO * 0.3} ${ALTO * 0.7}, ${ANCHO * 0.38} ${ALTO}`} />
      </g>
      <g stroke="hsl(var(--mapa-rio))" strokeWidth="9" fill="none" strokeLinecap="round" opacity="0.9">
        <path d={`M0 ${ALTO * 0.78} C 140 ${ALTO * 0.6}, 300 ${ALTO * 0.92}, 460 ${ALTO * 0.7} S ${ANCHO} ${ALTO * 0.6}, ${ANCHO} ${ALTO * 0.58}`} />
      </g>
      {trazo.length > 1 && (
        <polyline
          points={trazo.map((p) => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ')}
          fill="none"
          stroke="hsl(var(--mapa-ruta))"
          strokeWidth="4"
          strokeLinejoin="round"
          strokeLinecap="round"
        />
      )}
      {alertas.map((p, i) => (
        <rect
          key={`a${i}`}
          x={p.x - 5}
          y={p.y - 5}
          width="10"
          height="10"
          transform={`rotate(45 ${p.x} ${p.y})`}
          fill="hsl(var(--advertencia))"
          data-testid="marca-incidente"
        />
      ))}
      {vivos.map((p, i) => (
        <g key={`c${i}`} data-testid="marca-corredor">
          <circle cx={p.x} cy={p.y} r="10" fill="hsl(var(--mapa-vivo) / 0.22)" />
          <circle cx={p.x} cy={p.y} r="5" fill="hsl(var(--mapa-vivo))" />
        </g>
      ))}
    </svg>
  )
}

/** Trazo de una ruta ajustado a su propio recuadro, para las tarjetas de rutas. */
export function TrazoRuta({ puntos, etiqueta }: { puntos: PuntoGeo[]; etiqueta: string }) {
  const W = 320
  const H = 120
  const lat = puntos.map((p) => p.lat)
  const lng = puntos.map((p) => p.lng)
  const minLat = Math.min(...lat)
  const maxLat = Math.max(...lat)
  const minLng = Math.min(...lng)
  const maxLng = Math.max(...lng)
  const ancho = Math.max(maxLng - minLng, 1e-6)
  const alto = Math.max(maxLat - minLat, 1e-6)
  const k = Math.min((W - 32) / ancho, (H - 28) / alto)
  const ox = (W - ancho * k) / 2
  const oy = (H - alto * k) / 2
  // Se reducen los puntos para no dibujar miles de vertices por tarjeta.
  const paso = Math.max(1, Math.floor(puntos.length / 60))
  const xy = puntos
    .filter((_, i) => i % paso === 0 || i === puntos.length - 1)
    .map((p) => ({ x: ox + (p.lng - minLng) * k, y: oy + (maxLat - p.lat) * k }))
  const ini = xy[0]
  const fin = xy[xy.length - 1]
  return (
    <svg viewBox={`0 0 ${W} ${H}`} role="img" aria-label={etiqueta} style={{ width: '100%', display: 'block' }}>
      <rect width={W} height={H} fill="hsl(var(--mapa-fondo))" />
      <g stroke="hsl(var(--mapa-calle-2))" strokeWidth="1">
        {[40, 90, 150, 210, 270].map((x) => (
          <line key={`x${x}`} x1={x} y1="0" x2={x} y2={H} />
        ))}
        {[30, 60, 90].map((y) => (
          <line key={`y${y}`} x1="0" y1={y} x2={W} y2={y} />
        ))}
      </g>
      <polyline
        points={xy.map((p) => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ')}
        fill="none"
        stroke="hsl(var(--mapa-ruta))"
        strokeWidth="3"
        strokeLinejoin="round"
        strokeLinecap="round"
      />
      <circle cx={ini.x} cy={ini.y} r="5" fill="hsl(var(--mapa-vivo))" />
      <circle cx={fin.x} cy={fin.y} r="5" fill="hsl(var(--mapa-ruta))" />
    </svg>
  )
}
