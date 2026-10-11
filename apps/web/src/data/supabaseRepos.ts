import type { SupabaseClient, User } from '@supabase/supabase-js'
import type { Evento, Notificacion } from '../mocks/data'
import { tiempoRelativo } from '../lib/logica'
import { ValidacionAuth } from '../lib/validacion'
import { configSupabase } from '../lib/supabase'
import {
  escribir,
  leer,
  LocalAjustesRepository,
  LocalCarrerasRepository,
  LocalEventosRepository,
  LocalRutasRepository,
  type AjustesRepository,
  type AuthRepository,
  type CarrerasRepository,
  type EventosRepository,
  type NotificacionesRepository,
  type Repositorios,
  type RutasRepository,
} from './repositorios'
import {
  AJUSTES_POR_DEFECTO,
  type Ajustes,
  type CarreraGuardada,
  type Perfil,
  type ResultadoAuth,
  type Visibilidad,
} from './tipos'

/*
 * Repositorios sobre Supabase. Mantienen las mismas interfaces que los locales; cuando no hay
 * sesion o la red falla se usa la copia local (cache), y las carreras se encolan para subirlas.
 */

type Fila = Record<string, unknown>

const origen = () => (typeof window === 'undefined' ? '' : window.location.origin)

export function traducirError(mensaje: string): string {
  const m = mensaje.toLowerCase()
  if (m.includes('invalid login')) return 'Correo o contraseña incorrectos'
  if (m.includes('already registered') || m.includes('already been registered'))
    return 'Ya existe una cuenta con ese correo'
  if (m.includes('not confirmed')) return 'Confirma tu correo antes de iniciar sesión'
  if (m.includes('rate limit') || m.includes('security purposes'))
    return 'Demasiados intentos. Espera un momento y vuelve a probar'
  if (m.includes('fetch') || m.includes('network')) return 'Sin conexión con el servidor'
  if (m.includes('password')) return 'La contraseña no es válida'
  return 'No se pudo completar. Intenta de nuevo'
}

export function perfilDesdeFila(fila: Fila | null, user: User): Perfil {
  const meta = (user.user_metadata ?? {}) as Record<string, string | undefined>
  const email = (user.email ?? '').toLowerCase()
  const nombre = (fila?.full_name as string) || meta.full_name || meta.name || email.split('@')[0]
  return {
    email,
    nombre,
    username: (fila?.username as string) || ValidacionAuth.usernameDesde(nombre, email),
    bio: (fila?.bio as string) ?? '',
    ubicacion: (fila?.location as string) ?? 'Loja, Ecuador',
    edad: (fila?.age as string) ?? '',
    genero: (fila?.gender as string) ?? 'Prefiero no decir',
    foto: (fila?.avatar_url as string) || meta.avatar_url || null,
  }
}

async function usuario(sb: SupabaseClient): Promise<User | null> {
  const { data } = await sb.auth.getSession()
  return data.session?.user ?? null
}

export class SupabaseAuthRepository implements AuthRepository {
  private sb: SupabaseClient
  constructor(sb: SupabaseClient) {
    this.sb = sb
  }

  private async perfil(user: User): Promise<Perfil> {
    const { data } = await this.sb.from('profiles').select('*').eq('id', user.id).maybeSingle()
    return perfilDesdeFila(data as Fila | null, user)
  }

  async sesionActual() {
    const user = await usuario(this.sb)
    return user ? this.perfil(user) : null
  }

  async iniciarSesion(email: string, password: string): Promise<ResultadoAuth> {
    const errorEmail = ValidacionAuth.email(email)
    if (errorEmail) return { ok: false, error: errorEmail }
    if (!password) return { ok: false, error: 'Ingresa tu contraseña' }
    const { data, error } = await this.sb.auth.signInWithPassword({
      email: email.trim().toLowerCase(),
      password,
    })
    if (error || !data.user) return { ok: false, error: traducirError(error?.message ?? '') }
    return { ok: true, perfil: await this.perfil(data.user) }
  }

  async registrar(nombre: string, email: string, password: string): Promise<ResultadoAuth> {
    const error =
      ValidacionAuth.nombre(nombre) ??
      ValidacionAuth.email(email) ??
      ValidacionAuth.password(password)
    if (error) return { ok: false, error }
    const clave = email.trim().toLowerCase()
    const { data, error: e } = await this.sb.auth.signUp({
      email: clave,
      password,
      options: {
        data: { full_name: nombre.trim(), username: ValidacionAuth.usernameDesde(nombre, clave) },
        emailRedirectTo: `${origen()}/inicio`,
      },
    })
    if (e) return { ok: false, error: traducirError(e.message) }
    // Con la confirmacion de correo activa, Supabase no abre sesion hasta confirmar.
    if (!data.session || !data.user)
      return {
        ok: false,
        error: 'Te enviamos un correo para confirmar tu cuenta. Ábrelo y luego inicia sesión.',
      }
    return { ok: true, perfil: await this.perfil(data.user) }
  }

