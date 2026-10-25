import { listarEnVivo, suscribirLista, type SesionEnVivo } from '../../lib/enVivo'
import { useEsEscritorio } from '../../lib/useEsEscritorio'
import { ProximosEventos, RankingSemanal } from '../../components/PanelesEscritorio'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Aviso, BotonTexto, Dialogo } from '../../components/ui'
import { ModalC2 } from '../../components/modales'
import { compartir, useAviso } from '../../hooks'
import { formatearMs, formatearRitmo, Unidades } from '../../lib/geo'
import { useApp } from '../../store/app'
import { construirFeed, filtrarFeed, type FiltroFeed, type Publicacion } from '../../lib/logica'
import type { ComentarioPublicacion } from '../../data/repositorios'
import { Fragment } from 'react'
import { kmPorAutor, nivelCorredor } from '../../lib/logica'
import { filtrarEventos } from '../../lib/filtros'
import { ModalReporte } from '../social/Social'
import { yaReportada, yaReportado } from '../social/moderacion'
import { Carrusel, InsigniaNivel, TarjetaClub } from './PostSocial'

export function Feed() {
  const [enVivo, setEnVivo] = useState<SesionEnVivo[]>([])
  useEffect(() => {
    let vivo = true
    const cargar = () => void listarEnVivo().then((l) => vivo && setEnVivo(l))
    cargar()
    const salir = suscribirLista(cargar)
    return () => {
      vivo = false
      salir()
    }
  }, [])
  const escritorio = useEsEscritorio()
  const navigate = useNavigate()
  const sesion = useApp((s) => s.sesion)
  const carreras = useApp((s) => s.carreras)
  const remotas = useApp((s) => s.publicaciones)
  const siguiendo = useApp((s) => s.siguiendo)
  const alternarSeguir = useApp((s) => s.alternarSeguir)
  const [dejar, setDejar] = useState<{ id: string; nombre: string } | null>(null)
  const ajustes = useApp((s) => s.ajustes)
  const actualizar = useApp((s) => s.actualizarAjustes)
  const m = ajustes.unidadesMetricas
  const [filtro, setFiltro] = useState<FiltroFeed>('Todos')
  const [menuFiltro, setMenuFiltro] = useState(false)
  const [menuPost, setMenuPost] = useState<string | null>(null)
  const [comentando, setComentando] = useState<Publicacion | null>(null)
  const [texto, setTexto] = useState('')
  const [aviso, mostrarAviso] = useAviso()

  const feedSocial = useApp((s) => s.feedSocial)
  const alternarMeGustaPost = useApp((s) => s.alternarMeGustaPost)
  const comentarPost = useApp((s) => s.comentarPost)
  const comentariosPost = useApp((s) => s.comentariosPost)
  const suscribirFeed = useApp((s) => s.suscribirFeed)
  const refrescarFeed = useApp((s) => s.refrescarFeed)
  const [hilo, setHilo] = useState<ComentarioPublicacion[]>([])
  const alternarCompartidaPost = useApp((s) => s.alternarCompartidaPost)
  const subirFotosPost = useApp((s) => s.subirFotosPost)
  const creados = useApp((s) => s.eventosCreados)
  const inscritos = useApp((s) => s.inscritos)
  const alternarInscripcion = useApp((s) => s.alternarInscripcion)
  const [fotosPara, setFotosPara] = useState<string | null>(null)
  const [reportando, setReportando] = useState<string | null>(null)
  const [reportandoComentario, setReportandoComentario] = useState<string | null>(null)

  // Feed real: se refresca al entrar y con cada cambio en la base (realtime).
  useEffect(() => {
    if (!sesion) return
    void refrescarFeed()
    return suscribirFeed()
  }, [sesion, refrescarFeed, suscribirFeed])

  const posts = filtrarFeed(
    construirFeed(carreras, sesion?.nombre ?? 'Tú', Date.now(), remotas, feedSocial),
    filtro,
    ajustes.ocultas,
    siguiendo
  )

  const kms = kmPorAutor(posts)
  const nivelDe = (p: Publicacion) =>
    nivelCorredor(p.autorKm ?? kms.get(p.propia ? 'yo' : (p.autorId ?? p.autor)) ?? 0)
  const eventoClub = feedSocial ? filtrarEventos(creados, 'Próximos', inscritos)[0] : undefined
  const repostear = (p: Publicacion) => {
    if (!p.remota) return mostrarAviso('Publica la carrera para poder compartirla')
    alternarCompartidaPost(p.id).catch(() => mostrarAviso('No se pudo compartir'))
  }
  const subirFotos = async (id: string, archivos: FileList | null) => {
    setFotosPara(null)
    if (!archivos?.length) return
    try {
      await subirFotosPost(id, Array.from(archivos))
      mostrarAviso('Fotos publicadas')
    } catch {
      mostrarAviso('No se pudieron subir las fotos')
    }
  }

  const meGustaDe = (p: Publicacion) =>
    p.remota ? Boolean(p.meGusta) : ajustes.likes.includes(p.id)
  const likesDe = (p: Publicacion) =>
    p.remota ? p.likes : p.likes + (ajustes.likes.includes(p.id) ? 1 : 0)
  const comentariosLocales = (p: Publicacion) => (p.remota ? [] : (ajustes.comentarios[p.id] ?? []))

  const alternarLike = (p: Publicacion) => {
    if (p.remota) {
      alternarMeGustaPost(p.id).catch(() => mostrarAviso('No se pudo guardar el aplauso'))
      return
    }
    void actualizar((a) => ({
      likes: a.likes.includes(p.id) ? a.likes.filter((x) => x !== p.id) : [...a.likes, p.id],
    }))
  }

  const abrirComentarios = (p: Publicacion) => {
    setComentando(p)
    setHilo([])
    if (p.remota) void comentariosPost(p.id).then(setHilo)
  }

  const enviarComentario = async (p: Publicacion | null = comentando, cuerpo = texto) => {
    if (!p || !cuerpo.trim()) return
    if (p.remota) {
      try {
        await comentarPost(p.id, cuerpo.trim())
        if (comentando?.id === p.id) setHilo(await comentariosPost(p.id))
      } catch {
        return mostrarAviso('No se pudo publicar el comentario')
      }
    } else {
      const id = p.id
      void actualizar((a) => ({
        comentarios: { ...a.comentarios, [id]: [...(a.comentarios[id] ?? []), cuerpo.trim()] },
      }))
    }
    setTexto('')
    mostrarAviso('Comentario publicado')
  }

  const compartirPost = async (p: Publicacion) => {
    const r = await compartir(
      p.titulo,
      `${p.autor} corrió ${Unidades.texto(p.distanciaKm, m)} ${Unidades.etiquetaDistancia(m)} en ${formatearMs(p.segundos)} con Gacelas.`
    )
    if (r === 'copiado') mostrarAviso('Enlace copiado')
    else if (r === 'error') mostrarAviso('No se pudo compartir')
  }

  const pestanas: { id: FiltroFeed; etiqueta: string }[] = [
    { id: 'Todos', etiqueta: 'Para ti' },
    { id: 'Siguiendo', etiqueta: 'Siguiendo' },
  ]

  return (
    <div className="contenedor-escritorio lg:grid lg:grid-cols-[minmax(0,600px)_340px] lg:justify-center lg:gap-8 lg:items-start">
      <div className="flex flex-col w-full min-w-0">
        <div className="px-margin pt-space-md pb-space-sm lg:px-0 lg:pb-4">
          <div className="flex items-center justify-between mb-4 relative">
            <div>
              <h1 className="font-display italic uppercase text-[26px] text-text-primary">
                Comunidad
              </h1>
            </div>
            <div className="flex-1" />
            <button
              onClick={() => navigate('/comunidad/corredores')}
              aria-label="Buscar corredores"
              className="w-11 h-11 mr-2 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors"
            >
              <span className="material-symbols-outlined">person_search</span>
            </button>
            <button
              onClick={() => setMenuFiltro((v) => !v)}
              aria-label="Filtrar publicaciones"
              aria-expanded={menuFiltro}
              className="w-11 h-11 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors"
            >
              <span className="material-symbols-outlined">filter_list</span>
            </button>
            {menuFiltro && (
              <div
                role="menu"
                className="absolute right-0 top-12 z-30 bg-bg-secondary border border-border-subtle rounded-xl shadow-xl overflow-hidden"
              >
                {(['Todos', 'Siguiendo', 'Comunidad', 'Mías'] as FiltroFeed[]).map((f) => (
                  <button
                    key={f}
                    role="menuitemradio"
                    aria-checked={filtro === f}
                    onClick={() => {
                      setFiltro(f)
                      setMenuFiltro(false)
                    }}
                    className={`block w-full text-left px-4 py-2 text-sm ${filtro === f ? 'text-primary-500' : 'text-text-primary'} hover:bg-surface-container`}
                  >
                    {f}
                  </button>
                ))}
              </div>
            )}
          </div>

          <nav
            aria-label="Secciones de comunidad"
            className="flex p-1 mb-4 rounded-full bg-surface-container border border-border-subtle"
          >
            {pestanas.map((t) => (
              <button
                key={t.id}
                onClick={() => setFiltro(t.id)}
                aria-current={filtro === t.id ? 'page' : undefined}
                className={`flex-1 h-10 rounded-full font-label-md text-label-md transition-colors ${
                  filtro === t.id
                    ? 'bg-bg-primary text-primary shadow'
                    : 'text-text-secondary hover:text-text-primary'
                }`}
              >
                {t.etiqueta}
              </button>
            ))}
            <button
              onClick={() => navigate('/comunidad/clubes')}
              className="flex-1 h-10 rounded-full font-label-md text-label-md text-text-secondary hover:text-text-primary"
            >
              Clubes
            </button>
            <button
              onClick={() => navigate('/comunidad/eventos')}
              className="flex-1 h-10 rounded-full font-label-md text-label-md text-text-secondary hover:text-text-primary"
            >
              Eventos
            </button>
          </nav>

          {enVivo.length > 0 && (
            <Link
              to="/comunidad/en-vivo"
              data-testid="banner-en-vivo"
              className="flex items-center gap-3 min-h-[52px] mb-4 px-4 rounded-2xl bg-danger/15 border border-danger/40 text-text-primary"
            >
              <span className="w-2.5 h-2.5 rounded-full bg-danger animate-pulse" />
              <span className="flex-1 font-semibold">
                {enVivo.length === 1
                  ? `${enVivo[0].nombre} está corriendo en vivo`
                  : `${enVivo.length} corredores en vivo ahora`}
              </span>
              <span className="material-symbols-outlined">chevron_right</span>
            </Link>
          )}

          <button
            onClick={() => navigate('/correr')}
            className="w-full flex items-center gap-3 p-3 rounded-2xl bg-surface-container border border-border-subtle text-left hover:border-primary/50 transition-colors"
          >
            <span className="w-10 h-10 rounded-full bg-primary-600/30 flex items-center justify-center text-primary-400 font-bold">
              {(sesion?.nombre ?? 'T').charAt(0)}
            </span>
            <span className="flex-1 text-body-md text-text-secondary">
              Comparte tu entrenamiento: sal a correr y publícalo.
            </span>
            <span className="px-4 h-9 rounded-full bg-primary-600 text-on-primary font-label-md text-label-md flex items-center">
              Correr
            </span>
          </button>
          {filtro !== 'Todos' && filtro !== 'Siguiendo' && (
            <p className="mt-3 text-label-md text-text-secondary">Filtro: {filtro}</p>
          )}
        </div>

        <div className="px-margin space-y-2 pb-space-3xl lg:px-0 lg:space-y-3">
          {posts.length === 0 && (
            <p className="text-center text-text-secondary py-8">
              {filtro === 'Mías'
                ? 'Aún no tienes carreras públicas.'
                : filtro === 'Siguiendo'
                  ? 'Las carreras de quienes sigues aparecerán aquí. Busca corredores para seguir.'
                  : 'No hay publicaciones todavía.'}
            </p>
          )}
          {posts.map((post, indice) => {
            const meGusta = meGustaDe(post)
            const locales = comentariosLocales(post)
            return (
              <Fragment key={post.id}>
                {indice === 1 && eventoClub && (
                  <TarjetaClub
                    evento={eventoClub}
                    inscrito={inscritos.includes(eventoClub.id)}
                    onAlternar={() => void alternarInscripcion(eventoClub.id)}
                  />
                )}
                <article
                  className="border-b border-c2-linea pb-2 lg:pb-3"
                  data-testid="publicacion"
                >
                  <header className="px-4 pt-3 pb-2 flex items-start gap-3 relative">
                    {post.avatar ? (
                      <img
                        src={post.avatar}
                        alt=""
                        className="w-11 h-11 rounded-full object-cover border-2 border-primary/60"
                      />
                    ) : (
                      <div className="w-11 h-11 rounded-full bg-primary-600/30 border-2 border-primary/60 flex items-center justify-center text-primary-400 font-bold">
                        {post.autor.charAt(0)}
                      </div>
                    )}
                    <div className="flex-1 min-w-0">
                      <div className="flex flex-wrap items-center gap-x-2 gap-y-0.5">
                        <span className="font-body-md text-body-md text-text-primary font-semibold truncate max-w-full">
                          {post.autor}
                        </span>
                        <InsigniaNivel nivel={nivelDe(post)} />
                      </div>
                      <p className="font-body-sm text-body-sm text-text-secondary truncate">
                        {post.titulo} · {post.tiempo}
                      </p>
                    </div>
                    {post.autorId && (
                      <button
                        onClick={() =>
                          siguiendo.includes(post.autorId!)
                            ? setDejar({ id: post.autorId!, nombre: post.autor })
                            : void alternarSeguir(post.autorId!)
                        }
                        aria-pressed={siguiendo.includes(post.autorId)}
                        className={`h-9 px-4 rounded-full text-sm font-semibold transition-colors ${
                          siguiendo.includes(post.autorId)
                            ? 'bg-surface-container-high text-text-primary border border-border-subtle'
                            : 'bg-primary-600 text-on-primary hover:bg-primary-700'
                        }`}
                      >
                        {siguiendo.includes(post.autorId) ? 'Siguiendo' : 'Seguir'}
                      </button>
                    )}
                    <button
                      onClick={() => setMenuPost(menuPost === post.id ? null : post.id)}
                      aria-label="Más opciones"
                      className="w-11 h-11 -mr-2 flex items-center justify-center rounded-full text-text-secondary hover:text-text-primary"
                    >
                      <span className="material-symbols-outlined">more_vert</span>
                    </button>
                    {menuPost === post.id && (
                      <div
                        role="menu"
                        className="absolute right-4 top-14 z-30 bg-bg-secondary border border-border-subtle rounded-xl shadow-xl overflow-hidden"
                      >
                        {post.propia && post.remota && (
                          <label
                            role="menuitem"
                            className="block w-full text-left px-4 py-2 text-sm text-text-primary hover:bg-surface-container cursor-pointer"
                          >
                            Agregar fotos
                            <input
                              type="file"
                              accept="image/jpeg,image/png,image/webp"
                              multiple
                              className="sr-only"
                              aria-label="Agregar fotos"
                              onClick={() => setFotosPara(post.id)}
                              onChange={(e) => {
                                setMenuPost(null)
                                void subirFotos(fotosPara ?? post.id, e.target.files)
                              }}
                            />
                          </label>
                        )}
                        <button
                          role="menuitem"
                          onClick={() => {
                            void actualizar((a) => ({ ocultas: [...a.ocultas, post.id] }))
                            setMenuPost(null)
                            mostrarAviso('Publicación oculta')
                          }}
                          className="block w-full text-left px-4 py-2 text-sm text-text-primary hover:bg-surface-container"
                        >
                          Ocultar publicación
                        </button>
                        {!post.propia && (
                          <button
                            role="menuitem"
                            onClick={() => {
                              setMenuPost(null)
                              if (yaReportada(post.id))
                                mostrarAviso('Ya reportaste esta publicación')
                              else setReportando(post.id)
                            }}
                            className="block w-full text-left px-4 py-2 text-sm text-c2-peligro hover:bg-surface-container"
                          >
                            Reportar publicación
                          </button>
                        )}
                      </div>
                    )}
                  </header>

                  <div className="pl-[68px] pr-4 pt-2">
                    <div
                      className="flex flex-wrap items-baseline gap-x-4 gap-y-1 mb-1 font-display italic text-[15px] tabular-nums"
                      data-testid="metricas-post"
                    >
                      <span className="text-c2-violeta">
                        {Unidades.texto(post.distanciaKm, m, 1)}
                        <span className="font-sans not-italic text-[11px] text-c2-muted">
                          {Unidades.etiquetaDistancia(m)}
                        </span>
                      </span>
                      <span className="text-text-primary">
                        {formatearRitmo(Unidades.ritmo(post.ritmoSegKm, m))}
                      </span>
                      <span className="text-text-primary">{formatearMs(post.segundos)}</span>
                      <span className="text-c2-cielo" data-testid="desnivel-post">
                        +{post.desnivelM ?? 0}
                        <span className="font-sans not-italic text-[11px] text-c2-muted">m</span>
                      </span>
                    </div>

                    <Carrusel
                      imagenes={post.imagenes ?? []}
                      puntos={post.puntos}
                      desnivelM={post.desnivelM}
                    />
                    {locales.length > 0 && (
                      <ul className="mb-3 space-y-1">
                        {locales.map((c, i) => (
                          <li key={i} className="text-body-sm text-text-secondary">
                            <strong className="text-text-primary">Tú:</strong> {c}
                          </li>
                        ))}
                      </ul>
                    )}

                    <div className="flex items-center gap-4 pt-1 [&_.material-symbols-outlined]:text-[18px]">
                      <button
                        onClick={() => alternarLike(post)}
                        aria-pressed={meGusta}
                        aria-label={meGusta ? 'Quitar me gusta' : 'Me gusta'}
                        className={`min-h-11 px-3 rounded-full flex items-center gap-2 transition-colors ${meGusta ? 'bg-primary/15 text-primary' : 'text-text-secondary hover:text-primary'}`}
                      >
                        <span className="material-symbols-outlined">
                          {meGusta ? 'favorite' : 'favorite_border'}
                        </span>
                        <span className="font-label-md text-label-md">{likesDe(post)}</span>
                      </button>
                      <button
                        onClick={() => abrirComentarios(post)}
                        aria-label="Comentar"
                        className="min-h-11 min-w-11 flex items-center gap-2 text-text-secondary hover:text-text-primary transition-colors"
                      >
                        <span className="material-symbols-outlined">chat_bubble_outline</span>
                        <span className="font-label-md text-label-md">
                          {post.comentarios + locales.length}
                        </span>
                      </button>
                      <button
                        onClick={() => repostear(post)}
                        aria-pressed={Boolean(post.compartida)}
                        aria-label={
                          post.compartida ? 'Dejar de compartir' : 'Compartir en mi perfil'
                        }
                        className={`min-h-11 px-3 rounded-full flex items-center gap-2 transition-colors ${post.compartida ? 'bg-secondary/15 text-secondary' : 'text-text-secondary hover:text-secondary'}`}
                      >
                        <span className="material-symbols-outlined">repeat</span>
                        <span className="font-label-md text-label-md">{post.compartidos ?? 0}</span>
                      </button>
                      <button
                        onClick={() => void compartirPost(post)}
                        aria-label="Compartir"
                        className="min-h-11 min-w-11 justify-center flex items-center gap-2 text-text-secondary hover:text-text-primary transition-colors ml-auto"
                      >
                        <span className="material-symbols-outlined">share</span>
                      </button>
                    </div>
                    {post.ultimoComentario && (
                      <button
                        onClick={() => abrirComentarios(post)}
                        className="mt-3 w-full text-left rounded-xl bg-bg-primary/40 border border-border-subtle p-3"
                        data-testid="vista-comentario"
                      >
                        <span className="text-body-sm">
                          <strong className="text-text-primary">
                            {post.ultimoComentario.autor}
                          </strong>{' '}
                          <span className="text-text-secondary">{post.ultimoComentario.texto}</span>
                        </span>
                        {post.comentarios > 1 && (
                          <span className="block text-[11px] text-primary mt-1">
                            Ver los {post.comentarios} comentarios
                          </span>
                        )}
                      </button>
                    )}
                  </div>
                </article>
              </Fragment>
            )
          })}
        </div>

        <Dialogo
          titulo={`Comentar: ${comentando?.titulo ?? ''}`}
          abierto={comentando != null}
          onCerrar={() => setComentando(null)}
          acciones={
            <>
              <BotonTexto onClick={() => setComentando(null)}>Cerrar</BotonTexto>
              <BotonTexto onClick={() => void enviarComentario()} disabled={!texto.trim()}>
                Publicar
              </BotonTexto>
            </>
          }
        >
          {hilo.length > 0 && (
            <ul className="mb-3 max-h-60 overflow-y-auto space-y-2" aria-label="Comentarios">
              {hilo.map((c) => (
                <li key={c.id} className="text-body-sm flex items-start gap-2">
                  <span className="flex-1">
                    <strong className="text-text-primary">{c.autor}:</strong>{' '}
                    <span className="text-text-secondary">{c.texto}</span>
                  </span>
                  {sesion && c.autor !== sesion.nombre && (
                    <button
                      type="button"
                      aria-label={`Reportar comentario de ${c.autor}`}
                      onClick={() =>
                        yaReportado('comentario', c.id)
                          ? mostrarAviso('Ya reportaste este comentario')
                          : setReportandoComentario(c.id)
                      }
                      className="text-c2-muted hover:text-c2-peligro"
                    >
                      <span className="material-symbols-outlined text-[16px]">flag</span>
                    </button>
                  )}
                </li>
              ))}
            </ul>
          )}
          <textarea
            aria-label="Comentario"
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            rows={3}
            maxLength={1000}
            placeholder="Escribe un aplauso o comentario..."
            className="w-full px-3 py-2 bg-surface-container border border-border-subtle rounded-lg text-text-primary resize-none"
          />
        </Dialogo>
      </div>
      {escritorio && (
        <aside
          className="space-y-6 lg:sticky lg:top-8 lg:pt-8"
          aria-label="Resumen de la comunidad"
        >
          <RankingSemanal />
          <ProximosEventos />
        </aside>
      )}
      <ModalC2
        abierto={dejar != null}
        icono="person_remove"
        titulo="Dejar de seguir"
        texto={`Ya no verás las carreras de ${dejar?.nombre ?? ''} en tu feed de Siguiendo.`}
        primario="Dejar de seguir"
        peligro
        onPrimario={() => {
          if (dejar) void alternarSeguir(dejar.id)
          setDejar(null)
        }}
        onCerrar={() => setDejar(null)}
        secundario="Cancelar"
      />
      <ModalReporte
        tipo="comentario"
        postId={reportandoComentario}
        onCerrar={() => setReportandoComentario(null)}
        onEnviado={(ok) => {
          setReportandoComentario(null)
          mostrarAviso(ok ? 'Reporte enviado' : 'No se pudo enviar el reporte. Revisa tu conexión')
        }}
      />
      <ModalReporte
        postId={reportando}
        onCerrar={() => setReportando(null)}
        onEnviado={(ok) => {
          setReportando(null)
          mostrarAviso(
            ok
              ? 'Reporte enviado. Gracias por cuidar la comunidad'
              : 'No se pudo enviar el reporte. Revisa tu conexión'
          )
        }}
      />
      <Aviso mensaje={aviso} />
    </div>
  )
}
