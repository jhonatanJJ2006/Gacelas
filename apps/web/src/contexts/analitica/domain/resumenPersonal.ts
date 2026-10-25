import type { CarreraGuardada } from '../../../data/tipos'
import type { Ruta } from '../../../mocks/data'
import { calcularRacha } from '../../../features/carreras/domain/rachas'
import { inicioDia, inicioSemana } from '../../../features/carreras/domain/estadisticas'

/**
 * Reporte personal del corredor calculado en el cliente con sus propias carreras.
 * Es la misma definicion que public.reporte_personal (migracion 023): semana desde el lunes,
 * mes y año calendario, comparados con el periodo anterior de igual tipo.
 */
export type Periodo = 'semana' | 'mes' | 'anio'

export interface Totales {
  carreras: number
  km: number
  segundos: number
  desnivelM: number
  /** Segundos por km, null sin distancia. */
  ritmoSegKm: number | null
}

export interface ReportePersonal {
  periodo: Periodo
  desdeMs: number
  hastaMs: number
  actual: Totales
  previo: Totales
  /** Variacion porcentual de km respecto al periodo anterior (null si el anterior es 0). */
  variacionKm: number | null
  mejores: { masLargaKm: number; mejorRitmoSegKm: number | null; masDesnivelM: number }
  racha: { actual: number; mejor: number }
  serie: { etiqueta: string; km: number }[]
  rutasFavoritas: { id: string; nombre: string; veces: number }[]
}

const MESES = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic']
const DIAS = ['L', 'M', 'X', 'J', 'V', 'S', 'D']

export function rangoPeriodo(periodo: Periodo, refMs: number, atras = 0): [number, number] {
  const d = new Date(refMs)
  if (periodo === 'semana') {
    const ini = new Date(inicioSemana(refMs))
    ini.setDate(ini.getDate() - 7 * atras)
    const fin = new Date(ini)
    fin.setDate(fin.getDate() + 7)
    return [ini.getTime(), fin.getTime()]
  }
  if (periodo === 'mes') {
    return [
      new Date(d.getFullYear(), d.getMonth() - atras, 1).getTime(),
      new Date(d.getFullYear(), d.getMonth() - atras + 1, 1).getTime(),
    ]
  }
  return [
    new Date(d.getFullYear() - atras, 0, 1).getTime(),
    new Date(d.getFullYear() - atras + 1, 0, 1).getTime(),
  ]
}

export function totales(carreras: CarreraGuardada[]): Totales {
  const km = carreras.reduce((s, c) => s + c.distanciaKm, 0)
  const segundos = carreras.reduce((s, c) => s + c.segundos, 0)
  return {
    carreras: carreras.length,
    km: Math.round(km * 100) / 100,
    segundos,
    desnivelM: Math.round(carreras.reduce((s, c) => s + c.desnivelM, 0)),
    ritmoSegKm: km > 0 ? Math.round(segundos / km) : null,
  }
}

/** Distancia aproximada en metros (suficiente para asociar una carrera a una ruta). */
function metros(a: { lat: number; lng: number }, b: { lat: number; lng: number }) {
  const x = (b.lng - a.lng) * Math.cos(((a.lat + b.lat) / 2) * (Math.PI / 180)) * 111_320
  const y = (b.lat - a.lat) * 110_540
  return Math.hypot(x, y)
}

/** Ruta cuyo inicio queda a menos de 150 m del inicio de la carrera (la mas cercana). */
export function rutaDeCarrera(c: CarreraGuardada, rutas: Ruta[]): Ruta | null {
  const p = c.puntos[0]
  if (!p) return null
  let mejor: Ruta | null = null
  let dist = 150
  for (const r of rutas) {
    if (!r.puntos?.[0]) continue
    const d = metros(p, r.puntos?.[0])
    if (d < dist) {
      dist = d
      mejor = r
    }
  }
  return mejor
}

