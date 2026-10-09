import { useNavigate } from 'react-router-dom'

export function ResumenPostCarrera() {
  const navigate = useNavigate()

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <header className="px-margin py-4">
        <button
          onClick={() => navigate('/inicio')}
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
              <div className="font-metric-huge text-metric-huge text-primary leading-none mb-1">
                5.2
              </div>
              <div className="font-label-md text-label-md text-text-secondary uppercase">km</div>
            </div>
            <div className="text-center">
              <div className="font-metric-huge text-metric-huge text-text-primary leading-none mb-1">
                28:34
              </div>
              <div className="font-label-md text-label-md text-text-secondary uppercase">
                Tiempo
              </div>
            </div>
            <div className="text-center">
              <div className="font-metric-huge text-metric-huge text-secondary leading-none mb-1">
                5:30
              </div>
              <div className="font-label-md text-label-md text-text-secondary uppercase">/km</div>
            </div>
          </div>

          <div className="border-t border-border-subtle pt-4 grid grid-cols-4 gap-3 text-center">
            <div>
              <div className="font-headline-sm text-headline-sm text-text-primary">245</div>
              <div className="font-label-md text-label-md text-text-secondary text-[10px]">Cal</div>
            </div>
            <div>
              <div className="font-headline-sm text-headline-sm text-text-primary">142</div>
              <div className="font-label-md text-label-md text-text-secondary text-[10px]">BPM</div>
            </div>
            <div>
              <div className="font-headline-sm text-headline-sm text-text-primary">125</div>
              <div className="font-label-md text-label-md text-text-secondary text-[10px]">SPM</div>
            </div>
            <div>
              <div className="font-headline-sm text-headline-sm text-text-primary">45m</div>
              <div className="font-label-md text-label-md text-text-secondary text-[10px]">
                Elev
              </div>
            </div>
          </div>
        </div>

        <div className="space-y-3">
          <input
            type="text"
            placeholder="Título de la carrera (opcional)"
            className="w-full h-12 px-4 bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary focus:outline-none focus:ring-2 focus:ring-primary-600 focus:border-transparent"
          />

          <textarea
            placeholder="¿Cómo te fue? Comparte tus pensamientos..."
            rows={3}
            className="w-full px-4 py-3 bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary focus:outline-none focus:ring-2 focus:ring-primary-600 focus:border-transparent resize-none"
          />

          <div className="flex items-center justify-between px-4 py-3 bg-surface-container border border-border-subtle rounded-xl">
            <span className="font-body-md text-body-md text-text-primary">Hacer pública</span>
            <label className="relative inline-flex items-center cursor-pointer">
              <input type="checkbox" className="sr-only peer" defaultChecked />
              <div className="w-11 h-6 bg-surface-container-high peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-primary-600 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
            </label>
          </div>
        </div>

        <div className="mt-8 space-y-3">
          <button
            onClick={() => navigate('/inicio')}
            className="w-full h-14 bg-primary-600 hover:bg-primary-700 active:scale-[0.98] text-bg-primary font-headline-md font-bold text-base rounded-xl flex items-center justify-center gap-2 shadow-[0_8px_20px_-4px_rgba(217,119,6,0.35)] transition-all"
          >
            <span className="material-symbols-outlined text-[20px]">save</span>
            <span>Guardar carrera</span>
          </button>

          <button
            onClick={() => navigate('/inicio')}
            className="w-full h-12 bg-transparent hover:bg-surface-container active:scale-[0.99] border border-border-subtle text-text-secondary font-medium text-sm rounded-xl flex items-center justify-center transition-all"
          >
            Descartar
          </button>
        </div>
      </main>
    </div>
  )
}
