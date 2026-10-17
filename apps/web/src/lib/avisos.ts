/** Sonidos con Web Audio y vibracion para el cronometro, intervalos y metronomo. */
let contexto: AudioContext | null = null

function audio(): AudioContext | null {
  if (typeof window === 'undefined') return null
  const Ctor =
    window.AudioContext ??
    (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
  if (!Ctor) return null
  contexto ??= new Ctor()
  if (contexto.state === 'suspended') void contexto.resume()
  return contexto
}

export function tono(frecuencia = 880, duracionMs = 60, volumen = 0.3) {
  const ctx = audio()
  if (!ctx) return
  const osc = ctx.createOscillator()
  const gan = ctx.createGain()
  osc.frequency.value = frecuencia
  osc.type = 'square'
  gan.gain.setValueAtTime(volumen, ctx.currentTime)
  gan.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + duracionMs / 1000)
  osc.connect(gan).connect(ctx.destination)
  osc.start()
  osc.stop(ctx.currentTime + duracionMs / 1000)
}

export const tick = (acento = false) => tono(acento ? 1320 : 990, 40)

export function vibrar(patron: number | number[]) {
  if (typeof navigator !== 'undefined' && 'vibrate' in navigator) navigator.vibrate(patron)
}

/** Aviso de cambio de fase en intervalos. */
export function cambioFase(sonido: boolean, vibracion: boolean) {
  if (sonido) {
    tono(660, 150)
    setTimeout(() => tono(990, 200), 180)
  }
  if (vibracion) vibrar([200, 100, 200])
}
