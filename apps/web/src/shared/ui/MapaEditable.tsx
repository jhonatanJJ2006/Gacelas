import { MarkerClusterer } from '@googlemaps/markerclusterer'
import { APIProvider, Map, useMap, type MapMouseEvent } from '@vis.gl/react-google-maps'
import { useEffect, useRef } from 'react'
import { colorCorredor } from '../../contexts/eventos/api'
import { estiloMapa } from '../lib/mapaEstilo'
import { CENTRO_LOJA, claveMapas, type Coordenada } from '../lib/mapas'
import { usePaddingPaneles } from '../lib/usePaddingPaneles'
import { EncuadrarRuta } from './mapa/EncuadrarRuta'
import { Controles } from './mapa/MapaBase'
import { useTemaResuelto } from './useTema'

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
  const tema = useTemaResuelto()
  const puntosIniciales = useRef(puntos.length > 1 ? puntos : [])
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
          styles={estiloMapa(tema)}

          style={{ width: '100%', height: '100%' }}
          onClick={(e: MapMouseEvent) => {
            const ll = e.detail.latLng
            if (ll) onTocar({ lat: +ll.lat.toFixed(6), lng: +ll.lng.toFixed(6) })
          }}
        >
          <Linea puntos={puntos} />
          {/* Al abrir una ruta ya trazada (edicion) se muestra entera; los toques nuevos no mueven la camara. */}
          <EncuadrarRuta puntos={puntosIniciales.current} />
        </Map>
        <Controles />
      </APIProvider>
    </div>
  )
}

type RutaMapa = {
  id: string
  nombre: string
  salida: Coordenada
  camino?: Coordenada[]
  detalle?: string
}

function Agrupados({ rutas, onTocar }: { rutas: RutaMapa[]; onTocar: (id: string) => void }) {
  const mapa = useMap()
  const tocar = useRef(onTocar)
  tocar.current = onTocar
  const huella = rutas.map((r) => r.id).join(',')
  useEffect(() => {
    if (!mapa || !rutas.length) return
    const marcas = rutas.map((r) => {
      const m = new google.maps.Marker({ position: r.salida, title: r.nombre })
      m.addListener('click', () => tocar.current(r.id))
      return m
    })
    const grupo = new MarkerClusterer({ map: mapa, markers: marcas })
    return () => {
      grupo.clearMarkers()
      grupo.setMap(null)
    }
    // Se recrean solo si cambian las rutas visibles (no en cada render del padre).
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mapa, huella])
  return null
}

/** Trazos de las rutas visibles; la elegida va resaltada con sus pines de inicio y meta. */
function TrazosRutas({ rutas, elegida }: { rutas: RutaMapa[]; elegida: string | null }) {
  const mapa = useMap()
  const huella = rutas.map((r) => r.id).join(',')
  useEffect(() => {
    if (!mapa) return
    const objetos: { setMap(m: google.maps.Map | null): void }[] = []
    rutas.forEach((r) => {
      if ((r.camino?.length ?? 0) < 2 || r.id === elegida) return
      objetos.push(
        new google.maps.Polyline({
          path: r.camino,
          strokeColor: '#A855F7',
          strokeOpacity: 0.45,
          strokeWeight: 3,
          map: mapa,
          clickable: false,
        })
      )
    })
    const e = rutas.find((r) => r.id === elegida)
    if (e?.camino && e.camino.length > 1) {
      const pin = (p: Coordenada, color: string) =>
        new google.maps.Marker({
          position: p,
          map: mapa,
          clickable: false,
          zIndex: 5,
          icon: {
            path: google.maps.SymbolPath.CIRCLE,
            scale: 7,
            fillColor: color,
            fillOpacity: 1,
            strokeColor: '#fff',
            strokeWeight: 2,
          },
        })
      objetos.push(
        new google.maps.Polyline({
          path: e.camino,
          strokeColor: '#05080f',
          strokeOpacity: 0.9,
          strokeWeight: 10,
          map: mapa,
          zIndex: 3,
          clickable: false,
        }),
        new google.maps.Polyline({
          path: e.camino,
          strokeColor: '#C084FC',
          strokeWeight: 6,
          map: mapa,
          zIndex: 4,
          clickable: false,
        }),
        pin(e.camino[0], '#10B981'),
        pin(e.camino[e.camino.length - 1], '#111827')
      )
    }
    return () => objetos.forEach((o) => o.setMap(null))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mapa, huella, elegida])
  return null
}

/**
 * Mapa de exploracion: salida de cada ruta (clusters) y, si traen trazo, su recorrido.
 * Con `elegida`, la camara se anima para mostrar ESA ruta entera en la franja libre sobre
 * la tarjeta inferior; sin ella, encuadra todas las salidas.
 */
