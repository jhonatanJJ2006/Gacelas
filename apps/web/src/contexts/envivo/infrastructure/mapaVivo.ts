import { obtenerSupabase } from '../../../shared/supabase/client'
import { type CorredorEnMapa, tocaPublicar, visibles } from '../domain/mapaVivo'
export * from '../domain/mapaVivo'

type Fila = Record<string, unknown>

export function corredorDesdeFila(f: Fila, perfil?: Fila): CorredorEnMapa {
  return {
    userId: String(f.user_id),
    nombre: String(perfil?.full_name ?? perfil?.username ?? 'Corredor'),
    avatarUrl: (perfil?.avatar_url as string | null) ?? null,
    lat: Number(f.lat),
    lng: Number(f.lng),
    corriendo: Boolean(f.running),
    actualizadoMs: typeof f.updated_at === 'string' ? new Date(f.updated_at).getTime() : 0,
  }
}

let ultimo: { ms: number; corriendo: boolean } | null = null

/** Publica la posicion propia (el servidor exige consentimiento y vuelve a limitar a 10 s). */
export async function publicarPosicion(
  lat: number,
  lng: number,
  corriendo: boolean,
  ahora = Date.now()
) {
  if (!tocaPublicar(ultimo?.ms ?? null, ultimo?.corriendo ?? null, ahora, corriendo)) return false
  ultimo = { ms: ahora, corriendo }
  const sb = obtenerSupabase()
  if (!sb) return false
  const { data, error } = await sb.rpc('mapa_publicar', { lat, lng, corriendo })
  return !error && data === true
}

export async function dejarDeCompartir() {
  ultimo = null
  const sb = obtenerSupabase()
  if (!sb) return
  const { data } = await sb.auth.getUser()
  if (data.user) await sb.from('runner_positions').delete().eq('user_id', data.user.id)
}

export async function listarCorredores(ahora = Date.now()): Promise<CorredorEnMapa[]> {
  const sb = obtenerSupabase()
  if (!sb) return []
  const [{ data: filas }, { data: yo }] = await Promise.all([
    sb.from('runner_positions').select('*').order('updated_at', { ascending: false }),
    sb.auth.getUser(),
  ])
  const lista = (filas ?? []) as Fila[]
  const ids = lista.map((f) => String(f.user_id))
  const { data: perfiles } = ids.length
    ? await sb.from('profiles').select('id, full_name, username, avatar_url').in('id', ids)
    : { data: [] }
  const porId = new Map(((perfiles ?? []) as Fila[]).map((p) => [String(p.id), p]))
  return visibles(
    lista.map((f) => corredorDesdeFila(f, porId.get(String(f.user_id)))),
    ahora,
    yo.user?.id ?? null
  )
}

/** Avisa de cualquier cambio en runner_positions (Supabase Realtime). */
export function suscribirCorredores(alCambiar: () => void): () => void {
  const sb = obtenerSupabase()
  if (!sb) return () => undefined
  const canal = sb
    .channel('mapa-vivo')
    .on('postgres_changes', { event: '*', schema: 'public', table: 'runner_positions' }, alCambiar)
    .subscribe()
  return () => {
    void sb.removeChannel(canal)
  }
}
