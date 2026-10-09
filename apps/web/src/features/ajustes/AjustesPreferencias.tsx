import { useNavigate, Link } from 'react-router-dom';

export function AjustesPreferencias() {
  const navigate = useNavigate();

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <header className="px-margin py-4 border-b border-border-subtle">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/perfil')}
            className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors"
          >
            <span className="material-symbols-outlined">arrow_back</span>
          </button>
          <h1 className="font-headline-md text-headline-md text-text-primary">
            Ajustes
          </h1>
        </div>
      </header>

      <main className="flex-1 px-margin py-6 space-y-6 pb-24">
        <section>
          <h2 className="font-title-md text-title-md text-text-primary mb-3">
            Cuenta
          </h2>
          <div className="space-y-2">
            <Link
              to="/perfil/editar"
              className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl hover:bg-surface-container-high transition-colors"
            >
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-text-secondary">person</span>
                <span className="font-body-md text-body-md text-text-primary">
                  Editar perfil
                </span>
              </div>
              <span className="material-symbols-outlined text-text-secondary">chevron_right</span>
            </Link>
          </div>
        </section>

        <section>
          <h2 className="font-title-md text-title-md text-text-primary mb-3">
            Privacidad
          </h2>
          <div className="space-y-2">
            <Link
              to="/ajustes/privacidad"
              className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl hover:bg-surface-container-high transition-colors"
            >
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-text-secondary">lock</span>
                <span className="font-body-md text-body-md text-text-primary">
                  Privacidad y ubicación
                </span>
              </div>
              <span className="material-symbols-outlined text-text-secondary">chevron_right</span>
            </Link>

            <Link
              to="/ajustes/notificaciones"
              className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl hover:bg-surface-container-high transition-colors"
            >
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-text-secondary">notifications</span>
                <span className="font-body-md text-body-md text-text-primary">
                  Notificaciones
                </span>
              </div>
              <span className="material-symbols-outlined text-text-secondary">chevron_right</span>
            </Link>
          </div>
        </section>

        <section>
          <h2 className="font-title-md text-title-md text-text-primary mb-3">
            General
          </h2>
          <div className="space-y-2">
            <div className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl">
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-text-secondary">dark_mode</span>
                <span className="font-body-md text-body-md text-text-primary">
                  Modo oscuro
                </span>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input type="checkbox" className="sr-only peer" defaultChecked />
                <div className="w-11 h-6 bg-surface-container-high peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-primary-600 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
              </label>
            </div>

            <div className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl">
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-text-secondary">straighten</span>
                <span className="font-body-md text-body-md text-text-primary">
                  Unidades métricas
                </span>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input type="checkbox" className="sr-only peer" defaultChecked />
                <div className="w-11 h-6 bg-surface-container-high peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-primary-600 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
              </label>
            </div>
          </div>
        </section>

        <section>
          <h2 className="font-title-md text-title-md text-text-primary mb-3">
            Información
          </h2>
          <div className="space-y-2">
            <button className="w-full flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl hover:bg-surface-container-high transition-colors">
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-text-secondary">info</span>
                <span className="font-body-md text-body-md text-text-primary">
                  Acerca de
                </span>
              </div>
              <span className="material-symbols-outlined text-text-secondary">chevron_right</span>
            </button>

            <button className="w-full flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl hover:bg-surface-container-high transition-colors">
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-text-secondary">help</span>
                <span className="font-body-md text-body-md text-text-primary">
                  Ayuda y soporte
                </span>
              </div>
              <span className="material-symbols-outlined text-text-secondary">chevron_right</span>
            </button>
          </div>
        </section>

        <section>
          <button
            onClick={() => navigate('/bienvenida')}
            className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-danger/20 text-danger rounded-xl hover:bg-danger/30 transition-colors"
          >
            <span className="material-symbols-outlined">logout</span>
            <span className="font-body-md text-body-md font-medium">
              Cerrar sesión
            </span>
          </button>
        </section>
      </main>
    </div>
  );
}
