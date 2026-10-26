import type { PuntoGps } from '../lib/geo'

/** Dibuja el recorrido GPS en un SVG, escalado al recuadro y con norte arriba. */
export function TrazadoRuta({
  puntos,
  className = '',
}: {
  puntos: PuntoGps[]
  className?: string
}) {
  if (puntos.length < 2) {
    return (
      <div
        className={`flex items-center justify-center text-text-secondary text-sm ${className}`}
        data-testid="trazado-vacio"
      >
        <span className="material-symbols-outlined mr-1 text-[18px]">gps_fixed</span>
        Esperando puntos GPS...
      </div>
    )
  }
  const lats = puntos.map((p) => p.lat)
  const lngs = puntos.map((p) => p.lng)
  const [minLat, maxLat, minLng, maxLng] = [
    Math.min(...lats),
    Math.max(...lats),
    Math.min(...lngs),
    Math.max(...lngs),
  ]
  const kLng = Math.cos(((minLat + maxLat) / 2) * (Math.PI / 180))
  const ancho = Math.max((maxLng - minLng) * kLng, 1e-6)
  const alto = Math.max(maxLat - minLat, 1e-6)
  const escala = 90 / Math.max(ancho, alto)
  const x = (lng: number) => 5 + (lng - minLng) * kLng * escala + (90 - ancho * escala) / 2
  const y = (lat: number) => 95 - (lat - minLat) * escala - (90 - alto * escala) / 2
  const d = puntos
    .map((p, i) => `${i === 0 ? 'M' : 'L'}${x(p.lng).toFixed(2)},${y(p.lat).toFixed(2)}`)
    .join(' ')
  const ini = puntos[0]
  const fin = puntos[puntos.length - 1]
  return (
    <svg
      viewBox="0 0 100 100"
      className={className}
      data-testid="trazado-ruta"
      role="img"
      aria-label="Recorrido GPS"
    >
      <path
        d={d}
        fill="none"
        stroke="#7B1FA2"
        strokeWidth="2.2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx={x(ini.lng)} cy={y(ini.lat)} r="2.5" fill="#10B981" />
      <circle cx={x(fin.lng)} cy={y(fin.lat)} r="2.5" fill="#A855F7" />
    </svg>
  )
}
