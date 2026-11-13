import { useCallback, useMemo } from 'react'
import { Link, Outlet, useLocation } from 'react-router-dom'
import { create } from 'zustand'
import { useEntradaPagina } from '../shared/lib/movimiento'
import { useActividad } from '../contexts/actividad/api'
import { useRacha } from '../contexts/actividad/ui/useRacha'
import { notificacionesVisibles, useAvisos } from '../contexts/avisos/api'
import { FotoPerfil } from '../contexts/identidad'
import { useIdentidad } from '../contexts/identidad/api'
import { useRutas } from '../contexts/rutas/api'
import { diaSemana, inicioSemana, resumenSemana } from '../shared/kernel/estadisticas'
import { nivelCorredor } from '../shared/kernel/nivel'
import { rachaEnRiesgo } from '../shared/kernel/rachas'
import { Unidades } from '../shared/lib/geo'
import { useEsEscritorio } from '../shared/lib/useEsEscritorio'
import { AvatarIniciales, Progreso } from '../shared/ui/Visual'
import { BusquedaGlobal, useAtajoBusqueda } from './BusquedaGlobal'
import { useEnVivo, useEventosVisibles, useSondeoEnVivo } from './datosApp'

/** Estado de la busqueda global (Ctrl+K), compartido por la barra superior e Inicio movil. */
export const useBusqueda = create<{ abierta: boolean; abrir(): void; cerrar(): void }>((set) => ({
  abierta: false,
  abrir: () => set({ abierta: true }),
  cerrar: () => set({ abierta: false }),
}))

type Contador = 'rutas' | 'eventos' | 'envivo' | 'notif'

const NAV_ESCRITORIO: { path: string; label: string; icon: string; contador?: Contador }[] = [
  { path: '/inicio', label: 'Inicio', icon: 'home' },
  { path: '/correr', label: 'Correr', icon: 'play_circle' },
  { path: '/rutas', label: 'Rutas', icon: 'map', contador: 'rutas' },
  { path: '/comunidad', label: 'Comunidad', icon: 'groups' },
  { path: '/comunidad/clubes', label: 'Clubes y grupos', icon: 'shield' },
  { path: '/comunidad/eventos', label: 'Eventos', icon: 'event', contador: 'eventos' },
  { path: '/comunidad/en-vivo', label: 'En vivo', icon: 'sensors', contador: 'envivo' },
  { path: '/comunidad/corredores', label: 'Corredores', icon: 'person_search' },
  { path: '/notificaciones', label: 'Notificaciones', icon: 'notifications', contador: 'notif' },
  { path: '/perfil', label: 'Perfil', icon: 'person' },
  { path: '/ajustes', label: 'Ajustes', icon: 'settings' },
]

const SUBSECCIONES_COMUNIDAD = [
  '/comunidad/clubes',
  '/comunidad/eventos',
  '/comunidad/en-vivo',
  '/comunidad/mapa',
  '/comunidad/corredores',
]

/** Entrada activa: la ruta exacta o una hija; Comunidad no se enciende en sus subsecciones. */
export function navActiva(pathname: string, path: string) {
  if (path === '/comunidad')
    return (
      pathname === path ||
      (pathname.startsWith('/comunidad/') &&
        !SUBSECCIONES_COMUNIDAD.some((s) => pathname.startsWith(s)))
    )
  if (path === '/comunidad/en-vivo')
    return pathname.startsWith(path) || pathname.startsWith('/comunidad/mapa')
  return pathname === path || pathname.startsWith(`${path}/`)
}

const DIAS = ['L', 'M', 'X', 'J', 'V', 'S', 'D']

