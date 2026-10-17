/** Devuelve true si el navegador puede crear un contexto WebGL. */
export function isWebGLAvailable(): boolean {
  try {
    if (typeof window === 'undefined' || typeof document === 'undefined') return false
    const canvas = document.createElement('canvas')
    const gl = canvas.getContext('webgl2') || canvas.getContext('webgl')
    return !!(window.WebGLRenderingContext && gl)
  } catch {
    return false
  }
}
