import { useEffect, useState, type ReactNode } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useAviso } from '../../../hooks'
import { eventosMock, type Evento } from '../../../mocks/data'
import { ModalC2, ResumenModal } from '../../../shared/ui/modales'
import { MiniMapa, Panel, Progreso } from '../../../shared/ui/Visual'
import { useEsEscritorio } from '../../../shared/lib/useEsEscritorio'
import { Aviso, BotonTexto, Dialogo, MensajeError } from '../../../shared/ui/ui'
import { crearId } from '../../actividad/api'
import { useComunidad } from '../../comunidad/api'
import { useRutas } from '../../rutas/api'
import type { Ruta } from '../../../mocks/data'
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

  const proximos = todos
    .filter((e) => !e.pasado)
    .sort((x, y) => (x.inicioMs ?? Infinity) - (y.inicioMs ?? Infinity))
  const principal = pestana === 'Próximos' && !tipo ? eventos[0] : undefined
  const resto = (principal ? eventos.filter((e) => e !== principal) : eventos).sort((x, y) =>
    pestana === 'Pasados'
      ? (y.inicioMs ?? 0) - (x.inicioMs ?? 0)
      : (x.inicioMs ?? Infinity) - (y.inicioMs ?? Infinity)
  )
  const conteos: Record<PestanaEventos, number> = {
    Próximos: filtrarEventos(todos, 'Próximos', inscritos).length,
    'Mis eventos': filtrarEventos(todos, 'Mis eventos', inscritos).length,
    Pasados: filtrarEventos(todos, 'Pasados', inscritos).length,
  }
  const tresSemanas = proximos.filter(
    (e) => e.inicioMs != null && e.inicioMs < Date.now() + 21 * 86_400_000
  ).length
  const miProximo = proximos.find((e) => inscritos.includes(e.id))
  const accion = (evento: Evento, grande = false) => {
    const inscrito = inscritos.includes(evento.id)
    if (evento.pasado)
      return (
        <Link
          to={`/comunidad/eventos/${evento.id}`}
          className="h-9 px-4 rounded-md border border-c2-linea text-white text-[13px] font-semibold inline-flex items-center justify-center hover:border-c2-morado"
        >
          Ver detalles
        </Link>
      )
    return (
      <button
        onClick={() => {
          if (!inscrito) return setUniendo(evento)
          void alternar(evento.id)
          mostrarAviso('Inscripción cancelada')
        }}
        aria-pressed={inscrito}
        className={`${grande ? 'panel-c2 h-12 px-6 text-[14px]' : 'h-9 px-4 rounded-md text-[13px]'} font-bold inline-flex items-center justify-center gap-1.5 transition-colors ${inscrito ? 'bg-c2-menta/15 text-c2-menta border border-c2-menta/40' : 'bg-[linear-gradient(90deg,#7B1FA2,#A855F7)] text-white hover:brightness-110'}`}
      >
        {inscrito ? 'Inscrito' : 'Inscribirse'}
      </button>
    )
  }
  const escritorio = useEsEscritorio()
  const calendario = <CalendarioEventos eventos={todos} inscritos={inscritos} />

  return (
    <div className="flex flex-col w-full px-4 lg:px-8 lg:max-w-[1440px] lg:mx-auto pt-space-md lg:pt-0 pb-space-3xl lg:pb-10">
      <div className="flex flex-wrap items-end gap-3 mb-5">
        <div className="flex-1 min-w-0">
          <h1 className="titular-c2 text-[30px] lg:text-[34px] text-white leading-none">Eventos</h1>
          <p className="text-[12.5px] text-c2-muted mt-1">
            {tresSemanas === 1 ? '1 evento' : `${tresSemanas} eventos`} en las próximas 3 semanas en
            Loja
          </p>
        </div>
        <Link
          to="/comunidad/clubes"
          aria-label="Clubes"
          data-testid="ir-clubes"
          className="h-10 px-3 rounded-md border border-c2-linea text-white text-[13px] font-semibold inline-flex items-center gap-1.5 hover:border-c2-morado"
        >
          <span aria-hidden="true" className="material-symbols-outlined text-[18px]">
            groups
          </span>
          <span className="hidden sm:inline">Clubes</span>
        </Link>
        <button
          onClick={() => setVista(vista === 'lista' ? 'calendario' : 'lista')}
          aria-pressed={vista === 'calendario'}
          data-testid="ver-calendario"
          aria-label={vista === 'lista' ? 'Ver calendario' : 'Ver lista'}
          className="lg:hidden h-10 w-10 rounded-md border border-c2-linea text-white inline-flex items-center justify-center"
        >
          <span aria-hidden="true" className="material-symbols-outlined text-[18px]">
            {vista === 'lista' ? 'calendar_month' : 'list'}
          </span>
        </button>
        <button
          onClick={() => {
            setError(null)
            setCreando(true)
          }}
          aria-label="Crear evento"
          className="panel-c2 h-10 px-4 bg-[linear-gradient(90deg,#7B1FA2,#A855F7)] text-white text-[13px] font-bold inline-flex items-center gap-1.5"
        >
          <span aria-hidden="true" className="material-symbols-outlined text-[18px]">
            add
          </span>
          <span className="hidden sm:inline">Crear evento</span>
        </button>
      </div>

      {principal && (
        <HeroEvento
          evento={principal}
          participantes={principal.participantes + (inscritos.includes(principal.id) ? 1 : 0)}
          ruta={principal.rutaId ? rutas.find((r) => r.id === principal.rutaId) : undefined}
          accion={accion(principal, true)}
        />
      )}

      <div className="mt-5 flex flex-col lg:grid lg:grid-cols-[minmax(0,1fr)_320px] gap-5 items-start">
        <div className="min-w-0 w-full">
          <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
            <div
              className="flex gap-1 p-1 rounded-md bg-c2-card border border-c2-linea"
              role="tablist"
            >
              {PESTANAS_EVENTOS.map((p) => (
                <button
                  key={p}
                  role="tab"
                  aria-selected={pestana === p}
                  onClick={() => setPestana(p)}
                  className={`h-8 px-3 rounded text-[13px] font-semibold whitespace-nowrap ${pestana === p ? 'bg-c2-s2 text-white' : 'text-c2-muted hover:text-white'}`}
                >
                  {p}
                  <span aria-hidden="true" className="text-c2-muted">
                    {` · ${conteos[p]}`}
                  </span>
                </button>
              ))}
            </div>
            <div
              className="flex gap-1.5 overflow-x-auto no-scrollbar max-w-full"
              aria-label="Filtrar por tipo"
            >
              {(Object.keys(TIPOS_EVENTO) as TipoEvento[]).map((t) => (
                <button
                  key={t}
                  aria-pressed={tipo === t}
                  onClick={() => setTipo(tipo === t ? null : t)}
                  className={`h-8 px-3 rounded-md border text-[12.5px] font-semibold whitespace-nowrap ${tipo === t ? 'border-c2-morado bg-c2-morado/20 text-white' : 'border-c2-linea text-c2-muted hover:text-white'}`}
                >
                  {TIPOS_EVENTO[t]}
                </button>
              ))}
            </div>
          </div>

          {vista === 'calendario' && !escritorio && <div className="mb-5">{calendario}</div>}

          {eventos.length === 0 && (
            <p className="panel-c2 bg-c2-card border border-c2-linea p-6 text-center text-[13px] text-c2-muted">
              {pestana === 'Mis eventos'
                ? 'No estás inscrito en eventos. Inscríbete o crea uno con el botón Crear evento.'
                : pestana === 'Pasados'
                  ? 'Todavía no hay eventos pasados.'
                  : 'No hay eventos próximos con este filtro.'}
            </p>
          )}
          {agruparPorSemana(resto).map(([grupo, lista]) => (
            <section key={grupo} className="mb-5" aria-label={grupo}>
              <h2 className="rotulo-c2 mb-2">{grupo}</h2>
              <div className="space-y-3">
                {lista.map((evento) => (
                  <FilaEvento
                    key={evento.id}
                    evento={evento}
                    participantes={evento.participantes + (inscritos.includes(evento.id) ? 1 : 0)}
                    ruta={evento.rutaId ? rutas.find((r) => r.id === evento.rutaId) : undefined}
                    accion={accion(evento)}
                  />
                ))}
              </div>
            </section>
          ))}
        </div>

        <aside className="w-full space-y-5 lg:sticky lg:top-[88px]">
          {escritorio && calendario}
          {escritorio && (
            <Panel titulo="Tu próximo evento" testId="mi-proximo-evento">
              {miProximo ? (
                <Link to={`/comunidad/eventos/${miProximo.id}`} className="block group">
                  <p className="text-[14px] font-semibold text-white group-hover:text-c2-violeta">
                    {miProximo.titulo}
                  </p>
                  <p className="text-[12px] text-c2-muted">
                    {mayuscula(miProximo.fecha)} · {miProximo.hora}
                  </p>
                  <p className="text-[12px] text-c2-muted">{miProximo.ubicacion}</p>
                  {miProximo.inicioMs != null && (
                    <p className="mt-2 text-[12px] text-c2-menta font-semibold">
                      {textoFalta(miProximo.inicioMs)}
                    </p>
                  )}
                </Link>
              ) : (
                <p className="text-[13px] text-c2-muted">
                  Aún no te inscribes a ningún evento próximo.
                </p>
              )}
            </Panel>
          )}
        </aside>
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

const DIA_MS = 86_400_000
const mayuscula = (t: string) => t.charAt(0).toUpperCase() + t.slice(1)

/** Lunes 00:00 (hora local) de la semana del instante. */
function lunes(ms: number) {
  const d = new Date(ms)
  d.setHours(0, 0, 0, 0)
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7))
  return d.getTime()
}

