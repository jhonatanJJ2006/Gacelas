import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Interruptor } from '../../components/ui'
import { cambioFase, tick } from '../../lib/avisos'
import { formatearMs, formatearRitmo, Unidades } from '../../lib/geo'
import {
  duracionTotalSeg,
  estadoIntervalo,
  intervaloMetronomoMs,
  type ConfigIntervalos,
} from '../../lib/intervalos'
import { estaActiva, ritmoPromedio } from '../../lib/runTracker'
import { useApp } from '../../store/app'
import { empezarSeguimiento, finalizarCarrera, tracker, useCarrera } from '../../store/carrera'

type Modo = 'libre' | 'fartlek' | 'pacer'
type Pestana = 'splits' | 'intervals' | 'metronome'

const MODOS: { id: Modo; etiqueta: string; icono: string }[] = [
  { id: 'libre', etiqueta: 'Modo Libre', icono: 'directions_run' },
  { id: 'fartlek', etiqueta: 'Fartlek', icono: 'timer_3' },
  { id: 'pacer', etiqueta: 'Ritmo Pacer', icono: 'speed' },
]

function Tarjeta({
  icono,
  color,
  titulo,
  valor,
  unidad,
}: {
  icono: string
  color: string
  titulo: string
  valor: string
  unidad: string
}) {
  return (
    <div className="bg-surface-container p-space-md rounded-xl flex items-center gap-space-sm shadow-sm">
      <div
        className={`w-10 h-10 rounded-xl bg-surface-container-high flex items-center justify-center shrink-0 ${color}`}
      >
        <span className="material-symbols-outlined text-[22px]">{icono}</span>
      </div>
      <div className="min-w-0 flex flex-col">
        <span className="text-label-md font-label-md text-text-secondary">{titulo}</span>
        <span className="text-headline-sm font-headline-sm text-on-surface tabular-nums truncate">
          {valor} <span className="text-label-lg font-label-lg text-text-secondary">{unidad}</span>
        </span>
      </div>
    </div>
  )
}

function Paso({
  etiqueta,
  valor,
  onMenos,
  onMas,
}: {
  etiqueta: string
  valor: string
  onMenos: () => void
  onMas: () => void
}) {
  return (
    <div className="flex items-center justify-between bg-surface-container-low rounded-xl px-space-sm py-2">
      <span className="text-body-md font-body-md text-on-surface">{etiqueta}</span>
      <div className="flex items-center gap-3">
        <button
          type="button"
          aria-label={`Menos ${etiqueta}`}
          onClick={onMenos}
          className="w-9 h-9 rounded-full bg-surface-container-high text-on-surface flex items-center justify-center"
        >
          <span className="material-symbols-outlined text-[18px]">remove</span>
        </button>
        <span className="w-14 text-center tabular-nums text-label-lg font-label-lg text-on-surface">
          {valor}
        </span>
        <button
          type="button"
          aria-label={`Más ${etiqueta}`}
          onClick={onMas}
          className="w-9 h-9 rounded-full bg-surface-container-high text-on-surface flex items-center justify-center"
        >
          <span className="material-symbols-outlined text-[18px]">add</span>
        </button>
      </div>
    </div>
  )
}

