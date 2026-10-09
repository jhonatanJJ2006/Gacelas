import { lazy, Suspense } from 'react'
import { useNavigate } from 'react-router-dom'

const Runner3D = lazy(() =>
  import('../../components/Runner3D').then((m) => ({ default: m.Runner3D }))
)

export function NavegacionRuta() {
  const navigate = useNavigate()

  return (
    <div className="w-full min-h-screen bg-[#0c1322] relative overflow-hidden">
      <div className="absolute top-0 inset-x-0 z-50 pt-3 px-6 flex justify-between items-center text-xs text-slate-400">
        <span className="pl-2">06:45 AM</span>
        <div className="flex items-center space-x-2 pr-1">
          <span className="text-[10px]">98%</span>
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
          <Runner3D animation="Jog" cameraPosition={[0, 3.8, 6.2]} />
        </Suspense>
      </div>

      <div className="absolute top-20 left-4 right-4 z-20">
        <div className="bg-black/80 backdrop-blur-md rounded-2xl p-4 shadow-2xl">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-12 h-12 rounded-full bg-primary-600 flex items-center justify-center">
              <span className="material-symbols-outlined text-white text-[28px]">navigation</span>
            </div>
            <div className="flex-1">
              <div className="text-primary-400 text-xs font-semibold uppercase tracking-wide">
                Siguiente giro en
              </div>
              <div className="text-white text-2xl font-bold">200 metros</div>
            </div>
          </div>
          <div className="text-slate-300 text-sm">Gira a la derecha en Av. Universitaria</div>
        </div>
      </div>

      <div className="absolute bottom-0 left-0 right-0 z-20">
        <div className="bg-gradient-to-t from-[#0c1322] via-[#0c1322]/95 to-transparent p-6 pb-8">
          <div className="grid grid-cols-3 gap-4 mb-4">
            <div className="bg-black/60 backdrop-blur-md rounded-xl p-3 text-center">
              <div className="text-primary-400 text-2xl font-bold">2.3</div>
              <div className="text-slate-400 text-xs">km</div>
            </div>
            <div className="bg-black/60 backdrop-blur-md rounded-xl p-3 text-center">
              <div className="text-secondary-500 text-2xl font-bold">12:15</div>
              <div className="text-slate-400 text-xs">tiempo</div>
            </div>
            <div className="bg-black/60 backdrop-blur-md rounded-xl p-3 text-center">
              <div className="text-white text-2xl font-bold">5:18</div>
              <div className="text-slate-400 text-xs">/km</div>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate('/correr/cronometro')}
              className="flex-1 h-12 bg-primary-600 hover:bg-primary-700 text-white font-semibold rounded-xl flex items-center justify-center gap-2 shadow-lg transition-colors active:scale-95"
            >
              <span className="material-symbols-outlined">timer</span>
              Ver cronómetro
            </button>
            <button
              onClick={() => navigate('/rutas')}
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
    </div>
  )
}
