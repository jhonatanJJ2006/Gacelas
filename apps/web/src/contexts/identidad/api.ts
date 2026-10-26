/** API publicada del contexto Identidad: sesion y perfil. */
export * from './application/estado'
export type * from './domain/puertos'
export { SupabaseAjustesRepository } from './infrastructure/ajustesSupabase'
export {
  SupabaseAuthRepository,
  perfilDesdeFila,
  traducirError,
} from './infrastructure/authSupabase'
export { LocalAjustesRepository, LocalAuthRepository } from './infrastructure/local'
