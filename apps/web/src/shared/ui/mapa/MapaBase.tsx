import { APIProvider, Map, useMap } from '@vis.gl/react-google-maps'
import { useEffect, useRef, useState, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import {
  deltaAngulo,
  desplazar,
  ESTILO_GACELAS,
  mapId,
  NARANJA,
  tieneMapIdPropio,
  VERDE,
} from '../../lib/mapaEstilo'
import { CENTRO_LOJA, claveMapas, type Coordenada } from '../../lib/mapas'
import { usePosicionSuave } from './usePosicionSuave'

/**
 * Mapa base de Gacelas: estilo propio (oscuro, sin ruido), controles propios y, en modo 3D,
 * mapa vectorial con inclinacion y giro (necesita Map ID). Sin clave muestra `alternativa`.
 */
export function MapaBase({
  centro = CENTRO_LOJA,
  zoom = 15,
  modo3d = false,
  className = '',
  controles = true,
  alternativa = null,
  children,
  testId = 'mapa-google',
}: {
  centro?: Coordenada
  zoom?: number
  modo3d?: boolean
  className?: string
  controles?: boolean
  alternativa?: ReactNode
  children?: ReactNode
  testId?: string
}) {
  const clave = claveMapas()
  // El vectorial inclinado solo con un Map ID propio (estilo oscuro de Gacelas en la nube). Con el
  // DEMO_MAP_ID salia el estilo por defecto de Google con comercios: se usa el raster oscuro propio.
  const vector = modo3d && tieneMapIdPropio()
  if (!clave) return <>{alternativa}</>
  return (
    <div
      className={/\b(absolute|fixed)\b/.test(className) ? className : `relative ${className}`}
      data-testid={testId}
    >
      <APIProvider apiKey={clave} language="es" region="EC">
        <Map
          key={vector ? 'vector' : 'raster'}
          defaultCenter={centro}
          defaultZoom={modo3d ? 18 : zoom}
          defaultTilt={vector ? 65 : 0}
          disableDefaultUI
          clickableIcons={false}
          gestureHandling="greedy"
          backgroundColor="#0d1422"
          {...(vector
            ? { mapId: mapId(), colorScheme: 'DARK' as const, renderingType: 'VECTOR' as const }
            : { styles: ESTILO_GACELAS })}
          style={{ width: '100%', height: '100%' }}
        >
          {children}
        </Map>
        {controles && <Controles />}
      </APIProvider>
    </div>
  )
}

const botonCtl =
  'w-11 h-11 flex items-center justify-center text-white/90 hover:text-white bg-[#121a2a]/90 backdrop-blur-md'

/** Zoom, brujula (toca para volver al norte) en estilo Gacelas. */
export function Controles() {
  const mapa = useMap()
  const [rumboMapa, setRumbo] = useState(0)
  useEffect(() => {
    if (!mapa) return
    const l = mapa.addListener('heading_changed', () => setRumbo(mapa.getHeading() ?? 0))
    return () => l.remove()
  }, [mapa])
  if (!mapa) return null
  return (
    <div
      className="absolute right-3 top-1/2 -translate-y-1/2 z-10 flex flex-col gap-2"
      data-testid="controles-mapa"
    >
      <div className="rounded-2xl overflow-hidden border border-white/10 shadow-xl divide-y divide-white/10">
        <button
          aria-label="Acercar"
          className={botonCtl}
          onClick={() => mapa.setZoom((mapa.getZoom() ?? 15) + 1)}
        >
          <span className="material-symbols-outlined">add</span>
        </button>
        <button
          aria-label="Alejar"
          className={botonCtl}
          onClick={() => mapa.setZoom((mapa.getZoom() ?? 15) - 1)}
        >
          <span className="material-symbols-outlined">remove</span>
        </button>
      </div>
      <button
        aria-label="Orientar al norte"
        className={`${botonCtl} rounded-full border border-white/10 shadow-xl`}
        onClick={() => mapa.moveCamera({ heading: 0 })}
      >
        <span
          className="material-symbols-outlined text-primary-400"
          style={{ transform: `rotate(${-rumboMapa}deg)` }}
        >
          navigation
        </span>
      </button>
    </div>
  )
}

/** Boton "seguirme" estilo Uber (se muestra cuando el usuario movio el mapa). */
export function BotonSeguir({ siguiendo, onSeguir }: { siguiendo: boolean; onSeguir: () => void }) {
  if (siguiendo) return null
  return (
    <button
      onClick={onSeguir}
      data-testid="recentrar"
      className="absolute left-1/2 -translate-x-1/2 bottom-4 z-10 h-11 px-4 rounded-full bg-[#121a2a]/95 text-white border border-white/10 shadow-xl flex items-center gap-2"
    >
      <span className="material-symbols-outlined text-primary-400">my_location</span>Recentrar
    </button>
  )
}

/** Marcador HTML libre (OverlayView): funciona en mapas raster y vectoriales sin Map ID. */
export function MarcadorHtml({
  pos,
  children,
  z = 1,
}: {
  pos: Coordenada
  children: ReactNode
  z?: number
}) {
  const mapa = useMap()
  const [div] = useState(() => document.createElement('div'))
  const ov = useRef<google.maps.OverlayView | null>(null)
  const posRef = useRef(pos)
  posRef.current = pos
  useEffect(() => {
    if (!mapa) return
    class Capa extends google.maps.OverlayView {
      onAdd() {
        div.style.position = 'absolute'
        div.style.zIndex = String(z)
        this.getPanes()!.overlayMouseTarget.appendChild(div)
      }
      draw() {
        const p = this.getProjection()?.fromLatLngToDivPixel(posRef.current)
        if (p) div.style.transform = `translate(${p.x}px, ${p.y}px)`
      }
      onRemove() {
        div.remove()
      }
    }
    const c = new Capa()
    c.setMap(mapa)
    ov.current = c
    return () => c.setMap(null)
  }, [mapa, div, z])
  useEffect(() => ov.current?.draw(), [pos])
  return <PortalDiv div={div}>{children}</PortalDiv>
}

function PortalDiv({ div, children }: { div: HTMLElement; children: ReactNode }) {
  return createPortal(
    <div style={{ position: 'absolute', left: 0, top: 0, transform: 'translate(-50%, -50%)' }}>
      {children}
    </div>,
    div
  )
}

/** "Estas aqui": punto naranja, aro blanco, halo de precision pulsante y cono de direccion. */
export function Puck({
  pos,
  rumbo,
  precisionPx = 34,
}: {
  pos: Coordenada
  rumbo?: number | null
  precisionPx?: number
}) {
  const suave = usePosicionSuave(pos)
  const [giro, setGiro] = useState(rumbo ?? 0)
  useEffect(() => {
    if (rumbo != null) setGiro((g) => g + deltaAngulo(g, rumbo))
  }, [rumbo])
  if (!suave) return null
  return (
    <MarcadorHtml pos={suave} z={50}>
      <div
        className="relative"
        style={{ width: precisionPx * 2, height: precisionPx * 2 }}
        data-testid="puck"
      >
        <span
          className="absolute inset-0 rounded-full bg-[#FF6A13]/20 animate-ping"
          style={{ animationDuration: '2.4s' }}
        />
        <span className="absolute inset-0 rounded-full bg-[#FF6A13]/10 border border-[#FF6A13]/30" />
        {rumbo != null && (
          <span
            className="absolute left-1/2 top-1/2 transition-transform duration-500"
            style={{
              width: 0,
              height: 0,
              transform: `translate(-50%, -100%) rotate(${giro}deg)`,
              transformOrigin: '50% 100%',
              borderLeft: '16px solid transparent',
              borderRight: '16px solid transparent',
              borderBottom: `38px solid rgba(255,106,19,0.45)`,
              filter: 'blur(1px)',
            }}
          />
        )}
        <span className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-[18px] h-[18px] rounded-full bg-[#FF6A13] border-[3px] border-white shadow-[0_0_12px_rgba(255,106,19,0.9)]" />
      </div>
    </MarcadorHtml>
  )
}

/** Pines de inicio y meta. */
export function Pin({ pos, tipo }: { pos: Coordenada; tipo: 'inicio' | 'meta' }) {
  return (
    <MarcadorHtml pos={pos} z={40}>
      <div
        className="w-7 h-7 rounded-full border-[3px] border-white shadow-lg flex items-center justify-center"
        style={{ background: tipo === 'inicio' ? VERDE : '#111827' }}
        aria-label={tipo === 'inicio' ? 'Inicio' : 'Meta'}
        data-testid={`pin-${tipo}`}
      >
        {tipo === 'meta' ? (
          <span className="material-symbols-outlined text-white text-[16px]">sports_score</span>
        ) : (
          <span className="w-2 h-2 rounded-full bg-white" />
        )}
      </div>
    </MarcadorHtml>
  )
}

/** Recorrido: contorno oscuro + brillo + linea por tramos de color y flechas de direccion. */
export function RutaEstilizada({
  tramos,
  ajustar = false,
}: {
  tramos: { camino: Coordenada[]; color: string }[]
  ajustar?: boolean
}) {
  const mapa = useMap()
  useEffect(() => {
    if (!mapa || !tramos.length) return
    const todo = tramos.flatMap((t, i) => (i ? t.camino.slice(1) : t.camino))
    const capas: google.maps.Polyline[] = [
      new google.maps.Polyline({
        path: todo,
        strokeColor: '#05080f',
        strokeOpacity: 0.9,
        strokeWeight: 11,
        map: mapa,
        zIndex: 1,
      }),
      new google.maps.Polyline({
        path: todo,
        strokeColor: NARANJA,
        strokeOpacity: 0.18,
        strokeWeight: 18,
        map: mapa,
        zIndex: 0,
      }),
      ...tramos.map(
        (t) =>
          new google.maps.Polyline({
            path: t.camino,
            strokeColor: t.color,
            strokeOpacity: 1,
            strokeWeight: 6,
            map: mapa,
            zIndex: 2,
          })
      ),
      new google.maps.Polyline({
        path: todo,
        strokeOpacity: 0,
        map: mapa,
        zIndex: 3,
        icons: [
          {
            icon: {
              path: 'M -2,2 0,-1 2,2',
              strokeColor: '#ffffff',
              strokeOpacity: 0.85,
              strokeWeight: 2,
              scale: 2,
            },
            offset: '40px',
            repeat: '90px',
          },
        ],
      }),
    ]
    if (ajustar && todo.length > 1) {
      const b = new google.maps.LatLngBounds()
      todo.forEach((p) => b.extend(p))
      mapa.fitBounds(b, 48)
    }
    return () => capas.forEach((c) => c.setMap(null))
  }, [mapa, tramos, ajustar])
  return null
}

/** Ruta planificada por delante: camino brillante sobre el suelo. */
export function RutaPlan({ camino }: { camino: Coordenada[] }) {
  const mapa = useMap()
  useEffect(() => {
    if (!mapa || camino.length < 2) return
    const capas = [
      new google.maps.Polyline({
        path: camino,
        strokeColor: NARANJA,
        strokeOpacity: 0.22,
        strokeWeight: 26,
        map: mapa,
        zIndex: 0,
      }),
      new google.maps.Polyline({
        path: camino,
        strokeColor: '#F3E8FF',
        strokeOpacity: 0.55,
        strokeWeight: 8,
        map: mapa,
        zIndex: 0,
      }),
    ]
    return () => capas.forEach((c) => c.setMap(null))
  }, [mapa, camino])
  return null
}

/**
 * Camara que sigue al corredor. En 3D va detras, inclinada y con el rumbo hacia arriba
 * (estilo navegacion). Deja de seguir si el usuario arrastra el mapa.
 */
export function SeguirCamara({
  pos,
  rumbo,
  modo3d,
  siguiendo,
  onDejarDeSeguir,
  adelanteM = 160,
}: {
  pos: Coordenada | null
  rumbo: number | null
  modo3d: boolean
  siguiendo: boolean
  onDejarDeSeguir: () => void
  /** Metros (a zoom 18) que la camara mira por delante: el corredor queda mas abajo. */
  adelanteM?: number
}) {
  const mapa = useMap()
  const suave = usePosicionSuave(pos, 900)
  useEffect(() => {
    if (!mapa) return
    const l = mapa.addListener('dragstart', onDejarDeSeguir)
    return () => l.remove()
  }, [mapa, onDejarDeSeguir])
  useEffect(() => {
    if (!mapa || !suave || !siguiendo) return
    if (modo3d) {
      // Corredor abajo al centro: la camara mira unos metros por delante, con el rumbo arriba.
      const zoom = Math.max(mapa.getZoom() ?? 18.5, 17)
      const h = rumbo ?? mapa.getHeading() ?? 0
      mapa.moveCamera({
        center: desplazar(suave, h, adelanteM * Math.pow(2, 18 - zoom)),
        heading: h,
        tilt: 65,
        zoom,
      })
    } else mapa.moveCamera({ center: suave })
  }, [mapa, suave, rumbo, modo3d, siguiendo, adelanteM])
  return null
}
