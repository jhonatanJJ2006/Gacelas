/** Estimacion simple: alrededor de 1 kcal por kg y km, con 70 kg de referencia. */
export const caloriasEstimadas = (km: number, pesoKg = 70) => Math.round(km * pesoKg)

export function calidadSenal(precisionM: number) {
  if (precisionM <= 10) return 'Señal fuerte'
  if (precisionM <= 30) return 'Señal media'
  return 'Señal débil'
}
