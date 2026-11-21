import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { formatearMs, formatearRitmo, Unidades } from '../../../shared/lib/geo'
import { useActividad } from '../application/estado'
import { useIdentidad } from '../../identidad/api'
import { ladoComparacion, type LadoComparacion } from '../domain/analisis'

const filas: { id: string; etiqueta: string; valor: (c: LadoComparacion, m: boolean) => string }[] =
  [
    {
      id: 'distancia',
      etiqueta: 'Distancia',
      valor: (c, m) => `${Unidades.texto(c.distanciaKm, m)} ${Unidades.etiquetaDistancia(m)}`,
    },
    { id: 'tiempo', etiqueta: 'Tiempo', valor: (c) => formatearMs(c.segundos) },
    {
      id: 'ritmo',
      etiqueta: 'Ritmo',
      valor: (c, m) =>
        `${formatearRitmo(Unidades.ritmo(c.ritmoSegKm, m))} ${Unidades.etiquetaRitmo(m)}`,
    },
    { id: 'desnivel', etiqueta: 'Desnivel', valor: (c) => `+${Math.round(c.desnivelM)} m` },
  ]

/** Dos carreras del mismo usuario, lado a lado. */
export function CompararCarreras() {
  const carreras = useActividad((s) => s.carreras)
  const m = useIdentidad((s) => s.ajustes.unidadesMetricas)
  const orden = useMemo(() => [...carreras].sort((a, b) => b.inicioMs - a.inicioMs), [carreras])
  const [ida, setA] = useState<string | null>(null)
  const [idb, setB] = useState<string | null>(null)
  const aId = ida ?? orden[0]?.id ?? ''
  const bId = idb ?? orden[1]?.id ?? orden[0]?.id ?? ''
  const a = orden.find((c) => c.id === aId)
  const b = orden.find((c) => c.id === bId)
  const la = a ? ladoComparacion(a) : null
  const lb = b ? ladoComparacion(b) : null

  return (
    <div className="contenedor-escritorio max-w-3xl pb-space-3xl" data-testid="comparar-carreras">
      <header className="flex items-center gap-2 px-margin pt-space-md pb-space-sm lg:px-0">
        <Link
          to="/perfil"
          aria-label="Atrás"
          className="w-11 h-11 rounded-full flex items-center justify-center text-text-primary hover:bg-surface-container"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </Link>
        <h1 className="font-headline-sm text-headline-sm text-text-primary">Comparar carreras</h1>
      </header>
      {orden.length < 2 ? (
        <p className="px-margin text-text-secondary">
          Guarda al menos dos carreras para compararlas.
        </p>
      ) : (
        <div className="px-margin lg:px-0 space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <label className="text-body-sm text-text-secondary">
              Carrera A
              <select
                aria-label="Carrera A"
                value={aId}
                onChange={(ev) => setA(ev.target.value)}
                className="mt-1 w-full h-12 px-3 rounded-xl bg-surface-container border border-border-subtle text-text-primary"
              >
                {orden.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.titulo}
                  </option>
                ))}
              </select>
            </label>
            <label className="text-body-sm text-text-secondary">
              Carrera B
              <select
                aria-label="Carrera B"
                value={bId}
                onChange={(ev) => setB(ev.target.value)}
                className="mt-1 w-full h-12 px-3 rounded-xl bg-surface-container border border-border-subtle text-text-primary"
              >
                {orden.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.titulo}
                  </option>
                ))}
              </select>
            </label>
          </div>
          {la && lb && (
            <table className="w-full text-left">
              <thead>
                <tr className="text-text-secondary text-body-sm">
                  <th className="py-2 font-medium"> </th>
                  <th className="py-2 font-medium">{la.titulo}</th>
                  <th className="py-2 font-medium">{lb.titulo}</th>
                </tr>
              </thead>
              <tbody>
                {filas.map((f) => (
                  <tr
                    key={f.id}
                    className="border-t border-border-subtle"
                    data-testid={`cmp-${f.id}`}
                  >
                    <th className="py-3 font-medium text-text-secondary">{f.etiqueta}</th>
                    <td className="py-3 text-text-primary">{f.valor(la, m)}</td>
                    <td className="py-3 text-text-primary">{f.valor(lb, m)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  )
}
