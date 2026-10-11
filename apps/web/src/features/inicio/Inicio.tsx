import { useEsEscritorio } from '../../lib/useEsEscritorio'
import { MetaSemanal, RankingSemanal } from '../../components/PanelesEscritorio'
import { lazy, Suspense, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { filtrarEventos } from '../../lib/filtros'
import { formatearRitmo, Unidades } from '../../lib/geo'
import { resumenSemana } from '../../lib/estadisticas'
import { eventosMock } from '../../mocks/data'
import { useApp } from '../../store/app'
import { saludo } from '../../lib/logica'

const Runner3D = lazy(() =>
  import('../../components/Runner3D').then((m) => ({ default: m.Runner3D }))
)

const META_DIAS = 5

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
  const proximo = filtrarEventos([...creados, ...eventosMock], 'Próximos', inscritos)[0]
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
            <h2 className="font-headline-md text-headline-md text-text-primary tracking-tight">
              {saludo(new Date(ahora).getHours())}, {nombre}
            </h2>
          </div>
          <div className="bg-surface-container px-3 py-1.5 rounded-full flex items-center gap-1.5 shadow-sm text-text-secondary">
            <span className="material-symbols-outlined text-[18px] text-primary-400">
              calendar_today
            </span>
            <span className="font-label-md text-label-md text-on-surface capitalize">{fecha}</span>
          </div>
        </div>
      </section>

      <section className="lg:col-span-7 lg:row-span-3 relative flex flex-col rounded-3xl overflow-hidden bg-surface-container-high border border-border-subtle shadow-2xl">
        <div className="absolute top-3 inset-x-3 z-20 flex items-center justify-between pointer-events-none">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-bg-secondary/90 backdrop-blur-md text-primary border border-primary/20 shadow-md">
            <span className="w-2 h-2 rounded-full bg-secondary animate-ping"></span>
            <span className="font-label-md text-label-md font-bold tracking-wide uppercase">
              {semana.carreras} {semana.carreras === 1 ? 'carrera' : 'carreras'} esta semana
            </span>
          </div>
          <div className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-surface-container-highest/90 backdrop-blur-md text-text-primary shadow-sm border border-white/5">
            <span className="material-symbols-outlined text-[14px] text-primary">
              local_fire_department
            </span>
            <span
              className="font-label-md text-label-md font-semibold text-primary"
              data-testid="racha"
            >
              Racha {semana.racha}d
            </span>
          </div>
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
            <Runner3D animation="Idle" autoRotate cameraPosition={[0, 1.5, 4]} />
          </Suspense>
        </div>

        <div className="absolute bottom-4 inset-x-4 z-20 grid grid-cols-3 gap-2">
          <div className="bg-surface/90 backdrop-blur-md border border-border-subtle rounded-xl px-3 py-2.5 flex flex-col items-center shadow-lg">
            <span className="font-metric-huge text-metric-huge text-primary leading-none">
              {semana.diasCorridos.size}
            </span>
            <span className="font-label-md text-label-md text-text-secondary uppercase mt-0.5">
              Días
            </span>
          </div>
          <div className="bg-surface/90 backdrop-blur-md border border-border-subtle rounded-xl px-3 py-2.5 flex flex-col items-center shadow-lg">
            <span
              className="font-metric-huge text-metric-huge text-secondary leading-none"
              data-testid="km-semana"
            >
              {Unidades.texto(semana.km, m, 1)}
            </span>
            <span className="font-label-md text-label-md text-text-secondary uppercase mt-0.5">
              {Unidades.etiquetaDistancia(m)}
            </span>
          </div>
          <div className="bg-surface/90 backdrop-blur-md border border-border-subtle rounded-xl px-3 py-2.5 flex flex-col items-center shadow-lg">
            <span className="font-metric-huge text-metric-huge text-text-primary leading-none">
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
          <h3 className="font-title-md text-title-md text-text-primary">Acción rápida</h3>
        </div>

        <Link
          to="/correr"
          className="w-full bg-primary-600 hover:bg-primary-700 active:scale-[0.98] rounded-2xl p-6 flex items-center justify-between shadow-[0_8px_20px_-4px_rgba(217,119,6,0.35)] transition-all"
        >
          <div className="flex flex-col">
            <span className="font-headline-sm text-headline-sm text-bg-primary font-bold">
              Empezar a correr
            </span>
            <span className="font-body-md text-body-md text-bg-primary/80 mt-1">
              Graba tu siguiente carrera
            </span>
          </div>
          <div className="w-14 h-14 rounded-full bg-bg-primary/20 flex items-center justify-center">
            <span className="material-symbols-outlined text-[32px] text-bg-primary">
              play_arrow
            </span>
          </div>
        </Link>
      </section>

      <section className="space-y-space-md lg:col-span-5">
        <div className="flex items-center justify-between">
          <h3 className="font-title-md text-title-md text-text-primary">Semana atlética</h3>
          <span className="font-label-md text-label-md text-text-secondary">
            {semana.diasCorridos.size} de {META_DIAS} días
          </span>
        </div>

        <div className="flex gap-2">
          {['L', 'M', 'X', 'J', 'V', 'S', 'D'].map((day, index) => (
            <div
              key={day}
              data-corrido={semana.diasCorridos.has(index)}
              className={`flex-1 aspect-square rounded-xl flex flex-col items-center justify-center ${
                semana.diasCorridos.has(index)
                  ? 'bg-secondary-600/20 border-2 border-secondary-600'
                  : index === hoy
                    ? 'bg-surface-container border-2 border-primary-600'
                    : 'bg-surface-container border border-border-subtle'
              }`}
            >
              <span className="font-label-md text-label-md text-text-primary">{day}</span>
              {semana.diasCorridos.has(index) && (
                <span className="material-symbols-outlined text-[16px] text-secondary-500 mt-1">
                  check_circle
                </span>
              )}
            </div>
          ))}
        </div>
      </section>

      <section className="space-y-space-md lg:col-span-5">
        <div className="flex items-center justify-between">
          <h3 className="font-title-md text-title-md text-text-primary">Próximo encuentro</h3>
          <Link
            to="/comunidad/eventos"
            className="inline-block py-2 font-label-md text-label-md text-primary"
          >
            Ver todos
          </Link>
        </div>

        {proximo ? (
          <Link
            to={`/comunidad/eventos/${proximo.id}`}
            className="block bg-surface-container rounded-2xl border border-border-subtle overflow-hidden"
          >
            <div className="h-32 bg-gradient-to-br from-primary-600 to-secondary-600 flex items-center justify-center">
              <span className="material-symbols-outlined text-[64px] text-white">event</span>
            </div>
            <div className="p-4">
              <div className="flex items-center gap-2 mb-2">
                <span className="material-symbols-outlined text-[16px] text-primary-500">
                  calendar_today
                </span>
                <span className="font-label-md text-label-md text-text-secondary">
                  {proximo.fecha}, {proximo.hora}
                </span>
              </div>
              <h4 className="font-title-md text-title-md text-text-primary mb-1">
                {proximo.titulo}
              </h4>
              <p className="font-body-sm text-body-sm text-text-secondary mb-3">
                {proximo.descripcion}
              </p>
              <span className="font-label-md text-label-md text-text-secondary">
                {proximo.participantes + (inscritos.includes(proximo.id) ? 1 : 0)} corredores
                {inscritos.includes(proximo.id) ? ' (tú incluido)' : ''}
              </span>
            </div>
          </Link>
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
