// Clima con Open-Meteo (gratis, sin clave, CORS abierto, CC BY 4.0) y cache en localStorage.
import {
  aClima,
  aproximar,
  debeRefrescar,
  etiquetaLugar,
  LOJA,
  TIMEOUT_MS,
  type ClimaActual,
  type Coordenadas,
  type LecturaClima,
} from '../domain/clima'

export const CLAVE_CACHE_CLIMA = 'gacelas.clima'

export function urlOpenMeteo(c: Coordenadas): string {
  return `https://api.open-meteo.com/v1/forecast?latitude=${c.lat}&longitude=${c.lng}&current=temperature_2m,weather_code,is_day`
}

export function leerRespuesta(json: unknown): LecturaClima {
  const actual = (json as { current?: Record<string, unknown> }).current
  const t = actual?.temperature_2m
  const codigo = actual?.weather_code
  if (typeof t !== 'number' || typeof codigo !== 'number')
    throw new Error('Respuesta de clima invalida')
  return { temperatura: t, codigoWmo: codigo, esDeDia: actual?.is_day !== 0 }
}

export async function leerOpenMeteo(
  c: Coordenadas,
  timeoutMs = TIMEOUT_MS,
  f: typeof fetch = fetch
): Promise<LecturaClima> {
  const ctrl = new AbortController()
  const t = setTimeout(() => ctrl.abort(), timeoutMs)
  try {
    const r = await f(urlOpenMeteo(c), { signal: ctrl.signal })
    if (!r.ok) throw new Error(`HTTP ${r.status}`)
    return leerRespuesta(await r.json())
  } finally {
    clearTimeout(t)
  }
}

export function leerCacheClima(): ClimaActual | null {
  try {
    const v = JSON.parse(localStorage.getItem(CLAVE_CACHE_CLIMA) ?? 'null') as ClimaActual | null
    return v && typeof v.temperatura === 'number' && typeof v.lugar === 'string' ? v : null
  } catch {
    return null
  }
}

function guardarCacheClima(c: ClimaActual) {
  try {
    localStorage.setItem(CLAVE_CACHE_CLIMA, JSON.stringify(c))
  } catch {
    // Sin almacenamiento (modo privado): se sigue sin cache.
  }
}

/**
 * Ubicacion solo si el permiso ya esta concedido (no abre el aviso del navegador); null = Loja.
 * Acepta una posicion en cache de hasta 30 min y precision baja.
 */
export async function ubicacionSiHayPermiso(): Promise<Coordenadas | null> {
  try {
    if (!('geolocation' in navigator) || !navigator.permissions?.query) return null
    const p = await navigator.permissions.query({ name: 'geolocation' })
    if (p.state !== 'granted') return null
    return await new Promise((ok) =>
      navigator.geolocation.getCurrentPosition(
        (pos) => ok({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
        () => ok(null),
        { enableHighAccuracy: false, maximumAge: 30 * 60_000, timeout: TIMEOUT_MS }
      )
    )
  } catch {
    return null
  }
}

let ultimoIntentoMs: number | null = null

/** Solo pruebas. */
export function reiniciarIntentosClima() {
  ultimoIntentoMs = null
}

/** Cache primero; a la red como mucho cada 15 min (tambien tras un fallo). Nunca lanza. */
export async function climaActual(
  ahoraMs: number,
  deps: {
    ubicacion?: () => Promise<Coordenadas | null>
    leer?: (c: Coordenadas) => Promise<LecturaClima>
  } = {}
): Promise<ClimaActual | null> {
  const guardado = leerCacheClima()
  if (guardado && !debeRefrescar(guardado.consultadoMs, ahoraMs)) return guardado
  if (!debeRefrescar(ultimoIntentoMs, ahoraMs)) return guardado
  ultimoIntentoMs = ahoraMs
  const coords = await (deps.ubicacion ?? ubicacionSiHayPermiso)().catch(() => null)
  try {
    const lectura = await (deps.leer ?? ((c) => leerOpenMeteo(c)))(aproximar(coords ?? LOJA))
    const clima = aClima(lectura, etiquetaLugar(coords), ahoraMs)
    guardarCacheClima(clima)
    return clima
  } catch {
    return guardado
  }
}
