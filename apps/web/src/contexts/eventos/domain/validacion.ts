/** Valida el formulario de nuevo evento; devuelve el error o null. */
export function validarEvento(e: {
  titulo: string
  fecha: string
  hora: string
  ubicacion: string
  distanciaKm: string
}): string | null {
  if (e.titulo.trim().length < 3) return 'El título debe tener al menos 3 caracteres'
  if (!e.fecha) return 'Elige la fecha'
  if (!e.hora) return 'Elige la hora'
  if (!e.ubicacion.trim()) return 'Indica el punto de encuentro'
  const km = Number(e.distanciaKm)
  if (!Number.isFinite(km) || km <= 0 || km > 100) return 'La distancia debe estar entre 1 y 100 km'
  return null
}
