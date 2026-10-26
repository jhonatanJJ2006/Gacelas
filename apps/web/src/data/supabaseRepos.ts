import { SupabaseCarrerasRepository } from '../contexts/actividad/infrastructure/carrerasSupabase'
import type { SupabaseClient } from '@supabase/supabase-js'
import type { Evento, Notificacion, Ruta } from '../mocks/data'
import { tiempoRelativo } from '../lib/logica'
import type { Racha } from '../lib/rachas'
import {
  escribir,
  LocalAjustesRepository,
  LocalEventosRepository,
  LocalRutasRepository,
  type AjustesRepository,
  type EventosRepository,
  type NotificacionesRepository,
  type ComunidadRepository,
  type PublicacionRemota,
  type ComentarioPublicacion,
  type CorredorPublico,
  type RachasRepository,
  type Repositorios,
  type RutasRepository,
  type ExplorarRepository,
  type NuevaRuta,
  type FotoRuta,
  type CorredorRuta,
} from './repositorios'
import { AJUSTES_POR_DEFECTO, type Ajustes, type Visibilidad } from './tipos'

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
import {
  SupabaseAuthRepository,
  traducirError,
  usuario,
} from '../contexts/identidad/infrastructure/authSupabase'

/* ---------------- Carreras con cache local y cola sin conexion ---------------- */

export {
  carreraDesdeFila,
  filaDesdeCarrera,
  SupabaseCarrerasRepository,
  type OperacionPendiente,
} from '../contexts/actividad/infrastructure/carrerasSupabase'

export class SupabaseRutasRepository implements RutasRepository {
  private local = new LocalRutasRepository()
  private sb: SupabaseClient
  constructor(sb: SupabaseClient) {
    this.sb = sb
  }

  async guardadas() {
    const user = await usuario(this.sb)
    if (!user) return this.local.guardadas()
    const { data, error } = await this.sb
      .from('saved_routes')
      .select('route_id')
      .eq('user_id', user.id)
    if (error) return this.local.guardadas()
    return escribir(
      'rutas-guardadas',
      ((data ?? []) as Fila[]).map((f) => f.route_id as string)
    )
  }

  async alternarGuardada(id: string) {
    const user = await usuario(this.sb)
    if (!user) return this.local.alternarGuardada(id)
    const actuales = await this.guardadas()
    const { error } = actuales.includes(id)
      ? await this.sb.from('saved_routes').delete().eq('user_id', user.id).eq('route_id', id)
      : await this.sb.from('saved_routes').insert({ user_id: user.id, route_id: id })
    if (error) throw new Error(traducirError(error.message))
    return this.guardadas()
  }
}

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

const DIFICULTADES = ['Fácil', 'Moderado', 'Difícil'] as const

export function rutaDesdeFila(f: Fila): Ruta {
  return {
    id: String(f.id),
    nombre: String(f.name ?? ''),
    distanciaKm: Number(f.distance_km ?? 0),
    dificultad: DIFICULTADES.includes(f.difficulty as Ruta['dificultad'])
      ? (f.difficulty as Ruta['dificultad'])
      : 'Moderado',
    elevacionM: Number(f.elevation_m ?? 0),
    descripcion: String(f.description ?? ''),
    autorId: f.author_id ? String(f.author_id) : 'gacelas',
    etiquetas: Array.isArray(f.tags) ? (f.tags as string[]) : [],
    puntos: Array.isArray(f.path) ? (f.path as { lat: number; lng: number }[]) : [],
    meGusta: Number(f.likes_count ?? 0),
    valoracion: f.rating_avg == null ? null : Number(f.rating_avg),
    resenas: Number(f.rating_count ?? 0),
    perfilElevacion: Array.isArray(f.elevation_profile) ? (f.elevation_profile as number[]) : [],
    origen: (f.origin as Ruta['origen']) ?? 'actividad',
    oficial: Boolean(f.is_official),
    categoria: (f.category as Ruta['categoria']) ?? undefined,
    superficie: (f.surface as Ruta['superficie']) ?? undefined,
    fuente: f.source ? `${String(f.source)} (${String(f.license ?? 'ODbL 1.0')})` : undefined,
  }
}

