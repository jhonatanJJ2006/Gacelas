import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { usuarioActualMock, type Ruta } from '../../../../mocks/data'
import {
  inicioSemana,
  kmMensual,
  medallas,
  records,
  type RecordPersonal,
} from '../../../../shared/kernel/estadisticas'
import { progresoNivel } from '../../../../shared/kernel/nivel'
import { rachaEnRiesgo } from '../../../../shared/kernel/rachas'
import type { CarreraGuardada } from '../../../../shared/kernel/tipos'
import { formatearHms, formatearMs, formatearRitmo, Unidades } from '../../../../shared/lib/geo'
import { BotonTexto, Dialogo } from '../../../../shared/ui/ui'
import {
  Barras,
  diaLoja,
  MapaCalor,
  MiniMapa,
  Panel,
  Progreso,
  Rotulo,
} from '../../../../shared/ui/Visual'
import { useRacha } from '../../../actividad'
import { useActividad } from '../../../actividad/api'
import { ListaLogros } from '../../../comunidad'
import { useComunidad } from '../../../comunidad/api'
import { useRutas } from '../../../rutas/api'
import { useIdentidad } from '../../application/estado'
import { Medallero } from './SeccionesPerfil'

export function FotoPerfil({
  foto,
  nombre,
  className = 'w-20 h-20',
}: {
  foto: string | null | undefined
  nombre: string
  className?: string
}) {
  if (foto)
    return (
      <img
        src={foto}
        alt="Foto de perfil"
        className={`${className} rounded-full object-cover border-2 border-primary-600`}
      />
    )
  return (
    <div
      className={`${className} rounded-full bg-gradient-to-br from-primary-600 to-primary-400 border-2 border-primary-600 flex items-center justify-center text-on-primary text-2xl font-bold`}
      aria-label="Foto de perfil"
    >
      {nombre.charAt(0).toUpperCase()}
    </div>
  )
}

const PERIODOS = [
  ['4 sem', 4],
  ['12 sem', 12],
  ['Año', 52],
  ['Todo', 0],
] as const
type Periodo = (typeof PERIODOS)[number][0]
const PESTANAS = ['Resumen', 'Actividades', 'Logros'] as const
type Pestana = (typeof PESTANAS)[number]
const POR_PAGINA = 10
const SEMANA_MS = 7 * 86_400_000

const fechaCorta = (ms: number) =>
  new Date(ms)
    .toLocaleDateString('es-EC', { day: 'numeric', month: 'short', year: 'numeric' })
    .replace(/\./g, '')
const plural = (n: number, uno: string, varios: string) => `${n} ${n === 1 ? uno : varios}`

/** Carreras del periodo elegido (semanas hacia atras desde hoy; 0 = todas). */
function carrerasDelPeriodo(c: CarreraGuardada[], semanas: number, ahora = Date.now()) {
  if (!semanas) return c
  const desde = inicioSemana(ahora) - (semanas - 1) * SEMANA_MS
  return c.filter((x) => x.inicioMs >= desde)
}

/** Km por semana de las ultimas `n` semanas (la ultima es la actual). */
function kmPorSemanas(c: CarreraGuardada[], n: number, ahora = Date.now()) {
  const actual = inicioSemana(ahora)
  return Array.from({ length: n }, (_, i) => {
    const desde = actual - (n - 1 - i) * SEMANA_MS
    const d = new Date(desde)
    return {
      etiqueta: `${d.getDate()}/${d.getMonth() + 1}`,
      km: c
        .filter((x) => x.inicioMs >= desde && x.inicioMs < desde + SEMANA_MS)
        .reduce((s, x) => s + x.distanciaKm, 0),
    }
  })
}

