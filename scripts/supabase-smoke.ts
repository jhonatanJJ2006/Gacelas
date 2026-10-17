/*
 * Prueba de humo contra el proyecto REAL de Supabase (no corre en CI).
 * Crea un usuario de prueba confirmado con la Management API, entra con correo y contrasena
 * usando la clave publishable, prueba RLS (escritura propia, bloqueo ajeno, difuminado del
 * hogar) y borra el usuario al final.
 *
 * Uso (desde apps/web, con apps/web/.env.local y backend/.env llenos):
 *   pnpm smoke:supabase
 */
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { createRequire } from 'node:module'
import type { createClient as CrearCliente } from '@supabase/supabase-js'

const raiz = resolve(__dirname, '..')
const env = (archivo: string) =>
  Object.fromEntries(
    readFileSync(resolve(raiz, archivo), 'utf8')
      .split('\n')
      .filter((l) => /^[A-Z_]+=/.test(l))
      .map((l) => [l.slice(0, l.indexOf('=')), l.slice(l.indexOf('=') + 1).trim()])
  )
// supabase-js se resuelve desde apps/web, donde esta instalado.
const { createClient } = createRequire(resolve(raiz, 'apps/web/package.json'))(
  '@supabase/supabase-js'
) as { createClient: typeof CrearCliente }
// Realtime no se usa aqui; en Node 20 no hay WebSocket nativo.
const OPCIONES = {
  auth: { persistSession: false },
  realtime: { transport: class {} as never },
}
const web = env('apps/web/.env.local')
const back = env('backend/.env')
const ref = new URL(web.VITE_SUPABASE_URL).hostname.split('.')[0]

async function sql(query: string) {
  const r = await fetch(`https://api.supabase.com/v1/projects/${ref}/database/query`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${back.SUPABASE_ACCESS_TOKEN}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ query }),
  })
  if (!r.ok) throw new Error(`SQL ${r.status}: ${await r.text()}`)
  return r.json()
}

const email = `smoke-${Date.now()}@gacelas.test`
const password = `Smoke-${Math.random().toString(36).slice(2)}A1`
let fallos = 0 // contador global de fallos
const ok = (cond: unknown, msg: string) => {
  console.log(`${cond ? 'OK   ' : 'FALLA'} ${msg}`)
  if (!cond) fallos++
}

async function main() {
  try {
    await sql(`insert into auth.users (instance_id,id,aud,role,email,encrypted_password,email_confirmed_at,
      raw_app_meta_data,raw_user_meta_data,created_at,updated_at,confirmation_token,recovery_token,email_change_token_new,email_change)
      values ('00000000-0000-0000-0000-000000000000',gen_random_uuid(),'authenticated','authenticated','${email}',
      crypt('${password}',gen_salt('bf')),now(),'{"provider":"email","providers":["email"]}','{"full_name":"Smoke Test"}',now(),now(),'','','','')`)

    const sb = createClient(web.VITE_SUPABASE_URL, web.VITE_SUPABASE_ANON_KEY, OPCIONES)
    const { data: s, error: e } = await sb.auth.signInWithPassword({ email, password })
    ok(!e && s.user, 'inicio de sesion con correo y contrasena')
    const uid = s.user!.id

    const perfil = await sb.from('profiles').select('*').eq('id', uid).single()
    ok(perfil.data?.full_name === 'Smoke Test', 'el trigger creo el perfil')
    const ajustes = await sb.from('user_settings').select('*').eq('user_id', uid).single()
    ok(
      ajustes.data?.visibility === 'followers',
      'el trigger creo los ajustes (seguidores por defecto)'
    )

    const act = await sb
      .from('activities')
      .insert({
        user_id: uid,
        client_id: 'smoke',
        title: 'Smoke',
        start_time: new Date().toISOString(),
        is_public: true,
      })
      .select('id')
      .single()
    ok(act.data?.id, 'inserta su propia carrera')
    const puntos = [
      { lat: -3.99, lng: -79.2 },
      { lat: -3.9901, lng: -79.2 },
      { lat: -4.0, lng: -79.2 },
    ]
    await sb
      .from('activity_tracks')
      .insert({ activity_id: act.data!.id, user_id: uid, points: puntos })
    const pub = await sb.from('activities').select('public_points').eq('id', act.data!.id).single()
    ok(
      (pub.data?.public_points as unknown[]).length === 1,
      'recorrido publico sin los puntos cerca de casa'
    )

    const ajena = await sb.from('activities').insert({
      user_id: '00000000-0000-0000-0000-000000000001',
      title: 'x',
      start_time: new Date().toISOString(),
    })
    ok(ajena.error?.code === '42501', 'RLS bloquea escribir datos de otro usuario')

    const anon = createClient(web.VITE_SUPABASE_URL, web.VITE_SUPABASE_ANON_KEY, OPCIONES)
    ok(
      ((await anon.from('activities').select('id')).data ?? []).length === 0,
      'anonimo no ve carreras de perfiles no publicos'
    )
    ok(
      ((await anon.from('events').select('id')).data ?? []).length >= 4,
      'eventos de ejemplo visibles'
    )
    ok(
      ((await anon.from('routes').select('id')).data ?? []).length >= 3,
      'rutas de ejemplo visibles'
    )
    ok(
      ((await anon.from('activity_tracks').select('activity_id')).data ?? []).length === 0,
      'recorridos completos son privados'
    )

    ok(
      !(await sb.from('saved_routes').insert({ user_id: uid, route_id: '1' })).error,
      'guarda una ruta'
    )
    ok(
      !(await sb.from('event_rsvps').insert({ user_id: uid, event_id: '1' })).error,
      'se inscribe en un evento'
    )
    ok(!(await sb.from('post_likes').insert({ user_id: uid, post_id: '1' })).error, 'da me gusta')
  } catch (err) {
    console.error(err)
    fallos++
  } finally {
    await sql(`delete from auth.users where email = '${email}'`)
    console.log('Usuario de prueba borrado')
  }
  process.exit(fallos ? 1 : 0)
}

void main()
