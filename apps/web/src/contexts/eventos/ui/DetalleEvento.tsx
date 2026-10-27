import { useEffect, useState } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { compartir, useAviso } from '../../../hooks'
import { eventoPorId } from '../../../mocks/data'
import { Aviso } from '../../../shared/ui/ui'
import { ModalReporte } from '../../comunidad'
import { yaReportado } from '../../moderacion/api'
import { estadoEvento, textoCupo, TIPOS_EVENTO, type EstadoEvento } from '../api'
import { useEventos } from '../application/estado'

export function DetalleEvento() {
  const navigate = useNavigate()
  const { id = '' } = useParams()
  const creados = useEventos((s) => s.eventosCreados)
  const inscritos = useEventos((s) => s.inscritos)
  const alternar = useEventos((s) => s.alternarInscripcion)
  const [aviso, mostrarAviso] = useAviso()
  const [estado, setEstado] = useState<EstadoEvento | null>(null)
  const [reportando, setReportando] = useState(false)
  useEffect(() => {
    void estadoEvento(id).then(setEstado)
  }, [id, inscritos])
  const evento = creados.find((e) => e.id === id) ?? eventoPorId(id)

  if (!evento) return <Navigate to="/comunidad/eventos" replace />

  const inscrito = inscritos.includes(evento.id)
  const participantes = estado
    ? evento.participantes + estado.inscritos
    : evento.participantes + (inscrito ? 1 : 0)
  const enEspera = estado?.miEstado === 'waitlist'

  const compartirEvento = async () => {
    const r = await compartir(
      evento.titulo,
      `${evento.titulo}: ${evento.fecha}, ${evento.hora} en ${evento.ubicacion}. Únete con Runcity.`
    )
    if (r === 'copiado') mostrarAviso('Enlace copiado')
    else if (r === 'error') mostrarAviso('No se pudo compartir')
  }

  return (
    <main className="w-full min-h-screen bg-surface flex flex-col lg:max-w-5xl lg:mx-auto lg:px-8 lg:py-8">
      <div className="h-64 lg:h-80 lg:rounded-3xl lg:overflow-hidden bg-gradient-to-br from-primary-600 to-secondary-600 relative">
        <button
          onClick={() => navigate('/comunidad/eventos')}
          aria-label="Volver"
          className="absolute top-4 left-4 w-10 h-10 rounded-full bg-black/60 backdrop-blur-md flex items-center justify-center text-white shadow-lg z-10"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </button>
        <button
          onClick={() => void compartirEvento()}
          aria-label="Compartir evento"
          className="absolute top-4 right-4 w-10 h-10 rounded-full bg-black/60 backdrop-blur-md flex items-center justify-center text-white shadow-lg z-10"
        >
          <span className="material-symbols-outlined">share</span>
        </button>
        <div className="absolute inset-0 flex items-center justify-center">
          <span className="material-symbols-outlined text-[120px] text-white/20">event</span>
        </div>
        <div className="absolute bottom-4 left-4 right-4">
          <div className="bg-black/60 backdrop-blur-md px-3 py-1.5 rounded-full inline-flex items-center gap-2">
            <span className="material-symbols-outlined text-[16px] text-primary-400">
              directions_run
            </span>
            <span className="text-white font-label-md text-label-md">{evento.distanciaKm} km</span>
          </div>
        </div>
      </div>

      <div className="flex-1 px-6 py-6 pb-24 lg:px-0 lg:grid lg:grid-cols-2 lg:gap-x-8 lg:content-start">
        <h1 className="font-headline-lg text-headline-lg text-text-primary mb-2 lg:col-span-2">
          {evento.titulo}
        </h1>
        <div className="flex flex-wrap gap-2 mb-4 lg:col-span-2">
          <span className="px-3 py-1 rounded-full bg-surface-container text-sm text-text-secondary">
            {TIPOS_EVENTO[evento.tipo ?? 'grupal']}
          </span>
          {evento.clubId && (
            <Link
              to={`/comunidad/clubes/${evento.clubId}`}
              className="px-3 py-1 rounded-full bg-surface-container text-sm text-primary-400"
            >
              Evento de club
            </Link>
          )}
          {evento.rutaId && (
            <Link
              to={`/rutas/${evento.rutaId}`}
              className="px-3 py-1 rounded-full bg-surface-container text-sm text-primary-400"
            >
              Ver ruta planificada
            </Link>
          )}
          <Link
            to={`/comunidad/eventos/${evento.id}/en-vivo`}
            data-testid="evento-en-vivo"
            className="px-3 py-1 rounded-full bg-danger/20 text-sm text-red-300 flex items-center gap-1"
          >
            <span className="material-symbols-outlined text-[16px]">sensors</span>
            {evento.pasado ? 'Repetición' : 'En vivo'}
          </Link>
        </div>

        <div className="space-y-3 mb-6">
          <div className="flex items-start gap-3">
            <div className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center shrink-0">
              <span className="material-symbols-outlined text-[20px] text-primary-500">
                calendar_today
              </span>
            </div>
            <div>
              <div className="font-body-md text-body-md text-text-primary font-semibold capitalize">
                {evento.fecha}
              </div>
              <div className="font-body-sm text-body-sm text-text-secondary">{evento.hora}</div>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <div className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center shrink-0">
              <span className="material-symbols-outlined text-[20px] text-secondary-500">
                location_on
              </span>
            </div>
            <div>
              <div className="font-body-md text-body-md text-text-primary font-semibold">
                {evento.ubicacion}
              </div>
              <div className="font-body-sm text-body-sm text-text-secondary">Loja, Ecuador</div>
            </div>
          </div>
        </div>

        <div className="bg-surface-container rounded-2xl p-4 mb-6">
          <h2 className="font-title-md text-title-md text-text-primary mb-3">Descripción</h2>
          <p className="font-body-md text-body-md text-text-secondary leading-relaxed">
            {evento.descripcion}
          </p>
        </div>

        <div className="bg-surface-container rounded-2xl p-4 mb-6">
          <h2
            className="font-title-md text-title-md text-text-primary mb-1"
            data-testid="participantes"
          >
            Participantes ({participantes})
          </h2>
          {estado && (
            <p className="text-sm text-text-secondary mb-3" data-testid="cupo-evento">
              {textoCupo(estado)}
              {enEspera && ` · estás en la lista de espera (puesto ${estado.miPosicion})`}
            </p>
          )}
          <div className="flex flex-wrap gap-2">
            {Array.from({ length: Math.min(participantes, 24) }, (_, i) => (
              <div
                key={i}
                className={`w-12 h-12 rounded-full border-2 border-border-subtle ${inscrito && i === 0 ? 'bg-primary-600/50' : 'bg-surface-variant'}`}
              ></div>
            ))}
          </div>
        </div>

        {evento.pasado ? (
          <p className="text-center text-text-secondary">Este evento ya terminó.</p>
        ) : (
          <button
            onClick={() => {
              void alternar(evento.id).then(async () => {
                const e = await estadoEvento(evento.id)
                setEstado(e)
                mostrarAviso(
                  inscrito
                    ? 'Inscripción cancelada'
                    : e?.miEstado === 'waitlist'
                      ? `Evento lleno: quedaste en la lista de espera (puesto ${e.miPosicion})`
                      : 'Te inscribiste al evento'
                )
              })
            }}
            aria-pressed={inscrito}
            className={`w-full h-14 active:scale-[0.98] font-bold text-base rounded-xl flex items-center justify-center gap-2 transition-all ${
              inscrito
                ? 'bg-surface-container-high text-text-primary border border-border-subtle'
                : 'bg-primary-600 hover:bg-primary-700 text-on-primary shadow-[0_8px_20px_-4px_rgba(123,31,162,0.35)]'
            }`}
          >
            <span className="material-symbols-outlined text-[20px]">
              {inscrito ? 'event_busy' : 'check_circle'}
            </span>
            <span>
              {inscrito
                ? enEspera
                  ? 'Salir de la lista de espera'
                  : 'Cancelar inscripción'
                : estado?.cupo != null && estado.inscritos >= estado.cupo
                  ? 'Unirme a la lista de espera'
                  : 'Inscribirme al evento'}
            </span>
          </button>
        )}
      </div>
      <div className="px-margin lg:px-0 pb-8">
        <button
          type="button"
          data-testid="reportar-evento"
          onClick={() =>
            yaReportado('evento', evento.id)
              ? mostrarAviso('Ya reportaste este evento')
              : setReportando(true)
          }
          className="inline-flex items-center gap-1 text-[13px] text-c2-muted hover:text-c2-peligro"
        >
          <span className="material-symbols-outlined text-[16px]">flag</span>
          Reportar evento
        </button>
      </div>
      <ModalReporte
        tipo="evento"
        postId={reportando ? evento.id : null}
        onCerrar={() => setReportando(false)}
        onEnviado={(ok) => {
          setReportando(false)
          mostrarAviso(ok ? 'Reporte enviado' : 'No se pudo enviar el reporte. Revisa tu conexión')
        }}
      />
      <Aviso mensaje={aviso} />
    </main>
  )
}
