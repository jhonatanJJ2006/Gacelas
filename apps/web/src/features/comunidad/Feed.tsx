import { useEsEscritorio } from '../../lib/useEsEscritorio'
import { ProximosEventos, RankingSemanal } from '../../components/PanelesEscritorio'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { TrazadoRuta } from '../../components/TrazadoRuta'
import { Aviso, BotonTexto, Dialogo } from '../../components/ui'
import { compartir, useAviso } from '../../hooks'
import { formatearMs, formatearRitmo, Unidades } from '../../lib/geo'
import { useApp } from '../../store/app'
import { construirFeed, filtrarFeed, type FiltroFeed, type Publicacion } from '../../lib/logica'

export function Feed() {
  const escritorio = useEsEscritorio()
  const navigate = useNavigate()
  const sesion = useApp((s) => s.sesion)
  const carreras = useApp((s) => s.carreras)
  const remotas = useApp((s) => s.publicaciones)
  const siguiendo = useApp((s) => s.siguiendo)
  const alternarSeguir = useApp((s) => s.alternarSeguir)
  const ajustes = useApp((s) => s.ajustes)
  const actualizar = useApp((s) => s.actualizarAjustes)
  const m = ajustes.unidadesMetricas
  const [filtro, setFiltro] = useState<FiltroFeed>('Todos')
  const [menuFiltro, setMenuFiltro] = useState(false)
  const [menuPost, setMenuPost] = useState<string | null>(null)
  const [comentando, setComentando] = useState<Publicacion | null>(null)
  const [texto, setTexto] = useState('')
  const [aviso, mostrarAviso] = useAviso()

  const posts = filtrarFeed(
    construirFeed(carreras, sesion?.nombre ?? 'Tú', Date.now(), remotas),
    filtro,
    ajustes.ocultas
  )

  const alternarLike = (id: string) =>
    void actualizar((a) => ({
      likes: a.likes.includes(id) ? a.likes.filter((x) => x !== id) : [...a.likes, id],
    }))

  const enviarComentario = () => {
    if (!comentando || !texto.trim()) return
    const id = comentando.id
    void actualizar((a) => ({
      comentarios: { ...a.comentarios, [id]: [...(a.comentarios[id] ?? []), texto.trim()] },
    }))
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

  return (
    <div className="contenedor-escritorio lg:grid lg:grid-cols-[minmax(0,1fr)_340px] lg:gap-8 lg:items-start">
      <div className="flex flex-col w-full min-w-0">
        <div className="px-margin pt-space-md pb-space-sm lg:px-0 lg:pb-6">
          <div className="flex items-center justify-between mb-4 relative">
            <h1 className="font-headline-md text-headline-md text-text-primary">Comunidad</h1>
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
                {(['Todos', 'Comunidad', 'Mías'] as FiltroFeed[]).map((f) => (
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

          <div className="flex gap-2 mb-4">
            <span
              className="px-4 py-2 rounded-full font-label-md text-label-md bg-primary-600 text-bg-primary"
              aria-current="page"
            >
              Feed
            </span>
            <button
              onClick={() => navigate('/comunidad/eventos')}
              className="px-4 py-2 rounded-full font-label-md text-label-md bg-surface-container text-text-secondary hover:text-text-primary"
            >
              Eventos
            </button>
            {filtro !== 'Todos' && (
              <span className="px-3 py-2 text-label-md text-text-secondary">Filtro: {filtro}</span>
            )}
          </div>
        </div>

        <div className="px-margin space-y-4 pb-space-3xl lg:px-0 lg:grid lg:grid-cols-1 xl:grid-cols-2 lg:gap-6 lg:space-y-0 lg:items-start">
          {posts.length === 0 && (
            <p className="text-center text-text-secondary py-8 lg:col-span-full">
              {filtro === 'Mías' ? 'Aún no tienes carreras públicas.' : 'No hay publicaciones.'}
            </p>
          )}
          {posts.map((post) => {
            const meGusta = ajustes.likes.includes(post.id)
            const misComentarios = ajustes.comentarios[post.id] ?? []
            return (
              <article
                key={post.id}
                className="bg-surface-container rounded-2xl border border-border-subtle overflow-hidden"
                data-testid="publicacion"
              >
                <div className="p-4 flex items-center gap-3 border-b border-border-subtle relative">
                  <div className="w-10 h-10 rounded-full bg-primary-600/30 flex items-center justify-center text-primary-400 font-bold">
                    {post.autor.charAt(0)}
                  </div>
                  <div className="flex-1">
                    <p className="font-body-md text-body-md text-text-primary font-semibold">
                      {post.autor}
                    </p>
                    <p className="font-body-sm text-body-sm text-text-secondary">
                      {post.titulo} · {post.tiempo}
                    </p>
                  </div>
                  {post.autorId && (
                    <button
                      onClick={() => void alternarSeguir(post.autorId!)}
                      aria-pressed={siguiendo.includes(post.autorId)}
                      className={`h-9 px-4 rounded-full text-sm font-semibold transition-colors ${
                        siguiendo.includes(post.autorId)
                          ? 'bg-surface-container-high text-text-primary border border-border-subtle'
                          : 'bg-primary text-on-primary hover:bg-primary-700'
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
                    </div>
                  )}
                </div>

                <div className="h-48 bg-gradient-to-br from-primary-600/20 to-secondary-600/20 flex items-center justify-center">
                  {post.puntos.length > 1 ? (
                    <TrazadoRuta puntos={post.puntos} className="w-full h-full p-4" />
                  ) : (
                    <span className="material-symbols-outlined text-[64px] text-primary">
                      route
                    </span>
                  )}
                </div>

                <div className="p-4">
                  <div className="grid grid-cols-3 gap-4 mb-4">
                    <div className="text-center">
                      <div className="font-headline-sm text-headline-sm text-primary">
                        {Unidades.texto(post.distanciaKm, m, 1)}
                      </div>
                      <div className="font-label-md text-label-md text-text-secondary">
                        {Unidades.etiquetaDistancia(m)}
                      </div>
                    </div>
                    <div className="text-center">
                      <div className="font-headline-sm text-headline-sm text-text-primary">
                        {formatearMs(post.segundos)}
                      </div>
                      <div className="font-label-md text-label-md text-text-secondary">tiempo</div>
                    </div>
                    <div className="text-center">
                      <div className="font-headline-sm text-headline-sm text-secondary">
                        {formatearRitmo(Unidades.ritmo(post.ritmoSegKm, m))}
                      </div>
                      <div className="font-label-md text-label-md text-text-secondary">
                        {Unidades.etiquetaRitmo(m)}
                      </div>
                    </div>
                  </div>

                  {misComentarios.length > 0 && (
                    <ul className="mb-3 space-y-1">
                      {misComentarios.map((c, i) => (
                        <li key={i} className="text-body-sm text-text-secondary">
                          <strong className="text-text-primary">Tú:</strong> {c}
                        </li>
                      ))}
                    </ul>
                  )}

                  <div className="flex items-center gap-6 pt-3 border-t border-border-subtle">
                    <button
                      onClick={() => alternarLike(post.id)}
                      aria-pressed={meGusta}
                      aria-label={meGusta ? 'Quitar me gusta' : 'Me gusta'}
                      className={`min-h-11 min-w-11 flex items-center gap-2 transition-colors ${meGusta ? 'text-danger' : 'text-text-secondary hover:text-primary'}`}
                    >
                      <span className="material-symbols-outlined">
                        {meGusta ? 'favorite' : 'favorite_border'}
                      </span>
                      <span className="font-label-md text-label-md">
                        {post.likes + (meGusta ? 1 : 0)}
                      </span>
                    </button>
                    <button
                      onClick={() => setComentando(post)}
                      aria-label="Comentar"
                      className="min-h-11 min-w-11 flex items-center gap-2 text-text-secondary hover:text-text-primary transition-colors"
                    >
                      <span className="material-symbols-outlined">chat_bubble_outline</span>
                      <span className="font-label-md text-label-md">
                        {post.comentarios + misComentarios.length}
                      </span>
                    </button>
                    <button
                      onClick={() => void compartirPost(post)}
                      aria-label="Compartir"
                      className="min-h-11 min-w-11 justify-center flex items-center gap-2 text-text-secondary hover:text-text-primary transition-colors ml-auto"
                    >
                      <span className="material-symbols-outlined">share</span>
                    </button>
                  </div>
                </div>
              </article>
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
              <BotonTexto onClick={enviarComentario} disabled={!texto.trim()}>
                Publicar
              </BotonTexto>
            </>
          }
        >
          <textarea
            aria-label="Comentario"
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            rows={3}
            placeholder="Escribe un comentario..."
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
      <Aviso mensaje={aviso} />
    </div>
  )
}
