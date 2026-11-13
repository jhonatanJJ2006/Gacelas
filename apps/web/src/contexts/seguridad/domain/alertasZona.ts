import { nombreTipo, type IncidentePublico } from './incidentes'

/**
 * Alertas de zona durante la carrera (ticket 86e3uf09b, N-11 y VI-08): al acercarse a menos de
 * RADIO_ALERTA_M de un reporte reciente se avisa una sola vez por reporte. Se apagan en Ajustes,
 * Privacidad y ubicacion (prefs.alertasZona, igual que en Android).
 */
export const RADIO_ALERTA_M = 200
export const VIGENCIA_ALERTA_MS = 7 * 24 * 60 * 60 * 1000
export const REFRESCO_ALERTAS_MS = 5 * 60 * 1000

export function distanciaM(
  a: { lat: number; lng: number },
  b: { lat: number; lng: number }
): number {
  const r = 6_371_000
  const rad = (x: number) => (x * Math.PI) / 180
  const dLat = rad(b.lat - a.lat)
  const dLng = rad(b.lng - a.lng)
  const h =
    Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.lat)) * Math.cos(rad(b.lat)) * Math.sin(dLng / 2) ** 2
  return 2 * r * Math.asin(Math.sqrt(Math.min(1, Math.max(0, h))))
}

/** Reportes vigentes a menos del radio que aun no se avisaron, del mas cercano al mas lejano. */
export function alertasNuevas(
  pos: { lat: number; lng: number },
  reportes: IncidentePublico[],
  avisados: ReadonlySet<string>,
  ahora = Date.now()
): { incidente: IncidentePublico; metros: number }[] {
  return reportes
    .filter(
      (i) => !avisados.has(i.id) && (!i.ocurridoMs || ahora - i.ocurridoMs <= VIGENCIA_ALERTA_MS)
    )
    .map((incidente) => ({ incidente, metros: Math.round(distanciaM(pos, incidente)) }))
    .filter((x) => x.metros <= RADIO_ALERTA_M)
    .sort((a, b) => a.metros - b.metros)
}

export const mensajeAlerta = (i: IncidentePublico, metros: number) =>
  `Atención: reporte de ${nombreTipo(i.tipo).toLowerCase()} a ${metros} m${i.estado === 'verificado' ? ' (verificado)' : ''}`
