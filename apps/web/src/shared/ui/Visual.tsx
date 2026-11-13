import type { CSSProperties, ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Esqueleto } from './Esqueletos'

/*
 * Piezas del rediseno de la web (docs/REDISENO-WEB.md): paneles C2, rotulos, metricas grandes y
 * graficos SVG livianos (mini mapa, anillo, barras, mapa de calor). Sin dependencias ni Google Maps.
 */

export type AccionPanel = { texto: string; to: string } | { texto: string; onClick: () => void }

/** Panel C2: tarjeta #191F2F, borde fino y esquina inferior derecha cortada. */
export function Panel({
  titulo,
  subtitulo,
  accion,
  children,
  className = '',
  variante = 'normal',
  testId,
  sinPadding = false,
  id,
}: {
  titulo?: ReactNode
  subtitulo?: ReactNode
  accion?: AccionPanel
  children: ReactNode
  className?: string
  variante?: 'normal' | 'hero' | 'acento'
  testId?: string
  sinPadding?: boolean
  id?: string
}) {
  const fondo =
    variante === 'hero'
      ? 'bg-[linear-gradient(160deg,#2A0A3D_0%,#1A0B2E_55%,#191F2F_100%)] border-[#3B1A55]'
      : variante === 'acento'
        ? 'bg-[linear-gradient(170deg,#2A0A3D_0%,#191F2F_70%)] border-[#3B1A55]'
        : 'bg-c2-card border-c2-linea'
  const idTitulo = id ? `${id}-titulo` : undefined
  return (
    <section
      id={id}
      data-testid={testId}
      aria-labelledby={titulo ? idTitulo : undefined}
      className={`panel-c2 relative border min-w-0 ${fondo} ${sinPadding ? '' : 'p-5'} ${className}`}
    >
      {(titulo || accion) && (
        <div
          className={`flex items-start justify-between gap-3 ${sinPadding ? 'px-5 pt-5' : ''} mb-4`}
        >
          <div className="min-w-0">
            {titulo && (
              <h2 id={idTitulo} className="titulo-panel">
                {titulo}
              </h2>
            )}
            {subtitulo && <p className="text-[12px] text-c2-muted mt-0.5">{subtitulo}</p>}
          </div>
          {accion && <BotonAccion accion={accion} />}
        </div>
      )}
      {children}
    </section>
  )
}

function BotonAccion({ accion }: { accion: AccionPanel }) {
  const clase =
    'shrink-0 inline-flex items-center gap-1 text-[12px] font-semibold text-c2-violeta hover:text-white py-1'
  const contenido = (
    <>
      {accion.texto}
      <span aria-hidden="true" className="material-symbols-outlined text-[16px]">
        arrow_forward
      </span>
    </>
  )
  return 'to' in accion ? (
    <Link to={accion.to} className={clase}>
      {contenido}
    </Link>
  ) : (
    <button type="button" onClick={accion.onClick} className={clase}>
      {contenido}
    </button>
  )
}

