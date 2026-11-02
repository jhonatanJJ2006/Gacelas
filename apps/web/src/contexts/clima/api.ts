// Fachada publica del contexto Clima (chip de Inicio, datos de Open-Meteo).
export * from './domain/clima'
export { climaActual, leerCacheClima } from './infrastructure/climaOpenMeteo'
export { useClima } from './ui/useClima'
