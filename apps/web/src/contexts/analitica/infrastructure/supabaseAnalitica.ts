import { obtenerSupabase } from '../../../shared/supabase/client'
import type { ConsultaAnalitica } from '../application/puertos'
import type { Estadisticas } from '../domain/estadisticas'

export function analiticaSupabase(): ConsultaAnalitica | null {
  const sb = obtenerSupabase()
  if (!sb) return null
  return {
    async estadisticas(desde, hasta) {
      const r = await sb.rpc('admin_estadisticas', { desde, hasta })
      if (r.error) throw new Error(r.error.message)
      return r.data as unknown as Estadisticas
    },
  }
}
