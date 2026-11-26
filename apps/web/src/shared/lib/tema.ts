/**
 * Tema de la app (claro, oscuro o el del sistema). La preferencia vive en `Ajustes.tema`
 * (localStorage y, con sesion, user_settings.prefs); aqui se resuelve a claro u oscuro, se aplica
 * al documento y se avisa a quien dibuja con colores propios (mapas, graficos).
 */
import type { ModoTema } from '../kernel/tipos'

export type { ModoTema }
export type TemaResuelto = 'claro' | 'oscuro'

export const MODOS_TEMA: readonly ModoTema[] = ['claro', 'oscuro', 'sistema']
export const TEMA_POR_DEFECTO: ModoTema = 'oscuro'

/** Colores de la barra del navegador y del lienzo segun el tema (coinciden con --c-bg-primary). */
export const COLOR_BARRA: Record<TemaResuelto, string> = { claro: '#F3F3F7', oscuro: '#0C1322' }

const CONSULTA_OSCURO = '(prefers-color-scheme: dark)'

export function esModoTema(v: unknown): v is ModoTema {
  return v === 'claro' || v === 'oscuro' || v === 'sistema'
}

/** Normaliza un valor guardado (puede venir de otra version o de Supabase). */
export function normalizarTema(v: unknown): ModoTema {
  return esModoTema(v) ? v : TEMA_POR_DEFECTO
}

export function sistemaPrefiereOscuro(): boolean {
  try {
    return typeof matchMedia === 'function' ? matchMedia(CONSULTA_OSCURO).matches : true
  } catch {
    return true
  }
}

export function resolverTema(
  modo: ModoTema,
  sistemaOscuro = sistemaPrefiereOscuro()
): TemaResuelto {
  if (modo === 'sistema') return sistemaOscuro ? 'oscuro' : 'claro'
  return modo
}

type Oyente = (t: TemaResuelto) => void
const oyentes = new Set<Oyente>()
let actual: TemaResuelto =
  typeof document !== 'undefined' && document.documentElement.classList.contains('tema-claro')
    ? 'claro'
    : 'oscuro'

/** Tema aplicado ahora mismo al documento. */
export const temaActual = (): TemaResuelto => actual

/** Aplica el tema al documento: clases `dark` / `tema-claro`, color-scheme y color de la barra. */
export function aplicarTema(resuelto: TemaResuelto, doc: Document = document): void {
  const raiz = doc.documentElement
  raiz.classList.toggle('tema-claro', resuelto === 'claro')
  raiz.classList.toggle('dark', resuelto === 'oscuro')
  raiz.style.colorScheme = resuelto === 'claro' ? 'light' : 'dark'
  raiz.dataset.tema = resuelto
  doc
    .querySelectorAll('meta[name="theme-color"]')
    .forEach((m) => m.setAttribute('content', COLOR_BARRA[resuelto]))
  doc
    .querySelector('meta[name="color-scheme"]')
    ?.setAttribute('content', resuelto === 'claro' ? 'light' : 'dark')
  if (actual !== resuelto) {
    actual = resuelto
    oyentes.forEach((o) => o(resuelto))
  }
}

/** Para useSyncExternalStore: suscripcion a los cambios del tema resuelto. */
export function suscribirTema(o: Oyente): () => void {
  oyentes.add(o)
  return () => oyentes.delete(o)
}

/** Sigue los cambios del tema del sistema mientras el modo sea `sistema`. Devuelve la baja. */
export function seguirSistema(alCambiar: (oscuro: boolean) => void): () => void {
  if (typeof matchMedia !== 'function') return () => {}
  const mq = matchMedia(CONSULTA_OSCURO)
  const f = (e: MediaQueryListEvent) => alCambiar(e.matches)
  mq.addEventListener('change', f)
  return () => mq.removeEventListener('change', f)
}
