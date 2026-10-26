import { Fragment, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ModalC2 } from '../../../components/modales'
import { ProximosEventos, RankingSemanal } from '../../../components/PanelesEscritorio'
import { Aviso, BotonTexto, Dialogo } from '../../../components/ui'
import { compartir, useAviso } from '../../../hooks'
import { listarEnVivo, suscribirLista, type SesionEnVivo } from '../../../lib/enVivo'
import { filtrarEventos } from '../../../lib/filtros'
import { formatearMs, formatearRitmo, Unidades } from '../../../lib/geo'
import {
  construirFeed,
  filtrarFeed,
  kmPorAutor,
  nivelCorredor,
  type FiltroFeed,
  type Publicacion,
} from '../../../lib/logica'
import { useEsEscritorio } from '../../../lib/useEsEscritorio'
import type { ComentarioPublicacion } from '../../../shared/kernel/comunidad'
import { useApp } from '../../../store/app'
import { yaReportada, yaReportado } from '../../moderacion/api'
import { AvatarLetra, handle, InsigniaNivel, MediaPost, TarjetaClub } from './PostSocial'
import { ModalReporte } from './Social'

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
        <div className="pt-[max(env(safe-area-inset-top),8px)] lg:pt-4">
          <div className="relative h-14 px-4 flex items-center justify-between lg:px-0">
            <AvatarLetra nombre={sesion?.nombre ?? 'Tú'} />
            <h1 className="absolute left-1/2 -translate-x-1/2 font-display italic font-black uppercase text-[22px] text-white tracking-wide">
              Comunidad
            </h1>
            <button
              onClick={() => setMenuFiltro((v) => !v)}
              aria-label="Filtrar publicaciones"
              aria-expanded={menuFiltro}
              data-testid="boton-buscar-feed"
              className="w-9 h-9 rounded-full bg-c2-s2 border border-c2-linea flex items-center justify-center text-white"
            >
              <span className="material-symbols-outlined text-[20px]">search</span>
            </button>
            {menuFiltro && (
              <div
                role="menu"
                className="absolute right-4 top-12 z-30 min-w-[200px] bg-c2-card border border-c2-linea rounded-xl shadow-xl overflow-hidden lg:right-0"
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
                    className={`block w-full text-left px-4 py-2 text-sm hover:bg-c2-s2 ${filtro === f ? 'text-c2-violeta' : 'text-white'}`}
                  >
                    {f}
                  </button>
                ))}
                <div className="border-t border-c2-linea" />
                <button
                  role="menuitem"
                  onClick={() => navigate('/comunidad/corredores')}
                  className="block w-full text-left px-4 py-2 text-sm hover:bg-c2-s2 text-white"
                >
                  Buscar corredores
                </button>
                <button
                  role="menuitem"
                  onClick={() => navigate('/comunidad/en-vivo')}
                  className="block w-full text-left px-4 py-2 text-sm hover:bg-c2-s2 text-white"
                >
                  Corredores en vivo
                </button>
                <button
                  role="menuitem"
                  onClick={() => navigate('/comunidad/eventos')}
                  className="block w-full text-left px-4 py-2 text-sm hover:bg-c2-s2 text-white"
                >
                  Eventos
                </button>
              </div>
            )}
          </div>

          <nav aria-label="Secciones de comunidad" className="flex border-b border-c2-linea">
            {pestanas.map((t) => (
              <button
                key={t.id}
                onClick={() => setFiltro(t.id)}
                aria-current={filtro === t.id ? 'page' : undefined}
                className={`flex-1 h-11 text-[13px] font-black italic uppercase tracking-wide border-b-2 transition-colors ${
                  filtro === t.id
                    ? 'text-white border-c2-violeta'
                    : 'text-c2-muted border-transparent'
                }`}
              >
                {t.etiqueta}
              </button>
            ))}
            <button
              onClick={() => navigate('/comunidad/clubes')}
              className="flex-1 h-11 text-[13px] font-black italic uppercase tracking-wide text-c2-muted border-b-2 border-transparent"
            >
              Clubes
            </button>
          </nav>

          {enVivo.length > 0 && (
            <Link
              to="/comunidad/en-vivo"
              data-testid="banner-en-vivo"
              className="flex items-center gap-3 min-h-[44px] mx-4 mt-3 px-4 rounded-[10px] bg-c2-peligro/15 border border-c2-peligro/40 text-white text-sm lg:mx-0"
            >
              <span className="w-2.5 h-2.5 rounded-full bg-c2-peligro animate-pulse" />
              <span className="flex-1 font-semibold">
                {enVivo.length === 1
                  ? `${enVivo[0].nombre} está corriendo en vivo`
                  : `${enVivo.length} corredores en vivo ahora`}
              </span>
              <span className="material-symbols-outlined">chevron_right</span>
            </Link>
          )}

          <div
            className="px-4 pt-3 pb-3 border-b border-c2-linea lg:px-0"
            data-testid="compositor-feed"
          >
            <div className="flex items-center gap-3">
              <AvatarLetra nombre={sesion?.nombre ?? 'Tú'} />
              <button
                onClick={() => navigate('/correr')}
                className="flex-1 text-left text-[14px] text-c2-muted"
              >
                ¿Cómo te fue hoy{sesion?.nombre ? `, ${sesion.nombre.split(' ')[0]}` : ''}?
              </button>
            </div>
            <div className="mt-2 pl-12 flex items-center gap-4 text-c2-violeta [&_.material-symbols-outlined]:text-[18px]">
              <span className="material-symbols-outlined" aria-hidden="true">
                image
              </span>
              <span className="material-symbols-outlined" aria-hidden="true">
                directions_run
              </span>
              <span className="material-symbols-outlined" aria-hidden="true">
                map
              </span>
              <button
                onClick={() => navigate('/correr')}
                data-testid="boton-publicar"
                className="ml-auto h-9 px-5 text-white text-[13px] font-black italic uppercase bg-gradient-to-r from-[#7b1fa2] to-c2-morado [clip-path:polygon(8px_0,100%_0,calc(100%-8px)_100%,0_100%)]"
              >
                Publicar
              </button>
            </div>
          </div>
          {filtro !== 'Todos' && filtro !== 'Siguiendo' && (
            <p className="px-4 mt-2 text-[12px] text-c2-muted lg:px-0">Filtro: {filtro}</p>
          )}
        </div>

        <div className="pb-space-3xl">
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
                  className="relative flex gap-3 px-4 pt-3 pb-2 border-b border-c2-linea lg:px-0"
                  data-testid="publicacion"
                >
                  <div className="relative flex flex-col items-center">
                    {post.avatar ? (
                      <img src={post.avatar} alt="" className="w-9 h-9 rounded-full object-cover" />
                    ) : (
                      <AvatarLetra nombre={post.autor} />
                    )}
                    {post.ultimoComentario && <span className="mt-1 flex-1 w-0.5 bg-c2-linea" />}
                  </div>
                  <div className="flex-1 min-w-0">
                    <header className="relative flex items-center gap-1.5 min-w-0">
                      <span className="text-[14px] font-bold text-white truncate">
                        {post.autor}
                      </span>
                      <InsigniaNivel nivel={nivelDe(post)} />
                      <span className="text-[12px] text-c2-muted truncate min-w-0">
                        {handle(post.autor)} · {post.tiempo}
                      </span>
                      <button
                        onClick={() => setMenuPost(menuPost === post.id ? null : post.id)}
                        aria-label="Más opciones"
                        className="ml-auto w-8 h-8 -mr-1 shrink-0 flex items-center justify-center text-c2-muted hover:text-white"
                      >
                        <span className="material-symbols-outlined text-[18px]">more_horiz</span>
                      </button>
                      {menuPost === post.id && (
                        <div
                          role="menu"
                          className="absolute right-0 top-8 z-30 bg-c2-card border border-c2-linea rounded-xl shadow-xl overflow-hidden"
                        >
                          {post.autorId && (
                            <button
                              role="menuitem"
                              onClick={() => {
                                setMenuPost(null)
                                if (siguiendo.includes(post.autorId!))
                                  setDejar({ id: post.autorId!, nombre: post.autor })
                                else void alternarSeguir(post.autorId!)
                              }}
                              className="block w-full text-left px-4 py-2 text-sm text-white hover:bg-c2-s2"
                            >
                              {siguiendo.includes(post.autorId) ? 'Dejar de seguir' : 'Seguir'}
                            </button>
                          )}
                          {post.propia && post.remota && (
                            <label
                              role="menuitem"
                              className="block w-full text-left px-4 py-2 text-sm text-white hover:bg-c2-s2 cursor-pointer"
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
                            className="block w-full text-left px-4 py-2 text-sm text-white hover:bg-c2-s2"
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
                              className="block w-full text-left px-4 py-2 text-sm text-c2-peligro hover:bg-c2-s2"
                            >
                              Reportar publicación
                            </button>
                          )}
                        </div>
                      )}
                    </header>
                    <p className="text-[14px] leading-snug text-white/90">{post.titulo}</p>
                    {post.distanciaKm > 0 && (
                      <div
                        className="mt-1.5 mb-2 flex flex-wrap items-baseline gap-x-3 font-display italic font-black text-[14px] tabular-nums"
                        data-testid="metricas-post"
                      >
                        <span className="text-c2-violeta">
                          {Unidades.texto(post.distanciaKm, m, 1)}
                          <span className="font-sans not-italic font-normal text-[10px] text-c2-muted">
                            {Unidades.etiquetaDistancia(m)}
                          </span>
                        </span>
                        <span className="text-white">
                          {formatearRitmo(Unidades.ritmo(post.ritmoSegKm, m))}
                          <span className="font-sans not-italic font-normal text-[10px] text-c2-muted">
                            /{m ? 'km' : 'mi'}
                          </span>
                        </span>
                        <span className="text-white">{formatearMs(post.segundos)}</span>
                        <span className="text-c2-cielo" data-testid="desnivel-post">
                          +{post.desnivelM ?? 0}
                          <span className="font-sans not-italic font-normal text-[10px] text-c2-muted">
                            m
                          </span>
                        </span>
                      </div>
                    )}
                    {(post.distanciaKm > 0 || (post.imagenes ?? []).length > 0) && (
                      <MediaPost imagenes={post.imagenes ?? []} puntos={post.puntos} />
                    )}
                    {locales.length > 0 && (
                      <ul className="mt-2 space-y-1">
                        {locales.map((c, i) => (
                          <li key={i} className="text-[13px] text-c2-muted">
                            <strong className="text-white">Tú:</strong> {c}
                          </li>
                        ))}
                      </ul>
                    )}

                    <div className="mt-1 flex items-center justify-between text-c2-muted [&_.material-symbols-outlined]:text-[17px] [&_button]:min-h-9 [&_button]:flex [&_button]:items-center [&_button]:gap-1.5 [&_button]:text-[12px]">
                      <button
                        onClick={() => abrirComentarios(post)}
                        aria-label="Comentar"
                        data-testid="accion-comentar"
                        className="hover:text-white"
                      >
                        <span className="material-symbols-outlined">chat_bubble_outline</span>
                        {post.comentarios + locales.length}
                      </button>
                      <button
                        onClick={() => repostear(post)}
                        aria-pressed={Boolean(post.compartida)}
                        aria-label={
                          post.compartida ? 'Dejar de compartir' : 'Compartir en mi perfil'
                        }
                        data-testid="accion-repost"
                        className={post.compartida ? 'text-c2-menta' : 'hover:text-c2-menta'}
                      >
                        <span className="material-symbols-outlined">repeat</span>
                        {post.compartidos ?? 0}
                      </button>
                      <button
                        onClick={() => alternarLike(post)}
                        aria-pressed={meGusta}
                        aria-label={meGusta ? 'Quitar me gusta' : 'Me gusta'}
                        data-testid="accion-like"
                        className={meGusta ? 'text-c2-violeta' : 'hover:text-c2-violeta'}
                      >
                        <span className="material-symbols-outlined">
                          {meGusta ? 'favorite' : 'favorite_border'}
                        </span>
                        {likesDe(post)}
                      </button>
                      <button
                        onClick={() => void compartirPost(post)}
                        aria-label="Compartir"
                        className="hover:text-white"
                      >
                        <span className="material-symbols-outlined">ios_share</span>
                      </button>
                    </div>
                    {post.ultimoComentario && (
                      <button
                        onClick={() => abrirComentarios(post)}
                        className="mt-1 mb-1 w-full text-left"
                        data-testid="vista-comentario"
                      >
                        <span className="flex gap-2 rounded-[10px] bg-c2-s2 border border-c2-linea p-2.5">
                          <AvatarLetra nombre={post.ultimoComentario.autor} chico />
                          <span className="min-w-0 text-[13px]">
                            <strong className="text-white">{post.ultimoComentario.autor}</strong>{' '}
                            <span className="text-c2-muted">
                              {handle(post.ultimoComentario.autor)}
                            </span>
                            <span className="block text-white/85">
                              {post.ultimoComentario.texto}
                            </span>
                          </span>
                        </span>
                        {post.comentarios > 1 && (
                          <span className="block text-[12px] text-c2-violeta mt-1.5">
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
