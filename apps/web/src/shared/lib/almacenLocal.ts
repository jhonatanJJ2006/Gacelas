/** Lectura y escritura en localStorage con el prefijo de la app. */
const PREFIJO = 'gacelas:'

export function leer<T>(clave: string, defecto: T): T {
  try {
    const crudo = localStorage.getItem(PREFIJO + clave)
    return crudo == null ? defecto : (JSON.parse(crudo) as T)
  } catch {
    return defecto
  }
}

export function escribir<T>(clave: string, valor: T): T {
  try {
    localStorage.setItem(PREFIJO + clave, JSON.stringify(valor))
  } catch {
    // Almacenamiento lleno o bloqueado: se mantiene el valor en memoria.
  }
  return valor
}

export const alternar = (lista: string[], id: string) =>
  lista.includes(id) ? lista.filter((x) => x !== id) : [...lista, id]
