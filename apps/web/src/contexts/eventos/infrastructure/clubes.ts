import { obtenerSupabase } from '../../../shared/supabase/client'
import {
  type Club,
  type EstadoMiembro,
  type MiembroClub,
  type PostClub,
  type RolClub,
} from '../domain/clubes'
export * from '../domain/clubes'

type Fila = Record<string, unknown>
export const clubDesdeFila = (f: Fila): Club => ({
  id: String(f.id),
  nombre: String(f.name ?? ''),
  descripcion: String(f.description ?? ''),
  ubicacion: String(f.location ?? ''),
  conAprobacion: Boolean(f.requires_approval),
  miembros: Number(f.members_count ?? 0),
  ownerId: String(f.owner_id ?? ''),
})

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
  async listar(): Promise<{ todos: Club[]; mios: Set<string> }> {
    const todos = (
      ok(
        await sb().from('clubs').select('*').order('members_count', { ascending: false }).limit(100)
      ) as Fila[]
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
  async crear(c: {
    nombre: string
    descripcion: string
    ubicacion: string
    conAprobacion: boolean
  }) {
    const yo = await uid()
    if (!yo) throw new Error('Inicia sesión para crear un club')
    const f = ok(
      await sb()
        .from('clubs')
        .insert({
          name: c.nombre.trim(),
          description: c.descripcion.trim(),
          location: c.ubicacion.trim(),
          requires_approval: c.conAprobacion,
          owner_id: yo,
        })
        .select()
        .single()
    ) as Fila
    return clubDesdeFila(f)
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
