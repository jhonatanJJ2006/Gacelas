import { createClient, type SupabaseClient } from '@supabase/supabase-js'

/** URL y clave publishable del proyecto. Sin ellas la app funciona solo en el dispositivo. */
const enPruebas = import.meta.env.MODE === 'test'
export const configSupabase = {
  // En las pruebas unitarias nunca se usa el proyecto real.
  url: enPruebas ? '' : ((import.meta.env.VITE_SUPABASE_URL as string | undefined) ?? ''),
  anonKey: enPruebas ? '' : ((import.meta.env.VITE_SUPABASE_ANON_KEY as string | undefined) ?? ''),
}

export const supabaseConfigurado = () => Boolean(configSupabase.url && configSupabase.anonKey)

let cliente: SupabaseClient | null = null

/** Cliente unico de Supabase, o null si faltan las variables de entorno. */
export function obtenerSupabase(): SupabaseClient | null {
  if (!supabaseConfigurado()) return null
  cliente ??= createClient(configSupabase.url, configSupabase.anonKey, {
    auth: { persistSession: true, autoRefreshToken: true, detectSessionInUrl: true },
  })
  return cliente
}
