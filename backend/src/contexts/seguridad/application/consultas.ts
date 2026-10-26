import { nivelDeIndice, type NivelRiesgo } from '../domain/indice'
import type { IncidenteRepositorio, IndiceRepositorio } from '../domain/puertos'

export interface SeguridadDeRuta {
  rutaId: string
  dias: { dia: number; nivel: NivelRiesgo; indice: number | null }[]
  fuente: string | null
  actualizadoHasta: string | null
}

/** ConsultarSeguridadDeRuta: sin datos devuelve los siete dias como "sin_datos". */
export async function consultarSeguridadDeRuta(
  repo: IndiceRepositorio,
  rutaId: string
): Promise<SeguridadDeRuta> {
  const zona = await repo.zonaDeRuta(rutaId)
  const dias = Array.from({ length: 7 }, (_, dia) => {
    const indice = zona?.dias.find((d) => d.dia === dia)?.indice ?? null
    return { dia, nivel: nivelDeIndice(indice), indice }
  })
  return {
    rutaId,
    dias,
    fuente: zona?.fuente ?? null,
    actualizadoHasta: zona?.actualizadoHasta ?? null,
  }
}

/** SugerirHorarioSeguro: el dia con menor indice conocido, o null si no hay datos. */
export function sugerirDiaSeguro(s: SeguridadDeRuta): number | null {
  const conDatos = s.dias.filter((d) => d.indice !== null)
  if (!conDatos.length) return null
  return conDatos.reduce((a, b) => ((b.indice as number) < (a.indice as number) ? b : a)).dia
}

/** Incidentes recientes, con un limite entre 1 y 500. */
export function incidentesRecientes(repo: IncidenteRepositorio, limite = 100) {
  return repo.recientes(Math.min(500, Math.max(1, Math.trunc(limite))))
}