  async recuperarContrasena(email: string): Promise<ResultadoAuth> {
    const error = ValidacionAuth.email(email)
    if (error) return { ok: false, error }
    const clave = email.trim().toLowerCase()
    const { error: e } = await this.sb.auth.resetPasswordForEmail(clave, {
      redirectTo: `${origen()}/nueva-contrasena`,
    })
    // Igual que en local: no se revela si el correo existe.
    if (e && traducirError(e.message).startsWith('Demasiados'))
      return { ok: false, error: traducirError(e.message) }
    return {
      ok: true,
      perfil: { ...perfilDesdeFila(null, { email: clave } as User), email: clave },
    }
  }

  async cambiarContrasena(password: string): Promise<ResultadoAuth> {
    const error = ValidacionAuth.password(password)
    if (error) return { ok: false, error }
    const { data, error: e } = await this.sb.auth.updateUser({ password })
    if (e || !data.user) return { ok: false, error: e ? traducirError(e.message) : 'Sin sesión' }
    return { ok: true, perfil: await this.perfil(data.user) }
  }

  async actualizarPerfil(perfil: Perfil) {
    const user = await usuario(this.sb)
    if (!user) return perfil
    const { error } = await this.sb
      .from('profiles')
      .update({
        full_name: perfil.nombre,
        username: perfil.username,
        bio: perfil.bio,
        location: perfil.ubicacion,
        age: perfil.edad,
        gender: perfil.genero,
        avatar_url: perfil.foto,
      })
      .eq('id', user.id)
    if (error) throw new Error(traducirError(error.message))
    return perfil
  }

  async cerrarSesion() {
    await this.sb.auth.signOut()
  }

  /** Mira en /auth/v1/settings si Google esta activo, para no mandar al usuario a un error. */
  private async googleActivo(): Promise<boolean> {
    if (!configSupabase.url) return true
    try {
      const r = await fetch(`${configSupabase.url}/auth/v1/settings`, {
        headers: { apikey: configSupabase.anonKey },
      })
      if (!r.ok) return true
      const ajustes = (await r.json()) as { external?: { google?: boolean } }
      return ajustes.external?.google !== false
    } catch {
      return true
    }
  }

  async entrarConGoogle() {
    if (!(await this.googleActivo()))
      return 'El inicio con Google todavía no está activado en el servidor'
    const { error } = await this.sb.auth.signInWithOAuth({
      provider: 'google',
      options: { redirectTo: `${origen()}/inicio` },
    })
    if (error)
      return error.message.toLowerCase().includes('provider is not enabled')
        ? 'El inicio con Google todavía no está activado en el servidor'
        : traducirError(error.message)
    return null
  }

  alCambiarSesion(cb: (perfil: Perfil | null) => void) {
    const { data } = this.sb.auth.onAuthStateChange((evento, sesion) => {
      if (evento === 'INITIAL_SESSION' || evento === 'TOKEN_REFRESHED') return
      // Fuera del callback para no bloquear el cliente de auth.
      setTimeout(() => {
        if (!sesion?.user) cb(null)
        else void this.perfil(sesion.user).then(cb)
      }, 0)
    })
    return () => data.subscription.unsubscribe()
  }
}

/* ---------------- Carreras con cache local y cola sin conexion ---------------- */

export type OperacionPendiente =
  { tipo: 'guardar'; carrera: CarreraGuardada } | { tipo: 'eliminar'; id: string }

const CLAVE_COLA = 'carreras-pendientes'

export const filaDesdeCarrera = (c: CarreraGuardada, userId: string) => ({
  user_id: userId,
  client_id: c.id,
  title: c.titulo,
  description: c.nota,
  is_public: c.publica,
  start_time: new Date(c.inicioMs).toISOString(),
  end_time: new Date(c.inicioMs + c.segundos * 1000).toISOString(),
  duration_seconds: Math.round(c.segundos),
  distance_meters: Math.round(c.distanciaKm * 1000),
  avg_pace_seconds_per_km: c.distanciaKm > 0 ? Math.round(c.segundos / c.distanciaKm) : null,
  elevation_gain_meters: Math.round(c.desnivelM),
  splits: c.parciales,
})

export function carreraDesdeFila(fila: Fila, puntos: CarreraGuardada['puntos']): CarreraGuardada {
  return {
    id: (fila.client_id as string) || (fila.id as string),
    titulo: fila.title as string,
    nota: (fila.description as string) ?? '',
    publica: Boolean(fila.is_public),
    inicioMs: Date.parse(fila.start_time as string),
    segundos: Number(fila.duration_seconds ?? 0),
    distanciaKm: Number(fila.distance_meters ?? 0) / 1000,
    desnivelM: Number(fila.elevation_gain_meters ?? 0),
    parciales: (fila.splits as CarreraGuardada['parciales']) ?? [],
    puntos,
  }
}