/** Rotulo chico en mayusculas (10.5 px, tracking ancho). */
export function Rotulo({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <span className={`rotulo-c2 ${className}`}>{children}</span>
}

/** Cifra grande en Anton con unidad y nota opcional. */
export function Cifra({
  rotulo,
  valor,
  unidad,
  nota,
  color = 'text-white',
  tamano = 'text-[28px]',
  testId,
  className = '',
}: {
  rotulo: string
  valor: ReactNode
  unidad?: string
  nota?: ReactNode
  color?: string
  tamano?: string
  testId?: string
  className?: string
}) {
  return (
    <div className={`min-w-0 ${className}`}>
      <Rotulo>{rotulo}</Rotulo>
      <p className="mt-1 flex items-baseline gap-1 whitespace-nowrap">
        <span className={`cifra-c2 ${tamano} leading-none ${color}`} data-testid={testId}>
          {valor}
        </span>
        {unidad && <span className="text-[12px] text-c2-muted">{unidad}</span>}
      </p>
      {nota && <p className="mt-1 text-[11px] text-c2-muted truncate">{nota}</p>}
    </div>
  )
}

export interface PuntoMapa {
  lat: number
  lng: number
}

/** Reduce una polilinea a como mucho `max` puntos (submuestreo uniforme, conserva los extremos). */
export function simplificar(puntos: PuntoMapa[], max = 80): PuntoMapa[] {
  if (puntos.length <= max) return puntos
  const paso = (puntos.length - 1) / (max - 1)
  return Array.from({ length: max }, (_, i) => puntos[Math.round(i * paso)]!)
}

/** Proyecta lat/lng a un lienzo de ancho x alto con margen, sin deformar (equirectangular). */
export function proyectar(puntos: PuntoMapa[], ancho: number, alto: number, margen = 8) {
  if (!puntos.length) return []
  const latMedia = puntos.reduce((s, p) => s + p.lat, 0) / puntos.length
  const k = Math.cos((latMedia * Math.PI) / 180)
  const xs = puntos.map((p) => p.lng * k)
  const ys = puntos.map((p) => -p.lat)
  const [x0, x1] = [Math.min(...xs), Math.max(...xs)]
  const [y0, y1] = [Math.min(...ys), Math.max(...ys)]
  const esc = Math.min(
    (ancho - 2 * margen) / Math.max(x1 - x0, 1e-9),
    (alto - 2 * margen) / Math.max(y1 - y0, 1e-9)
  )
  const dx = (ancho - (x1 - x0) * esc) / 2
  const dy = (alto - (y1 - y0) * esc) / 2
  return xs.map((x, i) => ({ x: dx + (x - x0) * esc, y: dy + (ys[i]! - y0) * esc }))
}

/**
 * Mini mapa SVG del recorrido: cuadricula de fondo, trazo con degradado morado, inicio en menta y
 * fin en blanco. Sin puntos muestra la cuadricula con un rotulo.
 */
export function MiniMapa({
  puntos,
  ancho = 120,
  alto = 64,
  className = '',
  grosor = 2.5,
  marcadores = [],
  etiqueta,
  testId,
}: {
  puntos: PuntoMapa[] | undefined
  ancho?: number
  alto?: number
  className?: string
  grosor?: number
  /** Puntos extra (incidentes, corredores) sobre el mismo encuadre. */
  marcadores?: (PuntoMapa & { color: string; titulo?: string })[]
  etiqueta?: string
  testId?: string
}) {
  const traza = simplificar(puntos ?? [])
  const todos = [...traza, ...marcadores]
  const xy = proyectar(todos, ancho, alto, Math.max(6, grosor * 3))
  const linea = xy.slice(0, traza.length)
  const marcas = xy.slice(traza.length)
  const idGrad = `grad-${Math.abs(hash(traza.map((p) => p.lat + p.lng).join()))}`
  const d = linea.map((p, i) => `${i ? 'L' : 'M'}${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ')
  return (
    <svg
      viewBox={`0 0 ${ancho} ${alto}`}
      className={`mapa-c2 block ${className}`}
      role="img"
      aria-label={etiqueta ?? (traza.length > 1 ? 'Mapa del recorrido' : 'Sin trazado')}
      data-testid={testId}
      preserveAspectRatio="xMidYMid meet"
    >
      <defs>
        <linearGradient id={idGrad} x1="0" x2="1">
          <stop offset="0" stopColor="#7B1FA2" />
          <stop offset="1" stopColor="#C084FC" />
        </linearGradient>
      </defs>
      {traza.length > 1 && (
        <>
          <path
            d={d}
            fill="none"
            stroke="#A855F7"
            strokeOpacity="0.25"
            strokeWidth={grosor * 3}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <path
            d={d}
            fill="none"
            stroke={`url(#${idGrad})`}
            strokeWidth={grosor}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <circle cx={linea[0]!.x} cy={linea[0]!.y} r={grosor * 1.2} fill="#68DBA9" />
          <circle cx={linea.at(-1)!.x} cy={linea.at(-1)!.y} r={grosor * 1.2} fill="#FFFFFF" />
        </>
      )}
      {marcas.map((p, i) => (
        <circle
          key={i}
          cx={p.x}
          cy={p.y}
          r={grosor * 1.6}
          fill={marcadores[i]!.color}
          stroke="#0C1322"
          strokeWidth="1"
        >
          {marcadores[i]!.titulo && <title>{marcadores[i]!.titulo}</title>}
        </circle>
      ))}
    </svg>
  )
}

