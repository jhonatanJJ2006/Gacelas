import { haversineM } from './geo'
import type { Coordenada } from './rutas'

/** Toma `n` puntos repartidos a igual distancia a lo largo del trazo. */
export function muestrear(puntos: Coordenada[], n = 60): Coordenada[] {
  if (puntos.length < 2) return puntos.slice()
  const acum = [0]
  for (let i = 1; i < puntos.length; i++)
    acum.push(
      acum[i - 1] + haversineM(puntos[i - 1].lat, puntos[i - 1].lng, puntos[i].lat, puntos[i].lng)
    )
  const total = acum[acum.length - 1]
  if (total === 0) return [puntos[0]]
  const r: Coordenada[] = []
  let j = 1
  for (let k = 0; k < n; k++) {
    const d = (total * k) / (n - 1)
    while (j < puntos.length - 1 && acum[j] < d) j++
    const t = acum[j] === acum[j - 1] ? 0 : (d - acum[j - 1]) / (acum[j] - acum[j - 1])
    const a = puntos[j - 1]
    const b = puntos[j]
    r.push({ lat: a.lat + (b.lat - a.lat) * t, lng: a.lng + (b.lng - a.lng) * t })
  }
  return r
}

/** Suma de subidas (m), ignorando ruido menor a `umbral`. */
export function desnivelPositivo(alturas: number[], umbral = 2): number {
  let total = 0
  let base = alturas[0] ?? 0
  for (const h of alturas.slice(1)) {
    if (h - base >= umbral) {
      total += h - base
      base = h
    } else if (h < base) base = h
  }
  return Math.round(total)
}

/** Alturas del terreno con la API gratuita de Open-Meteo (100 puntos por llamada). */
export async function perfilElevacion(
  puntos: Coordenada[],
  pedir: typeof fetch = fetch
): Promise<number[]> {
  const m = muestrear(puntos, 60)
  const alturas: number[] = []
  for (let i = 0; i < m.length; i += 100) {
    const lote = m.slice(i, i + 100)
    const url =
      'https://api.open-meteo.com/v1/elevation?latitude=' +
      lote.map((p) => p.lat.toFixed(5)).join(',') +
      '&longitude=' +
      lote.map((p) => p.lng.toFixed(5)).join(',')
    const r = await pedir(url)
    if (!r.ok) throw new Error('No se pudo obtener la elevación')
    const j = (await r.json()) as { elevation?: number[] }
    alturas.push(...(j.elevation ?? []).map((h) => Math.round(h)))
  }
  return alturas
}
