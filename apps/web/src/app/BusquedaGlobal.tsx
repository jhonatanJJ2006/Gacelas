import { useEffect, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useNavigate } from 'react-router-dom'
import { useComunidad } from '../contexts/comunidad/api'
import { clubes as repoClubes, type Club } from '../contexts/eventos/api'
import { useRutas } from '../contexts/rutas/api'
import type { CorredorPublico } from '../shared/kernel/comunidad'
import { useEventosVisibles } from './datosApp'

/** Pantallas que se pueden abrir desde la busqueda. */
const PANTALLAS = [
  { titulo: 'Inicio', ruta: '/inicio', icono: 'home' },
  { titulo: 'Correr', ruta: '/correr', icono: 'play_circle' },
  { titulo: 'Rutas', ruta: '/rutas', icono: 'map' },
  { titulo: 'Comunidad', ruta: '/comunidad', icono: 'groups' },
  { titulo: 'Clubes y grupos', ruta: '/comunidad/clubes', icono: 'shield' },
  { titulo: 'Eventos', ruta: '/comunidad/eventos', icono: 'event' },
  { titulo: 'En vivo', ruta: '/comunidad/en-vivo', icono: 'sensors' },
  { titulo: 'Corredores', ruta: '/comunidad/corredores', icono: 'person_search' },
  { titulo: 'Notificaciones', ruta: '/notificaciones', icono: 'notifications' },
  { titulo: 'Perfil', ruta: '/perfil', icono: 'person' },
  { titulo: 'Ajustes', ruta: '/ajustes', icono: 'settings' },
]

interface Resultado {
  grupo: string
  titulo: string
  detalle?: string
  ruta: string
  icono: string
}

/** Normaliza para buscar sin tildes ni mayusculas. */
export const normalizar = (s: string) =>
  s
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')

/** Abre la busqueda con Ctrl+K o Cmd+K (y "/" fuera de campos de texto). */
export function useAtajoBusqueda(abrir: () => void) {
  useEffect(() => {
    const alTeclear = (e: KeyboardEvent) => {
      const enCampo =
        e.target instanceof HTMLElement &&
        (e.target.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(e.target.tagName))
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault()
        abrir()
      } else if (e.key === '/' && !enCampo) {
        e.preventDefault()
        abrir()
      }
    }
    window.addEventListener('keydown', alTeclear)
    return () => window.removeEventListener('keydown', alTeclear)
  }, [abrir])
}

