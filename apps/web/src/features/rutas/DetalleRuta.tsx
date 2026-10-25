import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { MapaGoogle } from '../../components/MapaGoogle'
import { TrazadoRuta } from '../../components/TrazadoRuta'
import type { CorredorRuta, FotoRuta, ResenaRuta } from '../../data/repositorios'
import { PerfilElevacion } from './PerfilElevacion'
import { CompartirDialogo } from '../../components/CompartirDialogo'
import { enlaceRuta, type DatosCompartir } from '../../lib/compartir'
import { subirVistaPrevia } from '../../lib/vistaPrevia'
import { useRacha } from '../../store/app'
import { aPuntosGps } from '../../lib/enVivo'
import { Unidades } from '../../lib/geo'
import { CATEGORIAS_RUTA, ETIQUETAS_RUTA } from '../../lib/rutas'
import { InsigniaOficial } from './ListaRutas'
import { useApp } from '../../store/app'
import { Estrellas, SelectorEstrellas } from './Estrellas'

export function DetalleRuta() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const ruta = useApp((s) => s.rutas.find((r) => r.id === id))
  const m = useApp((s) => s.ajustes.unidadesMetricas)
  const sesion = useApp((s) => s.sesion)
  const sociales = useApp((s) => s.rutasSociales)
  const meGusta = useApp((s) => s.rutasMeGusta.includes(id))
  const alternarMeGusta = useApp((s) => s.alternarMeGustaRuta)
  const guardada = useApp((s) => s.rutasGuardadas.includes(id))
  const alternarGuardada = useApp((s) => s.alternarRutaGuardada)
  const resenasRuta = useApp((s) => s.resenasRuta)
  const resenarRuta = useApp((s) => s.resenarRuta)
  const [resenas, setResenas] = useState<ResenaRuta[]>([])
  const [estrellas, setEstrellas] = useState(0)
  const [comentario, setComentario] = useState('')
  const [aviso, setAviso] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const explorar = useApp((s) => s.explorar)
  const elegirRutaActiva = useApp((s) => s.elegirRutaActiva)
  const [fotos, setFotos] = useState<FotoRuta[]>([])
  const [corredores, setCorredores] = useState<{ lista: CorredorRuta[]; total: number }>({
    lista: [],
    total: 0,
  })
  const [subiendo, setSubiendo] = useState(false)
  const [compartiendo, setCompartiendo] = useState(false)
  const rachaDias = useRacha().diaria

  useEffect(() => {
    if (!sociales) return
    void resenasRuta(id).then(setResenas)
    void explorar()?.fotos(id).then(setFotos)
    void explorar()?.corredores(id).then(setCorredores)
  }, [id, sociales, resenasRuta, explorar])

  const subirFoto = async (archivo: File | undefined) => {
    const repo = explorar()
    if (!archivo || !repo) return
    setSubiendo(true)
    try {
      await repo.subirFoto(id, archivo)
      setFotos(await repo.fotos(id))
      setAviso('Foto subida')
    } catch (e) {
      setAviso(e instanceof Error ? e.message : 'No se pudo subir la foto')
    } finally {
      setSubiendo(false)
    }
  }

  const datosCompartir = useMemo<DatosCompartir>(
    () => ({
      tipo: 'ruta',
      titulo: ruta?.nombre ?? 'Ruta',
      puntos: ruta?.puntos ?? [],
      distanciaKm: ruta?.distanciaKm ?? 0,
      desnivelM: ruta?.elevacionM,
      rachaDias,
      enlace: enlaceRuta(id),
    }),
    [ruta?.nombre, ruta?.puntos, ruta?.distanciaKm, ruta?.elevacionM, rachaDias, id]
  )
  const esPropia = !!ruta?.propia
  const alGenerarPost = useCallback(
    (b: Blob) => {
      if (esPropia) void subirVistaPrevia(id, b)
    },
    [esPropia, id]
  )
  if (!ruta)
    return (
      <main className="w-full min-h-screen bg-surface px-margin py-6">
        <h1 className="font-display italic uppercase text-[26px] leading-tight text-text-primary mb-2">
          Ruta no encontrada
        </h1>
        <Link to="/rutas" className="text-primary-400 underline">
          Volver a rutas
        </Link>
      </main>
    )

  const enviar = async () => {
    if (!estrellas) return setAviso('Elige de 1 a 5 estrellas')
    setEnviando(true)
    try {
      await resenarRuta(id, estrellas, comentario)
      setResenas(await resenasRuta(id))
      setComentario('')
      setAviso('¡Gracias por tu reseña!')
    } catch (e) {
      setAviso(e instanceof Error ? e.message : 'No se pudo enviar la reseña')
    } finally {
      setEnviando(false)
    }
  }

  const puntos = ruta.puntos ?? []
  return (
    <main className="w-full min-h-screen bg-surface contenedor-escritorio lg:max-w-5xl pb-28">
      <header className="flex items-center gap-3 px-margin py-4 lg:px-0">
        <button
          onClick={() => navigate(-1)}
          aria-label="Volver"
          className="w-11 h-11 rounded-full bg-surface-container flex items-center justify-center text-text-primary"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </button>
        <h1 className="flex-1 min-w-0 font-headline-md text-headline-md text-text-primary truncate">
          {ruta.nombre}
        </h1>
        <button
          onClick={() => void alternarGuardada(id)}
          aria-pressed={guardada}
          aria-label={guardada ? 'Quitar de guardadas' : 'Guardar ruta'}
          className="w-11 h-11 rounded-full bg-surface-container flex items-center justify-center text-primary-400"
        >
          <span
            className="material-symbols-outlined"
            style={{ fontVariationSettings: guardada ? "'FILL' 1" : "'FILL' 0" }}
          >
            bookmark
          </span>
        </button>
      </header>

      <div className="px-margin lg:px-0 lg:grid lg:grid-cols-[minmax(0,1fr)_360px] lg:gap-8 space-y-4 lg:space-y-0">
        <div className="space-y-4">
          <div className="h-[280px] lg:h-[420px] rounded-2xl overflow-hidden bg-surface-container border border-border-subtle">
            <MapaGoogle
              puntos={puntos}
              zoom={14}
              className="w-full h-full"
              alternativa={<TrazadoRuta puntos={aPuntosGps(puntos)} className="w-full h-full" />}
            />
          </div>
          <div className="grid grid-cols-3 gap-3">
            {[
              [
                `${Unidades.texto(ruta.distanciaKm, m, 1)} ${Unidades.etiquetaDistancia(m)}`,
                'Distancia',
              ],
              [`${ruta.elevacionM} m`, 'Desnivel'],
              [ruta.dificultad, 'Dificultad'],
            ].map(([v, e]) => (
              <div key={e} className="bg-surface-container rounded-xl p-3 text-center">
                <div className="text-[15px] font-semibold text-text-primary whitespace-nowrap">
                  {v}
                </div>
                <div className="text-xs text-text-secondary">{e}</div>
              </div>
            ))}
          </div>
          {(ruta.perfilElevacion?.length ?? 0) > 1 && (
            <PerfilElevacion alturas={ruta.perfilElevacion!} distanciaKm={ruta.distanciaKm} />
          )}
          {ruta.oficial && (
            <div className="flex flex-wrap items-center gap-2" data-testid="detalle-oficial">
              <InsigniaOficial />
              {ruta.categoria && (
                <span className="text-[12px] text-c2-muted">
                  {CATEGORIAS_RUTA.find(([k]) => k === ruta.categoria)?.[1]}
                  {ruta.superficie ? ` · superficie ${ruta.superficie}` : ''}
                </span>
              )}
            </div>
          )}
          <p className="text-text-secondary">{ruta.descripcion}</p>
          {ruta.fuente && (
            <p className="text-[11px] text-c2-muted" data-testid="fuente-ruta">
              Trazado: {ruta.fuente}. Datos (c) colaboradores de OpenStreetMap.
            </p>
          )}
          {(ruta.etiquetas?.length ?? 0) > 0 && (
            <ul className="flex flex-wrap gap-2" aria-label="Etiquetas">
              {ruta.etiquetas!.map((t) => (
                <li
                  key={t}
                  className="px-3 py-1 rounded-full bg-surface-container text-sm text-text-primary"
                >
                  #{ETIQUETAS_RUTA[t] ?? t}
                </li>
              ))}
            </ul>
          )}
          {ruta.autorNombre && (
            <p className="text-sm text-text-secondary">Creada por {ruta.autorNombre}</p>
          )}
          <div className="flex gap-3">
            <button
              onClick={() => {
                elegirRutaActiva(ruta.id)
                navigate(`/rutas/buscar?ruta=${ruta.id}`)
              }}
              data-testid="correr-ruta"
              className="flex-1 h-12 rounded-xl bg-primary-600 text-on-primary font-semibold flex items-center justify-center gap-2"
            >
              <span className="material-symbols-outlined">directions_run</span>
              Correr esta ruta
            </button>
            <button
              onClick={() => setCompartiendo(true)}
              aria-label="Compartir ruta"
              data-testid="compartir-ruta"
              className="h-12 w-12 rounded-xl bg-surface-container text-text-primary flex items-center justify-center"
            >
              <span className="material-symbols-outlined">share</span>
            </button>
            {sociales && (
              <button
                onClick={() =>
                  sesion
                    ? void alternarMeGusta(id).catch((e: Error) => setAviso(e.message))
                    : setAviso('Inicia sesión para dar me gusta')
                }
                aria-pressed={meGusta}
                aria-label={meGusta ? 'Quitar me gusta' : 'Me gusta'}
                data-testid="me-gusta-ruta"
                className="h-12 px-4 rounded-xl bg-surface-container flex items-center gap-2 text-text-primary"
              >
                <span
                  className={`material-symbols-outlined ${meGusta ? 'text-red-400' : ''}`}
                  style={{ fontVariationSettings: meGusta ? "'FILL' 1" : "'FILL' 0" }}
                >
                  favorite
                </span>
                {ruta.meGusta ?? 0}
              </button>
            )}
          </div>
        </div>

        {sociales && (
          <div className="space-y-6">
            <section aria-labelledby="titulo-fotos" className="space-y-3">
              <div className="flex items-center justify-between">
                <h2 id="titulo-fotos" className="font-title-md text-title-md text-text-primary">
                  Fotos
                </h2>
                {sesion && (
                  <label className="h-10 px-3 rounded-full bg-surface-container text-text-primary flex items-center gap-1 cursor-pointer focus-within:ring-2 focus-within:ring-primary-600">
                    <span className="material-symbols-outlined text-[18px]">add_a_photo</span>
                    {subiendo ? 'Subiendo…' : 'Añadir foto'}
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/webp"
                      className="sr-only"
                      data-testid="subir-foto"
                      onChange={(ev) => void subirFoto(ev.target.files?.[0])}
                    />
                  </label>
                )}
              </div>
              {fotos.length === 0 ? (
                <p className="text-sm text-text-secondary">Aún no hay fotos de esta ruta.</p>
              ) : (
                <ul className="grid grid-cols-3 gap-2" data-testid="fotos-ruta">
                  {fotos.map((f) => (
                    <li key={f.id}>
                      <a href={f.url} target="_blank" rel="noreferrer">
                        <img
                          src={f.url}
                          alt={`Foto de ${ruta.nombre}`}
                          loading="lazy"
                          className="w-full aspect-square object-cover rounded-xl"
                        />
                      </a>
                    </li>
                  ))}
                </ul>
              )}
            </section>
            <section aria-labelledby="titulo-corredores" className="space-y-2">
              <h2 id="titulo-corredores" className="font-title-md text-title-md text-text-primary">
                Quién la ha corrido
              </h2>
              <p className="text-sm text-text-secondary" data-testid="total-corredores">
                {corredores.total === 0
                  ? 'Nadie la ha corrido todavía. ¡Sé el primero!'
                  : `${corredores.total} ${corredores.total === 1 ? 'persona' : 'personas'}`}
              </p>
              <ul className="space-y-1" data-testid="lista-corredores">
                {corredores.lista.map((c) => (
                  <li key={c.usuarioId} className="flex justify-between text-sm">
                    <span className="text-text-primary">{c.nombre}</span>
                    <span className="text-text-secondary">
                      {c.veces} {c.veces === 1 ? 'vez' : 'veces'}
                    </span>
                  </li>
                ))}
              </ul>
            </section>
            <section aria-labelledby="titulo-resenas" className="space-y-4">
              <div className="flex items-center justify-between">
                <h2 id="titulo-resenas" className="font-title-md text-title-md text-text-primary">
                  Reseñas
                </h2>
                {ruta.valoracion != null && (
                  <span
                    className="flex items-center gap-1 text-text-primary"
                    data-testid="valoracion-ruta"
                  >
                    <Estrellas valor={ruta.valoracion} />
                    {ruta.valoracion.toFixed(1)} · {ruta.resenas}
                  </span>
                )}
              </div>
              {sesion ? (
                <div className="bg-surface-container rounded-2xl p-4 space-y-3">
                  <SelectorEstrellas valor={estrellas} onCambiar={setEstrellas} />
                  <label className="block">
                    <span className="sr-only">Comentario</span>
                    <textarea
                      value={comentario}
                      onChange={(e) => setComentario(e.target.value)}
                      maxLength={500}
                      rows={3}
                      placeholder="¿Qué tal la ruta? (opcional)"
                      className="w-full rounded-xl bg-surface p-3 text-text-primary border border-border-subtle placeholder-text-secondary"
                    />
                  </label>
                  <button
                    onClick={() => void enviar()}
                    disabled={enviando}
                    className="w-full h-11 rounded-xl bg-primary-600 text-on-primary font-semibold disabled:opacity-60"
                  >
                    {enviando ? 'Enviando…' : 'Publicar reseña'}
                  </button>
                </div>
              ) : (
                <p className="text-text-secondary">Inicia sesión para dejar una reseña.</p>
              )}
              {aviso && (
                <p role="status" className="text-sm text-text-primary">
                  {aviso}
                </p>
              )}
              <ul className="space-y-3" data-testid="lista-resenas">
                {resenas.length === 0 && (
                  <li className="text-text-secondary text-sm">Aún no hay reseñas.</li>
                )}
                {resenas.map((r) => (
                  <li key={r.id} className="bg-surface-container rounded-xl p-3">
                    <div className="flex items-center justify-between gap-2">
                      <span className="font-semibold text-text-primary truncate">{r.nombre}</span>
                      <span
                        className="flex items-center"
                        aria-label={`${r.estrellas} de 5 estrellas`}
                      >
                        <Estrellas valor={r.estrellas} tamano={16} />
                      </span>
                    </div>
                    {r.comentario && (
                      <p className="text-sm text-text-secondary mt-1">{r.comentario}</p>
                    )}
                  </li>
                ))}
              </ul>
            </section>
          </div>
        )}
      </div>
      {compartiendo && (
        <CompartirDialogo
          datos={datosCompartir}
          onCerrar={() => setCompartiendo(false)}
          alGenerarPost={alGenerarPost}
        />
      )}
    </main>
  )
}
