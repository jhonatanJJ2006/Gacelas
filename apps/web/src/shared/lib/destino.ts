/**
 * Destino tras iniciar sesion. Si alguien abre una pantalla protegida sin sesion, se guarda la
 * ruta y, al entrar (correo o Google), se vuelve ahi. Sin destino se va a Inicio.
 * Se usa sessionStorage para que sobreviva a la redireccion de Google.
 */

const CLAVE = 'gacelas.destino'
export const DESTINO_POR_DEFECTO = '/inicio'

/** Rutas de acceso o publicas: nunca son destino tras entrar. */
const NO_DESTINO =
  /^\/(bienvenida|inicio-sesion|registro|recuperar-contrasena|nueva-contrasena|legal\/)?$/

type Almacen = Pick<Storage, 'getItem' | 'setItem' | 'removeItem'>

function almacen(): Almacen | null {
  try {
    return window.sessionStorage
  } catch {
    return null
  }
}

/** Ruta interna valida como destino (sin dominio, sin rutas de acceso). */
export function destinoValido(ruta: string | null | undefined): string | null {
  if (!ruta || !ruta.startsWith('/') || ruta.startsWith('//')) return null
  const camino = ruta.split(/[?#]/)[0]
  if (NO_DESTINO.test(camino) || camino.startsWith('/legal/')) return null
  return ruta
}

let ignorarSiguiente = false

/** Al cerrar sesion no se guarda la pantalla actual: la proxima vez se entra a Inicio. */
export function olvidarDestino(a: Almacen | null = almacen()) {
  ignorarSiguiente = true
  try {
    a?.removeItem(CLAVE)
  } catch {
    /* nada */
  }
}

export function guardarDestino(ruta: string, a: Almacen | null = almacen()) {
  if (ignorarSiguiente) {
    ignorarSiguiente = false
    return
  }
  const v = destinoValido(ruta)
  try {
    if (v) a?.setItem(CLAVE, v)
  } catch {
    /* almacenamiento bloqueado */
  }
}

/** Destino guardado sin borrarlo (para el redirectTo de Google). */
export function verDestino(a: Almacen | null = almacen()): string {
  try {
    return destinoValido(a?.getItem(CLAVE)) ?? DESTINO_POR_DEFECTO
  } catch {
    return DESTINO_POR_DEFECTO
  }
}

/** Devuelve el destino guardado y lo borra. */
export function tomarDestino(a: Almacen | null = almacen()): string {
  const d = verDestino(a)
  try {
    a?.removeItem(CLAVE)
  } catch {
    /* nada */
  }
  return d
}
