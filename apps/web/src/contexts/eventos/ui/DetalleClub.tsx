import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useAviso } from '../../../hooks'
import type { Evento } from '../../../mocks/data'
import { Aviso } from '../../../shared/ui/ui'
import { useApp } from '../../../store/app'
import {
  accionClub,
  clubes,
  ETIQUETA_ROL,
  eventoDesdeFila,
  puedeGestionar,
  type Club,
  type MiembroClub,
  type PostClub,
} from '../api'

export function DetalleClub() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const sesion = useApp((s) => s.sesion)
  const [club, setClub] = useState<Club | null | undefined>(undefined)
  const [miembros, setMiembros] = useState<MiembroClub[]>([])
  const [posts, setPosts] = useState<PostClub[]>([])
  const [eventos, setEventos] = useState<Evento[]>([])
  const [yoId, setYoId] = useState<string | null>(null)
  const [texto, setTexto] = useState('')
  const [aviso, mostrarAviso] = useAviso()

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
        Club no encontrado.{' '}
        <Link to="/comunidad/clubes" className="text-primary-400 underline">
          Ver clubes
        </Link>
      </main>
    )

  const yo = miembros.find((m) => m.usuarioId === yoId) ?? null
  const activo = yo?.estado === 'active'
  const gestiono = activo && puedeGestionar(yo?.rol)
  const accion = sesion ? accionClub(yo, club.conAprobacion) : null
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
      <header className="flex items-center gap-3 px-margin py-4 lg:px-0">
        <button
          onClick={() => navigate('/comunidad/clubes')}
          aria-label="Volver"
          className="w-11 h-11 rounded-full bg-surface-container flex items-center justify-center text-text-primary"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </button>
        <h1 className="flex-1 font-headline-md text-headline-md text-text-primary truncate">
          {club.nombre}
        </h1>
      </header>
      <div className="px-margin lg:px-0 lg:grid lg:grid-cols-[minmax(0,1fr)_340px] lg:gap-8 space-y-6 lg:space-y-0">
        <div className="space-y-6">
          <section className="bg-surface-container rounded-2xl p-5 space-y-3">
            <p className="text-text-secondary">{club.descripcion || 'Sin descripción.'}</p>
            <p className="text-sm text-text-secondary" data-testid="miembros-club">
              {club.miembros} {club.miembros === 1 ? 'miembro' : 'miembros'}
              {club.ubicacion && ` · ${club.ubicacion}`} ·{' '}
              {club.conAprobacion ? 'Con aprobación' : 'Abierto'}
            </p>
            {accion && (
              <button
                data-testid="accion-club"
                disabled={yo?.estado === 'pending'}
                onClick={() =>
                  void (yo
                    ? intentar(() => clubes.salir(id), 'Saliste del club')
                    : intentar(
                        () => clubes.unirse(id),
                        club.conAprobacion ? 'Solicitud enviada' : 'Te uniste al club'
                      ))
                }
                className={`h-12 px-5 rounded-xl font-semibold disabled:opacity-60 ${yo ? 'bg-surface-container-high text-text-primary border border-border-subtle' : 'bg-primary-600 text-on-primary'}`}
              >
                {accion}
              </button>
            )}
            {!sesion && <p className="text-sm text-text-secondary">Inicia sesión para unirte.</p>}
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
                  aria-label="Escribe al club"
                  placeholder="Escribe algo al club…"
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
          <section aria-labelledby="titulo-eventos-club" className="space-y-2">
            <div className="flex items-center justify-between">
              <h2
                id="titulo-eventos-club"
                className="font-title-md text-title-md text-text-primary"
              >
                Eventos del club
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
      <Aviso mensaje={aviso} />
    </main>
  )
}