export class SupabaseExplorarRepository implements ExplorarRepository {
  private sb: SupabaseClient
  constructor(sb: SupabaseClient) {
    this.sb = sb
  }

  async listar() {
    const { data, error } = await this.sb
      .from('routes')
      .select('*')
      .order('created_at', { ascending: true })
      .limit(200)
    if (error) throw new Error(error.message)
    const user = await usuario(this.sb)
    const rutas = ((data ?? []) as Fila[]).map(rutaDesdeFila)
    rutas.forEach((r) => (r.propia = !!user && r.autorId === user.id))
    const autores = [...new Set(rutas.map((r) => r.autorId).filter((a) => a !== 'gacelas'))]
    if (autores.length) {
      const { data: p } = await this.sb
        .from('profiles')
        .select('id,full_name,username')
        .in('id', autores)
      const n = new Map(
        ((p ?? []) as Fila[]).map((x) => [String(x.id), String(x.full_name || x.username || '')])
      )
      rutas.forEach((r) => (r.autorNombre = n.get(r.autorId)))
    }
    return rutas
  }

  async cercanas(lat: number, lng: number) {
    const { data, error } = await this.sb.rpc('rutas_cercanas', { lat, lng, radio_km: 50 })
    if (error) return {}
    return Object.fromEntries(
      ((data ?? []) as Fila[]).map((f) => [String(f.id), Number(f.distancia_km)])
    )
  }

  async crear(r: NuevaRuta) {
    const user = await usuario(this.sb)
    if (!user) throw new Error('Inicia sesión para publicar rutas')
    const { data, error } = await this.sb
      .from('routes')
      .insert({
        name: r.nombre,
        description: r.descripcion,
        difficulty: r.dificultad,
        tags: r.etiquetas,
        path: r.puntos,
        elevation_m: Math.round(r.elevacionM),
        distance_km: 0,
        author_id: user.id,
        is_public: true,
        source_activity_id: r.actividadId ?? null,
        elevation_profile: r.perfilElevacion ?? [],
        origin: r.origen ?? 'actividad',
      })
      .select('*')
      .single()
    if (error) throw new Error(error.message)
    return { ...rutaDesdeFila(data as Fila), propia: true }
  }

  async meGusta() {
    const user = await usuario(this.sb)
    if (!user) return []
    const { data } = await this.sb.from('route_likes').select('route_id').eq('user_id', user.id)
    return ((data ?? []) as Fila[]).map((f) => String(f.route_id))
  }

  async alternarMeGusta(id: string, activo: boolean) {
    const user = await usuario(this.sb)
    if (!user) throw new Error('Inicia sesión para dar me gusta')
    const { error } = activo
      ? await this.sb.from('route_likes').insert({ user_id: user.id, route_id: id })
      : await this.sb.from('route_likes').delete().eq('user_id', user.id).eq('route_id', id)
    if (error) throw new Error(error.message)
  }

  async resenas(id: string) {
    const { data } = await this.sb
      .from('route_reviews')
      .select('*')
      .eq('route_id', id)
      .order('created_at', { ascending: false })
    const filas = (data ?? []) as Fila[]
    const ids = [...new Set(filas.map((f) => String(f.user_id)))]
    const { data: p } = ids.length
      ? await this.sb.from('profiles').select('id,full_name,username').in('id', ids)
      : { data: [] }
    const n = new Map(
      ((p ?? []) as Fila[]).map((x) => [
        String(x.id),
        String(x.full_name || x.username || 'Corredor'),
      ])
    )
    return filas.map((f) => ({
      id: String(f.id),
      usuarioId: String(f.user_id),
      nombre: n.get(String(f.user_id)) ?? 'Corredor',
      estrellas: Number(f.rating),
      comentario: String(f.comment ?? ''),
      fecha: String(f.created_at ?? ''),
    }))
  }

