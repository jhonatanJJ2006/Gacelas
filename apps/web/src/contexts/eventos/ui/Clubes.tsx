import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAviso } from '../../../hooks'
import { Aviso } from '../../../shared/ui/ui'
import { useIdentidad } from '../../identidad/api'
import {
  clubes,
  estaLleno,
  ETIQUETA_NIVEL,
  filtrarClubes,
  textoHorario,
  textoRitmo,
  urlPublica,
  type Club,
  type NivelGrupo,
  type TipoClub,
} from '../api'

const campo =
  'w-full h-14 pl-11 pr-11 bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary focus:border-primary-600 outline-none'

/** Chip C2 (Android Chip de ClubesRegistro): esquinas cortadas arriba-izq y abajo-der. */
const chip = (activo: boolean) =>
  `forma-boton-c2 h-9 px-3 text-xs font-semibold whitespace-nowrap shrink-0 ${activo ? 'bg-primary-600/35 text-text-primary shadow-[inset_0_0_0_1px_#c084fc]' : 'bg-surface-container text-text-secondary'}`

function Dato({ icono, children }: { icono: string; children: React.ReactNode }) {
  return (
    <span className="flex items-center gap-1.5 min-w-0 text-xs text-text-secondary">
      <span aria-hidden className="material-symbols-outlined text-[16px] text-c2-violeta">
        {icono}
      </span>
      <span className="truncate">{children}</span>
    </span>
  )
}

/** Estado vacio ilustrado (Android VacioClubes). */
function Vacio({
  icono,
  titulo,
  texto,
  accion,
  principal = true,
  onAccion,
}: {
  icono: string
  titulo: string
  texto: string
  accion?: string
  principal?: boolean
  onAccion?: () => void
}) {
  return (
    <div
      data-testid="vacio-clubes"
      className="flex flex-col items-center text-center gap-3 pt-8 px-2"
    >
      <div className="tarjeta-c2 !bg-c2-morado/25 w-24 h-24 flex items-center justify-center">
        <span aria-hidden className="material-symbols-outlined text-[48px] text-c2-violeta">
          {icono}
        </span>
      </div>
      <h2 className="font-headline-sm text-[24px] italic text-text-primary">{titulo}</h2>
      <p className="text-text-secondary">{texto}</p>
      {accion &&
        (principal ? (
          <button data-testid="vacio-accion" onClick={onAccion} className="boton-c2 mt-1">
            <span aria-hidden className="material-symbols-outlined">
              add
            </span>
            {accion}
          </button>
        ) : (
          <button
            data-testid="vacio-accion"
            onClick={onAccion}
            className="h-12 px-4 font-semibold text-primary-400"
          >
            {accion}
          </button>
        ))}
    </div>
  )
}

/** Esqueleto de carga con la forma de las tarjetas (Android EsqueletoClubes). */
function Esqueleto() {
  return (
    <div data-testid="cargando-clubes" aria-busy="true" aria-label="Cargando" className="space-y-3">
      {[0, 1, 2].map((i) => (
        <div key={i} className="tarjeta-c2 p-4 space-y-3 animate-pulse">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-xl bg-c2-s2" />
            <div className="flex-1 space-y-2">
              <div className="h-[18px] w-3/5 rounded bg-c2-s2" />
              <div className="h-3 w-2/5 rounded bg-c2-s2" />
            </div>
          </div>
          <div className="h-3 w-4/5 rounded bg-c2-s2" />
        </div>
      ))}
    </div>
  )
}

