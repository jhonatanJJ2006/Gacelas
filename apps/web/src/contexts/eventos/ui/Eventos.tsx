import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useAviso } from '../../../hooks'
import { eventosMock, type Evento } from '../../../mocks/data'
import { ModalC2, ResumenModal } from '../../../shared/ui/modales'
import { Aviso, BotonTexto, Dialogo, MensajeError } from '../../../shared/ui/ui'
import { crearId } from '../../actividad/api'
import { useComunidad } from '../../comunidad/api'
import { useRutas } from '../../rutas/api'
import { TIPOS_EVENTO, unirEventos, type TipoEvento } from '../api'
import { useEventos } from '../application/estado'
import { filtrarEventos, PESTANAS_EVENTOS, type PestanaEventos } from '../domain/filtros'
import { validarEvento } from '../domain/validacion'
import { CalendarioEventos } from './CalendarioEventos'

const campo =
  'w-full h-11 px-3 bg-surface-container border border-border-subtle rounded-lg text-text-primary placeholder-text-secondary'

export function Eventos() {
  const creados = useEventos((s) => s.eventosCreados)
  const inscritos = useEventos((s) => s.inscritos)
  const crearEvento = useEventos((s) => s.crearEvento)
  const alternar = useEventos((s) => s.alternarInscripcion)
  const [pestana, setPestana] = useState<PestanaEventos>('Próximos')
  const [params, setParams] = useSearchParams()
  const clubId = params.get('club')
  const [creando, setCreando] = useState(() => !!clubId)
  const [vista, setVista] = useState<'lista' | 'calendario'>('lista')
  const rutas = useRutas((s) => s.rutas)
  const [extra, setExtra] = useState({ tipo: 'grupal' as TipoEvento, cupo: '', rutaId: '' })
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
  const [uniendo, setUniendo] = useState<Evento | null>(null)
  const feedSocial = useComunidad((s) => s.feedSocial)
  const todos = feedSocial ? creados : unirEventos(creados, eventosMock)
  const [tipo, setTipo] = useState<TipoEvento | null>(null)
  const eventos = filtrarEventos(todos, pestana, inscritos).filter((e) => !tipo || e.tipo === tipo)

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
      inicioMs: fecha.getTime(),
      tipo: extra.tipo,
      cupo: extra.cupo ? Math.max(1, Math.round(Number(extra.cupo))) : null,
      rutaId: extra.rutaId || null,
      clubId,
    }
    try {
      await crearEvento(evento)
    } catch (e) {
      return setError(e instanceof Error ? e.message : 'No se pudo crear el evento')
    }
    if (clubId) setParams({}, { replace: true })
    setExtra({ tipo: 'grupal', cupo: '', rutaId: '' })
    setCreando(false)
    setForm({ titulo: '', descripcion: '', fecha: '', hora: '', ubicacion: '', distanciaKm: '5' })
    setPestana('Mis eventos')
    mostrarAviso('Evento creado')
  }

  return (
    <div className="flex flex-col w-full contenedor-escritorio">
      <div className="px-margin pt-space-md pb-space-sm lg:px-0 lg:pb-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Link
              to="/comunidad"
              aria-label="Volver al feed"
              className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary"
            >
              <span className="material-symbols-outlined">arrow_back</span>
            </Link>
            <h1 className="font-display italic uppercase text-[26px] leading-tight text-text-primary">
              Eventos
            </h1>
          </div>
          <div className="flex items-center gap-2 ml-auto mr-2">
            <Link
              to="/comunidad/clubes"
              className="h-10 px-3 rounded-full bg-surface-container text-text-primary flex items-center gap-1"
              data-testid="ir-clubes"
            >
              <span className="material-symbols-outlined text-[20px]">groups</span>Clubes
            </Link>
            <button
              onClick={() => setVista(vista === 'lista' ? 'calendario' : 'lista')}
              aria-pressed={vista === 'calendario'}
              data-testid="ver-calendario"
              aria-label={vista === 'lista' ? 'Ver calendario' : 'Ver lista'}
              className="w-10 h-10 rounded-full bg-surface-container text-text-primary flex items-center justify-center"
            >
              <span className="material-symbols-outlined">
                {vista === 'lista' ? 'calendar_month' : 'list'}
              </span>
            </button>
          </div>
          <button
            onClick={() => {
              setError(null)
              setCreando(true)
            }}
            aria-label="Crear evento"
            className="w-10 h-10 rounded-full bg-primary-600 flex items-center justify-center text-on-primary hover:bg-primary-700 transition-colors shadow-lg"
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
              className={`px-4 py-2 rounded-full font-label-md text-label-md ${pestana === p ? 'bg-primary-600 text-on-primary' : 'bg-surface-container text-text-secondary hover:text-text-primary'}`}
            >
              {p}
            </button>
          ))}
        </div>
        <div className="flex gap-2 overflow-x-auto no-scrollbar" aria-label="Filtrar por tipo">
          {(Object.keys(TIPOS_EVENTO) as TipoEvento[]).map((t) => (
            <button
              key={t}
              aria-pressed={tipo === t}
              onClick={() => setTipo(tipo === t ? null : t)}
              className={`px-3 py-1.5 rounded-lg border text-label-md whitespace-nowrap ${tipo === t ? 'border-primary-600 bg-primary-600/15 text-primary' : 'border-border-subtle text-text-secondary'}`}
            >
              {TIPOS_EVENTO[t]}
            </button>
          ))}
        </div>
      </div>

      {vista === 'calendario' && (
        <div className="px-margin lg:px-0 pb-6">
          <CalendarioEventos eventos={todos} inscritos={inscritos} />
        </div>
      )}
      <div className="px-margin space-y-4 pb-space-3xl lg:px-0 lg:grid lg:grid-cols-2 xl:grid-cols-3 lg:gap-6 lg:space-y-0">
        {eventos.length === 0 && (
          <p className="text-center text-text-secondary py-8 lg:col-span-full">
            {pestana === 'Mis eventos'
              ? 'No estás inscrito en eventos. Inscríbete o crea uno con el botón +.'
              : 'No hay eventos.'}
          </p>
        )}
        {eventos.map((evento, i) => {
          const inscrito = inscritos.includes(evento.id)
          const participantes = evento.participantes + (inscrito ? 1 : 0)
          const ruta = evento.rutaId ? rutas.find((r) => r.id === evento.rutaId) : undefined
          const inscribir = !evento.pasado && (
            <button
              onClick={() => {
                if (!inscrito) return setUniendo(evento)
                void alternar(evento.id)
                mostrarAviso('Inscripción cancelada')
              }}
              aria-pressed={inscrito}
              className={`px-4 py-2 font-label-lg text-label-lg rounded-lg transition-colors flex items-center gap-1 ${inscrito ? 'bg-surface-container-high text-text-primary' : 'bg-primary-600 hover:bg-primary-700 text-on-primary'}`}
            >
              {i === 0 && (
                <span className="material-symbols-outlined text-[18px]">check_circle</span>
              )}
              {inscrito ? 'Inscrito' : 'Inscribirse'}
            </button>
          )
          const lleno = evento.cupo != null && participantes >= evento.cupo
          const fecha = evento.inicioMs != null ? new Date(evento.inicioMs) : null
          if (i === 0)
            return (
              <article
                key={evento.id}
                className="relative bg-surface-container rounded-2xl border border-primary-600/40 overflow-hidden lg:col-span-full"
                data-testid="evento"
              >
                <div className="h-1 bg-gradient-to-r from-primary-600 via-secondary-500 to-success" />
                <div className="p-4 space-y-3">
                  <div className="flex items-center justify-between gap-2">
                    <span className="px-2 py-0.5 rounded-md border border-primary-600/50 text-primary text-label-sm flex items-center gap-1">
                      <span className="material-symbols-outlined text-[14px]">star</span>Evento
                      principal
                    </span>
                    <span
                      className={`px-2 py-0.5 rounded-md text-label-sm ${evento.pasado ? 'bg-surface-container-high text-text-secondary' : 'bg-success/15 text-success'}`}
                    >
                      {evento.pasado
                        ? 'Finalizado'
                        : lleno
                          ? 'Lista de espera'
                          : 'Inscripción abierta'}
                    </span>
                  </div>
                  <Link to={`/comunidad/eventos/${evento.id}`} className="block space-y-1">
                    <h2 className="font-headline-sm text-headline-sm text-text-primary">
                      {evento.titulo}
                    </h2>
                    {evento.creadoPorUsuario && (
                      <span className="text-label-sm text-primary">Organizas tú</span>
                    )}
                    <p className="flex items-center gap-2 text-text-secondary text-body-sm capitalize">
                      <span className="material-symbols-outlined text-[18px] text-primary">
                        schedule
                      </span>
                      {evento.fecha} • {evento.hora}
                    </p>
                    <p className="flex items-center gap-2 text-text-secondary text-body-sm">
                      <span className="material-symbols-outlined text-[18px] text-primary">
                        location_on
                      </span>
                      {evento.ubicacion}
                    </p>
                  </Link>
                  <div className="grid grid-cols-3 gap-2" data-testid="evento-metricas">
                    {[
                      ['Distancia', `${evento.distanciaKm} km`, 'text-primary'],
                      ['Tipo', TIPOS_EVENTO[evento.tipo ?? 'grupal'], 'text-text-primary'],
                      [
                        'Desnivel',
                        ruta ? `+${Math.round(ruta.elevacionM)} m` : '--',
                        'text-success',
                      ],
                    ].map(([t, v, c]) => (
                      <div key={t} className="rounded-xl bg-surface-container-high p-2 text-center">
                        <div className="text-label-sm text-text-secondary">{t}</div>
                        <div className={`font-title-sm text-title-sm ${c}`}>{v}</div>
                      </div>
                    ))}
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="font-label-md text-label-md text-text-secondary">
                      {participantes} participantes
                    </span>
                    {inscribir}
                  </div>
                </div>
              </article>
            )
          return (
            <article
              key={evento.id}
              className="tarjeta-c2 p-4 space-y-3 hover:border-primary-600/50 transition-colors"
              data-testid="evento"
            >
              {i === 1 && <h3 className="sr-only">Más eventos locales</h3>}
              <Link to={`/comunidad/eventos/${evento.id}`} className="flex gap-3">
                <div className="w-14 shrink-0 rounded-xl border border-border-subtle bg-surface-container-high flex flex-col items-center justify-center py-1">
                  <span className="text-label-sm text-primary uppercase">
                    {fecha
                      ? fecha.toLocaleDateString('es-EC', { weekday: 'short' }).replace('.', '')
                      : '--'}
                  </span>
                  <span className="font-headline-sm text-headline-sm text-text-primary leading-none">
                    {fecha ? fecha.getDate() : '--'}
                  </span>
                  <span className="text-label-sm text-text-secondary uppercase">
                    {fecha
                      ? fecha.toLocaleDateString('es-EC', { month: 'short' }).replace('.', '')
                      : ''}
                  </span>
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-start justify-between gap-2">
                    <span className="text-body-sm text-text-secondary truncate">
                      {evento.hora} • {evento.ubicacion}
                    </span>
                    <span className="px-2 py-0.5 rounded-md bg-primary-600/15 text-primary text-label-sm shrink-0">
                      {TIPOS_EVENTO[evento.tipo ?? 'grupal']}
                    </span>
                  </div>
                  <h2 className="font-title-md text-title-md text-text-primary">{evento.titulo}</h2>
                  {evento.creadoPorUsuario && (
                    <span className="text-label-sm text-primary">Organizas tú</span>
                  )}
                  <p className="text-body-sm text-text-secondary">
                    {evento.distanciaKm} km
                    {ruta ? ` • +${Math.round(ruta.elevacionM)} m desnivel` : ''}
                  </p>
                </div>
              </Link>
              {evento.cupo != null && (
                <div>
                  <div className="flex justify-between text-label-sm text-text-secondary mb-1">
                    <span>Cupos confirmados</span>
                    <span className="text-primary">
                      {Math.min(participantes, evento.cupo)}/{evento.cupo} cupos
                    </span>
                  </div>
                  <div className="h-1.5 rounded-full bg-surface-container-highest overflow-hidden">
                    <div
                      className="h-full bg-primary-600"
                      style={{ width: `${Math.min(100, (participantes / evento.cupo) * 100)}%` }}
                    />
                  </div>
                </div>
              )}
              <div className="flex items-center justify-between">
                <span className="font-label-md text-label-md text-text-secondary">
                  {participantes} participantes
                </span>
                {inscribir || (
                  <Link
                    to={`/comunidad/eventos/${evento.id}`}
                    className="px-4 py-2 rounded-lg border border-border-subtle text-text-primary text-label-lg"
                  >
                    Ver detalles
                  </Link>
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
        <div className="grid grid-cols-2 gap-2">
          <select
            aria-label="Tipo de evento"
            className={campo}
            value={extra.tipo}
            onChange={(e) => setExtra({ ...extra, tipo: e.target.value as TipoEvento })}
          >
            {Object.entries(TIPOS_EVENTO).map(([k, v]) => (
              <option key={k} value={k}>
                {v}
              </option>
            ))}
          </select>
          <input
            aria-label="Cupo máximo (opcional)"
            type="number"
            min={1}
            max={5000}
            placeholder="Cupo (opcional)"
            className={campo}
            value={extra.cupo}
            onChange={(e) => setExtra({ ...extra, cupo: e.target.value })}
          />
        </div>
        <select
          aria-label="Ruta planificada (opcional)"
          className={campo}
          value={extra.rutaId}
          onChange={(e) => setExtra({ ...extra, rutaId: e.target.value })}
        >
          <option value="">Sin ruta planificada</option>
          {rutas.map((r) => (
            <option key={r.id} value={r.id}>
              {r.nombre}
            </option>
          ))}
        </select>
        {clubId && <p className="text-primary-400">Se publicará como evento del club.</p>}
        <MensajeError mensaje={error} />
      </Dialogo>
      <ModalC2
        abierto={uniendo != null}
        icono="event"
        titulo="Unirse al evento"
        texto={
          uniendo
            ? `${uniendo.titulo} · ${uniendo.fecha} ${uniendo.hora} · ${uniendo.ubicacion}`
            : ''
        }
        primario="Inscribirme"
        onPrimario={() => {
          if (!uniendo) return
          void alternar(uniendo.id)
          mostrarAviso(`Te inscribiste a ${uniendo.titulo}`)
          setUniendo(null)
        }}
        onCerrar={() => setUniendo(null)}
        secundario="Ahora no"
      >
        {uniendo && (
          <ResumenModal
            datos={[
              [`${uniendo.distanciaKm} km`, 'Distancia'],
              [String(uniendo.participantes), 'Inscritos'],
              [uniendo.cupo != null ? String(uniendo.cupo) : 'Libre', 'Cupo'],
            ]}
          />
        )}
      </ModalC2>
      <Aviso mensaje={aviso} />
    </div>
  )
}
