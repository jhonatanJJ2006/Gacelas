import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import { useActividad } from '../../contexts/actividad/api'
import { rankingSemanal } from '../../contexts/comunidad/domain/ranking'
import { useComunidad } from '../../contexts/comunidad/api'
import {
  horaIdeal,
  horaLoja,
  mejorVentana,
  useEstadoClima,
  type ClimaActual,
} from '../../contexts/clima/api'
import { useEventos } from '../../contexts/eventos/api'
import { useIdentidad } from '../../contexts/identidad/api'
import { useRutas } from '../../contexts/rutas/api'
import { haceTexto, nombreTipo, TIPOS_INCIDENTE } from '../../contexts/seguridad'
import type { Evento } from '../../mocks/data'
import {
  diaSemana,
  inicioSemana,
  medallas,
  mejorRacha,
  retoMes,
  totalesSemana,
} from '../../shared/kernel/estadisticas'
import type { CarreraGuardada } from '../../shared/kernel/tipos'
import { formatearRitmo, Unidades } from '../../shared/lib/geo'
import { Esqueleto } from '../../shared/ui/Esqueletos'
import {
  Anillo,
  AvatarIniciales,
  Barras,
  Cifra,
  EsqueletoPanel,
  MiniMapa,
  Panel,
  Progreso,
} from '../../shared/ui/Visual'
import { iconoClima } from '../BarraSuperior'
import { textoNivelSeguridad, useEnVivo, useSeguridadDeRutas, useSeguridadZona } from '../datosApp'
import { useMetaSemanal } from '../ShellEscritorio'
import { useRutaSugerida } from './SeccionesInicio'

const DIAS = ['L', 'M', 'X', 'J', 'V', 'S', 'D']
const DIAS_LARGOS = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo']
const dos = (n: number) => String(n).padStart(2, '0')
const hhmm = (h: number) => `${dos(h % 24)}:00`

/** Ritmo medio historico (s/km) del corredor o null si aun no hay carreras de 1 km o mas. */
export function ritmoHistorico(carreras: CarreraGuardada[]): number | null {
  const validas = carreras.filter((c) => c.distanciaKm >= 1 && c.segundos > 0)
  const km = validas.reduce((s, c) => s + c.distanciaKm, 0)
  return km > 0 ? Math.round(validas.reduce((s, c) => s + c.segundos, 0) / km) : null
}

/** Km por dia (lunes a domingo) de la semana que contiene `ms`. */
export function kmPorDiaSemana(carreras: CarreraGuardada[], ms: number): number[] {
  const desde = inicioSemana(ms)
  const hasta = desde + 7 * 86_400_000
  const km = Array(7).fill(0) as number[]
  for (const c of carreras)
    if (c.inicioMs >= desde && c.inicioMs < hasta) km[diaSemana(c.inicioMs)]! += c.distanciaKm
  return km
}

/* ---------- Clima para correr ---------- */

/** Fila de horas: temperatura y si es buena hora para correr. */
function HorasClima({ clima, max = 7 }: { clima: ClimaActual; max?: number }) {
  const ahora = Date.now()
  const horas = (clima.horas ?? []).filter((h) => h.ms + 3_600_000 > ahora).slice(0, max)
  if (!horas.length) return null
  return (
    <ul
      className="flex gap-1.5 overflow-x-auto no-scrollbar"
      aria-label="Pronóstico por horas"
      data-testid="clima-horas"
    >
      {horas.map((h) => {
        const ideal = horaIdeal(h)
        return (
          <li
            key={h.ms}
            className={`min-w-[52px] flex-1 rounded-md border px-1.5 py-2 text-center ${
              ideal ? 'border-c2-menta/40 bg-c2-menta/10' : 'border-c2-linea bg-c2-s2'
            }`}
          >
            <span className="block text-[10px] text-c2-muted">{hhmm(horaLoja(h.ms))}</span>
            <span className="block cifra-c2 text-[17px] text-white mt-0.5">{h.temperatura}°</span>
            <span
              className={`block text-[9.5px] mt-0.5 ${ideal ? 'text-c2-menta' : h.lluviaPct >= 50 ? 'text-c2-cielo' : 'text-c2-muted'}`}
            >
              {ideal ? 'ideal' : h.lluviaPct >= 50 ? 'lluvia' : `${h.lluviaPct}%`}
            </span>
          </li>
        )
      })}
    </ul>
  )
}

/** Texto de la mejor ventana del dia o null. */
export function textoVentana(clima: ClimaActual | null) {
  const v = mejorVentana(clima?.horas, Date.now())
  if (!v) return null
  return { rango: `${hhmm(v.desde)} - ${hhmm(v.hasta)}`, lluvia: v.lluvia }
}

