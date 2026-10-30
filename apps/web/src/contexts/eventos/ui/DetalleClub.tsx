import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useAviso } from '../../../hooks'
import { LogoClub } from './Clubes'
import type { Evento } from '../../../mocks/data'
import { Aviso, BotonTexto, Dialogo } from '../../../shared/ui/ui'
import { ModalReporte } from '../../comunidad'
import { useIdentidad } from '../../identidad/api'
import { yaReportado } from '../../moderacion/api'
import {
  accionClub,
  clubes,
  ETIQUETA_NIVEL,
  ETIQUETA_ROL,
  ETIQUETA_TIPO,
  estaLleno,
  eventoDesdeFila,
  puedeGestionar,
  textoHorario,
  textoRitmo,
  urlPublica,
  type Club,
  type MiembroClub,
  type PostClub,
} from '../api'

export function DetalleClub() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const sesion = useIdentidad((s) => s.sesion)
  const [club, setClub] = useState<Club | null | undefined>(undefined)
  const [miembros, setMiembros] = useState<MiembroClub[]>([])
  const [posts, setPosts] = useState<PostClub[]>([])
  const [eventos, setEventos] = useState<Evento[]>([])
  const [yoId, setYoId] = useState<string | null>(null)
  const [texto, setTexto] = useState('')
  const [aviso, mostrarAviso] = useAviso()
  const [comunidad, setComunidad] = useState<Club | null>(null)
  const [gruposComunidad, setGruposComunidad] = useState<Club[]>([])
  const [reportando, setReportando] = useState(false)
  const [borrando, setBorrando] = useState(false)

  const cargar = useCallback(async () => {
    const c = await clubes.detalle(id).catch(() => null)
    setClub(c)
    if (!c) return
    const [m, p, e, yo] = await Promise.all([
      clubes.miembros(id),
      clubes.posts(id).catch(() => []),
      clubes.eventos(id).catch(() => []),
      clubes.uid(),
    ])
    setMiembros(m)
    setPosts(p)
    setEventos(e.map((f) => eventoDesdeFila(f, yo ?? undefined)))
    setYoId(yo)
    setComunidad(c.comunidadId ? await clubes.detalle(c.comunidadId).catch(() => null) : null)
    setGruposComunidad(c.tipo === 'comunidad' ? await clubes.grupos(c.id).catch(() => []) : [])
  }, [id])
  useEffect(() => {
    void cargar()
  }, [cargar, sesion])
  useEffect(() => {
    if (!id || !sesion) return
    return clubes.escucharPosts(id, () => void cargar())
  }, [id, sesion, cargar])

  if (club === undefined) return <main className="p-6 text-text-secondary">Cargando…</main>
  if (club === null)
    return (
      <main className="p-6 text-text-secondary">
        No encontramos este grupo o comunidad.{' '}
        <Link to="/comunidad/clubes" className="text-primary-400 underline">
          Ver grupos
        </Link>
      </main>
    )

  const yo = miembros.find((m) => m.usuarioId === yoId) ?? null
  const activo = yo?.estado === 'active'
  const gestiono = activo && puedeGestionar(yo?.rol)
  const lleno = estaLleno(club)
  const accion = sesion ? accionClub(yo, club.conAprobacion, club.tipo, lleno) : null
  const esGrupo = club.tipo === 'grupo'
  const portada = urlPublica(club.portada)
  const horario = textoHorario(club.dias, club.hora)
  const intentar = async (fn: () => Promise<unknown>, ok: string) => {
    try {
      await fn()
      mostrarAviso(ok)
      await cargar()
    } catch (e) {
      mostrarAviso(e instanceof Error ? e.message : 'Algo salió mal')
    }
  }
  const activos = miembros.filter((m) => m.estado === 'active')
  const pendientes = miembros.filter((m) => m.estado === 'pending')

  return (
    <main className="w-full min-h-screen bg-surface contenedor-escritorio lg:max-w-5xl pb-28">
      <header className="flex items-center gap-1 px-margin h-16 bg-bg-secondary mb-4">
        <button
          onClick={() => navigate('/comunidad/clubes')}
          aria-label="Volver"
          className="w-12 h-12 -ml-3 shrink-0 flex items-center justify-center text-text-primary"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </button>
        <h1 className="flex-1 min-w-0 font-title-md text-title-md text-text-primary truncate">
          {club.nombre}
        </h1>
        {gestiono && (
          <button
            onClick={() => navigate(`/comunidad/clubes/${id}/editar`)}
            aria-label="Editar"
            data-testid="editar-club"
            className="w-11 h-11 rounded-full bg-surface-container flex items-center justify-center text-text-primary"
          >
            <span className="material-symbols-outlined">edit</span>
          </button>
        )}
      </header>
      {portada && (
        <div className="px-margin lg:px-0 mb-4">
          <img src={portada} alt="" className="w-full max-h-[200px] object-cover rounded-2xl" />
        </div>
      )}
      <div className="px-margin lg:px-0 lg:grid lg:grid-cols-[minmax(0,1fr)_340px] lg:gap-8 space-y-6 lg:space-y-0">
        <div className="space-y-6">
          <section className="tarjeta-c2 p-5 space-y-3">
            <div className="flex items-center gap-3">
              <LogoClub club={club} grande />
              <div className="min-w-0">
                <span className="text-[11px] uppercase tracking-wide text-primary-400">
                  {ETIQUETA_TIPO[club.tipo]}
                  {club.oculto && ' · Oculto por moderación'}
                </span>
                {comunidad && (
                  <Link
                    to={`/comunidad/clubes/${comunidad.id}`}
                    className="block text-sm text-secondary-400 truncate"
                  >
                    Parte de {comunidad.nombre}
                  </Link>
                )}
              </div>
            </div>
            <p className="text-text-secondary">{club.descripcion || 'Sin descripción.'}</p>
            <p className="text-sm text-text-secondary" data-testid="miembros-club">
              {club.miembros}
              {club.cupo != null ? `/${club.cupo}` : ''}{' '}
              {club.miembros === 1 ? 'miembro' : 'miembros'}
              {club.ciudad && ` · ${club.ciudad}`} ·{' '}
              {club.conAprobacion ? 'Privado, con solicitud' : 'Público'}
              {lleno && ' · Cupo lleno'}
            </p>
            {esGrupo && (
              <dl className="grid grid-cols-2 gap-2" data-testid="datos-grupo">
                {(
                  [
                    ['Encuentro', club.lugarEncuentro || 'Por definir'],
                    ['Horario', horario || 'Por definir'],
                    ['Nivel', ETIQUETA_NIVEL[club.nivel]],
                    ['Ritmo', textoRitmo(club.ritmoSegKm) || 'Libre'],
                  ] as const
                ).map(([t, v]) => (
                  <div key={t} className="rounded-xl bg-surface-container-high p-2">
                    <dt className="text-[11px] text-text-secondary">{t}</dt>
                    <dd className="text-sm text-text-primary">{v}</dd>
                  </div>
                ))}
              </dl>
            )}
            {esGrupo && club.punto && (
              <a
                href={`https://www.google.com/maps/search/?api=1&query=${club.punto.lat},${club.punto.lng}`}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-1 text-sm text-primary-400"
              >
                <span className="material-symbols-outlined text-[18px]">location_on</span>
                Ver punto de encuentro en el mapa
              </a>
            )}
            {club.contacto && (
              <p className="text-sm text-text-secondary">Contacto: {club.contacto}</p>
            )}
            {accion && (
              <button
                data-testid="accion-club"
                disabled={yo?.estado === 'pending'}
                onClick={() =>
                  void (yo
                    ? intentar(() => clubes.salir(id), 'Saliste')
                    : intentar(
                        () => clubes.unirse(id),
                        club.conAprobacion ? 'Solicitud enviada' : 'Te uniste'
                      ))
                }
                className={`h-12 px-5 rounded-xl font-semibold disabled:opacity-60 ${yo ? 'bg-surface-container-high text-text-primary border border-border-subtle' : 'bg-primary-600 text-on-primary'}`}
              >
                {accion}
              </button>
            )}
            {!sesion && <p className="text-sm text-text-secondary">Inicia sesión para unirte.</p>}
            {sesion && !yo && lleno && (
              <p className="text-sm text-text-secondary">El grupo está lleno.</p>
            )}
          </section>

          <section aria-labelledby="titulo-feed" className="space-y-3">
            <h2 id="titulo-feed" className="font-title-md text-title-md text-text-primary">
              Publicaciones
            </h2>
            {activo && (
              <form
                className="flex gap-2"
                onSubmit={(e) => {
                  e.preventDefault()
                  if (!texto.trim()) return
                  void intentar(() => clubes.publicar(id, texto), 'Publicado').then(() =>
                    setTexto('')
                  )
                }}
              >
                <input
                  aria-label="Escribe al grupo"
                  placeholder="Escribe algo a los miembros…"
                  value={texto}
                  maxLength={1000}
                  onChange={(e) => setTexto(e.target.value)}
                  className="flex-1 h-12 px-4 bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary"
                />
                <button
                  type="submit"
                  className="h-12 px-4 rounded-xl bg-primary-600 text-on-primary font-semibold"
                  data-testid="publicar-club"
                >
                  Publicar
                </button>
              </form>
            )}
            {posts.length === 0 ? (
              <p className="text-sm text-text-secondary">
                {activo || !club.conAprobacion
                  ? 'Aún no hay publicaciones.'
                  : 'Únete para ver las publicaciones.'}
              </p>
            ) : (
              <ul className="space-y-2" data-testid="posts-club">
                {posts.map((p) => (
                  <li key={p.id} className="bg-surface-container rounded-2xl p-4">
                    <div className="flex items-center justify-between gap-2">
                      <span className="font-semibold text-text-primary">{p.nombre}</span>
                      <span className="text-xs text-text-secondary">
                        {new Date(p.creadoMs).toLocaleDateString('es-EC', {
                          day: 'numeric',
                          month: 'short',
                        })}
                      </span>
                    </div>
                    <p className="text-text-secondary mt-1 whitespace-pre-wrap break-words">
                      {p.texto}
                    </p>
                    {(p.usuarioId === yoId || gestiono) && (
                      <button
                        onClick={() =>
                          void intentar(() => clubes.borrarPost(p.id), 'Publicación borrada')
                        }
                        className="text-xs text-red-300 mt-2"
                      >
                        Borrar
                      </button>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>

        <aside className="space-y-6">
          {club.tipo === 'comunidad' && (
            <section aria-labelledby="titulo-grupos" className="space-y-2">
              <div className="flex items-center justify-between">
                <h2 id="titulo-grupos" className="font-title-md text-title-md text-text-primary">
                  Grupos ({gruposComunidad.length})
                </h2>
                {sesion && (
                  <Link
                    to={`/comunidad/clubes/nuevo?tipo=grupo&comunidad=${id}`}
                    className="text-sm text-primary-400"
                  >
                    + Registrar grupo
                  </Link>
                )}
              </div>
              {gruposComunidad.length === 0 ? (
                <p className="text-sm text-text-secondary">Aún no hay grupos en esta comunidad.</p>
              ) : (
                <ul className="space-y-2">
                  {gruposComunidad.map((g) => (
                    <li key={g.id}>
                      <Link
                        to={`/comunidad/clubes/${g.id}`}
                        className="flex items-center gap-2 bg-surface-container rounded-xl p-3"
                      >
                        <LogoClub club={g} />
                        <span className="flex-1 min-w-0">
                          <span className="block font-semibold text-text-primary truncate">
                            {g.nombre}
                          </span>
                          <span className="block text-xs text-text-secondary truncate">
                            {g.miembros} miembros ·{' '}
                            {textoHorario(g.dias, g.hora) || g.lugarEncuentro}
                          </span>
                        </span>
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          )}
          <section aria-labelledby="titulo-eventos-club" className="space-y-2">
            <div className="flex items-center justify-between">
              <h2
                id="titulo-eventos-club"
                className="font-title-md text-title-md text-text-primary"
              >
                Eventos
              </h2>
              {gestiono && (
                <Link
                  to={`/comunidad/eventos?club=${id}`}
                  className="text-sm text-primary-400"
                  data-testid="crear-evento-club"
                >
                  + Crear
                </Link>
              )}
            </div>
            {eventos.length === 0 ? (
              <p className="text-sm text-text-secondary">Sin eventos programados.</p>
            ) : (
              <ul className="space-y-2">
                {eventos.map((e) => (
                  <li key={e.id}>
                    <Link
                      to={`/comunidad/eventos/${e.id}`}
                      className="block bg-surface-container rounded-xl p-3"
                    >
                      <p className="font-semibold text-text-primary">{e.titulo}</p>
                      <p className="text-sm text-text-secondary capitalize">
                        {e.fecha} · {e.hora}
                      </p>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </section>
          {gestiono && pendientes.length > 0 && (
            <section aria-labelledby="titulo-solicitudes" className="space-y-2">
              <h2 id="titulo-solicitudes" className="font-title-md text-title-md text-text-primary">
                Solicitudes ({pendientes.length})
              </h2>
              <ul className="space-y-2" data-testid="solicitudes">
                {pendientes.map((m) => (
                  <li
                    key={m.usuarioId}
                    className="flex items-center gap-2 bg-surface-container rounded-xl p-3"
                  >
                    <span className="flex-1 text-text-primary truncate">{m.nombre}</span>
                    <button
                      onClick={() =>
                        void intentar(() => clubes.aprobar(id, m.usuarioId), 'Solicitud aprobada')
                      }
                      className="h-9 px-3 rounded-full bg-secondary-600 text-on-primary text-sm font-semibold"
                    >
                      Aprobar
                    </button>
                    <button
                      onClick={() =>
                        void intentar(() => clubes.salir(id, m.usuarioId), 'Solicitud rechazada')
                      }
                      className="h-9 px-3 rounded-full bg-surface-container-high text-text-primary text-sm"
                    >
                      Rechazar
                    </button>
                  </li>
                ))}
              </ul>
            </section>
          )}
          <section aria-labelledby="titulo-miembros" className="space-y-2">
            <h2 id="titulo-miembros" className="font-title-md text-title-md text-text-primary">
              Miembros
            </h2>
            <ul className="space-y-1" data-testid="lista-miembros">
              {activos.map((m) => (
                <li key={m.usuarioId} className="flex items-center gap-2 py-1">
                  <span className="flex-1 text-text-primary truncate">{m.nombre}</span>
                  <span className="text-xs px-2 py-1 rounded-full bg-surface-container text-text-secondary">
                    {ETIQUETA_ROL[m.rol]}
                  </span>
                  {yo?.rol === 'owner' && m.rol !== 'owner' && (
                    <button
                      onClick={() =>
                        void intentar(
                          () =>
                            clubes.cambiarRol(
                              id,
                              m.usuarioId,
                              m.rol === 'admin' ? 'member' : 'admin'
                            ),
                          'Rol actualizado'
                        )
                      }
                      className="text-xs text-primary-400"
                    >
                      {m.rol === 'admin' ? 'Quitar admin' : 'Hacer admin'}
                    </button>
                  )}
                  {gestiono && m.rol === 'member' && m.usuarioId !== yoId && (
                    <button
                      onClick={() =>
                        void intentar(() => clubes.salir(id, m.usuarioId), 'Miembro expulsado')
                      }
                      className="text-xs text-red-300"
                    >
                      Expulsar
                    </button>
                  )}
                </li>
              ))}
            </ul>
          </section>
        </aside>
      </div>
      <div className="px-margin lg:px-0 py-6 flex flex-wrap items-center gap-4">
        {sesion && club.ownerId !== yoId && (
          <button
            type="button"
            data-testid="reportar-club"
            onClick={() =>
              yaReportado(club.tipo, club.id)
                ? mostrarAviso('Ya lo reportaste')
                : setReportando(true)
            }
            className="inline-flex items-center gap-1 text-[13px] text-c2-muted hover:text-c2-peligro"
          >
            <span className="material-symbols-outlined text-[16px]">flag</span>
            {esGrupo ? 'Reportar grupo' : 'Reportar comunidad'}
          </button>
        )}
        {yo?.rol === 'owner' && (
          <button
            type="button"
            data-testid="eliminar-club"
            onClick={() => setBorrando(true)}
            className="inline-flex items-center gap-1 text-[13px] text-red-300"
          >
            <span className="material-symbols-outlined text-[16px]">delete</span>
            {esGrupo ? 'Eliminar grupo' : 'Eliminar comunidad'}
          </button>
        )}
      </div>
      <ModalReporte
        tipo={club.tipo}
        postId={reportando ? club.id : null}
        onCerrar={() => setReportando(false)}
        onEnviado={(ok) => {
          setReportando(false)
          mostrarAviso(ok ? 'Reporte enviado' : 'No se pudo enviar el reporte. Revisa tu conexión')
        }}
      />
      <Dialogo
        titulo={esGrupo ? 'Eliminar grupo' : 'Eliminar comunidad'}
        abierto={borrando}
        peligro
        onCerrar={() => setBorrando(false)}
        acciones={
          <>
            <BotonTexto onClick={() => setBorrando(false)}>Cancelar</BotonTexto>
            <BotonTexto
              peligro
              onClick={() =>
                void clubes
                  .eliminar(id)
                  .then(() => navigate(`/comunidad/clubes${esGrupo ? '' : '?tipo=comunidad'}`))
                  .catch(() => mostrarAviso('No se pudo eliminar'))
              }
            >
              Eliminar
            </BotonTexto>
          </>
        }
      >
        <p className="text-text-secondary text-sm">
          Se borran sus miembros, publicaciones y eventos. No se puede deshacer.
          {!esGrupo && ' Los grupos de la comunidad siguen existiendo sin comunidad.'}
        </p>
      </Dialogo>
      <Aviso mensaje={aviso} />
    </main>
  )
}