/** Entrenamiento por intervalos con su propio reloj y avisos de cambio de fase. */
export function PanelIntervalos() {
  const config = useApp((s) => s.ajustes.intervalos)
  const actualizar = useApp((s) => s.actualizarAjustes)
  const [transcurrido, setTranscurrido] = useState<number | null>(null)
  const faseAnterior = useRef<string | null>(null)
  const estado = transcurrido == null ? null : estadoIntervalo(config, transcurrido)

  const cambiar = (c: Partial<ConfigIntervalos>) =>
    void actualizar((a) => ({ intervalos: { ...a.intervalos, ...c } }))

  useEffect(() => {
    if (transcurrido == null) return
    const id = setInterval(() => setTranscurrido((t) => (t == null ? t : t + 1)), 1000)
    return () => clearInterval(id)
  }, [transcurrido == null]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (!estado) {
      faseAnterior.current = null
      return
    }
    const clave = `${estado.fase}-${estado.ronda}`
    if (faseAnterior.current !== null && faseAnterior.current !== clave)
      cambioFase(config.sonido, config.vibracion)
    faseAnterior.current = clave
    if (estado.fase === 'terminado') setTranscurrido(null)
  }, [estado?.fase, estado?.ronda]) // eslint-disable-line react-hooks/exhaustive-deps

  const iniciar = () => {
    cambioFase(config.sonido, config.vibracion)
    faseAnterior.current = null
    setTranscurrido(0)
  }

  return (
    <div
      className="px-margin mt-space-sm flex flex-col gap-space-xs"
      data-testid="panel-intervalos"
    >
      {estado ? (
        <div
          className={`rounded-2xl p-space-md text-center ${estado.fase === 'trabajo' ? 'bg-primary-600/20' : 'bg-secondary-600/20'}`}
        >
          <div className="text-label-lg font-label-lg uppercase tracking-wider text-on-surface">
            {estado.fase === 'trabajo'
              ? 'Trabajo'
              : estado.fase === 'descanso'
                ? 'Descanso'
                : 'Terminado'}
          </div>
          <div
            className="font-metric-huge text-metric-huge text-on-surface tabular-nums"
            data-testid="intervalo-restante"
          >
            {formatearMs(estado.restanteSeg)}
          </div>
          <div className="text-body-md text-text-secondary">
            Ronda {estado.ronda} de {config.rondas}
          </div>
        </div>
      ) : (
        <>
          <Paso
            etiqueta="Trabajo"
            valor={formatearMs(config.trabajoSeg)}
            onMenos={() => cambiar({ trabajoSeg: Math.max(10, config.trabajoSeg - 10) })}
            onMas={() => cambiar({ trabajoSeg: Math.min(1800, config.trabajoSeg + 10) })}
          />
          <Paso
            etiqueta="Descanso"
            valor={formatearMs(config.descansoSeg)}
            onMenos={() => cambiar({ descansoSeg: Math.max(0, config.descansoSeg - 10) })}
            onMas={() => cambiar({ descansoSeg: Math.min(900, config.descansoSeg + 10) })}
          />
          <Paso
            etiqueta="Rondas"
            valor={`${config.rondas}`}
            onMenos={() => cambiar({ rondas: Math.max(1, config.rondas - 1) })}
            onMas={() => cambiar({ rondas: Math.min(50, config.rondas + 1) })}
          />
          <div className="flex items-center justify-between bg-surface-container-low rounded-xl px-space-sm py-2">
            <span className="text-body-md text-on-surface">Vibración</span>
            <Interruptor
              etiqueta="Vibración"
              activo={config.vibracion}
              onCambiar={(v) => cambiar({ vibracion: v })}
            />
          </div>
          <div className="flex items-center justify-between bg-surface-container-low rounded-xl px-space-sm py-2">
            <span className="text-body-md text-on-surface">Sonido</span>
            <Interruptor
              etiqueta="Sonido"
              activo={config.sonido}
              onCambiar={(v) => cambiar({ sonido: v })}
            />
          </div>
          <p className="text-body-sm text-text-secondary text-center">
            Duración total: {formatearMs(duracionTotalSeg(config))}
          </p>
        </>
      )}
      <button
        type="button"
        onClick={estado ? () => setTranscurrido(null) : iniciar}
        className={`h-12 rounded-xl font-bold ${estado ? 'bg-danger/20 text-red-300' : 'bg-primary-600 text-bg-primary'}`}
      >
        {estado ? 'Detener intervalos' : 'Iniciar intervalos'}
      </button>
    </div>
  )
}

