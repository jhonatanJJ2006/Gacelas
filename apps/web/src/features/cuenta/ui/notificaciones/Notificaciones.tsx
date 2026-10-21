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
import { retoMes } from '../../../../lib/estadisticas'
import { FilaInterruptor } from '../ajustes/Comunes'

export function Notificaciones() {
  const navigate = useNavigate()
  const ajustes = useApp((s) => s.ajustes)
  const lista = useApp((s) => s.notificaciones)
  const marcar = useApp((s) => s.marcarNotificacionesLeidas)
  const [filtro, setFiltro] = useState<FiltroNotificaciones>('Todas')
  const notificaciones = filtrarNotificaciones(notificacionesVisibles(ajustes, lista), filtro)
  const actualizar = useApp((s) => s.actualizarAjustes)
  const carreras = useApp((s) => s.carreras)
  const reto = retoMes(carreras, Date.now())
  const pctReto = Math.min(100, Math.round((reto.km / reto.metaKm) * 100))
  const nuevas = lista.filter((n) => !n.leida).length

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
            <div>
              <h1 className="font-headline-md text-headline-md text-text-primary">
                Notificaciones
              </h1>
              <p className="text-body-sm text-text-secondary hidden sm:block">
                Actualizaciones de ruta, club y rendimiento
              </p>
            </div>
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
                className={`px-4 py-2 rounded-full font-label-md text-label-md ${filtro === f ? 'bg-primary-600 text-on-primary' : 'bg-surface-container text-text-secondary hover:text-text-primary'}`}
              >
                {f}
              </button>
            ))}
          </div>
        </div>

        <div className="px-margin flex items-center justify-between mb-2">
          <span className="text-label-md text-primary uppercase tracking-wide flex items-center gap-2">
            Recientes
            {nuevas > 0 && (
              <span className="px-2 py-0.5 rounded-full bg-primary-600/20 text-primary normal-case">
                {nuevas} {nuevas === 1 ? 'nueva' : 'nuevas'}
              </span>
            )}
          </span>
          <span className="text-body-sm text-text-secondary">Loja, Ecuador</span>
        </div>
        <div className="px-margin space-y-2">
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
                className={`w-full px-4 py-4 rounded-2xl border flex items-start gap-3 hover:bg-surface-container-high transition-colors text-left bg-surface-container ${
                  !notif.leida ? 'border-primary-600/40' : 'border-border-subtle'
                }`}
              >
                <div
                  className={`w-10 h-10 rounded-xl bg-surface-container-highest flex items-center justify-center shrink-0 ${color}`}
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

        <section className="px-margin mt-6" data-testid="reto-notificacion">
          <h2 className="text-label-md text-primary uppercase tracking-wide mb-2">Esta semana</h2>
          <div className="p-4 rounded-2xl bg-surface-container border border-border-subtle space-y-2">
            <div className="flex items-center gap-3">
              <span className="material-symbols-outlined text-primary">flag</span>
              <div>
                <div className="font-title-sm text-title-sm text-text-primary">
                  Progreso de reto mensual
                </div>
                <div className="text-body-sm text-text-secondary">{reto.titulo}</div>
              </div>
            </div>
            <div className="flex justify-between text-label-md">
              <span className="text-text-primary">
                {reto.km.toFixed(1)} de {reto.metaKm} km acumulados
              </span>
              <span className="text-primary">{pctReto}%</span>
            </div>
            <div className="h-2 rounded-full bg-surface-container-highest overflow-hidden">
              <div className="h-full bg-primary-600" style={{ width: `${pctReto}%` }} />
            </div>
            <div className="flex justify-between text-body-sm text-text-secondary">
              <span>Faltan {Math.max(0, reto.metaKm - reto.km).toFixed(1)} km</span>
              <span>Cierra en {reto.diasRestantes} días</span>
            </div>
          </div>
        </section>

        <section className="px-margin mt-6 space-y-2">
          <div className="flex justify-between items-baseline">
            <h2 className="text-label-md text-primary uppercase tracking-wide">
              Preferencias de alertas
            </h2>
            <span className="text-body-sm text-text-secondary">Ajustes directos</span>
          </div>
          <FilaInterruptor
            icono="event"
            titulo="Eventos del club"
            descripcion="Quedadas en Jipiro, Vilcabamba y Zamora"
            activo={ajustes.notifEventos}
            onCambiar={(v) => void actualizar({ notifEventos: v })}
          />
          <FilaInterruptor
            icono="forum"
            titulo="Comentarios y aplausos"
            descripcion="Interacción social de compañeros de ruta"
            activo={ajustes.notifComentarios && ajustes.notifLikes}
            onCambiar={(v) => void actualizar({ notifComentarios: v, notifLikes: v })}
          />
          <FilaInterruptor
            icono="person_add"
            titulo="Nuevos seguidores"
            descripcion="Corredores que empiezan a seguirte"
            activo={ajustes.notifSeguidores}
            onCambiar={(v) => void actualizar({ notifSeguidores: v })}
          />
          <FilaInterruptor
            icono="sensors"
            titulo="Corredores en vivo"
            descripcion="Aviso cuando alguien que sigues sale a correr"
            activo={ajustes.notifEnVivo}
            onCambiar={(v) => void actualizar({ notifEnVivo: v })}
          />
        </section>
      </main>
    </div>
  )
}
