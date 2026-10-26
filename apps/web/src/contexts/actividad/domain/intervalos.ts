export interface ConfigIntervalos {
  trabajoSeg: number
  descansoSeg: number
  rondas: number
  vibracion: boolean
  sonido: boolean
}

export type FaseIntervalo = 'trabajo' | 'descanso' | 'terminado'

export interface EstadoIntervalo {
  fase: FaseIntervalo
  ronda: number
  restanteSeg: number
}

/** Duracion total: la ultima ronda no lleva descanso. */
export const duracionTotalSeg = (c: ConfigIntervalos) =>
  c.rondas * (c.trabajoSeg + c.descansoSeg) - c.descansoSeg

export function estadoIntervalo(c: ConfigIntervalos, transcurridoSeg: number): EstadoIntervalo {
  if (c.rondas <= 0 || transcurridoSeg >= duracionTotalSeg(c))
    return { fase: 'terminado', ronda: c.rondas, restanteSeg: 0 }
  const ciclo = c.trabajoSeg + c.descansoSeg
  const ronda = Math.floor(transcurridoSeg / ciclo) + 1
  const enCiclo = transcurridoSeg % ciclo
  return enCiclo < c.trabajoSeg
    ? { fase: 'trabajo', ronda, restanteSeg: c.trabajoSeg - enCiclo }
    : { fase: 'descanso', ronda, restanteSeg: ciclo - enCiclo }
}

/** Intervalo entre ticks del metronomo en milisegundos (40 a 240 BPM). */
export const intervaloMetronomoMs = (bpm: number) =>
  Math.floor(60_000 / Math.min(240, Math.max(40, bpm)))