export function reportePersonal(
  carreras: CarreraGuardada[],
  periodo: Periodo,
  refMs: number,
  rutas: Ruta[] = []
): ReportePersonal {
  const [desde, hasta] = rangoPeriodo(periodo, refMs)
  const [pDesde, pHasta] = rangoPeriodo(periodo, refMs, 1)
  const en = (a: number, b: number) => carreras.filter((c) => c.inicioMs >= a && c.inicioMs < b)
  const actuales = en(desde, hasta)
  const actual = totales(actuales)
  const previo = totales(en(pDesde, pHasta))

  const serie: ReportePersonal['serie'] = []
  if (periodo === 'anio') {
    const y = new Date(desde).getFullYear()
    for (let m = 0; m < 12; m++) {
      const a = new Date(y, m, 1).getTime()
      const b = new Date(y, m + 1, 1).getTime()
      serie.push({ etiqueta: MESES[m], km: totales(en(a, b)).km })
    }
  } else {
    for (let t = desde, i = 0; t < hasta; i++) {
      const sig = new Date(t)
      sig.setDate(sig.getDate() + 1)
      serie.push({
        etiqueta: periodo === 'semana' ? DIAS[i] : String(new Date(t).getDate()),
        km: totales(en(t, sig.getTime())).km,
      })
      t = inicioDia(sig.getTime())
    }
  }

  const veces = new Map<string, { id: string; nombre: string; veces: number }>()
  for (const c of actuales) {
    const r = rutaDeCarrera(c, rutas)
    if (!r) continue
    const v = veces.get(r.id) ?? { id: r.id, nombre: r.nombre, veces: 0 }
    v.veces++
    veces.set(r.id, v)
  }

  const conKm = actuales.filter((c) => c.distanciaKm >= 1)
  const r = calcularRacha(carreras, Math.min(refMs, hasta - 1))
  return {
    periodo,
    desdeMs: desde,
    hastaMs: hasta,
    actual,
    previo,
    variacionKm:
      previo.km > 0 ? Math.round(((actual.km - previo.km) / previo.km) * 1000) / 10 : null,
    mejores: {
      masLargaKm: Math.max(0, ...actuales.map((c) => c.distanciaKm)),
      mejorRitmoSegKm: conKm.length
        ? Math.min(...conKm.map((c) => Math.round(c.segundos / c.distanciaKm)))
        : null,
      masDesnivelM: Math.max(0, ...actuales.map((c) => c.desnivelM)),
    },
    racha: { actual: r.diaria, mejor: r.mejorDiaria },
    serie,
    rutasFavoritas: [...veces.values()].sort((a, b) => b.veces - a.veces).slice(0, 5),
  }
}

export const ritmoTexto = (s: number | null) =>
  s == null ? '--' : `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`

export function duracionTexto(s: number) {
  const h = Math.floor(s / 3600)
  const m = Math.round((s % 3600) / 60)
  return h ? `${h} h ${m} min` : `${m} min`
}

export const NOMBRE_PERIODO: Record<Periodo, string> = {
  semana: 'Semana',
  mes: 'Mes',
  anio: 'Año',
}

export function etiquetaRango(rep: ReportePersonal): string {
  const f = (ms: number) =>
    new Date(ms).toLocaleDateString('es-EC', { day: 'numeric', month: 'short', year: 'numeric' })
  return `${f(rep.desdeMs)} al ${f(rep.hastaMs - 1)}`
}

/** Texto plano para compartir el reporte. */
export function textoCompartir(rep: ReportePersonal): string {
  const v =
    rep.variacionKm == null
      ? ''
      : ` (${rep.variacionKm >= 0 ? '+' : ''}${rep.variacionKm}% vs. periodo anterior)`
  return [
    `Mi ${NOMBRE_PERIODO[rep.periodo].toLowerCase()} en Gacelas: ${rep.actual.km.toFixed(1)} km${v}`,
    `${rep.actual.carreras} carreras, ${duracionTexto(rep.actual.segundos)}, ritmo ${ritmoTexto(rep.actual.ritmoSegKm)} /km`,
    `Racha: ${rep.racha.actual} días`,
  ].join('\n')
}
