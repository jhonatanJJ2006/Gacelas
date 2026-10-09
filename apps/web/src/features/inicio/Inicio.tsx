import { lazy, Suspense } from 'react';
import { Link } from 'react-router-dom';

const Runner3D = lazy(() => import('../../components/Runner3D').then(m => ({ default: m.Runner3D })));

export function Inicio() {
  return (
    <div className="flex flex-col w-full px-margin pb-space-3xl space-y-space-xl">
      <section className="flex flex-col space-y-space-xs pt-space-md">
        <div className="flex items-center justify-between">
          <div>
            <span className="font-label-md text-label-md text-primary flex items-center gap-1.5 uppercase tracking-wider">
              <span className="material-symbols-outlined text-[16px]">location_on</span>
              Loja, Ecuador
            </span>
            <h2 className="font-headline-md text-headline-md text-text-primary tracking-tight">
              Buenos días, Corredor
            </h2>
          </div>
          <div className="bg-surface-container px-3 py-1.5 rounded-full flex items-center gap-1.5 shadow-sm text-text-secondary">
            <span className="material-symbols-outlined text-[18px] text-primary-400">wb_twilight</span>
            <span className="font-label-md text-label-md text-on-surface">14°C Fresco</span>
          </div>
        </div>
      </section>

      <section className="relative flex flex-col rounded-3xl overflow-hidden bg-surface-container-high border border-border-subtle shadow-2xl">
        <div className="absolute top-3 inset-x-3 z-20 flex items-center justify-between pointer-events-none">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-bg-secondary/90 backdrop-blur-md text-primary border border-primary/20 shadow-md">
            <span className="w-2 h-2 rounded-full bg-secondary animate-ping"></span>
            <span className="font-label-md text-label-md font-bold tracking-wide">AUTOPILOT ACTIVO</span>
          </div>
          <div className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-surface-container-highest/90 backdrop-blur-md text-text-primary shadow-sm border border-white/5">
            <span className="text-xs">🔥</span>
            <span className="font-label-md text-label-md font-semibold text-primary">Racha 5d</span>
          </div>
        </div>

        <div className="relative w-full h-[430px] bg-surface-container-lowest">
          <Suspense fallback={
            <div className="w-full h-full flex items-center justify-center text-text-secondary">
              <div className="flex flex-col items-center gap-2">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
                <span className="text-sm">Cargando escena 3D...</span>
              </div>
            </div>
          }>
            <Runner3D animation="Idle" autoRotate cameraPosition={[0, 1.5, 4]} />
          </Suspense>
        </div>

        <div className="absolute bottom-4 inset-x-4 z-20 grid grid-cols-3 gap-2">
          <div className="bg-surface/90 backdrop-blur-md border border-border-subtle rounded-xl px-3 py-2.5 flex flex-col items-center shadow-lg">
            <span className="font-metric-huge text-metric-huge text-primary leading-none">8</span>
            <span className="font-label-md text-label-md text-text-secondary uppercase mt-0.5">Días</span>
          </div>
          <div className="bg-surface/90 backdrop-blur-md border border-border-subtle rounded-xl px-3 py-2.5 flex flex-col items-center shadow-lg">
            <span className="font-metric-huge text-metric-huge text-secondary leading-none">42</span>
            <span className="font-label-md text-label-md text-text-secondary uppercase mt-0.5">km</span>
          </div>
          <div className="bg-surface/90 backdrop-blur-md border border-border-subtle rounded-xl px-3 py-2.5 flex flex-col items-center shadow-lg">
            <span className="font-metric-huge text-metric-huge text-text-primary leading-none">5:20</span>
            <span className="font-label-md text-label-md text-text-secondary uppercase mt-0.5">/km</span>
          </div>
        </div>
      </section>

      <section className="space-y-space-md">
        <div className="flex items-center justify-between">
          <h3 className="font-title-md text-title-md text-text-primary">Acción rápida</h3>
        </div>

        <Link
          to="/correr"
          className="w-full bg-primary-600 hover:bg-primary-700 active:scale-[0.98] rounded-2xl p-6 flex items-center justify-between shadow-[0_8px_20px_-4px_rgba(217,119,6,0.35)] transition-all"
        >
          <div className="flex flex-col">
            <span className="font-headline-sm text-headline-sm text-bg-primary font-bold">
              Empezar a correr
            </span>
            <span className="font-body-md text-body-md text-bg-primary/80 mt-1">
              Graba tu siguiente carrera
            </span>
          </div>
          <div className="w-14 h-14 rounded-full bg-bg-primary/20 flex items-center justify-center">
            <span className="material-symbols-outlined text-[32px] text-bg-primary">play_arrow</span>
          </div>
        </Link>
      </section>

      <section className="space-y-space-md">
        <div className="flex items-center justify-between">
          <h3 className="font-title-md text-title-md text-text-primary">Semana atlética</h3>
          <span className="font-label-md text-label-md text-text-secondary">3 de 5 días</span>
        </div>

        <div className="flex gap-2">
          {['L', 'M', 'X', 'J', 'V', 'S', 'D'].map((day, index) => (
            <div
              key={day}
              className={`flex-1 aspect-square rounded-xl flex flex-col items-center justify-center ${
                index < 3
                  ? 'bg-secondary-600/20 border-2 border-secondary-600'
                  : 'bg-surface-container border border-border-subtle'
              }`}
            >
              <span className="font-label-md text-label-md text-text-primary">{day}</span>
              {index < 3 && (
                <span className="material-symbols-outlined text-[16px] text-secondary-500 mt-1">
                  check_circle
                </span>
              )}
            </div>
          ))}
        </div>
      </section>

      <section className="space-y-space-md">
        <div className="flex items-center justify-between">
          <h3 className="font-title-md text-title-md text-text-primary">Próximo encuentro</h3>
          <Link to="/comunidad/eventos" className="font-label-md text-label-md text-primary">
            Ver todo
          </Link>
        </div>

        <div className="bg-surface-container rounded-2xl border border-border-subtle overflow-hidden">
          <div className="h-32 bg-gradient-to-br from-primary-600 to-secondary-600 flex items-center justify-center">
            <span className="material-symbols-outlined text-[64px] text-white">event</span>
          </div>
          <div className="p-4">
            <div className="flex items-center gap-2 mb-2">
              <span className="material-symbols-outlined text-[16px] text-primary-500">calendar_today</span>
              <span className="font-label-md text-label-md text-text-secondary">
                Sábado 11 de Octubre, 6:00 AM
              </span>
            </div>
            <h4 className="font-title-md text-title-md text-text-primary mb-1">
              Carrera Parque Jipiro
            </h4>
            <p className="font-body-sm text-body-sm text-text-secondary mb-3">
              Carrera grupal de 10km por el Parque Jipiro. Todos los niveles bienvenidos.
            </p>
            <div className="flex items-center gap-2">
              <div className="flex -space-x-2">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="w-8 h-8 rounded-full bg-surface-variant border-2 border-surface-container"></div>
                ))}
              </div>
              <span className="font-label-md text-label-md text-text-secondary">
                +12 corredores
              </span>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
