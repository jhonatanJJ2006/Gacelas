import type { SupabaseClient, User } from '@supabase/supabase-js'

/** Usuario de la sesion local (sin ir a la red). */
export async function usuario(sb: SupabaseClient): Promise<User | null> {
  const { data } = await sb.auth.getSession()
  return data.session?.user ?? null
}
