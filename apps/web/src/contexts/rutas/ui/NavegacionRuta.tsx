import { lazy, Suspense, useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { formatearMs, formatearRitmo, Unidades } from '../../../shared/lib/geo'
import { claveMapas, trazoDeOpcion } from '../../../shared/lib/mapas'
import { MapaGoogle } from '../../../shared/ui/MapaGoogle'
import {
  empezarSeguimiento,
  estaActiva,
  finalizarCarrera,
  tracker,
  useCarrera,
} from '../../actividad/api'
import { useIdentidad } from '../../identidad/api'
import { useRutas } from '../application/estado'
import { indicacion } from '../domain/buscador'

const Runner3D = lazy(() =>
  import('../../../shared/ui/Runner3D').then((m) => ({ default: m.Runner3D }))
)

export function NavegacionRuta() {
  const navigate = useNavigate()
  const location = useLocation()
  const estado = location.state as { ruta?: string; trazoId?: string } | null
  const nombreRuta = estado?.ruta ?? 'Ruta libre'
  const rutas = useRutas((s) => s.rutas)
  const rutaActivaId = useRutas((s) => s.rutaActivaId)
  const plan = estado?.trazoId
    ? trazoDeOpcion(estado.trazoId)
    : (rutas.find((r) => r.id === rutaActivaId)?.puntos ?? [])
  const [mapa3d, setMapa3d] = useState(() => !!claveMapas())
  const e = useCarrera()
  const ajustes = useIdentidad((s) => s.ajustes)
  const m = ajustes.unidadesMetricas
  const [errorGps, setErrorGps] = useState<string | null>(null)
  const [animacion, setAnimacion] = useState<'Jog' | 'Run'>('Jog')

  // La navegacion graba la carrera; si ya habia una en curso, la reutiliza.
  useEffect(() => {
    if (!estaActiva(tracker.estado))
      empezarSeguimiento({ precisa: ajustes.ubicacionPrecisa, onError: setErrorGps })
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (e.puntos.length) setErrorGps(null)
  }, [e.puntos.length])

  const paso = indicacion(e.distanciaKm)

  const cerrar = () => {
    if (estaActiva(tracker.estado) && tracker.estado.segundos > 0) {
      finalizarCarrera()
      navigate('/correr/resumen')
    } else {
      navigate('/rutas')
    }
  }

  return (
    <main className="w-full min-h-screen bg-[#0c1322] relative overflow-hidden">
      <h1 className="sr-only">Navegación de ruta</h1>
      <div className="absolute top-3 inset-x-0 z-50 px-6 flex justify-between items-center text-xs text-slate-300">
        <span className="pl-2 truncate">{nombreRuta}</span>
        <div className="flex gap-1">
          {claveMapas() && (
            <button
              onClick={() => setMapa3d(!mapa3d)}
              aria-pressed={mapa3d}
              data-testid="mapa-3d"
              className={`px-3 py-1.5 min-h-8 rounded-full ${mapa3d ? 'bg-primary-600 text-on-primary' : 'bg-black/50'}`}
            >
              Mapa 3D
            </button>
          )}
          {(['Jog', 'Run'] as const).map((a) => (
            <button
              key={a}
              onClick={() => setAnimacion(a)}
              aria-pressed={animacion === a}
              className={`px-3 py-1.5 min-h-8 rounded-full ${animacion === a ? 'bg-primary-600 text-on-primary' : 'bg-black/50'}`}
            >
              {a === 'Jog' ? 'Trote' : 'Carrera'}
            </button>
          ))}
        </div>
      </div>

      <div className="absolute inset-0 w-full h-full z-0">
        <Suspense
          fallback={
            <div className="w-full h-full flex items-center justify-center bg-surface-container-lowest">
              <div className="flex flex-col items-center gap-2">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
                <span className="text-sm text-text-secondary">Cargando navegación 3D...</span>
              </div>
            </div>
          }
        >
          {mapa3d ? (
            <MapaGoogle
              puntos={e.puntos.length ? e.puntos : plan.slice(0, 1)}
              plan={plan}
              seguir
              modo3d
              ritmoSegKm={e.ritmoActualSegKm}
              className="w-full h-full"
              alternativa={null}
            />
          ) : (
            <Runner3D animation={animacion} cameraPosition={[0, 3.8, 6.2]} />
          )}
        </Suspense>
      </div>

      <div className="absolute top-20 left-4 right-4 z-20 lg:right-auto lg:left-8 lg:w-[420px]">
        <div className="bg-black/80 backdrop-blur-md rounded-2xl p-4 shadow-2xl">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-12 h-12 rounded-full bg-primary-600 flex items-center justify-center">
              <span className="material-symbols-outlined text-white text-[28px]">navigation</span>
            </div>
            <div className="flex-1">
              <div className="text-primary-400 text-xs font-semibold uppercase tracking-wide">
                Siguiente giro en
              </div>
              <div className="text-white text-2xl font-bold" data-testid="giro-metros">
                {paso.metros} metros
              </div>
            </div>
          </div>
          <div className="text-slate-300 text-sm">{paso.texto}</div>
          {errorGps && e.puntos.length === 0 && (
            <div role="alert" className="text-danger text-xs mt-2">
              {errorGps}
            </div>
          )}
        </div>
      </div>

      {claveMapas() && !mapa3d && (
        <div className="absolute top-56 right-4 z-20 w-36 h-36 lg:top-20 lg:right-8 lg:w-80 lg:h-80 rounded-2xl overflow-hidden border border-white/10 shadow-2xl">
          <MapaGoogle
            puntos={e.puntos}
            seguir
            zoom={16}
            className="w-full h-full"
            alternativa={null}
          />
        </div>
      )}

      <div className="absolute bottom-0 left-0 right-0 z-20">
        <div className="bg-gradient-to-t from-[#0c1322] via-[#0c1322]/95 to-transparent p-6 pb-8 lg:px-8 [&>*]:lg:max-w-4xl [&>*]:lg:mx-auto">
          <div className="grid grid-cols-3 gap-4 mb-4">
            <div className="bg-black/60 backdrop-blur-md rounded-xl p-3 text-center">
              <div className="text-primary-400 text-2xl font-bold">
                {Unidades.texto(e.distanciaKm, m)}
              </div>
              <div className="text-slate-400 text-xs">{Unidades.etiquetaDistancia(m)}</div>
            </div>
            <div className="bg-black/60 backdrop-blur-md rounded-xl p-3 text-center">
              <div className="text-secondary-500 text-2xl font-bold">{formatearMs(e.segundos)}</div>
              <div className="text-slate-400 text-xs">tiempo</div>
            </div>
            <div className="bg-black/60 backdrop-blur-md rounded-xl p-3 text-center">
              <div className="text-white text-2xl font-bold">
                {formatearRitmo(Unidades.ritmo(e.ritmoActualSegKm, m))}
              </div>
              <div className="text-slate-400 text-xs">{Unidades.etiquetaRitmo(m)}</div>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate('/correr/cronometro')}
              className="flex-1 h-12 bg-primary-600 hover:bg-primary-700 text-on-primary font-semibold rounded-xl flex items-center justify-center gap-2 shadow-lg transition-colors active:scale-95"
            >
              <span className="material-symbols-outlined">timer</span>
              Ver cronómetro
            </button>
            <button
              onClick={cerrar}
              aria-label="Terminar navegación"
              className="w-12 h-12 bg-black/60 backdrop-blur-md hover:bg-black/80 text-white rounded-xl flex items-center justify-center shadow-lg transition-colors active:scale-95"
            >
              <span className="material-symbols-outlined">close</span>
            </button>
          </div>
        </div>
      </div>

      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-10 pointer-events-none">
        <div className="w-2 h-2 rounded-full bg-primary-600 shadow-lg shadow-primary-600/50"></div>
      </div>
    </main>
  )
}
