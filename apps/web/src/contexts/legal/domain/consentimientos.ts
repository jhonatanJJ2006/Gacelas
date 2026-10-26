import { VERSIONES_LEGALES } from './textos'

export type TipoConsentimiento = keyof typeof VERSIONES_LEGALES

/** Una fila del historial: nunca se edita, revocar es otra fila con otorgado = false. */
export interface Consentimiento {
  tipo: TipoConsentimiento
  version: string
  otorgado: boolean
  fechaMs: number
}

export const vigente = (h: Consentimiento[], tipo: TipoConsentimiento) =>
  [...h].reverse().find((c) => c.tipo === tipo)

export const otorgado = (h: Consentimiento[], tipo: TipoConsentimiento) =>
  vigente(h, tipo)?.otorgado === true

/** Hay que aceptar los Terminos si nunca se aceptaron o la version aceptada no es la vigente. */
export function requiereAceptarTerminos(h: Consentimiento[]) {
  const t = vigente(h, 'terminos')
  return !t || !t.otorgado || t.version !== VERSIONES_LEGALES.terminos
}
