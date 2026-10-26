import { Link } from 'react-router-dom'
import { eventosMock } from '../../../mocks/data'
import { tarjeta } from '../../../shared/ui/tarjeta'
import { useApp } from '../../../store/app'
import { unirEventos } from '../api'

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