/** Perfil redisenado: cabecera con nivel, pestanas, cifras por periodo y actividades paginadas. */
export function Perfil() {
  const sesion = useIdentidad((s) => s.sesion)
  const carreras = useActividad((s) => s.carreras)
  const m = useIdentidad((s) => s.ajustes.unidadesMetricas)
  const eliminar = useActividad((s) => s.eliminarCarrera)
  const [borrar, setBorrar] = useState<string | null>(null)
  const [pestana, setPestana] = useState<Pestana>('Resumen')
  const [periodo, setPeriodo] = useState<Periodo>('12 sem')
  const [pagina, setPagina] = useState(0)
  const nombre = sesion?.nombre ?? usuarioActualMock.nombre
  const seguidores = useComunidad((s) => s.seguidores)
  const siguiendo = useComunidad((s) => s.siguiendo)
  const refrescarFeed = useComunidad((s) => s.refrescarFeed)
  useEffect(() => {
    if (sesion) void refrescarFeed()
  }, [sesion, refrescarFeed])
  const ordenadas = useMemo(() => [...carreras].sort((a, b) => b.inicioMs - a.inicioMs), [carreras])
  const totalKm = carreras.reduce((s, c) => s + c.distanciaKm, 0)
  const nivel = progresoNivel(totalKm)
  const meds = useMemo(() => medallas(carreras), [carreras])
  const paginas = Math.max(1, Math.ceil(ordenadas.length / POR_PAGINA))
  const pag = Math.min(pagina, paginas - 1)
  const desde = new Date(ordenadas.at(-1)?.inicioMs ?? Date.now())
  const irPagina = (n: number) => {
    setPagina(n)
    document
      .querySelector('[data-testid="tabla-actividades"]')
      ?.scrollIntoView?.({ block: 'start' })
  }

  return (
    <div className="flex flex-col w-full px-4 lg:px-8 lg:max-w-[1440px] lg:mx-auto pb-space-3xl lg:pb-10 pt-space-md lg:pt-0">
      <section
        aria-label="Tu perfil"
        className="panel-c2 relative overflow-hidden border border-[#3B1A55] bg-[linear-gradient(160deg,#2A0A3D_0%,#1A0B2E_55%,#191F2F_100%)] p-5 lg:p-6"
      >
        <div className="flex flex-col lg:flex-row lg:items-center gap-5">
          <div className="flex items-center gap-4 min-w-0 flex-1">
            <FotoPerfil foto={sesion?.foto} nombre={nombre} className="w-20 h-20 shrink-0" />
            <div className="min-w-0 flex-1">
              <h1 className="font-display italic text-[28px] lg:text-[32px] leading-tight text-white truncate tracking-[0.035em]">
                {nombre}
              </h1>
              <p className="text-[12.5px] text-c2-muted truncate">
                {[`@${sesion?.username ?? usuarioActualMock.username}`, sesion?.ubicacion]
                  .filter(Boolean)
                  .join(' · ')}
              </p>
              {sesion?.bio && (
                <p className="text-[12.5px] text-c2-muted line-clamp-2">{sesion.bio}</p>
              )}
              <div className="mt-2 flex flex-wrap items-center gap-2">
                <span
                  className="text-[11px] font-semibold px-2 py-1 rounded-full bg-c2-morado/25 text-c2-violeta"
                  data-testid="nivel-perfil"
                >
                  {nivel.nivel}
                </span>
                <span className="text-[11px] text-c2-muted">
                  {nivel.siguiente
                    ? `${Unidades.texto(nivel.faltanKm, m, 0)} ${Unidades.etiquetaDistancia(m)} para ${nivel.siguiente}`
                    : 'Nivel máximo'}
                </span>
              </div>
              <div className="mt-2 max-w-[360px]">
                <Progreso pct={nivel.pct} alto={5} etiqueta="Progreso al siguiente nivel" />
              </div>
            </div>
          </div>
          <div className="grid grid-cols-3 gap-4 lg:px-6 lg:border-x border-c2-linea">
            {[
              ['Carreras', carreras.length, 'total-carreras'],
              ['Seguidores', seguidores, 'total-seguidores'],
              ['Siguiendo', siguiendo.length, 'total-siguiendo'],
            ].map(([t, v, id]) => (
              <div key={t} className="text-center">
                <div className="cifra-c2 text-[26px] leading-none text-white" data-testid={id}>
                  {v}
                </div>
                <div className="text-[11px] text-c2-muted mt-1">{t}</div>
              </div>
            ))}
          </div>
          <div className="grid grid-cols-[1fr_1fr_auto] lg:grid-cols-1 gap-2 lg:w-[200px]">
            <Link
              to="/perfil/avatar"
              className="panel-c2 h-10 flex items-center justify-center gap-1.5 bg-[linear-gradient(90deg,#7B1FA2,#A855F7)] text-white text-[13px] font-bold"
            >
              Editar avatar 3D
            </Link>
            <Link
              to="/perfil/editar"
              aria-label="Editar perfil"
              className="h-10 rounded-md flex items-center justify-center gap-1.5 border border-c2-linea text-white text-[13px] hover:border-c2-morado"
            >
              Editar perfil
            </Link>
            <Link
              to="/ajustes"
              aria-label="Configuración"
              className="h-10 w-10 lg:w-auto rounded-md flex items-center justify-center gap-1.5 border border-c2-linea text-c2-muted text-[13px] hover:text-white hover:border-c2-morado"
            >
              <span aria-hidden="true" className="material-symbols-outlined text-[18px]">
                settings
              </span>
              <span className="hidden lg:inline">Configuración</span>
            </Link>
          </div>
        </div>
      </section>

      <div className="mt-5 mb-4 flex flex-wrap items-center justify-between gap-3">
        <div
          className="flex gap-1 p-1 rounded-md bg-c2-card border border-c2-linea overflow-x-auto no-scrollbar max-w-full"
          role="tablist"
          aria-label="Secciones del perfil"
        >
          {PESTANAS.map((p) => (
            <button
              key={p}
              role="tab"
              aria-selected={p === pestana}
              onClick={() => setPestana(p)}
              className={`h-8 px-3 rounded text-[13px] font-semibold whitespace-nowrap ${p === pestana ? 'bg-c2-s2 text-white' : 'text-c2-muted hover:text-white'}`}
            >
              {p}
              {p !== 'Resumen' && (
                <span className="hidden sm:inline">
                  {` (${p === 'Actividades' ? carreras.length : meds.filter((x) => x.lograda).length})`}
                </span>
              )}
            </button>
          ))}
          <Link
            to="/perfil/reportes"
            data-testid="ir-reportes"
            className="h-8 px-3 rounded text-[13px] font-semibold whitespace-nowrap text-c2-muted hover:text-white flex items-center"
          >
            Reportes
          </Link>
        </div>
        {pestana === 'Resumen' && (
          <div
            className="flex gap-1 p-1 rounded-md bg-c2-card border border-c2-linea"
            role="group"
            aria-label="Periodo"
          >
            {PERIODOS.map(([p]) => (
              <button
                key={p}
                aria-pressed={p === periodo}
                onClick={() => setPeriodo(p)}
                className={`h-7 px-2.5 rounded text-[12px] font-semibold ${p === periodo ? 'bg-c2-s2 text-white' : 'text-c2-muted hover:text-white'}`}
              >
                {p}
              </button>
            ))}
          </div>
        )}
      </div>

      {pestana === 'Resumen' && (
        <Resumen
          carreras={ordenadas}
          periodo={periodo}
          m={m}
          onBorrar={setBorrar}
          onVerTodas={() => setPestana('Actividades')}
          desde={desde}
        />
      )}
      {pestana === 'Actividades' && (
        <Panel
          titulo="Todas tus actividades"
          subtitulo={`${carreras.length} en total, ${POR_PAGINA} por página`}
          testId="tabla-actividades"
        >
          <TablaActividades
            lista={ordenadas.slice(pag * POR_PAGINA, (pag + 1) * POR_PAGINA)}
            m={m}
            onBorrar={setBorrar}
          />
          {paginas > 1 && (
            <nav
              className="mt-4 flex items-center justify-between text-[12.5px]"
              aria-label="Páginas de actividades"
            >
              <button
                type="button"
                disabled={pag === 0}
                onClick={() => irPagina(pag - 1)}
                className="h-9 px-3 rounded-md border border-c2-linea text-white disabled:opacity-40"
              >
                Anterior
              </button>
              <span className="text-c2-muted">
                Página {pag + 1} de {paginas}
              </span>
              <button
                type="button"
                disabled={pag >= paginas - 1}
                onClick={() => irPagina(pag + 1)}
                className="h-9 px-3 rounded-md border border-c2-linea text-white disabled:opacity-40"
              >
                Siguiente
              </button>
            </nav>
          )}
        </Panel>
      )}
      {pestana === 'Logros' && (
        <div className="space-y-5">
          <Panel titulo="Medallas" testId="panel-medallas">
            <Medallero lista={meds} />
          </Panel>
          <ListaLogros />
        </div>
      )}

      <Dialogo
        titulo="¿Eliminar carrera?"
        abierto={borrar != null}
        onCerrar={() => setBorrar(null)}
        acciones={
          <>
            <BotonTexto onClick={() => setBorrar(null)}>Cancelar</BotonTexto>
            <BotonTexto
              peligro
              onClick={() => {
                if (borrar) void eliminar(borrar)
                setBorrar(null)
              }}
            >
              Eliminar
            </BotonTexto>
          </>
        }
      >
        <p>Esta acción no se puede deshacer.</p>
      </Dialogo>
    </div>
  )
}

