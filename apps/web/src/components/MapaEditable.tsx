import { useEffect } from 'react'
import { Controles } from './mapa/MapaBase'
import { ESTILO_GACELAS } from '../lib/mapaEstilo'
import { APIProvider, Map, useMap, type MapMouseEvent } from '@vis.gl/react-google-maps'
import { MarkerClusterer } from '@googlemaps/markerclusterer'
import { colorCorredor } from '../lib/eventos'
import { CENTRO_LOJA, claveMapas, type Coordenada } from '../lib/mapas'

function Linea({ puntos }: { puntos: Coordenada[] }) {
  const mapa = useMap()
  useEffect(() => {
    if (!mapa) return
    const linea = new google.maps.Polyline({
      path: puntos,
      strokeColor: '#A855F7',
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
            fillColor: i === 0 ? '#10B981' : '#A855F7',
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
    <div className={`relative ${className}`} data-testid="mapa-editable">
      <APIProvider apiKey={clave} language="es" region="EC">
        <Map
          defaultCenter={puntos[0] ?? CENTRO_LOJA}
          defaultZoom={15}
          disableDefaultUI
          clickableIcons={false}
          gestureHandling="greedy"
          styles={ESTILO_GACELAS}

          style={{ width: '100%', height: '100%' }}
          onClick={(e: MapMouseEvent) => {
            const ll = e.detail.latLng
            if (ll) onTocar({ lat: +ll.lat.toFixed(6), lng: +ll.lng.toFixed(6) })
          }}
        >
          <Linea puntos={puntos} />
        </Map>
        <Controles />
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
    <div className={`relative ${className}`} data-testid="mapa-rutas">
      <APIProvider apiKey={clave} language="es" region="EC">
        <Map
          defaultCenter={CENTRO_LOJA}
          defaultZoom={12}
          disableDefaultUI
          gestureHandling="greedy"
          styles={ESTILO_GACELAS}
          clickableIcons={false}
          style={{ width: '100%', height: '100%' }}
        >
          <Agrupados rutas={rutas} onAbrir={onAbrir} />
        </Map>
        <Controles />
      </APIProvider>
    </div>
  )
}

function Corredores({
  corredores,
}: {
  corredores: { id: string; nombre: string; punto: Coordenada | null; trazo: Coordenada[] }[]
}) {
  const mapa = useMap()
  useEffect(() => {
    if (!mapa) return
    const objetos: { setMap(m: google.maps.Map | null): void }[] = []
    const limites = new google.maps.LatLngBounds()
    corredores.forEach((c, i) => {
      if (c.trazo.length > 1)
        objetos.push(
          new google.maps.Polyline({
            path: c.trazo,
            strokeColor: colorCorredor(i),
            strokeOpacity: 0.7,
            strokeWeight: 4,
            map: mapa,
          })
        )
      if (c.punto) {
        limites.extend(c.punto)
        objetos.push(
          new google.maps.Marker({
            position: c.punto,
            map: mapa,
            title: c.nombre,
            label: { text: String(i + 1), color: '#0b1220', fontWeight: '700' },
            icon: {
              path: google.maps.SymbolPath.CIRCLE,
              scale: 11,
              fillColor: colorCorredor(i),
              fillOpacity: 1,
              strokeColor: '#fff',
              strokeWeight: 2,
            },
          })
        )
      }
    })
    if (!limites.isEmpty()) mapa.fitBounds(limites, 60)
    return () => objetos.forEach((o) => o.setMap(null))
  }, [mapa, corredores])
  return null
}

/** Mapa con varios corredores (en vivo o repeticion de un evento). */
export function MapaCorredores({
  corredores,
  className = '',
}: {
  corredores: { id: string; nombre: string; punto: Coordenada | null; trazo: Coordenada[] }[]
  className?: string
}) {
  const clave = claveMapas()
  if (!clave)
    return <p className="text-text-secondary p-4">El mapa necesita Google Maps configurado.</p>
  return (
    <div className={`relative ${className}`} data-testid="mapa-corredores">
      <APIProvider apiKey={clave} language="es" region="EC">
        <Map
          defaultCenter={CENTRO_LOJA}
          defaultZoom={14}
          disableDefaultUI
          gestureHandling="greedy"
          styles={ESTILO_GACELAS}
          clickableIcons={false}
          style={{ width: '100%', height: '100%' }}
        >
          <Corredores corredores={corredores} />
        </Map>
        <Controles />
      </APIProvider>
    </div>
  )
}
