import { Link, useLocation, useNavigate } from 'react-router-dom'
import { notificacionesVisibles, useAvisos } from '../contexts/avisos/api'
import { useEstadoClima } from '../contexts/clima/api'
import { useIdentidad } from '../contexts/identidad/api'
import { useEsEscritorio } from '../shared/lib/useEsEscritorio'
import { Esqueleto } from '../shared/ui/Esqueletos'
import { useBusqueda } from './ShellEscritorio'

const SECCIONES: [string, string][] = [
  ['/inicio', 'Inicio'],
  ['/rutas', 'Rutas'],
  ['/comunidad/eventos', 'Eventos'],
  ['/comunidad/clubes', 'Clubes y grupos'],
  ['/comunidad', 'Comunidad'],
  ['/perfil', 'Perfil'],
]

/** Fecha larga en español en la zona de Loja: "Viernes 13 de noviembre". */
export function fechaLarga(ms = Date.now()) {
  const t = new Date(ms).toLocaleDateString('es-EC', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    timeZone: 'America/Guayaquil',
  })
  const limpio = t.replace(',', '')
  return limpio.charAt(0).toUpperCase() + limpio.slice(1)
}

/** Fecha corta para movil: "Viernes 13 nov". */
export function fechaCorta(ms = Date.now()) {
  const d = new Date(ms - 5 * 3_600_000)
  const dias = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado']
  const meses = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']
  return `${dias[d.getUTCDay()]} ${d.getUTCDate()} ${meses[d.getUTCMonth()]}`
}

/** Icono Material segun la descripcion del clima. */
export function iconoClima(descripcion: string) {
  const d = descripcion.toLowerCase()
  if (d.includes('tormenta')) return 'thunderstorm'
  if (d.includes('lluvia') || d.includes('llovizna') || d.includes('chubasco')) return 'rainy'
  if (d.includes('niebla')) return 'foggy'
  if (d.includes('nieve')) return 'weather_snowy'
  if (d.includes('despejado')) return 'clear_day'
  if (d.includes('parcial')) return 'partly_cloudy_day'
  return 'cloud'
}

/** Chip de clima: esqueleto mientras carga, dato real o "Reintentar" (nunca "--"). */
export function ChipClima({
  compacto = false,
  onAbrir,
  testId = 'chip-clima',
}: {
  compacto?: boolean
  onAbrir?: () => void
  testId?: string
}) {
  const e = useEstadoClima()
  const base = `h-10 shrink-0 rounded-md bg-c2-card border border-c2-linea flex items-center gap-2 text-[13px] ${compacto ? 'px-2.5' : 'px-3'}`
  if (e.estado === 'cargando')
    return (
      <span className={base} role="status" aria-busy="true" data-testid={testId}>
        <span className="sr-only">Cargando clima</span>
        <Esqueleto className="h-4 w-4 rounded-full" />
        <Esqueleto className={`h-3 rounded ${compacto ? 'w-8' : 'w-20'}`} />
      </span>
    )
  if (e.estado === 'sin-datos')
    return (
      <button
        type="button"
        onClick={e.reintentar}
        className={`${base} text-c2-muted hover:text-white`}
        data-testid={testId}
        title="No pudimos leer el clima. Toca para reintentar."
      >
        <span aria-hidden="true" className="material-symbols-outlined text-[18px]">
          cloud_off
        </span>
        {compacto ? 'Reintentar' : 'Clima no disponible'}
      </button>
    )
  const c = e.clima
  return (
    <button
      type="button"
      onClick={onAbrir}
      className={`${base} hover:border-c2-cielo/50`}
      data-testid={testId}
      aria-label={`Clima en ${c.lugar}: ${c.temperatura} grados, ${c.descripcion}`}
    >
      <span aria-hidden="true" className="material-symbols-outlined text-[18px] text-c2-cielo">
        {iconoClima(c.descripcion)}
      </span>
      <span className="font-semibold text-white">{c.temperatura}°</span>
      {!compacto && <span className="text-c2-muted first-letter:uppercase">{c.descripcion}</span>}
    </button>
  )
}

