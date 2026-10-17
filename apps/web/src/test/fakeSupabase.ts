/* Cliente de Supabase falso en memoria para pruebas unitarias (sin red ni secretos). */
import type { SupabaseClient } from '@supabase/supabase-js'

type Fila = Record<string, unknown>
type Resultado = { data: unknown; error: { message: string } | null }

export interface FakeSupabase {
  cliente: SupabaseClient
  tablas: Record<string, Fila[]>
  usuarios: { id: string; email: string; password: string; meta: Fila }[]
  sesion: { user: { id: string; email: string; user_metadata: Fila } } | null
  sinRed: boolean
  confirmarCorreo: boolean
  llamadas: string[]
  oauth: Fila | null
}

const CLAVES: Record<string, string[]> = {
  activities: ['user_id', 'client_id'],
  activity_tracks: ['activity_id'],
  user_settings: ['user_id'],
  saved_routes: ['user_id', 'route_id'],
  event_rsvps: ['event_id', 'user_id'],
  post_likes: ['user_id', 'post_id'],
}

export function crearFakeSupabase(): FakeSupabase {
  const f: FakeSupabase = {
    tablas: {},
    usuarios: [],
    sesion: null,
    sinRed: false,
    confirmarCorreo: false,
    llamadas: [],
    oauth: null,
    cliente: null as unknown as SupabaseClient,
  }
  let n = 0
  const tabla = (t: string) => (f.tablas[t] ??= [])
  const errRed = { message: 'Failed to fetch' }

  function consulta(t: string) {
    const filtros: [string, unknown][] = []
    const predicados: ((r: Fila) => boolean)[] = []
    let op: 'select' | 'insert' | 'upsert' | 'update' | 'delete' = 'select'
    let datos: Fila[] = []
    let cambios: Fila = {}
    let orden: { col: string; asc: boolean } | null = null
    let modo: 'lista' | 'una' | 'quiza' = 'lista'
    let devolver = false

    const coincide = (r: Fila) =>
      filtros.every(([c, v]) => r[c] === v) && predicados.every((p) => p(r))
    const ejecutar = (): Resultado => {
      f.llamadas.push(`${op}:${t}`)
      if (f.sinRed) return { data: null, error: errRed }
      let filas: Fila[] = []
      if (op === 'select') filas = tabla(t).filter(coincide)
      else if (op === 'insert' || op === 'upsert') {
        for (const d of datos) {
          const fila: Fila = {
            id: d.id ?? `id-${++n}`,
            created_at: new Date(++n).toISOString(),
            ...d,
          }
          const claves = CLAVES[t]
          const i =
            op === 'upsert' && claves
              ? tabla(t).findIndex((r) => claves.every((k) => r[k] === fila[k]))
              : -1
          if (i >= 0) tabla(t)[i] = { ...tabla(t)[i], ...d }
          else tabla(t).push(fila)
          filas.push(i >= 0 ? tabla(t)[i] : fila)
        }
      } else if (op === 'update') {
        filas = tabla(t).filter(coincide)
        filas.forEach((r) => Object.assign(r, cambios))
      } else {
        f.tablas[t] = tabla(t).filter((r) => !coincide(r))
      }
      if (orden) {
        const { col, asc } = orden
        filas = [...filas].sort(
          (a, b) => (String(a[col]) < String(b[col]) ? -1 : 1) * (asc ? 1 : -1)
        )
      }
      if (modo !== 'lista') return { data: filas[0] ?? null, error: null }
      return { data: op === 'select' || devolver ? filas : null, error: null }
    }

    const b = {
      select() {
        if (op !== 'select') devolver = true
        return b
      },
      insert(d: Fila | Fila[]) {
        op = 'insert'
        datos = Array.isArray(d) ? d : [d]
        return b
      },
      upsert(d: Fila | Fila[]) {
        op = 'upsert'
        datos = Array.isArray(d) ? d : [d]
        return b
      },
      update(c: Fila) {
        op = 'update'
        cambios = c
        return b
      },
      delete() {
        op = 'delete'
        return b
      },
      eq(c: string, v: unknown) {
        filtros.push([c, v])
        return b
      },
      neq(c: string, v: unknown) {
        predicados.push((r) => r[c] !== v)
        return b
      },
      in(c: string, vs: unknown[]) {
        predicados.push((r) => vs.includes(r[c]))
        return b
      },
      not(c: string, _op: string, v: unknown) {
        predicados.push((r) => (v === null ? r[c] != null : r[c] !== v))
        return b
      },
      limit() {
        return b
      },
      order(col: string, o?: { ascending?: boolean }) {
        orden = { col, asc: o?.ascending ?? true }
        return b
      },
      single() {
        modo = 'una'
        return b
      },
      maybeSingle() {
        modo = 'quiza'
        return b
      },
      then<R>(ok: (r: Resultado) => R, mal?: (e: unknown) => R) {
        return Promise.resolve(ejecutar()).then(ok, mal)
      },
    }
    return b
  }

  const escuchas: ((e: string, s: unknown) => void)[] = []
  const aviso = (e: string) => escuchas.forEach((cb) => cb(e, f.sesion))
  const abrir = (u: FakeSupabase['usuarios'][number]) => {
    f.sesion = { user: { id: u.id, email: u.email, user_metadata: u.meta } }
    aviso('SIGNED_IN')
    return f.sesion
  }

  const auth = {
    async getSession() {
      return { data: { session: f.sesion }, error: null }
    },
    async signInWithPassword({ email, password }: { email: string; password: string }) {
      if (f.sinRed) return { data: { user: null }, error: errRed }
      const u = f.usuarios.find((x) => x.email === email && x.password === password)
      if (!u) return { data: { user: null }, error: { message: 'Invalid login credentials' } }
      return { data: { user: abrir(u).user }, error: null }
    },
    async signUp({
      email,
      password,
      options,
    }: {
      email: string
      password: string
      options?: { data?: Fila }
    }) {
      if (f.usuarios.some((x) => x.email === email))
        return {
          data: { user: null, session: null },
          error: { message: 'User already registered' },
        }
      const u = { id: `u-${++n}`, email, password, meta: options?.data ?? {} }
      f.usuarios.push(u)
      // Igual que el trigger handle_new_user.
      tabla('profiles').push({
        id: u.id,
        email,
        full_name: u.meta.full_name,
        username: u.meta.username,
      })
      tabla('user_settings').push({
        user_id: u.id,
        visibility: 'followers',
        hide_home: true,
        home_radius_m: 300,
        precise_location: true,
        prefs: {},
      })
      if (f.confirmarCorreo) return { data: { user: { id: u.id }, session: null }, error: null }
      const s = abrir(u)
      return { data: { user: s.user, session: s }, error: null }
    },
    async signOut() {
      f.sesion = null
      aviso('SIGNED_OUT')
      return { error: null }
    },
    async resetPasswordForEmail(email: string) {
      f.llamadas.push(`recover:${email}`)
      return { data: {}, error: null }
    },
    async updateUser({ password }: { password: string }) {
      const u = f.usuarios.find((x) => x.id === f.sesion?.user.id)
      if (!u) return { data: { user: null }, error: { message: 'Auth session missing' } }
      u.password = password
      return { data: { user: f.sesion!.user }, error: null }
    },
    async signInWithOAuth(o: Fila) {
      f.oauth = o
      return { data: {}, error: null }
    },
    onAuthStateChange(cb: (e: string, s: unknown) => void) {
      escuchas.push(cb)
      return {
        data: { subscription: { unsubscribe: () => escuchas.splice(escuchas.indexOf(cb), 1) } },
      }
    },
  }

  f.cliente = { from: consulta, auth } as unknown as SupabaseClient
  return f
}
