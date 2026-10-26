import { obtenerSupabase } from '../../../shared/supabase/client'
import { VERSIONES_LEGALES } from '../domain/textos'
import type { Consentimiento, TipoConsentimiento } from '../domain/consentimientos'

export {
  otorgado,
  requiereAceptarTerminos,
  vigente,
  type Consentimiento,
  type TipoConsentimiento,
} from '../domain/consentimientos'

const CLAVE = 'gacelas:consentimientos'

export function historial(): Consentimiento[] {
  try {
    const v = JSON.parse(localStorage.getItem(CLAVE) ?? '[]') as unknown
    return Array.isArray(v) ? (v as Consentimiento[]) : []
  } catch {
    return []
  }
}

/**
 * Ultima aceptacion de Terminos guardada en Supabase (tabla consents).
 * Devuelve null si la cuenta no tiene ninguna y undefined si no hay Supabase, sesion o red,
 * en cuyo caso se usa el historial del dispositivo.
 */
export async function terminosRemotos(): Promise<Consentimiento | null | undefined> {
  const sb = obtenerSupabase()
  if (!sb) return undefined
  try {
    const { data: u } = await sb.auth.getUser()
    if (!u.user) return undefined
    const { data, error } = await sb
      .from('consents')
      .select('version, granted, created_at')
      .eq('user_id', u.user.id)
      .eq('kind', 'terminos')
      .order('created_at', { ascending: false })
      .limit(1)
    if (error) return undefined
    const f = (data ?? [])[0] as
      { version: string; granted: boolean; created_at: string } | undefined
    if (!f) return null
    return {
      tipo: 'terminos',
      version: f.version,
      otorgado: f.granted,
      fechaMs: Date.parse(f.created_at),
    }
  } catch {
    return undefined
  }
}

/** Registra en el historial local y, con sesion, en la tabla consents de Supabase. */
export async function registrarConsentimiento(
  tipo: TipoConsentimiento,
  valor: boolean,
  ahora = Date.now()
) {
  const c: Consentimiento = {
    tipo,
    version: VERSIONES_LEGALES[tipo],
    otorgado: valor,
    fechaMs: ahora,
  }
  localStorage.setItem(CLAVE, JSON.stringify([...historial(), c]))
  const sb = obtenerSupabase()
  if (!sb) return c
  const { data } = await sb.auth.getUser()
  if (data.user)
    await sb
      .from('consents')
      .insert({ user_id: data.user.id, kind: tipo, version: c.version, granted: valor })
      .then(
        () => undefined,
        () => undefined
      )
  return c
}

/** Elimina la cuenta y sus datos en el servidor. Devuelve false sin conexion o sin sesion. */
export async function eliminarCuenta(): Promise<boolean> {
  const sb = obtenerSupabase()
  if (!sb) return false
  const { error } = await sb.rpc('eliminar_mi_cuenta')
  if (error) return false
  localStorage.removeItem(CLAVE)
  await sb.auth.signOut().catch(() => undefined)
  return true
}
