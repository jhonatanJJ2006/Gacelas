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
import { ModalC2, ResumenModal } from '../../../components/modales'
import { formatearHms, formatearRitmo, Unidades } from '../../../lib/geo'
import { estaActiva, ritmoPromedio } from '../../../lib/runTracker'
import { useApp } from '../../../store/app'
import { descartarCarrera, finalizarCarrera, tracker, useCarrera } from '../../../store/carrera'
import { caloriasEstimadas } from '../../../lib/logica'
import { claveMapas } from '../../../lib/mapas'
import { HojaReportarIncidente } from '../../incidentes/HojaReportarIncidente'

export function CarreraEnCurso() {
  const navigate = useNavigate()
  const e = useCarrera()
  const m = useApp((s) => s.ajustes.unidadesMetricas)
  const [descartar, setDescartar] = useState(false)
  const [modalFin, setModalFin] = useState(false)
  const [modalPausa, setModalPausa] = useState(false)
  const [gpsAvisado, setGpsAvisado] = useState(false)
  const [vista3d, setVista3d] = useState(true)
  const [recentrar, setRecentrar] = useState(0)
  const [reportando, setReportando] = useState(false)
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

  const precision = e.ultimaPrecisionM ?? null
  const gpsDebil = precision != null && precision > 50 && !pausada
  const resumen = (
    <ResumenModal
      datos={[
        [`${Unidades.texto(e.distanciaKm, m)} ${Unidades.etiquetaDistancia(m)}`, 'Distancia'],
        [formatearHms(e.segundos), 'Tiempo'],
        [formatearRitmo(Unidades.ritmo(ritmoPromedio(e), m)), 'Ritmo'],
      ]}
    />
  )
  const modales = (
    <>
      <ModalC2
        abierto={modalFin}
        icono="flag"
        titulo="¿Terminar carrera?"
        texto="Se guardará tu recorrido y verás el resumen."
        primario="Terminar y guardar"
        onPrimario={() => {
          setModalFin(false)
          terminar()
        }}
        onCerrar={() => setModalFin(false)}
        secundario="Seguir corriendo"
      >
        {resumen}
      </ModalC2>
      <ModalC2
        abierto={modalPausa && pausada && !modalFin}
        icono="pause"
        titulo="Carrera en pausa"
        texto="El cronómetro está detenido. Reanuda cuando estés listo."
        primario="Reanudar"
        onPrimario={() => {
          setModalPausa(false)
          tracker.alternarPausa()
        }}
        onCerrar={() => setModalPausa(false)}
        secundario="Terminar"
        onSecundario={() => {
          setModalPausa(false)
          setModalFin(true)
        }}
      >
        {resumen}
      </ModalC2>
      <ModalC2
        abierto={gpsDebil && !gpsAvisado && !modalFin}
        icono="gps_off"
        titulo="GPS sin señal"
        texto={`La precisión es baja (±${Math.round(precision ?? 0)} m). Sal a un lugar abierto, lejos de edificios altos.`}
        primario="Entendido"
        onPrimario={() => setGpsAvisado(true)}
        onCerrar={() => setGpsAvisado(true)}
      />
    </>
  )
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

  const botonReportar = (
    <button
      onClick={() => setReportando(true)}
      data-testid="reportar-incidente"
      aria-label="Reportar incidente"
      className="absolute top-4 left-1/2 -translate-x-1/2 z-30 h-10 px-3 rounded-full bg-c2-card/90 backdrop-blur-md text-white border border-c2-linea shadow-lg flex items-center gap-1 text-sm font-semibold"
    >
      <span className="material-symbols-outlined text-[18px] text-[#F5C451]">warning</span>
      Reportar
    </button>
  )
  const hojaReportar = (
    <HojaReportarIncidente
      abierta={reportando}
      ubicacion={ultimo ? { lat: ultimo.lat, lng: ultimo.lng } : null}
      onCerrar={() => setReportando(false)}
    />
  )

  if (claveMapas()) {
    const precision = e.ultimaPrecisionM
    const gps =
      precision == null
        ? 'BUSCANDO'
        : precision <= 10
          ? 'FUERTE'
          : precision <= 25
            ? 'MEDIO'
            : 'DÉBIL'
    const parciales = e.parciales.filter((x) => !x.manual)
    const ultimos = parciales.slice(-5)
    const mejor = parciales.reduce<(typeof parciales)[number] | null>(
      (a, x) => (a == null || x.segundos < a.segundos ? x : a),
      null
    )
    const ultimo = parciales[parciales.length - 1]
    const maxSeg = Math.max(1, ...ultimos.map((x) => x.segundos))
    const minSeg = Math.min(...ultimos.map((x) => x.segundos), maxSeg)
    const ctl =
      'w-[42px] h-[42px] rounded-[6px] bg-c2-card/95 border border-c2-linea text-white flex items-center justify-center shadow-lg backdrop-blur-md'
    const chip =
      'h-8 px-4 flex items-center gap-2 bg-c2-card/95 border border-c2-linea text-white text-[12px] font-black italic uppercase tracking-wide backdrop-blur-md [clip-path:polygon(8px_0,100%_0,calc(100%-8px)_100%,0_100%)]'
    return (
      <div className="fixed inset-0 bg-c2-bg z-40 overflow-hidden" data-testid="carrera-mapa">
        <MapaGoogle
          puntos={e.puntos}
          seguir
          modo3d={vista3d}
          adelanteM={-140}
          ritmoSegKm={e.ritmoActualSegKm}
          className="absolute inset-0"
          alternativa={null}
          controles={false}
          recentrar={recentrar}
        />
        <div className="absolute inset-x-0 top-0 h-24 bg-gradient-to-b from-c2-bg/80 to-transparent pointer-events-none" />
        <div className="absolute top-[max(env(safe-area-inset-top),12px)] inset-x-3 z-20 flex gap-2">
          <span className={chip} data-testid="chip-gps">
            <span
              className={`w-2.5 h-2.5 rounded-full ${pausada ? 'bg-c2-muted' : gps === 'FUERTE' ? 'bg-c2-menta' : gps === 'MEDIO' ? 'bg-amber-400' : 'bg-c2-peligro'}`}
            />
            {pausada ? (autoPausada ? 'Autopausa' : 'En pausa') : 'En curso'} · GPS {gps}
          </span>
          <span className={chip} data-testid="chip-promedio">
            Promedio {formatearRitmo(Unidades.ritmo(ritmoPromedio(e), m))}
          </span>
        </div>
        <div
          className="absolute right-3 top-[58px] z-20 flex flex-col gap-2"
          data-testid="controles-carrera"
        >
          <button
            onClick={() => setReportando(true)}
            aria-label="Reportar incidente"
            data-testid="reportar-incidente"
            className={ctl}
          >
            <span className="material-symbols-outlined text-[20px] text-[#F5C451]">warning</span>
          </button>
          <button
            onClick={() => setRecentrar((n) => n + 1)}
            aria-label="Recentrar"
            data-testid="recentrar"
            className={ctl}
          >
            <span className="material-symbols-outlined text-[20px]">my_location</span>
          </button>
          <button
            onClick={() => setVista3d(!vista3d)}
            aria-pressed={vista3d}
            aria-label={vista3d ? 'Vista 2D' : 'Vista 3D'}
            data-testid="vista-3d"
            className={`${ctl} text-[12px] font-black italic`}
          >
            {vista3d ? '2D' : '3D'}
          </button>
          <button
            onClick={() => navigate('/correr/cronometro')}
            aria-label="Abrir cronómetro"
            className={ctl}
          >
            <span className="material-symbols-outlined text-[20px]">timer</span>
          </button>
          <button
            onClick={() => void actualizar({ anunciosVoz: !voz })}
            aria-pressed={voz}
            aria-label={voz ? 'Silenciar voz' : 'Activar voz'}
            data-testid="boton-voz"
            className={ctl}
          >
            <span className="material-symbols-outlined text-[20px]">
              {voz ? 'volume_up' : 'volume_off'}
            </span>
          </button>
          {sesion && puedeCompartir() && (
            <button
              onClick={() => void (compartiendo ? dejarDeCompartir() : empezarACompartir())}
              aria-pressed={compartiendo}
              aria-label={compartiendo ? 'Dejar de compartir en vivo' : 'Compartir en vivo'}
              data-testid="compartir-en-vivo"
              className={`${ctl} ${compartiendo ? '!bg-c2-peligro' : ''}`}
            >
              <span className="material-symbols-outlined text-[20px]">sensors</span>
            </button>
          )}
          <button
            onClick={() => setDescartar(true)}
            aria-label="Salir de la carrera"
            className={ctl}
          >
            <span className="material-symbols-outlined text-[20px]">close</span>
          </button>
        </div>
        <section
          aria-label="Datos de la carrera"
          data-testid="tarjeta-metricas"
          className="absolute inset-x-3 bottom-[max(env(safe-area-inset-bottom),12px)] z-20 rounded-[14px] bg-c2-card/95 backdrop-blur-xl border border-c2-linea shadow-[0_-12px_40px_rgba(0,0,0,0.5)] px-4 pt-2 pb-3 lg:left-1/2 lg:-translate-x-1/2 lg:w-[420px]"
        >
          <div className="mx-auto mb-2 h-1 w-9 rounded-full bg-white/20" />
          <div className="flex items-end gap-4">
            <div className="shrink-0">
              <div className="text-[10px] font-bold uppercase tracking-wider text-white/80">
                Distancia
              </div>
              <div className="flex items-baseline gap-1">
                <span
                  className="font-display italic font-black text-[40px] leading-none text-c2-violeta tabular-nums"
                  data-testid="distancia"
                >
                  {Unidades.texto(e.distanciaKm, m)}
                </span>
                <span className="text-[12px] italic text-c2-muted">{m ? 'km' : 'mi'}</span>
              </div>
            </div>
            <div className="flex-1 grid grid-cols-3 gap-2 pb-1">
              <div>
                <div className="text-[10px] font-bold uppercase text-white/80">Tiempo</div>
                <div
                  className="text-[18px] font-black italic text-white tabular-nums leading-tight"
                  data-testid="tiempo"
                >
                  {e.segundos < 3600 ? formatearHms(e.segundos).slice(3) : formatearHms(e.segundos)}
                </div>
              </div>
              <div>
                <div className="text-[10px] font-bold uppercase text-white/80">Ritmo</div>
                <div
                  className="text-[18px] font-black italic text-c2-menta tabular-nums leading-tight"
                  data-testid="ritmo"
                >
                  {formatearRitmo(Unidades.ritmo(e.ritmoActualSegKm, m))}
                </div>
              </div>
              <div>
                <div className="text-[10px] font-bold uppercase text-white/80">Desnivel</div>
                <div className="text-[18px] font-black italic text-sky-300 tabular-nums leading-tight">
                  +{Math.round(e.desnivelM)}
                </div>
              </div>
            </div>
          </div>
          <div className="mt-2 flex items-end gap-1 h-6" data-testid="barras-parciales">
            {Array.from({ length: 6 }, (_, i) => {
              const p = ultimos[i]
              const actual = i === ultimos.length
              const alto = p
                ? 45 + (55 * (maxSeg - p.segundos)) / Math.max(1, maxSeg - minSeg || 1)
                : actual
                  ? 50
                  : 30
              return (
                <span
                  key={i}
                  className={`flex-1 ${actual ? 'bg-c2-menta' : p ? 'bg-gradient-to-b from-c2-violeta to-c2-morado' : 'bg-white/10'} [clip-path:polygon(4px_0,100%_0,calc(100%-4px)_100%,0_100%)]`}
                  style={{ height: `${Math.min(100, alto)}%` }}
                />
              )
            })}
          </div>
          <div className="mt-1 flex justify-between text-[10px] font-bold uppercase text-c2-muted">
            <span>Parciales</span>
            <span className="text-white/90 normal-case">
              {ultimo
                ? `KM ${ultimo.numero} ${formatearRitmo(ultimo.segundos)} · MEJOR KM ${mejor?.numero} ${formatearRitmo(mejor?.segundos ?? null)}`
                : 'Sin parciales aún'}
            </span>
          </div>
          <div className="mt-3 flex gap-3 items-center">
            <button
              onClick={() => setBloqueo(true)}
              aria-label="Bloquear pantalla"
              data-testid="boton-bloqueo"
              className="w-[52px] h-[52px] shrink-0 rounded-[10px] bg-c2-s2 border border-c2-linea text-white flex items-center justify-center"
            >
              <span className="material-symbols-outlined">lock</span>
            </button>
            <button
              onClick={() => {
                if (!pausada) setModalPausa(true)
                tracker.alternarPausa()
              }}
              aria-label={pausada ? 'Reanudar' : 'Pausar'}
              data-testid="boton-pausa"
              className="flex-1 boton-c2 !h-[52px]"
            >
              <span className="material-symbols-outlined">{pausada ? 'play_arrow' : 'pause'}</span>
              {pausada ? 'Reanudar' : 'Pausar'}
            </button>
            <button
              onClick={() => setModalFin(true)}
              aria-label="Terminar carrera"
              data-testid="boton-stop"
              className="w-[52px] h-[52px] shrink-0 rounded-[10px] bg-c2-s2 border border-c2-linea text-white flex items-center justify-center"
            >
              <span className="material-symbols-outlined">flag</span>
            </button>
          </div>
        </section>
        {bloqueo && (
          <div
            className="fixed inset-0 z-50 bg-black/40 flex items-end justify-center pb-16"
            data-testid="bloqueo-toque"
          >
            <button
              onClick={() => setBloqueo(false)}
              className="h-12 px-6 rounded-full bg-c2-card text-white flex items-center gap-2"
            >
              <span className="material-symbols-outlined">lock_open</span>Desbloquear
            </button>
          </div>
        )}
        {hojaReportar}
        {dialogo}
        {modales}
      </div>
    )
  }

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <div className="relative flex-1 bg-surface-container-low">
        <div className="absolute inset-0 bg-gradient-to-b from-surface-container-low via-surface to-surface"></div>

        {barra}
        {botonReportar}
        {hojaReportar}

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

          <div className="rounded-3xl bg-c2-card border border-c2-linea p-6 text-center">
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
                onClick={() => setModalFin(true)}
                aria-label="Terminar carrera"
                className="w-full h-16 rounded-2xl bg-danger/15 border border-danger/40 text-danger flex items-center justify-center gap-2 font-semibold"
              >
                <span className="material-symbols-outlined">stop_circle</span>Terminar
              </button>
            </div>
            <div className="text-center">
              <button
                onClick={() => {
                  if (!pausada) setModalPausa(true)
                  tracker.alternarPausa()
                }}
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
      {modales}
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
    <div className="rounded-2xl bg-c2-card border border-c2-linea p-4">
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
