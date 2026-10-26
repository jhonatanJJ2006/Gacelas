import type { PuntoGps } from '../../../shared/lib/geo'

/** Sesion de seguimiento en vivo tal como la ve un espectador. */
export interface SesionEnVivo {
  id: string
  userId: string
  nombre: string
  audiencia: 'followers' | 'public'
  inicioMs: number
  finMs: number | null
  /** true mientras el corredor esta dentro del radio de casa (posicion oculta). */
  oculta: boolean
  ultimaMs: number | null
  posicion: { lat: number; lng: number } | null
  puntos: { lat: number; lng: number }[]
  distanciaKm: number
  segundos: number
}

export type Fila = Record<string, unknown>
const ms = (v: unknown) => (typeof v === 'string' ? new Date(v).getTime() : null)

export function sesionDesdeFila(f: Fila, nombre = 'Corredor'): SesionEnVivo {
  const lat = f.last_lat as number | null
  const lng = f.last_lng as number | null
  return {
    id: String(f.id),
    userId: String(f.user_id),
    nombre,
    audiencia: f.audience === 'public' ? 'public' : 'followers',
    inicioMs: ms(f.started_at) ?? 0,
    finMs: ms(f.ended_at),
    oculta: Boolean(f.hidden),
    ultimaMs: ms(f.last_at),
    posicion: !f.hidden && lat != null && lng != null ? { lat, lng } : null,
    puntos: Array.isArray(f.points) ? (f.points as { lat: number; lng: number }[]) : [],
    distanciaKm: Number(f.distance_m ?? 0) / 1000,
    segundos: Number(f.elapsed_s ?? 0),
  }
}

/** Una sesion sin posiciones durante 15 min se considera terminada (igual que el servidor). */
export const sesionActiva = (s: SesionEnVivo, ahora = Date.now()) =>
  s.finMs == null && ahora - (s.ultimaMs ?? s.inicioMs) < 15 * 60_000

/** Puntos para dibujar el trazado con los componentes de mapa. */
export const aPuntosGps = (p: { lat: number; lng: number }[]): PuntoGps[] =>
  p.map((x, i) => ({ lat: x.lat, lng: x.lng, tiempoMs: i, precisionM: 0, altitudM: null }))

export const INTERVALO_ENVIO_MS = 5000