/** Bloque de racha de la barra lateral: dias seguidos, estado y la semana de lunes a domingo. */
function BloqueRacha() {
  const r = useRacha()
  const carreras = useActividad((s) => s.carreras)
  const ahora = Date.now()
  const dias = useMemo(() => resumenSemana(carreras, ahora).diasCorridos, [carreras]) // eslint-disable-line react-hooks/exhaustive-deps
  const hoy = diaSemana(ahora)
  const riesgo = rachaEnRiesgo(r, ahora)
  const texto =
    r.diaria === 0
      ? 'Sin racha. Corre hoy para empezar una.'
      : riesgo
        ? `${r.diaria === 1 ? 'día' : 'días'}. Corre hoy para mantenerla`
        : `${r.diaria === 1 ? 'día' : 'días'} seguidos. Vas al día`
  return (
    <div
      data-testid="racha-barra"
      className="panel-c2 mx-4 mb-2 p-3 border border-[#3B2354] bg-[linear-gradient(170deg,#2A0A3D,#191F2F)]"
    >
      <div className="flex items-center justify-between">
        <span className="rotulo-c2 !text-c2-violeta">Racha</span>
        {riesgo && (
          <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-400/15 text-amber-400">
            en riesgo
          </span>
        )}
      </div>
      <div className="flex items-baseline gap-2 mt-1">
        {r.diaria > 0 && (
          <span className="cifra-c2 text-[26px] leading-none text-white">{r.diaria}</span>
        )}
        <span className="text-[12px] text-c2-muted leading-tight">{texto}</span>
      </div>
      <div className="flex gap-1 mt-2" aria-label="Días corridos esta semana">
        {DIAS.map((d, i) => (
          <span
            key={d}
            title={dias.has(i) ? 'Corriste' : i === hoy ? 'Hoy' : undefined}
            className={`flex-1 h-5 rounded-sm grid place-items-center text-[9px] font-bold ${
              dias.has(i)
                ? 'bg-c2-menta/80 text-[#003825]'
                : i === hoy
                  ? 'border border-c2-violeta text-c2-violeta'
                  : 'bg-c2-s2 text-c2-muted'
            }`}
          >
            {d}
          </span>
        ))}
      </div>
    </div>
  )
}

/** Meta semanal en km (la unica meta: ajustes.metaKmSemana, columna weekly_goal_km). */
export function useMetaSemanal() {
  const carreras = useActividad((s) => s.carreras)
  const metaKm = useIdentidad((s) => s.ajustes.metaKmSemana) || 25
  const ahora = Date.now()
  return useMemo(() => {
    const s = resumenSemana(carreras, ahora)
    const lunes = new Date(inicioSemana(ahora))
    const domingo = new Date(lunes.getTime() + 6 * 86_400_000)
    const mes = (d: Date) => d.toLocaleDateString('es-EC', { month: 'short' }).replace('.', '')
    const rango =
      lunes.getMonth() === domingo.getMonth()
        ? `${lunes.getDate()} - ${domingo.getDate()} ${mes(domingo)}`
        : `${lunes.getDate()} ${mes(lunes)} - ${domingo.getDate()} ${mes(domingo)}`
    return {
      km: s.km,
      metaKm,
      faltan: Math.max(0, metaKm - s.km),
      pct: Math.min(100, (s.km / metaKm) * 100),
      carreras: s.carreras,
      rango,
      semana: s,
    }
  }, [carreras, metaKm]) // eslint-disable-line react-hooks/exhaustive-deps
}

function MetaLateral() {
  const meta = useMetaSemanal()
  const m = useIdentidad((s) => s.ajustes.unidadesMetricas)
  return (
    <Link
      to="/perfil/editar"
      title="Cambiar la meta semanal"
      className="panel-c2 mx-4 mb-2 p-3 block bg-c2-card border border-c2-linea hover:border-c2-morado/50"
      data-testid="meta-lateral"
    >
      <span className="flex justify-between items-baseline">
        <span className="rotulo-c2">Meta semanal</span>
        <span className="text-[11px] text-c2-muted">{meta.rango}</span>
      </span>
      <span className="flex items-baseline gap-1 mt-1">
        <span className="cifra-c2 text-[20px] text-white">{Unidades.texto(meta.km, m, 1)}</span>
        <span className="text-c2-muted text-[12px]">
          / {Unidades.texto(meta.metaKm, m, 0)} {Unidades.etiquetaDistancia(m)}
        </span>
      </span>
      <span className="block mt-2">
        <Progreso pct={meta.pct} etiqueta="Progreso de la meta semanal" />
      </span>
    </Link>
  )
}

/**
 * Contenedor de escritorio (>=1024px): barra lateral fija con racha, navegacion con contadores,
 * meta semanal y usuario. En movil no pinta nada extra. La busqueda global (Ctrl+K) vive aqui.
 */
export function ShellEscritorio() {
  const { pathname } = useLocation()
  const refPagina = useEntradaPagina<HTMLDivElement>(pathname)
  const escritorio = useEsEscritorio()
  const busqueda = useBusqueda()
  const abrir = useBusqueda((s) => s.abrir)
  useAtajoBusqueda(useCallback(() => abrir(), [abrir]))

  return (
    <div className="shell-escritorio min-h-screen bg-bg-primary">
      {escritorio && <BarraLateral pathname={pathname} />}
      <div ref={refPagina} className="lg:pl-[248px]">
        <Outlet />
      </div>
      <BusquedaGlobal abierto={busqueda.abierta} onCerrar={busqueda.cerrar} />
    </div>
  )
}

