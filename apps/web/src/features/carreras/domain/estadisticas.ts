import type { CarreraGuardada } from '../../../data/tipos'

const DIA_MS = 86_400_000

export function inicioDia(ms: number): number {
  const d = new Date(ms)
  d.setHours(0, 0, 0, 0)
  return d.getTime()
}

/** Dia de la semana con lunes = 0. */
export const diaSemana = (ms: number) => (new Date(ms).getDay() + 6) % 7

export function inicioSemana(ahoraMs: number): number {
  const d = new Date(inicioDia(ahoraMs))
  d.setDate(d.getDate() - diaSemana(ahoraMs))
  return d.getTime()
}

export function racha(carreras: CarreraGuardada[], ahoraMs: number): number {
  const dias = new Set(carreras.map((c) => inicioDia(c.inicioMs)))
  const anterior = (ms: number) => {
    const d = new Date(ms)
    d.setDate(d.getDate() - 1)
    return d.getTime()
  }
  let dia = inicioDia(ahoraMs)
  if (!dias.has(dia)) dia = anterior(dia)
  let n = 0
  while (dias.has(dia)) {
    n++
    dia = anterior(dia)
  }
  return n
}

export interface ResumenSemana {
  diasCorridos: Set<number>
  km: number
  ritmoSegKm: number | null
  carreras: number
  racha: number
}

export function resumenSemana(carreras: CarreraGuardada[], ahoraMs: number): ResumenSemana {
  const desde = inicioSemana(ahoraMs)
  const semana = carreras.filter((c) => c.inicioMs >= desde && c.inicioMs <= ahoraMs + DIA_MS)
  const km = semana.reduce((s, c) => s + c.distanciaKm, 0)
  const seg = semana.reduce((s, c) => s + c.segundos, 0)
  return {
    diasCorridos: new Set(semana.map((c) => diaSemana(c.inicioMs))),
    km,
    ritmoSegKm: km > 0.01 ? Math.floor(seg / km) : null,
    carreras: semana.length,
    racha: racha(carreras, ahoraMs),
  }
}

export interface ResumenPeriodo {
  carreras: number
  km: number
  segundos: number
  desnivelM: number
}

export function resumenMes(carreras: CarreraGuardada[], ahoraMs: number): ResumenPeriodo {
  const a = new Date(ahoraMs)
  const delMes = carreras.filter((c) => {
    const d = new Date(c.inicioMs)
    return d.getFullYear() === a.getFullYear() && d.getMonth() === a.getMonth()
  })
  return {
    carreras: delMes.length,
    km: delMes.reduce((s, c) => s + c.distanciaKm, 0),
    segundos: delMes.reduce((s, c) => s + c.segundos, 0),
    desnivelM: delMes.reduce((s, c) => s + c.desnivelM, 0),
  }
}
