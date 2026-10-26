import type { BloqueoPantalla, FuenteUbicacion } from '../domain/puertos'

/** Adaptadores del navegador para el flujo de carrera: Geolocation API y Wake Lock API. */

export const geolocalizacionDisponible = () =>
  typeof navigator !== 'undefined' && 'geolocation' in navigator

export function mensajeErrorGps(e: GeolocationPositionError): string {
  if (e.code === e.PERMISSION_DENIED)
    return 'Permiso de ubicación denegado. Actívalo en el navegador para grabar la carrera.'
  if (e.code === e.TIMEOUT) return 'El GPS tarda en responder. Sal a un lugar abierto.'
  return 'No se pudo obtener la ubicación.'
}

export const gpsNavegador: FuenteUbicacion = {
  disponible: geolocalizacionDisponible,
  observar({ precisa, alPunto, alError }) {
    const id = navigator.geolocation.watchPosition(
      (pos) =>
        alPunto({
          lat: pos.coords.latitude,
          lng: pos.coords.longitude,
          tiempoMs: pos.timestamp ?? Date.now(),
          precisionM: pos.coords.accuracy ?? 0,
          altitudM: pos.coords.altitude ?? null,
        }),
      (e) => alError(mensajeErrorGps(e)),
      { enableHighAccuracy: precisa, maximumAge: 1000, timeout: 20_000 }
    )
    return () => {
      if (geolocalizacionDisponible()) navigator.geolocation.clearWatch(id)
    }
  },
}

let wakeLock: WakeLockSentinel | null = null
let sigueActiva: () => boolean = () => false

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
  if (document.visibilityState === 'visible' && sigueActiva() && !wakeLock) void pedirWakeLock()
}

export const pantallaNavegador: BloqueoPantalla = {
  activar(activa) {
    sigueActiva = activa
    void pedirWakeLock()
    document.addEventListener('visibilitychange', alVolverVisible)
  },
  liberar() {
    void wakeLock?.release().catch(() => undefined)
    wakeLock = null
    document.removeEventListener('visibilitychange', alVolverVisible)
  },
}
