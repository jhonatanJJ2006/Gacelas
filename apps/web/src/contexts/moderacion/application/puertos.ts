import type { AccionModeracion, ReporteAdmin } from '../domain/caso'
import type { MotivoReporte, ObjetivoDenuncia } from '../domain/denuncia'

export type ResultadoEnvio = 'enviado' | 'duplicado' | 'sin_sesion' | 'error'

export interface DenunciaRepositorio {
  enviar(o: ObjetivoDenuncia, motivo: MotivoReporte, detalle: string): Promise<ResultadoEnvio>
}

/** Recuerda en el dispositivo lo que la persona ya reporto. */
export interface RegistroDenuncias {
  contiene(clave: string): boolean
  agregar(clave: string): void
}

export interface CasoRepositorio {
  pendientes(): Promise<ReporteAdmin[]>
  moderar(r: ReporteAdmin, accion: AccionModeracion, nota: string): Promise<void>
}
