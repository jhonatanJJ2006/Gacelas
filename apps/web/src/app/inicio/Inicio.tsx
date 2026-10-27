import { lazy, Suspense, useEffect, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { InsigniaRacha } from '../../contexts/actividad'
import { useActividad } from '../../contexts/actividad/api'
import { MetaSemanal } from '../../contexts/analitica'
import { InsigniaNivel, RankingSemanal } from '../../contexts/comunidad'
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
  const fecha = new Date(ahora).toLocaleDateString('es-EC', {
    weekday: 'long',
    day: 'numeric',
    month: 'short',
  })
  const nombre = sesion?.nombre.split(' ')[0] ?? 'Corredor'
  const rutas = useRutas((s) => s.rutas)

  return (
    <div className="flex flex-col w-full px-margin pb-space-3xl space-y-space-xl contenedor-escritorio lg:grid lg:grid-cols-12 lg:gap-x-8 lg:gap-y-6 lg:space-y-0 lg:items-start">
      <section className="flex flex-col gap-2 pt-space-md lg:col-span-12 lg:pt-0">
        <span className="text-[11px] font-bold uppercase tracking-[0.14em] text-c2-muted capitalize">
          {fecha}
        </span>
        <h1 className="font-display italic uppercase text-[26px] leading-tight text-text-primary tracking-tight">
          {saludo(new Date(ahora).getHours())}, {nombre}
        </h1>
        <div className="flex flex-wrap items-center gap-2">
          <span className="pildora-c2">
            <span className="material-symbols-outlined text-[14px] text-c2-violeta">
              location_on
            </span>
            Loja 14° Fresco
          </span>
          <InsigniaNivel nivel={nivelCorredor(carreras.reduce((t, c) => t + c.distanciaKm, 0))} />
          <InsigniaRacha compacta />
        </div>
      </section>

      <section className="lg:col-span-7 lg:row-span-3 relative flex flex-col overflow-hidden bg-c2-card tarjeta-c2">
        <div
          className="relative w-full h-[320px] lg:h-[520px] escenario-c2"
          data-testid="inicio-escenario"
        >
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
          <div className="pointer-events-none absolute top-3 left-3 z-20">
            <span className="etiqueta-c2">Avatar</span>
            <p className="font-headline-sm text-[18px] italic uppercase text-text-primary">
              {semana.diasCorridos.size} {semana.diasCorridos.size === 1 ? 'día' : 'días'}
            </p>
          </div>
          <div className="pointer-events-none absolute top-3 right-3 z-20 text-right">
            <span className="etiqueta-c2">Esta semana</span>
            <p className="font-headline-sm text-[18px] italic text-c2-violeta">
              {Unidades.texto(semana.km, m, 1)} {Unidades.etiquetaDistancia(m)}
            </p>
          </div>
        </div>
        <div className="p-4 flex flex-col gap-4">
          <Link to="/correr" className="boton-c2">
            <span className="material-symbols-outlined text-[20px]">play_arrow</span>
            Empezar a correr
          </Link>
          <div className="grid grid-cols-3 gap-2">
            <div className="metrica-c2">
              <span className="etiqueta-c2">Días</span>
              <span className="valor-c2 text-text-primary">{semana.diasCorridos.size}</span>
            </div>
            <div className="metrica-c2">
              <span className="etiqueta-c2">{Unidades.etiquetaDistancia(m)}</span>
              <span className="valor-c2 text-c2-violeta" data-testid="km-semana">
                {Unidades.texto(semana.km, m, 1)}
              </span>
            </div>
            <div className="metrica-c2">
              <span className="etiqueta-c2">Ritmo {Unidades.etiquetaRitmo(m)}</span>
              <span className="valor-c2 text-c2-menta">
                {formatearRitmo(Unidades.ritmo(semana.ritmoSegKm, m))}
              </span>
            </div>
          </div>
        </div>
      </section>

      <RutaSugerida rutas={rutas} carreras={carreras} m={m} />

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
