import { useEffect, useState } from 'react'
import {
  dejarDeCompartir,
  empezarACompartir,
  enviarPosicion,
  puedeCompartir,
  useCompartirEnVivo,
} from '../../../lib/enVivo'
import { Navigate, useNavigate } from 'react-router-dom'
import { MapaGoogle } from '../../../components/MapaGoogle'
import { TrazadoRuta } from '../../../components/TrazadoRuta'
import { BotonTexto, Dialogo } from '../../../components/ui'
import { formatearHms, formatearRitmo, Unidades } from '../../../lib/geo'
import { estaActiva, ritmoPromedio } from '../../../lib/runTracker'
import { useApp } from '../../../store/app'
import { descartarCarrera, finalizarCarrera, tracker, useCarrera } from '../../../store/carrera'
import { caloriasEstimadas } from '../../../lib/logica'
import { claveMapas } from '../../../lib/mapas'

export function CarreraEnCurso() {
  const navigate = useNavigate()
  const e = useCarrera()
  const m = useApp((s) => s.ajustes.unidadesMetricas)
  const [descartar, setDescartar] = useState(false)
  const [vista3d, setVista3d] = useState(false)
  const sesion = useApp((s) => s.sesion)
  const vivo = useCompartirEnVivo()
  const compartiendo = vivo.sesionId !== null
  const ultimo = e.puntos.at(-1)
  useEffect(() => {
    if (compartiendo && e.fase === 'corriendo')
      void enviarPosicion(ultimo, e.distanciaKm, e.segundos)
  }, [compartiendo, e.fase, ultimo, e.distanciaKm, e.segundos])

  const voz = useApp((s) => s.ajustes.anunciosVoz)
  const actualizar = useApp((s) => s.actualizarAjustes)
  const [bloqueo, setBloqueo] = useState(false)
  const altitud = (() => {
    const a = [...e.puntos].reverse().find((p) => p.altitudM != null)?.altitudM
    return a == null ? null : Math.round(a)
  })()
  const autopausa = useApp((s) => s.ajustes.autopausa)
  const tramo = Math.floor(m ? e.distanciaKm : e.distanciaKm / 1.609)
  useEffect(() => {
    if (!voz || tramo < 1 || typeof speechSynthesis === 'undefined') return
    const u = new SpeechSynthesisUtterance(
      `${tramo} ${m ? 'kilómetros' : 'millas'}. Tiempo ${formatearHms(e.segundos)}.`
    )
    u.lang = 'es-EC'
    speechSynthesis.speak(u)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tramo, voz])
  // Autopausa: sin puntos GPS nuevos durante 15 s mientras corre, la carrera se pausa.
  const [autoPausada, setAutoPausada] = useState(false)
  useEffect(() => {
    if (e.fase === 'corriendo') setAutoPausada(false)
    if (!autopausa || e.fase !== 'corriendo') return
    const t = setTimeout(() => {
      setAutoPausada(true)
      tracker.alternarPausa()
    }, 15_000)
    return () => clearTimeout(t)
  }, [autopausa, e.fase, e.puntos.length])

  if (!estaActiva(e))
    return <Navigate to={e.fase === 'terminada' ? '/correr/resumen' : '/correr'} replace />

  const pausada = e.fase === 'pausada'
  const terminar = () => {
    void dejarDeCompartir()
    finalizarCarrera()
    navigate('/correr/resumen')
  }

  const dialogo = (
    <Dialogo
      titulo="¿Descartar la carrera?"
      abierto={descartar}
      onCerrar={() => setDescartar(false)}
      acciones={
        <>
          <BotonTexto onClick={() => setDescartar(false)}>Seguir corriendo</BotonTexto>
          <BotonTexto
            peligro
            onClick={() => {
              void dejarDeCompartir()
              descartarCarrera()
              navigate('/inicio')
            }}
          >
            Descartar
          </BotonTexto>
        </>
      }
    >
      <p>
        Se perderán el tiempo y la distancia de esta carrera. Para guardarla, toca el botón de
        detener.
      </p>
    </Dialogo>
  )
  const barra = (
    <div className="absolute top-4 inset-x-4 z-20 flex justify-between items-center gap-2">
      <button
        onClick={() => setDescartar(true)}
        aria-label="Salir de la carrera"
        className="w-10 h-10 rounded-full bg-surface/90 backdrop-blur-md flex items-center justify-center text-text-primary shadow-lg"
      >
        <span className="material-symbols-outlined">close</span>
      </button>
      {sesion && puedeCompartir() && (
        <button
          onClick={() => void (compartiendo ? dejarDeCompartir() : empezarACompartir())}
          aria-pressed={compartiendo}
          data-testid="compartir-en-vivo"
          className={`h-11 px-4 rounded-full flex items-center gap-2 font-semibold shadow-lg ${
            compartiendo
              ? 'bg-danger text-white'
              : 'bg-surface/90 backdrop-blur-md text-text-primary'
          }`}
        >
          <span className="material-symbols-outlined text-[20px]">sensors</span>
          {compartiendo
            ? vivo.visible === false
              ? 'En vivo · casa oculta'
              : 'En vivo'
            : 'Compartir en vivo'}
        </button>
      )}
      <button
        onClick={() => navigate('/correr/cronometro')}
        aria-label="Abrir cronómetro"
        className="w-10 h-10 rounded-full bg-surface/90 backdrop-blur-md flex items-center justify-center text-text-primary shadow-lg"
      >
        <span className="material-symbols-outlined">timer</span>
      </button>
    </div>
  )

  if (claveMapas())
    return (
      <div className="fixed inset-0 bg-[#0d1422] z-40" data-testid="carrera-mapa">
        <MapaGoogle
          puntos={e.puntos}
          seguir
          modo3d={vista3d}
          adelanteM={-140}
          ritmoSegKm={e.ritmoActualSegKm}
          className="absolute inset-0"
          alternativa={null}
        />
        <div className="absolute inset-x-0 top-0 h-28 bg-gradient-to-b from-black/60 to-transparent pointer-events-none" />
        {barra}
        <button
          onClick={() => setVista3d(!vista3d)}
          aria-pressed={vista3d}
          data-testid="vista-3d"
          className="absolute top-20 left-4 z-20 h-10 px-4 rounded-full bg-[#121a2a]/90 backdrop-blur-md text-white border border-white/10 shadow-lg font-semibold"
        >
          {vista3d ? '2D' : '3D'}
        </button>
        <section
          aria-label="Datos de la carrera"
          className="absolute inset-x-0 bottom-0 z-20 rounded-t-[28px] bg-[#0f1625]/95 backdrop-blur-xl border-t border-white/10 shadow-[0_-12px_40px_rgba(0,0,0,0.5)] px-6 pt-3 pb-[max(env(safe-area-inset-bottom),20px)] lg:left-1/2 lg:-translate-x-1/2 lg:max-w-lg lg:bottom-6 lg:rounded-[28px] lg:border"
        >
          <div className="mx-auto mb-3 h-1.5 w-12 rounded-full bg-white/20" />
          <div className="flex items-center justify-between mb-1 text-xs text-text-secondary">
            <span className="flex items-center gap-2 uppercase tracking-wider font-semibold text-primary-400">
              <span
                className={`w-2 h-2 rounded-full ${pausada ? 'bg-text-secondary' : 'bg-secondary animate-pulse'}`}
              />
              {pausada ? (autoPausada ? 'Autopausa' : 'En pausa') : 'En curso'}
            </span>
            {e.ultimaPrecisionM != null && <span>GPS ±{Math.round(e.ultimaPrecisionM)} m</span>}
          </div>
          <div className="flex items-end gap-6">
            <div className="flex-1">
              <div
                className="text-[56px] leading-none font-light text-white tabular-nums"
                data-testid="distancia"
              >
                {Unidades.texto(e.distanciaKm, m)}
              </div>
              <div className="text-xs uppercase tracking-[0.2em] text-text-secondary mt-1">
                {m ? 'Kilómetros' : 'Millas'}
              </div>
            </div>
            <div className="text-right">
              <div className="text-2xl font-light text-white tabular-nums" data-testid="tiempo">
                {formatearHms(e.segundos)}
              </div>
              <div className="text-[11px] uppercase tracking-wider text-text-secondary">Tiempo</div>
            </div>
            <div className="text-right">
              <div className="text-2xl font-light text-white tabular-nums" data-testid="ritmo">
                {formatearRitmo(Unidades.ritmo(e.ritmoActualSegKm, m))}
              </div>
              <div className="text-[11px] uppercase tracking-wider text-text-secondary">
                Ritmo {Unidades.etiquetaRitmo(m)}
              </div>
            </div>
          </div>
          <div className="mt-4 grid grid-cols-3 gap-3 text-center text-sm text-text-secondary border-t border-white/10 pt-3">
            <div>
              <span className="block text-white">
                {formatearRitmo(Unidades.ritmo(ritmoPromedio(e), m))}
              </span>
              Promedio
            </div>
            <div>
              <span className="block text-white">{Math.round(e.desnivelM)} m</span>Desnivel
            </div>
            <div>
              <span className="block text-white">{caloriasEstimadas(e.distanciaKm)}</span>kcal est.
            </div>
          </div>
          <div className="mt-4 flex gap-3">
            <button
              onClick={() => tracker.alternarPausa()}
              aria-label={pausada ? 'Reanudar' : 'Pausar'}
              className="flex-1 h-14 rounded-2xl bg-white/10 text-white flex items-center justify-center gap-2 font-semibold"
            >
              <span className="material-symbols-outlined">{pausada ? 'play_arrow' : 'pause'}</span>
              {pausada ? 'Reanudar' : 'Pausar'}
            </button>
            <button
              onClick={terminar}
              aria-label="Terminar carrera"
              className="flex-1 h-14 rounded-2xl bg-[#FF6A13] text-[#0b1220] flex items-center justify-center gap-2 font-bold"
            >
              <span className="material-symbols-outlined">stop</span>Terminar
            </button>
          </div>
        </section>
        {dialogo}
      </div>
    )

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <div className="relative flex-1 bg-surface-container-low">
        <div className="absolute inset-0 bg-gradient-to-b from-surface-container-low via-surface to-surface"></div>

        {barra}

        <div className="relative z-10 flex flex-col gap-3 px-4 pt-20 pb-6 max-w-md mx-auto w-full">
          <div className="flex items-center justify-between rounded-2xl bg-surface-container/70 border border-border-subtle px-4 py-3">
            <div>
              <span className="font-label-lg text-label-lg text-primary uppercase tracking-wider flex items-center gap-2">
                <span
                  className={`w-2 h-2 rounded-full ${pausada ? 'bg-text-secondary' : 'bg-secondary animate-pulse'}`}
                ></span>
                {pausada ? (autoPausada ? 'Autopausa' : 'En pausa') : 'En curso'}
              </span>
              {altitud != null && (
                <span className="text-body-sm text-text-secondary" data-testid="altitud">
                  {altitud.toLocaleString('es-EC')} msnm
                </span>
              )}
            </div>
            {e.ultimaPrecisionM != null && (
              <span className="text-label-md text-secondary flex items-center gap-1">
                <span className="material-symbols-outlined text-[18px]">satellite_alt</span>
                GPS ±{Math.round(e.ultimaPrecisionM)} m
              </span>
            )}
          </div>

          <div className="rounded-3xl bg-surface-container/60 border border-border-subtle p-6 text-center">
            <div className="text-label-md text-text-secondary uppercase tracking-widest mb-1">
              Distancia recorrida
            </div>
            <div
              className="font-metric-huge text-metric-huge text-primary leading-none"
              data-testid="distancia"
            >
              {Unidades.texto(e.distanciaKm, m)}
            </div>
            <div className="font-headline-sm text-headline-sm text-text-secondary uppercase tracking-wider">
              {m ? 'Kilómetros' : 'Millas'}
            </div>
            <span className="inline-flex items-center gap-1 mt-2 px-3 py-1 rounded-full bg-success/15 text-success text-label-md">
              <span className="material-symbols-outlined text-[16px]">trending_up</span>+
              {Math.round(e.desnivelM)} m desnivel positivo
            </span>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Metrica
              titulo="Ritmo actual"
              detalle={`min ${Unidades.etiquetaRitmo(m)}`}
              testid="ritmo"
            >
              {formatearRitmo(Unidades.ritmo(e.ritmoActualSegKm, m))}
            </Metrica>
            <Metrica titulo="Tiempo" detalle="transcurrido" testid="tiempo">
              {formatearHms(e.segundos)}
            </Metrica>
            {/* Sin sensor de pulso conectado: el bloque queda oculto hasta tener una fuente real. */}
            <div hidden data-testid="pulsaciones" aria-hidden="true">
              <Metrica titulo="Pulsaciones" detalle="bpm">
                --
              </Metrica>
            </div>
            <Metrica titulo="Ritmo promedio" detalle={`min ${Unidades.etiquetaRitmo(m)}`}>
              {formatearRitmo(Unidades.ritmo(ritmoPromedio(e), m))}
            </Metrica>
            <Metrica titulo="Calorías" detalle="kcal estimadas">
              {caloriasEstimadas(e.distanciaKm)}
            </Metrica>
          </div>

          <div className="w-full h-44 rounded-2xl bg-surface-container/40 border border-border-subtle p-2">
            <MapaGoogle
              puntos={e.puntos}
              seguir
              className="w-full h-full"
              alternativa={<TrazadoRuta puntos={e.puntos} className="w-full h-full" />}
            />
          </div>

          <div className="flex justify-between">
            <button
              onClick={() => setBloqueo(true)}
              className="h-10 px-4 rounded-full bg-surface-container text-text-secondary flex items-center gap-2 text-label-md"
            >
              <span className="material-symbols-outlined text-[18px]">lock</span>Bloquear toque
            </button>
            <button
              onClick={() => void actualizar({ anunciosVoz: !voz })}
              aria-pressed={voz}
              className={`h-10 px-4 rounded-full flex items-center gap-2 text-label-md ${voz ? 'bg-primary-600/15 text-primary' : 'bg-surface-container text-text-secondary'}`}
            >
              <span className="material-symbols-outlined text-[18px]">
                {voz ? 'volume_up' : 'volume_off'}
              </span>
              Voz cada 1 {m ? 'km' : 'mi'}
            </button>
          </div>

          <div className="grid grid-cols-[1fr_1.4fr] gap-3">
            <div className="text-center">
              <button
                onClick={terminar}
                aria-label="Terminar carrera"
                className="w-full h-16 rounded-2xl bg-danger/15 border border-danger/40 text-danger flex items-center justify-center gap-2 font-semibold"
              >
                <span className="material-symbols-outlined">stop_circle</span>Terminar
              </button>
            </div>
            <div className="text-center">
              <button
                onClick={() => tracker.alternarPausa()}
                aria-label={pausada ? 'Reanudar' : 'Pausar'}
                className="w-full h-16 rounded-2xl bg-primary-600 text-on-primary flex items-center justify-center gap-2 text-title-md font-bold shadow-lg"
              >
                <span className="material-symbols-outlined text-[28px]">
                  {pausada ? 'play_arrow' : 'pause'}
                </span>
                {pausada ? 'Reanudar' : 'Pausar'}
              </button>
              {autopausa && (
                <span className="text-body-sm text-text-secondary">Auto-pausa activa</span>
              )}
            </div>
          </div>
        </div>
      </div>

      {bloqueo && (
        <div
          className="fixed inset-0 z-50 bg-black/40 flex items-end justify-center pb-16"
          data-testid="bloqueo-toque"
        >
          <button
            onClick={() => setBloqueo(false)}
            className="h-12 px-6 rounded-full bg-surface-container text-text-primary flex items-center gap-2"
          >
            <span className="material-symbols-outlined">lock_open</span>Desbloquear
          </button>
        </div>
      )}
      {dialogo}
    </div>
  )
}

function Metrica({
  titulo,
  detalle,
  testid,
  children,
}: {
  titulo: string
  detalle: string
  testid?: string
  children: React.ReactNode
}) {
  return (
    <div className="rounded-2xl bg-surface-container/60 border border-border-subtle p-4">
      <div className="text-label-md text-text-secondary uppercase">{titulo}</div>
      <div
        className="font-headline-lg text-headline-lg text-text-primary leading-tight tabular-nums"
        data-testid={testid}
      >
        {children}
      </div>
      <div className="text-body-sm text-text-secondary">{detalle}</div>
    </div>
  )
}