/** Busqueda global: pantallas, rutas, clubes, eventos y corredores (Supabase). */
export function BusquedaGlobal({ abierto, onCerrar }: { abierto: boolean; onCerrar: () => void }) {
  const navegar = useNavigate()
  const [texto, setTexto] = useState('')
  const [activo, setActivo] = useState(0)
  const [clubes, setClubes] = useState<Club[] | null>(null)
  const [corredores, setCorredores] = useState<CorredorPublico[]>([])
  const [buscando, setBuscando] = useState(false)
  const rutas = useRutas((s) => s.rutas)
  const { proximos } = useEventosVisibles()
  const buscarCorredores = useComunidad((s) => s.buscarCorredores)
  const campo = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (!abierto) return
    setTexto('')
    setActivo(0)
    window.setTimeout(() => campo.current?.focus(), 0)
    if (clubes === null && repoClubes.disponible())
      void repoClubes
        .listar()
        .then((r) => setClubes(r.todos.filter((c) => !c.oculto)))
        .catch(() => setClubes([]))
  }, [abierto]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const q = texto.trim()
    if (!abierto || q.length < 2) {
      setCorredores([])
      return
    }
    setBuscando(true)
    const id = window.setTimeout(() => {
      void buscarCorredores(q)
        .then((r) => setCorredores(r.slice(0, 5)))
        .catch(() => setCorredores([]))
        .finally(() => setBuscando(false))
    }, 250)
    return () => window.clearTimeout(id)
  }, [texto, abierto, buscarCorredores])

  const resultados = useMemo<Resultado[]>(() => {
    const q = normalizar(texto.trim())
    const coincide = (...c: (string | undefined)[]) =>
      !q || c.some((x) => x && normalizar(x).includes(q))
    const lista: Resultado[] = []
    PANTALLAS.filter((p) => coincide(p.titulo))
      .slice(0, q ? 5 : 6)
      .forEach((p) =>
        lista.push({ grupo: 'Pantallas', titulo: p.titulo, ruta: p.ruta, icono: p.icono })
      )
    if (q) {
      rutas
        .filter((r) => coincide(r.nombre, r.descripcion))
        .slice(0, 5)
        .forEach((r) =>
          lista.push({
            grupo: 'Rutas',
            titulo: r.nombre,
            detalle: `${r.distanciaKm.toFixed(1)} km · ${r.dificultad}`,
            ruta: `/rutas/${r.id}`,
            icono: 'route',
          })
        )
      ;(clubes ?? [])
        .filter((c) => coincide(c.nombre, c.ubicacion, c.lugarEncuentro))
        .slice(0, 4)
        .forEach((c) =>
          lista.push({
            grupo: 'Clubes y grupos',
            titulo: c.nombre,
            detalle: `${c.miembros} miembros`,
            ruta: `/comunidad/clubes/${c.id}`,
            icono: 'shield',
          })
        )
      proximos
        .filter((e) => coincide(e.titulo, e.ubicacion))
        .slice(0, 4)
        .forEach((e) =>
          lista.push({
            grupo: 'Eventos',
            titulo: e.titulo,
            detalle: `${e.fecha} · ${e.hora}`,
            ruta: `/comunidad/eventos/${e.id}`,
            icono: 'event',
          })
        )
      corredores.forEach((c) =>
        lista.push({
          grupo: 'Corredores',
          titulo: c.nombre,
          detalle: c.username ? `@${c.username}` : c.ubicacion,
          ruta: `/comunidad/corredores?q=${encodeURIComponent(c.username || c.nombre)}`,
          icono: 'person',
        })
      )
    }
    return lista
  }, [texto, rutas, clubes, proximos, corredores])

  useEffect(() => setActivo(0), [texto])

  if (!abierto) return null
  const ir = (r: Resultado) => {
    onCerrar()
    navegar(r.ruta)
  }
  const alTeclear = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') onCerrar()
    else if (e.key === 'ArrowDown') {
      e.preventDefault()
      setActivo((a) => Math.min(resultados.length - 1, a + 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setActivo((a) => Math.max(0, a - 1))
    } else if (e.key === 'Enter' && resultados[activo]) ir(resultados[activo])
  }
  let grupoPrevio = ''
  return createPortal(
    <div
      className="mov-velo fixed inset-0 z-[110] bg-black/70 flex items-start justify-center p-3 sm:p-4 pt-[max(env(safe-area-inset-top),12px)] sm:pt-[12vh]"
      onClick={onCerrar}
      data-testid="modal-busqueda"
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Buscar en Runcity"
        className="w-full max-w-[620px] bg-c2-card border border-c2-linea border-t-2 border-t-c2-violeta rounded-[10px] shadow-2xl flex flex-col max-h-[calc(100dvh-24px)] sm:max-h-[70vh] overflow-hidden"
        onClick={(e) => e.stopPropagation()}
        onKeyDown={alTeclear}
      >
        <div className="flex items-center gap-3 px-4 h-14 border-b border-c2-linea shrink-0">
          <span aria-hidden="true" className="material-symbols-outlined text-c2-muted">
            search
          </span>
          <input
            ref={campo}
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            placeholder="Buscar rutas, corredores, clubes, eventos"
            aria-label="Buscar"
            role="combobox"
            aria-expanded="true"
            aria-controls="resultados-busqueda"
            aria-activedescendant={resultados[activo] ? `resultado-${activo}` : undefined}
            className="flex-1 min-w-0 bg-transparent outline-none text-[15px] text-white placeholder:text-c2-muted"
          />
          <button
            type="button"
            onClick={onCerrar}
            className="text-[11px] font-semibold text-c2-muted border border-c2-linea rounded px-2 py-1 hover:text-white"
          >
            Esc
          </button>
        </div>
        <ul
          id="resultados-busqueda"
          role="listbox"
          className="overflow-y-auto overscroll-contain p-2 min-h-0"
        >
          {resultados.map((r, i) => {
            const cabecera = r.grupo !== grupoPrevio
            grupoPrevio = r.grupo
            return (
              <li key={`${r.grupo}-${r.ruta}-${i}`} role="presentation">
                {cabecera && (
                  <p className="rotulo-c2 px-3 pt-3 pb-1.5" role="presentation">
                    {r.grupo}
                  </p>
                )}
                <button
                  type="button"
                  id={`resultado-${i}`}
                  role="option"
                  aria-selected={i === activo}
                  onMouseEnter={() => setActivo(i)}
                  onClick={() => ir(r)}
                  className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-md text-left ${i === activo ? 'bg-c2-s2' : ''}`}
                >
                  <span
                    aria-hidden="true"
                    className="material-symbols-outlined text-[20px] text-c2-violeta"
                  >
                    {r.icono}
                  </span>
                  <span className="flex-1 min-w-0">
                    <span className="block text-[14px] text-white truncate">{r.titulo}</span>
                    {r.detalle && (
                      <span className="block text-[12px] text-c2-muted truncate">{r.detalle}</span>
                    )}
                  </span>
                  {i === activo && (
                    <span
                      aria-hidden="true"
                      className="material-symbols-outlined text-[18px] text-c2-muted"
                    >
                      keyboard_return
                    </span>
                  )}
                </button>
              </li>
            )
          })}
          {texto.trim().length >= 2 && buscando && (
            <li className="px-3 py-2 text-[12px] text-c2-muted" role="status">
              Buscando corredores...
            </li>
          )}
          {texto.trim() && !resultados.length && !buscando && (
            <li
              className="px-3 py-6 text-center text-[13px] text-c2-muted"
              data-testid="busqueda-vacia"
            >
              Sin resultados para "{texto.trim()}".
            </li>
          )}
        </ul>
        <div className="hidden sm:flex items-center gap-4 px-4 h-10 border-t border-c2-linea text-[11px] text-c2-muted shrink-0">
          <span>Flechas para moverte</span>
          <span>Enter para abrir</span>
          <span>Esc para cerrar</span>
        </div>
      </div>
    </div>,
    document.body
  )
}
