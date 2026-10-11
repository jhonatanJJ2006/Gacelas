import { useEffect, type ReactNode } from 'react'
import { APIProvider, Map, useMap } from '@vis.gl/react-google-maps'
import { CENTRO_LOJA, claveMapas, ESTILO_CLARO, ESTILO_OSCURO, type Coordenada } from '../lib/mapas'
import { useApp } from '../store/app'

/** Dibuja el trazado con google.maps.Polyline y ajusta la vista a los puntos. */
function Trazado({ puntos, seguir }: { puntos: Coordenada[]; seguir: boolean }) {
  const mapa = useMap()
  const oscuro = useApp((s) => s.ajustes.modoOscuro)
  useEffect(() => {
    if (!mapa || puntos.length === 0) return
    const linea = new google.maps.Polyline({
      path: puntos,
      strokeColor: oscuro ? '#F59E0B' : '#D97706',
      strokeWeight: 4,
      strokeOpacity: 0.95,
      map: mapa,
    })
    const ultimo = puntos[puntos.length - 1]
    const marca = new google.maps.Marker({
      position: ultimo,
      map: mapa,
      icon: {
        path: google.maps.SymbolPath.CIRCLE,
        scale: 7,
        fillColor: '#10B981',
        fillOpacity: 1,
        strokeColor: '#ffffff',
        strokeWeight: 2,
      },
    })
    if (seguir) mapa.panTo(ultimo)
    else if (puntos.length > 1) {
      const limites = new google.maps.LatLngBounds()
      puntos.forEach((p) => limites.extend(p))
      mapa.fitBounds(limites, 32)
    }
    return () => {
      linea.setMap(null)
      marca.setMap(null)
    }
  }, [mapa, puntos, seguir, oscuro])
  return null
}

/**
 * Mapa de Google con el recorrido. Sin VITE_GOOGLE_MAPS_API_KEY muestra `alternativa`
 * (el mapa dibujado de siempre), asi la app funciona sin clave y en las pruebas.
 */
export function MapaGoogle({
  puntos,
  alternativa,
  className = '',
  seguir = false,
  zoom = 15,
}: {
  puntos: Coordenada[]
  alternativa: ReactNode
  className?: string
  seguir?: boolean
  zoom?: number
}) {
  const clave = claveMapas()
  const oscuro = useApp((s) => s.ajustes.modoOscuro)
  if (!clave) return <>{alternativa}</>
  return (
    <div className={className} data-testid="mapa-google">
      <APIProvider apiKey={clave} language="es" region="EC">
        <Map
          defaultCenter={puntos[0] ?? CENTRO_LOJA}
          defaultZoom={zoom}
          disableDefaultUI
          gestureHandling="greedy"
          styles={oscuro ? ESTILO_OSCURO : ESTILO_CLARO}
          style={{ width: '100%', height: '100%' }}
        >
          <Trazado puntos={puntos} seguir={seguir} />
        </Map>
      </APIProvider>
    </div>
  )
}
