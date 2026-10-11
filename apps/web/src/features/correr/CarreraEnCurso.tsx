import { useEffect, useState } from 'react'
import {
  dejarDeCompartir,
  empezarACompartir,
  enviarPosicion,
  puedeCompartir,
  useCompartirEnVivo,
} from '../../lib/enVivo'
import { Navigate, useNavigate } from 'react-router-dom'
import { MapaGoogle } from '../../components/MapaGoogle'
import { TrazadoRuta } from '../../components/TrazadoRuta'
import { BotonTexto, Dialogo } from '../../components/ui'
import { formatearHms, formatearRitmo, Unidades } from '../../lib/geo'
import { estaActiva, ritmoPromedio } from '../../lib/runTracker'
import { useApp } from '../../store/app'
import { descartarCarrera, finalizarCarrera, tracker, useCarrera } from '../../store/carrera'
import { caloriasEstimadas } from '../../lib/logica'

export function CarreraEnCurso() {
  const navigate = useNavigate()
  const e = useCarrera()
  const m = useApp((s) => s.ajustes.unidadesMetricas)
  const [descartar, setDescartar] = useState(false)
  const sesion = useApp((s) => s.sesion)
  const vivo = useCompartirEnVivo()
  const compartiendo = vivo.sesionId !== null
  const ultimo = e.puntos.at(-1)
  useEffect(() => {
    if (compartiendo && e.fase === 'corriendo')
      void enviarPosicion(ultimo, e.distanciaKm, e.segundos)
  }, [compartiendo, e.fase, ultimo, e.distanciaKm, e.segundos])

  if (!estaActiva(e))
    return <Navigate to={e.fase === 'terminada' ? '/correr/resumen' : '/correr'} replace />

  const pausada = e.fase === 'pausada'
  const terminar = () => {
    void dejarDeCompartir()
    finalizarCarrera()
    navigate('/correr/resumen')
  }

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <div className="relative flex-1 bg-surface-container-low">
        <div className="absolute inset-0 bg-gradient-to-b from-surface-container-low via-surface to-surface"></div>

        <div className="absolute top-4 inset-x-4 z-20 flex justify-between items-center gap-2">
          <button
            onClick={() => setDescartar(true)}
            aria-label="Salir de la carrera"
            className="w-10 h-10 rounded-full bg-surface/90 backdrop-blur-md flex items-center justify-center text-text-primary shadow-lg"
          >
            <span className="material-symbols-outlined">close</span>
          </button>
          {sesion && puedeCompartir() && (
            <button
              onClick={() => void (compartiendo ? dejarDeCompartir() : empezarACompartir())}
              aria-pressed={compartiendo}
              data-testid="compartir-en-vivo"
              className={`h-11 px-4 rounded-full flex items-center gap-2 font-semibold shadow-lg ${
                compartiendo
                  ? 'bg-danger text-white'
                  : 'bg-surface/90 backdrop-blur-md text-text-primary'
              }`}
            >
              <span className="material-symbols-outlined text-[20px]">sensors</span>
              {compartiendo
                ? vivo.visible === false
                  ? 'En vivo · casa oculta'
                  : 'En vivo'
                : 'Compartir en vivo'}
            </button>
          )}
          <button
            onClick={() => navigate('/correr/cronometro')}
            aria-label="Abrir cronómetro"
            className="w-10 h-10 rounded-full bg-surface/90 backdrop-blur-md flex items-center justify-center text-text-primary shadow-lg"
          >
            <span className="material-symbols-outlined">timer</span>
          </button>
        </div>

        <div className="relative z-10 flex flex-col items-center justify-center h-full px-6 pt-20 pb-10">
          <div className="mb-4 text-center">
            <span className="font-label-lg text-label-lg text-primary uppercase tracking-wider flex items-center justify-center gap-2">
              <span
                className={`w-2 h-2 rounded-full ${pausada ? 'bg-text-secondary' : 'bg-secondary animate-pulse'}`}
              ></span>
              {pausada ? 'En pausa' : 'En curso'}
            </span>
            {e.ultimaPrecisionM != null && (
              <span className="text-xs text-text-secondary">
                GPS ±{Math.round(e.ultimaPrecisionM)} m
              </span>
            )}
          </div>

          <div className="bg-surface-container/60 backdrop-blur-xl border border-border-subtle rounded-3xl p-8 mb-6 shadow-2xl w-full max-w-md">
            <div className="text-center mb-6">
              <div
                className="font-metric-huge text-metric-huge text-primary leading-none mb-2"
                data-testid="distancia"
              >
                {Unidades.texto(e.distanciaKm, m)}
              </div>
              <div className="font-headline-sm text-headline-sm text-text-secondary uppercase tracking-wider">
                {m ? 'Kilómetros' : 'Millas'}
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="text-center">
                <div
                  className="font-headline-lg text-headline-lg text-text-primary leading-none mb-1"
                  data-testid="tiempo"
                >
                  {formatearHms(e.segundos)}
                </div>
                <div className="font-label-md text-label-md text-text-secondary uppercase">
                  Tiempo
                </div>
              </div>
              <div className="text-center">
                <div
                  className="font-headline-lg text-headline-lg text-text-primary leading-none mb-1"
                  data-testid="ritmo"
                >
                  {formatearRitmo(Unidades.ritmo(e.ritmoActualSegKm, m))}
                </div>
                <div className="font-label-md text-label-md text-text-secondary uppercase">
                  Ritmo {Unidades.etiquetaRitmo(m)}
                </div>
              </div>
            </div>
          </div>

          <div className="w-full max-w-md h-40 mb-6 rounded-2xl bg-surface-container/40 border border-border-subtle p-2">
            <MapaGoogle
              puntos={e.puntos}
              seguir
              className="w-full h-full"
              alternativa={<TrazadoRuta puntos={e.puntos} className="w-full h-full" />}
            />
          </div>

          <div className="flex gap-4">
            <button
              onClick={() => tracker.alternarPausa()}
              aria-label={pausada ? 'Reanudar' : 'Pausar'}
              className="w-16 h-16 rounded-full bg-surface-container border-2 border-border-subtle flex items-center justify-center text-text-primary hover:bg-surface-container-high transition-all shadow-lg"
            >
              <span className="material-symbols-outlined text-[32px]">
                {pausada ? 'play_arrow' : 'pause'}
              </span>
            </button>
            <button
              onClick={terminar}
              aria-label="Terminar carrera"
              className="w-16 h-16 rounded-full bg-danger/20 border-2 border-danger flex items-center justify-center text-danger hover:bg-danger/30 transition-all shadow-lg"
            >
              <span className="material-symbols-outlined text-[32px]">stop</span>
            </button>
          </div>
        </div>
      </div>

      <div className="bg-surface-container-high border-t border-border-subtle p-4">
        <div className="grid grid-cols-3 gap-4 text-center">
          <div>
            <div className="font-headline-sm text-headline-sm text-text-primary">
              {formatearRitmo(Unidades.ritmo(ritmoPromedio(e), m))}
            </div>
            <div className="font-label-md text-label-md text-text-secondary">Promedio</div>
          </div>
          <div>
            <div className="font-headline-sm text-headline-sm text-text-primary">
              {Math.round(e.desnivelM)} m
            </div>
            <div className="font-label-md text-label-md text-text-secondary">Desnivel</div>
          </div>
          <div>
            <div className="font-headline-sm text-headline-sm text-text-primary">
              {caloriasEstimadas(e.distanciaKm)}
            </div>
            <div className="font-label-md text-label-md text-text-secondary">kcal est.</div>
          </div>
        </div>
      </div>

      <Dialogo
        titulo="¿Descartar la carrera?"
        abierto={descartar}
        onCerrar={() => setDescartar(false)}
        acciones={
          <>
            <BotonTexto onClick={() => setDescartar(false)}>Seguir corriendo</BotonTexto>
            <BotonTexto
              peligro
              onClick={() => {
                void dejarDeCompartir()
                descartarCarrera()
                navigate('/inicio')
              }}
            >
              Descartar
            </BotonTexto>
          </>
        }
      >
        <p>
          Se perderán el tiempo y la distancia de esta carrera. Para guardarla, toca el botón de
          detener.
        </p>
      </Dialogo>
    </div>
  )
}
