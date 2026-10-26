import type { PuntoGps } from '../../../shared/lib/geo'

/** Puerto de salida: fuente de posiciones GPS de la plataforma (navegador, Android, iOS). */
export interface FuenteUbicacion {
  disponible(): boolean
  /** Empieza a observar posiciones. Devuelve la funcion para dejar de observar. */
  observar(opciones: {
    precisa: boolean
    alPunto: (p: PuntoGps) => void
    alError: (mensaje: string) => void
  }): () => void
}

/** Puerto de salida: mantiene la pantalla encendida mientras dura la carrera. */
export interface BloqueoPantalla {
  /** Pide el bloqueo y lo renueva al volver a la app mientras `activa()` sea true. */
  activar(activa: () => boolean): void
  liberar(): void
}
