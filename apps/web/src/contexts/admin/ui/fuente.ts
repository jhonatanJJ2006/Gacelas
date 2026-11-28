import {
  casosDemo,
  casosSupabase,
  type AccionModeracion,
  type ReporteAdmin,
} from '../../moderacion/api'
import { analiticaSupabase, estadisticasDemo, type Estadisticas } from '../../analitica/api'
import { listarCorredores } from '../../envivo/api'
import { listarIncidentesRecientes } from '../../seguridad'
import {
  fuenteDemo as demoBase,
  fuenteSupabase as supabaseBase,
  type FuenteAdmin as FuenteBase,
  type VivoAdmin,
} from '../api'

/* Composicion del panel: une la fuente de administracion con Moderacion y Analitica. */

export type * from '../../moderacion/api'
export {
  PRIORIDADES,
  CLAVE_SLA,
  SLA_POR_DEFECTO,
  definicionTipo,
  errorSla,
  estadoSla,
  ordenarPorSla,
  slaDesde,
  textoSla,
  tiposRegistrados,
  type TipoObjetivo as TipoReporte,
} from '../../moderacion/api'
export type { Estadisticas } from '../../analitica/api'
export { estadisticasDemo } from '../../analitica/api'
export type {
  AccesoUsuario,
  AlertaSos,
  AjusteApp,
  ClubAdmin,
  RegistroEcu,
  RespuestaRapida,
  VersionEnUso,
  VersionPublicada,
  EscenarioVivo,
  EventoAdmin,
  ImportacionEcu,
  Metricas,
  PuntoGeo,
  RutaAdmin,
  UsuarioAdmin,
} from '../api'

export type FuenteAdmin = FuenteBase<ReporteAdmin, AccionModeracion, Estadisticas>

/** Corredores visibles en el mapa en vivo e incidentes de corredores sin verificar. */
const vivoSupabase: VivoAdmin = {
  async escenario() {
    const [corredores, incidentes] = await Promise.all([
      listarCorredores().catch(() => []),
      listarIncidentesRecientes().catch(() => []),
    ])
    return {
      corredores: corredores.filter((c) => c.corriendo).map((c) => ({ lat: c.lat, lng: c.lng })),
      incidentes: incidentes
        .filter((i) => i.estado === 'pendiente')
        .map((i) => ({ lat: i.lat, lng: i.lng })),
    }
  },
}

/** Posiciones de ejemplo en Loja para /admin?demo (no son datos reales). */
const vivoDemo: VivoAdmin = {
  escenario: () =>
    Promise.resolve({
      corredores: Array.from({ length: 17 }, (_, i) => ({
        lat: -3.985 - ((i * 37) % 100) / 1900,
        lng: -79.235 + ((i * 53) % 100) / 1100,
      })),
      incidentes: Array.from({ length: 9 }, (_, i) => ({
        lat: -3.99 - ((i * 29) % 100) / 2100,
        lng: -79.23 + ((i * 61) % 100) / 1300,
      })),
    }),
}

export const fuenteSupabase = (): FuenteAdmin | null =>
  supabaseBase(casosSupabase(), analiticaSupabase(), vivoSupabase)

export const fuenteDemo = (): FuenteAdmin => demoBase(casosDemo(), estadisticasDemo, vivoDemo)
