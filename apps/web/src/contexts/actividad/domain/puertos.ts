import type { Racha } from '../../../shared/kernel/rachas'
import { type CarreraGuardada } from '../../../shared/kernel/tipos'
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

export interface CarrerasRepository {
  listar(): Promise<CarreraGuardada[]>
  guardar(c: CarreraGuardada): Promise<CarreraGuardada[]>
  eliminar(id: string): Promise<CarreraGuardada[]>
}

export interface RachasRepository {
  /** Racha calculada en el servidor, o null si no se pudo leer. */
  leer(): Promise<Racha | null>
}
