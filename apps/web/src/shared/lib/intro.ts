/**
 * Intro de marca "Ruta y destello v2b" (docs/MARCA.md): regla de cuando se muestra.
 * La animacion es un video pre-renderizado (public/marca/intro.webm y intro.mp4); el mismo MP4 va en Android.
 */

export const DURACION_INTRO_S = 2.2
export const CLAVE_INTRO = 'gacelas.intro.vista'
/** Ausencia minima para repetir la intro al volver a abrir la app: 30 s (igual que Android). */
export const UMBRAL_AUSENCIA_MS = 30_000

type Almacen = Pick<Storage, 'getItem' | 'setItem'>

/**
 * Se muestra cada vez que se abre la app (carga de la pagina o de la PWA), salvo que se haya
 * salido hace menos de UMBRAL_AUSENCIA_MS (recarga o cambio rapido). Nunca con
 * prefers-reduced-motion. CLAVE_INTRO guarda la ultima salida (ms epoch).
 */
export function debeMostrarIntro(
  almacen: Almacen | null,
  reducirMovimiento: boolean,
  ahora: number = Date.now()
): boolean {
  if (reducirMovimiento) return false
  try {
    const v = almacen?.getItem(CLAVE_INTRO)
    if (v == null) return true
    const salida = Number(v)
    return Number.isFinite(salida) && ahora - salida >= UMBRAL_AUSENCIA_MS
  } catch {
    return false
  }
}

/** Guarda el instante de la ultima vez que se vio o se dejo la app. */
export function marcarIntroVista(almacen: Almacen | null, ahora: number = Date.now()) {
  try {
    almacen?.setItem(CLAVE_INTRO, String(ahora))
  } catch {
    /* almacenamiento bloqueado: la intro simplemente no se recuerda */
  }
}

/** Si el video no muestra su primer cuadro en este tiempo, la intro se cierra (fallback). */
export const ESPERA_PRIMER_CUADRO_MS = 1_000
