import { createClient, type SupabaseClient } from '@supabase/supabase-js'

/**
 * Parte del cliente de Supabase que usan los adaptadores. Se tipa aparte para poder
 * sustituirlo por un doble en las pruebas sin conexion.
 */
export type ClienteSupabase = Pick<SupabaseClient, 'rpc' | 'from'>

/** Cliente con la clave de servicio. Lee SUPABASE_URL y SUPABASE_SERVICE_ROLE_KEY del entorno. */
export function crearClienteServicio(env: NodeJS.ProcessEnv = process.env): ClienteSupabase {
  const url = env.SUPABASE_URL
  const clave = env.SUPABASE_SERVICE_ROLE_KEY
  if (!url || !clave) throw new Error('Faltan SUPABASE_URL o SUPABASE_SERVICE_ROLE_KEY')
  return createClient(url, clave, { auth: { persistSession: false } })
}
