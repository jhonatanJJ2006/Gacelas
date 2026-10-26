import { useEffect, useState } from 'react'

/** Igual que core/sensores/Rumbo.kt de Android: brujula quieto o lento, curso GPS corriendo. */
export const VEL_SENSOR_MS = 0.6
export const VEL_GPS_MS = 1.0

export const normalizar = (g: number) => ((g % 360) + 360) % 360

export function diferencia(a: number, b: number) {
  const d = normalizar(b - a)
  return d > 180 ? d - 360 : d
}

export const mezclar = (a: number, b: number, t: number) =>
  normalizar(a + diferencia(a, b) * Math.min(1, Math.max(0, t)))

export function pesoGps(velocidadMs: number | null | undefined) {
  if (velocidadMs == null) return 0
  return Math.min(1, Math.max(0, (velocidadMs - VEL_SENSOR_MS) / (VEL_GPS_MS - VEL_SENSOR_MS)))
}

export function combinar(
  sensor: number | null,
  gps: number | null,
  velocidadMs: number | null | undefined
): number | null {
  if (sensor == null) return gps
  if (gps == null) return sensor
  return mezclar(sensor, gps, pesoGps(velocidadMs))
}

type EventoOrientacion = DeviceOrientationEvent & { webkitCompassHeading?: number }

/** Rumbo de la brujula del telefono (grados desde el norte) o null si no hay sensor. */
export function useRumboSensor(activo: boolean): number | null {
  const [rumbo, setRumbo] = useState<number | null>(null)
  useEffect(() => {
    if (!activo || typeof window === 'undefined') return
    let previo: number | null = null
    const manejar = (e: Event) => {
      const ev = e as EventoOrientacion
      const crudo =
        ev.webkitCompassHeading != null
          ? ev.webkitCompassHeading
          : ev.absolute && ev.alpha != null
            ? 360 - ev.alpha
            : null
      if (crudo == null) return
      const g = normalizar(crudo + (screen.orientation?.angle ?? 0))
      previo = previo == null ? g : mezclar(previo, g, 0.25)
      setRumbo(Math.round(previo))
    }
    const tipo =
      'ondeviceorientationabsolute' in window ? 'deviceorientationabsolute' : 'deviceorientation'
    window.addEventListener(tipo, manejar)
    return () => {
      window.removeEventListener(tipo, manejar)
      setRumbo(null)
    }
  }, [activo])
  return rumbo
}
