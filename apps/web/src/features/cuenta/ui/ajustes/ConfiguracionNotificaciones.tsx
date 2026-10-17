import type { Ajustes } from '../../../../data/tipos'
import { useApp } from '../../../../store/app'
import { FilaInterruptor, PantallaAjustes, Seccion } from './Comunes'

type ClaveNotif =
  | 'notifLikes'
  | 'notifComentarios'
  | 'notifSeguidores'
  | 'notifEnVivo'
  | 'notifEventos'
  | 'notifEmail'

const PUSH: {
  clave: ClaveNotif
  icono: string
  color: string
  titulo: string
  descripcion: string
}[] = [
  {
    clave: 'notifLikes',
    icono: 'favorite',
    color: 'text-danger',
    titulo: 'Likes',
    descripcion: 'Cuando alguien da like a tu actividad',
  },
  {
    clave: 'notifComentarios',
    icono: 'chat_bubble',
    color: 'text-primary-500',
    titulo: 'Comentarios',
    descripcion: 'Cuando alguien comenta tu actividad',
  },
  {
    clave: 'notifSeguidores',
    icono: 'person_add',
    color: 'text-secondary-500',
    titulo: 'Nuevos seguidores',
    descripcion: 'Cuando alguien te sigue',
  },
  {
    clave: 'notifEnVivo',
    icono: 'sensors',
    color: 'text-secondary-500',
    titulo: 'Carreras en vivo',
    descripcion: 'Cuando alguien inicia seguimiento en vivo',
  },
  {
    clave: 'notifEventos',
    icono: 'event',
    color: 'text-tertiary',
    titulo: 'Eventos',
    descripcion: 'Recordatorios de eventos y nuevos eventos',
  },
]

export function ConfiguracionNotificaciones() {
  const ajustes = useApp((s) => s.ajustes)
  const actualizar = useApp((s) => s.actualizarAjustes)
  const cambiar = (clave: ClaveNotif, v: boolean) =>
    void actualizar({ [clave]: v } as Partial<Ajustes>)

  return (
    <PantallaAjustes titulo="Notificaciones" volverA="/ajustes">
      <Seccion titulo="Notificaciones push">
        {PUSH.map((n) => (
          <FilaInterruptor
            key={n.clave}
            icono={n.icono}
            color={n.color}
            titulo={n.titulo}
            descripcion={n.descripcion}
            activo={ajustes[n.clave]}
            onCambiar={(v) => cambiar(n.clave, v)}
          />
        ))}
      </Seccion>
      <Seccion titulo="Notificaciones por email">
        <FilaInterruptor
          icono="mail"
          color="text-info"
          titulo="Resumen semanal"
          descripcion="Recibe un resumen de tu actividad cada semana"
          activo={ajustes.notifEmail}
          onCambiar={(v) => cambiar('notifEmail', v)}
        />
      </Seccion>
      <Seccion titulo="Frecuencia">
        <div className="px-4 py-3 bg-surface-container rounded-xl text-body-md text-text-secondary">
          Los likes se agrupan cada 10 o cada 6 horas para evitar notificaciones excesivas. Las
          notificaciones desactivadas dejan de aparecer en tu bandeja.
        </div>
      </Seccion>
    </PantallaAjustes>
  )
}
