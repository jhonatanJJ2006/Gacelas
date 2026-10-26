/** Traduce los mensajes de error de Supabase a textos para el usuario. */
export function traducirError(mensaje: string): string {
  const m = mensaje.toLowerCase()
  if (m.includes('invalid login')) return 'Correo o contraseña incorrectos'
  if (m.includes('already registered') || m.includes('already been registered'))
    return 'Ya existe una cuenta con ese correo'
  if (m.includes('not confirmed')) return 'Confirma tu correo antes de iniciar sesión'
  if (m.includes('rate limit') || m.includes('security purposes'))
    return 'Demasiados intentos. Espera un momento y vuelve a probar'
  if (m.includes('fetch') || m.includes('network')) return 'Sin conexión con el servidor'
  if (m.includes('password')) return 'La contraseña no es válida'
  return 'No se pudo completar. Intenta de nuevo'
}
