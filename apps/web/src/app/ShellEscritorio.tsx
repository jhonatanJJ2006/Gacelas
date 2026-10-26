import { Link, Outlet, useLocation } from 'react-router-dom'
import { InsigniaRacha } from '../contexts/actividad'
import { notificacionesVisibles, useAvisos } from '../contexts/avisos/api'
import { FotoPerfil } from '../contexts/identidad'
import { useIdentidad } from '../contexts/identidad/api'
import { useEsEscritorio } from '../shared/lib/useEsEscritorio'

const NAV_ESCRITORIO = [
  { path: '/inicio', label: 'Inicio', icon: 'home' },
  { path: '/correr', label: 'Correr', icon: 'play_circle' },
  { path: '/rutas', label: 'Rutas', icon: 'map' },
  { path: '/comunidad', label: 'Comunidad', icon: 'groups', exacto: true },
  { path: '/comunidad/eventos', label: 'Eventos', icon: 'event' },
  { path: '/comunidad/corredores', label: 'Corredores', icon: 'person_search' },
  { path: '/notificaciones', label: 'Notificaciones', icon: 'notifications' },
  { path: '/perfil', label: 'Perfil', icon: 'person' },
  { path: '/ajustes', label: 'Ajustes', icon: 'settings' },
]

/**
 * Contenedor de escritorio (>=1024px): barra lateral fija a la izquierda.
 * En movil no pinta nada extra; cada pantalla mantiene su diseno movil.
 */
export function ShellEscritorio() {
  const { pathname } = useLocation()
  const escritorio = useEsEscritorio()
  const sesion = useIdentidad((s) => s.sesion)
  const ajustes = useIdentidad((s) => s.ajustes)
  const listaNotif = useAvisos((s) => s.notificaciones)
  const sinLeer = notificacionesVisibles(ajustes, listaNotif).filter((n) => !n.leida).length

  const activo = (path: string, exacto?: boolean) => {
    if (exacto)
      return (
        pathname === path ||
        (pathname.startsWith(`${path}/`) &&
          !pathname.startsWith('/comunidad/eventos') &&
          !pathname.startsWith('/comunidad/corredores'))
      )
    return pathname === path || pathname.startsWith(`${path}/`)
  }

  return (
    <div className="shell-escritorio min-h-screen bg-bg-primary">
      {escritorio && (
        <aside
          aria-label="Navegación principal"
          className="hidden lg:flex fixed inset-y-0 left-0 z-[60] w-[260px] flex-col bg-bg-secondary border-r border-border-subtle"
        >
          <Link to="/inicio" className="flex items-center gap-3 px-6 h-20 shrink-0">
            <div className="h-10 w-10 rounded-xl bg-primary-600 flex items-center justify-center shadow-[0_8px_24px_-4px_rgba(123,31,162,0.45)]">
              <span className="material-symbols-outlined text-on-primary">directions_run</span>
            </div>
            <div className="flex flex-col leading-tight">
              <span className="font-headline-sm text-[20px] font-bold text-text-primary">
                Gacelas
              </span>
              <span className="font-label-md text-label-md text-primary uppercase tracking-wider">
                Loja
              </span>
            </div>
          </Link>
          <div className="px-6 pb-4">
            <InsigniaRacha testId="racha-barra" />
          </div>
          <nav className="flex-1 overflow-y-auto px-3 py-2 space-y-1">
            {NAV_ESCRITORIO.map((item) => {
              const on = activo(item.path, item.exacto)
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  aria-current={on ? 'page' : undefined}
                  className={`flex items-center gap-3 h-12 px-4 rounded-xl font-label-md text-[15px] transition-colors ${
                    on
                      ? 'bg-primary/15 text-primary'
                      : 'text-text-secondary hover:text-text-primary hover:bg-surface-container'
                  }`}
                >
                  <span className="material-symbols-outlined text-[22px]">{item.icon}</span>
                  <span className="flex-1">{item.label}</span>
                  {item.path === '/notificaciones' && sinLeer > 0 && (
                    <span className="min-w-6 h-6 px-1.5 rounded-full bg-primary-600 text-on-primary text-xs font-bold flex items-center justify-center">
                      {sinLeer}
                    </span>
                  )}
                </Link>
              )
            })}
          </nav>
          <div className="p-4 border-t border-border-subtle">
            <Link
              to="/correr"
              className="mb-4 h-12 rounded-xl bg-primary-600 text-on-primary font-bold flex items-center justify-center gap-2 hover:bg-primary-700 transition-colors"
            >
              <span className="material-symbols-outlined">play_arrow</span>
              Empezar a correr
            </Link>
            <Link to="/perfil" className="flex items-center gap-3 min-w-0">
              {sesion?.foto ? (
                <FotoPerfil foto={sesion.foto} nombre={sesion.nombre} className="h-10 w-10" />
              ) : (
                <div className="h-10 w-10 rounded-full bg-primary-600 flex items-center justify-center text-on-primary font-bold">
                  {(sesion?.nombre ?? 'G').slice(0, 1).toUpperCase()}
                </div>
              )}
              <div className="min-w-0">
                <p className="text-sm font-semibold text-text-primary truncate">
                  {sesion?.nombre ?? 'Corredor'}
                </p>
                <p className="text-xs text-text-secondary truncate">{sesion?.email ?? ''}</p>
              </div>
            </Link>
          </div>
        </aside>
      )}
      <div className="lg:pl-[260px]">
        <Outlet />
      </div>
    </div>
  )
}
