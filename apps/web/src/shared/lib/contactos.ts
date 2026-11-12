import type { ContactoEmergencia } from '../kernel/tipos'

/* Contactos de emergencia del SOS: validacion compartida por Ajustes y Seguridad. */

export const MAX_CONTACTOS = 3

/** Deja solo digitos y el + inicial. Numeros moviles de Ecuador (09...) pasan a +5939... */
export function normalizarTelefono(t: string): string {
  const limpio = t
    .trim()
    .replace(/[^\d+]/g, '')
    .replace(/(?!^)\+/g, '')
  if (/^09\d{8}$/.test(limpio)) return `+593${limpio.slice(1)}`
  if (/^5939\d{8}$/.test(limpio)) return `+${limpio}`
  return limpio
}

/** Telefono valido: entre 7 y 15 digitos (E.164). */
export function telefonoValido(t: string): boolean {
  const d = normalizarTelefono(t).replace('+', '')
  return /^\d{7,15}$/.test(d)
}

export function validarContacto(c: ContactoEmergencia): string | null {
  if (c.nombre.trim().length < 2) return 'Escribe el nombre del contacto'
  if (!telefonoValido(c.telefono)) return 'Escribe un teléfono válido, por ejemplo 0991234567'
  return null
}
