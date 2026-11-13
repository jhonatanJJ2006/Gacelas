import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAviso } from '../../../hooks'
import { useEsEscritorio } from '../../../shared/lib/useEsEscritorio'
import { Aviso } from '../../../shared/ui/ui'
import { Barras, Panel, Rotulo } from '../../../shared/ui/Visual'
import { useIdentidad } from '../../identidad/api'
import {
  clubes,
  estaLleno,
  ETIQUETA_NIVEL,
  filtrarClubes,
  proximaSalida,
  textoHorario,
  textoRitmo,
  textoSalida,
  type KmGrupoSemana,
  urlPublica,
  type Club,
  type NivelGrupo,
  type TipoClub,
} from '../api'

const campo =
  'w-full h-10 pl-10 pr-10 bg-c2-card border border-c2-linea rounded-md text-[13px] text-white placeholder-c2-muted focus:border-c2-morado outline-none'

/** Chip C2 (Android Chip de ClubesRegistro): esquinas cortadas arriba-izq y abajo-der. */
const chip = (activo: boolean) =>
  `h-8 px-3 rounded-md border text-[12.5px] font-semibold whitespace-nowrap shrink-0 ${activo ? 'border-c2-morado bg-c2-morado/20 text-white' : 'border-c2-linea text-c2-muted hover:text-white'}`

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
  const [km, setKm] = useState<Map<string, KmGrupoSemana[]>>(new Map())
  const escritorio = useEsEscritorio()
  useEffect(() => {
    void clubes.kmGrupos(7).then(setKm)
  }, [sesion])

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

  const semanas = ultimasSemanas(7)
  const kmDe = (id: string) => {
    const filas = km.get(id) ?? []
    return semanas.map((w) => filas.find((f) => f.semana === w)?.km ?? 0)
  }
  const tarjeta = (c: Club) => {
    const comunidad = c.comunidadId ? comunidades.get(c.comunidadId) : undefined
    const horario = textoHorario(c.dias, c.hora)
    const ritmo = c.tipo === 'grupo' ? textoRitmo(c.ritmoSegKm) : ''
    const mio = mios.has(c.id)
    const grupos =
      c.tipo === 'comunidad' ? (lista ?? []).filter((g) => g.comunidadId === c.id).length : 0
    const lugar = c.lugarEncuentro || c.ciudad
    const salida = c.tipo === 'grupo' ? proximaSalida(c.dias, c.hora) : null
    const kms = kmDe(c.id)
    const conKm = kms.some((x) => x > 0)
    return (
      <li
        key={c.id}
        className="panel-c2 bg-c2-card border border-c2-linea p-4 flex flex-col gap-3 min-w-0"
      >
        <Link
          to={`/comunidad/clubes/${c.id}`}
          data-testid="tarjeta-club"
          className="flex items-center gap-3 group"
        >
          <LogoClub club={c} />
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap gap-1.5 mb-0.5">
              <span className="text-[10.5px] font-semibold px-2 py-0.5 rounded-full bg-c2-morado/20 text-c2-violeta">
                {c.tipo === 'grupo' ? ETIQUETA_NIVEL[c.nivel] : 'Comunidad'}
              </span>
              {comunidad && (
                <span className="text-[10.5px] px-2 py-0.5 rounded-full bg-c2-s2 text-c2-muted truncate max-w-[160px]">
                  {comunidad.nombre}
                </span>
              )}
            </div>
            <p className="font-display italic text-[20px] leading-tight text-white truncate group-hover:text-c2-violeta">
              {c.nombre}
            </p>
          </div>
          {mio && (
            <span className="shrink-0 text-[11px] font-semibold px-2 py-1 rounded-full bg-c2-menta/15 text-c2-menta">
              Miembro
            </span>
          )}
          {c.conAprobacion && !mio && (
            <span className="material-symbols-outlined text-c2-muted" aria-label="Privado">
              lock
            </span>
          )}
        </Link>
        <dl className="grid grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)_minmax(0,0.8fr)] gap-3 text-[12.5px]">
          {c.tipo === 'grupo' ? (
            <>
              <DatoC2 t="Horario" v={horario || 'Sin horario'} />
              <DatoC2 t="Ritmo" v={ritmo || 'Libre'} />
            </>
          ) : (
            <>
              <DatoC2 t="Grupos" v={`${grupos}`} />
              <DatoC2 t="Ciudad" v={c.ciudad || 'Loja'} />
            </>
          )}
          <DatoC2 t="Miembros" v={`${c.miembros}${c.cupo != null ? ` de ${c.cupo}` : ''}`} />
        </dl>
        {lugar && (
          <p className="text-[12px] text-c2-muted flex items-center gap-1.5 min-w-0">
            <span
              aria-hidden="true"
              className="material-symbols-outlined text-[16px] text-c2-violeta"
            >
              place
            </span>
            <span className="truncate">{lugar}</span>
          </p>
        )}
        {c.tipo === 'grupo' && (
          <div className="flex items-end gap-4 pt-3 border-t border-c2-linea">
            <div className="flex-1 min-w-0">
              <Rotulo className="mb-1.5">Km del grupo, 7 semanas</Rotulo>
              {conKm ? (
                <Barras
                  valores={kms}
                  etiquetas={semanas.map(() => '')}
                  resaltar={kms.length - 1}
                  alto={34}
                  className="!gap-1"
                />
              ) : (
                <p className="text-[11.5px] text-c2-muted">Sin carreras registradas aún</p>
              )}
            </div>
            {salida && (
              <div className="text-right shrink-0">
                <Rotulo>Próxima salida</Rotulo>
                <p className="text-[13px] font-semibold text-c2-violeta mt-0.5">
                  {textoSalida(salida)}
                </p>
              </div>
            )}
          </div>
        )}
        {!mio && sesion && (
          <div className="flex justify-end mt-auto">
            {solicitados.has(c.id) ? (
              <span className="text-xs text-c2-muted">Solicitud enviada</span>
            ) : estaLleno(c) ? (
              <span className="text-xs text-c2-muted">Grupo lleno</span>
            ) : (
              <button
                data-testid="unirme-club"
                onClick={() => void unirme(c)}
                className="panel-c2 h-9 px-5 bg-[linear-gradient(90deg,#7B1FA2,#A855F7)] text-white text-[13px] font-bold"
              >
                {c.conAprobacion ? 'Solicitar' : 'Unirme'}
              </button>
            )}
          </div>
        )}
      </li>
    )
  }

  const misGrupos = (lista ?? []).filter((c) => c.tipo === 'grupo' && mios.has(c.id))
  const agenda = misGrupos
    .map((c) => ({ c, ms: proximaSalida(c.dias, c.hora) }))
    .filter((x): x is { c: Club; ms: number } => x.ms != null)
    .sort((a, b) => a.ms - b.ms)
  const ranking = (lista ?? [])
    .filter((c) => c.tipo === 'grupo')
    .map((c) => ({
      c,
      km: kmDe(c.id)
        .slice(-4)
        .reduce((s, x) => s + x, 0),
    }))
    .filter((x) => x.km > 0)
    .sort((a, b) => b.km - a.km)
    .slice(0, 5)

  return (
    <main className="w-full min-h-screen px-4 lg:px-8 lg:max-w-[1440px] lg:mx-auto pt-space-md lg:pt-8 pb-28 lg:pb-10">
      <header className="flex items-center gap-2 mb-5">
        <button
          onClick={() => navigate('/comunidad')}
          aria-label="Volver"
          className="lg:hidden w-10 h-10 -ml-2 shrink-0 flex items-center justify-center text-white"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </button>
        <div className="flex-1 min-w-0">
          <h1 className="titular-c2 text-[26px] lg:text-[34px] leading-none text-white truncate">
            Grupos y comunidades
          </h1>
          {lista && lista.length > 0 && (
            <p className="text-[12.5px] text-c2-muted mt-1">
              {total('comunidad')} {total('comunidad') === 1 ? 'comunidad' : 'comunidades'} y{' '}
              {total('grupo')} {total('grupo') === 1 ? 'grupo' : 'grupos'} de entrenamiento en Loja
            </p>
          )}
        </div>
        {sesion && (
          <button
            onClick={crear}
            aria-label={tipo === 'comunidad' ? 'Registrar comunidad' : 'Registrar grupo'}
            data-testid="crear-club"
            className="panel-c2 h-10 px-3 sm:px-4 shrink-0 bg-[linear-gradient(90deg,#7B1FA2,#A855F7)] text-white text-[13px] font-bold inline-flex items-center gap-1.5"
          >
            <span aria-hidden="true" className="material-symbols-outlined text-[18px]">
              add
            </span>
            <span className="hidden sm:inline">
              {tipo === 'comunidad' ? 'Registrar comunidad' : 'Crear grupo'}
            </span>
          </button>
        )}
      </header>
      <div className="flex flex-col lg:grid lg:grid-cols-[minmax(0,1fr)_320px] gap-5 items-start">
        <div className="space-y-4 min-w-0 w-full">
          <div className="flex flex-wrap items-center gap-3">
            <div
              role="tablist"
              className="flex gap-1 p-1 rounded-md bg-c2-card border border-c2-linea"
            >
              {(['grupo', 'comunidad'] as const).map((t) => (
                <button
                  key={t}
                  role="tab"
                  aria-selected={tipo === t}
                  data-testid={t === 'grupo' ? 'pestana-grupos' : 'pestana-comunidades'}
                  onClick={() => setParams(t === 'grupo' ? {} : { tipo: t }, { replace: true })}
                  className={`h-8 px-3 rounded text-[13px] font-semibold whitespace-nowrap ${tipo === t ? 'bg-c2-s2 text-white' : 'text-c2-muted hover:text-white'}`}
                >
                  {t === 'grupo' ? 'Grupos' : 'Comunidades'}
                  {lista && lista.length > 0 ? ` · ${total(t)}` : ''}
                </button>
              ))}
            </div>
            <div className="relative flex-1 min-w-[220px]">
              <span
                aria-hidden
                className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-c2-muted text-[20px]"
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
                  className="absolute right-1 top-1/2 -translate-y-1/2 w-10 h-10 flex items-center justify-center text-c2-muted"
                >
                  <span className="material-symbols-outlined">close</span>
                </button>
              )}
            </div>
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
                  <h2 id="mis-clubes" className="rotulo-c2">
                    {tipo === 'grupo' ? 'Mis grupos' : 'Mis comunidades'}
                  </h2>
                  <ul className="grid xl:grid-cols-2 gap-3">{propios.map(tarjeta)}</ul>
                </section>
              )}
              <section aria-labelledby="descubrir" className="space-y-3">
                <h2 id="descubrir" className="rotulo-c2">
                  Descubrir
                </h2>
                {otros.length === 0 ? (
                  <p className="text-text-secondary text-sm">
                    {tipo === 'grupo'
                      ? 'Ya eres parte de todos los grupos. ¿Creas uno nuevo?'
                      : 'Ya eres parte de todas las comunidades.'}
                  </p>
                ) : (
                  <ul className="grid xl:grid-cols-2 gap-3" data-testid="lista-clubes">
                    {otros.map(tarjeta)}
                  </ul>
                )}
              </section>
            </>
          )}
        </div>
        {escritorio && (
          <aside className="w-full space-y-5 lg:sticky lg:top-[88px]">
            <Panel titulo="Tu agenda de grupos" testId="agenda-grupos">
              {agenda.length === 0 ? (
                <p className="text-[13px] text-c2-muted">
                  {misGrupos.length
                    ? 'Tus grupos aún no tienen días de salida.'
                    : 'Únete a un grupo para ver aquí sus próximas salidas.'}
                </p>
              ) : (
                <ul className="divide-y divide-c2-linea -my-2">
                  {agenda.slice(0, 5).map(({ c, ms }) => (
                    <li key={c.id}>
                      <Link
                        to={`/comunidad/clubes/${c.id}`}
                        className="flex items-center gap-3 py-2.5 group"
                      >
                        <span className="w-11 shrink-0 text-center">
                          <span className="block text-[10px] font-bold uppercase text-c2-violeta">
                            {new Date(ms)
                              .toLocaleDateString('es-EC', { weekday: 'short' })
                              .replace('.', '')}
                          </span>
                          <span className="block cifra-c2 text-[20px] leading-none text-white">
                            {new Date(ms).getDate()}
                          </span>
                        </span>
                        <span className="min-w-0">
                          <span className="block text-[13px] font-semibold text-white truncate group-hover:text-c2-violeta">
                            {c.nombre}
                          </span>
                          <span className="block text-[11.5px] text-c2-muted truncate">
                            {textoSalida(ms)}
                            {c.lugarEncuentro ? ` · ${c.lugarEncuentro}` : ''}
                          </span>
                        </span>
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </Panel>
            <Panel
              titulo="Ranking de grupos"
              subtitulo="Km de sus miembros, últimas 4 semanas"
              testId="ranking-grupos"
            >
              {ranking.length === 0 ? (
                <p className="text-[13px] text-c2-muted">
                  Todavía no hay carreras de miembros en las últimas 4 semanas.
                </p>
              ) : (
                <ol className="space-y-2.5">
                  {ranking.map(({ c, km: k }, i) => (
                    <li key={c.id}>
                      <div className="flex items-center gap-2 text-[13px]">
                        <span className="cifra-c2 w-4 text-c2-muted">{i + 1}</span>
                        <span className="flex-1 truncate text-white">{c.nombre}</span>
                        <span className="cifra-c2 text-white">{k.toFixed(1)} km</span>
                      </div>
                      <div className="ml-6 mt-1 h-1 rounded-full bg-c2-s2 overflow-hidden">
                        <div
                          className="h-full bg-[linear-gradient(90deg,#7B1FA2,#A855F7)]"
                          style={{ width: `${(k / ranking[0]!.km) * 100}%` }}
                        />
                      </div>
                    </li>
                  ))}
                </ol>
              )}
            </Panel>
          </aside>
        )}
      </div>
      <Aviso mensaje={aviso} />
    </main>
  )
}

function DatoC2({ t, v }: { t: string; v: string }) {
  return (
    <div className="min-w-0">
      <dt className="text-[10px] font-bold uppercase tracking-[0.1em] text-c2-muted">{t}</dt>
      <dd className="text-white font-semibold truncate">{v}</dd>
    </div>
  )
}

/** Lunes (YYYY-MM-DD, hora de Loja) de las ultimas `n` semanas; la ultima es la actual. */
function ultimasSemanas(n: number, ahora = Date.now()) {
  const loja = new Date(ahora - 5 * 3_600_000)
  const dia = (loja.getUTCDay() + 6) % 7
  const lunes = Date.UTC(loja.getUTCFullYear(), loja.getUTCMonth(), loja.getUTCDate() - dia)
  return Array.from({ length: n }, (_, i) =>
    new Date(lunes - (n - 1 - i) * 7 * 86_400_000).toISOString().slice(0, 10)
  )
}
