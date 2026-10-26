import { Link } from 'react-router-dom'
import { Unidades } from '../../../shared/lib/geo'
import { tarjeta } from '../../../shared/ui/tarjeta'
import { useActividad } from '../../actividad/api'
import { useIdentidad } from '../../identidad/api'
import { useComunidad } from '../application/estado'
import { rankingSemanal } from '../domain/ranking'

export function RankingSemanal() {
  const carreras = useActividad((s) => s.carreras)
  const remotas = useComunidad((s) => s.publicaciones)
  const sesion = useIdentidad((s) => s.sesion)
  const m = useIdentidad((s) => s.ajustes.unidadesMetricas)
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
