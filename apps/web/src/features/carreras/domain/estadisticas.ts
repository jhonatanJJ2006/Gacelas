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

/** Totales de la semana en curso para "Tu semana atlética" (misma regla que Android). */
export function totalesSemana(carreras: CarreraGuardada[], ahoraMs: number): ResumenPeriodo {
  const desde = inicioSemana(ahoraMs)
  const semana = carreras.filter((c) => c.inicioMs >= desde && c.inicioMs <= ahoraMs + DIA_MS)
  return sumar(semana)
}

function sumar(lista: CarreraGuardada[]): ResumenPeriodo {
  return {
    carreras: lista.length,
    km: lista.reduce((s, c) => s + c.distanciaKm, 0),
    segundos: lista.reduce((s, c) => s + c.segundos, 0),
    desnivelM: lista.reduce((s, c) => s + c.desnivelM, 0),
  }
}

export interface Historico extends ResumenPeriodo {
  /** Mejor ritmo medio (s/km) entre carreras de al menos 1 km. */
  mejorRitmoSegKm: number | null
}

export function historico(carreras: CarreraGuardada[]): Historico {
  const ritmos = carreras
    .filter((c) => c.distanciaKm >= 1 && c.segundos > 0)
    .map((c) => Math.floor(c.segundos / c.distanciaKm))
  return { ...sumar(carreras), mejorRitmoSegKm: ritmos.length ? Math.min(...ritmos) : null }
}

export const MESES_CORTOS = [
  'Ene',
  'Feb',
  'Mar',
  'Abr',
  'May',
  'Jun',
  'Jul',
  'Ago',
  'Sep',
  'Oct',
  'Nov',
  'Dic',
]
export const MESES = [
  'Enero',
  'Febrero',
  'Marzo',
  'Abril',
  'Mayo',
  'Junio',
  'Julio',
  'Agosto',
  'Septiembre',
  'Octubre',
  'Noviembre',
  'Diciembre',
]

export interface KmMes {
  anio: number
  /** 1 = enero. */
  mes: number
  etiqueta: string
  km: number
}

/** Kilometros por mes de los ultimos `n` meses (el ultimo es el mes actual). */
export function kmMensual(carreras: CarreraGuardada[], ahoraMs: number, n = 6): KmMes[] {
  const a = new Date(ahoraMs)
  const res: KmMes[] = []
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date(a.getFullYear(), a.getMonth() - i, 1)
    const km = carreras
      .filter((c) => {
        const x = new Date(c.inicioMs)
        return x.getFullYear() === d.getFullYear() && x.getMonth() === d.getMonth()
      })
      .reduce((s, c) => s + c.distanciaKm, 0)
    res.push({
      anio: d.getFullYear(),
      mes: d.getMonth() + 1,
      etiqueta: MESES_CORTOS[d.getMonth()],
      km,
    })
  }
  return res
}

export interface RecordPersonal {
  etiqueta: string
  km: number
  /** Mejor tiempo estimado (s) o null si aun no hay una carrera de esa distancia. */
  segundos: number | null
}

const DISTANCIAS_RECORD: [string, number][] = [
  ['1K', 1],
  ['5K', 5],
  ['10K', 10],
  ['21K', 21.0975],
]

/** Records: mejor tiempo a ritmo medio entre carreras que cubren la distancia (y parciales de 1 km). */
export function records(carreras: CarreraGuardada[]): RecordPersonal[] {
  return DISTANCIAS_RECORD.map(([etiqueta, km]) => {
    const tiempos = carreras
      .filter((c) => c.distanciaKm >= km && c.segundos > 0)
      .map((c) => Math.round((c.segundos / c.distanciaKm) * km))
    if (km === 1)
      for (const c of carreras)
        for (const p of c.parciales ?? [])
          if (p.distanciaKm >= 0.99 && p.segundos > 0) tiempos.push(p.segundos)
    return { etiqueta, km, segundos: tiempos.length ? Math.min(...tiempos) : null }
  })
}

/** Mayor numero de dias seguidos con carrera en todo el historial. */
export function mejorRacha(carreras: CarreraGuardada[]): number {
  const dias = [...new Set(carreras.map((c) => inicioDia(c.inicioMs)))].sort((x, y) => x - y)
  let mejor = 0
  let actual = 0
  let previo: number | null = null
  for (const d of dias) {
    actual = previo != null && Math.round((d - previo) / DIA_MS) === 1 ? actual + 1 : 1
    mejor = Math.max(mejor, actual)
    previo = d
  }
  return mejor
}

export interface Medalla {
  id: string
  titulo: string
  descripcion: string
  lograda: boolean
}

export function medallas(carreras: CarreraGuardada[]): Medalla[] {
  const max = carreras.reduce((m, c) => Math.max(m, c.distanciaKm), 0)
  const total = carreras.reduce((s, c) => s + c.distanciaKm, 0)
  return [
    {
      id: 'primera',
      titulo: 'Primera zancada',
      descripcion: 'Primera carrera guardada',
      lograda: carreras.length > 0,
    },
    { id: '5k', titulo: 'Gacela 5K', descripcion: 'Una carrera de 5 km', lograda: max >= 5 },
    { id: '10k', titulo: 'Diez mil', descripcion: 'Una carrera de 10 km', lograda: max >= 10 },
    {
      id: '21k',
      titulo: 'Media maratón',
      descripcion: 'Una carrera de 21 km',
      lograda: max >= 21.0975,
    },
    {
      id: 'centenario',
      titulo: 'Centenario',
      descripcion: '100 km acumulados',
      lograda: total >= 100,
    },
    {
      id: 'racha7',
      titulo: 'Semana perfecta',
      descripcion: '7 días seguidos corriendo',
      lograda: mejorRacha(carreras) >= 7,
    },
  ]
}

export interface RetoMes {
  titulo: string
  metaKm: number
  km: number
  diasRestantes: number
}

export const META_RETO_KM = 100

/** Reto del mes: llegar a META_RETO_KM km dentro del mes en curso. */
export function retoMes(carreras: CarreraGuardada[], ahoraMs: number): RetoMes {
  const a = new Date(ahoraMs)
  const ultimo = new Date(a.getFullYear(), a.getMonth() + 1, 0).getDate()
  return {
    titulo: `Reto ${MESES[a.getMonth()]} Andino`,
    metaKm: META_RETO_KM,
    km: resumenMes(carreras, ahoraMs).km,
    diasRestantes: ultimo - a.getDate(),
  }
}