/** Logo o iniciales del grupo o comunidad. */
export function LogoClub({ club, grande = false }: { club: Club; grande?: boolean }) {
  const url = urlPublica(club.logo)
  const tam = grande ? 'w-16 h-16 text-xl' : 'w-12 h-12 text-sm'
  if (url)
    return (
      <img
        src={url}
        alt=""
        className={`${tam} rounded-xl object-cover shrink-0 bg-surface-container-high`}
      />
    )
  const iniciales = club.nombre
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p[0] ?? '')
    .join('')
    .toUpperCase()
  return (
    <div
      aria-hidden
      className={`${tam} rounded-xl shrink-0 flex items-center justify-center font-bold ${club.tipo === 'comunidad' ? 'bg-secondary-600/25 text-secondary-400' : 'bg-primary-600/25 text-primary-400'}`}
    >
      {iniciales || 'G'}
    </div>
  )
}

export function Clubes() {
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const tipo: TipoClub = params.get('tipo') === 'comunidad' ? 'comunidad' : 'grupo'
  const sesion = useIdentidad((s) => s.sesion)
  const [lista, setLista] = useState<Club[] | null>(null)
  const [error, setError] = useState(false)
  const [mios, setMios] = useState<Set<string>>(new Set())
  const [solicitados, setSolicitados] = useState<Set<string>>(new Set())
  const [consulta, setConsulta] = useState('')
  const [nivel, setNivel] = useState<NivelGrupo>('todos')
  const [aviso, mostrarAviso] = useAviso()

  const cargar = useCallback(() => {
    if (!clubes.disponible()) return setLista([])
    setError(false)
    setLista(null)
    void clubes
      .listar()
      .then((r) => {
        setLista(r.todos)
        setMios(r.mios)
      })
      .catch(() => {
        setError(true)
        setLista([])
      })
  }, [])

  useEffect(cargar, [cargar, sesion])

  const comunidades = useMemo(
    () => new Map((lista ?? []).filter((c) => c.tipo === 'comunidad').map((c) => [c.id, c])),
    [lista]
  )
  const visibles = filtrarClubes(lista ?? [], {
    tipo,
    consulta,
    nivel: tipo === 'grupo' ? nivel : null,
  })
  const propios = visibles.filter((c) => mios.has(c.id))
  const otros = visibles.filter((c) => !mios.has(c.id))
  const plural = tipo === 'comunidad' ? 'comunidades' : 'grupos'
  const singular = tipo === 'comunidad' ? 'comunidad' : 'grupo'
  const total = (t: TipoClub) => (lista ?? []).filter((c) => c.tipo === t).length
  const filtrando = consulta !== '' || nivel !== 'todos'
  const crear = () => navigate(`/comunidad/clubes/nuevo?tipo=${tipo}`)

  const unirme = async (c: Club) => {
    try {
      await clubes.unirse(c.id)
      if (c.conAprobacion) {
        setSolicitados((s) => new Set(s).add(c.id))
        mostrarAviso(`Solicitud enviada a ${c.nombre}`)
      } else {
        mostrarAviso(`Te uniste a ${c.nombre}`)
        cargar()
      }
    } catch (e) {
      mostrarAviso(
        e instanceof Error && e.message ? e.message : 'No pudimos unirte. Inténtalo de nuevo.'
      )
    }
  }

  const tarjeta = (c: Club) => {
    const comunidad = c.comunidadId ? comunidades.get(c.comunidadId) : undefined
    const horario = textoHorario(c.dias, c.hora)
    const ritmo = c.tipo === 'grupo' ? textoRitmo(c.ritmoSegKm) : ''
    const mio = mios.has(c.id)
    const grupos =
      c.tipo === 'comunidad' ? (lista ?? []).filter((g) => g.comunidadId === c.id).length : 0
    const lugar = c.lugarEncuentro || c.ciudad
    const etiqueta =
      c.tipo === 'grupo'
        ? [ETIQUETA_NIVEL[c.nivel], comunidad?.nombre].filter(Boolean).join(' · ')
        : 'Comunidad'
    return (
      <li key={c.id} className="tarjeta-c2 p-4 space-y-3">
        <Link
          to={`/comunidad/clubes/${c.id}`}
          data-testid="tarjeta-club"
          className="flex items-center gap-3"
        >
          <LogoClub club={c} />
          <div className="min-w-0 flex-1">
            <p className="etiqueta-c2 !text-c2-violeta truncate">{etiqueta}</p>
            <p className="font-headline-sm text-[20px] italic text-text-primary truncate">
              {c.nombre}
            </p>
          </div>
          {mio && <span className="pildora-c2 shrink-0 text-xs text-c2-menta">Miembro</span>}
          {c.conAprobacion && !mio && (
            <span className="material-symbols-outlined text-text-secondary" aria-label="Privado">
              lock
            </span>
          )}
        </Link>
        <div className="grid grid-cols-2 gap-x-3 gap-y-1.5">
          <Dato icono="group">
            {c.miembros}
            {c.cupo != null ? `/${c.cupo}` : ''} {c.miembros === 1 ? 'miembro' : 'miembros'}
          </Dato>
          {ritmo && <Dato icono="speed">{ritmo}</Dato>}
          {c.tipo === 'comunidad' && (
            <Dato icono="directions_run">
              {grupos} {grupos === 1 ? 'grupo' : 'grupos'}
            </Dato>
          )}
          {horario && <Dato icono="schedule">{horario}</Dato>}
          {lugar && <Dato icono="place">{lugar}</Dato>}
        </div>
        {!mio && sesion && (
          <div className="flex justify-end">
            {solicitados.has(c.id) ? (
              <span className="text-xs text-text-secondary">Solicitud enviada</span>
            ) : estaLleno(c) ? (
              <span className="text-xs text-text-secondary">Grupo lleno</span>
            ) : (
              <button
                data-testid="unirme-club"
                onClick={() => void unirme(c)}
                className="boton-c2-chico"
              >
                {c.conAprobacion ? 'Solicitar' : 'Unirme'}
              </button>
            )}
          </div>
        )}
      </li>
    )
  }

  return (
    <main className="w-full min-h-screen bg-surface contenedor-escritorio lg:max-w-4xl pb-28">
      <header className="flex items-center gap-1 px-margin h-16 bg-bg-secondary mb-4">
        <button
          onClick={() => navigate('/comunidad')}
          aria-label="Volver"
          className="w-12 h-12 -ml-3 shrink-0 flex items-center justify-center text-text-primary"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </button>
        <h1 className="flex-1 min-w-0 font-headline-sm text-[22px] italic text-text-primary truncate">
          Grupos y comunidades
        </h1>
        {sesion && (
          <button
            onClick={crear}
            aria-label={tipo === 'comunidad' ? 'Registrar comunidad' : 'Registrar grupo'}
            data-testid="crear-club"
            className="forma-boton-c2 w-10 h-10 shrink-0 bg-c2-morado text-white flex items-center justify-center"
          >
            <span className="material-symbols-outlined">add</span>
          </button>
        )}
      </header>
      <div className="px-margin lg:px-0 space-y-4">
        <div role="tablist" className="grid grid-cols-2 gap-2">
          {(['grupo', 'comunidad'] as const).map((t) => (
            <button
              key={t}
              role="tab"
              aria-selected={tipo === t}
              data-testid={t === 'grupo' ? 'pestana-grupos' : 'pestana-comunidades'}
              onClick={() => setParams(t === 'grupo' ? {} : { tipo: t }, { replace: true })}
              className={chip(tipo === t)}
            >
              {t === 'grupo' ? 'Grupos' : 'Comunidades'}
              {lista && lista.length > 0 ? ` · ${total(t)}` : ''}
            </button>
          ))}
        </div>
        <div className="relative">
          <span
            aria-hidden
            className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-text-secondary"
          >
            search
          </span>
          <input
            aria-label={`Buscar ${plural}`}
            placeholder={
              tipo === 'grupo' ? 'Buscar por nombre, lugar o ciudad' : 'Buscar comunidades'
            }
            className={campo}
            value={consulta}
            onChange={(e) => setConsulta(e.target.value)}
          />
          {consulta && (
            <button
              aria-label="Limpiar búsqueda"
              data-testid="limpiar-busqueda"
              onClick={() => setConsulta('')}
              className="absolute right-1 top-1/2 -translate-y-1/2 w-11 h-11 flex items-center justify-center text-text-secondary"
            >
              <span className="material-symbols-outlined">close</span>
            </button>
          )}
        </div>
        {tipo === 'grupo' && (
          <div
            className="flex gap-2 overflow-x-auto pb-1 -mx-margin px-margin lg:mx-0 lg:px-0"
            aria-label="Filtrar por nivel"
            data-testid="filtros-nivel"
          >
            {(Object.keys(ETIQUETA_NIVEL) as NivelGrupo[]).map((n) => (
              <button
                key={n}
                aria-pressed={nivel === n}
                onClick={() => setNivel(n)}
                className={chip(nivel === n)}
              >
                {n === 'todos' ? 'Todos' : ETIQUETA_NIVEL[n]}
              </button>
            ))}
          </div>
        )}
        {lista === null ? (
          <Esqueleto />
        ) : !clubes.disponible() ? (
          <Vacio
            icono="cloud_off"
            titulo="Sin conexión"
            texto="Los grupos y comunidades necesitan conexión con el servidor."
          />
        ) : error ? (
          <div
            data-testid="error-clubes"
            className="tarjeta-c2 p-5 flex flex-col items-center text-center gap-3"
          >
            <span aria-hidden className="material-symbols-outlined text-[40px] text-c2-violeta">
              cloud_off
            </span>
            <p className="text-text-primary">
              No pudimos cargar los grupos y comunidades. Revisa tu conexión.
            </p>
            <button data-testid="reintentar-clubes" onClick={cargar} className="boton-c2">
              Reintentar
            </button>
          </div>
        ) : visibles.length === 0 ? (
          filtrando ? (
            <Vacio
              icono="search_off"
              titulo="Sin resultados"
              texto={
                tipo === 'grupo'
                  ? 'No encontramos grupos con esa búsqueda o nivel.'
                  : 'No encontramos comunidades con esa búsqueda.'
              }
              accion="Limpiar filtros"
              principal={false}
              onAccion={() => {
                setConsulta('')
                setNivel('todos')
              }}
            />
          ) : (
            <Vacio
              icono="groups"
              titulo={`Aún no hay ${plural}`}
              texto={
                tipo === 'grupo'
                  ? 'Crea el primero: elige punto de encuentro, días, hora y nivel para que otros corredores se sumen.'
                  : 'Crea la primera para reunir a los grupos de tu ciudad en un solo lugar.'
              }
              accion={sesion ? `Crear ${singular}` : undefined}
              onAccion={crear}
            />
          )
        ) : (
          <>
            {propios.length > 0 && (
              <section aria-labelledby="mis-clubes" className="space-y-3">
                <h2
                  id="mis-clubes"
                  className="font-headline-sm text-[20px] italic text-text-primary"
                >
                  {tipo === 'grupo' ? 'Mis grupos' : 'Mis comunidades'}
                </h2>
                <ul className="space-y-3">{propios.map(tarjeta)}</ul>
              </section>
            )}
            <section aria-labelledby="descubrir" className="space-y-3">
              <h2 id="descubrir" className="font-headline-sm text-[20px] italic text-text-primary">
                Descubrir
              </h2>
              {otros.length === 0 ? (
                <p className="text-text-secondary text-sm">
                  {tipo === 'grupo'
                    ? 'Ya eres parte de todos los grupos. ¿Creas uno nuevo?'
                    : 'Ya eres parte de todas las comunidades.'}
                </p>
              ) : (
                <ul className="space-y-3" data-testid="lista-clubes">
                  {otros.map(tarjeta)}
                </ul>
              )}
            </section>
          </>
        )}
      </div>
      <Aviso mensaje={aviso} />
    </main>
  )
}
