import { Navigate } from 'react-router-dom'
import { esAppInstalada } from '../../shared/lib/appInstalada'
import { Landing } from './Landing'

/**
 * Pagina de "/" para quien no tiene sesion. Con sesion, SoloInvitados ya manda a /inicio.
 * La app instalada (PWA) abre en "/" (manifest start_url): va directo a la pantalla de acceso,
 * porque la presentacion es para quien llega desde el navegador.
 */
export function RaizPublica() {
  if (esAppInstalada()) return <Navigate to="/bienvenida" replace />
  return <Landing />
}
