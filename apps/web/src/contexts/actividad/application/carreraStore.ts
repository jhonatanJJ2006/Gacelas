import { useSyncExternalStore } from 'react'
import { RunTracker, estaActiva, type EstadoCarrera } from '../domain/runTracker'

/** Carrera en curso compartida por En curso, Cronometro y Navegacion. */
export const tracker = new RunTracker()

export function useCarrera(): EstadoCarrera {
  return useSyncExternalStore(
    (f) => tracker.suscribir(f),
    () => tracker.estado
  )
}

let watchId: number | null = null
let reloj: ReturnType<typeof setInterval> | null = null
let wakeLock: WakeLockSentinel | null = null
let errorGps: ((mensaje: string) => void) | null = null

export const geolocalizacionDisponible = () =>
  typeof navigator !== 'undefined' && 'geolocation' in navigator

export function mensajeErrorGps(e: GeolocationPositionError): string {
  if (e.code === e.PERMISSION_DENIED)
    return 'Permiso de ubicación denegado. Actívalo en el navegador para grabar la carrera.'
  if (e.code === e.TIMEOUT) return 'El GPS tarda en responder. Sal a un lugar abierto.'
  return 'No se pudo obtener la ubicación.'
}

async function pedirWakeLock() {
  try {
    if ('wakeLock' in navigator) {
      wakeLock = await navigator.wakeLock.request('screen')
      wakeLock.addEventListener('release', () => {
        wakeLock = null
      })
    }
  } catch {
    // El navegador puede negar el wake lock (por ejemplo con bateria baja); la carrera sigue.
  }
}

function alVolverVisible() {
  if (document.visibilityState === 'visible' && estaActiva(tracker.estado) && !wakeLock)
    void pedirWakeLock()
}

/** Inicia la carrera: GPS con watchPosition, reloj de 1 s y pantalla siempre encendida. */
export function empezarSeguimiento(opciones: { precisa: boolean; onError?: (m: string) => void }) {
  detenerSeguimiento()
  errorGps = opciones.onError ?? null
  tracker.iniciar()
  if (geolocalizacionDisponible()) {
    watchId = navigator.geolocation.watchPosition(
      (pos) =>
        tracker.registrarPunto({
          lat: pos.coords.latitude,
          lng: pos.coords.longitude,
          tiempoMs: pos.timestamp ?? Date.now(),
          precisionM: pos.coords.accuracy ?? 0,
          altitudM: pos.coords.altitude ?? null,
        }),
      (e) => errorGps?.(mensajeErrorGps(e)),
      { enableHighAccuracy: opciones.precisa, maximumAge: 1000, timeout: 20_000 }
    )
  } else {
    errorGps?.('Este navegador no tiene GPS.')
  }
  reloj = setInterval(() => tracker.actualizarTiempo(), 1000)
  void pedirWakeLock()
  document.addEventListener('visibilitychange', alVolverVisible)
}

export function detenerSeguimiento() {
  if (watchId != null && geolocalizacionDisponible()) navigator.geolocation.clearWatch(watchId)
  watchId = null
  if (reloj) clearInterval(reloj)
  reloj = null
  void wakeLock?.release().catch(() => undefined)
  wakeLock = null
  document.removeEventListener('visibilitychange', alVolverVisible)
}

/** Termina la carrera y deja el resultado listo para el resumen. */
export function finalizarCarrera() {
  detenerSeguimiento()
  return tracker.finalizar()
}

export function descartarCarrera() {
  detenerSeguimiento()
  tracker.reiniciar()
}