/** Metronomo con tick audible por Web Audio; el BPM se guarda en ajustes. */
export function PanelMetronomo() {
  const bpmGuardado = useApp((s) => s.ajustes.metronomoBpm)
  const actualizar = useApp((s) => s.actualizarAjustes)
  const [bpm, setBpm] = useState(bpmGuardado)
  const [sonando, setSonando] = useState(false)
  const [pulso, setPulso] = useState(0)

  useEffect(() => setBpm(bpmGuardado), [bpmGuardado])

  useEffect(() => {
    if (!sonando) return
    let n = 0
    const id = setInterval(() => {
      tick(n % 4 === 0)
      n++
      setPulso(n)
    }, intervaloMetronomoMs(bpm))
    return () => clearInterval(id)
  }, [sonando, bpm])

  return (
    <div
      className="px-margin mt-space-sm flex flex-col gap-space-sm items-center"
      data-testid="panel-metronomo"
    >
      <div
        className={`w-28 h-28 rounded-full flex flex-col items-center justify-center transition-colors ${sonando && pulso % 2 ? 'bg-primary-600/40' : 'bg-surface-container'}`}
      >
        <span
          className="font-metric-huge text-[40px] leading-none text-on-surface tabular-nums"
          data-testid="bpm"
        >
          {bpm}
        </span>
        <span className="text-label-md text-text-secondary">BPM</span>
      </div>
      <input
        type="range"
        aria-label="Pasos por minuto"
        min={40}
        max={240}
        value={bpm}
        onChange={(e) => setBpm(Number(e.target.value))}
        onPointerUp={() => void actualizar({ metronomoBpm: bpm })}
        onKeyUp={() => void actualizar({ metronomoBpm: bpm })}
        className="h-8 w-full accent-primary-600"
      />
      <div className="flex gap-2">
        {[160, 170, 180].map((v) => (
          <button
            key={v}
            type="button"
            onClick={() => {
              setBpm(v)
              void actualizar({ metronomoBpm: v })
            }}
            className="px-3 py-1 rounded-full bg-surface-container text-label-md text-text-secondary"
          >
            {v}
          </button>
        ))}
      </div>
      <button
        type="button"
        onClick={() => setSonando((s) => !s)}
        className={`w-full h-12 rounded-xl font-bold ${sonando ? 'bg-danger/20 text-red-300' : 'bg-primary-600 text-bg-primary'}`}
      >
        {sonando ? 'Detener metrónomo' : 'Iniciar metrónomo'}
      </button>
    </div>
  )
}

