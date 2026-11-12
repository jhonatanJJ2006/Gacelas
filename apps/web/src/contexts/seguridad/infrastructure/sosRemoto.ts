import { obtenerSupabase } from '../../../shared/supabase/client'

/* Adaptador de Supabase del SOS (migracion 039_sos.sql). */

export interface AlertaPublica {
  nombre: string
  activa: boolean
  inicio: string
  ultima: string | null
  pos: { lat: number; lng: number } | null
  puntos: { lat: number; lng: number }[]
}

export async function iniciarSos(pos: { lat: number; lng: number } | null): Promise<string | null> {
  const sb = obtenerSupabase()
  if (!sb) return null
  const { data, error } = await sb.rpc('sos_iniciar', {
    lat: pos?.lat ?? null,
    lng: pos?.lng ?? null,
  })
  if (error) throw new Error('No se pudo activar el SOS en el servidor')
  return (data as string) ?? null
}

export async function enviarPosicionSos(id: string, pos: { lat: number; lng: number }) {
  const sb = obtenerSupabase()
  if (!sb) return false
  const { data } = await sb.rpc('sos_enviar', { alerta: id, lat: pos.lat, lng: pos.lng })
  return data === true
}

export async function terminarSos(id: string) {
  const sb = obtenerSupabase()
  if (!sb) return
  await sb.rpc('sos_terminar', { alerta: id })
}

export async function verSos(id: string): Promise<AlertaPublica | null> {
  const sb = obtenerSupabase()
  if (!sb) return null
  const { data, error } = await sb.rpc('sos_ver', { alerta: id })
  const f = (Array.isArray(data) ? data[0] : data) as
    | {
        nombre: string
        activa: boolean
        started_at: string
        last_at: string | null
        lat: number | null
        lng: number | null
        points: { lat: number; lng: number }[] | null
      }
    | undefined
  if (error || !f) return null
  return {
    nombre: f.nombre,
    activa: f.activa,
    inicio: f.started_at,
    ultima: f.last_at,
    pos: f.lat != null && f.lng != null ? { lat: f.lat, lng: f.lng } : null,
    puntos: f.points ?? [],
  }
}