/** Agrupa por semana relativa a hoy: Esta semana, Próxima semana, Semana pasada o la fecha del lunes. */
function agruparPorSemana(eventos: Evento[], ahora = Date.now()): [string, Evento[]][] {
  const base = lunes(ahora)
  const grupos = new Map<string, Evento[]>()
  for (const e of eventos) {
    let nombre = 'Sin fecha'
    if (e.inicioMs != null) {
      const dif = Math.round((lunes(e.inicioMs) - base) / (7 * DIA_MS))
      nombre =
        dif === 0
          ? 'Esta semana'
          : dif === 1
            ? 'Próxima semana'
            : dif === -1
              ? 'Semana pasada'
              : `Semana del ${new Date(lunes(e.inicioMs)).toLocaleDateString('es-EC', { day: 'numeric', month: 'long' })}`
    }
    grupos.set(nombre, [...(grupos.get(nombre) ?? []), e])
  }
  return [...grupos.entries()]
}

function textoFalta(ms: number, ahora = Date.now()) {
  const d = ms - ahora
  if (d <= 0) return 'Ya empezó'
  const dias = Math.floor(d / DIA_MS)
  const horas = Math.floor((d % DIA_MS) / 3_600_000)
  if (dias === 0) return horas === 0 ? 'Empieza en menos de una hora' : `Faltan ${horas} h`
  return `Faltan ${dias} ${dias === 1 ? 'día' : 'días'} y ${horas} h`
}

