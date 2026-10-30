import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useIdentidad } from '../../identidad/api'
import {
  clubes,
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
  'w-full h-11 px-3 bg-surface-container border border-border-subtle rounded-lg text-text-primary placeholder-text-secondary'

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
  const [mios, setMios] = useState<Set<string>>(new Set())
  const [consulta, setConsulta] = useState('')
  const [nivel, setNivel] = useState<NivelGrupo>('todos')

  useEffect(() => {
    if (!clubes.disponible()) return setLista([])
    void clubes
      .listar()
      .then((r) => {
        setLista(r.todos)
        setMios(r.mios)
      })
      .catch(() => setLista([]))
  }, [sesion])

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

  const tarjeta = (c: Club) => {
    const comunidad = c.comunidadId ? comunidades.get(c.comunidadId) : undefined
    const horario = textoHorario(c.dias, c.hora)
    return (
      <li key={c.id}>
        <Link
          to={`/comunidad/clubes/${c.id}`}
          data-testid="tarjeta-club"
          className="flex items-center gap-3 tarjeta-c2 p-3 hover:border-primary-600/50 transition-colors"
        >
          <LogoClub club={c} />
          <div className="min-w-0 flex-1">
            <p className="font-semibold text-text-primary truncate">{c.nombre}</p>
            <p className="text-xs text-text-secondary truncate">
              {c.miembros}
              {c.cupo != null ? `/${c.cupo}` : ''} {c.miembros === 1 ? 'miembro' : 'miembros'}
              {c.tipo === 'grupo'
                ? c.lugarEncuentro && ` · ${c.lugarEncuentro}`
                : c.ciudad && ` · ${c.ciudad}`}
            </p>
            {c.tipo === 'grupo' && (horario || c.ritmoSegKm) && (
              <p className="text-xs text-primary-400 truncate">
                {[horario, textoRitmo(c.ritmoSegKm)].filter(Boolean).join(' · ')}
              </p>
            )}
            <div className="flex flex-wrap gap-1 mt-1">
              {c.tipo === 'grupo' && (
                <span className="px-2 py-0.5 rounded-md bg-primary-600/15 text-primary text-[11px]">
                  {ETIQUETA_NIVEL[c.nivel]}
                </span>
              )}
              {c.conAprobacion && (
                <span className="px-2 py-0.5 rounded-md bg-surface-container-high text-text-secondary text-[11px]">
                  Privado
                </span>
              )}
              {comunidad && (
                <span className="px-2 py-0.5 rounded-md bg-secondary-600/15 text-secondary-400 text-[11px] truncate max-w-[160px]">
                  {comunidad.nombre}
                </span>
              )}
            </div>
          </div>
          <span className="material-symbols-outlined text-text-secondary">chevron_right</span>
        </Link>
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
        <h1 className="flex-1 min-w-0 font-title-md text-title-md text-text-primary">
          Grupos y comunidades
        </h1>
        {sesion && (
          <button
            onClick={() => navigate(`/comunidad/clubes/nuevo?tipo=${tipo}`)}
            aria-label={tipo === 'comunidad' ? 'Registrar comunidad' : 'Registrar grupo'}
            data-testid="crear-club"
            className="w-11 h-11 rounded-full bg-primary-600 text-on-primary flex items-center justify-center"
          >
            <span className="material-symbols-outlined">add</span>
          </button>
        )}
      </header>
      <div className="px-margin lg:px-0 space-y-4">
        <div role="tablist" className="grid grid-cols-2 gap-1 p-1 bg-surface-container rounded-xl">
          {(['grupo', 'comunidad'] as const).map((t) => (
            <button
              key={t}
              role="tab"
              aria-selected={tipo === t}
              onClick={() => setParams(t === 'grupo' ? {} : { tipo: t }, { replace: true })}
              className={`h-10 rounded-lg text-sm font-semibold ${tipo === t ? 'bg-primary-600 text-on-primary' : 'text-text-secondary'}`}
            >
              {t === 'grupo' ? 'Grupos de running' : 'Comunidades'}
            </button>
          ))}
        </div>
        <input
          aria-label={`Buscar ${plural}`}
          placeholder={
            tipo === 'grupo' ? 'Buscar por nombre, lugar o ciudad' : 'Buscar comunidades'
          }
          className={campo}
          value={consulta}
          onChange={(e) => setConsulta(e.target.value)}
        />
        {tipo === 'grupo' && (
          <div className="flex gap-2 overflow-x-auto pb-1" aria-label="Filtrar por nivel">
            {(Object.keys(ETIQUETA_NIVEL) as NivelGrupo[]).map((n) => (
              <button
                key={n}
                aria-pressed={nivel === n}
                onClick={() => setNivel(n)}
                className={`h-8 px-3 rounded-full text-xs whitespace-nowrap border ${nivel === n ? 'border-primary-600 bg-primary-600/20 text-text-primary' : 'border-border-subtle text-text-secondary'}`}
              >
                {n === 'todos' ? 'Todos' : ETIQUETA_NIVEL[n]}
              </button>
            ))}
          </div>
        )}
        {lista === null ? (
          <p className="text-text-secondary">Cargando…</p>
        ) : !clubes.disponible() ? (
          <p className="text-text-secondary">Los grupos necesitan conexión con el servidor.</p>
        ) : (
          <>
            {propios.length > 0 && (
              <section aria-labelledby="mis-clubes" className="space-y-2">
                <h2 id="mis-clubes" className="font-title-md text-title-md text-text-primary">
                  {tipo === 'grupo' ? 'Mis grupos' : 'Mis comunidades'}
                </h2>
                <ul className="space-y-2">{propios.map(tarjeta)}</ul>
              </section>
            )}
            <section aria-labelledby="descubrir" className="space-y-2">
              <h2 id="descubrir" className="font-title-md text-title-md text-text-primary">
                Descubrir
              </h2>
              {otros.length === 0 ? (
                <p className="text-text-secondary text-sm">
                  {consulta || nivel !== 'todos'
                    ? `No hay ${plural} con ese filtro.`
                    : `Todavía no hay ${plural}. Registra el primero con el botón +.`}
                </p>
              ) : (
                <ul className="space-y-2" data-testid="lista-clubes">
                  {otros.map(tarjeta)}
                </ul>
              )}
            </section>
          </>
        )}
      </div>
    </main>
  )
}
