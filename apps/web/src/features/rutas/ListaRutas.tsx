import { Link } from 'react-router-dom';

export function ListaRutas() {
  const tabs = ['Explorar', 'Mis Rutas', 'Guardadas'];
  const activeTab = 'Explorar';

  const routes = [
    { id: 1, name: 'Parque Jipiro', distance: 5.2, difficulty: 'Fácil', elevation: 45 },
    { id: 2, name: 'Río Zamora', distance: 8.5, difficulty: 'Moderado', elevation: 120 },
    { id: 3, name: 'Cerro Villonaco', distance: 12.3, difficulty: 'Difícil', elevation: 380 },
  ];

  return (
    <div className="flex flex-col w-full">
      <div className="px-margin pt-space-md pb-space-sm">
        <h1 className="font-headline-md text-headline-md text-text-primary mb-4">
          Rutas de Loja
        </h1>

        <div className="flex gap-2 mb-4">
          {tabs.map((tab) => (
            <button
              key={tab}
              className={`px-4 py-2 rounded-full font-label-md text-label-md transition-colors ${
                tab === activeTab
                  ? 'bg-primary-600 text-bg-primary'
                  : 'bg-surface-container text-text-secondary hover:text-text-primary'
              }`}
            >
              {tab}
            </button>
          ))}
        </div>

        <Link to="/rutas/buscar" className="relative block">
          <input
            type="text"
            placeholder="Buscar rutas..."
            className="w-full h-12 pl-12 pr-4 bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary focus:outline-none focus:ring-2 focus:ring-primary-600 focus:border-transparent"
            readOnly
          />
          <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-text-secondary">
            search
          </span>
        </Link>
      </div>

      <div className="px-margin space-y-4 pb-space-3xl">
        {routes.map((route) => (
          <Link
            key={route.id}
            to={`/rutas/${route.id}`}
            className="block bg-surface-container rounded-2xl border border-border-subtle overflow-hidden hover:border-primary-600/50 transition-colors"
          >
            <div className="h-32 bg-gradient-to-br from-primary-600/30 to-secondary-600/30 flex items-center justify-center">
              <span className="material-symbols-outlined text-[64px] text-primary">route</span>
            </div>
            <div className="p-4">
              <h3 className="font-title-md text-title-md text-text-primary mb-2">
                {route.name}
              </h3>
              <div className="flex items-center gap-4 text-text-secondary">
                <span className="font-label-md text-label-md flex items-center gap-1">
                  <span className="material-symbols-outlined text-[16px]">straighten</span>
                  {route.distance} km
                </span>
                <span className="font-label-md text-label-md flex items-center gap-1">
                  <span className="material-symbols-outlined text-[16px]">trending_up</span>
                  {route.elevation}m
                </span>
                <span className={`font-label-md text-label-md px-2 py-0.5 rounded-full ${
                  route.difficulty === 'Fácil'
                    ? 'bg-secondary-600/20 text-secondary-500'
                    : route.difficulty === 'Moderado'
                    ? 'bg-primary-600/20 text-primary-500'
                    : 'bg-danger/20 text-danger'
                }`}>
                  {route.difficulty}
                </span>
              </div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
