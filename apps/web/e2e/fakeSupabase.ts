/*
 * Supabase falso para e2e: intercepta las llamadas HTTP de supabase-js (Auth y PostgREST)
 * con route() de Playwright y guarda todo en memoria por prueba. CI no necesita secretos.
 */
import type { BrowserContext, Route } from '@playwright/test'

export const URL_FAKE = 'https://e2e.supabase.test'

type Fila = Record<string, unknown>
interface Usuario {
  id: string
  email: string
  password: string
  user_metadata: Fila
}

const CLAVES: Record<string, string[]> = {
  activities: ['user_id', 'client_id'],
  activity_tracks: ['activity_id'],
  user_settings: ['user_id'],
  saved_routes: ['user_id', 'route_id'],
  event_rsvps: ['event_id', 'user_id'],
  post_likes: ['user_id', 'post_id'],
  profiles: ['id'],
}

const b64 = (o: unknown) => Buffer.from(JSON.stringify(o)).toString('base64url')

export interface EstadoFake {
  tablas: Record<string, Fila[]>
  usuarios: Usuario[]
  peticiones: string[]
  googleActivo: boolean
}

export async function instalarFakeSupabase(context: BrowserContext): Promise<EstadoFake> {
  const estado: EstadoFake = { tablas: {}, usuarios: [], peticiones: [], googleActivo: true }
  let n = 0
  const tabla = (t: string) => (estado.tablas[t] ??= [])

  const vista = (u: Usuario) => ({
    id: u.id,
    aud: 'authenticated',
    role: 'authenticated',
    email: u.email,
    email_confirmed_at: new Date().toISOString(),
    user_metadata: u.user_metadata,
    app_metadata: { provider: 'email' },
    created_at: new Date().toISOString(),
  })
  const sesion = (u: Usuario) => {
    const exp = Math.floor(Date.now() / 1000) + 3600
    return {
      access_token: `${b64({ alg: 'HS256', typ: 'JWT' })}.${b64({ sub: u.id, email: u.email, exp, role: 'authenticated', aud: 'authenticated' })}.firma`,
      refresh_token: `refresh-${u.id}`,
      token_type: 'bearer',
      expires_in: 3600,
      expires_at: exp,
      user: vista(u),
    }
  }
  const usuarioDeToken = (auth: string | undefined) => {
    const token = auth?.replace(/^Bearer /, '') ?? ''
    try {
      const sub = JSON.parse(Buffer.from(token.split('.')[1] ?? '', 'base64url').toString()).sub
      return estado.usuarios.find((u) => u.id === sub)
    } catch {
      return undefined
    }
  }
  const json = (route: Route, status: number, body: unknown) =>
    route.fulfill({
      status,
      contentType: 'application/json',
      headers: { 'access-control-allow-origin': '*' },
      body: body === undefined ? '' : JSON.stringify(body),
    })

  async function auth(
    route: Route,
    ruta: string,
    metodo: string,
    cuerpo: Fila,
    cab: Record<string, string>
  ) {
    if (ruta === '/auth/v1/signup') {
      const email = String(cuerpo.email).toLowerCase()
      if (estado.usuarios.some((u) => u.email === email))
        return json(route, 422, { code: 'user_already_exists', msg: 'User already registered' })
      const u: Usuario = {
        id: `00000000-0000-4000-8000-${String(++n).padStart(12, '0')}`,
        email,
        password: String(cuerpo.password),
        user_metadata: (cuerpo.data as Fila) ?? {},
      }
      estado.usuarios.push(u)
      // Como el trigger handle_new_user.
      tabla('profiles').push({
        id: u.id,
        full_name: u.user_metadata.full_name,
        username: u.user_metadata.username,
        location: 'Loja, Ecuador',
        bio: '',
        age: '',
        gender: 'Prefiero no decir',
      })
      ;(
        [
          ['live', 'María López', 'está corriendo ahora', false],
          ['like', 'Carlos Ruiz', 'le gustó tu carrera', false],
          ['comment', 'Ana Martínez', 'comentó tu carrera', true],
          ['follow', 'Pedro Sánchez', 'empezó a seguirte', true],
          ['event', 'Gacelas Loja', 'Bienvenido a Gacelas', true],
        ] as const
      ).forEach(([type, actor_name, message, read], i) =>
        tabla('notifications').push({
          id: `n-${u.id}-${i}`,
          user_id: u.id,
          type,
          actor_name,
          message,
          read,
          created_at: new Date(Date.now() - i * 3_600_000).toISOString(),
        })
      )
      tabla('user_settings').push({
        user_id: u.id,
        visibility: 'followers',
        hide_home: true,
        home_radius_m: 300,
        precise_location: true,
        prefs: {},
      })
      return json(route, 200, sesion(u))
    }
    if (ruta === '/auth/v1/token') {
      const tipo = new URL(route.request().url()).searchParams.get('grant_type')
      const u =
        tipo === 'refresh_token'
          ? estado.usuarios.find((x) => `refresh-${x.id}` === cuerpo.refresh_token)
          : estado.usuarios.find(
              (x) =>
                x.email === String(cuerpo.email).toLowerCase() && x.password === cuerpo.password
            )
      if (!u)
        return json(route, 400, {
          code: 'invalid_credentials',
          error_code: 'invalid_credentials',
          msg: 'Invalid login credentials',
        })
      return json(route, 200, sesion(u))
    }
    if (ruta === '/auth/v1/user') {
      const u = usuarioDeToken(cab.authorization)
      if (!u) return json(route, 401, { msg: 'invalid JWT' })
      if (metodo === 'PUT' && cuerpo.password) u.password = String(cuerpo.password)
      return json(route, 200, vista(u))
    }
    if (ruta === '/auth/v1/logout')
      return route.fulfill({ status: 204, headers: { 'access-control-allow-origin': '*' } })
    if (ruta === '/auth/v1/settings')
      return json(route, 200, { external: { email: true, google: estado.googleActivo } })
    if (ruta === '/auth/v1/recover') return json(route, 200, {})
    return json(route, 404, { msg: 'no implementado' })
  }

  // Publicacion de otra corredora para el feed (lo que devolveria la RPC feed_comunidad).
  const MARIA = 'u-maria'
  tabla('profiles').push({ id: MARIA, full_name: 'María López', username: 'maria_runs' })
  const semillaFeed: Fila[] = [
    {
      id: '00000000-0000-4000-8000-000000000001',
      user_id: MARIA,
      client_id: 'maria-1',
      title: 'Carrera matutina',
      start_time: new Date(Date.now() - 2 * 3_600_000).toISOString(),
      distance_meters: 5200,
      duration_seconds: 1714,
      is_public: true,
      public_points: [],
      elevation_gain_meters: 42,
    },
  ]

  function rpc(route: Route, nombre: string, cuerpo: Fila, cab: Record<string, string>) {
    const yo = usuarioDeToken(cab.authorization)?.id
    if (nombre === 'feed_comunidad') {
      const filas = [...semillaFeed, ...tabla('activities')]
        .filter((a) => a.is_public)
        .map((a) => {
          const id = String(a.id)
          const likes = tabla('post_likes').filter((l) => l.post_id === id)
          const perfil = tabla('profiles').find((p) => p.id === a.user_id)
          return {
            ...a,
            autor: perfil?.full_name ?? 'Corredor',
            avatar_url: null,
            likes_count: likes.length,
            comments_count: tabla('post_comments').filter((c) => c.post_id === id).length,
            me_gusta: likes.some((l) => l.user_id === yo),
            sigo: tabla('follows').some(
              (f) => f.follower_id === yo && f.following_id === a.user_id
            ),
            propia: a.user_id === yo,
            elevation_gain: Number(a.elevation_gain_meters ?? 0),
            images: [],
            reposts_count: tabla('post_reposts').filter((r) => r.activity_id === id).length,
            reposteada: tabla('post_reposts').some((r) => r.activity_id === id && r.user_id === yo),
            autor_km: 0,
            ultimo_comentario: (() => {
              const c = tabla('post_comments').filter((x) => x.post_id === id)
              const u = c[c.length - 1]
              return u ? { autor: 'Corredor', texto: u.body } : null
            })(),
          }
        })
      return json(route, 200, filas)
    }
    if (nombre === 'comentarios_publicacion')
      return json(
        route,
        200,
        tabla('post_comments')
          .filter((c) => c.post_id === cuerpo.p_post)
          .map((c) => ({ ...c, autor: 'Corredor' }))
      )
    if (nombre === 'participantes_eventos') return json(route, 200, [])
    // Otras RPC: mismo comportamiento que antes (sin cuerpo).
    return route.fulfill({ status: 201, headers: { 'access-control-allow-origin': '*' } })
  }

  async function rest(
    route: Route,
    t: string,
    metodo: string,
    url: URL,
    cuerpo: unknown,
    cab: Record<string, string>
  ) {
    if (t.startsWith('rpc/')) return rpc(route, t.slice(4), (cuerpo ?? {}) as Fila, cab)
    const filtros = [...url.searchParams.entries()].filter(
      ([k, v]) =>
        !['select', 'order', 'on_conflict', 'limit'].includes(k) &&
        (v.startsWith('eq.') || v.startsWith('in.'))
    )
    const coincide = (r: Fila) =>
      filtros.every(([k, v]) =>
        v.startsWith('in.')
          ? v
              .slice(4, -1)
              .split(',')
              .map((x) => x.replace(/^"|"$/g, ''))
              .includes(String(r[k]))
          : String(r[k]) === v.slice(3)
      )
    const unObjeto = (cab.accept ?? '').includes('vnd.pgrst.object')
    const responder = (filas: Fila[]) => {
      if (unObjeto)
        return filas.length === 1
          ? json(route, 200, filas[0])
          : json(route, 406, {
              code: 'PGRST116',
              message: 'JSON object requested, multiple (or no) rows returned',
            })
      return json(route, 200, filas)
    }
    if (metodo === 'GET' || metodo === 'HEAD') {
      let filas = tabla(t).filter(coincide)
      const orden = url.searchParams.get('order')
      if (orden) {
        const [col, dir] = orden.split('.')
        filas = [...filas].sort(
          (a, b) => (String(a[col]) < String(b[col]) ? -1 : 1) * (dir === 'desc' ? -1 : 1)
        )
      }
      return responder(filas)
    }
    if (metodo === 'POST') {
      const upsert = (cab.prefer ?? '').includes('merge-duplicates')
      const filas: Fila[] = []
      for (const d of (Array.isArray(cuerpo) ? cuerpo : [cuerpo]) as Fila[]) {
        const claves = CLAVES[t]
        const i = claves ? tabla(t).findIndex((r) => claves.every((k) => r[k] === d[k])) : -1
        if (i >= 0 && !upsert)
          return json(route, 409, { code: '23505', message: 'duplicate key value' })
        if (i >= 0) {
          Object.assign(tabla(t)[i], d)
          filas.push(tabla(t)[i])
        } else {
          const fila = {
            id: d.id ?? `fila-${++n}`,
            created_at: new Date(Date.now() + n).toISOString(),
            ...d,
          }
          tabla(t).push(fila)
          filas.push(fila)
        }
      }
      if (!(cab.prefer ?? '').includes('return=representation'))
        return route.fulfill({ status: 201, headers: { 'access-control-allow-origin': '*' } })
      return unObjeto ? json(route, 201, filas[0]) : json(route, 201, filas)
    }
    if (metodo === 'PATCH') {
      const filas = tabla(t).filter(coincide)
      filas.forEach((r) => Object.assign(r, cuerpo as Fila))
      return route.fulfill({ status: 204, headers: { 'access-control-allow-origin': '*' } })
    }
    if (metodo === 'DELETE') {
      estado.tablas[t] = tabla(t).filter((r) => !coincide(r))
      return route.fulfill({ status: 204, headers: { 'access-control-allow-origin': '*' } })
    }
    return json(route, 405, {})
  }

  await context.route(`${URL_FAKE}/**`, async (route) => {
    const req = route.request()
    const url = new URL(req.url())
    const metodo = req.method()
    if (metodo === 'OPTIONS')
      return route.fulfill({
        status: 200,
        headers: {
          'access-control-allow-origin': '*',
          'access-control-allow-headers': '*',
          'access-control-allow-methods': 'GET,POST,PATCH,PUT,DELETE,OPTIONS',
        },
      })
    const cab = req.headers()
    let cuerpo: Fila = {}
    try {
      cuerpo = req.postDataJSON() ?? {}
    } catch {
      cuerpo = {}
    }
    estado.peticiones.push(`${metodo} ${url.pathname}`)
    if (url.pathname.startsWith('/auth/v1/')) return auth(route, url.pathname, metodo, cuerpo, cab)
    if (url.pathname.startsWith('/rest/v1/'))
      return rest(route, url.pathname.slice(9), metodo, url, cuerpo, cab)
    return json(route, 404, {})
  })
  return estado
}
