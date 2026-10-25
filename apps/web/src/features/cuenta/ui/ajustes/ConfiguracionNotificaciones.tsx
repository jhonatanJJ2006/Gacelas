import type { Ajustes } from '../../../../data/tipos'
import { useApp } from '../../../../store/app'
import { FilaC2, GrupoC2, PantallaAjustes } from './Comunes'

type ClaveNotif =
  | 'notifLikes'
  | 'notifComentarios'
  | 'notifSeguidores'
  | 'notifEnVivo'
  | 'notifEventos'
  | 'notifEmail'

const PUSH: { clave: ClaveNotif; titulo: string; descripcion?: string }[] = [
  { clave: 'notifLikes', titulo: 'Me gusta' },
  { clave: 'notifComentarios', titulo: 'Comentarios' },
  { clave: 'notifSeguidores', titulo: 'Nuevos seguidores' },
  {
    clave: 'notifEnVivo',
    titulo: 'Amigos en vivo',
    descripcion: 'Cuando alguien te comparte su carrera',
  },
  { clave: 'notifEventos', titulo: 'Eventos y clubes' },
]

/** Ajustes de notificaciones, rediseño C2 segun figma-proto. */
export function ConfiguracionNotificaciones() {
  const ajustes = useApp((s) => s.ajustes)
  const actualizar = useApp((s) => s.actualizarAjustes)
  const cambiar = (clave: ClaveNotif, v: boolean) =>
    void actualizar({ [clave]: v } as Partial<Ajustes>)

  return (
    <PantallaAjustes titulo="Notificaciones" volverA="/ajustes">
      <GrupoC2 titulo="Push">
        {PUSH.map((n) => (
          <FilaC2
            key={n.clave}
            titulo={n.titulo}
            descripcion={n.descripcion}
            activo={ajustes[n.clave]}
            onCambiar={(v) => cambiar(n.clave, v)}
          />
        ))}
      </GrupoC2>
      <GrupoC2 titulo="Correo">
        <FilaC2
          titulo="Resumen semanal"
          descripcion="Tu actividad de la semana, cada lunes"
          activo={ajustes.notifEmail}
          onCambiar={(v) => cambiar('notifEmail', v)}
        />
      </GrupoC2>
      <p className="text-[13px] text-c2-muted">
        Los me gusta se agrupan para evitar avisos excesivos. Lo que desactives deja de aparecer en
        tu bandeja.
      </p>
    </PantallaAjustes>
  )
}
