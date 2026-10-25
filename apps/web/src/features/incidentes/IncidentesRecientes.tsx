import { useEffect, useState } from 'react'
import {
  confirmarIncidente,
  haceTexto,
  incidentesRecientes,
  nombreTipo,
  TIPOS_INCIDENTE,
  type IncidentePublico,
} from './incidentes'

export function useIncidentesRecientes(inicial?: IncidentePublico[]) {
  const [lista, setLista] = useState<IncidentePublico[]>(inicial ?? [])
  useEffect(() => {
    if (inicial) return
    void incidentesRecientes().then(setLista)
  }, [inicial])
  return lista
}

/** Lista de incidentes recientes con "Yo también lo vi". */
export function ListaIncidentes({ lista }: { lista: IncidentePublico[] }) {
  const [vistos, setVistos] = useState<string[]>([])
  if (!lista.length) return null
  return (
    <section aria-labelledby="titulo-incidentes" className="space-y-2">
      <h2 id="titulo-incidentes" className="text-text-primary font-semibold">
        Incidentes recientes
      </h2>
      <ul className="space-y-2">
        {lista.slice(0, 10).map((i) => (
          <li
            key={i.id}
            data-testid={`incidente-${i.id}`}
            className="flex items-center gap-3 p-3 rounded-xl bg-c2-card border border-c2-linea"
          >
            <span className="material-symbols-outlined text-[#F5C451]">
              {TIPOS_INCIDENTE.find((t) => t.id === i.tipo)?.icono ?? 'warning'}
            </span>
            <span className="flex-1 min-w-0">
              <span className="block text-text-primary text-sm font-semibold">
                {nombreTipo(i.tipo)}
              </span>
              <span className="block text-xs text-text-secondary">
                {haceTexto(i.ocurridoMs)} · {i.confirmaciones} confirmaciones
                {i.estado === 'verificado' ? ' · verificado' : ''}
              </span>
            </span>
            <button
              disabled={vistos.includes(i.id)}
              onClick={() =>
                void confirmarIncidente(i.id).then(() => setVistos((v) => [...v, i.id]))
              }
              className="h-9 px-3 rounded-full bg-c2-bg border border-c2-linea text-xs text-text-primary disabled:opacity-50"
            >
              {vistos.includes(i.id) ? 'Confirmado' : 'Yo también lo vi'}
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}