function Resumen({
  carreras,
  periodo,
  m,
  onBorrar,
  onVerTodas,
  desde,
}: {
  carreras: CarreraGuardada[]
  periodo: Periodo
  m: boolean
  onBorrar: (id: string) => void
  onVerTodas: () => void
  desde: Date
}) {
  const semanas = PERIODOS.find(([p]) => p === periodo)![1]
  const ahora = Date.now()
  const del = useMemo(() => carrerasDelPeriodo(carreras, semanas, ahora), [carreras, semanas]) // eslint-disable-line react-hooks/exhaustive-deps
  const km = del.reduce((s, c) => s + c.distanciaKm, 0)
  const seg = del.reduce((s, c) => s + c.segundos, 0)
  const desn = del.reduce((s, c) => s + c.desnivelM, 0)
  const u = Unidades.etiquetaDistancia(m)
  const recs = useMemo(() => records(carreras), [carreras])
  const meds = useMemo(() => medallas(carreras), [carreras])
  const nSem = semanas === 4 ? 4 : semanas === 12 ? 12 : 0
  const barras = nSem
    ? kmPorSemanas(carreras, nSem, ahora)
    : kmMensual(
        carreras,
        ahora,
        semanas === 52
          ? 12
          : Math.min(24, Math.max(6, Math.ceil((ahora - desde.getTime()) / (30 * 86_400_000)) + 1))
      ).map((x) => ({ etiqueta: x.etiqueta, km: x.km }))
  const mayor = barras.reduce((b, x) => (x.km > b.km ? x : b), { etiqueta: '', km: 0 })
  const diasActivos = new Set(del.map((c) => diaLoja(c.inicioMs))).size
  const diasPeriodo = semanas
    ? semanas * 7
    : Math.max(1, Math.ceil((ahora - desde.getTime()) / 86_400_000))
  const textoPeriodo =
    periodo === 'Todo'
      ? 'desde el inicio'
      : periodo === 'Año'
        ? 'últimas 52 semanas'
        : `últimas ${semanas} semanas`

  return (
    <div className="flex flex-col gap-5 lg:grid lg:grid-cols-12">
      <section
        className="lg:col-span-12"
        data-testid="estadisticas-historicas"
        aria-label="Cifras del periodo"
      >
        <p className="text-[12px] text-c2-muted mb-2">
          {del.length} carreras, {textoPeriodo}
        </p>
        <div className="grid grid-cols-2 xl:grid-cols-4 gap-3">
          {[
            ['Distancia', Unidades.texto(km, m, 1), u, 'text-c2-violeta'],
            ['Tiempo en movimiento', formatearHms(seg), '', 'text-white'],
            ['Desnivel acumulado', `+${Math.round(desn)}`, 'm', 'text-c2-cielo'],
            [
              'Ritmo medio',
              formatearRitmo(Unidades.ritmo(km > 0.01 ? seg / km : null, m)),
              Unidades.etiquetaRitmo(m),
              'text-c2-menta',
            ],
          ].map(([t, v, un, color]) => (
            <div key={t} className="panel-c2 bg-c2-card border border-c2-linea p-4 min-w-0">
              <Rotulo>{t}</Rotulo>
              <p className="mt-1 flex items-baseline gap-1 whitespace-nowrap">
                <span className={`cifra-c2 text-[26px] lg:text-[30px] leading-none ${color}`}>
                  {v}
                </span>
                {un && <span className="text-[12px] text-c2-muted">{un}</span>}
              </p>
            </div>
          ))}
        </div>
      </section>

      <Panel
        titulo="Volumen"
        subtitulo={nSem ? 'Km por semana' : 'Km por mes'}
        className="lg:col-span-8"
        testId="km-mensual"
      >
        {carreras.length === 0 ? (
          <p className="text-[13px] text-c2-muted">
            Tus kilómetros aparecerán aquí cuando guardes carreras.
          </p>
        ) : (
          <>
            <Barras
              valores={barras.map((b) => Unidades.distancia(b.km, m))}
              etiquetas={barras.map((b) => b.etiqueta)}
              resaltar={barras.length - 1}
              alto={150}
              className="!gap-1"
            />
            <div className="mt-5 pt-4 border-t border-c2-linea grid grid-cols-2 sm:grid-cols-4 gap-4">
              <Dato
                t={nSem ? 'Semana más larga' : 'Mes más largo'}
                v={`${Unidades.texto(mayor.km, m, 1)} ${u}`}
                n={mayor.etiqueta}
              />
              <Dato
                t="Carreras por semana"
                v={(del.length / Math.max(1, semanas || diasPeriodo / 7)).toFixed(1)}
                n="promedio"
              />
              <Dato
                t="Distancia media"
                v={`${Unidades.texto(del.length ? km / del.length : 0, m, 1)} ${u}`}
                n="por carrera"
              />
              <Dato
                t="Días activos"
                v={`${Math.round((diasActivos / diasPeriodo) * 100)}%`}
                n={`${diasActivos} de ${diasPeriodo} días`}
              />
            </div>
          </>
        )}
      </Panel>

      <Panel titulo="Récords personales" className="lg:col-span-4" testId="panel-records">
        <RecordsLista lista={recs} carreras={carreras} m={m} />
      </Panel>

      <Constancia className="lg:col-span-8" carreras={carreras} />

      <Panel
        titulo="Logros"
        className="lg:col-span-4"
        accion={{
          texto: `${meds.filter((x) => x.lograda).length} de ${meds.length}`,
          onClick: () =>
            document.querySelector<HTMLButtonElement>('[role="tab"]:nth-child(3)')?.click(),
        }}
      >
        <Medallero lista={meds} />
      </Panel>

      <Panel
        titulo="Actividades recientes"
        className="lg:col-span-8"
        accion={
          carreras.length > 5
            ? { texto: `Ver las ${carreras.length}`, onClick: onVerTodas }
            : undefined
        }
      >
        {carreras.length === 0 ? (
          <p className="text-[13px] text-c2-muted">
            Todavía no guardas carreras.{' '}
            <Link to="/correr" className="text-c2-violeta font-semibold">
              Graba la primera
            </Link>
            .
          </p>
        ) : (
          <TablaActividades lista={carreras.slice(0, 5)} m={m} onBorrar={onBorrar} />
        )}
      </Panel>

      <RutasFavoritas className="lg:col-span-4" carreras={carreras} m={m} />
    </div>
  )
}

