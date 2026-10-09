import { useNavigate } from 'react-router-dom'

export function DetalleEvento() {
  const navigate = useNavigate()

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <div className="h-64 bg-gradient-to-br from-primary-600 to-secondary-600 relative">
        <button
          onClick={() => navigate('/comunidad/eventos')}
          className="absolute top-4 left-4 w-10 h-10 rounded-full bg-black/60 backdrop-blur-md flex items-center justify-center text-white shadow-lg"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </button>

        <div className="absolute inset-0 flex items-center justify-center">
          <span className="material-symbols-outlined text-[120px] text-white/20">event</span>
        </div>

        <div className="absolute bottom-4 left-4 right-4">
          <div className="bg-black/60 backdrop-blur-md px-3 py-1.5 rounded-full inline-flex items-center gap-2">
            <span className="material-symbols-outlined text-[16px] text-primary-400">
              directions_run
            </span>
            <span className="text-white font-label-md text-label-md">10 km</span>
          </div>
        </div>
      </div>

      <div className="flex-1 px-6 py-6 pb-24">
        <h1 className="font-headline-lg text-headline-lg text-text-primary mb-4">
          Carrera Parque Jipiro
        </h1>

        <div className="space-y-3 mb-6">
          <div className="flex items-start gap-3">
            <div className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center shrink-0">
              <span className="material-symbols-outlined text-[20px] text-primary-500">
                calendar_today
              </span>
            </div>
            <div>
              <div className="font-body-md text-body-md text-text-primary font-semibold">
                Sábado 11 de Octubre, 2026
              </div>
              <div className="font-body-sm text-body-sm text-text-secondary">6:00 AM - 8:00 AM</div>
            </div>
          </div>

          <div className="flex items-start gap-3">
            <div className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center shrink-0">
              <span className="material-symbols-outlined text-[20px] text-secondary-500">
                location_on
              </span>
            </div>
            <div>
              <div className="font-body-md text-body-md text-text-primary font-semibold">
                Parque Jipiro
              </div>
              <div className="font-body-sm text-body-sm text-text-secondary">
                Entrada principal, Loja, Ecuador
              </div>
            </div>
          </div>
        </div>

        <div className="bg-surface-container rounded-2xl p-4 mb-6">
          <h2 className="font-title-md text-title-md text-text-primary mb-3">Descripción</h2>
          <p className="font-body-md text-body-md text-text-secondary leading-relaxed">
            Carrera grupal de 10km por el Parque Jipiro. Recorrido panorámico alrededor del parque
            con vistas espectaculares. Todos los niveles son bienvenidos. Habrá hidratación en los
            km 3, 5 y 7.
          </p>
        </div>

        <div className="bg-surface-container rounded-2xl p-4 mb-6">
          <h2 className="font-title-md text-title-md text-text-primary mb-3">Participantes (15)</h2>
          <div className="flex flex-wrap gap-2">
            {[...Array(15)].map((_, i) => (
              <div
                key={i}
                className="w-12 h-12 rounded-full bg-surface-variant border-2 border-border-subtle"
              ></div>
            ))}
          </div>
        </div>

        <div className="bg-surface-container rounded-2xl p-4 mb-6">
          <h2 className="font-title-md text-title-md text-text-primary mb-3">
            Detalles de la ruta
          </h2>
          <div className="grid grid-cols-3 gap-4">
            <div className="text-center">
              <div className="font-headline-sm text-headline-sm text-primary">10</div>
              <div className="font-label-md text-label-md text-text-secondary">km</div>
            </div>
            <div className="text-center">
              <div className="font-headline-sm text-headline-sm text-secondary">45m</div>
              <div className="font-label-md text-label-md text-text-secondary">elevación</div>
            </div>
            <div className="text-center">
              <div className="font-headline-sm text-headline-sm text-text-primary">Fácil</div>
              <div className="font-label-md text-label-md text-text-secondary">dificultad</div>
            </div>
          </div>
        </div>

        <button className="w-full h-14 bg-primary-600 hover:bg-primary-700 active:scale-[0.98] text-bg-primary font-headline-md font-bold text-base rounded-xl flex items-center justify-center gap-2 shadow-[0_8px_20px_-4px_rgba(217,119,6,0.35)] transition-all">
          <span className="material-symbols-outlined text-[20px]">check_circle</span>
          <span>Inscribirme al evento</span>
        </button>
      </div>
    </div>
  )
}
