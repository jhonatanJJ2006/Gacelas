/** true cuando la web corre instalada como app (PWA): modo standalone o "Agregar a inicio" de iOS. */
export function esAppInstalada(): boolean {
  if (typeof window === 'undefined') return false
  if (
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(display-mode: standalone)').matches
  )
    return true
  return (navigator as Navigator & { standalone?: boolean }).standalone === true
}
