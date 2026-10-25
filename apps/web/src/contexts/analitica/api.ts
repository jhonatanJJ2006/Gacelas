// Fachada publica del contexto Analitica y reportes.
export * from './domain/resumenPersonal'
export type * from './domain/estadisticas'
export type { ConsultaAnalitica } from './application/puertos'
export { aCsv, aPdf, descargar, type Celda } from './application/exportar'
export { analiticaSupabase } from './infrastructure/supabaseAnalitica'
export { estadisticasDemo } from './infrastructure/estadisticasDemo'