function Dato({ t, v, n }: { t: string; v: string; n: string }) {
  return (
    <div className="min-w-0">
      <Rotulo>{t}</Rotulo>
      <p className="cifra-c2 text-[20px] text-white mt-1 whitespace-nowrap">{v}</p>
      <p className="text-[11px] text-c2-muted truncate">{n}</p>
    </div>
  )
}

function RecordsLista({
  lista,
  carreras,
  m,
}: {
  lista: RecordPersonal[]
  carreras: CarreraGuardada[]
  m: boolean
}) {
  const larga = carreras.reduce<CarreraGuardada | null>(
    (b, c) => (!b || c.distanciaKm > b.distanciaKm ? c : b),
    null
  )
  const desnivel = carreras.reduce<CarreraGuardada | null>(
    (b, c) => (!b || c.desnivelM > b.desnivelM ? c : b),
    null
  )
  const filas: [string, string, string][] = [
    ...lista.map((r): [string, string, string] => [
      r.etiqueta,
      r.segundos == null
        ? 'Sin marca'
        : r.segundos >= 3600
          ? formatearHms(r.segundos)
          : formatearMs(r.segundos),
      r.segundos == null ? 'Aún sin esa distancia' : 'Mejor tiempo',
    ]),
    ...(larga
      ? [
          [
            'Más larga',
            `${Unidades.texto(larga.distanciaKm, m, 1)} ${Unidades.etiquetaDistancia(m)}`,
            fechaCorta(larga.inicioMs),
          ] as [string, string, string],
        ]
      : []),
    ...(desnivel && desnivel.desnivelM > 0
      ? [
          ['Mayor desnivel', `+${desnivel.desnivelM} m`, fechaCorta(desnivel.inicioMs)] as [
            string,
            string,
            string,
          ],
        ]
      : []),
  ]
  return (
    <ul className="divide-y divide-c2-linea -my-2" data-testid="records">
      {filas.map(([e, v, n]) => (
        <li key={e} className="flex items-center gap-3 py-2.5">
          <span className="w-9 h-9 rounded-md bg-c2-s2 grid place-items-center shrink-0">
            <span
              aria-hidden="true"
              className="material-symbols-outlined text-[18px] text-amber-400"
            >
              emoji_events
            </span>
          </span>
          <span className="flex-1 min-w-0">
            <span className="block text-[13px] text-white font-semibold">{e}</span>
            <span className="block text-[11px] text-c2-muted truncate">{n}</span>
          </span>
          <span className="cifra-c2 text-[18px] text-white whitespace-nowrap">{v}</span>
        </li>
      ))}
    </ul>
  )
}

