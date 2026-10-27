import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { BotonReportarMapa } from '../../../shared/ui/BotonesMapa'
import { claveMapas } from '../../../shared/lib/mapas'
import { MapaCorredores } from '../../../shared/ui/MapaEditable'
import { ModalC2 } from '../../../shared/ui/modales'
import { tracker } from '../../actividad'
import { historial, otorgado } from '../../legal/api'
import {
  HojaReportarIncidente,
  ListaIncidentes,
  useIncidentesRecientes,
  type IncidentePublico,
} from '../../seguridad'
import {
  listarCorredores,
  publicarPosicion,
  suscribirCorredores,
  type CorredorEnMapa,
} from '../api'

const iniciales = (n: string) =>
  n
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]!.toUpperCase())
    .join('') || '?'
const colorEstado = (corriendo: boolean) => (corriendo ? '#68DBA9' : '#96CCFF')
export const haceCuanto = (ms: number, ahora: number) => {
  const s = Math.max(0, Math.floor((ahora - ms) / 1000))
  return s < 60 ? `hace ${s} s` : `hace ${Math.floor(s / 60)} min`
}

function Avatar({ c }: { c: CorredorEnMapa }) {
  return (
    <span
      className="w-10 h-10 rounded-full bg-[#232A3A] border-2 flex items-center justify-center text-sm font-bold text-text-primary overflow-hidden"
      style={{ borderColor: colorEstado(c.corriendo) }}
    >
      {c.avatarUrl ? (
        <img src={c.avatarUrl} alt="" className="w-full h-full object-cover" />
      ) : (
        iniciales(c.nombre)
      )}
    </span>
  )
}

/** Alternativa sin Google Maps: posiciones relativas sobre una rejilla. */
function Esquema({
  lista,
  onTocar,
  incidentes = [],
}: {
  lista: CorredorEnMapa[]
  onTocar: (c: CorredorEnMapa) => void
  incidentes?: IncidentePublico[]
}) {
  const lats = [...lista, ...incidentes].map((c) => c.lat)
  const lngs = [...lista, ...incidentes].map((c) => c.lng)
  const [minLat, maxLat, minLng, maxLng] = [
    Math.min(...lats),
    Math.max(...lats),
    Math.min(...lngs),
    Math.max(...lngs),
  ]
  return (
    <div
      className="relative w-full h-full"
      style={{
        backgroundImage:
          'linear-gradient(#2C3446 1px, transparent 1px), linear-gradient(90deg, #2C3446 1px, transparent 1px)',
        backgroundSize: '48px 48px',
      }}
    >
      {incidentes.map((i) => {
        const x = maxLng > minLng ? (i.lng - minLng) / (maxLng - minLng) : 0.5
        const y = maxLat > minLat ? 1 - (i.lat - minLat) / (maxLat - minLat) : 0.5
        return (
          <span
            key={i.id}
            data-testid={`marcador-incidente-${i.id}`}
            title="Incidente reportado"
            className="absolute -translate-x-1/2 -translate-y-1/2 w-7 h-7 rounded-full bg-[#F5C451] text-[#0B1220] flex items-center justify-center shadow-lg"
            style={{ left: `${10 + x * 80}%`, top: `${10 + y * 80}%` }}
          >
            <span className="material-symbols-outlined text-[16px]">warning</span>
          </span>
        )
      })}
      {lista.length === 0 && (
        <p className="absolute inset-0 flex items-center justify-center text-[#A8B0C2]">
          Sin corredores visibles
        </p>
      )}
      {lista.map((c) => {
        const x = maxLng > minLng ? (c.lng - minLng) / (maxLng - minLng) : 0.5
        const y = maxLat > minLat ? 1 - (c.lat - minLat) / (maxLat - minLat) : 0.5
        return (
          <button
            key={c.userId}
            data-testid={`marcador-${c.userId}`}
            onClick={() => onTocar(c)}
            className="absolute -translate-x-1/2 -translate-y-1/2 flex flex-col items-center"
            style={{ left: `${10 + x * 80}%`, top: `${10 + y * 80}%` }}
          >
            <Avatar c={c} />
            <span className="text-xs font-semibold text-text-primary">
              {c.nombre.split(' ')[0]}
            </span>
          </button>
        )
      })}
    </div>
  )
}

/**
 * Mapa en vivo de corredores. Solo aparecen quienes activaron "Ubicación visible" y publicaron
 * en los ultimos 2 minutos (RLS de runner_positions). Se actualiza con Supabase Realtime.
 */
