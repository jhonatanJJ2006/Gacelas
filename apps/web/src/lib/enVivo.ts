import type { SupabaseClient } from '@supabase/supabase-js'
import { useSyncExternalStore } from 'react'
import type { PuntoGps } from './geo'
import { obtenerSupabase } from './supabase'

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

type Fila = Record<string, unknown>
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

async function nombres(sb: SupabaseClient, ids: string[]) {
  if (!ids.length) return new Map<string, string>()
  const { data } = await sb.from('profiles').select('id,full_name,username').in('id', ids)
  return new Map(
    ((data ?? []) as Fila[]).map((p) => [
      String(p.id),
      String(p.full_name || p.username || 'Corredor'),
    ])
  )
}

/** Sesiones en vivo que puedo ver (RLS), sin incluir la mia. */
export async function listarEnVivo(): Promise<SesionEnVivo[]> {
  const sb = obtenerSupabase()
  if (!sb) return []
  const { data: u } = await sb.auth.getUser()
  const { data, error } = await sb
    .from('live_sessions')
    .select('*')
    .is('ended_at', null)
    .order('started_at', { ascending: false })
    .limit(50)
  if (error || !data) return []
  const filas = (data as Fila[]).filter((f) => f.user_id !== u.user?.id)
  const n = await nombres(sb, [...new Set(filas.map((f) => String(f.user_id)))])
  return filas
    .map((f) => sesionDesdeFila(f, n.get(String(f.user_id))))
    .filter((s) => sesionActiva(s))
}

export async function leerSesion(id: string): Promise<SesionEnVivo | null> {
  const sb = obtenerSupabase()
  if (!sb) return null
  const { data } = await sb.from('live_sessions').select('*').eq('id', id).maybeSingle()
  if (!data) return null
  const n = await nombres(sb, [String((data as Fila).user_id)])
  return sesionDesdeFila(data as Fila, n.get(String((data as Fila).user_id)))
}

/** Escucha los cambios de una sesion por Supabase Realtime. Devuelve la funcion para salir. */
export function suscribirSesion(id: string, alCambiar: (f: Fila) => void): () => void {
  const sb = obtenerSupabase()
  if (!sb) return () => {}
  const canal = sb
    .channel(`live:session:${id}`)
    .on(
      'postgres_changes',
      { event: 'UPDATE', schema: 'public', table: 'live_sessions', filter: `id=eq.${id}` },
      (p) => alCambiar(p.new as Fila)
    )
    .subscribe()
  return () => void sb.removeChannel(canal)
}

/** Escucha cualquier cambio en sesiones visibles (para refrescar la lista). */
export function suscribirLista(alCambiar: () => void): () => void {
  const sb = obtenerSupabase()
  if (!sb) return () => {}
  const canal = sb
    .channel('live:lista')
    .on('postgres_changes', { event: '*', schema: 'public', table: 'live_sessions' }, () =>
      alCambiar()
    )
    .subscribe()
  return () => void sb.removeChannel(canal)
}

// --- Compartir mi carrera en vivo -------------------------------------------------------

interface EstadoCompartir {
  sesionId: string | null
  error: string | null
  /** Ultimo envio aceptado como visible (false = oculto cerca de casa). */
  visible: boolean | null
}
let estado: EstadoCompartir = { sesionId: null, error: null, visible: null }
const oyentes = new Set<() => void>()
const emitir = (e: Partial<EstadoCompartir>) => {
  estado = { ...estado, ...e }
  oyentes.forEach((o) => o())
}
let ultimoEnvio = 0

export const useCompartirEnVivo = () =>
  useSyncExternalStore(
    (o) => {
      oyentes.add(o)
      return () => oyentes.delete(o)
    },
    () => estado
  )

export const puedeCompartir = () => obtenerSupabase() !== null

export async function empezarACompartir(): Promise<boolean> {
  const sb = obtenerSupabase()
  if (!sb) return false
  const { data, error } = await sb.rpc('live_iniciar', {})
  if (error || !data) {
    emitir({ error: 'No se pudo iniciar el seguimiento en vivo' })
    return false
  }
  ultimoEnvio = 0
  emitir({ sesionId: String(data), error: null, visible: null })
  return true
}

/** Envia la posicion como mucho cada 5 s. */
export async function enviarPosicion(
  punto: PuntoGps | undefined,
  distanciaKm: number,
  segundos: number,
  ahora = Date.now()
) {
  const sb = obtenerSupabase()
  const id = estado.sesionId
  if (!sb || !id || !punto || ahora - ultimoEnvio < INTERVALO_ENVIO_MS) return
  ultimoEnvio = ahora
  const { data, error } = await sb.rpc('live_enviar', {
    sesion: id,
    lat: punto.lat,
    lng: punto.lng,
    distancia_m: Math.round(distanciaKm * 1000),
    segundos: Math.round(segundos),
  })
  if (!error) emitir({ visible: Boolean(data), error: null })
}

export async function dejarDeCompartir() {
  const sb = obtenerSupabase()
  const id = estado.sesionId
  emitir({ sesionId: null, visible: null })
  if (sb && id) await sb.rpc('live_terminar', { sesion: id })
}
