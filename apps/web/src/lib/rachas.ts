import type { CarreraGuardada } from '../data/tipos'
import { inicioDia, inicioSemana } from './estadisticas'

/** Estado de rachas. Misma logica que public.recalcular_racha en Supabase. */
export interface Racha {
  diaria: number
  mejorDiaria: number
  /** Ultimo dia con actividad (ms al inicio del dia) o null. */
  ultimoDiaMs: number | null
  semanal: number
  mejorSemanal: number
  metaSemanal: number
  carrerasSemana: number
}

export const RACHA_VACIA: Racha = {
  diaria: 0,
  mejorDiaria: 0,
  ultimoDiaMs: null,
  semanal: 0,
  mejorSemanal: 0,
  metaSemanal: 3,
  carrerasSemana: 0,
}

const sumarDias = (ms: number, n: number) => {
  const d = new Date(ms)
  d.setDate(d.getDate() + n)
  return d.getTime()
}

/** Longitud de la mejor isla y de la isla que termina en `fin` (o 0) dado un paso. */
function islas(claves: number[], paso: (ms: number) => number) {
  const orden = [...new Set(claves)].sort((a, b) => a - b)
  let mejor = 0
  let actual = 0
  let previo: number | null = null
  const finales = new Map<number, number>()
  for (const k of orden) {
    actual = previo !== null && paso(previo) === k ? actual + 1 : 1
    mejor = Math.max(mejor, actual)
    finales.set(k, actual)
    previo = k
  }
  return { mejor, finales, ultimo: orden.at(-1) ?? null }
}

export function calcularRacha(
  carreras: CarreraGuardada[],
  ahoraMs: number,
  metaSemanal = 3
): Racha {
  const hoy = inicioDia(ahoraMs)
  const dias = islas(
    carreras.map((c) => inicioDia(c.inicioMs)),
    (ms) => sumarDias(ms, 1)
  )
  const diaria =
    dias.ultimo !== null && dias.ultimo >= sumarDias(hoy, -1)
      ? (dias.finales.get(dias.ultimo) ?? 0)
      : 0

  const porSemana = new Map<number, number>()
  for (const c of carreras) {
    const w = inicioSemana(c.inicioMs)
    porSemana.set(w, (porSemana.get(w) ?? 0) + 1)
  }
  const cumplidas = [...porSemana].filter(([, n]) => n >= metaSemanal).map(([w]) => w)
  const semanas = islas(cumplidas, (ms) => sumarDias(ms, 7))
  const actual = inicioSemana(ahoraMs)
  const semanal =
    semanas.ultimo !== null && semanas.ultimo >= sumarDias(actual, -7)
      ? (semanas.finales.get(semanas.ultimo) ?? 0)
      : 0

  return {
    diaria,
    mejorDiaria: dias.mejor,
    ultimoDiaMs: dias.ultimo,
    semanal,
    mejorSemanal: semanas.mejor,
    metaSemanal,
    carrerasSemana: porSemana.get(actual) ?? 0,
  }
}

/** true si la racha diaria sigue viva pero hoy aun no se corrio. */
export const rachaEnRiesgo = (r: Racha, ahoraMs: number) =>
  r.diaria > 0 && r.ultimoDiaMs !== null && r.ultimoDiaMs < inicioDia(ahoraMs)

/** Celdas del mapa de calor: `semanas` columnas (lunes a domingo) que terminan en la actual. */
export function mapaCalor(carreras: CarreraGuardada[], ahoraMs: number, semanas = 12) {
  const km = new Map<number, number>()
  for (const c of carreras) {
    const d = inicioDia(c.inicioMs)
    km.set(d, (km.get(d) ?? 0) + c.distanciaKm)
  }
  const primera = sumarDias(inicioSemana(ahoraMs), -7 * (semanas - 1))
  const hoy = inicioDia(ahoraMs)
  return Array.from({ length: semanas }, (_, w) =>
    Array.from({ length: 7 }, (_, d) => {
      const dia = sumarDias(primera, w * 7 + d)
      return { dia, km: km.get(dia) ?? 0, futuro: dia > hoy }
    })
  )
}

/** Si guardar esta carrera aumenta la racha, devuelve la racha nueva. */
export function rachaTrasCarrera(
  carreras: CarreraGuardada[],
  nueva: Pick<CarreraGuardada, 'inicioMs'>,
  meta: number,
  ahora = Date.now()
): { antes: Racha; despues: Racha } {
  const antes = calcularRacha(carreras, ahora, meta)
  const despues = calcularRacha([...carreras, nueva as CarreraGuardada], ahora, meta)
  return { antes, despues }
}
