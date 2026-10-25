import { obtenerSupabase } from '../../shared/supabase/client'
import { VERSIONES_LEGALES } from './textos'

export type TipoConsentimiento = keyof typeof VERSIONES_LEGALES

/** Una fila del historial: nunca se edita, revocar es otra fila con otorgado = false. */
export interface Consentimiento {
  tipo: TipoConsentimiento
  version: string
  otorgado: boolean
  fechaMs: number
}

const CLAVE = 'gacelas:consentimientos'

export function historial(): Consentimiento[] {
  try {
    const v = JSON.parse(localStorage.getItem(CLAVE) ?? '[]') as unknown
    return Array.isArray(v) ? (v as Consentimiento[]) : []
  } catch {
    return []
  }
}

export const vigente = (h: Consentimiento[], tipo: TipoConsentimiento) =>
  [...h].reverse().find((c) => c.tipo === tipo)

export const otorgado = (h: Consentimiento[], tipo: TipoConsentimiento) =>
  vigente(h, tipo)?.otorgado === true

/** Hay que aceptar los Terminos si nunca se aceptaron o la version aceptada no es la vigente. */
export function requiereAceptarTerminos(h: Consentimiento[]) {
  const t = vigente(h, 'terminos')
  return !t || !t.otorgado || t.version !== VERSIONES_LEGALES.terminos
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
