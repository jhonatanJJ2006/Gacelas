/** Nivel del corredor segun km acumulados (misma regla que Android NivelCorredor). */
export function nivelCorredor(km: number): string {
  if (km >= 500) return 'Gacela Élite'
  if (km >= 150) return 'Gacela Avanzada'
  if (km >= 30) return 'Gacela Intermedia'
  return 'Gacela Principiante'
}

const UMBRALES_NIVEL = [
  { km: 0, nombre: 'Gacela Principiante' },
  { km: 30, nombre: 'Gacela Intermedia' },
  { km: 150, nombre: 'Gacela Avanzada' },
  { km: 500, nombre: 'Gacela Élite' },
]

export interface ProgresoNivel {
  nivel: string
  /** Nivel siguiente o null en el maximo. */
  siguiente: string | null
  faltanKm: number
  /** 0-100 dentro del tramo actual (100 en el maximo). */
  pct: number
}

/** Progreso al siguiente nivel con los mismos umbrales en km acumulados de nivelCorredor. */
export function progresoNivel(km: number): ProgresoNivel {
  let i = 0
  while (i + 1 < UMBRALES_NIVEL.length && km >= UMBRALES_NIVEL[i + 1]!.km) i++
  const actual = UMBRALES_NIVEL[i]!
  const sig = UMBRALES_NIVEL[i + 1]
  if (!sig) return { nivel: actual.nombre, siguiente: null, faltanKm: 0, pct: 100 }
  return {
    nivel: actual.nombre,
    siguiente: sig.nombre,
    faltanKm: Math.max(0, sig.km - km),
    pct: Math.min(100, ((km - actual.km) / (sig.km - actual.km)) * 100),
  }
}