export function MapaRutas({
  rutas,
  onAbrir,
  elegida = null,
  onElegir,
  className = '',
}: {
  rutas: RutaMapa[]
  onAbrir: (id: string) => void
  /** Ruta resaltada; si se pasa `onElegir`, tocar un marcador la elige en vez de abrirla. */
  elegida?: string | null
  onElegir?: (id: string | null) => void
  className?: string
}) {
  const clave = claveMapas()
  const tema = useTemaResuelto()
  const contenedor = useRef<HTMLDivElement>(null)
  const tarjeta = useRef<HTMLDivElement>(null)
  const padding = usePaddingPaneles(contenedor, [tarjeta])
  if (!clave)
    return <p className="text-text-secondary p-4">El mapa necesita Google Maps configurado.</p>
  const actual = rutas.find((r) => r.id === elegida) ?? null
  const camino = actual?.camino && actual.camino.length > 0 ? actual.camino : null
  const todas = rutas.map((r) => r.salida)
  return (
    <div ref={contenedor} className={`relative ${className}`} data-testid="mapa-rutas">
      <APIProvider apiKey={clave} language="es" region="EC">
        <Map
          defaultCenter={CENTRO_LOJA}
          defaultZoom={12}
          disableDefaultUI
          gestureHandling="greedy"
          styles={estiloMapa(tema)}
          clickableIcons={false}
          style={{ width: '100%', height: '100%' }}
        >
          <TrazosRutas rutas={rutas} elegida={actual?.id ?? null} />
          <Agrupados rutas={rutas} onTocar={onElegir ? onElegir : onAbrir} />
          <EncuadrarRuta
            puntos={camino ?? todas}
            padding={padding}
            clave={camino ? `ruta-${actual!.id}` : `todas-${rutas.length}`}
          />
        </Map>
        <Controles />
      </APIProvider>
      {actual && onElegir && (
        <div
          ref={tarjeta}
          data-testid="tarjeta-ruta-mapa"
          className="absolute inset-x-3 bottom-3 z-10 flex items-center gap-3 rounded-xl bg-[#121a2a]/95 border border-white/10 px-4 py-3 shadow-xl"
        >
          <div className="min-w-0 flex-1">
            <div className="text-[14px] font-bold text-white truncate">{actual.nombre}</div>
            {actual.detalle && (
              <div className="text-[12px] text-white/60 truncate">{actual.detalle}</div>
            )}
          </div>
          <button
            onClick={() => onAbrir(actual.id)}
            className="h-9 px-4 rounded-md bg-c2-morado text-white text-[13px] font-semibold"
          >
            Abrir ruta
          </button>
          <button
            onClick={() => onElegir(null)}
            aria-label="Quitar la ruta elegida"
            className="w-9 h-9 grid place-items-center text-white/70 hover:text-white"
          >
            <span aria-hidden="true" className="material-symbols-outlined text-[20px]">
              close
            </span>
          </button>
        </div>
      )}
    </div>
  )
}

function MarcadoresIncidentes({
  incidentes,
}: {
  incidentes: { id: string; lat: number; lng: number }[]
}) {
  const mapa = useMap()
  useEffect(() => {
    if (!mapa) return
    const marcas = incidentes.map(
      (i) =>
        new google.maps.Marker({
          position: { lat: i.lat, lng: i.lng },
          map: mapa,
          title: 'Incidente reportado',
          icon: {
            path: google.maps.SymbolPath.BACKWARD_CLOSED_ARROW,
            scale: 6,
            fillColor: '#F5C451',
            fillOpacity: 1,
            strokeColor: '#0B1220',
            strokeWeight: 1,
          },
        })
    )
    return () => marcas.forEach((m) => m.setMap(null))
  }, [mapa, incidentes])
  return null
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
    return () => objetos.forEach((o) => o.setMap(null))
  }, [mapa, corredores])
  return null
}

/** Todos los corredores con posicion, para encuadrarlos juntos. */
const puntosDeCorredores = (corredores: { punto: Coordenada | null }[]) =>
  corredores.flatMap((c) => (c.punto ? [c.punto] : []))

/** Mapa con varios corredores (en vivo o repeticion de un evento). */
export function MapaCorredores({
  corredores,
  className = '',
  incidentes = [],
}: {
  corredores: { id: string; nombre: string; punto: Coordenada | null; trazo: Coordenada[] }[]
  className?: string
  /** Incidentes recientes (posicion ya redondeada por el servidor). */
  incidentes?: { id: string; lat: number; lng: number }[]
}) {
  const clave = claveMapas()
  const tema = useTemaResuelto()
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
          styles={estiloMapa(tema)}
          clickableIcons={false}
          style={{ width: '100%', height: '100%' }}
        >
          <Corredores corredores={corredores} />
          {/* Se encuadra al aparecer el primero y cuando cambia cuantos corredores hay; el seguimiento no mueve la camara. */}
          <EncuadrarRuta
            puntos={puntosDeCorredores(corredores)}
            padding={60}
            clave={puntosDeCorredores(corredores).length}
          />
          <MarcadoresIncidentes incidentes={incidentes} />
        </Map>
        <Controles />
      </APIProvider>
    </div>
  )
}
