import type { Evento } from '../../../mocks/data'

export const TIPOS_EVENTO = {
  grupal: 'Carrera grupal',
  carrera: 'Competencia',
  entrenamiento: 'Entrenamiento',
} as const

export type TipoEvento = keyof typeof TIPOS_EVENTO

export interface EstadoEvento {
  inscritos: number
  enEspera: number
  cupo: number | null
  miEstado: 'going' | 'waitlist' | null
  miPosicion: number | null
}

export function textoCupo(e: EstadoEvento) {
  if (e.cupo == null) return `${e.inscritos} inscritos`
  const libres = Math.max(e.cupo - e.inscritos, 0)
  return libres > 0
    ? `${e.inscritos}/${e.cupo} · ${libres} ${libres === 1 ? 'cupo libre' : 'cupos libres'}`
    : `Lleno · ${e.enEspera} en espera`
}

export interface DiaCalendario {
  fecha: Date
  delMes: boolean
  eventos: Evento[]
}

const clave = (d: Date) => `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`

/** Semanas (lunes a domingo) del mes con los eventos de cada dia. */
export function calendarioMes(anio: number, mes: number, eventos: Evento[]): DiaCalendario[][] {
  const porDia = new Map<string, Evento[]>()
  for (const e of eventos) {
    if (e.inicioMs == null) continue
    const k = clave(new Date(e.inicioMs))
    porDia.set(k, [...(porDia.get(k) ?? []), e])
  }
  const primero = new Date(anio, mes, 1)
  const inicio = new Date(anio, mes, 1 - ((primero.getDay() + 6) % 7))
  const semanas: DiaCalendario[][] = []
  const d = new Date(inicio)
  do {
    const semana: DiaCalendario[] = []
    for (let i = 0; i < 7; i++) {
      semana.push({
        fecha: new Date(d),
        delMes: d.getMonth() === mes,
        eventos: porDia.get(clave(d)) ?? [],
      })
      d.setDate(d.getDate() + 1)
    }
    semanas.push(semana)
  } while (d.getMonth() === mes)
  return semanas
}

export interface CorredorEvento {
  sesionId: string
  usuarioId: string
  nombre: string
  distanciaM: number
  segundos: number
  lat: number | null
  lng: number | null
  oculto: boolean
  terminado: boolean
  puntos: { lat: number; lng: number }[]
}

/** Clasificacion: mas distancia primero; a igual distancia, menos tiempo. */
export const clasificacion = (c: CorredorEvento[]) =>
  [...c].sort((a, b) => b.distanciaM - a.distanciaM || a.segundos - b.segundos)

/** Posicion de cada corredor en la repeticion (fraccion 0..1 del recorrido de cada uno). */
export function posicionesRepeticion(c: CorredorEvento[], t: number) {
  return c
    .filter((x) => x.puntos.length > 0)
    .map((x) => {
      const i = Math.min(x.puntos.length - 1, Math.floor(t * (x.puntos.length - 1)))
      return { usuarioId: x.usuarioId, nombre: x.nombre, ...x.puntos[i] }
    })
}

/** Une eventos de la base con los de ejemplo sin duplicar ids (gana la base). */
export function unirEventos(base: Evento[], ejemplo: Evento[]): Evento[] {
  const ids = new Set(base.map((e) => e.id))
  return [...base, ...ejemplo.filter((e) => !ids.has(e.id))]
}
export { colorCorredor } from '../../../shared/lib/colores'
