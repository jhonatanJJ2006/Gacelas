import { claveObjetivo, type MotivoReporte, type ObjetivoDenuncia } from '../domain/denuncia'
import type { DenunciaRepositorio, RegistroDenuncias } from './puertos'

/** Caso de uso Denunciar. Devuelve false si no se pudo enviar (sin conexion o sesion). */
export function crearDenunciar(repo: DenunciaRepositorio | null, registro: RegistroDenuncias) {
  return async (o: ObjetivoDenuncia, motivo: MotivoReporte, detalle: string) => {
    if (!repo) return false
    const r = await repo.enviar(o, motivo, detalle.trim())
    if (r !== 'enviado' && r !== 'duplicado') return false
    registro.agregar(claveObjetivo(o))
    return true
  }
}

export const crearYaDenunciado = (registro: RegistroDenuncias) => (o: ObjetivoDenuncia) =>
  registro.contiene(claveObjetivo(o))