function useAhora(cadaMs = 60_000) {
  const [ahora, setAhora] = useState(() => Date.now())
  useEffect(() => {
    const t = setInterval(() => setAhora(Date.now()), cadaMs)
    return () => clearInterval(t)
  }, [cadaMs])
  return ahora
}

function BloqueFecha({ ms, destacado }: { ms?: number; destacado?: boolean }) {
  const f = ms != null ? new Date(ms) : null
  return (
    <div
      className={`w-14 shrink-0 rounded-md flex flex-col items-center justify-center py-1.5 ${destacado ? 'bg-[linear-gradient(160deg,#7B1FA2,#A855F7)] text-white' : 'bg-c2-s2 text-white'}`}
    >
      <span className="text-[10px] font-bold uppercase tracking-wider">
        {f ? f.toLocaleDateString('es-EC', { weekday: 'short' }).replace('.', '') : 'Fecha'}
      </span>
      <span className="cifra-c2 text-[24px] leading-none">{f ? f.getDate() : '--'}</span>
      <span className="text-[10px] uppercase text-white/70">
        {f ? f.toLocaleDateString('es-EC', { month: 'short' }).replace('.', '') : ''}
      </span>
    </div>
  )
}

function HeroEvento({
  evento,
  participantes,
  ruta,
  accion,
}: {
  evento: Evento
  participantes: number
  ruta?: Ruta
  accion: ReactNode
}) {
  const ahora = useAhora()
  const falta = Math.max(0, (evento.inicioMs ?? ahora) - ahora)
  const lleno = evento.cupo != null && participantes >= evento.cupo
  return (
    <article
      data-testid="evento"
      className="panel-c2 relative overflow-hidden border border-[#3B1A55] bg-[linear-gradient(160deg,#2A0A3D_0%,#1A0B2E_55%,#191F2F_100%)] lg:grid lg:grid-cols-[minmax(0,1fr)_38%]"
    >
      <div className="p-5 lg:p-7 min-w-0">
        <div className="flex flex-wrap gap-1.5 mb-3">
          <span className="text-[11px] font-semibold px-2 py-1 rounded-full bg-amber-400/15 text-amber-400">
            Evento principal
          </span>
          <span className="text-[11px] font-semibold px-2 py-1 rounded-full bg-c2-menta/15 text-c2-menta">
            {lleno ? 'Lista de espera' : 'Inscripción abierta'}
          </span>
          <span className="text-[11px] font-semibold px-2 py-1 rounded-full bg-c2-s2 text-c2-muted">
            {TIPOS_EVENTO[evento.tipo ?? 'grupal']}
          </span>
        </div>
        <Link to={`/comunidad/eventos/${evento.id}`} className="block">
          <h2 className="titular-c2 text-[28px] lg:text-[40px] leading-[1.02] text-white hover:text-c2-violeta">
            {evento.titulo}
          </h2>
        </Link>
        {evento.creadoPorUsuario && (
          <span className="text-[12px] text-c2-violeta font-semibold">Organizas tú</span>
        )}
        <p className="mt-2 text-[13px] text-c2-muted">
          {mayuscula(evento.fecha)} · {evento.hora} · {evento.ubicacion}
        </p>
        <div
          className="mt-5 flex flex-wrap items-end gap-x-6 gap-y-4"
          data-testid="evento-metricas"
        >
          {evento.inicioMs != null && (
            <div className="flex gap-2" aria-label={textoFalta(evento.inicioMs, ahora)}>
              {[
                [Math.floor(falta / DIA_MS), 'días'],
                [Math.floor((falta % DIA_MS) / 3_600_000), 'horas'],
                [Math.floor((falta % 3_600_000) / 60_000), 'min'],
              ].map(([v, t]) => (
                <div
                  key={t}
                  className="w-14 rounded-md bg-c2-bg/70 border border-c2-linea py-1.5 text-center"
                >
                  <div className="cifra-c2 text-[24px] leading-none text-white">
                    {String(v).padStart(2, '0')}
                  </div>
                  <div className="text-[10px] text-c2-muted">{t}</div>
                </div>
              ))}
            </div>
          )}
          {[
            [`${evento.distanciaKm} km`, 'distancia'],
            [ruta ? `+${Math.round(ruta.elevacionM)} m` : 'Sin ruta', 'desnivel'],
            [
              evento.cupo != null ? `${participantes} / ${evento.cupo}` : String(participantes),
              'inscritos',
            ],
          ].map(([v, t]) => (
            <div key={t}>
              <div className="cifra-c2 text-[22px] leading-none text-white">{v}</div>
              <div className="text-[11px] text-c2-muted">{t}</div>
            </div>
          ))}
        </div>
        <div className="mt-6 flex flex-wrap items-center gap-3">
          {accion}
          <Link
            to={ruta ? `/rutas/${ruta.id}` : `/comunidad/eventos/${evento.id}`}
            className="h-12 px-5 rounded-md border border-c2-linea text-white text-[14px] font-semibold inline-flex items-center hover:border-c2-morado"
          >
            {ruta ? 'Ver recorrido' : 'Ver detalles'}
          </Link>
          <span className="text-[12px] text-c2-muted">{participantes} participantes</span>
        </div>
      </div>
      {ruta?.puntos?.length ? (
        <div className="hidden lg:block relative border-l border-c2-linea mapa-c2">
          <MiniMapa
            puntos={ruta.puntos}
            ancho={420}
            alto={300}
            grosor={4}
            className="absolute inset-0 w-full h-full"
          />
        </div>
      ) : null}
    </article>
  )
}

