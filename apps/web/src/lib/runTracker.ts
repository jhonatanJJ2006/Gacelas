import { distanciaPuntos, formatearHms, type PuntoGps } from './geo'
import type { CarreraGuardada, Parcial } from '../data/tipos'

export type FaseCarrera = 'inactiva' | 'corriendo' | 'pausada' | 'terminada'

export interface EstadoCarrera {
  fase: FaseCarrera
  inicioMs: number
  segundos: number
  distanciaKm: number
  desnivelM: number
  puntos: PuntoGps[]
  parciales: Parcial[]
  ultimaPrecisionM: number | null
  ritmoActualSegKm: number | null
}

export const ESTADO_INICIAL: EstadoCarrera = {
  fase: 'inactiva',
  inicioMs: 0,
  segundos: 0,
  distanciaKm: 0,
  desnivelM: 0,
  puntos: [],
  parciales: [],
  ultimaPrecisionM: null,
  ritmoActualSegKm: null,
}

export const estaActiva = (e: EstadoCarrera) => e.fase === 'corriendo' || e.fase === 'pausada'
export const ritmoPromedio = (e: { segundos: number; distanciaKm: number }) =>
  e.distanciaKm > 0.01 ? Math.floor(e.segundos / e.distanciaKm) : null
export const tiempoTexto = (e: EstadoCarrera) => formatearHms(e.segundos)

/** Puntos con peor precision se descartan. */
export const PRECISION_MAX_M = 35
/** Saltos menores se consideran ruido del GPS. */
export const MOVIMIENTO_MIN_M = 2
/** Velocidad maxima creible para una persona corriendo (m/s). */
export const VELOCIDAD_MAX_MS = 12
const VENTANA_RITMO_MS = 30_000

/**
 * Estado de una carrera a partir de puntos GPS reales. No depende del navegador:
 * recibe el reloj por parametro para poder probarlo.
 */
export class RunTracker {
  private estadoActual: EstadoCarrera = ESTADO_INICIAL
  private acumuladoMs = 0
  private desdeMs: number | null = null
  private ultimoValido: PuntoGps | null = null
  private segundosUltimoParcial = 0
  private oyentes = new Set<(e: EstadoCarrera) => void>()

  private reloj: () => number
  private ahora: () => number

  constructor(
    reloj: () => number = () => performance.now(),
    ahora: () => number = () => Date.now()
  ) {
    this.reloj = reloj
    this.ahora = ahora
  }

  get estado() {
    return this.estadoActual
  }

  suscribir(f: (e: EstadoCarrera) => void): () => void {
    this.oyentes.add(f)
    return () => this.oyentes.delete(f)
  }

  private set(cambios: Partial<EstadoCarrera>) {
    this.estadoActual = { ...this.estadoActual, ...cambios }
    this.oyentes.forEach((f) => f(this.estadoActual))
  }

  private ms() {
    return this.acumuladoMs + (this.desdeMs != null ? this.reloj() - this.desdeMs : 0)
  }

  iniciar() {
    this.acumuladoMs = 0
    this.ultimoValido = null
    this.segundosUltimoParcial = 0
    this.desdeMs = this.reloj()
    this.set({ ...ESTADO_INICIAL, fase: 'corriendo', inicioMs: this.ahora() })
  }

  pausar() {
    if (this.estadoActual.fase !== 'corriendo') return
    this.actualizarTiempo()
    this.acumuladoMs = this.ms()
    this.desdeMs = null
    this.set({ fase: 'pausada' })
  }

  reanudar() {
    if (this.estadoActual.fase !== 'pausada') return
    this.desdeMs = this.reloj()
    // Al reanudar no se suma la distancia recorrida durante la pausa.
    this.ultimoValido = null
    this.set({ fase: 'corriendo' })
  }

  alternarPausa() {
    if (this.estadoActual.fase === 'pausada') this.reanudar()
    else this.pausar()
  }

  /** Recalcula los segundos transcurridos; se llama cada segundo desde la UI. */
  actualizarTiempo() {
    if (!estaActiva(this.estadoActual)) return
    const seg = Math.floor(this.ms() / 1000)
    if (seg !== this.estadoActual.segundos) this.set({ segundos: seg })
  }

