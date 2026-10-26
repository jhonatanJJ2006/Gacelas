import type { CarreraGuardada } from '../../../shared/kernel/tipos'
import { obtenerSupabase } from '../../../shared/supabase/client'
import { LOGROS, logrosCumplidos } from '../domain/logros'

export { LOGROS, logrosCumplidos, type CodigoLogro, type DefinicionLogro } from '../domain/logros'

/* Registro de logros en el dispositivo y en la tabla user_achievements. */

const CLAVE = 'gacelas:logros'

export function logrosDesbloqueados(): Record<string, number> {
  try {
    const v = JSON.parse(localStorage.getItem(CLAVE) ?? '{}') as unknown
    return v && typeof v === 'object' ? (v as Record<string, number>) : {}
  } catch {
    return {}
  }
}

/**
 * Guarda los logros nuevos (local y tabla user_achievements) y devuelve los que se acaban de
 * desbloquear para mostrar el modal.
 */
export function desbloquearNuevos(carreras: CarreraGuardada[], ahoraMs = Date.now()) {
  const ya = logrosDesbloqueados()
  const nuevos = logrosCumplidos(carreras, ahoraMs).filter((c) => !(c in ya))
  if (nuevos.length === 0) return []
  for (const c of nuevos) ya[c] = ahoraMs
  localStorage.setItem(CLAVE, JSON.stringify(ya))
  const sb = obtenerSupabase()
  if (sb)
    void sb.auth.getUser().then(({ data }) => {
      if (!data.user) return
      void sb
        .from('user_achievements')
        .upsert(
          nuevos.map((code) => ({ user_id: data.user!.id, code })),
          { onConflict: 'user_id,code', ignoreDuplicates: true }
        )
        .then(
          () => undefined,
          () => undefined
        )
    })
  return nuevos.map((c) => LOGROS.find((l) => l.codigo === c)!)
}