/** Campana con el numero de notificaciones sin leer. */
export function BotonNotificaciones() {
  const ajustes = useIdentidad((s) => s.ajustes)
  const lista = useAvisos((s) => s.notificaciones)
  const sinLeer = notificacionesVisibles(ajustes, lista).filter((n) => !n.leida).length
  return (
    <Link
      to="/notificaciones"
      aria-label={sinLeer ? `Notificaciones (${sinLeer} sin leer)` : 'Notificaciones'}
      className="relative w-10 h-10 shrink-0 rounded-md bg-c2-card border border-c2-linea grid place-items-center text-white hover:border-c2-morado/60"
    >
      <span aria-hidden="true" className="material-symbols-outlined text-[20px]">
        notifications
      </span>
      {sinLeer > 0 && (
        <span
          data-testid="punto-notificaciones"
          className="absolute -top-1 -right-1 min-w-4 h-4 px-1 rounded-full bg-c2-morado text-[9px] font-bold grid place-items-center text-white"
        >
          {sinLeer > 9 ? '9+' : sinLeer}
        </span>
      )}
    </Link>
  )
}

/** Lleva a Inicio y baja al panel "Clima para correr". */
export function useAbrirClima() {
  const navegar = useNavigate()
  const { pathname } = useLocation()
  return () => {
    const bajar = () =>
      document
        .getElementById('clima-correr')
        ?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    if (pathname === '/inicio') bajar()
    else {
      navegar('/inicio')
      window.setTimeout(bajar, 400)
    }
  }
}

/**
 * Barra superior de escritorio (rediseno web): fecha, saludo o seccion, busqueda global
 * (Ctrl+K), clima real y notificaciones. Solo se muestra desde 1024 px.
 */
export function BarraSuperior() {
  const escritorio = useEsEscritorio()
  if (!escritorio) return null
  return <BarraSuperiorEscritorio />
}

function BarraSuperiorEscritorio() {
  const { pathname } = useLocation()
  const sesion = useIdentidad((s) => s.sesion)
  const abrir = useBusqueda((s) => s.abrir)
  const abrirClima = useAbrirClima()
  const seccion = SECCIONES.find(([p]) => pathname === p || pathname.startsWith(`${p}/`))?.[1]
  const esInicio = pathname === '/inicio'
  const nombre = sesion?.nombre.split(' ')[0] ?? 'Corredor'
  return (
    <header
      data-testid="barra-superior"
      className="hidden lg:flex h-[68px] px-8 items-center gap-4 xl:gap-6 border-b border-c2-linea bg-c2-bg/95 backdrop-blur sticky top-0 z-40"
    >
      <div className="min-w-0 whitespace-nowrap">
        <span className="rotulo-c2 !text-[10px]">{fechaLarga()}</span>
        {esInicio ? (
          <h1 className="titular-c2 text-[26px] leading-none mt-1 text-white">Hola, {nombre}</h1>
        ) : (
          <p className="titular-c2 text-[22px] leading-none mt-1 text-white">
            {seccion ?? 'Runcity'}
          </p>
        )}
      </div>
      <div className="flex-1" />
      <button
        type="button"
        onClick={abrir}
        aria-keyshortcuts="Control+K"
        className="w-[240px] xl:w-[300px] shrink-0 whitespace-nowrap h-10 rounded-md bg-c2-card border border-c2-linea flex items-center gap-2 px-3 text-c2-muted text-[13px] hover:border-c2-morado/60"
        data-testid="abrir-busqueda"
      >
        <span aria-hidden="true" className="material-symbols-outlined text-[18px]">
          search
        </span>
        <span className="truncate">Buscar rutas, corredores, clubes</span>
        <span className="ml-auto text-[10px] border border-c2-linea rounded px-1.5 py-0.5">
          Ctrl K
        </span>
      </button>
      <ChipClima onAbrir={abrirClima} />
      <BotonNotificaciones />
    </header>
  )
}