  registrarPunto(p: PuntoGps) {
    if (this.estadoActual.fase !== 'corriendo') return
    this.actualizarTiempo()
    if (p.precisionM > PRECISION_MAX_M) {
      this.set({ ultimaPrecisionM: p.precisionM })
      return
    }
    const previo = this.ultimoValido
    if (!previo) {
      this.ultimoValido = p
      this.set({ puntos: [...this.estadoActual.puntos, p], ultimaPrecisionM: p.precisionM })
      return
    }
    const d = distanciaPuntos(previo, p)
    const dt = Math.max(1, p.tiempoMs - previo.tiempoMs) / 1000
    if (d < MOVIMIENTO_MIN_M || d / dt > VELOCIDAD_MAX_MS) {
      this.set({ ultimaPrecisionM: p.precisionM })
      return
    }
    this.ultimoValido = p
    const subida =
      previo.altitudM != null && p.altitudM != null ? Math.max(0, p.altitudM - previo.altitudM) : 0
    const actual = this.estadoActual
    const distanciaKm = actual.distanciaKm + d / 1000
    const puntos = [...actual.puntos, p]
    let parciales = actual.parciales
    const automaticos = parciales.filter((x) => !x.manual).length
    if (Math.floor(distanciaKm) > automaticos) {
      parciales = [
        ...parciales,
        {
          numero: automaticos + 1,
          distanciaKm: 1,
          segundos: actual.segundos - this.segundosUltimoParcial,
          manual: false,
        },
      ]
      this.segundosUltimoParcial = actual.segundos
    }
    this.set({
      distanciaKm,
      desnivelM: actual.desnivelM + subida,
      puntos,
      parciales,
      ultimaPrecisionM: p.precisionM,
      ritmoActualSegKm: ritmoVentana(puntos),
    })
  }

  /** Vuelta manual: registra el tramo desde la ultima vuelta manual. */
  vuelta() {
    const e = this.estadoActual
    if (!estaActiva(e)) return
    const manuales = e.parciales.filter((x) => x.manual)
    const distPrev = manuales.reduce((s, x) => s + x.distanciaKm, 0)
    const segPrev = manuales.reduce((s, x) => s + x.segundos, 0)
    this.set({
      parciales: [
        ...e.parciales,
        {
          numero: manuales.length + 1,
          distanciaKm: e.distanciaKm - distPrev,
          segundos: e.segundos - segPrev,
          manual: true,
        },
      ],
    })
  }

  finalizar(): EstadoCarrera {
    this.actualizarTiempo()
    this.acumuladoMs = this.ms()
    this.desdeMs = null
    this.set({ fase: 'terminada' })
    return this.estadoActual
  }

  reiniciar() {
    this.acumuladoMs = 0
    this.desdeMs = null
    this.ultimoValido = null
    this.segundosUltimoParcial = 0
    this.set(ESTADO_INICIAL)
  }

  aCarreraGuardada(
    titulo: string,
    nota: string,
    publica: boolean,
    id: string = crearId()
  ): CarreraGuardada {
    const e = this.estadoActual
    return {
      id,
      titulo: titulo.trim() || tituloPorHora(e.inicioMs || this.ahora()),
      nota: nota.trim(),
      publica,
      inicioMs: e.inicioMs || this.ahora(),
      segundos: e.segundos,
      distanciaKm: e.distanciaKm,
      desnivelM: Math.round(e.desnivelM),
      parciales: e.parciales,
      puntos: e.puntos,
    }
  }
}

function ritmoVentana(puntos: PuntoGps[]): number | null {
  const fin = puntos[puntos.length - 1]
  if (!fin) return null
  const ventana = puntos.filter((p) => fin.tiempoMs - p.tiempoMs <= VENTANA_RITMO_MS)
  if (ventana.length < 2) return null
  let metros = 0
  for (let i = 1; i < ventana.length; i++) metros += distanciaPuntos(ventana[i - 1], ventana[i])
  const seg = (fin.tiempoMs - ventana[0].tiempoMs) / 1000
  if (metros < 5 || seg <= 0) return null
  return Math.floor(seg / (metros / 1000))
}

export function crearId(): string {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

export function tituloPorHora(ms: number): string {
  const h = new Date(ms).getHours()
  if (h < 12) return 'Carrera matutina'
  if (h < 19) return 'Carrera de la tarde'
  return 'Carrera nocturna'
}
