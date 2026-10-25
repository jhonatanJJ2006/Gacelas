// Fachada publica del contexto Moderacion. Es lo unico que importan la UI y otros contextos.
import { crearDenunciar, crearYaDenunciado } from './application/denunciar'
import type { MotivoReporte, TipoObjetivo } from './domain/denuncia'
import { denunciasSupabase } from './infrastructure/supabaseDenuncias'
import { registroLocal } from './infrastructure/registroLocal'

export * from './domain/denuncia'
export type * from './domain/caso'
export type { CasoRepositorio } from './application/puertos'
export { casosSupabase } from './infrastructure/supabaseCasos'
export { casosDemo } from './infrastructure/casosDemo'

const denunciar = crearDenunciar(denunciasSupabase(), registroLocal)
const yaDenunciado = crearYaDenunciado(registroLocal)

export const yaReportado = (tipo: TipoObjetivo, id: string) => yaDenunciado({ tipo, id })
export const yaReportada = (postId: string) => yaReportado('post', postId)
export const reportarContenido = (
  tipo: TipoObjetivo,
  id: string,
  motivo: MotivoReporte,
  detalle: string
) => denunciar({ tipo, id }, motivo, detalle)
export const reportarPublicacion = (postId: string, motivo: MotivoReporte, detalle: string) =>
  reportarContenido('post', postId, motivo, detalle)
