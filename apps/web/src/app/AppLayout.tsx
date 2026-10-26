import { Link, Outlet, useLocation } from 'react-router-dom'
import { notificacionesVisibles } from '../contexts/avisos/api'
import { FotoPerfil } from '../contexts/identidad'
import { useApp } from '../store/app'

export function AppLayout() {
  const location = useLocation()

  const isActive = (path: string) =>
    location.pathname === path || location.pathname.startsWith(`${path}/`)
  const ajustes = useApp((s) => s.ajustes)
  const sesion = useApp((s) => s.sesion)
  const listaNotif = useApp((s) => s.notificaciones)
  const sinLeer = notificacionesVisibles(ajustes, listaNotif).filter((n) => !n.leida).length

  // Comunidad (Figma C2) trae su propia cabecera: sin barra superior en movil.
  const sinBarra = location.pathname === '/comunidad'

  const navItems = [
    { path: '/inicio', label: 'Inicio', icon: 'home' },
    { path: '/rutas', label: 'Rutas', icon: 'map' },
    { path: '/correr', label: '', icon: 'play_circle', isMain: true },
    { path: '/comunidad', label: 'Comunidad', icon: 'groups' },
    { path: '/perfil', label: 'Perfil', icon: 'person' },
  ]

  return (
    <div className="min-h-screen flex flex-col bg-bg-primary text-text-primary">
      <header
        className={`${sinBarra ? 'hidden' : 'lg:hidden'} fixed top-0 inset-x-0 z-50 bg-bg-secondary/95 backdrop-blur-xl shadow-[0_1px_8px_rgba(0,0,0,0.25)] pt-safe`}
      >
        <div className="h-16 px-margin flex items-center justify-between gap-space-sm">
          <div className="flex items-center gap-space-sm min-w-0">
            <Link to="/perfil" aria-label="Ir a mi perfil">
              {sesion?.foto ? (
                <FotoPerfil foto={sesion.foto} nombre={sesion.nombre} className="h-8 w-8" />
              ) : (
                <div className="h-8 w-8 bg-primary-600 rounded-full flex items-center justify-center">
                  <span className="text-on-primary font-bold text-sm">GL</span>
                </div>
              )}
            </Link>
            <div className="flex flex-col min-w-0">
              <span className="font-label-md text-label-md text-primary truncate uppercase tracking-wider">
                Gacelas Loja
              </span>
            </div>
          </div>
          <div className="flex items-center gap-space-xs">
            <Link
              to="/notificaciones"
              aria-label={sinLeer ? `Notificaciones (${sinLeer} sin leer)` : 'Notificaciones'}
              className="relative w-12 h-12 flex items-center justify-center rounded-full text-text-secondary hover:text-text-primary hover:bg-surface-container active:scale-95 transition-all"
            >
              <span className="material-symbols-outlined text-[24px]">notifications</span>
              {sinLeer > 0 && (
                <span
                  data-testid="punto-notificaciones"
                  className="absolute top-2.5 right-2.5 w-2 h-2 rounded-full bg-primary-container ring-2 ring-bg-secondary"
                ></span>
              )}
            </Link>
          </div>
        </div>
      </header>

      <main
        className={`flex-1 ${sinBarra ? 'pt-0' : 'pt-16'} pb-24 lg:pt-8 lg:pb-12 overflow-auto`}
      >
        <Outlet />
      </main>

      <nav className="lg:hidden fixed bottom-0 inset-x-0 z-40 bg-bg-secondary/95 backdrop-blur-xl border-t border-border-subtle pb-safe">
        <div className="flex items-center justify-around h-20 px-2">
          {navItems.map((item) =>
            item.isMain ? (
              <Link
                key={item.path}
                to={item.path}
                aria-label="Correr"
                className="flex flex-col items-center justify-center -mt-8"
              >
                <div className="w-14 h-14 rounded-full bg-primary-600 shadow-lg flex items-center justify-center text-on-primary active:scale-95 transition-transform">
                  <span className="material-symbols-outlined text-[32px]">{item.icon}</span>
                </div>
              </Link>
            ) : (
              <Link
                key={item.path}
                to={item.path}
                className={`flex flex-col items-center justify-center gap-1 px-3 py-2 rounded-lg transition-colors ${
                  isActive(item.path)
                    ? 'text-primary'
                    : 'text-text-secondary hover:text-text-primary'
                }`}
              >
                <span className="material-symbols-outlined text-[24px]">{item.icon}</span>
                <span className="font-label-md text-label-md">{item.label}</span>
              </Link>
            )
          )}
        </div>
      </nav>
    </div>
  )
}
