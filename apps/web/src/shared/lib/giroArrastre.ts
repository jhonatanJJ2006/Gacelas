/**
 * Giro del corredor 3D de Inicio: gira solo sobre su eje vertical, el arrastre horizontal lo mueve y al
 * soltar sigue con una inercia que se frena hasta volver al giro automatico. Misma logica que
 * GiroArrastre.kt (Android e iOS). Funciones puras para poder probarlas sin WebGL.
 */
export interface GiroEstado {
  angulo: number
  inercia: number
  arrastrando: boolean
}

export const GIRO_AUTO = 18
export const GRADOS_POR_PX = 0.4
export const FRENADO = 4
export const INERCIA_MAX = 720

export function nuevoGiro(): GiroEstado {
  return { angulo: 0, inercia: 0, arrastrando: false }
}

function normalizar(a: number): number {
  const r = a % 360
  return r < 0 ? r + 360 : r
}

export function empezarArrastre(g: GiroEstado): GiroEstado {
  return { ...g, arrastrando: true, inercia: 0 }
}

export function arrastrar(g: GiroEstado, dxPx: number): GiroEstado {
  return { ...g, angulo: normalizar(g.angulo + dxPx * GRADOS_POR_PX) }
}

/** velPx en px/s al soltar. */
export function soltar(g: GiroEstado, velPx: number): GiroEstado {
  const v = velPx * GRADOS_POR_PX
  return { ...g, arrastrando: false, inercia: Math.max(-INERCIA_MAX, Math.min(INERCIA_MAX, v)) }
}

export function avanzar(g: GiroEstado, dt: number, auto = GIRO_AUTO): GiroEstado {
  if (g.arrastrando) return g
  const angulo = normalizar(g.angulo + (auto + g.inercia) * dt)
  let inercia = g.inercia * Math.exp(-FRENADO * dt)
  if (Math.abs(inercia) < 1) inercia = 0
  return { ...g, angulo, inercia }
}
