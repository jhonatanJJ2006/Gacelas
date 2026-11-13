// Fachada publica del contexto Clima (chip de Inicio, datos de Open-Meteo).
export * from './domain/clima'
export {
  climaActual,
  leerCacheClima,
  reiniciarIntentosClima,
} from './infrastructure/climaOpenMeteo'
export { useClima, useEstadoClima, type EstadoClima } from './ui/useClima'
