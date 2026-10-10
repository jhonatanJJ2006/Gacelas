import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { Aviso } from '../../components/ui'
import { compartir, useAviso } from '../../hooks'
import { eventoPorId } from '../../mocks/data'
import { useApp } from '../../store/app'

export function DetalleEvento() {
  const navigate = useNavigate()
  const { id = '' } = useParams()
  const creados = useApp((s) => s.eventosCreados)
  const inscritos = useApp((s) => s.inscritos)
  const alternar = useApp((s) => s.alternarInscripcion)
  const [aviso, mostrarAviso] = useAviso()
  const evento = creados.find((e) => e.id === id) ?? eventoPorId(id)

  if (!evento) return <Navigate to="/comunidad/eventos" replace />

  const inscrito = inscritos.includes(evento.id)
  const participantes = evento.participantes + (inscrito ? 1 : 0)

  const compartirEvento = async () => {
    const r = await compartir(
      evento.titulo,
      `${evento.titulo}: ${evento.fecha}, ${evento.hora} en ${evento.ubicacion}. Únete con Gacelas.`
    )
    if (r === 'copiado') mostrarAviso('Enlace copiado')
    else if (r === 'error') mostrarAviso('No se pudo compartir')
  }

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <div className="h-64 bg-gradient-to-br from-primary-600 to-secondary-600 relative">
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

      <div className="flex-1 px-6 py-6 pb-24">
        <h1 className="font-headline-lg text-headline-lg text-text-primary mb-4">
          {evento.titulo}
        </h1>

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
            className="font-title-md text-title-md text-text-primary mb-3"
            data-testid="participantes"
          >
            Participantes ({participantes})
          </h2>
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
              void alternar(evento.id)
              mostrarAviso(inscrito ? 'Inscripción cancelada' : 'Te inscribiste al evento')
            }}
            aria-pressed={inscrito}
            className={`w-full h-14 active:scale-[0.98] font-bold text-base rounded-xl flex items-center justify-center gap-2 transition-all ${
              inscrito
                ? 'bg-surface-container-high text-text-primary border border-border-subtle'
                : 'bg-primary-600 hover:bg-primary-700 text-bg-primary shadow-[0_8px_20px_-4px_rgba(217,119,6,0.35)]'
            }`}
          >
            <span className="material-symbols-outlined text-[20px]">
              {inscrito ? 'event_busy' : 'check_circle'}
            </span>
            <span>{inscrito ? 'Cancelar inscripción' : 'Inscribirme al evento'}</span>
          </button>
        )}
      </div>
      <Aviso mensaje={aviso} />
    </div>
  )
}
