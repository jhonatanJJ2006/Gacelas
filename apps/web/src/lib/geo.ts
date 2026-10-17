/** Utilidades de distancia, formato y unidades. Misma logica que la app Android (tracking/Geo.kt). */

export interface PuntoGps {
  lat: number
  lng: number
  tiempoMs: number
  precisionM: number
  altitudM: number | null
}

const RADIO_TIERRA_M = 6_371_000
export const KM_POR_MILLA = 1.609344

const rad = (g: number) => (g * Math.PI) / 180

/** Distancia en metros entre dos coordenadas con la formula de Haversine. */
export function haversineM(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const dLat = rad(lat2 - lat1)
  const dLng = rad(lng2 - lng1)
  const a =
    Math.sin(dLat / 2) ** 2 + Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(dLng / 2) ** 2
  return 2 * RADIO_TIERRA_M * Math.asin(Math.min(1, Math.sqrt(a)))
}

export const distanciaPuntos = (a: PuntoGps, b: PuntoGps) => haversineM(a.lat, a.lng, b.lat, b.lng)

const dos = (n: number) => n.toString().padStart(2, '0')

/** Ritmo en segundos por km como m:ss, o --:-- si no hay datos. */
export function formatearRitmo(segKm: number | null | undefined): string {
  if (segKm == null || segKm <= 0 || segKm > 59 * 60) return '--:--'
  const s = Math.round(segKm)
  return `${Math.floor(s / 60)}:${dos(s % 60)}`
}

export const formatearHms = (seg: number) =>
  `${dos(Math.floor(seg / 3600))}:${dos(Math.floor((seg % 3600) / 60))}:${dos(seg % 60)}`

export const formatearMs = (seg: number) => `${Math.floor(seg / 60)}:${dos(seg % 60)}`

export const Unidades = {
  distancia: (km: number, metricas: boolean) => (metricas ? km : km / KM_POR_MILLA),
  etiquetaDistancia: (metricas: boolean) => (metricas ? 'km' : 'mi'),
  ritmo: (segKm: number | null, metricas: boolean) =>
    segKm == null ? null : metricas ? segKm : Math.floor(segKm * KM_POR_MILLA),
  etiquetaRitmo: (metricas: boolean) => (metricas ? '/km' : '/mi'),
  texto: (km: number, metricas: boolean, decimales = 2) =>
    Unidades.distancia(km, metricas).toFixed(decimales),
}
