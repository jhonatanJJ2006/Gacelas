import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import './NavColapsable.css'

/*
 * Navbar B colapsable, generico: quien lo usa le pasa sus items (ruta, etiqueta, icono, insignia).
 * No conoce pantallas ni datos. Las insignias son opcionales y solo se pintan con un numero real
 * mayor que cero. El estado colapsado se guarda en localStorage con `claveAlmacen`.
 *
 * Los colores salen de variables --nc-* (ver NavColapsable.css); el contenedor las define.
 */

export interface ItemNav {
  /** Identificador estable del item. */
  id: string
  etiqueta: string
  /** Ruta absoluta (con query si hace falta). */
  a: string
  /** Nombre de un Material Symbol (outlined) o un nodo propio. */
  icono: string | ReactNode
  /** Cuenta real a mostrar sobre el icono; se omite si no hay numero o es 0. */
  insignia?: number
  /** Texto del globo de la seccion activa (por defecto la etiqueta). */
  globo?: string
  /** Una linea corta separa un grupo del anterior cuando el valor cambia. */
  grupo?: string
  /** Decide si el item esta activo para esta ruta. Por defecto, prefijo de `a`. */
  activoEn?: (pathname: string) => boolean
}

export interface UsuarioNavColapsable {
  nombre: string
  detalle?: string
  /** URL de la foto; sin ella se muestran las iniciales. */
  foto?: string | null
}

export interface NavColapsableProps {
  items: ItemNav[]
  usuario?: UsuarioNavColapsable
  /** Etiqueta accesible de la navegacion. */
  etiqueta?: string
  /** Clave de localStorage donde se guarda si esta colapsado. */
  claveAlmacen?: string
  /** Colapsado la primera vez que se abre. */
  colapsadoInicial?: boolean
  /** Muestra el buscador "ir a..." (Ctrl K) que filtra los items. */
  buscador?: boolean
  /** Contenido extra al pie del riel (por ejemplo un selector de tema). */
  pie?: ReactNode
  className?: string
}

const iniciales = (n: string) => {
  const p = n.trim().split(/\s+/).filter(Boolean)
  return p.length ? (p[0][0] + (p.length > 1 ? p[p.length - 1][0] : '')).toUpperCase() : '?'
}

function Icono({ v, tam = 22 }: { v: ItemNav['icono']; tam?: number }) {
  return typeof v === 'string' ? (
    <span className="material-symbols-outlined" style={{ fontSize: tam }} aria-hidden>
      {v}
    </span>
  ) : (
    <>{v}</>
  )
}

const cuenta = (n?: number) => (n && n > 0 ? (n > 99 ? '99+' : String(n)) : null)

function leerColapsado(clave: string, defecto: boolean) {
  try {
    const v = localStorage.getItem(clave)
    return v == null ? defecto : v === '1'
  } catch {
    return defecto
  }
}

