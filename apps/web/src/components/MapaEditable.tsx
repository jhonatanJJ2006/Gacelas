import { useEffect } from 'react'
import { APIProvider, Map, useMap, type MapMouseEvent } from '@vis.gl/react-google-maps'
import { MarkerClusterer } from '@googlemaps/markerclusterer'
import { CENTRO_LOJA, claveMapas, ESTILO_OSCURO, type Coordenada } from '../lib/mapas'

function Linea({ puntos }: { puntos: Coordenada[] }) {
  const mapa = useMap()
  useEffect(() => {
    if (!mapa) return
    const linea = new google.maps.Polyline({
      path: puntos,
      strokeColor: '#F59E0B',
      strokeWeight: 4,
      map: mapa,
    })
    const marcas = puntos.map(
      (p, i) =>
        new google.maps.Marker({
          position: p,
          map: mapa,
          icon: {
            path: google.maps.SymbolPath.CIRCLE,
            scale: i === 0 || i === puntos.length - 1 ? 7 : 4,
            fillColor: i === 0 ? '#10B981' : '#F59E0B',
            fillOpacity: 1,
            strokeColor: '#fff',
            strokeWeight: 2,
          },
        })
    )
    return () => {
      linea.setMap(null)
      marcas.forEach((m) => m.setMap(null))
    }
  }, [mapa, puntos])
  return null
}

/** Mapa para dibujar una ruta tocando puntos. */
export function MapaEditable({
  puntos,
  onTocar,
  className = '',
}: {
  puntos: Coordenada[]
  onTocar: (p: Coordenada) => void
  className?: string
}) {
  const clave = claveMapas()
  if (!clave)
    return (
      <div
        className={
          className + ' flex items-center justify-center text-text-secondary p-6 text-center'
        }
      >
        Dibujar rutas necesita Google Maps configurado.
      </div>
    )
  return (
    <div className={className} data-testid="mapa-editable">
      <APIProvider apiKey={clave} language="es" region="EC">
        <Map
          defaultCenter={puntos[0] ?? CENTRO_LOJA}
          defaultZoom={15}
          disableDefaultUI
          clickableIcons={false}
          gestureHandling="greedy"
          styles={ESTILO_OSCURO}
          style={{ width: '100%', height: '100%' }}
          onClick={(e: MapMouseEvent) => {
            const ll = e.detail.latLng
            if (ll) onTocar({ lat: +ll.lat.toFixed(6), lng: +ll.lng.toFixed(6) })
          }}
        >
          <Linea puntos={puntos} />
        </Map>
      </APIProvider>
    </div>
  )
}

function Agrupados({
  rutas,
  onAbrir,
}: {
  rutas: { id: string; nombre: string; salida: Coordenada }[]
  onAbrir: (id: string) => void
}) {
  const mapa = useMap()
  useEffect(() => {
    if (!mapa || !rutas.length) return
    const marcas = rutas.map((r) => {
      const m = new google.maps.Marker({ position: r.salida, title: r.nombre })
      m.addListener('click', () => onAbrir(r.id))
      return m
    })
    const grupo = new MarkerClusterer({ map: mapa, markers: marcas })
    const limites = new google.maps.LatLngBounds()
    rutas.forEach((r) => limites.extend(r.salida))
    mapa.fitBounds(limites, 48)
    return () => {
      grupo.clearMarkers()
      grupo.setMap(null)
    }
  }, [mapa, rutas, onAbrir])
  return null
}

/** Mapa de exploracion: salida de cada ruta, agrupadas en clusters. */
export function MapaRutas({
  rutas,
  onAbrir,
  className = '',
}: {
  rutas: { id: string; nombre: string; salida: Coordenada }[]
  onAbrir: (id: string) => void
  className?: string
}) {
  const clave = claveMapas()
  if (!clave)
    return <p className="text-text-secondary p-4">El mapa necesita Google Maps configurado.</p>
  return (
    <div className={className} data-testid="mapa-rutas">
      <APIProvider apiKey={clave} language="es" region="EC">
        <Map
          defaultCenter={CENTRO_LOJA}
          defaultZoom={12}
          disableDefaultUI
          gestureHandling="greedy"
          styles={ESTILO_OSCURO}
          style={{ width: '100%', height: '100%' }}
        >
          <Agrupados rutas={rutas} onAbrir={onAbrir} />
        </Map>
      </APIProvider>
    </div>
  )
}
