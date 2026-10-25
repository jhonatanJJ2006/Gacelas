import type { RegistroDenuncias } from '../application/puertos'

const CLAVE = 'gacelas:reportes'

export const registroLocal: RegistroDenuncias = {
  contiene: (c) => leer().includes(c),
  agregar: (c) => localStorage.setItem(CLAVE, JSON.stringify([...leer(), c])),
}

function leer(): string[] {
  try {
    return JSON.parse(localStorage.getItem(CLAVE) ?? '[]') as string[]
  } catch {
    return []
  }
}
