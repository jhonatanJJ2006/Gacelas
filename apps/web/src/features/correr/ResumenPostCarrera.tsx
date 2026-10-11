import { useRef, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { MapaGoogle } from '../../components/MapaGoogle'
import { TrazadoRuta } from '../../components/TrazadoRuta'
import { formatearMs, formatearRitmo, Unidades } from '../../lib/geo'
import { ritmoPromedio, tituloPorHora } from '../../lib/runTracker'
import { useApp } from '../../store/app'
import { descartarCarrera, tracker, useCarrera } from '../../store/carrera'
import { caloriasEstimadas } from '../../lib/logica'

export function ResumenPostCarrera() {
  const navigate = useNavigate()
  const e = useCarrera()
  const ajustes = useApp((s) => s.ajustes)
  const guardarCarrera = useApp((s) => s.guardarCarrera)
  const m = ajustes.unidadesMetricas
  const [titulo, setTitulo] = useState('')
  const [nota, setNota] = useState('')
  const [publica, setPublica] = useState(ajustes.visibilidad !== 'privado')
  const [guardando, setGuardando] = useState(false)
  const saliendo = useRef(false)

  // Tras guardar se va a Perfil; no se redirige a Inicio al reiniciar la carrera.
  if (saliendo.current) return null
  if (e.fase !== 'terminada') return <Navigate to="/inicio" replace />

  const guardar = async () => {
    setGuardando(true)
    await guardarCarrera(tracker.aCarreraGuardada(titulo, nota, publica))
    saliendo.current = true
    navigate('/perfil', { replace: true })
    tracker.reiniciar()
  }

  const descartar = () => {
    descartarCarrera()
    navigate('/inicio', { replace: true })
  }

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <header className="px-margin py-4">
        <button
          onClick={descartar}
          aria-label="Cerrar sin guardar"
          className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors"
        >
          <span className="material-symbols-outlined">close</span>
        </button>
      </header>

      <main className="flex-1 px-6 pb-8 overflow-auto">
        <div className="text-center mb-8">
          <div className="w-20 h-20 rounded-full bg-secondary-600/20 flex items-center justify-center mx-auto mb-4">
            <span className="material-symbols-outlined text-[48px] text-secondary-500">
              check_circle
            </span>
          </div>
          <h1 className="font-headline-lg text-headline-lg text-text-primary mb-2">
            Carrera completada
          </h1>
          <p className="font-body-md text-body-md text-text-secondary">
            Gran trabajo, has terminado tu carrera
          </p>
        </div>

        <div className="bg-surface-container rounded-3xl border border-border-subtle p-6 mb-6">
          <div className="grid grid-cols-3 gap-4 mb-6">
            <div className="text-center">
              <div
                className="font-metric-huge text-[40px] text-primary leading-none mb-1"
                data-testid="resumen-distancia"
              >
                {Unidades.texto(e.distanciaKm, m)}
              </div>
              <div className="font-label-md text-label-md text-text-secondary uppercase">
                {Unidades.etiquetaDistancia(m)}
              </div>
            </div>
            <div className="text-center">
              <div
                className="font-metric-huge text-[40px] text-text-primary leading-none mb-1"
                data-testid="resumen-tiempo"
              >
                {formatearMs(e.segundos)}
              </div>
              <div className="font-label-md text-label-md text-text-secondary uppercase">
                Tiempo
              </div>
            </div>
            <div className="text-center">
              <div className="font-metric-huge text-[40px] text-secondary leading-none mb-1">
                {formatearRitmo(Unidades.ritmo(ritmoPromedio(e), m))}
              </div>
              <div className="font-label-md text-label-md text-text-secondary uppercase">
                {Unidades.etiquetaRitmo(m)}
              </div>
            </div>
          </div>
          <div className="border-t border-border-subtle pt-4 grid grid-cols-3 gap-3 text-center">
            <div>
              <div className="font-headline-sm text-headline-sm text-text-primary">
                {caloriasEstimadas(e.distanciaKm)}
              </div>
              <div className="text-label-md text-text-secondary text-[10px]">kcal est.</div>
            </div>
            <div>
              <div className="font-headline-sm text-headline-sm text-text-primary">
                {e.parciales.length}
              </div>
              <div className="text-label-md text-text-secondary text-[10px]">Parciales</div>
            </div>
            <div>
              <div className="font-headline-sm text-headline-sm text-text-primary">
                {Math.round(e.desnivelM)}m
              </div>
              <div className="text-label-md text-text-secondary text-[10px]">Elev</div>
            </div>
          </div>
          <div className="h-36 mt-4">
            <MapaGoogle
              puntos={e.puntos}
              className="w-full h-full"
              alternativa={<TrazadoRuta puntos={e.puntos} className="w-full h-full" />}
            />
          </div>
        </div>

        <div className="space-y-3">
          <input
            type="text"
            aria-label="Título de la carrera"
            value={titulo}
            onChange={(ev) => setTitulo(ev.target.value)}
            placeholder={tituloPorHora(e.inicioMs)}
            className="w-full h-12 px-4 bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary focus:outline-none focus:ring-2 focus:ring-primary-600"
          />
          <textarea
            aria-label="Notas"
            value={nota}
            onChange={(ev) => setNota(ev.target.value)}
            placeholder="¿Cómo te fue? Comparte tus pensamientos..."
            rows={3}
            className="w-full px-4 py-3 bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary focus:outline-none focus:ring-2 focus:ring-primary-600 resize-none"
          />
          <label className="flex items-center justify-between px-4 py-3 bg-surface-container border border-border-subtle rounded-xl cursor-pointer">
            <span className="font-body-md text-body-md text-text-primary">Hacer pública</span>
            <input
              type="checkbox"
              checked={publica}
              onChange={(ev) => setPublica(ev.target.checked)}
              className="w-5 h-5 accent-primary-600"
            />
          </label>
        </div>

        <div className="mt-8 space-y-3">
          <button
            onClick={guardar}
            disabled={guardando}
            className="w-full h-14 bg-primary-600 hover:bg-primary-700 active:scale-[0.98] text-bg-primary font-bold text-base rounded-xl flex items-center justify-center gap-2 shadow-[0_8px_20px_-4px_rgba(217,119,6,0.35)] transition-all disabled:opacity-60"
          >
            <span className="material-symbols-outlined text-[20px]">save</span>
            <span>Guardar carrera</span>
          </button>
          <button
            onClick={descartar}
            className="w-full h-12 bg-transparent hover:bg-surface-container border border-border-subtle text-text-secondary font-medium text-sm rounded-xl flex items-center justify-center transition-all"
          >
            Descartar
          </button>
        </div>
      </main>
    </div>
  )
}
