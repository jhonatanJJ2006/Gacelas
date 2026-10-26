import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { haversineM } from '../lib/geo'
import { rumboReciente, tieneMapIdPropio, tramosPorRitmo } from '../lib/mapaEstilo'
import { type Coordenada } from '../lib/mapas'
import { combinar, useRumboSensor } from '../lib/rumbo'
import { Corredor3DHtml } from './mapa/Corredor3DHtml'
import { Corredor3DMapa } from './mapa/Corredor3DMapa'
import {
  BotonSeguir,
  MapaBase,
  Pin,
  Puck,
  RutaEstilizada,
  RutaPlan,
  SeguirCamara,
} from './mapa/MapaBase'

type PuntoMapa = Coordenada & { tiempoMs?: number }

/**
 * Mapa de Gacelas con el recorrido (estilo propio, colores por ritmo, flechas, pines y "estas aqui").
 * `seguir` = camara de seguimiento; `modo3d` = vectorial inclinado con el corredor 3D animado en el mapa.
 * Sin VITE_GOOGLE_MAPS_API_KEY muestra `alternativa`.
 */
export function MapaGoogle({
  puntos,
  alternativa,
  className = '',
  seguir = false,
  zoom = 15,
  modo3d = false,
  ritmoSegKm = null,
  controles = true,
  hijos,
  plan,
  adelanteM,
  recentrar = 0,
}: {
  puntos: PuntoMapa[]
  alternativa: ReactNode
  className?: string
  seguir?: boolean
  zoom?: number
  modo3d?: boolean
  ritmoSegKm?: number | null
  controles?: boolean
  hijos?: ReactNode
  /** Ruta planificada (navegacion). */
  plan?: Coordenada[]
  adelanteM?: number
  /** Cambia el valor para volver a seguir al corredor (boton propio de la pantalla). */
  recentrar?: number
}) {
  const tramos = useMemo(() => tramosPorRitmo(puntos), [puntos])
  const ultimo = puntos[puntos.length - 1] ?? null
  const rumboGps = useMemo(() => rumboReciente(puntos), [puntos])
  const rumboSensor = useRumboSensor(seguir)
  const ultimoPunto = puntos[puntos.length - 1]
  const penultimo = puntos[puntos.length - 2]
  const velocidad =
    ultimoPunto?.tiempoMs != null &&
    penultimo?.tiempoMs != null &&
    ultimoPunto.tiempoMs > penultimo.tiempoMs
      ? haversineM(penultimo.lat, penultimo.lng, ultimoPunto.lat, ultimoPunto.lng) /
        ((ultimoPunto.tiempoMs - penultimo.tiempoMs) / 1000)
      : null
  const rumbo = combinar(rumboSensor, rumboGps ?? null, velocidad)
  const [siguiendo, setSiguiendo] = useState(true)
  const [sin3d, setSin3d] = useState(false)
  useEffect(() => {
    if (recentrar) setSiguiendo(true)
  }, [recentrar])
  const dejar = useCallback(() => setSiguiendo(false), [])
  const fallo3d = useCallback(() => setSin3d(true), [])
  const con3d = modo3d && !sin3d
  const vector = modo3d && tieneMapIdPropio()
  return (
    <MapaBase
      centro={puntos[0]}
      zoom={zoom}
      modo3d={modo3d}
      className={className}
      alternativa={alternativa}
      controles={controles}
    >
      {plan && <RutaPlan camino={plan} />}
      <RutaEstilizada tramos={tramos} ajustar={!seguir} />
      {!seguir && puntos.length > 1 && <Pin pos={puntos[0]} tipo="inicio" />}
      {!seguir && puntos.length > 1 && <Pin pos={puntos[puntos.length - 1]} tipo="meta" />}
      {seguir &&
        ultimo &&
        (con3d && vector ? (
          <Corredor3DMapa pos={ultimo} rumbo={rumbo} ritmoSegKm={ritmoSegKm} onFallo={fallo3d} />
        ) : modo3d ? (
          <Corredor3DHtml pos={ultimo} rumbo={rumbo} ritmoSegKm={ritmoSegKm} />
        ) : (
          <Puck pos={ultimo} rumbo={rumbo} />
        ))}
      {seguir && (
        <SeguirCamara
          pos={ultimo}
          rumbo={rumbo}
          modo3d={vector}
          siguiendo={siguiendo}
          adelanteM={adelanteM}
          onDejarDeSeguir={dejar}
        />
      )}
      {seguir && controles && (
        <BotonSeguir siguiendo={siguiendo} onSeguir={() => setSiguiendo(true)} />
      )}
      {hijos}
    </MapaBase>
  )
}
