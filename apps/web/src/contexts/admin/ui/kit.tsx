import { useCallback, useEffect, useId, useRef, useState, type ReactNode } from 'react'

/* Piezas comunes del panel "Escenario". Los colores salen de los tokens de escenario.css. */

export type Tono = 'pri' | 'ok' | 'warn' | 'bad' | 'info' | 'neu'

/** Carga asincrona con estado de error simple. */
export function useCarga<T>(f: () => Promise<T>, deps: unknown[]) {
  const [datos, setDatos] = useState<T | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [n, setN] = useState(0)
  useEffect(() => {
    let vivo = true
    f().then(
      (d) => vivo && setDatos(d),
      (e: Error) => vivo && setError(e.message)
    )
    return () => {
      vivo = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, n])
  const recargar = useCallback(() => setN((x) => x + 1), [])
  return { datos, error, recargar }
}

export function iniciales(nombre: string): string {
  const p = nombre.trim().split(/\s+/).filter(Boolean)
  if (p.length === 0) return '?'
  return (p[0][0] + (p.length > 1 ? p[p.length - 1][0] : '')).toUpperCase()
}

/** "hace 6 min", "hace 3 h", "hace 2 d". Fechas invalidas devuelven cadena vacia. */
export function haceCuanto(iso: string | number, ahora = Date.now()): string {
  const ms = typeof iso === 'number' ? iso : new Date(iso).getTime()
  if (!Number.isFinite(ms)) return ''
  const min = Math.max(0, Math.round((ahora - ms) / 60_000))
  if (min < 1) return 'ahora'
  if (min < 60) return `hace ${min} min`
  if (min < 1440) return `hace ${Math.round(min / 60)} h`
  return `hace ${Math.round(min / 1440)} d`
}

export const entero = (n: number) => n.toLocaleString('es-EC')
export const decimal1 = (n: number) =>
  n.toLocaleString('es-EC', { minimumFractionDigits: 1, maximumFractionDigits: 1 })

export function Chip({
  tono = 'neu',
  children,
  titulo,
}: {
  tono?: Tono
  children: ReactNode
  titulo?: string
}) {
  return (
    <span className={`adm-chip c-${tono}`} title={titulo}>
      {children}
    </span>
  )
}

export function Punto({ tono = 'neu' }: { tono?: Tono }) {
  return <span className={`adm-dot c-${tono}`} aria-hidden />
}

export function Avatar({
  nombre,
  tono = 'pri',
  tam = 36,
}: {
  nombre: string
  tono?: Tono
  tam?: number
}) {
  return (
    <span
      className={`adm-av c-${tono}`}
      style={{ width: tam, height: tam, fontSize: Math.round(tam * 0.36) }}
      aria-hidden
    >
      {iniciales(nombre)}
    </span>
  )
}

export function Icono({ nombre, tam = 20 }: { nombre: string; tam?: number }) {
  return (
    <span className="material-symbols-outlined" style={{ fontSize: tam }} aria-hidden>
      {nombre}
    </span>
  )
}

export function Boton({
  children,
  onClick,
  tipo = 'sec',
  chico,
  completo,
  deshabilitado,
  aria,
  testId,
}: {
  children: ReactNode
  onClick: () => void
  tipo?: 'pri' | 'sec' | 'bad' | 'ok' | 'gho'
  chico?: boolean
  completo?: boolean
  deshabilitado?: boolean
  aria?: string
  testId?: string
}) {
  return (
    <button
      type="button"
      aria-label={aria}
      data-testid={testId}
      disabled={deshabilitado}
      onClick={onClick}
      className={`adm-btn adm-b-${tipo}${chico ? ' sm' : ''}${completo ? ' full' : ''}`}
    >
      {children}
    </button>
  )
}

export function Cabecera({
  kicker,
  children,
  extra,
}: {
  kicker: string
  children: ReactNode
  extra?: ReactNode
}) {
  return (
    <header className="adm-cab">
      <div>
        <p className="adm-kicker">{kicker}</p>
        <h1 className="adm-h1">{children}</h1>
      </div>
      {extra && <div className="adm-fila adm-envuelve">{extra}</div>}
    </header>
  )
}

export function ErrorCarga({ mensaje }: { mensaje: string | null }) {
  if (!mensaje) return null
  return (
    <p
      role="alert"
      className="adm-card-el adm-pad"
      style={{ marginBottom: 16, borderColor: 'hsl(var(--peligro))' }}
    >
      No se pudo cargar: {mensaje}. Revisa que las migraciones 020 y 023 estén aplicadas.
    </p>
  )
}

/** Estado vacio o con fuente pendiente: dice que falta y por que, sin inventar cifras. */
export function Vacio({
  titulo,
  texto,
  testId,
}: {
  titulo: string
  texto?: string
  testId?: string
}) {
  return (
    <div
      data-testid={testId}
      className="adm-card-el"
      style={{ padding: 16, borderStyle: 'dashed', borderColor: 'hsl(var(--borde-fuerte))' }}
    >
      <p style={{ fontWeight: 700 }}>{titulo}</p>
      {texto && (
        <p className="adm-t3" style={{ fontSize: 13, marginTop: 4 }}>
          {texto}
        </p>
      )}
    </div>
  )
}

export interface PuntoBarra {
  etiqueta: string
  valor: number
}

/** Barras verticales por dia. Resalta la ultima barra con el tono dado. */
export function BarrasDia({
  datos,
  tono = 'pri',
  alto = 150,
  unidad = '',
  testId,
  resaltarMax = false,
}: {
  datos: PuntoBarra[]
  tono?: Tono
  alto?: number
  unidad?: string
  testId?: string
  resaltarMax?: boolean
}) {
  const max = Math.max(1, ...datos.map((d) => d.valor))
  const marca = resaltarMax ? datos.findIndex((d) => d.valor === max) : datos.length - 1
  const cada = Math.max(1, Math.ceil(datos.length / 6))
  return (
    <figure data-testid={testId} className={`c-${tono}`} style={{ width: '100%' }}>
      <div
        className="adm-bars"
        style={{ height: alto }}
        role="img"
        aria-label={datos.map((d) => `${d.etiqueta}: ${d.valor}${unidad}`).join(', ')}
      >
        {datos.map((d, i) => (
          <i
            key={`${d.etiqueta}-${i}`}
            className={i === marca ? 'on' : ''}
            title={`${d.etiqueta}: ${d.valor}${unidad}`}
            style={{ height: `${Math.max(d.valor > 0 ? 4 : 0, (d.valor / max) * 100)}%` }}
          />
        ))}
      </div>
      <figcaption className="adm-t3" style={{ display: 'flex', fontSize: 11, marginTop: 6 }}>
        {datos.map((d, i) => (
          <span
            key={i}
            style={{ flex: 1, textAlign: 'center', whiteSpace: 'nowrap', overflow: 'visible' }}
          >
            {i % cada === 0 ? d.etiqueta : ''}
          </span>
        ))}
      </figcaption>
    </figure>
  )
}

/** Mini serie de barras sin ejes, para tarjetas de cifras. */
export function MiniBarras({ valores, tono = 'pri' }: { valores: number[]; tono?: Tono }) {
  const max = Math.max(1, ...valores)
  return (
    <div className={`adm-bars c-${tono}`} style={{ height: 36, width: 96, gap: 3 }} aria-hidden>
      {valores.map((v, i) => (
        <i
          key={i}
          className={i === valores.length - 1 ? 'on' : 'soft'}
          style={{ height: `${Math.max(v > 0 ? 8 : 0, (v / max) * 100)}%`, minWidth: 2 }}
        />
      ))}
    </div>
  )
}

export function Interruptor({
  activo,
  onCambio,
  etiqueta,
}: {
  activo: boolean
  onCambio: () => void
  etiqueta: string
}) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={activo}
      aria-label={etiqueta}
      onClick={onCambio}
      className="adm-sw"
    >
      <span />
    </button>
  )
}

