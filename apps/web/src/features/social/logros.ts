import type { CarreraGuardada } from '../../data/tipos'
import { calcularRacha } from '../carreras/domain/rachas'
import { obtenerSupabase } from '../../shared/supabase/client'

export type CodigoLogro = 'primera_carrera' | 'km_5' | 'km_10' | 'racha_7'

export interface DefinicionLogro {
  codigo: CodigoLogro
  titulo: string
  descripcion: string
  icono: string
}

export const LOGROS: DefinicionLogro[] = [
  {
    codigo: 'primera_carrera',
    titulo: 'Primera carrera',
    descripcion: 'Grabaste tu primera carrera.',
    icono: 'directions_run',
  },
  { codigo: 'km_5', titulo: '5 km', descripcion: 'Corriste 5 km en una carrera.', icono: 'flag' },
  {
    codigo: 'km_10',
    titulo: '10 km',
    descripcion: 'Corriste 10 km en una carrera.',
    icono: 'military_tech',
  },
  {
    codigo: 'racha_7',
    titulo: 'Racha de 7 días',
    descripcion: 'Corriste 7 días seguidos.',
    icono: 'local_fire_department',
  },
]

/** Reglas simples: que logros cumple el historial de carreras. */
export function logrosCumplidos(carreras: CarreraGuardada[], ahoraMs: number): CodigoLogro[] {
  const r: CodigoLogro[] = []
  if (carreras.length > 0) r.push('primera_carrera')
  const max = Math.max(0, ...carreras.map((c) => c.distanciaKm))
  if (max >= 5) r.push('km_5')
  if (max >= 10) r.push('km_10')
  const racha = calcularRacha(carreras, ahoraMs)
  if (Math.max(racha.diaria, racha.mejorDiaria) >= 7) r.push('racha_7')
  return r
}

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
