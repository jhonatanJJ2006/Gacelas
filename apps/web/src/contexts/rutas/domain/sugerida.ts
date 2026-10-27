import type { Ruta } from '../../../mocks/data'
import type { CarreraGuardada } from '../../../shared/kernel/tipos'
import { haversineM } from '../../../shared/lib/geo'

/*
 * Ruta sugerida para hoy (contexto Rutas). La regla oficial vive en Supabase
 * (ruta_sugerida_hoy, migracion 035). Esta copia pura se usa solo sin red y aplica lo mismo:
 * 1. solo rutas seguras (ids que el servidor marco como seguras la ultima vez),
 * 2. sin rutas corridas ni sugeridas en los dias de Ecuador hoy-3 .. hoy,
 * 3. orden por cercania a la distancia objetivo y al usuario, desempate rotativo por dia.
 * Ver docs/RUTA-SUGERIDA.md.
 */

export const DIAS_SIN_REPETIR = 3
const OFFSET_ECUADOR_MS = 5 * 3600 * 1000

/** Fecha AAAA-MM-DD en Ecuador (UTC-5 fijo, sin horario de verano). */
export function diaEcuador(ms: number): string {
  return new Date(ms - OFFSET_ECUADOR_MS).toISOString().slice(0, 10)
}

function restarDias(dia: string, n: number): string {
  const d = new Date(`${dia}T00:00:00Z`)
  d.setUTCDate(d.getUTCDate() - n)
  return d.toISOString().slice(0, 10)
}

/** Dias de la ventana sin repetir: hoy, ayer, anteayer y hace 3 dias. */
export function ventanaSinRepetir(hoy: string): string[] {
  return Array.from({ length: DIAS_SIN_REPETIR + 1 }, (_, i) => restarDias(hoy, i))
}

/** Media de las ultimas 5 carreras; 5 km sin historial. */
export function distanciaObjetivo(carreras: CarreraGuardada[]): number {
  const ultimas = [...carreras]
    .filter((c) => c.distanciaKm > 0)
    .sort((a, b) => b.inicioMs - a.inicioMs)
    .slice(0, 5)
  if (!ultimas.length) return 5
  return Math.max(0.5, ultimas.reduce((t, c) => t + c.distanciaKm, 0) / ultimas.length)
}

/** Una carrera cuenta como esa ruta si empezo a 300 m o menos de su inicio y midio 70-130 %. */
export function carreraCoincide(carrera: CarreraGuardada, ruta: Ruta): boolean {
  const inicio = ruta.puntos?.[0]
  const p = carrera.puntos[0]
  if (!inicio || !p) return false
  const d = carrera.distanciaKm
  return (
    haversineM(p.lat, p.lng, inicio.lat, inicio.lng) <= 300 &&
    d >= ruta.distanciaKm * 0.7 &&
    d <= ruta.distanciaKm * 1.3
  )
}

/** Hash estable (FNV-1a) para rotar el desempate cada dia. */
function giro(texto: string): number {
  let h = 0x811c9dc5
  for (let i = 0; i < texto.length; i++) {
    h ^= texto.charCodeAt(i)
    h = Math.imul(h, 0x01000193) >>> 0
  }
  return h
}

export interface Sugerencia {
  dia: string
  rutaId: string
}

export interface EntradaSugerencia {
  rutas: Ruta[]
  carreras: CarreraGuardada[]
  /** Ids de rutas seguras ahora. Sin este dato no se sugiere nada. */
  seguras: Set<string>
  /** Sugerencias previas del usuario. */
  historial: Sugerencia[]
  ahoraMs: number
  ubicacion?: { lat: number; lng: number } | null
}

export type ResultadoSugerencia =
  | { estado: 'ok'; ruta: Ruta; dia: string; objetivoKm: number }
  | { estado: 'sin_ruta'; dia: string; motivo: string }

export function elegirRutaSugerida(e: EntradaSugerencia): ResultadoSugerencia {
  const hoy = diaEcuador(e.ahoraMs)
  const yaHoy = e.historial.find((s) => s.dia === hoy)
  const objetivoKm = distanciaObjetivo(e.carreras)
  if (yaHoy) {
    const ruta = e.rutas.find((r) => r.id === yaHoy.rutaId)
    if (ruta && e.seguras.has(ruta.id)) return { estado: 'ok', ruta, dia: hoy, objetivoKm }
  }
  const seguras = e.rutas.filter((r) => e.seguras.has(r.id) && r.distanciaKm > 0)
  if (!seguras.length) {
    return {
      estado: 'sin_ruta',
      dia: hoy,
      motivo: 'Ahora ninguna ruta tiene un nivel de seguridad suficiente para esta hora.',
    }
  }
  const ventana = new Set(ventanaSinRepetir(hoy))
  const sugeridas = new Set(e.historial.filter((s) => ventana.has(s.dia)).map((s) => s.rutaId))
  const recientes = e.carreras.filter((c) => ventana.has(diaEcuador(c.inicioMs)))
  const libres = seguras.filter(
    (r) => !sugeridas.has(r.id) && !recientes.some((c) => carreraCoincide(c, r))
  )
  if (!libres.length) {
    return {
      estado: 'sin_ruta',
      dia: hoy,
      motivo: 'Ya corriste o te sugerimos todas las rutas seguras en los últimos 3 días.',
    }
  }
  const puntaje = (r: Ruta) => {
    let p = Math.abs(r.distanciaKm - objetivoKm) / objetivoKm
    const ini = r.puntos?.[0]
    if (e.ubicacion && ini)
      p += haversineM(e.ubicacion.lat, e.ubicacion.lng, ini.lat, ini.lng) / 10_000
    return p
  }
  const ruta = [...libres].sort(
    (a, b) => puntaje(a) - puntaje(b) || giro(a.id + hoy) - giro(b.id + hoy)
  )[0]
  return { estado: 'ok', ruta, dia: hoy, objetivoKm }
}