export class SupabaseCarrerasRepository implements CarrerasRepository {
  private local = new LocalCarrerasRepository()
  private sb: SupabaseClient
  constructor(sb: SupabaseClient) {
    this.sb = sb
  }

  cola = () => leer<OperacionPendiente[]>(CLAVE_COLA, [])

  private async subir(op: OperacionPendiente, userId: string) {
    if (op.tipo === 'eliminar') {
      const { error } = await this.sb
        .from('activities')
        .delete()
        .eq('user_id', userId)
        .eq('client_id', op.id)
      if (error) throw error
      return
    }
    const { data, error } = await this.sb
      .from('activities')
      .upsert(filaDesdeCarrera(op.carrera, userId), { onConflict: 'user_id,client_id' })
      .select('id')
      .single()
    if (error || !data) throw error ?? new Error('sin id')
    const { error: e2 } = await this.sb
      .from('activity_tracks')
      .upsert(
        { activity_id: (data as Fila).id, user_id: userId, points: op.carrera.puntos },
        { onConflict: 'activity_id' }
      )
    if (e2) throw e2
  }

  /** Sube lo pendiente en orden; lo que falle queda en la cola. Devuelve cuantas quedan. */
  async sincronizar(): Promise<number> {
    const user = await usuario(this.sb)
    const cola = this.cola()
    if (!user || cola.length === 0) return cola.length
    const restantes: OperacionPendiente[] = []
    for (const op of cola) {
      if (restantes.length) {
        restantes.push(op)
        continue
      }
      try {
        await this.subir(op, user.id)
      } catch {
        restantes.push(op)
      }
    }
    escribir(CLAVE_COLA, restantes)
    return restantes.length
  }

  async listar() {
    const user = await usuario(this.sb)
    if (!user) return this.local.listar()
    await this.sincronizar()
    try {
      const [act, tracks] = await Promise.all([
        this.sb
          .from('activities')
          .select('*')
          .eq('user_id', user.id)
          .order('start_time', { ascending: false }),
        this.sb.from('activity_tracks').select('activity_id,points').eq('user_id', user.id),
      ])
      if (act.error || tracks.error) throw act.error ?? tracks.error
      const puntos = new Map(
        ((tracks.data ?? []) as Fila[]).map((t) => [
          t.activity_id,
          t.points as CarreraGuardada['puntos'],
        ])
      )
      const remotas = ((act.data ?? []) as Fila[]).map((f) =>
        carreraDesdeFila(f, puntos.get(f.id) ?? [])
      )
      // Las carreras aun no subidas se muestran igual.
      const pendientes = this.cola().flatMap((o) => (o.tipo === 'guardar' ? [o.carrera] : []))
      const borradas = new Set(this.cola().flatMap((o) => (o.tipo === 'eliminar' ? [o.id] : [])))
      const todas = [
        ...pendientes,
        ...remotas.filter((r) => !pendientes.some((p) => p.id === r.id)),
      ]
        .filter((c) => !borradas.has(c.id))
        .sort((a, b) => b.inicioMs - a.inicioMs)
      return escribir('carreras', todas)
    } catch {
      return this.local.listar()
    }
  }

  async guardar(c: CarreraGuardada) {
    const lista = await this.local.guardar(c)
    escribir(CLAVE_COLA, [
      ...this.cola().filter((o) => !(o.tipo === 'guardar' && o.carrera.id === c.id)),
      { tipo: 'guardar', carrera: c },
    ])
    await this.sincronizar()
    return lista
  }

  async eliminar(id: string) {
    const lista = await this.local.eliminar(id)
    escribir(CLAVE_COLA, [
      ...this.cola().filter((o) => !(o.tipo === 'guardar' && o.carrera.id === id)),
      { tipo: 'eliminar', id },
    ])
    await this.sincronizar()
    return lista
  }
}

/* ---------------- Rutas guardadas ---------------- */

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
        prefs,
      },
      { onConflict: 'user_id' }
    )
    return nuevos
  }
}

/* ---------------- Eventos ---------------- */

export function eventoDesdeFila(f: Fila): Evento {
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
    creadoPorUsuario: true,
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
    const { data, error } = await this.sb
      .from('events')
      .select('*')
      .eq('created_by', user.id)
      .order('created_at', { ascending: false })
    if (error) return this.local.creados()
    return escribir('eventos-creados', ((data ?? []) as Fila[]).map(eventoDesdeFila))
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

export const repositoriosSupabase = (sb: SupabaseClient): Repositorios => ({
  auth: new SupabaseAuthRepository(sb),
  carreras: new SupabaseCarrerasRepository(sb),
  rutas: new SupabaseRutasRepository(sb),
  ajustes: new SupabaseAjustesRepository(sb),
  eventos: new SupabaseEventosRepository(sb),
  notificaciones: new SupabaseNotificacionesRepository(sb),
})

const TIPOS_NOTIF = ['like', 'comment', 'follow', 'event', 'live'] as const

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
