import { Link } from 'react-router-dom'

export function Perfil() {
  return (
    <div className="flex flex-col w-full">
      <div className="px-margin pt-space-md pb-space-xl">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-4">
            <div className="w-20 h-20 rounded-full bg-surface-variant border-2 border-primary-600"></div>
            <div>
              <h1 className="font-headline-md text-headline-md text-text-primary mb-1">
                Jhonatan Runner
              </h1>
              <p className="font-body-md text-body-md text-text-secondary">@jhonatan_runs</p>
            </div>
          </div>
          <Link
            to="/perfil/editar"
            className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors"
          >
            <span className="material-symbols-outlined">edit</span>
          </Link>
        </div>

        <div className="grid grid-cols-3 gap-4 mb-6">
          <div className="text-center bg-surface-container rounded-xl p-4">
            <div className="font-headline-lg text-headline-lg text-primary leading-none mb-1">
              142
            </div>
            <div className="font-label-md text-label-md text-text-secondary uppercase">
              Carreras
            </div>
          </div>
          <div className="text-center bg-surface-container rounded-xl p-4">
            <div className="font-headline-lg text-headline-lg text-text-primary leading-none mb-1">
              248
            </div>
            <div className="font-label-md text-label-md text-text-secondary uppercase">
              Seguidores
            </div>
          </div>
          <div className="text-center bg-surface-container rounded-xl p-4">
            <div className="font-headline-lg text-headline-lg text-text-primary leading-none mb-1">
              189
            </div>
            <div className="font-label-md text-label-md text-text-secondary uppercase">
              Siguiendo
            </div>
          </div>
        </div>

        <div className="bg-surface-container rounded-2xl border border-border-subtle p-6 mb-6">
          <h2 className="font-title-md text-title-md text-text-primary mb-4">
            Estadísticas del mes
          </h2>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary">straighten</span>
                <span className="font-body-md text-body-md text-text-primary">Distancia total</span>
              </div>
              <span className="font-headline-sm text-headline-sm text-primary">42.5 km</span>
            </div>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-secondary">schedule</span>
                <span className="font-body-md text-body-md text-text-primary">Tiempo total</span>
              </div>
              <span className="font-headline-sm text-headline-sm text-secondary">4:25:30</span>
            </div>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-text-primary">trending_up</span>
                <span className="font-body-md text-body-md text-text-primary">Elevación</span>
              </div>
              <span className="font-headline-sm text-headline-sm text-text-primary">520 m</span>
            </div>
          </div>
        </div>

        <div className="space-y-3">
          <Link
            to="/perfil/avatar"
            className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl hover:bg-surface-container-high transition-colors"
          >
            <div className="flex items-center gap-3">
              <span className="material-symbols-outlined text-primary">person</span>
              <span className="font-body-md text-body-md text-text-primary">
                Personalizar avatar 3D
              </span>
            </div>
            <span className="material-symbols-outlined text-text-secondary">chevron_right</span>
          </Link>

          <Link
            to="/ajustes"
            className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl hover:bg-surface-container-high transition-colors"
          >
            <div className="flex items-center gap-3">
              <span className="material-symbols-outlined text-text-secondary">settings</span>
              <span className="font-body-md text-body-md text-text-primary">
                Ajustes y preferencias
              </span>
            </div>
            <span className="material-symbols-outlined text-text-secondary">chevron_right</span>
          </Link>
        </div>
      </div>

      <div className="px-margin pb-space-3xl">
        <h2 className="font-title-md text-title-md text-text-primary mb-4">Actividad reciente</h2>
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className="bg-surface-container rounded-xl p-4 border border-border-subtle"
            >
              <div className="flex items-center justify-between mb-2">
                <span className="font-body-md text-body-md text-text-primary font-semibold">
                  Carrera matutina
                </span>
                <span className="font-body-sm text-body-sm text-text-secondary">
                  hace {i} día{i > 1 ? 's' : ''}
                </span>
              </div>
              <div className="flex items-center gap-4 text-text-secondary">
                <span className="font-label-md text-label-md">5.2 km</span>
                <span className="font-label-md text-label-md">28:34</span>
                <span className="font-label-md text-label-md">5:30 /km</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
