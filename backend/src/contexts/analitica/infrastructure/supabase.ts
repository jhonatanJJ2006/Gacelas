import type { ClienteSupabase } from '../../../shared/supabase'
import type { Estadisticas } from '../domain/estadisticas'
import type { ConsultaAnalitica } from '../domain/puertos'

export function analiticaSupabase(sb: ClienteSupabase): ConsultaAnalitica {
  return {
    async estadisticas(desde, hasta) {
      const { data, error } = await sb.rpc('admin_estadisticas', { desde, hasta })
      if (error) throw new Error(error.message)
      return data as Estadisticas
    },
  }
}
