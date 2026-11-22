import { formatearMs } from '../../../shared/lib/geo'
import type { Parcial } from '../../../shared/kernel/tipos'
import type { PuntoGps } from '../../../shared/lib/geo'
import { parcialesPorKm, serieElevacion } from '../domain/analisis'

/** Barras de ritmo: la mas alta es el kilometro mas rapido. */
export function GraficoRitmo({ parciales }: { parciales: Parcial[] }) {
  const km = parcialesPorKm(parciales)
  if (km.length === 0) return null
  const ritmos = km.map((p) => p.segundos / p.distanciaKm)
  const rapido = Math.min(...ritmos)
  const lento = Math.max(...ritmos)
  return (
    <section
      className="rounded-2xl bg-surface-container border border-border-subtle p-4"
      data-testid="grafico-ritmo"
    >
      <h2 className="font-title-md text-title-md text-text-primary mb-3">Ritmo por kilómetro</h2>
      <div className="flex items-end gap-1 h-28" aria-hidden="true">
        {km.map((p, i) => {
          const r = ritmos[i]
          const alto = lento - rapido < 1 ? 70 : 40 + (60 * (lento - r)) / (lento - rapido)
          const mejor = r === rapido
          return (
            <div
              key={`${p.numero}-${i}`}
              className="flex-1 min-w-0 flex flex-col items-center justify-end h-full"
            >
              <span className={`text-[10px] ${mejor ? 'text-primary' : 'text-text-secondary'}`}>
                {formatearMs(p.segundos)}
              </span>
              <div
                className={`w-full rounded-t ${mejor ? 'bg-primary' : 'bg-primary-600/70'}`}
                style={{ height: `${alto}%` }}
              />
              <span className="text-[10px] text-text-secondary">{p.numero}</span>
            </div>
          )
        })}
      </div>
    </section>
  )
}

/** Perfil de la carrera: altura en el eje vertical, distancia en el horizontal. */
export function GraficoElevacion({ puntos }: { puntos: PuntoGps[] }) {
  const serie = serieElevacion(puntos)
  if (serie.length < 2) return null
  const min = Math.min(...serie.map((p) => p.altitudM))
  const max = Math.max(...serie.map((p) => p.altitudM))
  const rango = Math.max(max - min, 1)
  const total = serie[serie.length - 1].distanciaKm
  const w = 320
  const h = 96
  const coords = serie
    .map((p, i) => {
      const x = total <= 0 ? (i / (serie.length - 1)) * w : (p.distanciaKm / total) * w
      const y = h - 4 - ((p.altitudM - min) / rango) * (h - 12)
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
  return (
    <section
      className="rounded-2xl bg-surface-container border border-border-subtle p-4"
      data-testid="grafico-elevacion"
    >
      <div className="flex justify-between items-baseline gap-2 mb-2">
        <h2 className="font-title-md text-title-md text-text-primary">Elevación</h2>
        <p className="text-body-sm text-text-secondary">
          {Math.round(min)}–{Math.round(max)} m · {total.toFixed(1).replace('.', ',')} km
        </p>
      </div>
      <svg
        viewBox={`0 0 ${w} ${h}`}
        className="w-full h-24"
        role="img"
        aria-label={`Elevación entre ${Math.round(min)} y ${Math.round(max)} metros`}
      >
        <polyline
          points={coords}
          fill="none"
          stroke="currentColor"
          strokeWidth="3"
          className="text-primary"
        />
      </svg>
    </section>
  )
}