  async resenar(id: string, estrellas: number, comentario: string) {
    const user = await usuario(this.sb)
    if (!user) throw new Error('Inicia sesión para reseñar')
    const { error } = await this.sb.from('route_reviews').upsert(
      {
        route_id: id,
        user_id: user.id,
        rating: estrellas,
        comment: comentario.trim().slice(0, 500),
      },
      { onConflict: 'route_id,user_id' }
    )
    if (error) throw new Error(error.message)
  }

  async fotos(id: string): Promise<FotoRuta[]> {
    const { data } = await this.sb
      .from('route_photos')
      .select('id,path,user_id')
      .eq('route_id', id)
      .order('created_at', { ascending: false })
      .limit(30)
    return ((data ?? []) as Fila[]).map((f) => ({
      id: String(f.id),
      usuarioId: String(f.user_id),
      url: this.sb.storage.from('route-photos').getPublicUrl(String(f.path)).data.publicUrl,
    }))
  }

  async subirFoto(id: string, archivo: Blob) {
    const user = await usuario(this.sb)
    if (!user) throw new Error('Inicia sesión para subir fotos')
    if (archivo.size > 5 * 1024 * 1024) throw new Error('La foto pesa más de 5 MB')
    const ext =
      archivo.type === 'image/png' ? 'png' : archivo.type === 'image/webp' ? 'webp' : 'jpg'
    const path = `${user.id}/${id.replace(/[^\w-]/g, '')}/${crypto.randomUUID()}.${ext}`
    const subida = await this.sb.storage
      .from('route-photos')
      .upload(path, archivo, { contentType: archivo.type || 'image/jpeg' })
    if (subida.error) throw new Error(subida.error.message)
    const { error } = await this.sb
      .from('route_photos')
      .insert({ route_id: id, user_id: user.id, path })
    if (error) throw new Error(error.message)
  }

  async corredores(id: string): Promise<{ lista: CorredorRuta[]; total: number }> {
    const { data, error } = await this.sb.rpc('corredores_ruta', { ruta: id })
    if (error) return { lista: [], total: 0 }
    const filas = (data ?? []) as Fila[]
    return {
      total: Number(filas[0]?.total ?? 0),
      lista: filas.map((f) => ({
        usuarioId: String(f.user_id),
        nombre: String(f.nombre || f.usuario || 'Corredor'),
        usuario: String(f.usuario ?? ''),
        veces: Number(f.veces),
        ultima: String(f.ultima),
      })),
    }
  }

  async registrarCorrida(rutaId: string, actividadId: string) {
    const user = await usuario(this.sb)
    if (!user) return
    await this.sb
      .from('route_runs')
      .insert({ route_id: rutaId, user_id: user.id, activity_id: actividadId })
  }
}

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

/** Fila de la RPC feed_comunidad a publicacion del feed. */
export function publicacionDesdeFilaFeed(f: Fila): PublicacionRemota {
  const uc = f.ultimo_comentario as { autor?: string; texto?: string } | null | undefined
  return {
    id: String(f.id),
    autorId: String(f.user_id),
    autor: (f.autor as string) || 'Corredor',
    titulo: (f.title as string) || 'Carrera',
    inicioMs: Date.parse(f.start_time as string),
    distanciaKm: Number(f.distance_meters ?? 0) / 1000,
    segundos: Number(f.duration_seconds ?? 0),
    desnivelM: Number(f.elevation_gain ?? 0),
    imagenes: Array.isArray(f.images) ? (f.images as string[]) : [],
    compartidos: Number(f.reposts_count ?? 0),
    compartida: Boolean(f.reposteada),
    autorKm: Number(f.autor_km ?? 0),
    ultimoComentario: uc?.texto ? { autor: uc.autor || 'Corredor', texto: String(uc.texto) } : null,
    likes: Number(f.likes_count ?? 0),
    comentarios: Number(f.comments_count ?? 0),
    puntos: Array.isArray(f.public_points) ? (f.public_points as PublicacionRemota['puntos']) : [],
    clienteId: (f.client_id as string) ?? null,
    avatar: (f.avatar_url as string) ?? null,
    meGusta: Boolean(f.me_gusta),
    sigo: Boolean(f.sigo),
    propia: Boolean(f.propia),
  }
}

