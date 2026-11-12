import { create } from 'zustand'
import { ENVIO_CADA_MS } from '../domain/sos'
import { enviarPosicionSos, iniciarSos, terminarSos } from '../infrastructure/sosRemoto'

type Pos = { lat: number; lng: number }

interface EstadoSos {
  alertaId: string | null
  activando: boolean
  error: string | null
  ultimaPos: Pos | null
  ultimoEnvioMs: number
  activar(pos: Pos | null): Promise<string | null>
  posicion(pos: Pos): void
  terminar(): Promise<void>
}

/** Caso de uso del SOS: una alerta activa a la vez, posicion cada ENVIO_CADA_MS. */
export const useSos = create<EstadoSos>((set, get) => ({
  alertaId: null,
  activando: false,
  error: null,
  ultimaPos: null,
  ultimoEnvioMs: 0,
  async activar(pos) {
    if (get().alertaId) return get().alertaId
    set({ activando: true, error: null, ultimaPos: pos })
    try {
      const id = await iniciarSos(pos)
      set({ alertaId: id, activando: false, ultimoEnvioMs: Date.now() })
      if (!id) set({ error: 'Sin conexión con el servidor: llama al 911 o comparte tu ubicación.' })
      return id
    } catch (e) {
      set({ activando: false, error: e instanceof Error ? e.message : 'No se pudo activar el SOS' })
      return null
    }
  },
  posicion(pos) {
    set({ ultimaPos: pos })
    const { alertaId, ultimoEnvioMs } = get()
    if (!alertaId || Date.now() - ultimoEnvioMs < ENVIO_CADA_MS) return
    set({ ultimoEnvioMs: Date.now() })
    void enviarPosicionSos(alertaId, pos)
  },
  async terminar() {
    const id = get().alertaId
    set({ alertaId: null, error: null })
    if (id) await terminarSos(id).catch(() => undefined)
  },
}))
