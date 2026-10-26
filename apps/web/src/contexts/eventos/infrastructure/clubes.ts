import { obtenerSupabase } from '../../../shared/supabase/client'
import {
  mensajeErrorClub,
  type BorradorClub,
  type Club,
  type NivelGrupo,
  type TipoClub,
  type EstadoMiembro,
  type MiembroClub,
  type PostClub,
  type RolClub,
} from '../domain/clubes'
export * from '../domain/clubes'

type Fila = Record<string, unknown>
const texto = (v: unknown) => (v == null ? '' : String(v))
const numero = (v: unknown) => (v == null || v === '' ? null : Number(v))

/** URL publica de una ruta "bucket/archivo" del Storage de Supabase. */
export function urlPublica(ruta: string | null | undefined) {
  if (!ruta) return null
  if (/^(https?:|data:|blob:)/.test(ruta)) return ruta
  const base = import.meta.env.VITE_SUPABASE_URL as string | undefined
  return base ? `${base}/storage/v1/object/public/${ruta}` : null
}

export const clubDesdeFila = (f: Fila): Club => {
  const lat = numero(f.meeting_lat)
  const lng = numero(f.meeting_lng)
  return {
    id: String(f.id),
    tipo: f.kind === 'comunidad' ? 'comunidad' : 'grupo',
    nombre: texto(f.name),
    descripcion: texto(f.description),
    ubicacion: texto(f.location),
    ciudad: texto(f.city ?? 'Loja'),
    conAprobacion: Boolean(f.requires_approval),
    miembros: Number(f.members_count ?? 0),
    ownerId: texto(f.owner_id),
    comunidadId: f.community_id ? String(f.community_id) : null,
    logo: f.logo_path ? String(f.logo_path) : null,
    portada: f.cover_path ? String(f.cover_path) : null,
    contacto: texto(f.contact),
    lugarEncuentro: texto(f.meeting_place),
    punto: lat != null && lng != null ? { lat, lng } : null,
    dias: Array.isArray(f.training_days) ? (f.training_days as unknown[]).map(Number) : [],
    hora: f.training_time ? String(f.training_time).slice(0, 5) : null,
    nivel: (['principiante', 'intermedio', 'avanzado'].includes(String(f.level))
      ? f.level
      : 'todos') as NivelGrupo,
    ritmoSegKm: numero(f.pace_s_km),
    cupo: numero(f.max_members),
    oculto: Boolean(f.hidden_at),
  }
}

/** Columnas que escribe el cliente (owner, contadores y oculto los decide el servidor). */
export const filaDesdeBorrador = (b: BorradorClub): Fila => {
  const grupo = b.tipo === 'grupo'
  return {
    name: b.nombre.trim().replace(/\s+/g, ' '),
    description: b.descripcion.trim(),
    location: grupo ? b.lugarEncuentro.trim() : b.ciudad.trim(),
    city: b.ciudad.trim(),
    requires_approval: b.conAprobacion,
    community_id: grupo ? b.comunidadId : null,
    contact: b.contacto.trim(),
    logo_path: b.logo,
    cover_path: b.portada,
    meeting_place: grupo ? b.lugarEncuentro.trim() : '',
    meeting_lat: grupo ? (b.punto?.lat ?? null) : null,
    meeting_lng: grupo ? (b.punto?.lng ?? null) : null,
    training_days: grupo ? [...b.dias].sort((x, y) => x - y) : [],
    training_time: grupo ? b.hora : null,
    level: grupo ? b.nivel : 'todos',
    pace_s_km: grupo ? b.ritmoSegKm : null,
    max_members: grupo ? b.cupo : null,
  }
}

const sb = () => {
  const c = obtenerSupabase()
  if (!c) throw new Error('Los clubes necesitan conexión con el servidor')
  return c
}
const uid = async () => (await sb().auth.getUser()).data.user?.id ?? null
const ok = <T>(r: { data: T; error: { message: string } | null }) => {
  if (r.error) throw new Error(r.error.message)
  return r.data
}

async function nombres(ids: string[]) {
  if (!ids.length) return new Map<string, string>()
  const filas = ok(
    await sb().from('profiles').select('id,full_name,username').in('id', ids)
  ) as Fila[]
  return new Map(filas.map((f) => [String(f.id), String(f.full_name || f.username || 'Corredor')]))
}

