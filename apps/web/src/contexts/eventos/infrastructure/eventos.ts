import { obtenerSupabase } from '../../../shared/supabase/client'
import { type CorredorEvento, type EstadoEvento } from '../domain/eventos'
export * from '../domain/eventos'

export async function estadoEvento(id: string): Promise<EstadoEvento | null> {
  const sb = obtenerSupabase()
  if (!sb) return null
  const { data, error } = await sb.rpc('evento_estado', { evento: id })
  const f = (Array.isArray(data) ? data[0] : data) as Record<string, unknown> | undefined
  if (error || !f) return null
  return {
    inscritos: Number(f.inscritos ?? 0),
    enEspera: Number(f.en_espera ?? 0),
    cupo: f.cupo == null ? null : Number(f.cupo),
    miEstado: (f.mi_estado as EstadoEvento['miEstado']) ?? null,
    miPosicion: f.mi_estado === 'waitlist' ? Number(f.mi_posicion) : null,
  }
}

// --- Calendario --------------------------------------------------------------------------

// --- En vivo del evento -----------------------------------------------------------------

export async function corredoresEvento(id: string): Promise<CorredorEvento[]> {
  const sb = obtenerSupabase()
  if (!sb) return []
  const { data, error } = await sb.rpc('evento_en_vivo', { evento: id })
  if (error) return []
  return ((data ?? []) as Record<string, unknown>[]).map((f) => ({
    sesionId: String(f.session_id),
    usuarioId: String(f.user_id),
    nombre: String(f.nombre ?? 'Corredor'),
    distanciaM: Number(f.distance_m ?? 0),
    segundos: Number(f.elapsed_s ?? 0),
    lat: f.last_lat == null ? null : Number(f.last_lat),
    lng: f.last_lng == null ? null : Number(f.last_lng),
    oculto: Boolean(f.hidden),
    terminado: Boolean(f.ended),
    puntos: Array.isArray(f.points) ? (f.points as { lat: number; lng: number }[]) : [],
  }))
}

export function suscribirEvento(id: string, alCambiar: () => void): () => void {
  const sb = obtenerSupabase()
  if (!sb) return () => undefined
  const canal = sb
    .channel(`live:event:${id}`)
    .on(
      'postgres_changes',
      { event: '*', schema: 'public', table: 'live_sessions', filter: `event_id=eq.${id}` },
      alCambiar
    )
    .subscribe()
  return () => void sb.removeChannel(canal)
}
