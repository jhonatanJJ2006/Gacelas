import type { PublicacionRemota } from '../../../shared/kernel/comunidad'
import { inicioSemana } from '../../../shared/kernel/semana'
import type { CarreraGuardada } from '../../../shared/kernel/tipos'

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
