import type { SupabaseClient } from '@supabase/supabase-js'
import {
  type ComentarioPublicacion,
  type CorredorPublico,
  type PublicacionRemota,
} from '../../../shared/kernel/comunidad'
import { traducirError } from '../../../shared/supabase/errores'
import { usuario } from '../../../shared/supabase/sesion'
import { type ComunidadRepository } from '../domain/puertos'

/* Repositorio de la comunidad sobre Supabase: feed, comentarios, me gusta y corredores. */

type Fila = Record<string, unknown>

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
