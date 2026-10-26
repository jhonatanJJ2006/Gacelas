/** Corredor visible en el mapa en vivo. */
export interface CorredorEnMapa {
  userId: string
  nombre: string
  avatarUrl: string | null
  lat: number
  lng: number
  corriendo: boolean
  actualizadoMs: number
}

export const CADUCIDAD_MS = 2 * 60_000

export const INTERVALO_CORRIENDO_MS = 15_000

export const INTERVALO_QUIETO_MS = 60_000

/** Oculta posiciones de mas de 2 minutos y la propia. */
export const visibles = (lista: CorredorEnMapa[], ahora: number, yo: string | null) =>
  lista.filter((c) => c.userId !== yo && ahora - c.actualizadoMs <= CADUCIDAD_MS)

/** Limite de envio: 15 s corriendo, 60 s quieto; un cambio de estado se publica al momento. */
export function tocaPublicar(
  ultimoMs: number | null,
  ultimoCorriendo: boolean | null,
  ahora: number,
  corriendo: boolean
) {
  if (ultimoMs == null || ultimoCorriendo !== corriendo) return true
  return ahora - ultimoMs >= (corriendo ? INTERVALO_CORRIENDO_MS : INTERVALO_QUIETO_MS)
}
