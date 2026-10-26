/** Graficos SVG livianos con el estilo C2 (sin dependencias). */

export interface PuntoGrafico {
  etiqueta: string
  valor: number
}

const fmt = (v: number) => (Number.isInteger(v) ? String(v) : v.toFixed(1))

/** Barras verticales; marca en violeta la barra resaltada (por defecto la ultima). */
export function GraficoBarras({
  datos,
  alto = 140,
  resaltar,
  unidad = '',
  testId,
}: {
  datos: PuntoGrafico[]
  alto?: number
  resaltar?: number
  unidad?: string
  testId?: string
}) {
  const max = Math.max(1, ...datos.map((d) => d.valor))
  const n = Math.max(1, datos.length)
  const ancho = 100 / n
  const marca = resaltar ?? datos.length - 1
  const cadaCuanto = Math.ceil(n / 8)
  return (
    <figure data-testid={testId} className="w-full">
      <svg
        viewBox={`0 0 100 ${alto}`}
        preserveAspectRatio="none"
        className="w-full"
        style={{ height: alto }}
        role="img"
        aria-label={datos.map((d) => `${d.etiqueta}: ${fmt(d.valor)}${unidad}`).join(', ')}
      >
        {[0.25, 0.5, 0.75].map((f) => (
          <line
            key={f}
            x1="0"
            x2="100"
            y1={alto - 18 - f * (alto - 30)}
            y2={alto - 18 - f * (alto - 30)}
            stroke="#2C3446"
            strokeWidth="0.3"
          />
        ))}
        {datos.map((d, i) => {
          const h = (d.valor / max) * (alto - 30)
          return (
            <rect
              key={i}
              x={i * ancho + ancho * 0.15}
              y={alto - 18 - h}
              width={ancho * 0.7}
              height={Math.max(h, d.valor > 0 ? 1 : 0)}
              fill={i === marca ? '#C084FC' : '#4B3A6B'}
            />
          )
        })}
      </svg>
      <figcaption className="flex text-[10px] text-c2-muted -mt-4">
        {datos.map((d, i) => (
          <span key={i} className="text-center truncate" style={{ width: `${ancho}%` }}>
            {i % cadaCuanto === 0 ? d.etiqueta : ''}
          </span>
        ))}
      </figcaption>
    </figure>
  )
}

/** Linea con area para series diarias. */
export function GraficoLinea({
  datos,
  alto = 140,
  color = '#68DBA9',
  testId,
}: {
  datos: PuntoGrafico[]
  alto?: number
  color?: string
  testId?: string
}) {
  const max = Math.max(1, ...datos.map((d) => d.valor))
  const n = datos.length
  const x = (i: number) => (n <= 1 ? 50 : (i / (n - 1)) * 100)
  const y = (v: number) => alto - 6 - (v / max) * (alto - 16)
  const linea = datos.map((d, i) => `${x(i).toFixed(2)},${y(d.valor).toFixed(2)}`).join(' ')
  return (
    <figure data-testid={testId} className="w-full">
      <svg
        viewBox={`0 0 100 ${alto}`}
        preserveAspectRatio="none"
        className="w-full"
        style={{ height: alto }}
        role="img"
        aria-label={`Serie de ${n} días, máximo ${fmt(max)}`}
      >
        {n > 1 && (
          <>
            <polygon points={`0,${alto} ${linea} 100,${alto}`} fill={color} opacity="0.12" />
            <polyline
              points={linea}
              fill="none"
              stroke={color}
              strokeWidth="1.5"
              vectorEffect="non-scaling-stroke"
            />
          </>
        )}
      </svg>
      {n > 0 && (
        <figcaption className="flex justify-between text-[10px] text-c2-muted">
          <span>{datos[0].etiqueta}</span>
          <span>{datos[n - 1].etiqueta}</span>
        </figcaption>
      )}
    </figure>
  )
}

/** Barras horizontales con etiqueta y valor (rankings, motivos). */
export function BarrasHorizontales({
  datos,
  color = '#C084FC',
  testId,
}: {
  datos: PuntoGrafico[]
  color?: string
  testId?: string
}) {
  const max = Math.max(1, ...datos.map((d) => d.valor))
  return (
    <ul data-testid={testId} className="space-y-1.5">
      {datos.map((d) => (
        <li
          key={d.etiqueta}
          className="grid grid-cols-[minmax(0,9rem)_1fr_auto] items-center gap-2"
        >
          <span className="truncate text-[12px] text-white">{d.etiqueta}</span>
          <span className="h-2 bg-c2-s2">
            <span
              className="block h-2"
              style={{ width: `${(d.valor / max) * 100}%`, background: color }}
            />
          </span>
          <span className="text-[12px] tabular-nums text-c2-muted">{fmt(d.valor)}</span>
        </li>
      ))}
    </ul>
  )
}