export function MapaVivo({
  inicial,
  incidentesIniciales,
}: {
  inicial?: CorredorEnMapa[]
  incidentesIniciales?: IncidentePublico[]
}) {
  const incidentes = useIncidentesRecientes(incidentesIniciales)
  const [reportando, setReportando] = useState(false)
  const [miPos, setMiPos] = useState<{ lat: number; lng: number } | null>(null)
  const [avisoReporte, setAvisoReporte] = useState<string | null>(null)
  const abrirReporte = () => {
    const p = tracker.estado.puntos.at(-1)
    if (p) setMiPos({ lat: p.lat, lng: p.lng })
    else
      navigator.geolocation?.getCurrentPosition(
        (g) => setMiPos({ lat: g.coords.latitude, lng: g.coords.longitude }),
        () => setMiPos(null),
        { maximumAge: 30_000, timeout: 10_000 }
      )
    setReportando(true)
  }
  const [lista, setLista] = useState<CorredorEnMapa[]>(inicial ?? [])
  const [ahora, setAhora] = useState(Date.now())
  const [elegido, setElegido] = useState<CorredorEnMapa | null>(null)
  const visible = otorgado(historial(), 'ubicacion_visible')
  useEffect(() => {
    if (inicial) return
    let vivo = true
    const cargar = () =>
      void listarCorredores().then((l) => {
        if (!vivo) return
        setLista(l)
        setAhora(Date.now())
      })
    cargar()
    const salir = suscribirCorredores(cargar)
    const t = setInterval(cargar, 30_000)
    return () => {
      vivo = false
      salir()
      clearInterval(t)
    }
  }, [inicial])

  return (
    <main className="w-full min-h-screen bg-surface contenedor-escritorio lg:max-w-3xl">
      <header className="px-margin py-4 flex items-center gap-4">
        <Link
          to="/comunidad/en-vivo"
          aria-label="Volver"
          className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </Link>
        <h1 className="font-display italic uppercase text-[26px] text-text-primary">
          Mapa en vivo
        </h1>
      </header>
      <div className="px-margin pb-24 space-y-3">
        {!visible && (
          <Link
            to="/ajustes/privacidad"
            data-testid="aviso-no-visible"
            className="flex items-center gap-2 p-3 rounded-xl bg-[#191F2F] border border-[#2C3446] text-sm text-[#A8B0C2]"
          >
            <span className="material-symbols-outlined">location_off</span>
            No estás visible. Actívalo en Privacidad si quieres que otros te vean.
          </Link>
        )}
        <div
          className="relative h-[380px] lg:h-[460px] rounded-2xl overflow-hidden bg-[#191F2F]"
          data-testid="mapa-vivo"
        >
          {claveMapas() && !inicial ? (
            <MapaCorredores
              className="w-full h-full"
              incidentes={incidentes}
              corredores={lista.map((c) => ({
                id: c.userId,
                nombre: c.nombre,
                punto: { lat: c.lat, lng: c.lng },
                trazo: [],
              }))}
            />
          ) : (
            <Esquema lista={lista} onTocar={setElegido} incidentes={incidentes} />
          )}
          <BotonReportarMapa onClick={abrirReporte} className="absolute right-3 top-3 z-10" />
        </div>
        {avisoReporte && (
          <p role="status" className="text-sm text-[#68DBA9]">
            {avisoReporte}
          </p>
        )}
        <HojaReportarIncidente
          abierta={reportando}
          ubicacion={miPos}
          onCerrar={() => setReportando(false)}
          onEnviado={setAvisoReporte}
        />
        <div className="flex gap-4 text-sm text-[#A8B0C2]">
          <span className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-[#68DBA9]" /> Corriendo
          </span>
          <span className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-[#96CCFF]" /> Visible, sin correr
          </span>
        </div>
        <p className="text-text-primary font-semibold">
          {lista.length === 0
            ? 'Nadie está visible ahora mismo'
            : `${lista.length} corredores visibles`}
        </p>
        <ListaIncidentes lista={incidentes} />
        <ul className="space-y-2">
          {lista.map((c) => (
            <li key={c.userId}>
              <button
                onClick={() => setElegido(c)}
                data-testid={`corredor-${c.userId}`}
                className="w-full flex items-center gap-3 p-3 rounded-xl bg-[#191F2F] text-left"
              >
                <Avatar c={c} />
                <span className="flex-1">
                  <span className="block text-text-primary font-semibold">{c.nombre}</span>
                  <span className="block text-sm" style={{ color: colorEstado(c.corriendo) }}>
                    {c.corriendo ? 'Corriendo' : 'Sin correr'}
                  </span>
                </span>
                <span className="text-sm text-[#A8B0C2]">{haceCuanto(c.actualizadoMs, ahora)}</span>
              </button>
            </li>
          ))}
        </ul>
      </div>
      <ModalC2
        abierto={elegido != null}
        icono="person"
        titulo={elegido?.nombre ?? ''}
        texto={
          elegido
            ? `${elegido.corriendo ? 'Está corriendo' : 'Visible, sin correr'} · actualizado ${haceCuanto(elegido.actualizadoMs, ahora)}`
            : ''
        }
        primario="Cerrar"
        onPrimario={() => setElegido(null)}
        onCerrar={() => setElegido(null)}
      />
    </main>
  )
}

/** Publica la posicion propia mientras la app esta abierta, solo con el consentimiento vigente. */
export function PublicadorUbicacion() {
  useEffect(() => {
    const t = setInterval(() => {
      if (!otorgado(historial(), 'ubicacion_visible')) return
      const e = tracker.estado
      const ultimo = e.puntos.at(-1)
      const activa = e.fase === 'corriendo' || e.fase === 'pausada'
      if (activa && ultimo)
        return void publicarPosicion(ultimo.lat, ultimo.lng, e.fase === 'corriendo')
      navigator.geolocation?.getCurrentPosition(
        (p) => void publicarPosicion(p.coords.latitude, p.coords.longitude, false),
        () => undefined,
        { maximumAge: 60_000, timeout: 10_000 }
      )
    }, 15_000)
    return () => clearInterval(t)
  }, [])
  return null
}
