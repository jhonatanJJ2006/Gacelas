import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Aviso, BotonTexto, Dialogo, MensajeError } from '../../components/ui'
import { useAviso } from '../../hooks'
import { filtrarEventos, PESTANAS_EVENTOS, type PestanaEventos } from '../../lib/filtros'
import { crearId } from '../../lib/runTracker'
import { eventosMock, type Evento } from '../../mocks/data'
import { useApp } from '../../store/app'
import { validarEvento } from '../../lib/logica'

const campo =
  'w-full h-11 px-3 bg-surface-container border border-border-subtle rounded-lg text-text-primary placeholder-text-secondary'

export function Eventos() {
  const creados = useApp((s) => s.eventosCreados)
  const inscritos = useApp((s) => s.inscritos)
  const crearEvento = useApp((s) => s.crearEvento)
  const alternar = useApp((s) => s.alternarInscripcion)
  const [pestana, setPestana] = useState<PestanaEventos>('Próximos')
  const [creando, setCreando] = useState(false)
  const [form, setForm] = useState({
    titulo: '',
    descripcion: '',
    fecha: '',
    hora: '',
    ubicacion: '',
    distanciaKm: '5',
  })
  const [error, setError] = useState<string | null>(null)
  const [aviso, mostrarAviso] = useAviso()
  const eventos = filtrarEventos([...creados, ...eventosMock], pestana, inscritos)

  const guardar = async () => {
    const err = validarEvento(form)
    if (err) return setError(err)
    const fecha = new Date(`${form.fecha}T${form.hora}`)
    const evento: Evento = {
      id: `u-${crearId()}`,
      titulo: form.titulo.trim(),
      descripcion: form.descripcion.trim() || 'Evento creado por la comunidad.',
      fecha: fecha.toLocaleDateString('es-EC', { weekday: 'long', day: 'numeric', month: 'long' }),
      hora: form.hora,
      ubicacion: form.ubicacion.trim(),
      participantes: 0,
      distanciaKm: Math.round(Number(form.distanciaKm)),
      pasado: false,
      creadoPorUsuario: true,
    }
    await crearEvento(evento)
    setCreando(false)
    setForm({ titulo: '', descripcion: '', fecha: '', hora: '', ubicacion: '', distanciaKm: '5' })
    setPestana('Mis eventos')
    mostrarAviso('Evento creado')
  }

  return (
    <div className="flex flex-col w-full">
      <div className="px-margin pt-space-md pb-space-sm">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Link
              to="/comunidad"
              aria-label="Volver al feed"
              className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary"
            >
              <span className="material-symbols-outlined">arrow_back</span>
            </Link>
            <h1 className="font-headline-md text-headline-md text-text-primary">Eventos</h1>
          </div>
          <button
            onClick={() => {
              setError(null)
              setCreando(true)
            }}
            aria-label="Crear evento"
            className="w-10 h-10 rounded-full bg-primary-600 flex items-center justify-center text-bg-primary hover:bg-primary-700 transition-colors shadow-lg"
          >
            <span className="material-symbols-outlined">add</span>
          </button>
        </div>

        <div className="flex gap-2 mb-4" role="tablist">
          {PESTANAS_EVENTOS.map((p) => (
            <button
              key={p}
              role="tab"
              aria-selected={pestana === p}
              onClick={() => setPestana(p)}
              className={`px-4 py-2 rounded-full font-label-md text-label-md ${pestana === p ? 'bg-primary-600 text-bg-primary' : 'bg-surface-container text-text-secondary hover:text-text-primary'}`}
            >
              {p}
            </button>
          ))}
        </div>
      </div>

      <div className="px-margin space-y-4 pb-space-3xl">
        {eventos.length === 0 && (
          <p className="text-center text-text-secondary py-8">
            {pestana === 'Mis eventos'
              ? 'No estás inscrito en eventos. Inscríbete o crea uno con el botón +.'
              : 'No hay eventos.'}
          </p>
        )}
        {eventos.map((evento) => {
          const inscrito = inscritos.includes(evento.id)
          return (
            <article
              key={evento.id}
              className="bg-surface-container rounded-2xl border border-border-subtle overflow-hidden hover:border-primary-600/50 transition-colors"
              data-testid="evento"
            >
              <Link to={`/comunidad/eventos/${evento.id}`} className="block">
                <div className="h-40 bg-gradient-to-br from-primary-600 to-secondary-600 relative overflow-hidden">
                  <div className="absolute inset-0 flex items-center justify-center">
                    <span className="material-symbols-outlined text-[80px] text-white/20">
                      event
                    </span>
                  </div>
                  <div className="absolute top-4 right-4 bg-black/60 backdrop-blur-md px-3 py-1.5 rounded-full">
                    <span className="text-white font-label-md text-label-md">
                      {evento.distanciaKm} km
                    </span>
                  </div>
                  {evento.creadoPorUsuario && (
                    <div className="absolute top-4 left-4 bg-black/60 px-3 py-1.5 rounded-full text-white text-label-md">
                      Organizas tú
                    </div>
                  )}
                </div>
                <div className="p-4 pb-0">
                  <h3 className="font-title-md text-title-md text-text-primary mb-2">
                    {evento.titulo}
                  </h3>
                  <div className="space-y-2 mb-3">
                    <div className="flex items-center gap-2 text-text-secondary">
                      <span className="material-symbols-outlined text-[18px] text-primary-500">
                        calendar_today
                      </span>
                      <span className="font-body-sm text-body-sm capitalize">
                        {evento.fecha}, {evento.hora}
                      </span>
                    </div>
                    <div className="flex items-center gap-2 text-text-secondary">
                      <span className="material-symbols-outlined text-[18px] text-secondary-500">
                        location_on
                      </span>
                      <span className="font-body-sm text-body-sm">{evento.ubicacion}</span>
                    </div>
                  </div>
                </div>
              </Link>
              <div className="flex items-center justify-between p-4 pt-3 border-t border-border-subtle mx-4 px-0">
                <span className="font-label-md text-label-md text-text-secondary">
                  {evento.participantes + (inscrito ? 1 : 0)} participantes
                </span>
                {!evento.pasado && (
                  <button
                    onClick={() => {
                      void alternar(evento.id)
                      mostrarAviso(inscrito ? 'Inscripción cancelada' : 'Te inscribiste')
                    }}
                    aria-pressed={inscrito}
                    className={`px-4 py-2 font-label-lg text-label-lg rounded-lg transition-colors ${inscrito ? 'bg-surface-container-high text-text-primary' : 'bg-primary-600 hover:bg-primary-700 text-bg-primary'}`}
                  >
                    {inscrito ? 'Inscrito' : 'Inscribirse'}
                  </button>
                )}
              </div>
            </article>
          )
        })}
      </div>

      <Dialogo
        titulo="Crear evento"
        abierto={creando}
        onCerrar={() => setCreando(false)}
        acciones={
          <>
            <BotonTexto onClick={() => setCreando(false)}>Cancelar</BotonTexto>
            <BotonTexto onClick={() => void guardar()}>Crear</BotonTexto>
          </>
        }
      >
        <input
          aria-label="Título del evento"
          placeholder="Título"
          className={campo}
          value={form.titulo}
          onChange={(e) => setForm({ ...form, titulo: e.target.value })}
        />
        <textarea
          aria-label="Descripción del evento"
          placeholder="Descripción (opcional)"
          rows={2}
          className={`${campo} h-auto py-2`}
          value={form.descripcion}
          onChange={(e) => setForm({ ...form, descripcion: e.target.value })}
        />
        <div className="grid grid-cols-2 gap-2">
          <input
            aria-label="Fecha"
            type="date"
            className={campo}
            value={form.fecha}
            onChange={(e) => setForm({ ...form, fecha: e.target.value })}
          />
          <input
            aria-label="Hora"
            type="time"
            className={campo}
            value={form.hora}
            onChange={(e) => setForm({ ...form, hora: e.target.value })}
          />
        </div>
        <input
          aria-label="Punto de encuentro"
          placeholder="Punto de encuentro"
          className={campo}
          value={form.ubicacion}
          onChange={(e) => setForm({ ...form, ubicacion: e.target.value })}
        />
        <input
          aria-label="Distancia en km"
          type="number"
          min={1}
          max={100}
          className={campo}
          value={form.distanciaKm}
          onChange={(e) => setForm({ ...form, distanciaKm: e.target.value })}
        />
        <MensajeError mensaje={error} />
      </Dialogo>
      <Aviso mensaje={aviso} />
    </div>
  )
}