export function PanelClima({ className = '' }: { className?: string }) {
  const e = useEstadoClima()
  if (e.estado === 'cargando')
    return <EsqueletoPanel alto="h-40" className={className} testId="esqueleto-clima" />
  if (e.estado === 'sin-datos')
    return (
      <Panel
        titulo="Clima para correr"
        id="clima-correr"
        className={className}
        testId="panel-clima"
      >
        <p className="text-[13px] text-c2-muted">
          No pudimos leer el clima de Open-Meteo. Revisa tu conexión.
        </p>
        <button
          type="button"
          onClick={e.reintentar}
          className="mt-3 h-9 px-4 rounded-md border border-c2-linea text-[13px] text-white hover:border-c2-morado"
        >
          Reintentar
        </button>
      </Panel>
    )
  const c = e.clima
  const v = textoVentana(c)
  return (
    <Panel
      titulo="Clima para correr"
      id="clima-correr"
      className={className}
      testId="panel-clima"
      subtitulo={`${c.lugar}, datos de Open-Meteo`}
    >
      <div className="flex items-center gap-4">
        <span className="w-14 h-14 rounded-md bg-c2-s2 grid place-items-center shrink-0">
          <span aria-hidden="true" className="material-symbols-outlined text-[30px] text-c2-cielo">
            {iconoClima(c.descripcion)}
          </span>
        </span>
        <div className="min-w-0 flex-1">
          <p className="cifra-c2 text-[40px] leading-none text-white">{c.temperatura}°</p>
          <p className="text-[12px] text-c2-muted mt-1 first-letter:uppercase">
            {c.descripcion}
            {c.sensacion != null ? `. Sensación ${c.sensacion}°` : ''}
          </p>
        </div>
        <ul className="text-[12px] text-c2-muted space-y-1 text-right shrink-0">
          {c.vientoKmh != null && <li>{c.vientoKmh} km/h viento</li>}
          {c.humedad != null && <li>{c.humedad}% humedad</li>}
          {c.uv != null && <li>UV {c.uv}</li>}
        </ul>
      </div>
      <div className="mt-4">
        <HorasClima clima={c} />
      </div>
      {v ? (
        <p
          className="mt-4 rounded-md border border-c2-menta/30 bg-c2-menta/10 px-3 py-2.5 text-[12.5px] text-white"
          data-testid="mejor-ventana"
        >
          <span className="font-bold text-c2-menta">Mejor ventana: {v.rango}.</span>
          {v.lluvia
            ? ` Probabilidad de lluvia del ${v.lluvia.pct}% desde las ${hhmm(v.lluvia.hora)}.`
            : ' Sin lluvia fuerte prevista.'}
        </p>
      ) : c.horas?.length ? (
        <p className="mt-4 text-[12.5px] text-c2-muted">
          Hoy no quedan horas ideales (8 a 21 °C y poca lluvia). Abrígate o elige un tramo corto.
        </p>
      ) : null}
    </Panel>
  )
}

/* ---------- Ruta segura del dia ---------- */