function hash(s: string) {
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0
  return h
}

/** Anillo de progreso con el porcentaje en el centro. */
export function Anillo({
  pct,
  tamano = 88,
  grosor = 8,
  texto,
  color = '#A855F7',
  testId,
}: {
  pct: number
  tamano?: number
  grosor?: number
  texto?: ReactNode
  color?: string
  testId?: string
}) {
  const r = (tamano - grosor) / 2
  const c = 2 * Math.PI * r
  const v = Math.max(0, Math.min(100, pct))
  return (
    <div
      className="relative shrink-0"
      style={{ width: tamano, height: tamano }}
      data-testid={testId}
    >
      <svg
        width={tamano}
        height={tamano}
        viewBox={`0 0 ${tamano} ${tamano}`}
        role="img"
        aria-label={`${Math.round(v)} %`}
      >
        <circle
          cx={tamano / 2}
          cy={tamano / 2}
          r={r}
          fill="none"
          stroke="#232A3A"
          strokeWidth={grosor}
        />
        <circle
          cx={tamano / 2}
          cy={tamano / 2}
          r={r}
          fill="none"
          stroke={color}
          strokeWidth={grosor}
          strokeLinecap="round"
          strokeDasharray={`${(v / 100) * c} ${c}`}
          transform={`rotate(-90 ${tamano / 2} ${tamano / 2})`}
        />
      </svg>
      <span className="absolute inset-0 flex items-center justify-center cifra-c2 text-[20px]">
        {texto ?? `${Math.round(v)}%`}
      </span>
    </div>
  )
}

/** Barras verticales con etiqueta abajo (dias de la semana, semanas). */
export function Barras({
  valores,
  etiquetas,
  alto = 96,
  resaltar,
  formato = (v: number) => v.toFixed(1),
  testId,
  className = '',
}: {
  valores: number[]
  etiquetas: string[]
  alto?: number
  resaltar?: number
  formato?: (v: number) => string
  testId?: string
  className?: string
}) {
  const max = Math.max(...valores, 0.0001)
  return (
    <div
      className={`flex items-end gap-2 ${className}`}
      data-testid={testId}
      role="img"
      aria-label={valores.map((v, i) => `${etiquetas[i]}: ${formato(v)}`).join(', ')}
    >
      {valores.map((v, i) => (
        <div key={i} className="flex-1 min-w-0 flex flex-col items-center gap-1.5">
          <div className="w-full flex items-end" style={{ height: alto }}>
            <div
              className={`w-full rounded-t-[3px] ${i === resaltar ? 'bg-[linear-gradient(180deg,#C084FC,#7B1FA2)]' : v > 0 ? 'bg-[#4B3A6B]' : 'bg-c2-s2'}`}
              style={{ height: v > 0 ? `${Math.max(6, (v / max) * 100)}%` : 3 }}
              title={formato(v)}
            />
          </div>
          <span
            className={`text-[10px] font-semibold ${i === resaltar ? 'text-white' : 'text-c2-muted'}`}
          >
            {etiquetas[i]}
          </span>
        </div>
      ))}
    </div>
  )
}

/** Barra horizontal de progreso (cupos, logros, reto). */
export function Progreso({
  pct,
  color = 'grad',
  alto = 6,
  etiqueta,
}: {
  pct: number
  color?: 'grad' | 'menta' | 'ambar'
  alto?: number
  etiqueta?: string
}) {
  const fondo =
    color === 'menta'
      ? 'bg-c2-menta'
      : color === 'ambar'
        ? 'bg-amber-400'
        : 'bg-[linear-gradient(90deg,#7B1FA2,#A855F7)]'
  const v = Math.max(0, Math.min(100, pct))
  return (
    <div
      className="w-full rounded-full bg-c2-s2 overflow-hidden"
      style={{ height: alto }}
      role="progressbar"
      aria-valuenow={Math.round(v)}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-label={etiqueta}
    >
      <div className={`h-full rounded-full ${fondo}`} style={{ width: `${v}%` }} />
    </div>
  )
}