function BarraLateral({ pathname }: { pathname: string }) {
  useSondeoEnVivo()
  const sesion = useIdentidad((s) => s.sesion)
  const ajustes = useIdentidad((s) => s.ajustes)
  const listaNotif = useAvisos((s) => s.notificaciones)
  const sinLeer = notificacionesVisibles(ajustes, listaNotif).filter((n) => !n.leida).length
  const rutas = useRutas((s) => s.rutas)
  const cargandoRutas = useRutas((s) => s.cargandoRutas)
  const { proximos } = useEventosVisibles()
  const enVivo = useEnVivo((s) => s.corredores.length)
  const carreras = useActividad((s) => s.carreras)
  const nivel = nivelCorredor(carreras.reduce((t, c) => t + c.distanciaKm, 0))
  const contadores: Record<Contador, number> = {
    rutas: cargandoRutas ? 0 : rutas.length,
    eventos: proximos.length,
    envivo: enVivo,
    notif: sinLeer,
  }
  const nombre = sesion?.nombre ?? 'Corredor'
  return (
    <aside
      aria-label="Navegación principal"
      className="hidden lg:flex fixed inset-y-0 left-0 z-[60] w-[248px] flex-col bg-[#0A101D] border-r border-c2-linea"
    >
      <Link
        to="/inicio"
        className="px-5 pt-5 pb-4 flex items-center gap-3 shrink-0"
        aria-label="Runcity, ir a Inicio"
      >
        <span className="w-9 h-9 rounded-md bg-[linear-gradient(135deg,#7B1FA2,#A855F7)] grid place-items-center">
          <span aria-hidden="true" className="material-symbols-outlined text-white text-[20px]">
            directions_run
          </span>
        </span>
        <span>
          <span className="block font-display italic text-[19px] leading-none text-white tracking-[0.035em]">
            Runcity
          </span>
          <span className="block rotulo-c2 !text-[9.5px] !text-c2-violeta mt-1">Loja, Ecuador</span>
        </span>
      </Link>
      <BloqueRacha />
      <nav className="px-3 flex-1 min-h-0 overflow-y-auto space-y-0.5 pb-2">
        {NAV_ESCRITORIO.map((item) => {
          const on = navActiva(pathname, item.path)
          const n = item.contador ? contadores[item.contador] : 0
          return (
            <Link
              key={item.path}
              to={item.path}
              aria-current={on ? 'page' : undefined}
              className={`relative flex items-center gap-3 px-3 h-[34px] rounded-md text-[13.5px] transition-colors ${
                on
                  ? 'bg-c2-s2 text-white font-semibold before:absolute before:left-0 before:top-2 before:bottom-2 before:w-[3px] before:rounded before:bg-c2-violeta'
                  : 'text-c2-muted hover:text-white hover:bg-c2-card'
              }`}
            >
              <span
                aria-hidden="true"
                className={`material-symbols-outlined text-[19px] ${on ? 'text-c2-violeta' : ''}`}
              >
                {item.icon}
              </span>
              <span className="flex-1 truncate">{item.label}</span>
              {n > 0 && (
                <span
                  data-testid={`contador-${item.contador}`}
                  aria-label={
                    item.contador === 'notif'
                      ? `${n} sin leer`
                      : item.contador === 'envivo'
                        ? `${n} corriendo ahora`
                        : `${n}`
                  }
                  className={`text-[10.5px] font-bold px-1.5 rounded ${
                    item.contador === 'notif'
                      ? 'bg-c2-morado text-white'
                      : item.contador === 'envivo'
                        ? 'bg-c2-menta/15 text-c2-menta'
                        : 'text-c2-muted/70'
                  }`}
                >
                  {n}
                </span>
              )}
            </Link>
          )
        })}
      </nav>
      <MetaLateral />
      <div className="px-4 pb-3">
        <Link
          to="/correr"
          className="panel-c2 h-11 bg-[linear-gradient(90deg,#7B1FA2,#A855F7)] flex items-center justify-center gap-2 font-bold text-[14px] text-white hover:brightness-110"
        >
          <span aria-hidden="true" className="material-symbols-outlined text-[18px]">
            play_circle
          </span>
          Empezar a correr
        </Link>
      </div>
      <Link
        to="/perfil"
        className="border-t border-c2-linea px-4 py-3 flex items-center gap-3 min-w-0 hover:bg-c2-card"
      >
        {sesion?.foto ? (
          <FotoPerfil foto={sesion.foto} nombre={nombre} className="h-9 w-9" />
        ) : (
          <AvatarIniciales nombre={nombre} tamano={36} />
        )}
        <span className="min-w-0">
          <span className="block text-[13px] font-semibold text-white truncate">{nombre}</span>
          <span className="block text-[11px] text-c2-muted truncate">Nivel {nivel}</span>
        </span>
      </Link>
    </aside>
  )
}