function FilaEvento({
  evento,
  participantes,
  ruta,
  accion,
}: {
  evento: Evento
  participantes: number
  ruta?: Ruta
  accion: ReactNode
}) {
  const hoy =
    evento.inicioMs != null &&
    new Date(evento.inicioMs).toDateString() === new Date().toDateString()
  return (
    <article
      data-testid="evento"
      className={`panel-c2 bg-c2-card border border-c2-linea p-3 sm:p-4 flex flex-wrap sm:flex-nowrap items-center gap-3 sm:gap-4 hover:border-c2-morado/60 transition-colors ${evento.pasado ? 'opacity-80' : ''}`}
    >
      <BloqueFecha ms={evento.inicioMs} destacado={hoy} />
      {ruta?.puntos?.length ? (
        <MiniMapa
          puntos={ruta.puntos}
          ancho={96}
          alto={64}
          grosor={2.5}
          className="hidden md:block w-24 h-16 rounded shrink-0"
        />
      ) : null}
      <Link to={`/comunidad/eventos/${evento.id}`} className="min-w-0 flex-1 group">
        <span className="inline-block text-[10.5px] font-semibold px-2 py-0.5 rounded-full bg-c2-morado/20 text-c2-violeta mb-1">
          {TIPOS_EVENTO[evento.tipo ?? 'grupal']}
        </span>
        <h3 className="font-display italic text-[18px] leading-tight text-white group-hover:text-c2-violeta truncate">
          {evento.titulo}
        </h3>
        {evento.creadoPorUsuario && (
          <span className="text-[11.5px] text-c2-violeta font-semibold">Organizas tú</span>
        )}
        <p className="text-[12px] text-c2-muted truncate">
          {evento.hora} · {evento.ubicacion}
        </p>
      </Link>
      <div className="hidden sm:flex gap-4 shrink-0 text-right">
        <div>
          <div className="cifra-c2 text-[18px] text-c2-violeta leading-none">
            {evento.distanciaKm} km
          </div>
          <div className="text-[10.5px] text-c2-muted">distancia</div>
        </div>
        {ruta && (
          <div>
            <div className="cifra-c2 text-[18px] text-c2-cielo leading-none">
              +{Math.round(ruta.elevacionM)} m
            </div>
            <div className="text-[10.5px] text-c2-muted">desnivel</div>
          </div>
        )}
      </div>
      <div className="w-full sm:w-[180px] shrink-0 flex sm:flex-col items-center sm:items-stretch gap-2">
        <div className="flex-1 sm:flex-none min-w-0">
          <div className="flex justify-between text-[11px] text-c2-muted mb-1">
            <span>{participantes} participantes</span>
            {evento.cupo != null && (
              <span>
                {Math.min(participantes, evento.cupo)}/{evento.cupo} cupos
              </span>
            )}
          </div>
          {evento.cupo != null && (
            <Progreso
              pct={Math.min(100, (participantes / evento.cupo) * 100)}
              alto={4}
              etiqueta="Cupos confirmados"
            />
          )}
        </div>
        {accion}
      </div>
    </article>
  )
}
