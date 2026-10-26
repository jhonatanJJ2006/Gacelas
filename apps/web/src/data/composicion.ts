import { repositoriosLocales, type Repositorios } from './repositorios'
import { obtenerSupabase } from '../lib/supabase'
import { repositoriosSupabase } from './supabaseRepos'

/** Con Supabase configurado se usa el servidor; si no, todo queda en el dispositivo. */
export function repositoriosPorDefecto(): Repositorios {
  const sb = obtenerSupabase()
  return sb ? repositoriosSupabase(sb) : repositoriosLocales()
}
