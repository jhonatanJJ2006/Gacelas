/** Hace cuanto (texto corto) para listas de actividad. */
export function haceTiempo(ms: number, ahora = Date.now()): string {
  const min = Math.max(0, Math.round((ahora - ms) / 60000))
  if (min < 1) return 'Ahora'
  if (min < 60) return `Hace ${min} min`
  const h = Math.round(min / 60)
  if (h < 24) return `Hace ${h} h`
  const d = Math.round(h / 24)
  return d === 1 ? 'Ayer' : `Hace ${d} días`
}
