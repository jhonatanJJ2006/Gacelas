import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { eventoPorId } from '../../../mocks/data'
import { formatearHms } from '../../../shared/lib/geo'
import { MapaCorredores } from '../../../shared/ui/MapaEditable'
import { elegirEventoEnVivo } from '../../actividad/api'
import {
  clasificacion,
  colorCorredor,
  corredoresEvento,
  posicionesRepeticion,
  suscribirEvento,
  useEventos,
  type CorredorEvento,
} from '../../eventos/api'

/** Mapa en vivo de todos los participantes de un evento, clasificacion y repeticion. */
export function EventoEnVivo() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const creados = useEventos((s) => s.eventosCreados)
  const evento = creados.find((e) => e.id === id) ?? eventoPorId(id)
  const [corredores, setCorredores] = useState<CorredorEvento[]>([])
  const [repeticion, setRepeticion] = useState<number | null>(null)

  useEffect(() => {
    const cargar = () => void corredoresEvento(id).then(setCorredores)
    cargar()
    const fin = suscribirEvento(id, cargar)
    const t = setInterval(cargar, 15000)
    return () => {
      fin()
      clearInterval(t)
    }
  }, [id])

  useEffect(() => {
    if (repeticion === null || repeticion >= 1) return
    const t = setTimeout(
      () => setRepeticion((r) => (r === null ? null : Math.min(1, r + 0.01))),
      80
    )
    return () => clearTimeout(t)
  }, [repeticion])

  const tabla = clasificacion(corredores)
  const enCurso = tabla.filter((c) => !c.terminado).length
  const marcadores = useMemo(() => {
    if (repeticion !== null) {
      const pos = posicionesRepeticion(tabla, repeticion)
      return tabla.map((c) => {
        const p = pos.find((x) => x.usuarioId === c.usuarioId)
        return {
          id: c.usuarioId,
          nombre: c.nombre,
          punto: p ? { lat: p.lat, lng: p.lng } : null,
          trazo: c.puntos.slice(0, Math.max(1, Math.floor(repeticion * c.puntos.length))),
        }
      })
    }
    return tabla.map((c) => ({
      id: c.usuarioId,
      nombre: c.nombre,
      punto: c.lat != null && c.lng != null ? { lat: c.lat, lng: c.lng } : null,
      trazo: c.puntos,
    }))
  }, [tabla, repeticion])

  return (
    <main className="w-full min-h-screen bg-surface contenedor-escritorio lg:max-w-6xl pb-28">
      <header className="flex items-center gap-1 px-margin h-16 bg-bg-secondary mb-4">
        <button
          onClick={() => navigate(`/comunidad/eventos/${id}`)}
          aria-label="Volver"
          className="w-12 h-12 -ml-3 shrink-0 flex items-center justify-center text-text-primary"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </button>
        <div className="flex-1 min-w-0">
          <h1 className="font-display italic uppercase text-[26px] leading-tight text-text-primary truncate">
            {evento?.titulo ?? 'Evento'} · en vivo
          </h1>
          <p className="text-sm text-text-secondary">
            {enCurso > 0
              ? `${enCurso} corriendo ahora`
              : tabla.length
                ? 'Evento terminado'
                : 'Aún no hay corredores en vivo'}
          </p>
        </div>
      </header>
      <div className="px-margin lg:px-0 lg:grid lg:grid-cols-[minmax(0,1fr)_340px] lg:gap-6 space-y-4 lg:space-y-0">
        <div className="space-y-3">
          <MapaCorredores
            corredores={marcadores}
            className="h-[380px] lg:h-[560px] rounded-2xl overflow-hidden bg-surface-container"
          />
          <div className="flex flex-wrap gap-2">
            {!evento?.pasado && (
              <button
                data-testid="correr-evento"
                onClick={() => {
                  elegirEventoEnVivo(id)
                  navigate('/correr')
                }}
                className="h-12 px-5 rounded-xl bg-primary-600 text-on-primary font-semibold flex items-center gap-2"
              >
                <span className="material-symbols-outlined">sensors</span>Correr y compartir en el
                evento
              </button>
            )}
            {tabla.some((c) => c.puntos.length > 1) && (
              <button
                data-testid="repeticion"
                onClick={() => setRepeticion(repeticion === null || repeticion >= 1 ? 0 : null)}
                className="h-12 px-5 rounded-xl bg-surface-container text-text-primary flex items-center gap-2"
              >
                <span className="material-symbols-outlined">
                  {repeticion === null ? 'replay' : 'stop'}
                </span>
                {repeticion === null ? 'Ver repetición' : 'Salir de la repetición'}
              </button>
            )}
          </div>
          {repeticion !== null && (
            <input
              type="range"
              min={0}
              max={100}
              value={Math.round(repeticion * 100)}
              onChange={(e) => setRepeticion(Number(e.target.value) / 100)}
              aria-label="Progreso de la repetición"
              className="w-full"
            />
          )}
        </div>
        <section
          aria-labelledby="titulo-clasificacion"
          className="bg-surface-container rounded-2xl p-4 space-y-2 h-fit"
        >
          <h2 id="titulo-clasificacion" className="font-title-md text-title-md text-text-primary">
            Clasificación
          </h2>
          {tabla.length === 0 ? (
            <p className="text-sm text-text-secondary">
              Cuando los inscritos empiecen a correr con "Compartir en vivo", aparecerán aquí.
            </p>
          ) : (
            <ol className="space-y-2" data-testid="clasificacion">
              {tabla.map((c, i) => (
                <li key={c.sesionId} className="flex items-center gap-3">
                  <span
                    className="w-7 h-7 rounded-full flex items-center justify-center text-sm font-bold text-on-primary"
                    style={{ background: colorCorredor(i) }}
                  >
                    {i + 1}
                  </span>
                  <span className="flex-1 min-w-0 truncate text-text-primary">
                    {c.nombre}
                    {c.oculto && (
                      <span className="text-xs text-text-secondary"> · ubicación oculta</span>
                    )}
                    {c.terminado && <span className="text-xs text-text-secondary"> · terminó</span>}
                  </span>
                  <span className="text-sm text-text-primary whitespace-nowrap">
                    {(c.distanciaM / 1000).toFixed(2)} km
                  </span>
                  <span className="text-xs text-text-secondary whitespace-nowrap">
                    {formatearHms(c.segundos)}
                  </span>
                </li>
              ))}
            </ol>
          )}
        </section>
      </div>
    </main>
  )
}
