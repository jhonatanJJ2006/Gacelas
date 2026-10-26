import { obtenerSupabase } from '../../../shared/supabase/client'

/*
 * Origen de datos del panel de administracion. Los casos de moderacion y las estadisticas
 * vienen de sus contextos; se reciben como parametros para no depender de su codigo.
 */

/** Casos de moderacion que el panel lista y resuelve. */
export interface CasosAdmin<R, A> {
  pendientes(): Promise<R[]>
  moderar(r: R, accion: A, nota: string): Promise<void>
}

/** Estadisticas de uso entre dos fechas. */
export interface AnaliticaAdmin<E> {
  estadisticas(desde: string, hasta: string): Promise<E>
}

export interface Metricas {
  usuarios: number
  usuarios_7d: number
  carreras_7d: number
  km_7d: number
  reportes_pendientes: number
  eventos_proximos: number
  clubes: number
  rutas: number
}
export interface UsuarioAdmin {
  id: string
  username: string
  full_name: string | null
  created_at: string
  suspended_at: string | null
  role: 'admin' | 'moderador' | null
  carreras: number
  km: number
}
export interface EventoAdmin {
  id: string
  title: string
  description: string
  starts_at: string | null
  location: string
  distance_km: number
  published: boolean
}
export interface RutaAdmin {
  id: string
  name: string
  distance_km: number
  difficulty: string
}
export interface ClubAdmin {
  id: string
  name: string
  description: string
}
export interface AjusteApp {
  key: string
  value: unknown
}

/** Origen de datos del panel. En produccion es Supabase; en desarrollo hay datos de ejemplo. */
export interface FuenteAdmin<R, A, E> {
  demo: boolean
  esAdmin(): Promise<boolean>
  metricas(): Promise<Metricas>
  usuarios(): Promise<UsuarioAdmin[]>
  suspender(id: string, si: boolean): Promise<void>
  cambiarRol(id: string, rol: UsuarioAdmin['role']): Promise<void>
  reportes(): Promise<R[]>
  moderar(r: R, accion: A, nota: string): Promise<void>
  estadisticas(desde: string, hasta: string): Promise<E>
  eventos(): Promise<EventoAdmin[]>
  guardarEvento(e: Partial<EventoAdmin> & { title: string }): Promise<void>
  rutas(): Promise<RutaAdmin[]>
  borrarRuta(id: string): Promise<void>
  clubes(): Promise<ClubAdmin[]>
  borrarClub(id: string): Promise<void>
  ajustes(): Promise<AjusteApp[]>
  guardarAjuste(key: string, value: unknown): Promise<void>
  versionTerminos(): Promise<string | null>
  publicarTerminos(version: string): Promise<void>
}

function ok<T>(r: { data: T | null; error: { message: string } | null }): T {
  if (r.error) throw new Error(r.error.message)
  return r.data as T
}

export function fuenteSupabase<R, A, E>(
  casos: CasosAdmin<R, A> | null,
  analitica: AnaliticaAdmin<E> | null
): FuenteAdmin<R, A, E> | null {
  const sb = obtenerSupabase()
  if (!sb || !casos || !analitica) return null
  return {
    demo: false,
    async esAdmin() {
      const r = await sb.rpc('is_admin')
      return !r.error && r.data === true
    },
    async metricas() {
      return ok(await sb.rpc('admin_metricas')) as Metricas
    },
    async usuarios() {
      return ok(await sb.rpc('admin_usuarios')) as UsuarioAdmin[]
    },
    async suspender(id, si) {
      ok(await sb.rpc('admin_suspender', { uid: id, suspender: si }))
    },
    async cambiarRol(id, rol) {
      if (rol) ok(await sb.from('user_roles').upsert({ user_id: id, role: rol }))
      else ok(await sb.from('user_roles').delete().eq('user_id', id))
    },
    reportes: () => casos.pendientes(),
    moderar: (r, accion, nota) => casos.moderar(r, accion, nota),
    estadisticas: (desde, hasta) => analitica.estadisticas(desde, hasta),
    async eventos() {
      return ok(
        await sb
          .from('events')
          .select('id,title,description,starts_at,location,distance_km,published')
          .order('starts_at', { ascending: false })
      ) as EventoAdmin[]
    },
    async guardarEvento(e) {
      const fila = {
        title: e.title,
        description: e.description ?? '',
        starts_at: e.starts_at,
        location: e.location ?? '',
        distance_km: e.distance_km ?? 0,
        published: e.published ?? false,
        date_label: e.starts_at ? new Date(e.starts_at).toLocaleDateString('es-EC') : '',
        time_label: e.starts_at
          ? new Date(e.starts_at).toLocaleTimeString('es-EC', {
              hour: '2-digit',
              minute: '2-digit',
            })
          : '',
      }
      if (e.id) ok(await sb.from('events').update(fila).eq('id', e.id))
      else ok(await sb.from('events').insert(fila))
    },
    async rutas() {
      return ok(
        await sb.from('routes').select('id,name,distance_km,difficulty').order('name')
      ) as RutaAdmin[]
    },
    async borrarRuta(id) {
      ok(await sb.from('routes').delete().eq('id', id))
    },
    async clubes() {
      return ok(await sb.from('clubs').select('id,name,description').order('name')) as ClubAdmin[]
    },
    async borrarClub(id) {
      ok(await sb.from('clubs').delete().eq('id', id))
    },
    async ajustes() {
      return ok(await sb.from('app_settings').select('key,value').order('key')) as AjusteApp[]
    },
    async guardarAjuste(key, value) {
      ok(await sb.from('app_settings').upsert({ key, value, updated_at: new Date().toISOString() }))
    },
    async versionTerminos() {
      const r = await sb
        .from('legal_versions')
        .select('version')
        .eq('kind', 'terminos')
        .order('published_at', { ascending: false })
        .limit(1)
      return (ok(r) as { version: string }[])[0]?.version ?? null
    },
    async publicarTerminos(version) {
      ok(await sb.from('legal_versions').insert({ kind: 'terminos', version }))
    },
  }
}

