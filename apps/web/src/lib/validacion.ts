/** Reglas de validacion de los formularios de acceso. Devuelven el mensaje de error o null. */
const REGEX_EMAIL = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

export const ValidacionAuth = {
  email(email: string): string | null {
    if (!email.trim()) return 'Ingresa tu correo electrónico'
    if (!REGEX_EMAIL.test(email.trim())) return 'El correo no es válido'
    return null
  },
  password(password: string): string | null {
    if (password.length < 8) return 'La contraseña debe tener mínimo 8 caracteres'
    if (!/[A-Za-z]/.test(password) || !/\d/.test(password)) return 'Usa letras y números'
    return null
  },
  nombre: (nombre: string): string | null =>
    nombre.trim().length < 2 ? 'Ingresa tu nombre' : null,
  usernameDesde(nombre: string, email: string): string {
    const base = nombre
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9 ]/g, '')
      .trim()
      .replace(/\s+/g, '_')
    return (base || email.split('@')[0]).slice(0, 20)
  },
}