export function comentarioDesdeFila(f: Fila): ComentarioPublicacion {
  return {
    id: String(f.id),
    autorId: String(f.user_id),
    autor: (f.autor as string) || 'Corredor',
    texto: String(f.body ?? ''),
    creadoMs: Date.parse(f.created_at as string),
  }
}

export class SupabaseComunidadRepository implements ComunidadRepository {
  private sb: SupabaseClient
  constructor(sb: SupabaseClient) {
    this.sb = sb
  }
  async publicaciones(): Promise<PublicacionRemota[]> {
    const user = await usuario(this.sb)
    if (!user) return []
    const { data, error } = await this.sb.rpc('feed_comunidad', { p_limite: 40 })
    if (error || !data) return []
    const url = (ruta: string) => {
      if (/^https?:/.test(ruta)) return ruta
      const [bucket, ...resto] = ruta.split('/')
      return this.sb.storage.from(bucket).getPublicUrl(resto.join('/')).data.publicUrl
    }
    return (data as Fila[]).map((f) => {
      const p = publicacionDesdeFilaFeed(f)
      return { ...p, imagenes: (Array.isArray(f.images) ? (f.images as string[]) : []).map(url) }
    })
  }
  async alternarMeGusta(postId: string, activo: boolean) {
    const user = await usuario(this.sb)
    if (!user) return
    const { error } = activo
      ? await this.sb.from('post_likes').insert({ user_id: user.id, post_id: postId })
      : await this.sb.from('post_likes').delete().eq('user_id', user.id).eq('post_id', postId)
    if (error && error.code !== '23505') throw new Error(traducirError(error.message))
  }
  async comentarios(postId: string): Promise<ComentarioPublicacion[]> {
    const { data, error } = await this.sb.rpc('comentarios_publicacion', { p_post: postId })
    if (error || !data) return []
    return (data as Fila[]).map(comentarioDesdeFila)
  }
  async comentar(postId: string, texto: string) {
    const user = await usuario(this.sb)
    if (!user) throw new Error('Inicia sesión para comentar')
    const { error } = await this.sb
      .from('post_comments')
      .insert({ user_id: user.id, post_id: postId, body: texto.trim() })
    if (error) throw new Error(traducirError(error.message))
  }
  suscribir(alCambiar: () => void) {
    if (typeof this.sb.channel !== 'function') return () => undefined
    const canal = this.sb.channel(`feed-${Math.random().toString(36).slice(2)}`)
    for (const table of ['activities', 'post_likes', 'post_comments', 'follows', 'post_reposts'])
      canal.on('postgres_changes', { event: '*', schema: 'public', table }, () => alCambiar())
    canal.subscribe()
    return () => void this.sb.removeChannel(canal)
  }
  async buscar(consulta: string): Promise<CorredorPublico[]> {
    const user = await usuario(this.sb)
    if (!user) return []
    const q = consulta.trim().replace(/[%,()*]/g, '')
    let consultaSb = this.sb
      .from('profiles')
      .select('id,full_name,username,avatar_url,location')
      .neq('id', user.id)
      .order('full_name', { ascending: true })
      .limit(24)
    if (q) consultaSb = consultaSb.or(`full_name.ilike.*${q}*,username.ilike.*${q}*`)
    const { data, error } = await consultaSb
    if (error) return []
    return ((data ?? []) as Fila[]).map((f) => ({
      id: f.id as string,
      nombre: (f.full_name as string) || (f.username as string) || 'Corredor',
      username: (f.username as string) ?? '',
      foto: (f.avatar_url as string) ?? null,
      ubicacion: (f.location as string) ?? '',
    }))
  }
  async siguiendo() {
    const user = await usuario(this.sb)
    if (!user) return []
    const { data } = await this.sb.from('follows').select('following_id').eq('follower_id', user.id)
    return ((data ?? []) as Fila[]).map((f) => f.following_id as string)
  }
  async alternarCompartida(postId: string, activo: boolean) {
    const user = await usuario(this.sb)
    if (!user) throw new Error('Inicia sesión para compartir')
    const { error } = activo
      ? await this.sb.from('post_reposts').insert({ user_id: user.id, activity_id: postId })
      : await this.sb.from('post_reposts').delete().eq('user_id', user.id).eq('activity_id', postId)
    if (error && error.code !== '23505') throw new Error(traducirError(error.message))
  }
  async subirFotos(postId: string, archivos: File[]) {
    const user = await usuario(this.sb)
    if (!user) throw new Error('Inicia sesión para subir fotos')
    const urls: string[] = []
    for (const [i, a] of archivos.slice(0, 6).entries()) {
      const ext = (a.name.split('.').pop() || 'jpg').toLowerCase()
      const ruta = `${user.id}/${postId}/${Date.now()}-${i}.${ext}`
      const sub = await this.sb.storage
        .from('post-images')
        .upload(ruta, a, { contentType: a.type || 'image/jpeg' })
      if (sub.error) throw new Error(traducirError(sub.error.message))
      const { error } = await this.sb
        .from('post_images')
        .insert({ activity_id: postId, user_id: user.id, path: `post-images/${ruta}`, position: i })
      if (error) throw new Error(traducirError(error.message))
      urls.push(this.sb.storage.from('post-images').getPublicUrl(ruta).data.publicUrl)
    }
    return urls
  }
  async seguidores() {
    const user = await usuario(this.sb)
    if (!user) return 0
    const { count } = await this.sb
      .from('follows')
      .select('follower_id', { count: 'exact', head: true })
      .eq('following_id', user.id)
    return count ?? 0
  }
  async alternarSeguir(id: string) {
    const user = await usuario(this.sb)
    if (!user) return []
    const actuales = await this.siguiendo()
    if (actuales.includes(id)) {
      await this.sb.from('follows').delete().eq('follower_id', user.id).eq('following_id', id)
    } else {
      const { error } = await this.sb
        .from('follows')
        .insert({ follower_id: user.id, following_id: id })
      if (error) throw new Error(traducirError(error.message))
    }
    return this.siguiendo()
  }
}