/** Datos de ejemplo para revisar el panel en desarrollo sin servidor ni rol. */
export function fuenteDemo<R extends { status: string }, A, E>(
  casos: CasosAdmin<R, A>,
  estadisticas: (desde: string, hasta: string) => E
): FuenteAdmin<R, A, E> {
  const usuarios: UsuarioAdmin[] = [
    ['Andrea Montaño', 'andre.mont', 214, null],
    ['Kevin Jaramillo', 'kevin.jaramillo', 66, null],
    ['Daniela Cueva', 'dani.cueva', 412, 'moderador'],
    ['Luis Ordóñez', 'luiso.trail', 1020, null],
    ['Jhonatan Jara', 'jhonatan', 880, 'admin'],
  ].map(([n, u, km, role], i) => ({
    id: `u${i}`,
    username: u as string,
    full_name: n as string,
    created_at: new Date(2026, 9, 20 - i * 3).toISOString(),
    suspended_at: null,
    role: role as UsuarioAdmin['role'],
    carreras: Math.round((km as number) / 6),
    km: km as number,
  }))
  const eventos: EventoAdmin[] = [
    {
      id: 'e1',
      title: '10K Ciudad de Loja',
      description: 'Carrera urbana por el centro histórico.',
      starts_at: '2026-11-15T07:00:00-05:00',
      location: 'Parque Central',
      distance_km: 10,
      published: true,
    },
    {
      id: 'e2',
      title: 'Trail Podocarpus',
      description: 'Salida grupal de montaña.',
      starts_at: '2026-11-29T06:30:00-05:00',
      location: 'Cajanuma',
      distance_km: 18,
      published: false,
    },
  ]
  const ajustes: AjusteApp[] = [
    { key: 'mantenimiento', value: false },
    { key: 'registro_abierto', value: true },
    { key: 'version_minima_android', value: '1.0.0' },
  ]
  let terminos = '2026-10-25'
  const nada = () => Promise.resolve()
  return {
    demo: true,
    esAdmin: () => Promise.resolve(true),
    metricas: async () => ({
      usuarios: 2418,
      usuarios_7d: 126,
      carreras_7d: 1094,
      km_7d: 6120.4,
      reportes_pendientes: (await casos.pendientes()).filter((r) => r.status === 'pendiente')
        .length,
      eventos_proximos: 6,
      clubes: 14,
      rutas: 52,
    }),
    usuarios: () => Promise.resolve(usuarios),
    async suspender(id, si) {
      const u = usuarios.find((x) => x.id === id)
      if (u) u.suspended_at = si ? new Date().toISOString() : null
    },
    async cambiarRol(id, rol) {
      const u = usuarios.find((x) => x.id === id)
      if (u) u.role = rol
    },
    reportes: () => casos.pendientes(),
    moderar: (r, accion, nota) => casos.moderar(r, accion, nota),
    estadisticas: (desde, hasta) => Promise.resolve(estadisticas(desde, hasta)),
    eventos: () => Promise.resolve(eventos),
    async guardarEvento(e) {
      const i = eventos.findIndex((x) => x.id === e.id)
      const nuevo = {
        id: e.id ?? `e${eventos.length + 1}`,
        description: '',
        starts_at: null,
        location: '',
        distance_km: 0,
        published: false,
        ...e,
      }
      if (i >= 0) eventos[i] = nuevo
      else eventos.unshift(nuevo)
    },
    rutas: () =>
      Promise.resolve([
        { id: 'r1', name: 'Parque Jipiro', distance_km: 3.2, difficulty: 'Fácil' },
        { id: 'r2', name: 'Mirador Las Pavas', distance_km: 7.8, difficulty: 'Difícil' },
      ]),
    borrarRuta: nada,
    clubes: () =>
      Promise.resolve([
        { id: 'c1', name: 'Club Gacelas Loja', description: 'Club principal.' },
        { id: 'c2', name: 'Runners UNL', description: 'Universitarios.' },
      ]),
    borrarClub: nada,
    ajustes: () => Promise.resolve(ajustes),
    async guardarAjuste(key, value) {
      const a = ajustes.find((x) => x.key === key)
      if (a) a.value = value
    },
    versionTerminos: () => Promise.resolve(terminos),
    async publicarTerminos(v) {
      terminos = v
    },
  }
}
