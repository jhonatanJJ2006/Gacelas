import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  filtrarNotificaciones,
  notificacionesVisibles,
  type FiltroNotificaciones,
} from '../../../../lib/filtros'
import type { Notificacion } from '../../../../mocks/data'
import { useApp } from '../../../../store/app'
import { destinoNotificacion } from '../../../../lib/logica'

export function Notificaciones() {
  const navigate = useNavigate()
  const ajustes = useApp((s) => s.ajustes)
  const lista = useApp((s) => s.notificaciones)
  const marcar = useApp((s) => s.marcarNotificacionesLeidas)
  const [filtro, setFiltro] = useState<FiltroNotificaciones>('Todas')
  const notificaciones = filtrarNotificaciones(notificacionesVisibles(ajustes, lista), filtro)

  const marcarTodas = () => void marcar(lista.filter((n) => !n.leida).map((n) => n.id))
  const abrir = (n: Notificacion) => {
    void marcar([n.id])
    navigate(destinoNotificacion(n))
  }

  const getIcono = (tipo: string) => {
    switch (tipo) {
      case 'like':
        return { icon: 'favorite', color: 'text-danger' }
      case 'comment':
        return { icon: 'chat_bubble', color: 'text-primary-500' }
      case 'follow':
        return { icon: 'person_add', color: 'text-secondary-500' }
      case 'event':
        return { icon: 'event', color: 'text-tertiary' }
      case 'streak':
        return { icon: 'local_fire_department', color: 'text-primary' }
      case 'live':
        return { icon: 'sensors', color: 'text-secondary-500' }
      default:
        return { icon: 'notifications', color: 'text-text-secondary' }
    }
  }

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <header className="fixed top-0 inset-x-0 z-50 bg-bg-secondary/95 backdrop-blur-xl shadow-[0_1px_8px_rgba(0,0,0,0.25)] pt-safe">
        <div className="h-16 px-margin flex items-center justify-between gap-space-sm">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate(-1)}
              aria-label="Volver"
              className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors"
            >
              <span className="material-symbols-outlined">arrow_back</span>
            </button>
            <h1 className="font-headline-md text-headline-md text-text-primary">Notificaciones</h1>
          </div>
          <button
            onClick={marcarTodas}
            className="inline-block py-2 text-primary-500 font-label-lg text-label-lg hover:text-primary-400"
          >
            Marcar todas leídas
          </button>
        </div>
      </header>

      <main className="lg:w-full lg:max-w-3xl lg:mx-auto flex-1 pt-16 pb-24">
        <div className="px-margin pt-space-sm">
          <div className="flex gap-2 mb-4" role="tablist">
            {(['Todas', 'Sin leer', 'En vivo'] as FiltroNotificaciones[]).map((f) => (
              <button
                key={f}
                role="tab"
                aria-selected={filtro === f}
                onClick={() => setFiltro(f)}
                className={`px-4 py-2 rounded-full font-label-md text-label-md ${filtro === f ? 'bg-primary-600 text-bg-primary' : 'bg-surface-container text-text-secondary hover:text-text-primary'}`}
              >
                {f}
              </button>
            ))}
          </div>
        </div>

        <div className="divide-y divide-border-subtle">
          {notificaciones.length === 0 && (
            <p className="px-margin py-8 text-center text-text-secondary">No hay notificaciones.</p>
          )}
          {notificaciones.map((notif) => {
            const { icon, color } = getIcono(notif.tipo)
            return (
              <button
                key={notif.id}
                onClick={() => abrir(notif)}
                data-testid="notificacion"
                className={`w-full px-margin py-4 flex items-start gap-3 hover:bg-surface-container transition-colors text-left ${
                  !notif.leida ? 'bg-surface-container/50' : ''
                }`}
              >
                <div
                  className={`w-10 h-10 rounded-full bg-surface-container flex items-center justify-center shrink-0 ${color}`}
                >
                  <span className="material-symbols-outlined text-[20px]">{icon}</span>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-body-md text-body-md text-text-primary">
                    <span className="font-semibold">{notif.usuario}</span> {notif.mensaje}
                  </p>
                  <div className="flex items-center gap-2 mt-1">
                    <span className="font-body-sm text-body-sm text-text-secondary">
                      {notif.tiempo}
                    </span>
                    {!notif.leida && <span className="w-2 h-2 rounded-full bg-primary-600"></span>}
                  </div>
                </div>
                <span className="material-symbols-outlined text-text-secondary">chevron_right</span>
              </button>
            )
          })}
        </div>
      </main>
    </div>
  )
}
