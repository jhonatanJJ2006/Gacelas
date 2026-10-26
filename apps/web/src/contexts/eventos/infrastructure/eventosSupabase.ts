import type { SupabaseClient } from '@supabase/supabase-js'
import {
  escribir,
  LocalEventosRepository,
  type EventosRepository,
} from '../../../data/repositorios'
import type { Evento } from '../../../mocks/data'
import { traducirError } from '../../../shared/supabase/errores'
import { usuario } from '../../../shared/supabase/sesion'

/* Repositorio de eventos sobre Supabase (contexto Eventos y clubes). */

type Fila = Record<string, unknown>

export function eventoDesdeFila(f: Fila, uid?: string): Evento {
  const inicio = f.starts_at ? Date.parse(f.starts_at as string) : NaN
  return {
    id: f.id as string,
    titulo: f.title as string,
    descripcion: (f.description as string) ?? '',
    fecha: f.date_label as string,
    hora: f.time_label as string,
    ubicacion: f.location as string,
    participantes: Number(f.base_participants ?? 0),
    distanciaKm: Number(f.distance_km ?? 0),
    pasado: Number.isFinite(inicio) ? inicio < Date.now() : false,
    creadoPorUsuario: uid ? f.created_by === uid : true,
    inicioMs: Number.isFinite(inicio) ? inicio : undefined,
    cupo: f.max_participants == null ? null : Number(f.max_participants),
    tipo: (f.kind as Evento['tipo']) ?? 'grupal',
    clubId: (f.club_id as string) ?? null,
    rutaId: (f.route_id as string) ?? null,
  }
}

export class SupabaseEventosRepository implements EventosRepository {
  private local = new LocalEventosRepository()
  private sb: SupabaseClient
  constructor(sb: SupabaseClient) {
    this.sb = sb
  }

  async creados() {
    const user = await usuario(this.sb)
    if (!user) return this.local.creados()
    // Todos los eventos de la base (los de Loja sembrados y los creados por usuarios).
    const [{ data, error }, conteo] = await Promise.all([
      this.sb.from('events').select('*').order('starts_at', { ascending: true }).limit(200),
      Promise.resolve()
        .then(() => this.sb.rpc('participantes_eventos'))
        .catch(() => ({ data: null })),
    ])
    if (error) return this.local.creados()
    const otros = new Map(
      ((conteo.data ?? []) as Fila[]).map((c) => [String(c.event_id), Number(c.otros ?? 0)])
    )
    return escribir(
      'eventos-creados',
      ((data ?? []) as Fila[])
        .filter((f) => eventoVisible(f, user.id))
        .map((f) => {
          const e = eventoDesdeFila(f, user.id)
          return otros.has(e.id) ? { ...e, participantes: otros.get(e.id)! } : e
        })
    )
  }

  async inscritos() {
    const user = await usuario(this.sb)
    if (!user) return this.local.inscritos()
    const { data, error } = await this.sb
      .from('event_rsvps')
      .select('event_id')
      .eq('user_id', user.id)
    if (error) return this.local.inscritos()
    return escribir(
      'eventos-inscritos',
      ((data ?? []) as Fila[]).map((f) => f.event_id as string)
    )
  }

  async crear(e: Evento) {
    const user = await usuario(this.sb)
    if (!user) return this.local.crear(e)
    const { error } = await this.sb.from('events').insert({
      id: e.id,
      title: e.titulo,
      description: e.descripcion,
      date_label: e.fecha,
      time_label: e.hora,
      location: e.ubicacion,
      distance_km: e.distanciaKm,
      base_participants: e.participantes,
      created_by: user.id,
      starts_at: e.inicioMs ? new Date(e.inicioMs).toISOString() : null,
      max_participants: e.cupo ?? null,
      kind: e.tipo ?? 'grupal',
      club_id: e.clubId ?? null,
      route_id: e.rutaId ?? null,
    })
    if (error) throw new Error(traducirError(error.message))
    await this.sb.from('event_rsvps').insert({ event_id: e.id, user_id: user.id })
    return this.creados()
  }

  async alternarInscripcion(id: string) {
    const user = await usuario(this.sb)
    if (!user) return this.local.alternarInscripcion(id)
    const actuales = await this.inscritos()
    const { error } = actuales.includes(id)
      ? await this.sb.from('event_rsvps').delete().eq('user_id', user.id).eq('event_id', id)
      : await this.sb.from('event_rsvps').insert({ event_id: id, user_id: user.id })
    if (error) throw new Error(traducirError(error.message))
    return this.inscritos()
  }
}

/** Oculta borradores (published = false) salvo al creador; refuerza la RLS de 021. */
export function eventoVisible(f: Fila, uid?: string | null): boolean {
  return f.published !== false || (uid != null && f.created_by === uid)
}
