import { unirEventos } from '../lib/eventos'
import { Link } from 'react-router-dom'
import { formatearHms, formatearRitmo, Unidades } from '../lib/geo'
import { kmPorSemana, rankingSemanal, recordsPersonales } from '../lib/paneles'
import { eventosMock } from '../mocks/data'
import { useApp } from '../store/app'

const tarjeta = 'bg-surface-container border border-border-subtle rounded-2xl p-5'

export function RankingSemanal() {
  const carreras = useApp((s) => s.carreras)
  const remotas = useApp((s) => s.publicaciones)
  const sesion = useApp((s) => s.sesion)
  const m = useApp((s) => s.ajustes.unidadesMetricas)
  const filas = rankingSemanal(carreras, sesion?.nombre ?? 'Tú', remotas, Date.now())
  return (
    <section className={tarjeta} aria-labelledby="titulo-ranking" data-testid="panel-ranking">
      <div className="flex items-center justify-between mb-4">
        <h2 id="titulo-ranking" className="font-title-md text-title-md text-text-primary">
          Ranking semanal
        </h2>
        <span className="material-symbols-outlined text-primary">leaderboard</span>
      </div>
      <ol className="space-y-2">
        {filas.map((f, i) => (
          <li
            key={`${f.nombre}-${i}`}
            className={`flex items-center gap-3 rounded-xl px-3 py-2 ${f.propio ? 'bg-primary/10 border border-primary/30' : ''}`}
          >
            <span
              className={`w-6 text-center font-bold ${i === 0 ? 'text-primary' : 'text-text-secondary'}`}
            >
              {i + 1}
            </span>
            <span className="flex-1 truncate text-text-primary">
              {f.nombre}
              {f.propio ? ' (tú)' : ''}
            </span>
            <span className="font-semibold text-text-primary tabular-nums">
              {Unidades.texto(f.km, m, 1)} {Unidades.etiquetaDistancia(m)}
            </span>
          </li>
        ))}
      </ol>
      <Link
        to="/comunidad/corredores"
        className="mt-4 inline-flex items-center gap-1 py-2 text-sm font-semibold text-primary"
      >
        <span className="material-symbols-outlined text-[18px]">person_search</span>
        Buscar corredores para seguir
      </Link>
    </section>
  )
}

export function MetaSemanal({ km, metaKm = 30 }: { km: number; metaKm?: number }) {
  const m = useApp((s) => s.ajustes.unidadesMetricas)
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

export function ProximosEventos() {
  const creados = useApp((s) => s.eventosCreados)
  const lista = unirEventos(creados, eventosMock)
    .filter((e) => !e.pasado)
    .slice(0, 3)
  return (
    <section className={tarjeta} data-testid="panel-eventos">
      <h2 className="font-title-md text-title-md text-text-primary mb-3">Próximos eventos</h2>
      <ul className="space-y-1">
        {lista.map((e) => (
          <li key={e.id}>
            <Link
              to={`/comunidad/eventos/${e.id}`}
              className="flex items-center gap-3 rounded-xl px-2 py-2 hover:bg-surface-container-high"
            >
              <span className="w-10 h-10 rounded-lg bg-primary/15 text-primary flex items-center justify-center material-symbols-outlined">
                event
              </span>
              <span className="min-w-0">
                <span className="block truncate text-text-primary font-medium">{e.titulo}</span>
                <span className="block text-xs text-text-secondary">
                  {e.fecha}, {e.hora}
                </span>
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  )
}

export function EstadisticasPerfil() {
  const carreras = useApp((s) => s.carreras)
  const m = useApp((s) => s.ajustes.unidadesMetricas)
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