export function NavColapsable({
  items,
  usuario,
  etiqueta = 'Navegación principal',
  claveAlmacen = 'gacelas.nav.colapsado',
  colapsadoInicial = true,
  buscador = true,
  pie,
  className = '',
}: NavColapsableProps) {
  const { pathname } = useLocation()
  const [colapsado, setColapsado] = useState(() => leerColapsado(claveAlmacen, colapsadoInicial))
  const [buscando, setBuscando] = useState(false)

  const alternar = () =>
    setColapsado((c) => {
      try {
        localStorage.setItem(claveAlmacen, c ? '0' : '1')
      } catch {
        /* sin almacenamiento: el estado vive solo en memoria */
      }
      return !c
    })

  const activo = useMemo(() => {
    const i = items.findIndex((it) => (it.activoEn ? it.activoEn(pathname) : pathname.startsWith(it.a.split('?')[0])))
    return i
  }, [items, pathname])

  useEffect(() => {
    if (!buscador) return
    const alTeclear = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault()
        setBuscando(true)
      }
    }
    window.addEventListener('keydown', alTeclear)
    return () => window.removeEventListener('keydown', alTeclear)
  }, [buscador])

  return (
    <div className={`navc ${className}`}>
      <nav aria-label={etiqueta} className="navc-riel" data-expandido={!colapsado} data-testid="nav-riel">
        {usuario && (
          <div className="navc-cab">
            <span className="navc-av" aria-hidden style={usuario.foto ? { background: `center/cover url(${usuario.foto})` } : undefined}>
              {usuario.foto ? '' : iniciales(usuario.nombre)}
            </span>
            <span className="navc-nombre">
              <b>{usuario.nombre}</b>
              {usuario.detalle && <small>{usuario.detalle}</small>}
            </span>
            <button type="button" className="navc-btn navc-cierra" onClick={alternar} aria-label="Contraer menú">
              <Icono v="chevron_left" tam={18} />
            </button>
          </div>
        )}
        <div className="navc-lista">
          {items.map((it, i) => {
            const n = cuenta(it.insignia)
            return (
              <div key={it.id} style={{ display: 'contents' }}>
                {i > 0 && it.grupo !== items[i - 1].grupo && <div className="navc-sep" role="separator" />}
                <Link
                  to={it.a}
                  className="navc-item"
                  aria-current={i === activo ? 'page' : undefined}
                  aria-label={n ? `${it.etiqueta}, ${n} pendientes` : it.etiqueta}
                  data-testid={`nav-${it.id}`}
                >
                  <span className="navc-ico">
                    <Icono v={it.icono} />
                    {n && <span className="navc-ins" data-testid={`nav-ins-${it.id}`}>{n}</span>}
                  </span>
                  <span className="navc-etq">{it.etiqueta}</span>
                  {n && <span className="navc-ins navc-ins-t">{n}</span>}
                  {i === activo && <span className="navc-globo" aria-hidden>{it.globo ?? it.etiqueta}</span>}
                </Link>
              </div>
            )
          })}
        </div>
        <div className="navc-pie">
          {pie}
          {buscador && (
            <button type="button" className="navc-btn navc-busca" onClick={() => setBuscando(true)} aria-label="Buscar o ir a una sección">
              <Icono v="search" tam={18} />
              <span className="navc-busca-t">Buscar o ir a...</span>
              <kbd className="navc-atajo">Ctrl K</kbd>
            </button>
          )}
          <button
            type="button"
            className="navc-btn"
            onClick={alternar}
            aria-expanded={!colapsado}
            aria-label={colapsado ? 'Expandir menú' : 'Contraer menú'}
            data-testid="nav-alternar"
          >
            <Icono v={colapsado ? 'chevron_right' : 'chevron_left'} tam={20} />
          </button>
        </div>
      </nav>

      <nav aria-label={`${etiqueta} (móvil)`} className="navc-barra" data-testid="nav-barra">
        <div className="navc-barra-i" style={{ ['--n' as string]: items.length, ['--i' as string]: Math.max(activo, 0) }}>
          {items.map((it, i) => {
            const n = cuenta(it.insignia)
            return (
              <Link
                key={it.id}
                to={it.a}
                className="navc-slot"
                aria-current={i === activo ? 'page' : undefined}
                aria-label={n ? `${it.etiqueta}, ${n} pendientes` : it.etiqueta}
                data-testid={`navm-${it.id}`}
              >
                <span className="navc-ico">
                  <Icono v={it.icono} />
                  {n && <span className="navc-ins">{n}</span>}
                </span>
                {it.etiqueta}
              </Link>
            )
          })}
        </div>
        {activo >= 0 && (
          <span className="navc-burbuja" style={{ ['--n' as string]: items.length, ['--i' as string]: activo }} aria-hidden>
            <Icono v={items[activo].icono} />
            {cuenta(items[activo].insignia) && <span className="navc-ins">{cuenta(items[activo].insignia)}</span>}
          </span>
        )}
      </nav>

      {buscando && <Paleta items={items} onCerrar={() => setBuscando(false)} />}
    </div>
  )
}

function Paleta({ items, onCerrar }: { items: ItemNav[]; onCerrar: () => void }) {
  const [q, setQ] = useState('')
  const [sel, setSel] = useState(0)
  const navegar = useNavigate()
  const ref = useRef<HTMLInputElement>(null)
  const norm = (s: string) => s.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase()
  const lista = items.filter((i) => norm(i.etiqueta).includes(norm(q)))
  useEffect(() => ref.current?.focus(), [])
  return (
    <div className="navc-velo" onMouseDown={(e) => e.target === e.currentTarget && onCerrar()}>
      <div className="navc-pal" role="dialog" aria-modal="true" aria-label="Ir a una sección">
        <input
          ref={ref}
          value={q}
          placeholder="Ir a..."
          aria-label="Buscar sección"
          onChange={(e) => {
            setQ(e.target.value)
            setSel(0)
          }}
          onKeyDown={(e) => {
            if (e.key === 'Escape') onCerrar()
            if (e.key === 'ArrowDown') setSel((s) => Math.min(s + 1, lista.length - 1))
            if (e.key === 'ArrowUp') setSel((s) => Math.max(s - 1, 0))
            if (e.key === 'Enter' && lista[sel]) {
              navegar(lista[sel].a)
              onCerrar()
            }
          }}
        />
        <ul>
          {lista.map((i, k) => (
            <li key={i.id}>
              <Link to={i.a} data-sel={k === sel} onClick={onCerrar}>
                <Icono v={i.icono} tam={20} />
                {i.etiqueta}
              </Link>
            </li>
          ))}
          {lista.length === 0 && <li style={{ padding: '10px 12px', opacity: 0.7 }}>Sin resultados</li>}
        </ul>
      </div>
    </div>
  )
}
