import { type Notificacion } from '../../../mocks/data'

/** Pantalla a la que lleva cada tipo de notificacion. */
export function destinoNotificacion(n: Notificacion): string {
  switch (n.tipo) {
    case 'event':
      return '/comunidad/eventos/1'
    case 'follow':
      return '/perfil'
    case 'live':
      return '/comunidad/en-vivo'
    case 'club':
      return '/comunidad/clubes'
    default:
      return '/comunidad'
  }
}