export function PanelRutaSegura({ className = '' }: { className?: string }) {
  const rutas = useRutas((s) => s.rutas)
  const carreras = useActividad((s) => s.carreras)
  const m = useIdentidad((s) => s.ajustes.unidadesMetricas)
  const elegirRutaActiva = useRutas((s) => s.elegirRutaActiva)
  const sugerida = useRutaSugerida(rutas, carreras)
  const seguridad = useSeguridadDeRutas()
  const ritmo = ritmoHistorico(carreras)
  if (sugerida === null)
    return <EsqueletoPanel alto="h-56" className={className} testId="esqueleto-ruta-segura" />
  if (sugerida.estado !== 'ok')
    return (
      <Panel
        titulo="Ruta segura del día"
        className={className}
        testId="ruta-sugerida"
        accion={{ texto: 'Ver rutas', to: '/rutas' }}
      >
        <p className="text-[13px] text-c2-muted" data-testid="ruta-sugerida-vacia">
          {sugerida.motivo || 'No encontramos la ruta de hoy.'} Explora Rutas para ver todas las
          opciones.
        </p>
      </Panel>
    )
  const r = sugerida.ruta
  const seg = seguridad?.get(r.id)
  const minutos = Math.round((r.distanciaKm * (ritmo ?? 390)) / 60)
  return (
    <Panel sinPadding className={`overflow-hidden ${className}`} testId="ruta-sugerida">
      <div className="relative">
        <MiniMapa
          puntos={r.puntos}
          ancho={520}
          alto={200}
          grosor={3.5}
          className="w-full h-[170px] lg:h-[200px]"
          etiqueta={`Mapa de ${r.nombre}`}
        />
        <span
          className="absolute left-3 top-3 text-[11px] font-semibold px-2 py-1 rounded bg-c2-bg/85 text-c2-menta flex items-center gap-1"
          data-testid="ruta-sugerida-segura"
        >
          <span aria-hidden="true" className="material-symbols-outlined text-[14px]">
            verified_user
          </span>
          {seguridad === null
            ? 'Consultando seguridad'
            : seg
              ? `${seg.reportes7d} ${seg.reportes7d === 1 ? 'incidente' : 'incidentes'} en 7 días`
              : sugerida.offline
                ? 'Segura según la última consulta'
                : 'Zona segura a esta hora'}
        </span>
      </div>
      <div className="p-5">
        <div className="flex items-start justify-between gap-3">
          <h2 className="titulo-panel !text-[22px] min-w-0">
            <Link to={`/rutas/${r.id}`} className="hover:text-c2-violeta">
              {r.nombre}
            </Link>
          </h2>
          {seg && (
            <span
              className="shrink-0 text-[11px] font-semibold text-c2-menta flex items-center gap-1 mt-1"
              title="Indicador con reportes de corredores y datos ECU 911 por zona"
            >
              <span aria-hidden="true" className="material-symbols-outlined text-[15px]">
                shield
              </span>
              {seg.puntaje != null ? `${seg.puntaje}/100` : textoNivelSeguridad(seg.nivel)}
            </span>
          )}
        </div>
        <p className="text-[12.5px] text-c2-muted mt-1 line-clamp-2">
          Elegida porque no la corriste en los últimos 3 días y la zona está tranquila a esta hora.
        </p>
        <div className="grid grid-cols-4 gap-3 mt-4">
          <Cifra
            rotulo="Distancia"
            valor={Unidades.texto(r.distanciaKm, m, 1)}
            unidad={Unidades.etiquetaDistancia(m)}
            tamano="text-[20px]"
          />
          <Cifra rotulo="Desnivel" valor={`+${r.elevacionM}`} unidad="m" tamano="text-[20px]" />
          <Cifra
            rotulo="Tiempo"
            valor={minutos}
            unidad="min"
            tamano="text-[20px]"
            nota={ritmo ? 'a tu ritmo' : 'a 6:30/km'}
          />
          <Cifra rotulo="Dificultad" valor={r.dificultad} tamano="text-[18px]" />
        </div>
        <Link
          to={`/rutas/buscar?ruta=${r.id}`}
          onClick={() => elegirRutaActiva(r.id)}
          className="panel-c2 mt-5 inline-flex h-10 px-5 items-center gap-2 bg-[linear-gradient(90deg,#7B1FA2,#A855F7)] text-white text-[13.5px] font-bold hover:brightness-110"
        >
          Correr esta ruta
        </Link>
      </div>
    </Panel>
  )
}

/* ---------- Tu semana ---------- */

export function PanelSemana({ className = '' }: { className?: string }) {
  const carreras = useActividad((s) => s.carreras)
  const m = useIdentidad((s) => s.ajustes.unidadesMetricas)
  const metaCarreras = useIdentidad((s) => s.ajustes.metaSemanal) ?? 3
  const meta = useMetaSemanal()
  const ahora = Date.now()
  const dias = useMemo(() => kmPorDiaSemana(carreras, ahora), [carreras]) // eslint-disable-line react-hooks/exhaustive-deps
  const pasada = useMemo(() => totalesSemana(carreras, ahora - 7 * 86_400_000), [carreras]) // eslint-disable-line react-hooks/exhaustive-deps
  const mejor = dias.reduce((b, v, i) => (v > (dias[b] ?? 0) ? i : b), 0)
  const hoy = diaSemana(ahora)
  const u = Unidades.etiquetaDistancia(m)
  return (
    <Panel
      titulo="Tu semana"
      subtitulo={meta.rango}
      className={className}
      testId="semana-atletica"
      accion={{ texto: 'Estadísticas', to: '/perfil' }}
    >
      <div className="flex items-center gap-5">
        <Anillo pct={meta.pct} />
        <dl className="flex-1 min-w-0 space-y-1.5 text-[12.5px]">
          <div className="flex justify-between gap-2">
            <dt className="text-c2-muted">Meta semanal</dt>
            <dd className="text-white font-semibold whitespace-nowrap">
              <span data-testid="km-semana">{Unidades.texto(meta.km, m, 1)}</span> /{' '}
              {Unidades.texto(meta.metaKm, m, 0)} {u}
            </dd>
          </div>
          <div className="flex justify-between gap-2">
            <dt className="text-c2-muted">Carreras</dt>
            <dd className="text-white font-semibold">
              {meta.carreras} / {metaCarreras}
            </dd>
          </div>
          <div className="flex justify-between gap-2">
            <dt className="text-c2-muted">Mejor día</dt>
            <dd className="text-white font-semibold truncate">
              {dias[mejor]! > 0
                ? `${DIAS_LARGOS[mejor]}, ${Unidades.texto(dias[mejor]!, m, 1)} ${u}`
                : 'Aún ninguno'}
            </dd>
          </div>
        </dl>
      </div>
      <Barras
        valores={dias.map((k) => Unidades.distancia(k, m))}
        etiquetas={DIAS}
        resaltar={dias[hoy]! > 0 ? hoy : mejor}
        alto={84}
        className="mt-5"
        testId="barras-semana"
      />
      <p className="mt-4 text-[12px] text-c2-muted">
        {pasada.carreras > 0
          ? `Semana pasada: ${Unidades.texto(pasada.km, m, 1)} ${u} en ${pasada.carreras} ${pasada.carreras === 1 ? 'carrera' : 'carreras'}.${
              pasada.km > 0
                ? ` Vas al ${Math.round((meta.km / pasada.km) * 100)}% de ese volumen.`
                : ''
            }`
          : 'La semana pasada no registraste carreras.'}
      </p>
    </Panel>
  )
}