export function rachaDesdeFila(f: Fila): Racha {
  const fecha = f.last_active_date as string | null
  let ultimoDiaMs: number | null = null
  if (fecha) {
    const [a, m, d] = fecha.split('-').map(Number)
    ultimoDiaMs = new Date(a, m - 1, d).getTime()
  }
  return {
    diaria: Number(f.current_daily ?? 0),
    mejorDiaria: Number(f.best_daily ?? 0),
    ultimoDiaMs,
    semanal: Number(f.current_weekly ?? 0),
    mejorSemanal: Number(f.best_weekly ?? 0),
    metaSemanal: Number(f.weekly_goal_runs ?? 3),
    carrerasSemana: Number(f.runs_this_week ?? 0),
  }
}

export class SupabaseRachasRepository implements RachasRepository {
  private sb: SupabaseClient
  constructor(sb: SupabaseClient) {
    this.sb = sb
  }
  async leer() {
    const user = await usuario(this.sb)
    if (!user) return null
    const { data, error } = await this.sb.rpc('mi_racha')
    if (error || !data) return null
    const fila = (Array.isArray(data) ? data[0] : data) as Fila | undefined
    return fila ? rachaDesdeFila(fila) : null
  }
}

/** Oculta borradores (published = false) salvo al creador; refuerza la RLS de 021. */
export function eventoVisible(f: Fila, uid?: string | null): boolean {
  return f.published !== false || (uid != null && f.created_by === uid)
}
