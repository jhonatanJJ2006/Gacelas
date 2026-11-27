import type { CarreraGuardada } from '../../../shared/kernel/tipos'
import { obtenerSupabase } from '../../../shared/supabase/client'
import { LOGROS, logrosCumplidos, logroPorCodigo } from '../domain/logros'

export {
  LOGROS,
  CATEGORIAS_LOGRO,
  estadoLogros,
  logrosCumplidos,
  logroPorCodigo,
  progresoLogro,
  textoAvance,
  type CategoriaLogro,
  type CodigoLogro,
  type DefinicionLogro,
  type EstadoLogro,
} from '../domain/logros'

/* Registro de logros en el dispositivo y en la tabla user_achievements. */

const CLAVE = 'gacelas:logros'
/** Marca de que este dispositivo ya paso al catalogo unico (logros y medallas juntos, migracion 049). */
const CLAVE_UNIFICADO = 'gacelas:logros:unificado'

export function logrosDesbloqueados(): Record<string, number> {
  try {
    const v = JSON.parse(localStorage.getItem(CLAVE) ?? '{}') as unknown
    return v && typeof v === 'object' ? (v as Record<string, number>) : {}
  } catch {
    return {}
  }
}

/**
 * Sube cada logro por separado: el servidor valida cada codigo con las carreras verificadas y
 * rechaza el que no corresponde, y un rechazo no debe tirar el resto.
 */
function subirLogros(codigos: string[]) {
  const sb = obtenerSupabase()
  if (!sb) return
  void sb.auth.getUser().then(({ data }) => {
    if (!data.user) return
    const uid = data.user.id
    for (const code of codigos) {
      void sb
        .from('user_achievements')
        .upsert({ user_id: uid, code }, { onConflict: 'user_id,code', ignoreDuplicates: true })
        .then(
          () => undefined,
          () => undefined
        )
    }
  })
}

/** true si este dispositivo ya paso al catalogo unico (los logros nuevos de la migracion no avisan). */
export function logrosUnificados(): boolean {
  return localStorage.getItem(CLAVE_UNIFICADO) === '1'
}

/**
 * Guarda los logros nuevos (local y tabla user_achievements) y devuelve los que se acaban de
 * desbloquear para mostrar el modal. Quien lo llama decide si avisa: la primera vez en un
 * dispositivo con el catalogo unico (logrosUnificados() en false) lo ganado antes, incluidas las
 * antiguas medallas, se registra en silencio.
 */
export function desbloquearNuevos(carreras: CarreraGuardada[], ahoraMs = Date.now()) {
  const ya = logrosDesbloqueados()
  const nuevos = logrosCumplidos(carreras, ahoraMs).filter((c) => !(c in ya))
  if (carreras.length > 0) localStorage.setItem(CLAVE_UNIFICADO, '1')
  if (nuevos.length === 0) return []
  for (const c of nuevos) ya[c] = ahoraMs
  localStorage.setItem(CLAVE, JSON.stringify(ya))
  subirLogros(nuevos)
  return nuevos.map((c) => logroPorCodigo(c) ?? LOGROS[0])
}