/* ---------- Reto del mes y proximos logros ---------- */

export function PanelReto({ className = '' }: { className?: string }) {
  const carreras = useActividad((s) => s.carreras)
  const m = useIdentidad((s) => s.ajustes.unidadesMetricas)
  const ahora = Date.now()
  const reto = useMemo(() => retoMes(carreras, ahora), [carreras]) // eslint-disable-line react-hooks/exhaustive-deps
  const logros = useMemo(() => proximosLogros(carreras), [carreras])
  const faltan = Math.max(0, reto.metaKm - reto.km)
  const u = Unidades.etiquetaDistancia(m)
  return (
    <Panel variante="acento" className={className} testId="reto-mes">
      <div className="flex justify-between items-center">
        <span className="rotulo-c2 !text-c2-violeta">Reto del mes</span>
        <span className="text-[11px] text-c2-muted">
          Termina en {reto.diasRestantes} {reto.diasRestantes === 1 ? 'día' : 'días'}
        </span>
      </div>
      <h2 className="titulo-panel mt-1">{reto.titulo}</h2>
      <p className="mt-2 flex items-baseline gap-1">
        <span className="cifra-c2 text-[34px] leading-none text-white">
          {Unidades.texto(reto.km, m, 1)}
        </span>
        <span className="text-[12px] text-c2-muted">
          / {Unidades.texto(reto.metaKm, m, 0)} {u}
        </span>
      </p>
      <div className="mt-2">
        <Progreso pct={(reto.km / reto.metaKm) * 100} etiqueta="Progreso del reto" />
      </div>
      <p className="mt-2 text-[12px] text-c2-muted">
        {faltan > 0
          ? reto.diasRestantes > 0
            ? `Necesitas ${Unidades.texto(faltan / reto.diasRestantes, m, 1)} ${u} diarios para completarlo.`
            : `Te faltan ${Unidades.texto(faltan, m, 1)} ${u} hoy.`
          : 'Reto completado. Buen trabajo.'}
      </p>
      {logros.length > 0 && (
        <div className="mt-5 pt-4 border-t border-c2-linea">
          <span className="rotulo-c2">Próximos logros</span>
          <ul className="mt-2 space-y-3">
            {logros.map((l) => (
              <li key={l.id}>
                <div className="flex justify-between text-[12.5px]">
                  <span className="text-white truncate">{l.titulo}</span>
                  <span className="text-c2-muted shrink-0">{l.texto}</span>
                </div>
                <div className="mt-1">
                  <Progreso pct={l.pct} alto={4} etiqueta={l.titulo} />
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </Panel>
  )
}

/** Logros aun no conseguidos con su avance real (mismas medallas del Perfil). */
export function proximosLogros(carreras: CarreraGuardada[]) {
  const max = carreras.reduce((x, c) => Math.max(x, c.distanciaKm), 0)
  const total = carreras.reduce((s, c) => s + c.distanciaKm, 0)
  const racha = mejorRacha(carreras)
  const avance: Record<string, { v: number; meta: number; u: string }> = {
    primera: { v: carreras.length, meta: 1, u: '' },
    '5k': { v: max, meta: 5, u: 'km' },
    '10k': { v: max, meta: 10, u: 'km' },
    '21k': { v: max, meta: 21.1, u: 'km' },
    centenario: { v: total, meta: 100, u: 'km' },
    racha7: { v: racha, meta: 7, u: 'días' },
  }
  return medallas(carreras)
    .filter((md) => !md.lograda && avance[md.id])
    .map((md) => {
      const a = avance[md.id]!
      const fmt = (n: number) => (a.u === 'km' ? n.toFixed(1) : String(Math.floor(n)))
      return {
        id: md.id,
        titulo: md.titulo,
        pct: Math.min(100, (a.v / a.meta) * 100),
        texto: `${fmt(a.v)}/${a.meta}${a.u ? ` ${a.u}` : ''}`,
      }
    })
    .sort((a, b) => b.pct - a.pct)
    .slice(0, 3)
}

/* ---------- Actividad reciente ---------- */

const fechaCorta = (ms: number) => {
  const d = new Date(ms)
  const dias = Math.floor(
    (new Date().setHours(0, 0, 0, 0) - new Date(ms).setHours(0, 0, 0, 0)) / 86_400_000
  )
  const hora = `${dos(d.getHours())}:${dos(d.getMinutes())}`
  if (dias === 0) return `Hoy, ${hora}`
  if (dias === 1) return `Ayer, ${hora}`
  return `${d.toLocaleDateString('es-EC', { weekday: 'long', day: 'numeric', month: 'short' }).replace('.', '')}, ${hora}`
}

const duracion = (seg: number) => {
  const h = Math.floor(seg / 3600)
  const mi = Math.floor((seg % 3600) / 60)
  const s = seg % 60
  return h ? `${h}:${dos(mi)}:${dos(s)}` : `${mi}:${dos(s)}`
}

export function PanelActividad({ className = '' }: { className?: string }) {
  const carreras = useActividad((s) => s.carreras)
  const m = useIdentidad((s) => s.ajustes.unidadesMetricas)
  const recientes = useMemo(
    () => [...carreras].sort((a, b) => b.inicioMs - a.inicioMs).slice(0, 4),
    [carreras]
  )
  return (
    <Panel
      titulo="Actividad reciente"
      className={className}
      testId="actividad-reciente"
      accion={
        carreras.length ? { texto: `Ver todas (${carreras.length})`, to: '/perfil' } : undefined
      }
    >
      {recientes.length === 0 ? (
        <div className="text-[13px] text-c2-muted">
          <p>Aún no tienes carreras. La primera aparecerá aquí con su recorrido.</p>
          <Link to="/correr" className="inline-block mt-3 text-c2-violeta font-semibold">
            Registrar mi primera carrera
          </Link>
        </div>
      ) : (
        <ul className="divide-y divide-c2-linea -my-2">
          {recientes.map((c) => (
            <li key={c.id}>
              <Link
                to={`/perfil/carrera/${c.id}`}
                className="flex items-center gap-3 py-2.5 hover:bg-c2-s2/40 -mx-2 px-2 rounded"
              >
                <MiniMapa
                  puntos={c.puntos}
                  ancho={64}
                  alto={40}
                  grosor={2}
                  className="w-16 h-10 rounded shrink-0"
                />
                <span className="flex-1 min-w-0">
                  <span className="block text-[13.5px] font-semibold text-white truncate">
                    {c.titulo || 'Carrera'}
                  </span>
                  <span className="block text-[11.5px] text-c2-muted truncate first-letter:uppercase">
                    {fechaCorta(c.inicioMs)}
                  </span>
                </span>
                <span className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-right shrink-0">
                  <span>
                    <span className="block cifra-c2 text-[16px] text-white">
                      {Unidades.texto(c.distanciaKm, m, 1)}
                    </span>
                    <span className="block text-[9.5px] text-c2-muted">
                      {Unidades.etiquetaDistancia(m)}
                    </span>
                  </span>
                  <span className="hidden sm:block">
                    <span className="block cifra-c2 text-[16px] text-white">
                      {duracion(c.segundos)}
                    </span>
                    <span className="block text-[9.5px] text-c2-muted">tiempo</span>
                  </span>
                  <span>
                    <span className="block cifra-c2 text-[16px] text-c2-menta">
                      {formatearRitmo(
                        Unidades.ritmo(c.distanciaKm > 0.05 ? c.segundos / c.distanciaKm : null, m)
                      )}
                    </span>
                    <span className="block text-[9.5px] text-c2-muted">
                      {Unidades.etiquetaRitmo(m)}
                    </span>
                  </span>
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </Panel>
  )
}

/* ---------- En ruta ahora ---------- */

export function PanelEnRuta({ className = '' }: { className?: string }) {
  const corredores = useEnVivo((s) => s.corredores)
  const cargando = useEnVivo((s) => s.cargando)
  const publicaciones = useComunidad((s) => s.publicaciones)
  const m = useIdentidad((s) => s.ajustes.unidadesMetricas)
  const amigos = publicaciones.filter((p) => p.sigo && !p.propia).slice(0, 3)
  return (
    <Panel
      titulo="En ruta ahora"
      className={className}
      testId="amigos-en-ruta"
      accion={{ texto: 'Mapa en vivo', to: '/comunidad/mapa' }}
    >
      {cargando ? (
        <div role="status" aria-busy="true" className="space-y-2">
          <span className="sr-only">Cargando corredores en vivo</span>
          <Esqueleto className="h-24 w-full rounded" />
          <Esqueleto className="h-8 w-full rounded" />
        </div>
      ) : corredores.length > 0 ? (
        <>
          <MiniMapa
            puntos={[]}
            marcadores={corredores.map((c) => ({
              lat: c.lat,
              lng: c.lng,
              color: '#68DBA9',
              titulo: c.nombre,
            }))}
            ancho={360}
            alto={110}
            className="w-full h-[110px] rounded"
            etiqueta="Corredores compartiendo su ubicación"
          />
          <ul className="mt-3 space-y-2">
            {corredores.slice(0, 4).map((c) => (
              <li key={c.userId}>
                <Link
                  to={`/comunidad/en-vivo/${c.userId}`}
                  className="flex items-center gap-3 hover:bg-c2-s2/40 rounded px-1 py-1"
                >
                  <AvatarIniciales nombre={c.nombre} tamano={30} />
                  <span className="flex-1 min-w-0 text-[13px] text-white truncate">{c.nombre}</span>
                  <span className="text-[11.5px] text-c2-menta shrink-0">
                    {c.corriendo ? 'Corriendo' : 'En pausa'}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </>
      ) : (
        <div>
          <p className="text-[13px] text-c2-muted">Nadie comparte su ubicación en este momento.</p>
          {amigos.length > 0 && (
            <>
              <span className="rotulo-c2 mt-4">Últimas de quienes sigues</span>
              <ul className="mt-2 space-y-2">
                {amigos.map((p) => (
                  <li key={p.id} className="flex items-center gap-3">
                    <AvatarIniciales nombre={p.autor} tamano={30} />
                    <span className="flex-1 min-w-0">
                      <span className="block text-[13px] text-white truncate">{p.autor}</span>
                      <span className="block text-[11px] text-c2-muted truncate">
                        {Unidades.texto(p.distanciaKm, m, 1)} {Unidades.etiquetaDistancia(m)} en{' '}
                        {p.titulo}
                      </span>
                    </span>
                    <span className="text-[11px] text-c2-muted shrink-0">
                      {haceTiempoCorto(p.inicioMs)}
                    </span>
                  </li>
                ))}
              </ul>
            </>
          )}
          {amigos.length === 0 && (
            <Link
              to="/comunidad/corredores"
              className="inline-block mt-3 text-[13px] font-semibold text-c2-violeta"
            >
              Buscar corredores para seguir
            </Link>
          )}
        </div>
      )}
    </Panel>
  )
}

const haceTiempoCorto = (ms: number) => {
  const h = Math.round((Date.now() - ms) / 3_600_000)
  return h < 1 ? 'ahora' : h < 24 ? `hace ${h} h` : `hace ${Math.round(h / 24)} d`
}

/* ---------- Ranking semanal ---------- */

export function PanelRanking({ className = '', max = 5 }: { className?: string; max?: number }) {
  const carreras = useActividad((s) => s.carreras)
  const remotas = useComunidad((s) => s.publicaciones)
  const sesion = useIdentidad((s) => s.sesion)
  const m = useIdentidad((s) => s.ajustes.unidadesMetricas)
  const filas = rankingSemanal(carreras, sesion?.nombre ?? 'Tú', remotas, Date.now()).slice(0, max)
  const yo = filas.findIndex((f) => f.propio)
  const delante = yo > 0 ? filas[yo - 1] : undefined
  return (
    <Panel
      titulo="Ranking semanal"
      subtitulo="Tú y quienes sigues"
      className={className}
      testId="panel-ranking"
      accion={{ texto: 'Ver todo', to: '/comunidad' }}
    >
      <ol className="space-y-1">
        {filas.map((f, i) => (
          <li
            key={`${f.nombre}-${i}`}
            className={`flex items-center gap-2.5 py-1.5 px-2 rounded ${f.propio ? 'bg-c2-morado/15 border border-c2-morado/40' : ''}`}
          >
            <span
              className={`cifra-c2 w-4 text-[14px] ${i === 0 ? 'text-amber-400' : 'text-c2-muted'}`}
            >
              {i + 1}
            </span>
            <AvatarIniciales nombre={f.nombre} tamano={28} />
            <span
              className={`flex-1 min-w-0 truncate text-[12.5px] ${f.propio ? 'font-bold text-white' : 'text-white'}`}
            >
              {f.nombre}
              {f.propio ? ' (tú)' : ''}
            </span>
            <span className="cifra-c2 text-[14px] text-white">{Unidades.texto(f.km, m, 1)}</span>
          </li>
        ))}
      </ol>
      <p className="mt-3 text-[11.5px] text-c2-muted">
        {filas.length <= 1
          ? 'Sigue a otros corredores para competir en el ranking.'
          : delante
            ? `${Unidades.texto(delante.km - filas[yo]!.km + 0.1, m, 1)} ${Unidades.etiquetaDistancia(m)} más y alcanzas el puesto ${yo}.`
            : yo === 0
              ? 'Vas primero esta semana.'
              : ''}
      </p>
    </Panel>
  )
}

/* ---------- Proximos eventos ---------- */

const MES_CORTO = [
  'ene',
  'feb',
  'mar',
  'abr',
  'may',
  'jun',
  'jul',
  'ago',
  'sep',
  'oct',
  'nov',
  'dic',
]
const DIA_CORTO = ['dom', 'lun', 'mar', 'mié', 'jue', 'vie', 'sáb']

export function TarjetaEventoC2({
  evento,
  className = '',
}: {
  evento: Evento
  className?: string
}) {
  const inscritos = useEventos((s) => s.inscritos)
  const alternar = useEventos((s) => s.alternarInscripcion)
  const inscrito = inscritos.includes(evento.id)
  const d = evento.inicioMs ? new Date(evento.inicioMs) : null
  const lleno = evento.cupo != null && evento.participantes >= evento.cupo && !inscrito
  return (
    <article
      className={`panel-c2 bg-c2-s2/60 border border-c2-linea p-3 flex gap-3 min-w-[250px] lg:min-w-0 ${className}`}
      data-testid="tarjeta-evento"
    >
      <div
        className={`w-12 shrink-0 rounded flex flex-col items-center justify-center py-1.5 ${inscrito ? 'bg-[linear-gradient(180deg,#A855F7,#7B1FA2)]' : 'bg-c2-card border border-c2-linea'}`}
      >
        {d ? (
          <>
            <span className="text-[9px] font-bold uppercase text-white/80">
              {DIA_CORTO[d.getDay()]}
            </span>
            <span className="cifra-c2 text-[22px] leading-none text-white">{d.getDate()}</span>
            <span className="text-[9px] font-bold uppercase text-white/80">
              {MES_CORTO[d.getMonth()]}
            </span>
          </>
        ) : (
          <span aria-hidden="true" className="material-symbols-outlined text-c2-violeta">
            event
          </span>
        )}
      </div>
      <div className="min-w-0 flex-1">
        <h3 className="text-[13.5px] font-bold text-white leading-snug line-clamp-2">
          <Link to={`/comunidad/eventos/${evento.id}`} className="hover:text-c2-violeta">
            {evento.titulo}
          </Link>
        </h3>
        <p className="text-[11.5px] text-c2-muted truncate">
          {evento.ubicacion} · {evento.hora}
        </p>
        <p className="text-[11.5px] text-c2-violeta">{evento.distanciaKm} km</p>
        <div className="mt-2 flex items-center justify-between gap-2">
          <span className="text-[11px] text-c2-muted">
            {evento.participantes + (inscrito ? 1 : 0)} van
          </span>
          <button
            type="button"
            disabled={lleno}
            onClick={() => void alternar(evento.id)}
            aria-pressed={inscrito}
            className={`text-[11px] font-bold px-2 py-1 rounded ${inscrito ? 'bg-c2-menta/15 text-c2-menta' : 'text-c2-violeta hover:text-white'} disabled:opacity-50`}
          >
            {inscrito ? 'Inscrita' : lleno ? 'Cupo lleno' : 'Inscribirme'}
          </button>
        </div>
      </div>
    </article>
  )
}

export function PanelEventos({
  eventos,
  className = '',
}: {
  eventos: Evento[]
  className?: string
}) {
  return (
    <Panel
      titulo={
        <>
          Próximos eventos<span className="hidden sm:inline"> en Loja</span>
        </>
      }
      className={className}
      testId="proximos-eventos"
      accion={{ texto: 'Ver calendario', to: '/comunidad/eventos' }}
    >
      {eventos.length === 0 ? (
        <p className="text-[13px] text-c2-muted">
          No hay eventos próximos.{' '}
          <Link to="/comunidad/eventos" className="text-c2-violeta font-semibold">
            Crea uno
          </Link>
        </p>
      ) : (
        <div className="flex gap-3 overflow-x-auto no-scrollbar lg:grid lg:grid-cols-2 ancho:grid-cols-3 lg:overflow-visible">
          {eventos.slice(0, 3).map((e, i) => (
            <TarjetaEventoC2
              key={e.id}
              evento={e}
              className={i === 2 ? 'lg:hidden ancho:flex' : ''}
            />
          ))}
        </div>
      )}
    </Panel>
  )
}

/* ---------- Seguridad en tu zona ---------- */

const COLOR_TIPO: Record<string, string> = {
  robo_asalto: '#BE185D',
  acoso: '#BE185D',
  accidente: '#BE185D',
  perros_agresivos: '#F59E0B',
  iluminacion_deficiente: '#96CCFF',
  trafico_peligroso: '#C084FC',
  via_mal_estado: '#F59E0B',
}

export function PanelSeguridad({ className = '' }: { className?: string }) {
  const e = useSeguridadZona()
  if (e.cargando)
    return <EsqueletoPanel alto="h-40" className={className} testId="esqueleto-seguridad" />
  const recientes = e.incidentes
    .filter((i) => Date.now() - i.ocurridoMs < 7 * 86_400_000)
    .slice(0, 4)
  const ecu = e.resumen?.ecu ?? []
  return (
    <Panel
      titulo="Seguridad en tu zona"
      className={className}
      testId="seguridad-zona"
      accion={{ texto: 'Ver mapa', to: '/comunidad/mapa' }}
    >
      <div className="grid gap-4 sm:grid-cols-[minmax(0,1fr)_minmax(0,1.2fr)]">
        <MiniMapa
          puntos={[]}
          marcadores={recientes.map((i) => ({
            lat: i.lat,
            lng: i.lng,
            color: COLOR_TIPO[i.tipo] ?? '#F59E0B',
            titulo: nombreTipo(i.tipo),
          }))}
          ancho={260}
          alto={130}
          className="hidden sm:block w-full h-[130px] rounded"
          etiqueta="Incidentes reportados en los últimos 7 días"
        />
        {recientes.length === 0 ? (
          <p className="text-[13px] text-c2-muted self-center">
            Sin reportes de corredores en los últimos 7 días. Si ves algo, repórtalo desde el mapa.
          </p>
        ) : (
          <ul className="space-y-2.5">
            {recientes.map((i) => (
              <li key={i.id} className="flex gap-2.5">
                <span
                  aria-hidden="true"
                  className="mt-1.5 w-2 h-2 rounded-full shrink-0"
                  style={{ background: COLOR_TIPO[i.tipo] ?? '#F59E0B' }}
                />
                <span className="min-w-0">
                  <span className="block text-[13px] font-semibold text-white">
                    <span
                      aria-hidden="true"
                      className="material-symbols-outlined text-[14px] align-[-2px] mr-1 text-c2-muted"
                    >
                      {TIPOS_INCIDENTE.find((t) => t.id === i.tipo)?.icono ?? 'warning'}
                    </span>
                    {nombreTipo(i.tipo)}
                  </span>
                  <span className="block text-[11.5px] text-c2-muted">
                    {haceTexto(i.ocurridoMs)} · {i.confirmaciones}{' '}
                    {i.confirmaciones === 1 ? 'confirmación' : 'confirmaciones'}
                  </span>
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
      <div className="mt-4 pt-3 border-t border-c2-linea">
        {ecu.length > 0 ? (
          <div className="grid grid-cols-3 gap-3">
            {ecu.slice(0, 3).map((x) => (
              <Cifra
                key={x.servicio}
                rotulo={x.servicio}
                valor={x.conteo}
                tamano="text-[20px]"
                nota={`ECU 911, ${e.resumen?.mesEcu?.slice(0, 7) ?? ''}`}
              />
            ))}
          </div>
        ) : (
          <p className="text-[11.5px] text-c2-muted">
            Datos ECU 911 por parroquia: aún no cargados. Indicador orientativo; ante una emergencia
            llama al 911.
          </p>
        )}
      </div>
    </Panel>
  )
}
