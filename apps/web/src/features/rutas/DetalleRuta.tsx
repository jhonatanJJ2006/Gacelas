import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { MapaGoogle } from '../../components/MapaGoogle'
import { TrazadoRuta } from '../../components/TrazadoRuta'
import type { ResenaRuta } from '../../data/repositorios'
import { aPuntosGps } from '../../lib/enVivo'
import { Unidades } from '../../lib/geo'
import { ETIQUETAS_RUTA } from '../../lib/rutas'
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

  useEffect(() => {
    if (sociales) void resenasRuta(id).then(setResenas)
  }, [id, sociales, resenasRuta])

  if (!ruta)
    return (
      <main className="w-full min-h-screen bg-surface px-margin py-6">
        <h1 className="font-headline-md text-headline-md text-text-primary mb-2">
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
                <div className="font-headline-sm text-headline-sm text-text-primary whitespace-nowrap">
                  {v}
                </div>
                <div className="text-xs text-text-secondary">{e}</div>
              </div>
            ))}
          </div>
          <p className="text-text-secondary">{ruta.descripcion}</p>
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
              onClick={() => navigate(`/rutas/buscar?ruta=${ruta.id}`)}
              data-testid="correr-ruta"
              className="flex-1 h-12 rounded-xl bg-primary-600 text-bg-primary font-semibold flex items-center justify-center gap-2"
            >
              <span className="material-symbols-outlined">directions_run</span>
              Correr esta ruta
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
                  className="w-full h-11 rounded-xl bg-primary-600 text-bg-primary font-semibold disabled:opacity-60"
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
        )}
      </div>
    </main>
  )
}
