import { useNavigate } from 'react-router-dom'

export function Correr() {
  const navigate = useNavigate()

  const handleStart = () => {
    navigate('/correr/en-curso')
  }

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

      <main className="flex-1 flex flex-col items-center justify-center px-6 pb-24">
        <div className="text-center mb-12">
          <div className="w-24 h-24 rounded-full bg-primary-600/20 flex items-center justify-center mx-auto mb-6">
            <span className="material-symbols-outlined text-[64px] text-primary-500">
              directions_run
            </span>
          </div>
          <h1 className="font-headline-lg text-headline-lg text-text-primary mb-3">
            ¿Listo para correr?
          </h1>
          <p className="font-body-md text-body-md text-text-secondary max-w-sm mx-auto">
            Activa el GPS y comienza a grabar tu carrera
          </p>
        </div>

        <div className="w-full max-w-md space-y-4 mb-8">
          <div className="bg-surface-container rounded-2xl border border-border-subtle p-6">
            <div className="flex items-center gap-4 mb-4">
              <div className="w-12 h-12 rounded-full bg-secondary-600/20 flex items-center justify-center">
                <span className="material-symbols-outlined text-[24px] text-secondary-500">
                  location_on
                </span>
              </div>
              <div className="flex-1">
                <p className="font-body-md text-body-md text-text-primary font-semibold mb-1">
                  GPS Activado
                </p>
                <p className="font-body-sm text-body-sm text-text-secondary">Señal fuerte</p>
              </div>
              <span className="material-symbols-outlined text-secondary-500">check_circle</span>
            </div>

            <div className="space-y-3 pt-4 border-t border-border-subtle">
              <div className="flex items-center justify-between">
                <span className="font-body-md text-body-md text-text-secondary">
                  Tipo de actividad
                </span>
                <span className="font-body-md text-body-md text-text-primary font-medium">
                  Carrera
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="font-body-md text-body-md text-text-secondary">Ubicación</span>
                <span className="font-body-md text-body-md text-text-primary font-medium">
                  Loja, Ecuador
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="font-body-md text-body-md text-text-secondary">Temperatura</span>
                <span className="font-body-md text-body-md text-text-primary font-medium">
                  14°C
                </span>
              </div>
            </div>
          </div>
        </div>

        <button
          onClick={handleStart}
          className="w-24 h-24 rounded-full bg-primary-600 hover:bg-primary-700 active:scale-95 shadow-[0_12px_28px_-4px_rgba(217,119,6,0.5)] flex items-center justify-center transition-all"
        >
          <span className="material-symbols-outlined text-[48px] text-bg-primary">play_arrow</span>
        </button>

        <p className="font-label-lg text-label-lg text-text-secondary mt-6">Toca para comenzar</p>
      </main>
    </div>
  )
}
