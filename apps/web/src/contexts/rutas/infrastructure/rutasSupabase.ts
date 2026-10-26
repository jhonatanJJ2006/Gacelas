import type { SupabaseClient } from '@supabase/supabase-js'
import {
  escribir,
  LocalRutasRepository,
  type CorredorRuta,
  type ExplorarRepository,
  type FotoRuta,
  type NuevaRuta,
  type RutasRepository,
} from '../../../data/repositorios'
import type { Ruta } from '../../../mocks/data'
import { traducirError } from '../../../shared/supabase/errores'
import { usuario } from '../../../shared/supabase/sesion'

/* Repositorios de rutas sobre Supabase (contexto Rutas), con la copia local como respaldo. */

type Fila = Record<string, unknown>

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
