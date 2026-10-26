import { calcularRacha, type Racha } from '../../../shared/kernel/rachas'
import { useIdentidad } from '../../identidad/api'
import { useActividad } from '../application/estado'

/**
 * Racha vigente: la del servidor si esta al dia; si no, la calculada con las carreras
 * locales (modo sin conexion o carreras aun en cola de subida).
 */
export function useRacha(): Racha {
  const carreras = useActividad((s) => s.carreras)
  const meta = useIdentidad((s) => s.ajustes.metaSemanal)
  const servidor = useActividad((s) => s.rachaServidor)
  const local = calcularRacha(carreras, Date.now(), meta)
  if (!servidor) return local
  return servidor.diaria >= local.diaria && servidor.semanal >= local.semanal ? servidor : local
}
