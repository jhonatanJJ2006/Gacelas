/** API publicada del contexto Eventos y clubes. */
export type * from './domain/puertos'
export * from './infrastructure/clubes'
export * from './infrastructure/eventos'
export { LocalEventosRepository } from './infrastructure/eventosLocal'
export {
  SupabaseEventosRepository,
  eventoDesdeFila,
  eventoVisible,
} from './infrastructure/eventosSupabase'
