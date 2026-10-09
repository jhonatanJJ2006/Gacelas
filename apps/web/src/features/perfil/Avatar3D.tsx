import { lazy, Suspense } from 'react';
import { useNavigate } from 'react-router-dom';

const Runner3D = lazy(() => import('../../components/Runner3D').then(m => ({ default: m.Runner3D })));

export function Avatar3D() {
  const navigate = useNavigate();

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <header className="px-margin py-4 border-b border-border-subtle">
        <div className="flex items-center justify-between">
          <button
            onClick={() => navigate('/perfil')}
            className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors"
          >
            <span className="material-symbols-outlined">arrow_back</span>
          </button>
          <h1 className="font-headline-md text-headline-md text-text-primary">
            Personalizar Avatar
          </h1>
          <button className="px-4 py-2 bg-primary-600 hover:bg-primary-700 text-bg-primary font-label-lg text-label-lg rounded-lg transition-colors">
            Guardar
          </button>
        </div>
      </header>

      <main className="flex-1 flex flex-col pb-24">
        <div className="relative flex-1 bg-surface-container-low">
          <Suspense
            fallback={
              <div className="w-full h-full flex items-center justify-center">
                <div className="flex flex-col items-center gap-2">
                  <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
                  <span className="text-sm text-text-secondary">Cargando avatar 3D...</span>
                </div>
              </div>
            }
          >
            <Runner3D animation="Idle" autoRotate cameraPosition={[0, 1.2, 3.5]} />
          </Suspense>

          <div className="absolute bottom-4 left-4 right-4">
            <div className="bg-surface/90 backdrop-blur-xl border border-border-subtle rounded-2xl p-4 shadow-2xl">
              <div className="flex items-center gap-2 mb-3">
                <span className="material-symbols-outlined text-[20px] text-primary-500">
                  palette
                </span>
                <span className="font-title-md text-title-md text-text-primary">
                  Equipación
                </span>
              </div>
              <div className="grid grid-cols-4 gap-2">
                <button className="aspect-square rounded-lg bg-primary-600 border-2 border-primary-600 shadow-lg"></button>
                <button className="aspect-square rounded-lg bg-secondary-600 border-2 border-transparent hover:border-border-subtle"></button>
                <button className="aspect-square rounded-lg bg-danger border-2 border-transparent hover:border-border-subtle"></button>
                <button className="aspect-square rounded-lg bg-tertiary border-2 border-transparent hover:border-border-subtle"></button>
              </div>
            </div>
          </div>
        </div>

        <div className="px-margin py-6 space-y-4 bg-surface">
          <div>
            <div className="flex items-center justify-between mb-3">
              <label className="font-body-md text-body-md text-text-primary font-medium">
                Estilo de carrera
              </label>
            </div>
            <div className="grid grid-cols-3 gap-2">
              <button className="px-4 py-3 bg-primary-container/20 border-2 border-primary-600 rounded-xl flex flex-col items-center gap-1">
                <span className="material-symbols-outlined text-[24px] text-primary-500">
                  directions_run
                </span>
                <span className="font-label-md text-label-md text-text-primary">
                  Dinámico
                </span>
              </button>
              <button className="px-4 py-3 bg-surface-container border-2 border-transparent hover:border-border-subtle rounded-xl flex flex-col items-center gap-1">
                <span className="material-symbols-outlined text-[24px] text-text-secondary">
                  self_improvement
                </span>
                <span className="font-label-md text-label-md text-text-secondary">
                  Relajado
                </span>
              </button>
              <button className="px-4 py-3 bg-surface-container border-2 border-transparent hover:border-border-subtle rounded-xl flex flex-col items-center gap-1">
                <span className="material-symbols-outlined text-[24px] text-text-secondary">
                  bolt
                </span>
                <span className="font-label-md text-label-md text-text-secondary">
                  Veloz
                </span>
              </button>
            </div>
          </div>

          <div>
            <div className="flex items-center justify-between mb-3">
              <label className="font-body-md text-body-md text-text-primary font-medium">
                Accesorios
              </label>
            </div>
            <div className="space-y-2">
              <div className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl">
                <div className="flex items-center gap-3">
                  <span className="material-symbols-outlined text-[20px] text-text-secondary">
                    visibility
                  </span>
                  <span className="font-body-md text-body-md text-text-primary">
                    Lentes deportivos
                  </span>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input type="checkbox" className="sr-only peer" />
                  <div className="w-11 h-6 bg-surface-container-high peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-primary-600 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
                </label>
              </div>

              <div className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl">
                <div className="flex items-center gap-3">
                  <span className="material-symbols-outlined text-[20px] text-text-secondary">
                    watch
                  </span>
                  <span className="font-body-md text-body-md text-text-primary">
                    Reloj GPS
                  </span>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input type="checkbox" className="sr-only peer" defaultChecked />
                  <div className="w-11 h-6 bg-surface-container-high peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-primary-600 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
                </label>
              </div>

              <div className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl">
                <div className="flex items-center gap-3">
                  <span className="material-symbols-outlined text-[20px] text-text-secondary">
                    headphones
                  </span>
                  <span className="font-body-md text-body-md text-text-primary">
                    Audífonos
                  </span>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input type="checkbox" className="sr-only peer" defaultChecked />
                  <div className="w-11 h-6 bg-surface-container-high peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-primary-600 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
                </label>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
