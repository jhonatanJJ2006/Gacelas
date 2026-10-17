/** Grafica del perfil de elevacion (SVG, sin dependencias). */
export function PerfilElevacion({
  alturas,
  distanciaKm,
}: {
  alturas: number[]
  distanciaKm: number
}) {
  if (alturas.length < 2) return null
  const min = Math.min(...alturas)
  const max = Math.max(...alturas)
  const rango = Math.max(max - min, 10)
  const W = 600
  const H = 160
  const xy = alturas.map((h, i) => [
    (i / (alturas.length - 1)) * W,
    H - 8 - ((h - min) / rango) * (H - 24),
  ])
  const linea = xy.map(([x, y], i) => `${i ? 'L' : 'M'}${x.toFixed(1)},${y.toFixed(1)}`).join(' ')
  return (
    <figure className="bg-surface-container rounded-2xl p-4" data-testid="perfil-elevacion">
      <figcaption className="flex justify-between text-sm text-text-secondary mb-2">
        <span className="font-semibold text-text-primary">Perfil de elevación</span>
        <span>
          {min}–{max} m · {distanciaKm.toFixed(1)} km
        </span>
      </figcaption>
      <svg
        viewBox={`0 0 ${W} ${H}`}
        className="w-full h-32"
        role="img"
        aria-label={`Elevación entre ${min} y ${max} metros`}
        preserveAspectRatio="none"
      >
        <defs>
          <linearGradient id="g-elev" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stopColor="#F59E0B" stopOpacity="0.5" />
            <stop offset="1" stopColor="#F59E0B" stopOpacity="0" />
          </linearGradient>
        </defs>
        <path d={`${linea} L${W},${H} L0,${H} Z`} fill="url(#g-elev)" />
        <path
          d={linea}
          fill="none"
          stroke="#F59E0B"
          strokeWidth="3"
          vectorEffect="non-scaling-stroke"
        />
      </svg>
    </figure>
  )
}
