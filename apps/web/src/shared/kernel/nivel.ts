/** Nivel del corredor segun km acumulados (misma regla que Android NivelCorredor). */
export function nivelCorredor(km: number): string {
  if (km >= 500) return 'Gacela Élite'
  if (km >= 150) return 'Gacela Avanzada'
  if (km >= 30) return 'Gacela Intermedia'
  return 'Gacela Principiante'
}
