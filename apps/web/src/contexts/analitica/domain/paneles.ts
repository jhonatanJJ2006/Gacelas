import { SEMANA_MS, inicioSemana } from '../../../shared/kernel/semana'
import type { CarreraGuardada } from '../../../shared/kernel/tipos'

/** Km por semana de las ultimas `n` semanas (la ultima es la actual). */
export function kmPorSemana(carreras: CarreraGuardada[], ahora: number, n = 6) {
  const actual = inicioSemana(ahora)
  return Array.from({ length: n }, (_, i) => {
    const desde = actual - (n - 1 - i) * SEMANA_MS
    const km = carreras
      .filter((c) => c.inicioMs >= desde && c.inicioMs < desde + SEMANA_MS)
      .reduce((s, c) => s + c.distanciaKm, 0)
    return { desde, km }
  })
}

export function recordsPersonales(carreras: CarreraGuardada[]) {
  const validas = carreras.filter((c) => c.distanciaKm >= 0.2 && c.segundos > 0)
  const masLarga = carreras.reduce<CarreraGuardada | null>(
    (m, c) => (!m || c.distanciaKm > m.distanciaKm ? c : m),
    null
  )
  const masRapida = validas.reduce<CarreraGuardada | null>(
    (m, c) => (!m || c.segundos / c.distanciaKm < m.segundos / m.distanciaKm ? c : m),
    null
  )
  return {
    masLarga,
    mejorRitmoSegKm: masRapida ? Math.floor(masRapida.segundos / masRapida.distanciaKm) : null,
    totalSegundos: carreras.reduce((s, c) => s + c.segundos, 0),
  }
}