function Constancia({
  carreras,
  className = '',
}: {
  carreras: CarreraGuardada[]
  className?: string
}) {
  const r = useRacha()
  const porDia = useMemo(() => {
    const mapa = new Map<string, number>()
    for (const c of carreras)
      mapa.set(diaLoja(c.inicioMs), (mapa.get(diaLoja(c.inicioMs)) ?? 0) + c.distanciaKm)
    return mapa
  }, [carreras])
  const desde = Date.now() - 26 * SEMANA_MS
  const en26 = carreras.filter((c) => c.inicioMs >= desde)
  const dias = [0, 0, 0, 0, 0, 0, 0]
  en26.forEach((c) => dias[(new Date(c.inicioMs).getDay() + 6) % 7]!++)
  const NOMBRES = ['lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábados', 'domingos']
  const top = dias
    .map((n, i) => [n, i] as const)
    .sort((a, b) => b[0] - a[0])
    .filter(([n]) => n > 0)
    .slice(0, 2)
  return (
    <Panel titulo="Constancia" className={className} testId="tarjeta-racha">
      <div className="flex flex-wrap gap-2 mb-4">
        <span
          className={`text-[11.5px] font-semibold px-2.5 py-1 rounded-full ${rachaEnRiesgo(r, Date.now()) ? 'bg-amber-400/15 text-amber-400' : 'bg-c2-morado/20 text-c2-violeta'}`}
        >
          Racha diaria <span data-testid="racha-diaria">{plural(r.diaria, 'día', 'días')}</span>,
          mejor {r.mejorDiaria}
        </span>
        <span className="text-[11.5px] font-semibold px-2.5 py-1 rounded-full bg-c2-menta/15 text-c2-menta">
          Racha semanal{' '}
          <span data-testid="racha-semanal">{plural(r.semanal, 'semana', 'semanas')}</span>
        </span>
      </div>
      <div className="flex flex-col sm:flex-row gap-5 sm:items-end">
        <div data-testid="mapa-calor" className="overflow-x-auto no-scrollbar shrink-0">
          <MapaCalor kmPorDia={porDia} semanas={26} />
        </div>
        <div className="flex-1 min-w-0">
          <Rotulo className="mb-2">Carreras por día</Rotulo>
          <Barras
            valores={dias}
            etiquetas={['L', 'M', 'X', 'J', 'V', 'S', 'D']}
            resaltar={dias.indexOf(Math.max(...dias))}
            alto={70}
            formato={(v) => `${v}`}
            className="!gap-1"
          />
        </div>
      </div>
      <p className="mt-3 text-[11.5px] text-c2-muted">
        {plural(en26.length, 'carrera', 'carreras')} en 26 semanas.
        {top.length ? ` Corres más los ${top.map(([, i]) => NOMBRES[i]).join(' y ')}.` : ''}
      </p>
    </Panel>
  )
}

