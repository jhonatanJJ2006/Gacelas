import { useNavigate } from 'react-router-dom'

interface Notificacion {
  id: string
  tipo: 'like' | 'comment' | 'follow' | 'event' | 'live'
  usuario: string
  mensaje: string
  tiempo: string
  leida: boolean
}

export function Notificaciones() {
  const navigate = useNavigate()

  const notificaciones: Notificacion[] = [
    {
      id: '1',
      tipo: 'live',
      usuario: 'María López',
      mensaje: 'está corriendo ahora',
      tiempo: 'hace 5 min',
      leida: false,
    },
    {
      id: '2',
      tipo: 'like',
      usuario: 'Carlos Ruiz',
      mensaje: 'le dio like a tu carrera matutina',
      tiempo: 'hace 1h',
      leida: false,
    },
    {
      id: '3',
      tipo: 'comment',
      usuario: 'Ana Martínez',
      mensaje: 'comentó en tu actividad: "Excelente ritmo"',
      tiempo: 'hace 2h',
      leida: true,
    },
    {
      id: '4',
      tipo: 'follow',
      usuario: 'Pedro Sánchez',
      mensaje: 'comenzó a seguirte',
      tiempo: 'hace 3h',
      leida: true,
    },
    {
      id: '5',
      tipo: 'event',
      usuario: 'Gacelas Loja',
      mensaje: 'Nuevo evento: Carrera Parque Jipiro',
      tiempo: 'hace 1d',
      leida: true,
    },
  ]

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
              onClick={() => navigate('/inicio')}
              className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors"
            >
              <span className="material-symbols-outlined">arrow_back</span>
            </button>
            <h1 className="font-headline-md text-headline-md text-text-primary">Notificaciones</h1>
          </div>
          <button className="text-primary-500 font-label-lg text-label-lg hover:text-primary-400">
            Marcar todas leídas
          </button>
        </div>
      </header>

      <main className="flex-1 pt-16 pb-24">
        <div className="px-margin pt-space-sm">
          <div className="flex gap-2 mb-4">
            <button className="px-4 py-2 rounded-full font-label-md text-label-md bg-primary-600 text-bg-primary">
              Todas
            </button>
            <button className="px-4 py-2 rounded-full font-label-md text-label-md bg-surface-container text-text-secondary hover:text-text-primary">
              Sin leer
            </button>
            <button className="px-4 py-2 rounded-full font-label-md text-label-md bg-surface-container text-text-secondary hover:text-text-primary">
              En vivo
            </button>
          </div>
        </div>

        <div className="divide-y divide-border-subtle">
          {notificaciones.map((notif) => {
            const { icon, color } = getIcono(notif.tipo)
            return (
              <button
                key={notif.id}
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
