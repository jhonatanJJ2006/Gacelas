import { formatearHms, formatearRitmo, Unidades } from '../../../shared/lib/geo'
import { tarjeta } from '../../../shared/ui/tarjeta'
import { useActividad } from '../../actividad/api'
import { useIdentidad } from '../../identidad/api'
import { kmPorSemana, recordsPersonales } from '../domain/paneles'

export function MetaSemanal({ km, metaKm = 30 }: { km: number; metaKm?: number }) {
  const m = useIdentidad((s) => s.ajustes.unidadesMetricas)
  const pct = Math.min(100, Math.round((km / metaKm) * 100))
  const r = 34
  const c = 2 * Math.PI * r
  return (
    <section className={`${tarjeta} flex items-center gap-5`} data-testid="panel-meta">
      <svg
        width="88"
        height="88"
        viewBox="0 0 88 88"
        role="img"
        aria-label={`Meta semanal al ${pct}%`}
      >
        <circle
          cx="44"
          cy="44"
          r={r}
          fill="none"
          strokeWidth="8"
          className="stroke-surface-container-highest"
        />
        <circle
          cx="44"
          cy="44"
          r={r}
          fill="none"
          strokeWidth="8"
          strokeLinecap="round"
          className="stroke-primary"
          strokeDasharray={c}
          strokeDashoffset={c * (1 - pct / 100)}
          transform="rotate(-90 44 44)"
        />
        <text x="44" y="49" textAnchor="middle" className="fill-text-primary text-[16px] font-bold">
          {pct}%
        </text>
      </svg>
      <div>
        <p className="text-xs uppercase tracking-wider text-primary font-semibold">
          Objetivo semanal
        </p>
        <p className="font-headline-sm text-headline-sm text-text-primary">
          {Unidades.texto(km, m, 1)} / {Unidades.texto(metaKm, m, 0)}{' '}
          {Unidades.etiquetaDistancia(m)}
        </p>
        <p className="text-sm text-text-secondary">
          {pct >= 100 ? '¡Meta cumplida!' : 'Sigue sumando kilómetros esta semana'}
        </p>
      </div>
    </section>
  )
}

export function EstadisticasPerfil() {
  const carreras = useActividad((s) => s.carreras)
  const m = useIdentidad((s) => s.ajustes.unidadesMetricas)
  const semanas = kmPorSemana(carreras, Date.now())
  const max = Math.max(1, ...semanas.map((s) => s.km))
  const rec = recordsPersonales(carreras)
  return (
    <div className="space-y-6">
      <section className={tarjeta} data-testid="panel-km-semanas">
        <h2 className="font-title-md text-title-md text-text-primary mb-4">Volumen semanal</h2>
        <div className="flex items-end gap-3 h-40">
          {semanas.map((s, i) => (
            <div
              key={s.desde}
              className="flex-1 flex flex-col items-center gap-2 h-full justify-end"
            >
              <span className="text-xs text-text-secondary tabular-nums">
                {Unidades.texto(s.km, m, 1)}
              </span>
              <div
                className={`w-full rounded-t-lg ${i === semanas.length - 1 ? 'bg-primary' : 'bg-primary/40'}`}
                style={{ height: `${Math.max(4, (s.km / max) * 100)}%` }}
              />
              <span className="text-[11px] text-text-secondary">
                {i === semanas.length - 1 ? 'Esta' : `S-${semanas.length - 1 - i}`}
              </span>
            </div>
          ))}
        </div>
      </section>
      <section className={tarjeta} data-testid="panel-records">
        <h2 className="font-title-md text-title-md text-text-primary mb-4">Récords personales</h2>
        <dl className="grid grid-cols-3 gap-4">
          <div>
            <dt className="text-xs text-text-secondary">Más larga</dt>
            <dd className="font-headline-sm text-headline-sm text-primary">
              {rec.masLarga ? Unidades.texto(rec.masLarga.distanciaKm, m, 1) : '0.0'}
              <span className="text-sm text-text-secondary"> {Unidades.etiquetaDistancia(m)}</span>
            </dd>
          </div>
          <div>
            <dt className="text-xs text-text-secondary">Mejor ritmo</dt>
            <dd className="font-headline-sm text-headline-sm text-secondary">
              {formatearRitmo(Unidades.ritmo(rec.mejorRitmoSegKm, m))}
            </dd>
          </div>
          <div>
            <dt className="text-xs text-text-secondary">Tiempo total</dt>
            <dd className="font-headline-sm text-headline-sm text-text-primary">
              {formatearHms(rec.totalSegundos)}
            </dd>
          </div>
        </dl>
      </section>
    </div>
  )
}
