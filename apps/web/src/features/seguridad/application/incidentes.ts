import {
  SEGURIDAD_EJEMPLO,
  incidentesEjemplo,
  type IncidentePublico,
  type SeguridadZona,
} from '../domain/incidentes'
import {
  confirmarIncidenteRemoto,
  direccionDeRemoto,
  incidentesRecientesRemoto,
  reportarIncidenteRemoto,
  seguridadZonaRemoto,
  type NuevoIncidente,
} from '../data/incidentesRemotos'

/* Casos de uso del contexto Seguridad de rutas. La UI solo usa este modulo y el dominio. */

export type { NuevoIncidente }

export const esVistaPrevia = () =>
  import.meta.env.DEV &&
  typeof window !== 'undefined' &&
  new URLSearchParams(window.location.search).has('vista-previa')

export const consultarSeguridadDeRuta = (rutaId: string): Promise<SeguridadZona | null> =>
  esVistaPrevia() ? Promise.resolve(SEGURIDAD_EJEMPLO) : seguridadZonaRemoto(rutaId)

export const listarIncidentesRecientes = (): Promise<IncidentePublico[]> =>
  esVistaPrevia() ? Promise.resolve(incidentesEjemplo()) : incidentesRecientesRemoto()

export const obtenerDireccion = (lat: number, lng: number): Promise<string> =>
  esVistaPrevia() ? Promise.resolve('Av. Orillas del Zamora, Loja') : direccionDeRemoto(lat, lng)

export const reportarIncidente = (n: NuevoIncidente): Promise<void> =>
  esVistaPrevia() ? Promise.resolve() : reportarIncidenteRemoto(n)

export const confirmarIncidente = (id: string): Promise<void> =>
  esVistaPrevia() ? Promise.resolve() : confirmarIncidenteRemoto(id)
