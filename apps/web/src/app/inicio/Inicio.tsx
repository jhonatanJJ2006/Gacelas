import { lazy, Suspense, useEffect, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { useRacha } from '../../contexts/actividad/ui/useRacha'
import { useActividad } from '../../contexts/actividad/api'
import { MetaSemanal } from '../../contexts/analitica'
import { RankingSemanal } from '../../contexts/comunidad'
import { useClima } from '../../contexts/clima/api'
import { useComunidad } from '../../contexts/comunidad/api'
import { filtrarEventos, unirEventos, useEventos } from '../../contexts/eventos/api'
import { useIdentidad } from '../../contexts/identidad/api'
import { useRutas } from '../../contexts/rutas/api'
import { eventosMock } from '../../mocks/data'
import {
  inicioSemana,
  resumenSemana,
  retoMes,
  totalesSemana,
} from '../../shared/kernel/estadisticas'
import { nivelCorredor } from '../../shared/kernel/nivel'
import { formatearRitmo, Unidades } from '../../shared/lib/geo'
import { useEsEscritorio } from '../../shared/lib/useEsEscritorio'
import { saludo } from './saludo'
import {
  AmigosEnRuta,
  RutaSugerida,
  SemanaAtletica,
  TarjetaEvento,
  TarjetaReto,
} from './SeccionesInicio'

const Runner3D = lazy(() =>
  import('../../shared/ui/Runner3D').then((m) => ({ default: m.Runner3D }))
)

export function Inicio() {
  const clima = useClima()
  const escritorio = useEsEscritorio()
  const sesion = useIdentidad((s) => s.sesion)
  const carreras = useActividad((s) => s.carreras)
  const m = useIdentidad((s) => s.ajustes.unidadesMetricas)
  const creados = useEventos((s) => s.eventosCreados)
  const inscritos = useEventos((s) => s.inscritos)
  const ahora = Date.now()
  const semana = useMemo(() => resumenSemana(carreras, ahora), [carreras]) // eslint-disable-line react-hooks/exhaustive-deps
  const hoy = (new Date(ahora).getDay() + 6) % 7
  const feedSocial = useComunidad((s) => s.feedSocial)
  const publicaciones = useComunidad((s) => s.publicaciones)
  const refrescarFeed = useComunidad((s) => s.refrescarFeed)
  const meta = useIdentidad((s) => s.ajustes.metaSemanal) ?? 3
  useEffect(() => {
    if (sesion) void refrescarFeed()
  }, [sesion, refrescarFeed])
  const totales = useMemo(() => totalesSemana(carreras, ahora), [carreras]) // eslint-disable-line react-hooks/exhaustive-deps
  const reto = useMemo(() => retoMes(carreras, ahora), [carreras]) // eslint-disable-line react-hooks/exhaustive-deps
  const amigos = publicaciones.filter((p) => p.sigo && !p.propia).slice(0, 3)
  const lunes = new Date(inicioSemana(ahora))
  const domingo = new Date(lunes.getTime() + 6 * 86_400_000)
  const rango = `${lunes.getDate()} - ${domingo.getDate()} ${domingo.toLocaleDateString('es-EC', { month: 'short' }).replace('.', '')}`
  // Con Supabase los eventos vienen de la base; los de ejemplo solo sin backend.
  const eventos = feedSocial ? creados : unirEventos(creados, eventosMock)
  const proximo = filtrarEventos(eventos, 'Próximos', inscritos)[0]
  const nombre = sesion?.nombre.split(' ')[0] ?? 'Corredor'
  const rutas = useRutas((s) => s.rutas)
  const racha = useRacha()
  const nivel = nivelCorredor(carreras.reduce((t, c) => t + c.distanciaKm, 0))

  return (
    <div className="flex flex-col w-full px-margin pb-space-3xl space-y-space-xl contenedor-escritorio lg:grid lg:grid-cols-12 lg:gap-x-8 lg:gap-y-6 lg:space-y-0 lg:items-start">
      {/* Encabezado C2 (Android InicioContenido.kt): saludo, "HOLA, NOMBRE" en Anton y chips .wx. */}
      <section className="flex flex-col gap-1.5 pt-space-md lg:col-span-12 lg:pt-0">
        <span className="font-label-md text-[12px] font-semibold text-c2-muted">
          {saludo(new Date(ahora).getHours())}
        </span>
        <h1 className="font-display text-[30px] leading-9 text-text-primary uppercase">
          Hola, {nombre}
        </h1>
        <div className="flex flex-wrap gap-2 pt-1">
          {/* Clima real (Open-Meteo); sin dato muestra "--" en vez de un valor falso. */}
          <ChipC2
            rotulo={clima?.lugar ?? 'Loja'}
            valor={clima ? `${clima.temperatura} °C` : '--'}
            resto={clima ? ` ${clima.descripcion}` : ''}
            acento="#96CCFF"
            testId="chip-clima"
          />
          <ChipC2
            rotulo="Racha"
            valor={`${racha.diaria} ${racha.diaria === 1 ? 'día' : 'días'}`}
            acento="#34d399"
            borde="#34d399"
            testId="insignia-racha"
          />
          <ChipC2
            rotulo="Nivel"
            valor={nivel}
            acento="#E9D5FF"
            borde="#c084fc"
            fondo="linear-gradient(135deg, #5E1781, #7B1FA2)"
            testId="insignia-nivel"
          />
        </div>
      </section>

      {/* Escenario C2 (Android EscenarioInicioC2.kt): a todo el ancho, 430px, bordes en diagonal. */}
      <section
        className="lg:col-span-7 lg:row-span-3 relative -mx-margin lg:mx-0 h-[430px] lg:h-[560px] overflow-hidden"
        data-testid="inicio-escenario"
      >
        <div
          className="absolute inset-0 escenario-c2"
          style={{ clipPath: 'polygon(0 0, 100% 6%, 100% 100%, 0 92%)' }}
        >
          <span
            aria-hidden
            className="absolute bottom-6 -left-1.5 whitespace-nowrap font-display text-[120px] leading-[96px] text-transparent"
            style={{ WebkitTextStroke: '1.5px #C084FC40' }}
          >
            CORRE LOJA
          </span>
        </div>
        <div className="absolute inset-x-0 top-0 h-[84%]">
          <Suspense
            fallback={
              <div className="w-full h-full flex items-center justify-center text-text-secondary">
                <div className="flex flex-col items-center gap-2">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
                  <span className="text-sm">Cargando escena 3D...</span>
                </div>
              </div>
            }
          >
            <Runner3D
              animation="Idle"
              autoRotate
              giroArrastre
              cameraPosition={[0, 1.1, 5.3]}
              target={[0, 0.8, 0]}
            />
          </Suspense>
        </div>
        <EtiquetaEscenario rotulo="Avatar" valor={nivel} className="left-6 top-10" />
        <EtiquetaEscenario
          rotulo="Este mes"
          valor={`${Unidades.texto(reto.km, m, 1)} ${Unidades.etiquetaDistancia(m)}`}
          className="right-6 top-24"
        />
        <Link to="/correr" className="boton-c2 !absolute bottom-[30px] left-5 right-5 !w-auto">
          Empezar a correr
          <span
            className="material-symbols-outlined text-[24px]"
            style={{ fontVariationSettings: "'FILL' 1" }}
          >
            play_arrow
          </span>
        </Link>
      </section>

      <section className="lg:col-span-5 flex justify-between bg-c2-card p-4 tarjeta-c2">
        <MetricaInicio valor={`${semana.diasCorridos.size}`} etiqueta="Días" />
        <MetricaInicio
          valor={Unidades.texto(semana.km, m, 1)}
          etiqueta={Unidades.etiquetaDistancia(m)}
          color="text-c2-violeta"
          testId="km-semana"
        />
        <MetricaInicio
          valor={formatearRitmo(Unidades.ritmo(semana.ritmoSegKm, m))}
          etiqueta={Unidades.etiquetaRitmo(m)}
          color="text-c2-menta"
          alinear="items-end"
        />
      </section>

      <SemanaAtletica
        semana={totales}
        dias={semana.diasCorridos}
        hoy={hoy}
        metaCarreras={meta}
        rango={rango}
        m={m}
      />

      <TarjetaReto reto={reto} m={m} />

      <RutaSugerida rutas={rutas} carreras={carreras} m={m} />

      <AmigosEnRuta lista={amigos} m={m} />

      <section className="space-y-space-md lg:col-span-5">
        <div className="flex items-center justify-between">
          <h2 className="font-title-md text-title-md text-text-primary">Próximo evento</h2>
          <Link
            to="/comunidad/eventos"
            className="inline-block py-2 font-label-md text-label-md text-primary"
          >
            Ver todos
          </Link>
        </div>
        {proximo ? (
          <TarjetaEvento evento={proximo} inscrito={inscritos.includes(proximo.id)} />
        ) : (
          <p className="text-text-secondary">No hay encuentros próximos.</p>
        )}
      </section>

      {escritorio && (
        <div className="lg:col-span-12 grid grid-cols-2 gap-6">
          <MetaSemanal km={semana.km} />
          <RankingSemanal />
        </div>
      )}
    </div>
  )
}

/** Chip .wx del Figma C2 (Android ChipC2): 12px cursiva 800 en mayusculas, inclinado -10deg. */
function ChipC2({
  rotulo,
  valor,
  resto = '',
  acento,
  borde = '#2C3446',
  fondo = '#191f2f',
  testId,
}: {
  rotulo: string
  valor: string
  resto?: string
  acento: string
  borde?: string
  fondo?: string
  testId?: string
}) {
  return (
    <span
      data-testid={testId}
      className="inline-block px-2.5 py-1.5 text-[12px] font-extrabold italic uppercase text-white"
      style={{ transform: 'skewX(-10deg)', background: fondo, border: `1px solid ${borde}` }}
    >
      {rotulo} <span style={{ color: acento }}>{valor}</span>
      {resto}
    </span>
  )
}

/** Etiqueta .tg del escenario (Android EtiquetaEscenarioC2). */
function EtiquetaEscenario({
  rotulo,
  valor,
  className,
}: {
  rotulo: string
  valor: string
  className: string
}) {
  return (
    <div
      className={`pointer-events-none absolute z-20 flex flex-col px-2.5 py-1.5 border border-[#2C3446] bg-[#0C1322CC] ${className}`}
      style={{ transform: 'skewX(-10deg)' }}
    >
      <span className="text-[11px] font-extrabold italic uppercase text-white">{rotulo}</span>
      <span className="font-display text-[22px] leading-[22px] text-c2-violeta whitespace-nowrap">
        {valor}
      </span>
    </div>
  )
}

/** Metrica C2 de la fila bajo el escenario (Android MetricaC2). */
function MetricaInicio({
  valor,
  etiqueta,
  color = 'text-text-primary',
  alinear = 'items-start',
  testId,
}: {
  valor: string
  etiqueta: string
  color?: string
  alinear?: string
  testId?: string
}) {
  return (
    <div className={`flex flex-col gap-1 ${alinear}`}>
      <span className="etiqueta-c2">{etiqueta}</span>
      <span className={`valor-c2 ${color}`} data-testid={testId}>
        {valor}
      </span>
    </div>
  )
}
