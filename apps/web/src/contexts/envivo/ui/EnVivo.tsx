import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { formatearHms, formatearRitmo, Unidades } from '../../../shared/lib/geo'
import { MapaGoogle } from '../../../shared/ui/MapaGoogle'
import { TrazadoRuta } from '../../../shared/ui/TrazadoRuta'
import {
  aPuntosGps,
  leerSesion,
  listarEnVivo,
  sesionActiva,
  sesionDesdeFila,
  suscribirLista,
  suscribirSesion,
  type SesionEnVivo,
} from '../../actividad/api'
import { useIdentidad } from '../../identidad/api'

function Encabezado({ titulo }: { titulo: string }) {
  const navigate = useNavigate()
  return (
    <header className="flex items-center gap-1 px-margin py-4">
      <button
        onClick={() => navigate(-1)}
        aria-label="Volver"
        className="w-12 h-12 -ml-3 shrink-0 flex items-center justify-center text-text-primary"
      >
        <span className="material-symbols-outlined">arrow_back</span>
      </button>
      <h1 className="flex-1 min-w-0 font-headline-md text-headline-md text-text-primary">
        {titulo}
      </h1>
    </header>
  )
}

/** Lista de corredores en vivo que puedo ver. */
export function ListaEnVivo() {
  const [sesiones, setSesiones] = useState<SesionEnVivo[] | null>(null)
  const m = useIdentidad((s) => s.ajustes.unidadesMetricas)
  useEffect(() => {
    let vivo = true
    const cargar = () => void listarEnVivo().then((l) => vivo && setSesiones(l))
    cargar()
    const salir = suscribirLista(cargar)
    const t = setInterval(cargar, 30_000)
    return () => {
      vivo = false
      salir()
      clearInterval(t)
    }
  }, [])
  return (
    <main className="w-full min-h-screen bg-surface contenedor-escritorio lg:max-w-3xl">
      <Encabezado titulo="En vivo ahora" />
      <div className="px-margin pb-24 space-y-3">
        <Link
          to="/comunidad/mapa"
          data-testid="ir-mapa-vivo"
          className="tarjeta-c2 flex items-center gap-3 p-4 border border-[#C084FC]/50"
        >
          <span className="w-2.5 h-2.5 rounded-full bg-[#68DBA9]" />
          <span className="flex-1">
            <span className="block text-text-primary font-title-md text-title-md">
              Mapa de corredores
            </span>
            <span className="block text-body-sm text-text-secondary">
              Quién está visible cerca de ti ahora
            </span>
          </span>
          <span className="material-symbols-outlined text-[#C084FC]">chevron_right</span>
        </Link>
        {sesiones === null && <p className="text-text-secondary">Buscando corredores…</p>}
        {sesiones?.length === 0 && (
          <div className="tarjeta-c2 p-4 text-text-secondary">
            Nadie a quien sigues está corriendo en vivo ahora.
          </div>
        )}
        <ul className="space-y-3">
          {sesiones?.map((s) => (
            <li key={s.id}>
              <Link
                to={`/comunidad/en-vivo/${s.id}`}
                data-testid="sesion-en-vivo"
                className="tarjeta-c2 flex items-center gap-3 min-h-[64px] p-4"
              >
                <span className="w-3 h-3 rounded-full bg-danger animate-pulse shrink-0" />
                <span className="flex-1 min-w-0">
                  <span className="block font-semibold text-text-primary truncate">{s.nombre}</span>
                  <span className="block text-sm text-text-secondary">
                    {Unidades.texto(s.distanciaKm, m)} {Unidades.etiquetaDistancia(m)} ·{' '}
                    {formatearHms(s.segundos)}
                  </span>
                </span>
                <span className="material-symbols-outlined text-text-secondary">chevron_right</span>
              </Link>
            </li>
          ))}
        </ul>
      </div>
    </main>
  )
}

/** Mapa en vivo de un corredor, actualizado por Supabase Realtime. */
export function DetalleEnVivo() {
  const { id = '' } = useParams()
  const m = useIdentidad((s) => s.ajustes.unidadesMetricas)
  const [s, setS] = useState<SesionEnVivo | null | undefined>(undefined)
  useEffect(() => {
    let vivo = true
    void leerSesion(id).then((x) => vivo && setS(x))
    const salir = suscribirSesion(id, (f) =>
      setS((prev) => (prev ? sesionDesdeFila(f, prev.nombre) : prev))
    )
    return () => {
      vivo = false
      salir()
    }
  }, [id])

  if (s === undefined)
    return (
      <main className="w-full min-h-screen bg-surface">
        <Encabezado titulo="En vivo" />
        <p className="px-margin text-text-secondary">Cargando…</p>
      </main>
    )
  if (s === null)
    return (
      <main className="w-full min-h-screen bg-surface">
        <Encabezado titulo="En vivo" />
        <p className="px-margin text-text-secondary">
          Esta carrera en vivo no existe o no tienes permiso para verla.
        </p>
      </main>
    )
  const activa = sesionActiva(s)
  const puntos = aPuntosGps(s.puntos)
  return (
    <main className="w-full min-h-screen bg-surface contenedor-escritorio lg:max-w-5xl">
      <Encabezado titulo={s.nombre} />
      <div className="px-margin pb-24 space-y-4">
        <p
          role="status"
          data-testid="estado-en-vivo"
          className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-sm font-semibold ${
            activa ? 'bg-danger/20 text-red-300' : 'bg-surface-container text-text-secondary'
          }`}
        >
          <span
            className={`w-2 h-2 rounded-full ${activa ? 'bg-danger animate-pulse' : 'bg-text-secondary'}`}
          />
          {activa ? 'En vivo' : 'Carrera terminada'}
        </p>
        <div className="relative h-[360px] lg:h-[480px] rounded-2xl overflow-hidden bg-surface-container border border-border-subtle">
          <MapaGoogle
            puntos={s.puntos}
            seguir
            className="w-full h-full"
            alternativa={<TrazadoRuta puntos={puntos} className="w-full h-full" />}
          />
          {s.oculta && (
            <div className="absolute inset-x-3 bottom-3 bg-surface/95 rounded-xl p-3 text-sm text-text-primary flex items-center gap-2">
              <span className="material-symbols-outlined text-primary">home_pin</span>
              Ubicación oculta cerca de casa por privacidad
            </div>
          )}
        </div>
        <div className="grid grid-cols-3 gap-3">
          {[
            [Unidades.texto(s.distanciaKm, m), Unidades.etiquetaDistancia(m), 'en-vivo-distancia'],
            [formatearHms(s.segundos), 'Tiempo', 'en-vivo-tiempo'],
            [
              formatearRitmo(
                Unidades.ritmo(
                  s.distanciaKm > 0.01 ? Math.floor(s.segundos / s.distanciaKm) : null,
                  m
                )
              ),
              Unidades.etiquetaRitmo(m),
              'en-vivo-ritmo',
            ],
          ].map(([v, e, t]) => (
            <div key={t} className="bg-surface-container rounded-xl p-3 text-center">
              <div
                className="font-headline-sm text-headline-sm text-text-primary whitespace-nowrap"
                data-testid={t}
              >
                {v}
              </div>
              <div className="text-xs text-text-secondary">{e}</div>
            </div>
          ))}
        </div>
      </div>
    </main>
  )
}
