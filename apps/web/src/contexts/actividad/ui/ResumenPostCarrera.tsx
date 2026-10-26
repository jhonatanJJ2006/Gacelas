import { useRef, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { CompartirDialogo } from '../../../components/CompartirDialogo'
import { MapaGoogle } from '../../../components/MapaGoogle'
import { CelebracionRacha } from '../../../components/Racha'
import { TrazadoRuta } from '../../../components/TrazadoRuta'
import { recortarExtremos, SITIO, type DatosCompartir } from '../../../lib/compartir'
import { nuevoRecord } from '../../../lib/estadisticas'
import { formatearMs, formatearRitmo, Unidades } from '../../../lib/geo'
import { caloriasEstimadas } from '../../../lib/logica'
import { rachaTrasCarrera } from '../../../lib/rachas'
import { ritmoPromedio, tituloPorHora } from '../../../lib/runTracker'
import { useApp } from '../../../store/app'
import { descartarCarrera, tracker, useCarrera } from '../../../store/carrera'
import { desnivelPositivo, ETIQUETAS_RUTA, perfilElevacion, trazoParaRuta } from '../../rutas/api'

export function ResumenPostCarrera() {
  const navigate = useNavigate()
  const e = useCarrera()
  const ajustes = useApp((s) => s.ajustes)
  const guardarCarrera = useApp((s) => s.guardarCarrera)
  const carreras = useApp((s) => s.carreras)
  const [racha] = useState(() =>
    rachaTrasCarrera(carreras, { inicioMs: Date.now() }, ajustes.metaSemanal)
  )
  const m = ajustes.unidadesMetricas
  const [compartiendo, setCompartiendo] = useState(false)
  const [datosCompartir, setDatosCompartir] = useState<DatosCompartir | null>(null)
  const abrirCompartir = () => {
    setDatosCompartir({
      tipo: 'carrera',
      titulo: titulo.trim() || tituloPorHora(e.inicioMs),
      // Privacidad: sin los primeros/ultimos 250 m para no revelar la casa.
      puntos: recortarExtremos(e.puntos),
      distanciaKm: e.distanciaKm,
      segundos: e.segundos,
      ritmoSegKm: ritmoPromedio(e),
      desnivelM: e.desnivelM,
      rachaDias: racha.despues.diaria,
      enlace: SITIO,
    })
    setCompartiendo(true)
  }
  const [titulo, setTitulo] = useState('')
  const [nota, setNota] = useState('')
  const [publica, setPublica] = useState(ajustes.visibilidad !== 'privado')
  const [guardando, setGuardando] = useState(false)
  const sesion = useApp((s) => s.sesion)
  const rutasSociales = useApp((s) => s.rutasSociales)
  const publicarRuta = useApp((s) => s.publicarRuta)
  const [comoRuta, setComoRuta] = useState(false)
  const [dificultad, setDificultad] = useState<'Fácil' | 'Moderado' | 'Difícil'>('Moderado')
  const [etiquetas, setEtiquetas] = useState<string[]>([])
  const [errorRuta, setErrorRuta] = useState<string | null>(null)
  const puedeSerRuta = rutasSociales && !!sesion && e.puntos.length >= 2 && e.distanciaKm >= 0.3
  const saliendo = useRef(false)
  const [record] = useState(() =>
    e.fase === 'terminada'
      ? nuevoRecord(carreras, {
          distanciaKm: e.distanciaKm,
          segundos: e.segundos,
          parciales: e.parciales,
        })
      : null
  )
  const alturas = e.puntos.map((p) => p.altitudM).filter((a): a is number => a != null)

  // Tras guardar se va a Perfil; no se redirige a Inicio al reiniciar la carrera.
  if (saliendo.current) return null
  if (e.fase !== 'terminada') return <Navigate to="/inicio" replace />

  const guardar = async () => {
    setGuardando(true)
    const carrera = tracker.aCarreraGuardada(titulo, nota, publica)
    await guardarCarrera(carrera)
    if (comoRuta && puedeSerRuta) {
      try {
        const trazo = trazoParaRuta(e.puntos)
        const perfil = await perfilElevacion(trazo).catch(() => [])
        await publicarRuta({
          perfilElevacion: perfil,
          nombre: (titulo.trim() || tituloPorHora(e.inicioMs)).slice(0, 80),
          descripcion: nota.trim(),
          dificultad,
          etiquetas,
          puntos: trazo,
          elevacionM: perfil.length > 1 ? desnivelPositivo(perfil) : e.desnivelM,
          actividadId: carrera.id,
        })
      } catch (err) {
        setErrorRuta(err instanceof Error ? err.message : 'No se pudo publicar la ruta')
        setGuardando(false)
        return
      }
    }
    saliendo.current = true
    navigate('/perfil', { replace: true })
    tracker.reiniciar()
  }

  const descartar = () => {
    descartarCarrera()
    navigate('/inicio', { replace: true })
  }

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <header className="px-margin py-4">
        <button
          onClick={descartar}
          aria-label="Cerrar sin guardar"
          className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors"
        >
          <span className="material-symbols-outlined">close</span>
        </button>
      </header>

      <main className="lg:w-full lg:max-w-5xl lg:mx-auto flex-1 px-6 pb-8 overflow-auto">
        <section className="rounded-3xl bg-gradient-to-b from-primary-600/20 to-surface-container border border-border-subtle p-6 mb-4 text-center">
          <div className="w-20 h-20 rounded-full border-4 border-primary bg-surface-container flex items-center justify-center mx-auto mb-3">
            <span className="material-symbols-outlined text-[40px] text-primary">
              {record ? 'military_tech' : 'check_circle'}
            </span>
          </div>
          <h1 className="font-headline-lg text-headline-lg text-text-primary">
            ¡Excelente zancada, gacela!
          </h1>
          <p className="font-body-md text-body-md text-text-secondary">Carrera completada</p>
          {record && (
            <p className="mt-1 text-primary font-semibold" data-testid="nuevo-record">
              Nuevo récord personal en {record.etiqueta}
            </p>
          )}
          <p className="mt-3 inline-flex items-center gap-1 px-3 py-1 rounded-full bg-surface-container text-body-sm text-text-secondary">
            <span className="material-symbols-outlined text-[16px]">location_on</span>
            {new Date(e.inicioMs || Date.now()).toLocaleString('es-EC', {
              weekday: 'short',
              hour: '2-digit',
              minute: '2-digit',
            })}{' '}
            • Loja, Ecuador
          </p>
        </section>

        <CelebracionRacha antes={racha.antes} despues={racha.despues} />

        <section className="rounded-3xl bg-surface-container border border-border-subtle overflow-hidden mb-4">
          <div className="h-48 relative">
            <MapaGoogle
              puntos={e.puntos}
              className="w-full h-full"
              alternativa={<TrazadoRuta puntos={e.puntos} className="w-full h-full" />}
            />
            {alturas.length > 1 && (
              <span className="absolute bottom-2 left-2 px-2 py-1 rounded-lg bg-black/60 text-white text-label-sm">
                {Math.round(Math.min(...alturas))} m - {Math.round(Math.max(...alturas))} m
              </span>
            )}
          </div>
        </section>

        <section className="rounded-3xl bg-surface-container border border-border-subtle p-5 mb-3">
          <div className="text-label-md text-text-secondary uppercase tracking-wider">
            Distancia total
          </div>
          <div className="flex items-baseline gap-2">
            <span
              className="font-metric-huge text-[48px] text-text-primary leading-none"
              data-testid="resumen-distancia"
            >
              {Unidades.texto(e.distanciaKm, m)}
            </span>
            <span className="text-primary font-semibold">{m ? 'Kilómetros' : 'Millas'}</span>
          </div>
        </section>

        <div className="grid grid-cols-2 gap-3 mb-6">
          {(
            [
              [
                'Tiempo',
                formatearMs(e.segundos),
                'Tiempo en movimiento',
                'resumen-tiempo',
                'text-text-primary',
              ],
              [
                'Ritmo medio',
                formatearRitmo(Unidades.ritmo(ritmoPromedio(e), m)),
                Unidades.etiquetaRitmo(m),
                undefined,
                'text-primary',
              ],
              [
                'Desnivel (+)',
                `+${Math.round(e.desnivelM)} m`,
                'Subida acumulada',
                undefined,
                'text-success',
              ],
              [
                'Calorías',
                `${caloriasEstimadas(e.distanciaKm)}`,
                'kcal estimadas',
                undefined,
                'text-text-primary',
              ],
            ] as const
          ).map(([t, v, d, id, c]) => (
            <div
              key={t}
              className="rounded-2xl bg-surface-container border border-border-subtle p-4"
            >
              <div className="text-label-md text-text-secondary uppercase">{t}</div>
              <div className={`font-headline-lg text-headline-lg ${c}`} data-testid={id}>
                {v}
              </div>
              <div className="text-body-sm text-text-secondary">{d}</div>
            </div>
          ))}
          <div className="col-span-2 rounded-2xl bg-surface-container border border-border-subtle p-4 flex justify-between">
            <span className="text-text-secondary">Parciales</span>
            <span className="text-text-primary font-semibold">{e.parciales.length}</span>
          </div>
        </div>

        <div className="space-y-3">
          <input
            type="text"
            aria-label="Título de la carrera"
            value={titulo}
            onChange={(ev) => setTitulo(ev.target.value)}
            placeholder={tituloPorHora(e.inicioMs)}
            className="w-full h-12 px-4 bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary focus:outline-none focus:ring-2 focus:ring-primary-600"
          />
          <textarea
            aria-label="Notas"
            value={nota}
            onChange={(ev) => setNota(ev.target.value)}
            placeholder="¿Cómo te fue? Comparte tus pensamientos..."
            rows={3}
            className="w-full px-4 py-3 bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary focus:outline-none focus:ring-2 focus:ring-primary-600 resize-none"
          />
          <label className="flex items-center justify-between px-4 py-3 bg-surface-container border border-border-subtle rounded-xl cursor-pointer">
            <span className="font-body-md text-body-md text-text-primary">Hacer pública</span>
            <input
              type="checkbox"
              checked={publica}
              onChange={(ev) => setPublica(ev.target.checked)}
              className="w-5 h-5 accent-primary-600"
            />
          </label>
          {puedeSerRuta && (
            <div className="bg-surface-container border border-border-subtle rounded-xl">
              <label className="flex items-center justify-between px-4 py-3 cursor-pointer">
                <span className="font-body-md text-body-md text-text-primary">
                  Publicar también como ruta
                </span>
                <input
                  type="checkbox"
                  checked={comoRuta}
                  data-testid="publicar-ruta"
                  onChange={(ev) => setComoRuta(ev.target.checked)}
                  className="w-5 h-5 accent-primary-600"
                />
              </label>
              {comoRuta && (
                <div className="px-4 pb-4 space-y-3">
                  <p className="text-xs text-text-secondary">
                    Se publica el trazo completo (simplificado). Si sales de casa, mejor no la
                    publiques.
                  </p>
                  <div role="radiogroup" aria-label="Dificultad de la ruta" className="flex gap-2">
                    {(['Fácil', 'Moderado', 'Difícil'] as const).map((d) => (
                      <button
                        key={d}
                        type="button"
                        role="radio"
                        aria-checked={dificultad === d}
                        onClick={() => setDificultad(d)}
                        className={`flex-1 min-h-[40px] rounded-full text-sm font-semibold ${
                          dificultad === d
                            ? 'bg-primary-600 text-on-primary'
                            : 'bg-surface text-text-secondary'
                        }`}
                      >
                        {d}
                      </button>
                    ))}
                  </div>
                  <div className="flex flex-wrap gap-2" role="group" aria-label="Etiquetas">
                    {Object.entries(ETIQUETAS_RUTA).map(([k, v]) => (
                      <button
                        key={k}
                        type="button"
                        aria-pressed={etiquetas.includes(k)}
                        onClick={() =>
                          setEtiquetas((x) =>
                            x.includes(k) ? x.filter((y) => y !== k) : [...x, k]
                          )
                        }
                        className={`min-h-[36px] px-3 rounded-full text-sm ${
                          etiquetas.includes(k)
                            ? 'bg-secondary-600 text-on-primary'
                            : 'bg-surface text-text-secondary'
                        }`}
                      >
                        #{v}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
          {errorRuta && (
            <p role="alert" className="text-sm text-red-300">
              {errorRuta}
            </p>
          )}
        </div>

        <div className="mt-8 space-y-3">
          <button
            onClick={guardar}
            disabled={guardando}
            className="w-full h-14 bg-primary-600 hover:bg-primary-700 active:scale-[0.98] text-on-primary font-bold text-base rounded-xl flex items-center justify-center gap-2 shadow-[0_8px_20px_-4px_rgba(123,31,162,0.35)] transition-all disabled:opacity-60"
          >
            <span className="material-symbols-outlined text-[20px]">save</span>
            <span>Guardar carrera</span>
          </button>
          {e.segundos > 0 && (
            <button
              onClick={abrirCompartir}
              data-testid="compartir-carrera"
              className="w-full h-12 bg-surface-container border border-border-subtle text-text-primary font-semibold rounded-xl flex items-center justify-center gap-2"
            >
              <span className="material-symbols-outlined text-[20px]">share</span>
              Compartir en redes
            </button>
          )}
          <button
            onClick={descartar}
            className="w-full h-12 bg-transparent hover:bg-surface-container border border-border-subtle text-text-secondary font-medium text-sm rounded-xl flex items-center justify-center transition-all"
          >
            Descartar
          </button>
        </div>
      </main>
      {compartiendo && datosCompartir && (
        <CompartirDialogo datos={datosCompartir} onCerrar={() => setCompartiendo(false)} />
      )}
    </div>
  )
}