export const clubes = {
  disponible: () => obtenerSupabase() !== null,
  uid,
  async listar(tipo?: TipoClub): Promise<{ todos: Club[]; mios: Set<string> }> {
    let q = sb().from('clubs').select('*')
    if (tipo) q = q.eq('kind', tipo)
    const todos = (
      ok(await q.order('members_count', { ascending: false }).limit(200)) as Fila[]
    ).map(clubDesdeFila)
    const yo = await uid()
    const mios = yo
      ? new Set(
          (
            ok(
              await sb()
                .from('club_members')
                .select('club_id')
                .eq('user_id', yo)
                .eq('status', 'active')
            ) as Fila[]
          ).map((f) => String(f.club_id))
        )
      : new Set<string>()
    return { todos, mios }
  },
  async crear(b: BorradorClub) {
    const yo = await uid()
    if (!yo) throw new Error('Inicia sesión para registrar un grupo o comunidad')
    const r = await sb()
      .from('clubs')
      .insert({ ...filaDesdeBorrador(b), kind: b.tipo, owner_id: yo })
      .select()
      .single()
    if (r.error) throw new Error(mensajeErrorClub(r.error.message, b.tipo))
    return clubDesdeFila(r.data as Fila)
  },
  async editar(id: string, b: BorradorClub) {
    const r = await sb().from('clubs').update(filaDesdeBorrador(b)).eq('id', id)
    if (r.error) throw new Error(mensajeErrorClub(r.error.message, b.tipo))
  },
  async eliminar(id: string) {
    ok(await sb().from('clubs').delete().eq('id', id))
  },
  /** Sube un logo o portada a la carpeta del usuario y devuelve "bucket/ruta". */
  async subirImagen(archivo: Blob, nombre: string) {
    const yo = await uid()
    if (!yo) throw new Error('Inicia sesión para subir imágenes')
    const ext = (/\.(png|webp|jpe?g)$/i.exec(nombre)?.[1] ?? 'jpg').toLowerCase()
    const ruta = `${yo}/clubes/${Date.now()}.${ext}`
    const r = await sb().storage.from('post-images').upload(ruta, archivo, { upsert: false })
    if (r.error) throw new Error('No se pudo subir la imagen')
    return `post-images/${ruta}`
  },
  async grupos(comunidadId: string) {
    return (
      ok(
        await sb().from('clubs').select('*').eq('community_id', comunidadId).eq('kind', 'grupo')
      ) as Fila[]
    ).map(clubDesdeFila)
  },
  async detalle(id: string) {
    const f = ok(await sb().from('clubs').select('*').eq('id', id).maybeSingle()) as Fila | null
    return f ? clubDesdeFila(f) : null
  },
  async miembros(id: string): Promise<MiembroClub[]> {
    const filas = ok(
      await sb()
        .from('club_members')
        .select('user_id,role,status')
        .eq('club_id', id)
        .order('created_at')
    ) as Fila[]
    const n = await nombres(filas.map((f) => String(f.user_id)))
    const orden = { owner: 0, admin: 1, member: 2 }
    return filas
      .map((f) => ({
        usuarioId: String(f.user_id),
        nombre: n.get(String(f.user_id)) ?? 'Corredor',
        rol: f.role as RolClub,
        estado: f.status as EstadoMiembro,
      }))
      .sort((a, b) => orden[a.rol] - orden[b.rol])
  },
  async unirse(id: string) {
    const yo = await uid()
    if (!yo) throw new Error('Inicia sesión para unirte')
    ok(await sb().from('club_members').insert({ club_id: id, user_id: yo }))
  },
  async salir(id: string, usuario?: string) {
    const yo = usuario ?? (await uid())
    ok(await sb().from('club_members').delete().eq('club_id', id).eq('user_id', yo!))
  },
  async aprobar(id: string, usuario: string) {
    ok(
      await sb()
        .from('club_members')
        .update({ status: 'active' })
        .eq('club_id', id)
        .eq('user_id', usuario)
    )
  },
  async cambiarRol(id: string, usuario: string, rol: 'admin' | 'member') {
    ok(
      await sb().from('club_members').update({ role: rol }).eq('club_id', id).eq('user_id', usuario)
    )
  },
  /** Avisa cuando cambian las publicaciones del club (realtime). Devuelve la baja. */
  escucharPosts(id: string, alCambiar: () => void): () => void {
    const cliente = obtenerSupabase()
    if (!cliente || typeof cliente.channel !== 'function') return () => undefined
    const canal = cliente
      .channel(`club-posts-${id}`)
      .on(
        'postgres_changes',
        { event: '*', schema: 'public', table: 'club_posts', filter: `club_id=eq.${id}` },
        () => alCambiar()
      )
      .subscribe()
    return () => void cliente.removeChannel(canal)
  },
  async posts(id: string): Promise<PostClub[]> {
    const filas = ok(
      await sb()
        .from('club_posts')
        .select('*')
        .eq('club_id', id)
        .order('created_at', { ascending: false })
        .limit(50)
    ) as Fila[]
    const n = await nombres([...new Set(filas.map((f) => String(f.user_id)))])
    return filas.map((f) => ({
      id: String(f.id),
      usuarioId: String(f.user_id),
      nombre: n.get(String(f.user_id)) ?? 'Corredor',
      texto: String(f.body),
      creadoMs: Date.parse(String(f.created_at)),
    }))
  },
  async publicar(id: string, texto: string) {
    const yo = await uid()
    ok(await sb().from('club_posts').insert({ club_id: id, user_id: yo, body: texto.trim() }))
  },
  async borrarPost(postId: string) {
    ok(await sb().from('club_posts').delete().eq('id', postId))
  },
  async eventos(id: string) {
    const filas = ok(
      await sb().from('events').select('*').eq('club_id', id).order('starts_at')
    ) as Fila[]
    return filas.filter((f) => f.published !== false)
  },
}
