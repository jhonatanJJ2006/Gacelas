import type { SupabaseClient } from '@supabase/supabase-js'
import { SupabaseCarrerasRepository } from '../contexts/actividad/infrastructure/carrerasSupabase'
import { SupabaseRachasRepository } from '../contexts/actividad/infrastructure/rachasSupabase'
import { SupabaseComunidadRepository } from '../contexts/comunidad/infrastructure/comunidadSupabase'
import { SupabaseEventosRepository } from '../contexts/eventos/infrastructure/eventosSupabase'
import { SupabaseAuthRepository, usuario } from '../contexts/identidad/infrastructure/authSupabase'
import {
  SupabaseExplorarRepository,
  SupabaseRutasRepository,
} from '../contexts/rutas/infrastructure/rutasSupabase'
import { tiempoRelativo } from '../lib/logica'
import type { Notificacion } from '../mocks/data'
import { AJUSTES_POR_DEFECTO, type Ajustes, type Visibilidad } from '../shared/kernel/tipos'
import {
  escribir,
  LocalAjustesRepository,
  type AjustesRepository,
  type NotificacionesRepository,
  type Repositorios,
} from './repositorios'

/*
 * Repositorios sobre Supabase. Mantienen las mismas interfaces que los locales; cuando no hay
 * sesion o la red falla se usa la copia local (cache), y las carreras se encolan para subirlas.
 */

type Fila = Record<string, unknown>

export {
  perfilDesdeFila,
  SupabaseAuthRepository,
  traducirError,
} from '../contexts/identidad/infrastructure/authSupabase'

/* ---------------- Carreras con cache local y cola sin conexion ---------------- */

export {
  carreraDesdeFila,
  filaDesdeCarrera,
  SupabaseCarrerasRepository,
  type OperacionPendiente,
} from '../contexts/actividad/infrastructure/carrerasSupabase'
export {
  rachaDesdeFila,
  SupabaseRachasRepository,
} from '../contexts/actividad/infrastructure/rachasSupabase'
export {
  comentarioDesdeFila,
  publicacionDesdeFilaFeed,
  SupabaseComunidadRepository,
} from '../contexts/comunidad/infrastructure/comunidadSupabase'
export {
  eventoDesdeFila,
  eventoVisible,
  SupabaseEventosRepository,
} from '../contexts/eventos/infrastructure/eventosSupabase'
export {
  rutaDesdeFila,
  SupabaseExplorarRepository,
  SupabaseRutasRepository,
} from '../contexts/rutas/infrastructure/rutasSupabase'

/* ---------------- Ajustes, likes y comentarios ---------------- */

const VIS_A_DB: Record<Visibilidad, string> = {
  publico: 'public',
  seguidores: 'followers',
  privado: 'private',
}
const DB_A_VIS: Record<string, Visibilidad> = {
  public: 'publico',
  followers: 'seguidores',
  private: 'privado',
}

export class SupabaseAjustesRepository implements AjustesRepository {
  private local = new LocalAjustesRepository()
  private sb: SupabaseClient
  constructor(sb: SupabaseClient) {
    this.sb = sb
  }

  async leer(): Promise<Ajustes> {
    const user = await usuario(this.sb)
    if (!user) return this.local.leer()
    const [s, likes, comentarios] = await Promise.all([
      this.sb.from('user_settings').select('*').eq('user_id', user.id).maybeSingle(),
      this.sb.from('post_likes').select('post_id').eq('user_id', user.id),
      this.sb
        .from('post_comments')
        .select('post_id,body,created_at')
        .eq('user_id', user.id)
        .order('created_at', { ascending: true }),
    ])
    if (s.error || likes.error || comentarios.error) return this.local.leer()
    const fila = (s.data ?? {}) as Fila
    const porPost: Record<string, string[]> = {}
    for (const c of (comentarios.data ?? []) as Fila[])
      (porPost[c.post_id as string] ??= []).push(c.body as string)
    const ajustes: Ajustes = {
      ...AJUSTES_POR_DEFECTO,
      ...((fila.prefs as Partial<Ajustes>) ?? {}),
      ...(fila.visibility ? { visibilidad: DB_A_VIS[fila.visibility as string] } : {}),
      ...(fila.hide_home != null ? { ocultarHogar: Boolean(fila.hide_home) } : {}),
      ...(fila.home_radius_m != null ? { radioHogarM: Number(fila.home_radius_m) } : {}),
      ...(fila.weekly_goal_runs != null ? { metaSemanal: Number(fila.weekly_goal_runs) } : {}),
      ...(fila.precise_location != null
        ? { ubicacionPrecisa: Boolean(fila.precise_location) }
        : {}),
      likes: ((likes.data ?? []) as Fila[]).map((l) => l.post_id as string),
      comentarios: porPost,
    }
    escribir('ajustes', ajustes)
    return ajustes
  }

  async actualizar(cambios: Partial<Ajustes>) {
    const user = await usuario(this.sb)
    const antes = await this.local.leer()
    const nuevos = await this.local.actualizar(cambios)
    if (!user) return nuevos
    const id = user.id
    if (cambios.likes) {
      const quitar = antes.likes.filter((l) => !nuevos.likes.includes(l))
      const poner = nuevos.likes.filter((l) => !antes.likes.includes(l))
      if (poner.length)
        await this.sb.from('post_likes').insert(poner.map((post_id) => ({ user_id: id, post_id })))
      for (const post_id of quitar)
        await this.sb.from('post_likes').delete().eq('user_id', id).eq('post_id', post_id)
    }
    if (cambios.comentarios) {
      const filas = Object.entries(nuevos.comentarios).flatMap(([post_id, lista]) =>
        lista
          .slice(antes.comentarios[post_id]?.length ?? 0)
          .map((body) => ({ user_id: id, post_id, body }))
      )
      if (filas.length) await this.sb.from('post_comments').insert(filas)
    }
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { likes, comentarios, ...prefs } = nuevos
    await this.sb.from('user_settings').upsert(
      {
        user_id: id,
        visibility: VIS_A_DB[nuevos.visibilidad],
        hide_home: nuevos.ocultarHogar,
        home_radius_m: nuevos.radioHogarM,
        precise_location: nuevos.ubicacionPrecisa,
        weekly_goal_runs: nuevos.metaSemanal,
        prefs,
      },
      { onConflict: 'user_id' }
    )
    return nuevos
  }
}

/* ---------------- Eventos ---------------- */

export const repositoriosSupabase = (sb: SupabaseClient): Repositorios => ({
  auth: new SupabaseAuthRepository(sb),
  carreras: new SupabaseCarrerasRepository(sb),
  rutas: new SupabaseRutasRepository(sb),
  ajustes: new SupabaseAjustesRepository(sb),
  eventos: new SupabaseEventosRepository(sb),
  notificaciones: new SupabaseNotificacionesRepository(sb),
  comunidad: new SupabaseComunidadRepository(sb),
  rachas: new SupabaseRachasRepository(sb),
  explorar: new SupabaseExplorarRepository(sb),
})

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
    usuario: (f.actor_name as string) ?? 'Gacelas Loja',
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
