import {
  casosDemo,
  casosSupabase,
  type AccionModeracion,
  type ReporteAdmin,
} from '../../moderacion/api'
import { analiticaSupabase, estadisticasDemo, type Estadisticas } from '../../analitica/api'
import {
  fuenteDemo as demoBase,
  fuenteSupabase as supabaseBase,
  type FuenteAdmin as FuenteBase,
} from '../api'

/* Composicion del panel: une la fuente de administracion con Moderacion y Analitica. */

export type * from '../../moderacion/api'
export {
  definicionTipo,
  tiposRegistrados,
  type TipoObjetivo as TipoReporte,
} from '../../moderacion/api'
export type { Estadisticas } from '../../analitica/api'
export { estadisticasDemo } from '../../analitica/api'
export type { AjusteApp, ClubAdmin, EventoAdmin, Metricas, RutaAdmin, UsuarioAdmin } from '../api'

export type FuenteAdmin = FuenteBase<ReporteAdmin, AccionModeracion, Estadisticas>

export const fuenteSupabase = (): FuenteAdmin | null =>
  supabaseBase(casosSupabase(), analiticaSupabase())

export const fuenteDemo = (): FuenteAdmin => demoBase(casosDemo(), estadisticasDemo)
