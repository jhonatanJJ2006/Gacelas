import { lazy, Suspense, useEffect, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { useActividad } from '../../contexts/actividad/api'
import { useRacha } from '../../contexts/actividad/ui/useRacha'
import { useEstadoClima } from '../../contexts/clima/api'
import { useComunidad } from '../../contexts/comunidad/api'
import { useIdentidad } from '../../contexts/identidad/api'
import { MESES_CORTOS, resumenMes, totalesSemana } from '../../shared/kernel/estadisticas'
import { nivelCorredor } from '../../shared/kernel/nivel'
import { rachaEnRiesgo } from '../../shared/kernel/rachas'
import { formatearRitmo, Unidades } from '../../shared/lib/geo'
import { AvisoNavegacion } from '../../shared/ui/AvisoNavegacion'
import { EsqueletoEscena3D } from '../../shared/ui/Esqueletos'
import { Cifra, Progreso } from '../../shared/ui/Visual'
import { BotonNotificaciones, ChipClima, fechaCorta, useAbrirClima } from '../BarraSuperior'
import { useEventosVisibles, useSondeoEnVivo } from '../datosApp'
import { useBusqueda, useMetaSemanal } from '../ShellEscritorio'
import {
  PanelActividad,
  PanelClima,
  PanelEnRuta,
  PanelEventos,
  PanelRanking,
  PanelReto,
  PanelRutaSegura,
  PanelSeguridad,
  PanelSemana,
  textoVentana,
} from './PanelesInicio'

const Runner3D = lazy(() =>
  import('../../shared/ui/Runner3D').then((m) => ({ default: m.Runner3D }))
)

/**
 * Inicio del rediseno web (docs/REDISENO-WEB.md). Una sola meta semanal en km
 * (ajustes.metaKmSemana); la meta de carreras se muestra como dato secundario.
 * Escritorio: rejilla de 12 columnas. Movil: orden de inicio-movil.
 */
export function Inicio() {
  const sesion = useIdentidad((s) => s.sesion)
  const refrescarFeed = useComunidad((s) => s.refrescarFeed)
  const { proximos } = useEventosVisibles()
  useSondeoEnVivo()
  useEffect(() => {
    if (sesion) void refrescarFeed()
  }, [sesion, refrescarFeed])

  return (
    <div className="w-full px-4 pb-space-3xl lg:px-8 lg:pb-8 lg:max-w-[1440px] lg:mx-auto">
      <AvisoNavegacion />
      <CabeceraMovil />
      <div className="flex flex-col gap-4 lg:grid lg:grid-cols-12 lg:gap-5">
        <Hero className="order-1 lg:order-none lg:col-span-12 xl:col-span-8" />
        <PanelClima className="mov-escalon order-4 lg:order-none lg:col-span-6 xl:col-span-4" />
        <PanelRutaSegura className="mov-escalon order-2 lg:order-none lg:col-span-6 xl:col-span-5" />
        <PanelSemana className="mov-escalon order-3 lg:order-none lg:col-span-6 xl:col-span-4" />
        <PanelReto className="mov-escalon order-8 lg:order-none lg:col-span-6 xl:col-span-3" />
        <PanelActividad className="mov-escalon order-9 lg:order-none lg:col-span-12 xl:col-span-5" />
        <PanelEnRuta className="mov-escalon order-10 lg:order-none lg:col-span-6 xl:col-span-4" />
        <PanelRanking className="mov-escalon order-6 lg:order-none lg:col-span-6 xl:col-span-3" />
        <PanelEventos
          eventos={proximos}
          className="mov-escalon order-5 lg:order-none lg:col-span-12 xl:col-span-7"
        />
        <PanelSeguridad className="mov-escalon order-7 lg:order-none lg:col-span-12 xl:col-span-5" />
      </div>
    </div>
  )
}

/** Cabecera movil de Inicio (inicio-movil): fecha, saludo, buscar, clima y campana. */
function CabeceraMovil() {
  const sesion = useIdentidad((s) => s.sesion)
  const abrir = useBusqueda((s) => s.abrir)
  const abrirClima = useAbrirClima()
  const nombre = sesion?.nombre.split(' ')[0] ?? 'Corredor'
  return (
    <header className="lg:hidden flex items-center gap-2 pt-[max(env(safe-area-inset-top),12px)] pb-3">
      <Link
        to="/perfil"
        aria-label="Ir a mi perfil"
        className="w-10 h-10 shrink-0 rounded-md bg-[linear-gradient(135deg,#7B1FA2,#A855F7)] grid place-items-center"
      >
        <span aria-hidden="true" className="material-symbols-outlined text-white text-[20px]">
          directions_run
        </span>
      </Link>
      <div className="min-w-0 flex-1">
        <span className="rotulo-c2 !text-[9.5px] truncate">{fechaCorta()} · Loja</span>
        <h1 className="titular-c2 text-[22px] leading-none mt-0.5 text-white truncate">
          Hola, {nombre}
        </h1>
      </div>
      <button
        type="button"
        onClick={abrir}
        aria-label="Buscar"
        className="w-10 h-10 shrink-0 rounded-md bg-c2-card border border-c2-linea grid place-items-center text-white"
      >
        <span aria-hidden="true" className="material-symbols-outlined text-[20px]">
          search
        </span>
      </button>
      <ChipClima compacto onAbrir={abrirClima} testId="chip-clima-movil" />
      <BotonNotificaciones />
    </header>
  )
}

/** Hero: lo que falta para la meta semanal, mejor hora, acciones y cifras reales. */
function Hero({ className = '' }: { className?: string }) {
  const carreras = useActividad((s) => s.carreras)
  const m = useIdentidad((s) => s.ajustes.unidadesMetricas)
  const metaCarreras = useIdentidad((s) => s.ajustes.metaSemanal) ?? 3
  const meta = useMetaSemanal()
  const racha = useRacha()
  const clima = useEstadoClima().clima
  const ventana = textoVentana(clima)
  const ahora = Date.now()
  const nivel = nivelCorredor(carreras.reduce((t, c) => t + c.distanciaKm, 0))
  const mes = useMemo(() => resumenMes(carreras, ahora), [carreras]) // eslint-disable-line react-hooks/exhaustive-deps
  const mesPrevio = useMemo(() => {
    const d = new Date(ahora)
    return resumenMes(carreras, new Date(d.getFullYear(), d.getMonth() - 1, 15).getTime())
  }, [carreras]) // eslint-disable-line react-hooks/exhaustive-deps
  const nombreMesPrevio = MESES_CORTOS[(new Date(ahora).getMonth() + 11) % 12]!.toLowerCase()
  const s = meta.semana
  const u = Unidades.etiquetaDistancia(m)
  const riesgo = rachaEnRiesgo(racha, ahora)
  const diasRestantes = 7 - ((new Date(ahora).getDay() + 6) % 7)
  const variacion =
    mesPrevio.km > 0 ? Math.round(((mes.km - mesPrevio.km) / mesPrevio.km) * 100) : null
  const totales = useMemo(() => totalesSemana(carreras, ahora), [carreras]) // eslint-disable-line react-hooks/exhaustive-deps
  const tiempoSemana = `${Math.floor(totales.segundos / 3600)}:${String(Math.floor((totales.segundos % 3600) / 60)).padStart(2, '0')}`
  const desnivelSemana = totales.desnivelM

  const sugerencia =
    meta.faltan <= 0
      ? `Cumpliste tu meta de ${Unidades.texto(meta.metaKm, m, 0)} ${u}. Todo lo que sumes ahora es extra.`
      : `Llevas ${Unidades.texto(meta.km, m, 1)} de ${Unidades.texto(meta.metaKm, m, 0)} ${u} y quedan ${diasRestantes} ${diasRestantes === 1 ? 'día' : 'días'}.`
  const textoHora = ventana
    ? ` La mejor hora para salir hoy es entre ${ventana.rango.replace(' - ', ' y ')}.`
    : ''

  const chips = (movil: boolean) => (
    <>
      <span
        data-testid={movil ? 'insignia-racha' : undefined}
        className={`text-[11px] font-semibold px-2 py-1 rounded-full ${riesgo ? 'bg-amber-400/15 text-amber-400' : 'bg-c2-menta/15 text-c2-menta'}`}
      >
        Racha {racha.diaria} {racha.diaria === 1 ? 'día' : 'días'}
        {riesgo ? ', en riesgo' : ''}
      </span>
      <span
        data-testid={movil ? 'insignia-nivel' : undefined}
        className="text-[11px] font-semibold px-2 py-1 rounded-full bg-c2-morado/20 text-c2-violeta"
      >
        {nivel}
      </span>
      <span className="text-[11px] font-semibold px-2 py-1 rounded-full bg-c2-s2 text-c2-muted">
        {s.carreras} de {metaCarreras} carreras esta semana
      </span>
    </>
  )

  return (
    <>
      <div className="flex flex-wrap gap-1.5 lg:hidden order-1">{chips(true)}</div>
      <section
        data-testid="inicio-escenario"
        aria-labelledby="titulo-hero"
        className={`panel-c2 relative overflow-hidden border border-[#3B1A55] bg-[linear-gradient(160deg,#2A0A3D_0%,#1A0B2E_55%,#191F2F_100%)] ${className}`}
      >
        <span
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 bg-[repeating-linear-gradient(115deg,transparent_0_38px,rgba(192,132,252,0.05)_38px_40px)]"
        />
        <div className="absolute right-0 bottom-0 top-6 w-[44%] lg:w-[34%] ancho:w-[30%] pointer-events-auto">
          <Suspense fallback={<EsqueletoEscena3D texto="Cargando avatar" />}>
            <Runner3D
              animation="Idle"
              autoRotate
              giroArrastre
              cameraPosition={[0, 1.1, 4.6]}
              target={[0, 0.85, 0]}
            />
          </Suspense>
        </div>
        <div className="relative pointer-events-none [&_a]:pointer-events-auto [&_button]:pointer-events-auto p-5 lg:p-7 max-w-[100%] lg:max-w-[68%] ancho:max-w-[70%]">
          <div className="hidden lg:flex flex-wrap gap-1.5 mb-4">{chips(false)}</div>
          <span className="rotulo-c2 !text-c2-violeta lg:hidden">Meta semanal</span>
          <h2
            id="titulo-hero"
            className="titular-c2 text-[30px] leading-[1.02] lg:text-[42px] text-white mt-1 max-w-[60%] lg:max-w-none"
          >
            {meta.faltan > 0 ? (
              <>
                Te faltan {Unidades.texto(meta.faltan, m, meta.faltan < 10 ? 1 : 0)} {u}
                <span className="hidden lg:block text-c2-violeta">para tu meta semanal</span>
              </>
            ) : (
              <>
                Meta cumplida
                <span className="hidden lg:block text-c2-violeta">
                  {Unidades.texto(meta.km, m, 1)} {u} esta semana
                </span>
              </>
            )}
          </h2>
          <p className="mt-3 text-[13px] text-c2-muted max-w-[60%] lg:max-w-[520px]">
            <span className="hidden lg:inline">{sugerencia}</span>
            {textoHora ? (
              <span className="lg:hidden">Mejor hora para salir: {ventana!.rango}</span>
            ) : null}
            <span className="hidden lg:inline">{textoHora}</span>
          </p>
          <div className="mt-3 max-w-[60%] lg:hidden">
            <Progreso pct={meta.pct} etiqueta="Progreso de la meta semanal" />
            <p className="text-[11px] text-c2-muted mt-1">
              {Unidades.texto(meta.km, m, 1)} de {Unidades.texto(meta.metaKm, m, 0)} {u}
            </p>
          </div>
          <div className="mt-5 flex flex-wrap gap-3">
            <Link
              to="/correr"
              className="panel-c2 h-12 px-6 flex-1 lg:flex-none inline-flex items-center justify-center gap-2 bg-[linear-gradient(90deg,#7B1FA2,#A855F7)] text-white text-[14px] font-bold hover:brightness-110"
            >
              <span aria-hidden="true" className="material-symbols-outlined text-[20px]">
                play_circle
              </span>
              Empezar a correr
            </Link>
            <button
              type="button"
              onClick={() =>
                document
                  .querySelector('[data-testid="ruta-sugerida"]')
                  ?.scrollIntoView({ behavior: 'smooth', block: 'center' })
              }
              className="hidden lg:inline-flex h-12 px-5 items-center gap-2 rounded-md border border-c2-linea bg-c2-bg/40 text-white text-[13.5px] font-semibold hover:border-c2-morado"
            >
              <span aria-hidden="true" className="material-symbols-outlined text-[18px]">
                map
              </span>
              Ruta segura del día
            </button>
          </div>
          <div className="hidden lg:grid grid-cols-4 gap-3 mt-6 max-w-[620px]">
            <CajaCifra
              rotulo="Mes"
              valor={Unidades.texto(mes.km, m, 1)}
              unidad={u}
              nota={
                variacion != null
                  ? `${variacion >= 0 ? '+' : ''}${variacion}% vs ${nombreMesPrevio}`
                  : `${mes.carreras} carreras`
              }
              notaColor={variacion != null && variacion >= 0 ? 'text-c2-menta' : undefined}
            />
            <CajaCifra
              rotulo="Ritmo"
              valor={formatearRitmo(Unidades.ritmo(s.ritmoSegKm, m))}
              unidad={Unidades.etiquetaRitmo(m)}
            />
            <CajaCifra
              rotulo="Tiempo"
              valor={tiempoSemana}
              unidad="h"
              nota={`${s.carreras} ${s.carreras === 1 ? 'carrera' : 'carreras'}`}
            />
            <CajaCifra
              rotulo="Desnivel"
              valor={`+${Math.round(desnivelSemana)}`}
              unidad="m"
              nota="esta semana"
            />
          </div>
        </div>
      </section>
      <div className="grid grid-cols-3 gap-2 lg:hidden order-1">
        <CajaCifra rotulo="Mes" valor={Unidades.texto(mes.km, m, 1)} unidad={u} />
        <CajaCifra
          rotulo="Ritmo"
          valor={formatearRitmo(Unidades.ritmo(s.ritmoSegKm, m))}
          unidad={Unidades.etiquetaRitmo(m)}
          color="text-c2-menta"
        />
        <CajaCifra
          rotulo="Desnivel"
          valor={`+${Math.round(desnivelSemana)}`}
          unidad="m"
          color="text-c2-cielo"
        />
      </div>
    </>
  )
}

function CajaCifra({
  rotulo,
  valor,
  unidad,
  nota,
  notaColor,
  color = 'text-white',
}: {
  rotulo: string
  valor: string
  unidad?: string
  nota?: string
  notaColor?: string
  color?: string
}) {
  return (
    <div className="panel-c2 bg-c2-bg/60 border border-c2-linea px-3 py-2.5 min-w-0">
      <Cifra
        rotulo={rotulo}
        valor={valor}
        unidad={unidad}
        color={color}
        tamano="text-[22px] lg:text-[24px]"
        nota={nota ? <span className={notaColor}>{nota}</span> : undefined}
      />
    </div>
  )
}
