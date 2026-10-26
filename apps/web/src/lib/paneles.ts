import type { CarreraGuardada } from '../shared/kernel/tipos'
import type { PublicacionRemota } from '../data/repositorios'

const SEMANA_MS = 7 * 24 * 3600 * 1000

/** Lunes 00:00 de la semana de `ahora` (hora local). */
export function inicioSemana(ahora: number) {
  const d = new Date(ahora)
  const dia = (d.getDay() + 6) % 7
  d.setHours(0, 0, 0, 0)
  return d.getTime() - dia * 24 * 3600 * 1000
}

export interface FilaRanking {
  nombre: string
  km: number
  propio: boolean
}

/** Ranking de km de esta semana entre tus carreras y las publicas que puedes ver. */
export function rankingSemanal(
  propias: CarreraGuardada[],
  nombre: string,
  remotas: PublicacionRemota[],
  ahora: number
): FilaRanking[] {
  const desde = inicioSemana(ahora)
  const mapa = new Map<string, FilaRanking>()
  const sumar = (clave: string, n: string, km: number, propio: boolean) => {
    const f = mapa.get(clave) ?? { nombre: n, km: 0, propio }
    f.km += km
    mapa.set(clave, f)
  }
  propias
    .filter((c) => c.inicioMs >= desde)
    .forEach((c) => sumar('yo', nombre, c.distanciaKm, true))
  remotas
    .filter((r) => r.inicioMs >= desde)
    .forEach((r) => sumar(r.autorId, r.autor, r.distanciaKm, false))
  if (!mapa.has('yo')) mapa.set('yo', { nombre, km: 0, propio: true })
  return [...mapa.values()].sort((a, b) => b.km - a.km).slice(0, 5)
}

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