export function Cronometro() {
  const navigate = useNavigate()
  const e = useCarrera()
  const ajustes = useApp((s) => s.ajustes)
  const m = ajustes.unidadesMetricas
  const [modo, setModo] = useState<Modo>('libre')
  const [pestana, setPestana] = useState<Pestana>('splits')
  const [objetivo, setObjetivo] = useState(300)
  const [errorGps, setErrorGps] = useState<string | null>(null)
  const activa = estaActiva(e)

  const kmEnCurso = Math.floor(e.distanciaKm) + 1
  const segUltimoKm = e.parciales.filter((p) => !p.manual).reduce((s, p) => s + p.segundos, 0)
  const progreso = ((e.segundos % 3600) / 3600) * 100
  const dif = e.ritmoActualSegKm != null ? e.ritmoActualSegKm - objetivo : null

  const principal = () => {
    if (!activa) empezarSeguimiento({ precisa: ajustes.ubicacionPrecisa, onError: setErrorGps })
    else tracker.alternarPausa()
  }

  const fin = () => {
    if (!activa) return
    finalizarCarrera()
    navigate('/correr/resumen')
  }

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <header className="fixed top-0 inset-x-0 z-50 bg-surface/85 backdrop-blur-xl pt-safe">
        <div className="h-16 px-space-md flex items-center gap-space-xs">
          <button
            onClick={() => navigate(activa ? '/correr/en-curso' : '/correr')}
            aria-label="Volver"
            className="w-12 h-12 flex items-center justify-center rounded-full text-on-surface hover:text-primary-500 active:bg-surface-container transition-colors"
          >
            <span className="material-symbols-outlined text-[24px]">arrow_back</span>
          </button>
          <h1 className="text-headline-sm font-headline-sm text-on-surface tracking-tight">
            Cronómetro Profesional
          </h1>
        </div>
      </header>

      <main className="lg:w-full lg:max-w-5xl lg:mx-auto flex-1 pt-16 pb-24 overflow-auto">
        <div className="px-margin pt-space-sm pb-space-xs">
          <div className="flex items-center gap-space-xs p-1 bg-surface-container rounded-xl overflow-x-auto">
            {MODOS.map((mo) => (
              <button
                key={mo.id}
                onClick={() => {
                  setModo(mo.id)
                  if (mo.id === 'fartlek') setPestana('intervals')
                }}
                aria-pressed={modo === mo.id}
                className={`flex-1 min-w-[110px] py-2 px-space-sm rounded-lg text-label-md font-label-md flex items-center justify-center gap-1.5 transition-all ${
                  modo === mo.id
                    ? 'bg-primary-container/20 text-primary-400 shadow-sm'
                    : 'text-text-secondary hover:text-on-surface'
                }`}
              >
                <span className="material-symbols-outlined text-[16px]">{mo.icono}</span>
                <span>{mo.etiqueta}</span>
              </button>
            ))}
          </div>
        </div>

        {modo === 'pacer' && (
          <div className="px-margin my-space-xs">
            <div className="p-space-sm rounded-xl bg-surface-container-low shadow-sm flex items-center justify-between gap-space-sm">
              <div className="flex flex-col min-w-0">
                <span className="text-body-sm text-text-secondary truncate">
                  Objetivo Pacer:{' '}
                  <strong className="text-on-surface">
                    {formatearRitmo(Unidades.ritmo(objetivo, m))} {Unidades.etiquetaRitmo(m)}
                  </strong>
                </span>
                <span
                  className={`text-label-md font-label-md ${dif == null ? 'text-text-secondary' : dif <= 0 ? 'text-secondary-500' : 'text-danger'}`}
                >
                  {dif == null
                    ? 'Esperando ritmo del GPS'
                    : dif <= 0
                      ? `${-dif}s más rápido que el objetivo`
                      : `${dif}s más lento que el objetivo`}
                </span>
              </div>
              <div className="flex gap-1">
                <button
                  aria-label="Objetivo más rápido"
                  onClick={() => setObjetivo((o) => Math.max(150, o - 5))}
                  className="w-8 h-8 rounded-full bg-surface-container-high text-on-surface"
                >
                  -
                </button>
                <button
                  aria-label="Objetivo más lento"
                  onClick={() => setObjetivo((o) => Math.min(900, o + 5))}
                  className="w-8 h-8 rounded-full bg-surface-container-high text-on-surface"
                >
                  +
                </button>
              </div>
            </div>
          </div>
        )}

        <div className="relative px-margin my-space-sm flex flex-col items-center justify-center">
          <div className="relative w-72 h-72 flex items-center justify-center">
            <svg className="w-full h-full -rotate-90 transform" viewBox="0 0 240 240">
              <circle
                className="text-surface-container-high"
                cx="120"
                cy="120"
                fill="none"
                r="104"
                stroke="currentColor"
                strokeWidth="6"
              />
              <circle
                className="text-primary-500 transition-all duration-300"
                cx="120"
                cy="120"
                fill="none"
                r="104"
                stroke="currentColor"
                strokeDasharray="653.45"
                strokeDashoffset={653.45 - (653.45 * progreso) / 100}
                strokeLinecap="round"
                strokeWidth="7"
              />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center text-center px-4">
              <div className="flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-surface-container-highest mb-1 shadow-sm">
                <span
                  className={`w-2 h-2 rounded-full ${e.fase === 'corriendo' ? 'bg-secondary-500 animate-pulse' : 'bg-text-secondary'}`}
                ></span>
                <span className="text-label-md font-label-md text-on-surface uppercase tracking-wider">
                  {e.fase === 'corriendo'
                    ? 'En marcha'
                    : e.fase === 'pausada'
                      ? 'En pausa'
                      : 'Listo'}
                </span>
              </div>
              <div
                className="font-metric-huge text-metric-huge text-on-surface tabular-nums"
                data-testid="crono-tiempo"
              >
                {formatearMs(e.segundos)}
              </div>
              <div className="mt-1 flex items-center gap-1 text-label-md text-text-secondary bg-surface-container/60 px-3 py-1 rounded-full">
                <span className="material-symbols-outlined text-[15px] text-primary-500">flag</span>
                <span>
                  Km {kmEnCurso} en curso:{' '}
                  <strong className="text-on-surface">
                    {formatearMs(Math.max(0, e.segundos - segUltimoKm))}
                  </strong>
                </span>
              </div>
            </div>
          </div>
        </div>

        {errorGps && (
          <p role="alert" className="px-margin text-sm text-danger text-center">
            {errorGps}
          </p>
        )}

        <div className="px-margin my-space-xs grid grid-cols-2 gap-space-sm">
          <Tarjeta
            icono="distance"
            color="text-primary-500"
            titulo="Distancia Total"
            valor={Unidades.texto(e.distanciaKm, m)}
            unidad={Unidades.etiquetaDistancia(m)}
          />
          <Tarjeta
            icono="speed"
            color="text-secondary-500"
            titulo="Ritmo Actual"
            valor={formatearRitmo(Unidades.ritmo(e.ritmoActualSegKm, m))}
            unidad={Unidades.etiquetaRitmo(m)}
          />
          <Tarjeta
            icono="trending_up"
            color="text-tertiary"
            titulo="Ritmo Promedio"
            valor={formatearRitmo(Unidades.ritmo(ritmoPromedio(e), m))}
            unidad={Unidades.etiquetaRitmo(m)}
          />
          <Tarjeta
            icono="landscape"
            color="text-danger"
            titulo="Desnivel"
            valor={`${Math.round(e.desnivelM)}`}
            unidad="m"
          />
        </div>

        <div className="px-margin my-space-md flex items-center justify-between gap-space-md">
          <button
            onClick={() => tracker.vuelta()}
            disabled={!activa}
            className="w-16 h-16 rounded-full bg-surface-container hover:bg-surface-container-high active:scale-95 text-on-surface flex flex-col items-center justify-center shadow-md disabled:opacity-40"
          >
            <span className="material-symbols-outlined text-[22px] text-primary-400">flag</span>
            <span className="text-label-md font-label-md">Vuelta</span>
          </button>
          <button
            onClick={principal}
            aria-label={!activa ? 'Iniciar' : e.fase === 'pausada' ? 'Reanudar' : 'Pausar'}
            className="w-20 h-20 rounded-full bg-primary-600 hover:bg-primary-700 active:scale-95 text-bg-primary flex items-center justify-center shadow-lg"
          >
            <span className="material-symbols-outlined text-[36px] font-bold">
              {e.fase === 'corriendo' ? 'pause' : 'play_arrow'}
            </span>
          </button>
          <button
            onClick={fin}
            disabled={!activa}
            className="w-16 h-16 rounded-full bg-surface-container hover:bg-surface-container-high active:scale-95 text-danger flex flex-col items-center justify-center shadow-md disabled:opacity-40"
          >
            <span className="material-symbols-outlined text-[22px]">stop</span>
            <span className="text-label-md font-label-md">Fin</span>
          </button>
        </div>

        <div className="px-margin mt-space-sm">
          <div className="flex items-center bg-surface-container-low p-1 rounded-xl" role="tablist">
            {(
              [
                ['splits', 'list_alt', 'Splits'],
                ['intervals', 'repeat', 'Intervalos'],
                ['metronome', 'graphic_eq', 'Metrónomo'],
              ] as const
            ).map(([id, icono, etiqueta]) => (
              <button
                key={id}
                role="tab"
                aria-selected={pestana === id}
                onClick={() => setPestana(id)}
                className={`flex-1 min-h-11 py-1.5 rounded-lg text-label-md font-label-md flex items-center justify-center gap-1 ${
                  pestana === id
                    ? 'bg-surface-container text-on-surface font-semibold shadow-sm'
                    : 'text-text-secondary'
                }`}
              >
                <span className="material-symbols-outlined text-[16px]">{icono}</span>
                <span>{etiqueta}</span>
              </button>
            ))}
          </div>
        </div>

        {pestana === 'splits' && (
          <div className="px-margin mt-space-sm flex flex-col gap-space-xs">
            {e.parciales.length === 0 ? (
              <p className="text-body-md text-text-secondary text-center py-4">
                Los parciales aparecen cada kilómetro o al tocar Vuelta.
              </p>
            ) : (
              [...e.parciales].reverse().map((p) => (
                <div
                  key={`${p.manual ? 'v' : 'k'}${p.numero}`}
                  className="p-space-sm rounded-xl flex items-center justify-between bg-surface-container-low shadow-sm"
                >
                  <div className="flex items-center gap-2">
                    <div className="w-6 h-6 rounded-full text-label-md bg-surface-container-high text-on-surface flex items-center justify-center">
                      {p.numero}
                    </div>
                    <div>
                      <div className="text-body-md text-on-surface font-medium">
                        {Unidades.texto(p.distanciaKm, m)} {Unidades.etiquetaDistancia(m)}
                      </div>
                      <div className="text-body-sm text-text-secondary">
                        {p.manual ? `Vuelta ${p.numero}` : `Kilómetro ${p.numero}`}
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-6 text-right">
                    <span className="text-label-lg text-on-surface tabular-nums">
                      {formatearRitmo(
                        Unidades.ritmo(
                          p.distanciaKm > 0.01 ? Math.floor(p.segundos / p.distanciaKm) : null,
                          m
                        )
                      )}
                    </span>
                    <span className="text-label-lg text-text-secondary tabular-nums">
                      {formatearMs(p.segundos)}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        )}
        {pestana === 'intervals' && <PanelIntervalos />}
        {pestana === 'metronome' && <PanelMetronomo />}
      </main>
    </div>
  )
}
