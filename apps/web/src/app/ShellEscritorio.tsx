import { useCallback, useMemo } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { create } from 'zustand'
import { useEntradaPagina } from '../shared/lib/movimiento'
import { useActividad } from '../contexts/actividad/api'
import { notificacionesVisibles, useAvisos } from '../contexts/avisos/api'
import { useIdentidad } from '../contexts/identidad/api'
import { inicioSemana, resumenSemana } from '../shared/kernel/estadisticas'
import { nivelCorredor } from '../shared/kernel/nivel'
import { traducir } from '../shared/i18n/textos'
import { useEsEscritorio } from '../shared/lib/useEsEscritorio'
import { NavColapsable, type ItemNav } from '../shared/ui/navegacion/NavColapsable'
import { BusquedaGlobal, useAtajoBusqueda } from './BusquedaGlobal'

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
    <div className="shell-escritorio min-h-screen bg-bg-primary lg:flex">
      {escritorio && <BarraLateral />}
      <div ref={refPagina} className="min-w-0 lg:flex-1">
        <Outlet />
      </div>
      <BusquedaGlobal abierto={busqueda.abierta} onCerrar={busqueda.cerrar} />
    </div>
  )
}

/** Boton de busqueda global dentro del riel (reusa el estilo de los botones del navbar). */
function BotonBuscar() {
  const abrir = useBusqueda((s) => s.abrir)
  return (
    <button
      type="button"
      className="navc-btn navc-busca"
      onClick={abrir}
      aria-label="Buscar rutas, corredores y clubes"
      data-testid="nav-buscar"
    >
      <span aria-hidden="true" className="material-symbols-outlined" style={{ fontSize: 18 }}>
        search
      </span>
      <span className="navc-busca-t">Buscar</span>
      <kbd className="navc-atajo">Ctrl K</kbd>
    </button>
  )
}

/** Riel de escritorio: el navbar colapsable compartido con las secciones de la app de usuario. */
function BarraLateral() {
  const sesion = useIdentidad((s) => s.sesion)
  const ajustes = useIdentidad((s) => s.ajustes)
  const listaNotif = useAvisos((s) => s.notificaciones)
  const sinLeer = notificacionesVisibles(ajustes, listaNotif).filter((n) => !n.leida).length
  const carreras = useActividad((s) => s.carreras)
  const nivel = nivelCorredor(carreras.reduce((t, c) => t + c.distanciaKm, 0))
  const items: ItemNav[] = NAV_ESCRITORIO.map((item) => ({
    id: item.path.slice(1).replace(/\//g, '-'),
    etiqueta: traducir(ajustes.idioma, item.label),
    a: item.path,
    icono: item.icon,
    // Solo el conteo real de notificaciones sin leer; el resto no lleva insignia.
    insignia: item.contador === 'notif' ? sinLeer : undefined,
    activoEn: (p) => navActiva(p, item.path),
  }))
  return (
    <NavColapsable
      items={items}
      usuario={{
        nombre: sesion?.nombre ?? 'Corredor',
        detalle: `Nivel ${nivel}`,
        foto: sesion?.foto ?? null,
      }}
      etiqueta="Navegación principal"
      claveAlmacen="runcity.usuario.nav.colapsado"
      buscador={false}
      pie={<BotonBuscar />}
      className="navc-usuario navc-solo-riel"
    />
  )
}
