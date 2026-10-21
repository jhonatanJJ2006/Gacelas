import { unirEventos } from '../../lib/eventos'
import { InsigniaRacha } from '../../components/Racha'
import { useEsEscritorio } from '../../lib/useEsEscritorio'
import { MetaSemanal, RankingSemanal } from '../../components/PanelesEscritorio'
import { lazy, Suspense, useEffect, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { filtrarEventos } from '../../lib/filtros'
import { formatearRitmo, Unidades } from '../../lib/geo'
import { resumenSemana, retoMes, totalesSemana, inicioSemana } from '../../lib/estadisticas'
import { AmigosEnRuta, SemanaAtletica, TarjetaEvento, TarjetaReto } from './SeccionesInicio'
import { eventosMock } from '../../mocks/data'
import { useApp } from '../../store/app'
import { saludo } from '../../lib/logica'

const Runner3D = lazy(() =>
  import('../../components/Runner3D').then((m) => ({ default: m.Runner3D }))
)

export function Inicio() {
  const escritorio = useEsEscritorio()
  const sesion = useApp((s) => s.sesion)
  const carreras = useApp((s) => s.carreras)
  const m = useApp((s) => s.ajustes.unidadesMetricas)
  const creados = useApp((s) => s.eventosCreados)
  const inscritos = useApp((s) => s.inscritos)
  const ahora = Date.now()
  const semana = useMemo(() => resumenSemana(carreras, ahora), [carreras]) // eslint-disable-line react-hooks/exhaustive-deps
  const hoy = (new Date(ahora).getDay() + 6) % 7
  const feedSocial = useApp((s) => s.feedSocial)
  const publicaciones = useApp((s) => s.publicaciones)
  const refrescarFeed = useApp((s) => s.refrescarFeed)
  const meta = useApp((s) => s.ajustes.metaSemanal) ?? 3
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
  const fecha = new Date(ahora).toLocaleDateString('es-EC', {
    weekday: 'long',
    day: 'numeric',
    month: 'short',
  })
  const nombre = sesion?.nombre.split(' ')[0] ?? 'Corredor'

  return (
    <div className="flex flex-col w-full px-margin pb-space-3xl space-y-space-xl contenedor-escritorio lg:grid lg:grid-cols-12 lg:gap-x-8 lg:gap-y-6 lg:space-y-0 lg:items-start">
      <section className="flex flex-col space-y-space-xs pt-space-md lg:col-span-12 lg:pt-0">
        <div className="flex items-center justify-between">
          <div>
            <span className="font-label-md text-label-md text-primary flex items-center gap-1.5 uppercase tracking-wider">
              <span className="material-symbols-outlined text-[16px]">location_on</span>
              Loja, Ecuador
            </span>
            <h1 className="font-headline-md text-headline-md text-text-primary tracking-tight">
              {saludo(new Date(ahora).getHours())}, {nombre}
            </h1>
          </div>
          <div className="bg-surface-container px-3 py-1.5 rounded-full flex items-center gap-1.5 shadow-sm text-text-secondary">
            <span className="material-symbols-outlined text-[18px] text-primary-400">
              calendar_today
            </span>
            <span className="font-label-md text-label-md text-on-surface capitalize whitespace-nowrap">
              {fecha}
            </span>
          </div>
        </div>
      </section>

      <section className="lg:col-span-7 lg:row-span-3 relative flex flex-col rounded-3xl overflow-hidden bg-surface-container-high border border-border-subtle shadow-2xl">
        <div className="absolute top-3 inset-x-3 z-20 flex items-center justify-between pointer-events-none">
          <div className="inline-flex items-center gap-1.5 px-3 max-[380px]:px-2 py-1 min-w-0 rounded-full bg-bg-secondary/90 backdrop-blur-md text-primary border border-primary/20 shadow-md">
            <span className="w-2 h-2 rounded-full bg-secondary animate-ping"></span>
            <span className="font-label-md text-label-md font-bold tracking-wide uppercase whitespace-nowrap max-[380px]:text-[10px] max-[380px]:tracking-normal">
              {semana.carreras} {semana.carreras === 1 ? 'carrera' : 'carreras'} esta semana
            </span>
          </div>
          <InsigniaRacha compacta />
        </div>

        <div className="relative w-full h-[430px] lg:h-[560px] bg-surface-container-lowest">
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
              cameraPosition={[0, 1.1, 5.3]}
              target={[0, 0.8, 0]}
            />
          </Suspense>
        </div>

        <div className="absolute bottom-4 inset-x-4 z-20 grid grid-cols-3 gap-2">
          <div className="bg-surface/90 backdrop-blur-md border border-border-subtle rounded-xl px-3 py-2.5 flex flex-col items-center shadow-lg">
            <span className="font-metric-huge text-metric-huge max-[380px]:text-[28px] whitespace-nowrap text-primary leading-none">
              {semana.diasCorridos.size}
            </span>
            <span className="font-label-md text-label-md text-text-secondary uppercase mt-0.5">
              Días
            </span>
          </div>
          <div className="bg-surface/90 backdrop-blur-md border border-border-subtle rounded-xl px-3 py-2.5 flex flex-col items-center shadow-lg">
            <span
              className="font-metric-huge text-metric-huge max-[380px]:text-[28px] whitespace-nowrap text-secondary leading-none"
              data-testid="km-semana"
            >
              {Unidades.texto(semana.km, m, 1)}
            </span>
            <span className="font-label-md text-label-md text-text-secondary uppercase mt-0.5">
              {Unidades.etiquetaDistancia(m)}
            </span>
          </div>
          <div className="bg-surface/90 backdrop-blur-md border border-border-subtle rounded-xl px-3 py-2.5 flex flex-col items-center shadow-lg">
            <span className="font-metric-huge text-metric-huge max-[380px]:text-[28px] whitespace-nowrap text-text-primary leading-none">
              {formatearRitmo(Unidades.ritmo(semana.ritmoSegKm, m))}
            </span>
            <span className="font-label-md text-label-md text-text-secondary uppercase mt-0.5">
              {Unidades.etiquetaRitmo(m)}
            </span>
          </div>
        </div>
      </section>

      <section className="space-y-space-md lg:col-span-5">
        <div className="flex items-center justify-between">
          <h2 className="font-title-md text-title-md text-text-primary">Acción rápida</h2>
        </div>

        <Link
          to="/correr"
          className="w-full bg-primary-600 hover:bg-primary-700 active:scale-[0.98] rounded-2xl p-6 flex items-center justify-between shadow-[0_8px_20px_-4px_rgba(123,31,162,0.35)] transition-all"
        >
          <div className="flex flex-col">
            <span className="font-headline-sm text-headline-sm text-on-primary font-bold">
              Empezar a correr
            </span>
            <span className="font-body-md text-body-md text-on-primary mt-1">
              Graba tu siguiente carrera
            </span>
          </div>
          <div className="w-14 h-14 rounded-full bg-bg-primary/20 flex items-center justify-center">
            <span className="material-symbols-outlined text-[32px] text-on-primary">
              play_arrow
            </span>
          </div>
        </Link>
      </section>

      <SemanaAtletica
        semana={totales}
        dias={semana.diasCorridos}
        hoy={hoy}
        metaCarreras={meta}
        rango={rango}
        m={m}
      />

      <section className="space-y-space-md lg:col-span-5">
        <div className="flex items-center justify-between">
          <h2 className="font-title-md text-title-md text-text-primary">Próximo encuentro</h2>
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

      <TarjetaReto reto={reto} m={m} />

      <AmigosEnRuta lista={amigos} m={m} />
      {escritorio && (
        <div className="lg:col-span-12 grid grid-cols-2 gap-6">
          <MetaSemanal km={semana.km} />
          <RankingSemanal />
        </div>
      )}
    </div>
  )
}
