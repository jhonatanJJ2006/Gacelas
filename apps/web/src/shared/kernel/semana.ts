export const SEMANA_MS = 7 * 24 * 3600 * 1000

/** Lunes 00:00 de la semana de `ahora` (hora local). */
export function inicioSemana(ahora: number) {
  const d = new Date(ahora)
  const dia = (d.getDay() + 6) % 7
  d.setHours(0, 0, 0, 0)
  return d.getTime() - dia * 24 * 3600 * 1000
}
