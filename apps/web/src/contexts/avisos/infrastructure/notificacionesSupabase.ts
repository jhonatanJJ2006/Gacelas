import type { SupabaseClient } from '@supabase/supabase-js'
import type { Notificacion } from '../../../mocks/data'
import { tiempoRelativo } from '../../../shared/lib/tiempo'
import { usuario } from '../../../shared/supabase/sesion'
import { type NotificacionesRepository } from '../domain/puertos'

/* Notificaciones de la cuenta sobre Supabase (contexto Avisos). */

type Fila = Record<string, unknown>

const TIPOS_NOTIF = [
  'like',
  'comment',
  'follow',
  'event',
  'live',
  'streak',
  'club',
  'reporte',
] as const

export function notificacionDesdeFila(f: Fila, ahora = Date.now()): Notificacion {
  const tipo = TIPOS_NOTIF.includes(f.type as (typeof TIPOS_NOTIF)[number])
    ? (f.type as Notificacion['tipo'])
    : 'event'
  return {
    id: f.id as string,
    tipo,
    usuario: (f.actor_name as string) ?? 'Runcity',
    mensaje: (f.message as string) ?? '',
    tiempo: tiempoRelativo(Date.parse(f.created_at as string), ahora),
    leida: Boolean(f.read),
  }
}

export class SupabaseNotificacionesRepository implements NotificacionesRepository {
  private sb: SupabaseClient
  constructor(sb: SupabaseClient) {
    this.sb = sb
  }
  async listar() {
    const user = await usuario(this.sb)
    if (!user) return []
    const { data } = await this.sb
      .from('notifications')
      .select('id,type,actor_name,message,read,created_at')
      .eq('user_id', user.id)
      .order('created_at', { ascending: false })
      .limit(50)
    return ((data as Fila[] | null) ?? []).map((f) => notificacionDesdeFila(f))
  }
  async marcarLeidas(ids: string[]) {
    if (!ids.length) return
    const user = await usuario(this.sb)
    if (!user) return
    await this.sb.from('notifications').update({ read: true }).in('id', ids).eq('user_id', user.id)
  }
}