/** Mapa de calor tipo GitHub: columnas = semanas, filas = dias (lunes arriba). */
export function MapaCalor({
  kmPorDia,
  semanas = 26,
  ahoraMs = Date.now(),
  testId,
}: {
  kmPorDia: Map<string, number>
  semanas?: number
  ahoraMs?: number
  testId?: string
}) {
  const hoy = new Date(ahoraMs - 5 * 3_600_000)
  const dowHoy = (hoy.getUTCDay() + 6) % 7
  const lunesActual =
    Date.UTC(hoy.getUTCFullYear(), hoy.getUTCMonth(), hoy.getUTCDate()) - dowHoy * 86_400_000
  const max = Math.max(1, ...kmPorDia.values())
  const celdas: { clave: string; km: number; futuro: boolean }[][] = []
  for (let s = semanas - 1; s >= 0; s--) {
    const col = []
    for (let d = 0; d < 7; d++) {
      const ms = lunesActual - s * 7 * 86_400_000 + d * 86_400_000
      const clave = new Date(ms).toISOString().slice(0, 10)
      col.push({ clave, km: kmPorDia.get(clave) ?? 0, futuro: s === 0 && d > dowHoy })
    }
    celdas.push(col)
  }
  const tono = (km: number) => {
    if (km <= 0) return '#232A3A'
    const f = km / max
    return f > 0.75 ? '#C084FC' : f > 0.5 ? '#A855F7' : f > 0.25 ? '#7B1FA2' : '#4B2466'
  }
  return (
    <div
      className="flex gap-[3px] overflow-x-auto no-scrollbar"
      data-testid={testId}
      role="img"
      aria-label={`Constancia de las ultimas ${semanas} semanas`}
    >
      {celdas.map((col, i) => (
        <div key={i} className="flex flex-col gap-[3px]">
          {col.map((c) => (
            <span
              key={c.clave}
              title={c.futuro ? '' : `${c.clave}: ${c.km.toFixed(1)} km`}
              className="block w-[11px] h-[11px] rounded-[2px]"
              style={{ background: c.futuro ? 'transparent' : tono(c.km) } as CSSProperties}
            />
          ))}
        </div>
      ))}
    </div>
  )
}

/** Clave yyyy-mm-dd del dia en Loja (UTC-5). */
export const diaLoja = (ms: number) => new Date(ms - 5 * 3_600_000).toISOString().slice(0, 10)

/** Esqueleto de un panel del rediseno (titulo y lineas). */
export function EsqueletoPanel({
  alto = 'h-48',
  className = '',
  testId = 'esqueleto-panel',
}: {
  alto?: string
  className?: string
  testId?: string
}) {
  return (
    <div
      role="status"
      aria-busy="true"
      data-testid={testId}
      className={`panel-c2 bg-c2-card border border-c2-linea p-5 space-y-3 ${className}`}
    >
      <span className="sr-only">Cargando</span>
      <Esqueleto className="h-5 w-40 rounded" />
      <Esqueleto className={`${alto} w-full rounded-[4px]`} />
    </div>
  )
}

/** Iniciales de un nombre (2 letras). */
export const iniciales = (n: string) =>
  n
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]!.toUpperCase())
    .join('') || 'R'

const COLORES_AVATAR = ['#7B1FA2', '#0E9F6E', '#2563EB', '#DB2777', '#D97706', '#0891B2']

/** Avatar circular con iniciales, color estable por nombre. */
export function AvatarIniciales({
  nombre,
  tamano = 28,
  className = '',
}: {
  nombre: string
  tamano?: number
  className?: string
}) {
  const color = COLORES_AVATAR[Math.abs(hash(nombre)) % COLORES_AVATAR.length]
  return (
    <span
      aria-hidden="true"
      className={`inline-flex shrink-0 items-center justify-center rounded-full font-bold text-white ring-2 ring-c2-card ${className}`}
      style={{
        width: tamano,
        height: tamano,
        background: color,
        fontSize: Math.round(tamano * 0.38),
      }}
    >
      {iniciales(nombre)}
    </span>
  )
}
