import { useSyncExternalStore } from 'react'
import type { BloqueoPantalla, FuenteUbicacion } from '../domain/puertos'
import { RunTracker, estaActiva, type EstadoCarrera } from '../domain/runTracker'
import {
  geolocalizacionDisponible,
  gpsNavegador,
  mensajeErrorGps,
  pantallaNavegador,
} from '../infrastructure/gpsNavegador'

export { geolocalizacionDisponible, mensajeErrorGps }

/** Carrera en curso compartida por En curso, Cronometro y Navegacion. */
export const tracker = new RunTracker()

export function useCarrera(): EstadoCarrera {
  return useSyncExternalStore(
    (f) => tracker.suscribir(f),
    () => tracker.estado
  )
}

const gps: FuenteUbicacion = gpsNavegador
const pantalla: BloqueoPantalla = pantallaNavegador

let dejarDeObservar: (() => void) | null = null
let reloj: ReturnType<typeof setInterval> | null = null
let errorGps: ((mensaje: string) => void) | null = null

/** Caso de uso EmpezarCarrera: GPS, reloj de 1 s y pantalla siempre encendida. */
export function empezarSeguimiento(opciones: { precisa: boolean; onError?: (m: string) => void }) {
  detenerSeguimiento()
  errorGps = opciones.onError ?? null
  tracker.iniciar()
  if (gps.disponible()) {
    dejarDeObservar = gps.observar({
      precisa: opciones.precisa,
      alPunto: (p) => tracker.registrarPunto(p),
      alError: (m) => errorGps?.(m),
    })
  } else {
    errorGps?.('Este navegador no tiene GPS.')
  }
  reloj = setInterval(() => tracker.actualizarTiempo(), 1000)
  pantalla.activar(() => estaActiva(tracker.estado))
}

export function detenerSeguimiento() {
  dejarDeObservar?.()
  dejarDeObservar = null
  if (reloj) clearInterval(reloj)
  reloj = null
  pantalla.liberar()
}

/** Caso de uso TerminarCarrera: deja el resultado listo para el resumen. */
export function finalizarCarrera() {
  detenerSeguimiento()
  return tracker.finalizar()
}

/** Caso de uso DescartarCarrera. */
export function descartarCarrera() {
  detenerSeguimiento()
  tracker.reiniciar()
}
