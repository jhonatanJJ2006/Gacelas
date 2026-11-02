// Clima del chip de Inicio. Mismas reglas que Android (contextos/clima/domain/Clima.kt).

export interface Coordenadas {
  lat: number
  lng: number
}

export interface ClimaActual {
  /** Grados Celsius redondeados. */
  temperatura: number
  /** Texto en español en minusculas, p. ej. "nublado". */
  descripcion: string
  /** "Loja", un nombre simple o "Tu zona". */
  lugar: string
  /** Momento de la consulta (epoch ms). */
  consultadoMs: number
}

export interface LecturaClima {
  temperatura: number
  codigoWmo: number
  esDeDia: boolean
}

export const LOJA: Coordenadas = { lat: -3.9931, lng: -79.2042 }
export const NOMBRE_LOJA = 'Loja'
export const TU_ZONA = 'Tu zona'
export const RADIO_LOJA_KM = 30
export const REFRESCO_MS = 15 * 60_000
export const TIMEOUT_MS = 5_000

/** Minimo 15 min entre llamadas; un reloj que retrocede tambien refresca. */
export function debeRefrescar(ultimaMs: number | null | undefined, ahoraMs: number): boolean {
  return ultimaMs == null || ahoraMs < ultimaMs || ahoraMs - ultimaMs >= REFRESCO_MS
}

/** Privacidad: a Open-Meteo solo van 2 decimales (~1 km). */
export function aproximar(c: Coordenadas): Coordenadas {
  return { lat: Math.round(c.lat * 100) / 100, lng: Math.round(c.lng * 100) / 100 }
}

export function distanciaKm(a: Coordenadas, b: Coordenadas): number {
  const rad = (g: number) => (g * Math.PI) / 180
  const dLat = rad(b.lat - a.lat)
  const dLng = rad(b.lng - a.lng)
  const h =
    Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.lat)) * Math.cos(rad(b.lat)) * Math.sin(dLng / 2) ** 2
  return 2 * 6371 * Math.asin(Math.sqrt(h))
}

/** "Loja" con el respaldo o a menos de 30 km; si no, "Tu zona" (la web no tiene geocodificacion inversa). */
export function etiquetaLugar(c: Coordenadas | null): string {
  return c == null || distanciaKm(c, LOJA) < RADIO_LOJA_KM ? NOMBRE_LOJA : TU_ZONA
}

/** Codigo WMO (Open-Meteo) a texto en español. */
export function descripcionClima(codigo: number, esDeDia: boolean): string {
  if (codigo === 0) return esDeDia ? 'despejado' : 'noche despejada'
  if (codigo === 1) return esDeDia ? 'mayormente despejado' : 'noche mayormente despejada'
  if (codigo === 2) return 'parcialmente nublado'
  if (codigo === 3) return 'nublado'
  if ([45, 48].includes(codigo)) return 'niebla'
  if ([51, 53, 55, 56, 57].includes(codigo)) return 'llovizna'
  if ([61, 63, 65, 66, 67].includes(codigo)) return 'lluvia'
  if ([71, 73, 75, 77, 85, 86].includes(codigo)) return 'nieve'
  if ([80, 81, 82].includes(codigo)) return 'chubascos'
  if ([95, 96, 99].includes(codigo)) return 'tormenta'
  return 'nublado'
}

export function aClima(l: LecturaClima, lugar: string, ahoraMs: number): ClimaActual {
  return {
    temperatura: Math.round(l.temperatura),
    descripcion: descripcionClima(l.codigoWmo, l.esDeDia),
    lugar,
    consultadoMs: ahoraMs,
  }
}
