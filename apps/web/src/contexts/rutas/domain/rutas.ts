import type { CategoriaRuta, Ruta } from '../../../mocks/data'
// Catalogo de rutas oficiales: contexto Rutas.
export { CATEGORIAS_RUTA } from './catalogo'
import { haversineM } from '../../../shared/lib/geo'

export type Coordenada = { lat: number; lng: number }

/** Etiquetas permitidas (mismas que el CHECK de routes en 009_rutas.sql). */
export const ETIQUETAS_RUTA: Record<string, string> = {
  panoramica: 'Panorámica',
  plana: 'Plana',
  urbana: 'Urbana',
  sendero: 'Sendero',
  montana: 'Montaña',
  parque: 'Parque',
  rio: 'Río',
}

/** Distancia de un punto al segmento a-b en metros (aproximacion plana, valida a escala de ciudad). */
function distanciaASegmentoM(p: Coordenada, a: Coordenada, b: Coordenada): number {
  const kx = 111_320 * Math.cos((a.lat * Math.PI) / 180)
  const ky = 110_540
  const [px, py, bx, by] = [
    (p.lng - a.lng) * kx,
    (p.lat - a.lat) * ky,
    (b.lng - a.lng) * kx,
    (b.lat - a.lat) * ky,
  ]
  const l2 = bx * bx + by * by
  const t = l2 === 0 ? 0 : Math.max(0, Math.min(1, (px * bx + py * by) / l2))
  return Math.hypot(px - t * bx, py - t * by)
}

/** Douglas-Peucker: quita puntos que se desvian menos de `toleranciaM` de la linea. */
export function simplificar(puntos: Coordenada[], toleranciaM = 8): Coordenada[] {
  if (puntos.length <= 2) return puntos.slice()
  const conservar = new Array<boolean>(puntos.length).fill(false)
  conservar[0] = conservar[puntos.length - 1] = true
  const pila: [number, number][] = [[0, puntos.length - 1]]
  while (pila.length) {
    const [i, j] = pila.pop()!
    let max = -1
    let k = -1
    for (let x = i + 1; x < j; x++) {
      const d = distanciaASegmentoM(puntos[x], puntos[i], puntos[j])
      if (d > max) [max, k] = [d, x]
    }
    if (max > toleranciaM && k > 0) {
      conservar[k] = true
      pila.push([i, k], [k, j])
    }
  }
  return puntos.filter((_, i) => conservar[i])
}

/** Simplifica hasta que quepa en el limite del servidor (500 puntos). */
export function trazoParaRuta(puntos: Coordenada[], max = 500): Coordenada[] {
  let tol = 5
  let r = simplificar(puntos, tol)
  while (r.length > max) r = simplificar(puntos, (tol *= 2))
  return r.map((p) => ({ lat: +p.lat.toFixed(6), lng: +p.lng.toFixed(6) }))
}

export const longitudKm = (p: Coordenada[]) =>
  p.slice(1).reduce((s, x, i) => s + haversineM(p[i].lat, p[i].lng, x.lat, x.lng), 0) / 1000

export type FiltroDistancia = 'Todas' | 'Hasta 5 km' | '5-10 km' | 'Más de 10 km'
export const FILTROS_DISTANCIA: FiltroDistancia[] = [
  'Todas',
  'Hasta 5 km',
  '5-10 km',
  'Más de 10 km',
]

export interface FiltrosRuta {
  dificultad: Ruta['dificultad'] | 'Todas'
  distancia: FiltroDistancia
  etiqueta: string | null
  categoria?: CategoriaRuta | null
}

export const FILTROS_INICIALES: FiltrosRuta = {
  dificultad: 'Todas',
  distancia: 'Todas',
  etiqueta: null,
}

export function aplicarFiltros(rutas: Ruta[], f: FiltrosRuta): Ruta[] {
  return rutas.filter(
    (r) =>
      (f.dificultad === 'Todas' || r.dificultad === f.dificultad) &&
      (f.distancia === 'Todas' ||
        (f.distancia === 'Hasta 5 km' && r.distanciaKm <= 5) ||
        (f.distancia === '5-10 km' && r.distanciaKm > 5 && r.distanciaKm <= 10) ||
        (f.distancia === 'Más de 10 km' && r.distanciaKm > 10)) &&
      (!f.etiqueta || (r.etiquetas ?? []).includes(f.etiqueta)) &&
      (!f.categoria || r.categoria === f.categoria)
  )
}

/** Ordena por cercania (las que no tienen distancia van al final). */
export function ordenarPorCercania(rutas: Ruta[], cerca: Record<string, number>): Ruta[] {
  return [...rutas].sort((a, b) => (cerca[a.id] ?? Infinity) - (cerca[b.id] ?? Infinity))
}