/** Dialogo modal propio del panel (confirmaciones y formularios cortos). */
export function Dialogo({
  abierto,
  titulo,
  onCerrar,
  children,
  acciones,
  testId,
}: {
  abierto: boolean
  titulo: string
  onCerrar: () => void
  children?: ReactNode
  acciones: ReactNode
  testId?: string
}) {
  const id = useId()
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (!abierto) return
    ref.current?.focus()
    const alTeclear = (e: KeyboardEvent) => e.key === 'Escape' && onCerrar()
    window.addEventListener('keydown', alTeclear)
    return () => window.removeEventListener('keydown', alTeclear)
  }, [abierto, onCerrar])
  if (!abierto) return null
  return (
    <div className="adm-velo" onMouseDown={(e) => e.target === e.currentTarget && onCerrar()}>
      <div
        ref={ref}
        tabIndex={-1}
        role="dialog"
        aria-modal="true"
        aria-labelledby={id}
        data-testid={testId}
        className="adm-dialogo"
      >
        <h2 id={id} className="adm-h2" style={{ marginBottom: 8 }}>
          {titulo}
        </h2>
        {children}
        <div className="adm-fila" style={{ justifyContent: 'flex-end', marginTop: 20, gap: 8 }}>
          {acciones}
        </div>
      </div>
    </div>
  )
}

/** Confirmacion de acciones peligrosas o irreversibles. */
export function Confirmar({
  abierto,
  titulo,
  texto,
  confirmar,
  onConfirmar,
  onCerrar,
  peligro = true,
}: {
  abierto: boolean
  titulo: string
  texto: string
  confirmar: string
  onConfirmar: () => void
  onCerrar: () => void
  peligro?: boolean
}) {
  return (
    <Dialogo
      abierto={abierto}
      titulo={titulo}
      onCerrar={onCerrar}
      acciones={
        <>
          <Boton onClick={onCerrar}>Cancelar</Boton>
          <Boton tipo={peligro ? 'bad' : 'pri'} onClick={onConfirmar}>
            {confirmar}
          </Boton>
        </>
      }
    >
      <p className="adm-t2" style={{ fontSize: 14 }}>
        {texto}
      </p>
    </Dialogo>
  )
}