function TablaActividades({
  lista,
  m,
  onBorrar,
}: {
  lista: CarreraGuardada[]
  m: boolean
  onBorrar: (id: string) => void
}) {
  const fila =
    'lg:grid lg:grid-cols-[minmax(0,1fr)_88px_72px_60px_52px_32px] ancho:grid-cols-[minmax(0,1fr)_96px_76px_64px_52px_56px_68px_32px] lg:items-center lg:gap-3'
  return (
    <div className="text-[12.5px]">
      <div
        className={`hidden ${fila} pb-2 border-b border-c2-linea text-[10.5px] font-bold uppercase tracking-[0.1em] text-c2-muted`}
        aria-hidden="true"
      >
        <span>Actividad</span>
        <span>Fecha</span>
        <span className="text-right">Distancia</span>
        <span className="text-right">Tiempo</span>
        <span className="text-right">Ritmo</span>
        <span className="hidden ancho:block text-right">Desnivel</span>
        <span className="hidden ancho:block text-right">Visibilidad</span>
        <span />
      </div>
      <ul className="divide-y divide-c2-linea">
        {lista.map((c) => (
          <li
            key={c.id}
            data-testid="carrera-guardada"
            className={`py-2.5 ${fila} grid grid-cols-[auto_minmax(0,1fr)_auto] gap-x-3 gap-y-1 items-center`}
          >
            <span className="flex items-center gap-3 min-w-0 col-span-2 lg:col-span-1">
              <MiniMapa
                puntos={c.puntos}
                ancho={56}
                alto={36}
                grosor={2}
                className="w-14 h-9 rounded shrink-0"
              />
              <Link
                to={`/perfil/carrera/${c.id}`}
                className="font-semibold text-white truncate hover:text-c2-violeta"
              >
                {c.titulo}
              </Link>
            </span>
            <span className="text-c2-muted truncate col-start-2 lg:col-start-auto row-start-2 lg:row-start-auto first-letter:uppercase">
              {new Date(c.inicioMs)
                .toLocaleDateString('es-EC', { weekday: 'short', day: 'numeric', month: 'short' })
                .replace(/\./g, '')}
            </span>
            <span className="hidden lg:block text-right cifra-c2 text-[15px] text-white">{`${Unidades.texto(c.distanciaKm, m)} ${Unidades.etiquetaDistancia(m)}`}</span>
            <span className="hidden lg:block text-right cifra-c2 text-[15px] text-white">
              {formatearMs(c.segundos)}
            </span>
            <span className="hidden lg:block text-right cifra-c2 text-[15px] text-c2-menta">
              {formatearRitmo(
                Unidades.ritmo(
                  c.distanciaKm > 0.01 ? Math.floor(c.segundos / c.distanciaKm) : null,
                  m
                )
              )}
            </span>
            <span className="hidden ancho:block text-right text-c2-muted">+{c.desnivelM} m</span>
            <span className="hidden ancho:block text-right text-c2-muted">
              {c.publica ? 'Pública' : 'Privada'}
            </span>
            <span className="lg:hidden col-start-2 row-start-3 text-c2-muted">
              <span className="text-white font-semibold">{`${Unidades.texto(c.distanciaKm, m)} ${Unidades.etiquetaDistancia(m)}`}</span>{' '}
              · {formatearMs(c.segundos)} ·{' '}
              {formatearRitmo(
                Unidades.ritmo(
                  c.distanciaKm > 0.01 ? Math.floor(c.segundos / c.distanciaKm) : null,
                  m
                )
              )}{' '}
              {Unidades.etiquetaRitmo(m)}
              {!c.publica ? ' · Privada' : ''}
            </span>
            <button
              onClick={() => onBorrar(c.id)}
              aria-label={`Eliminar ${c.titulo}`}
              className="col-start-3 row-start-1 lg:col-start-auto lg:row-start-auto justify-self-end w-8 h-8 grid place-items-center text-c2-muted hover:text-pink-300"
            >
              <span aria-hidden="true" className="material-symbols-outlined text-[18px]">
                delete
              </span>
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}

function RutasFavoritas({
  carreras,
  m,
  className = '',
}: {
  carreras: CarreraGuardada[]
  m: boolean
  className?: string
}) {
  const guardadas = useRutas((s) => s.rutasGuardadas)
  const rutas = useRutas((s) => s.rutas)
  const favoritas = guardadas
    .map((id) => rutas.find((r) => r.id === id))
    .filter((r): r is Ruta => !!r)
    .slice(0, 4)
  const horas = [5, 7, 9, 11, 13, 15, 17, 19]
  const conteo = horas.map(
    (h) =>
      carreras.filter((c) => {
        const x = new Date(c.inicioMs).getHours()
        return x >= h && x < h + 2
      }).length
  )
  return (
    <Panel titulo="Rutas favoritas" className={className} accion={{ texto: 'Rutas', to: '/rutas' }}>
      {favoritas.length === 0 ? (
        <p className="text-[13px] text-c2-muted">Guarda rutas con el marcador para verlas aquí.</p>
      ) : (
        <ul className="space-y-2.5">
          {favoritas.map((r) => (
            <li key={r.id}>
              <Link
                to={`/rutas/${r.id}`}
                className="flex items-center gap-3 hover:bg-c2-s2/40 rounded"
              >
                <MiniMapa
                  puntos={r.puntos}
                  ancho={56}
                  alto={36}
                  grosor={2}
                  className="w-14 h-9 rounded shrink-0"
                />
                <span className="min-w-0">
                  <span className="block text-[13px] text-white font-semibold truncate">
                    {r.nombre}
                  </span>
                  <span className="block text-[11px] text-c2-muted">{`${Unidades.texto(r.distanciaKm, m, 1)} ${Unidades.etiquetaDistancia(m)}`}</span>
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
      {carreras.length > 0 && (
        <div className="mt-5 pt-4 border-t border-c2-linea">
          <Rotulo className="mb-2">Cuándo corres</Rotulo>
          <Barras
            valores={conteo}
            etiquetas={horas.map((h) => `${h}h`)}
            resaltar={conteo.indexOf(Math.max(...conteo))}
            alto={56}
            formato={(v) => `${v} carreras`}
            className="!gap-1"
          />
        </div>
      )}
    </Panel>
  )
}
