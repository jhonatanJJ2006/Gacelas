import { useEffect, useState, type ReactNode } from 'react'
import { Contador } from '../../../shared/ui/Animaciones'
import { aCsv, aPdf, descargar } from '../../analitica/api'
import {
  BarrasDia,
  Boton,
  Cabecera,
  Chip,
  ErrorCarga,
  Vacio,
  decimal1,
  entero,
  useCarga,
  type Tono,
} from './kit'
import { MOTIVOS } from './casos'
import type { Estadisticas as Datos, FuenteAdmin } from './fuente'

const hoy = () => new Date().toLocaleDateString('en-CA')
const haceDias = (n: number) => {
  const d = new Date()
  d.setDate(d.getDate() - n)
  return d.toLocaleDateString('en-CA')
}
const RANGOS: [string, number][] = [
  ['7 días', 6],
  ['14 días', 13],
  ['30 días', 29],
  ['90 días', 89],
  ['1 año', 364],
]
const ESTADO: Record<string, string> = {
  pendiente: 'Pendiente',
  en_revision: 'En revisión',
  resuelto: 'Resuelto',
  descartado: 'Descartado',
}
const TIPO: Record<string, string> = {
  post: 'Publicación',
  usuario: 'Usuario',
  comentario: 'Comentario',
  evento: 'Evento',
}

const aBarras = (o: Record<string, number>, nombres: Record<string, string> = {}) =>
  Object.entries(o)
    .sort((a, b) => b[1] - a[1])
    .map(([k, v]) => ({ etiqueta: nombres[k] ?? k, valor: v }))

export function csvEstadisticas(d: Datos): string {
  return aCsv(
    ['Día', 'Usuarios nuevos', 'Corredores activos', 'Carreras', 'Km'],
    d.serie.map((s) => [s.dia, s.nuevos, s.activos, s.carreras, s.km])
  )
}

export function pdfEstadisticas(d: Datos): Uint8Array {
  const t = d.totales
  return aPdf(`Estadísticas Runcity ${d.desde} a ${d.hasta}`, [
    {
      titulo: 'Resumen',
      lineas: [
        `Usuarios nuevos: ${t.usuarios_nuevos}   Total: ${t.usuarios_total}`,
        `Corredores activos: ${t.usuarios_activos}   Carreras: ${t.carreras}   Km: ${t.km}`,
        `Eventos: ${t.eventos}   Inscripciones: ${t.inscripciones}   Reportes: ${t.reportes}`,
        `Retención: ${d.retencion.porcentaje}% (${d.retencion.vuelven} de ${d.retencion.previos} volvieron a correr)`,
      ],
    },
    {
      titulo: 'Rutas más corridas',
      barras: d.rutas_top.map((r) => ({ etiqueta: r.nombre, valor: r.corridas })),
    },
    {
      titulo: 'Eventos con más inscritos',
      barras: d.eventos_top.map((e) => ({ etiqueta: e.titulo, valor: e.inscritos })),
    },
    { titulo: 'Reportes por estado', barras: aBarras(d.reportes.por_estado, ESTADO) },
    { titulo: 'Reportes por tipo', barras: aBarras(d.reportes.por_tipo, TIPO) },
    {
      titulo: 'Detalle diario',
      lineas: d.serie.map(
        (s) =>
          `${s.dia}  nuevos ${s.nuevos}  activos ${s.activos}  carreras ${s.carreras}  km ${s.km}`
      ),
    },
  ])
}

function Kpi({ t, v, s, tono }: { t: string; v: string | number; s?: string; tono?: Tono }) {
  return (
    <div className="adm-kpi">
      <p className="adm-t2" style={{ fontSize: 12 }}>
        {t}
      </p>
      <p
        className={`adm-num c-${tono ?? 'neu'}`}
        style={{ fontSize: 26, color: tono ? 'hsl(var(--tt))' : undefined }}
      >
        {typeof v === 'number' ? <Contador valor={v} formato={(n) => entero(Math.round(n))} /> : v}
      </p>
      {s && (
        <p className="adm-t3" style={{ fontSize: 12 }}>
          {s}
        </p>
      )}
    </div>
  )
}

function Panel({ titulo, nota, children }: { titulo: string; nota?: string; children: ReactNode }) {
  return (
    <section className="adm-card adm-pad">
      <div className="adm-fila" style={{ justifyContent: 'space-between', marginBottom: 12 }}>
        <h2 className="adm-h2">{titulo}</h2>
        {nota && (
          <span className="adm-t3" style={{ fontSize: 12 }}>
            {nota}
          </span>
        )}
      </div>
      {children}
    </section>
  )
}

/** Barras horizontales con tokens del panel. */
function Ranking({
  datos,
  tono = 'pri',
  testId,
}: {
  datos: { etiqueta: string; valor: number }[]
  tono?: Tono
  testId?: string
}) {
  const max = Math.max(1, ...datos.map((d) => d.valor))
  return (
    <ul data-testid={testId} style={{ display: 'grid', gap: 10, listStyle: 'none' }}>
      {datos.map((d, i) => (
        <li key={`${d.etiqueta}-${i}`}>
          <div className="adm-fila" style={{ justifyContent: 'space-between', fontSize: 13 }}>
            <span className="adm-ell">{d.etiqueta}</span>
            <b className="adm-num">{entero(d.valor)}</b>
          </div>
          <span className={`adm-barra c-${tono}`} style={{ marginTop: 4 }}>
            <i style={{ width: `${(d.valor / max) * 100}%` }} />
          </span>
        </li>
      ))}
    </ul>
  )
}

const NOMBRE_MES = [
  '',
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

const PLATAFORMA: Record<string, string> = { android: 'Android', ios: 'iOS', web: 'Web' }

/** Reportes del panel: rango de fechas, cifras, graficos y exportacion CSV y PDF. */
export function Estadisticas({ f }: { f: FuenteAdmin }) {
  const [desde, setDesde] = useState(haceDias(13))
  const [hasta, setHasta] = useState(hoy())
  const [datos, setDatos] = useState<Datos | null>(null)
  const [error, setError] = useState<string | null>(null)
  const ecu = useCarga(() => f.importacionesEcu(), [f])
  const registro = useCarga(() => f.registroEcu(), [f])
  const publicadas = useCarga(() => f.versionesPublicadas(), [f])
  const enUso = useCarga(() => f.versionesEnUso(), [f])
  const ajustes = useCarga(() => f.ajustes(), [f])
  useEffect(() => {
    let vivo = true
    setError(null)
    if (hasta < desde) {
      setError('La fecha final es anterior a la inicial')
      return
    }
    f.estadisticas(desde, hasta).then(
      (d) => vivo && setDatos(d),
      (e: Error) => vivo && setError(e.message)
    )
    return () => {
      vivo = false
    }
  }, [f, desde, hasta])

  const corta = (dia: string) => dia.slice(5).split('-').reverse().join('/')
  const t = datos?.totales
  const nombre = `gacelas-estadisticas-${desde}-a-${hasta}`
  const semanal = datos && datos.serie.length > 45
  // En rangos largos las barras se agrupan por semana para que sigan legibles.
  const agrupar = (valor: (s: Datos['serie'][number]) => number) =>
    datos
      ? semanal
        ? datos.serie.reduce<{ etiqueta: string; valor: number }[]>((acc, s, i) => {
            if (i % 7 === 0) acc.push({ etiqueta: corta(s.dia), valor: 0 })
            acc[acc.length - 1].valor += valor(s)
            return acc
          }, [])
        : datos.serie.map((s) => ({ etiqueta: corta(s.dia), valor: valor(s) }))
      : []
  const por = semanal ? 'por semana' : 'por día'
  const minima = (ajustes.datos ?? []).find((a) => a.key === 'version_minima_android')?.value as
    string | undefined

  return (
    <>
      <Cabecera
        kicker="Estadísticas"
        extra={
          <>
            <span
              className="adm-fila adm-envuelve"
              data-testid="rango-estadisticas"
              style={{ gap: 6 }}
            >
              {RANGOS.map(([n, d]) => (
                <button
                  key={n}
                  type="button"
                  className="adm-chip c-neu"
                  aria-pressed={desde === haceDias(d) && hasta === hoy()}
                  onClick={() => {
                    setDesde(haceDias(d))
                    setHasta(hoy())
                  }}
                >
                  {n}
                </button>
              ))}
              <input
                aria-label="Desde"
                type="date"
                value={desde}
                max={hasta}
                onChange={(e) => setDesde(e.target.value)}
                className="adm-input"
                style={{ height: 32 }}
              />
              <input
                aria-label="Hasta"
                type="date"
                value={hasta}
                min={desde}
                max={hoy()}
                onChange={(e) => setHasta(e.target.value)}
                className="adm-input"
                style={{ height: 32 }}
              />
            </span>
            <Boton
              deshabilitado={!datos}
              testId="estadisticas-csv"
              onClick={() =>
                datos &&
                descargar(`${nombre}.csv`, csvEstadisticas(datos), 'text/csv;charset=utf-8')
              }
            >
              Exportar CSV
            </Boton>
            <Boton
              deshabilitado={!datos}
              testId="estadisticas-pdf"
              onClick={() =>
                datos && descargar(`${nombre}.pdf`, pdfEstadisticas(datos), 'application/pdf')
              }
            >
              Exportar PDF
            </Boton>
          </>
        }
      >
        Reportes
      </Cabecera>
      <ErrorCarga mensaje={error} />
      {datos && t && (
        <div style={{ display: 'grid', gap: 16 }} data-testid="estadisticas">
          <div className="adm-cuatro">
            <Kpi t="Carreras" v={t.carreras} s={`${t.usuarios_activos} corredores activos`} />
            <Kpi t="Kilómetros" v={decimal1(t.km)} tono="pri" s="en el rango" />
            <Kpi t="Usuarios" v={t.usuarios_total} s={`${t.usuarios_nuevos} nuevos`} />
            <Kpi
              t="Retención"
              v={`${datos.retencion.porcentaje}%`}
              tono="ok"
              s={`${datos.retencion.vuelven} de ${datos.retencion.previos} volvieron`}
            />
          </div>
          <div className="adm-g2 adm-g-rep">
            <Panel titulo={`Kilómetros ${por}`} nota={`${datos.serie.length} días`}>
              <BarrasDia datos={agrupar((s) => s.km)} tono="pri" unidad=" km" testId="grafico-km" />
            </Panel>
            <Panel titulo={`Altas ${por}`}>
              <BarrasDia datos={agrupar((s) => s.nuevos)} tono="ok" testId="grafico-nuevos" />
            </Panel>
          </div>
          <div className="adm-g2 adm-g-rep">
            <Panel titulo={`Carreras ${por}`}>
              <BarrasDia
                datos={agrupar((s) => s.carreras)}
                tono="warn"
                testId="grafico-carreras"
                resaltarMax
              />
            </Panel>
            <Panel titulo="Rutas más corridas">
              {datos.rutas_top.length ? (
                <Ranking
                  testId="rutas-top"
                  datos={datos.rutas_top.map((r) => ({ etiqueta: r.nombre, valor: r.corridas }))}
                />
              ) : (
                <Vacio titulo="Sin recorridos de rutas en el rango" />
              )}
            </Panel>
          </div>
          <div className="adm-g2 adm-g-rep">
            <Panel titulo="Reportes de moderación" nota={`${t.reportes} en el rango`}>
              <div
                className="adm-g2"
                style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))' }}
              >
                <div>
                  <p className="adm-lbl" style={{ marginBottom: 8 }}>
                    Por estado
                  </p>
                  <Ranking tono="info" datos={aBarras(datos.reportes.por_estado, ESTADO)} />
                </div>
                <div>
                  <p className="adm-lbl" style={{ marginBottom: 8 }}>
                    Por tipo
                  </p>
                  <Ranking tono="bad" datos={aBarras(datos.reportes.por_tipo, TIPO)} />
                </div>
                <div>
                  <p className="adm-lbl" style={{ marginBottom: 8 }}>
                    Por motivo
                  </p>
                  <Ranking tono="pri" datos={aBarras(datos.reportes.por_motivo, MOTIVOS)} />
                </div>
              </div>
            </Panel>
            <Panel titulo="Eventos con más inscritos">
              {datos.eventos_top.length ? (
                <Ranking
                  tono="ok"
                  datos={datos.eventos_top.map((e) => ({ etiqueta: e.titulo, valor: e.inscritos }))}
                />
              ) : (
                <Vacio titulo="Sin eventos en el rango" />
              )}
            </Panel>
          </div>
        </div>
      )}
      <div className="adm-g2 adm-g-rep" style={{ marginTop: 16 }}>
        <Panel titulo="Importaciones del ECU 911" nota="Filas agregadas por mes">
          {ecu.datos && ecu.datos.length > 0 ? (
            <ul
              style={{ display: 'grid', gap: 10, listStyle: 'none' }}
              data-testid="importaciones-ecu"
            >
              {ecu.datos.map((m) => (
                <li
                  key={`${m.anio}-${m.mes}`}
                  className="adm-fila"
                  style={{ justifyContent: 'space-between' }}
                >
                  <b>
                    {m.anio}-{String(m.mes).padStart(2, '0')}
                  </b>
                  <span className="adm-t2">
                    {NOMBRE_MES[m.mes]} {m.anio}
                  </span>
                  {m.agrupaciones > 0 ? (
                    <Chip tono="ok">{entero(m.agrupaciones)} agrupaciones</Chip>
                  ) : (
                    <Chip>Sin datos</Chip>
                  )}
                </li>
              ))}
            </ul>
          ) : (
            <Vacio
              testId="ecu-reportes-vacio"
              titulo={ecu.error ? 'No se pudo leer la carga del ECU 911' : 'Sin cargas del ECU 911'}
              texto={ecu.error ?? 'Todavía no hay datos en incidentes_agregados.'}
            />
          )}
          <p className="adm-lbl" style={{ marginTop: 14, marginBottom: 8 }}>
            Registro de cargas
          </p>
          {registro.datos && registro.datos.length > 0 ? (
            <ul style={{ display: 'grid', gap: 8, listStyle: 'none' }} data-testid="registro-ecu">
              {registro.datos.map((r) => (
                <li key={`${r.periodo}-${r.fecha}`} style={{ fontSize: 13 }}>
                  <div className="adm-fila" style={{ justifyContent: 'space-between' }}>
                    <b>{r.periodo}</b>
                    <Chip tono={r.estado === 'ok' ? 'ok' : 'bad'}>
                      {r.estado === 'ok' ? 'Correcta' : 'Con error'}
                    </Chip>
                  </div>
                  <p className="adm-t2">
                    {entero(r.filas)} filas leídas, {entero(r.filas_loja)} de Loja ·{' '}
                    {new Date(r.fecha).toLocaleDateString('es-EC')}
                  </p>
                  {r.mensaje && <p className="adm-t3">{r.mensaje}</p>}
                </li>
              ))}
            </ul>
          ) : (
            <p className="adm-t3" style={{ fontSize: 12 }}>
              {registro.error ?? 'Sin cargas registradas.'}
            </p>
          )}
        </Panel>
        <Panel titulo="Versión de Android">
          <p className="adm-num" style={{ fontSize: 26 }}>
            {minima ?? 'Sin definir'}
          </p>
          <p className="adm-t2" style={{ fontSize: 13 }}>
            Versión mínima exigida. Cámbiala en Ajustes.
          </p>
          <p className="adm-lbl" style={{ marginTop: 14, marginBottom: 8 }}>
            En uso (últimos 30 días)
          </p>
          {enUso.datos && enUso.datos.length > 0 ? (
            <ul style={{ display: 'grid', gap: 6, listStyle: 'none' }} data-testid="versiones-uso">
              {enUso.datos.map((v) => (
                <li
                  key={`${v.plataforma}-${v.version_name}`}
                  className="adm-fila"
                  style={{ justifyContent: 'space-between', fontSize: 13 }}
                >
                  <span>
                    {PLATAFORMA[v.plataforma] ?? v.plataforma} {v.version_name}
                  </span>
                  <b className="adm-num">
                    {entero(v.usuarios)} {v.usuarios === 1 ? 'persona' : 'personas'}
                  </b>
                </li>
              ))}
            </ul>
          ) : (
            <p className="adm-t3" style={{ fontSize: 12 }}>
              {enUso.error ?? 'Sin registros de uso todavía (se guardan desde Android 1.0.2).'}
            </p>
          )}
          <p className="adm-lbl" style={{ marginTop: 14, marginBottom: 8 }}>
            Publicadas
          </p>
          {publicadas.datos && publicadas.datos.length > 0 ? (
            <ul
              style={{ display: 'grid', gap: 6, listStyle: 'none' }}
              data-testid="versiones-publicadas"
            >
              {publicadas.datos.slice(0, 6).map((v) => (
                <li
                  key={`${v.plataforma}-${v.version_name}`}
                  className="adm-fila"
                  style={{ justifyContent: 'space-between', fontSize: 13 }}
                >
                  <span>
                    <b>{v.version_name}</b>{' '}
                    <span className="adm-t3">
                      {new Date(v.publicada_en).toLocaleDateString('es-EC')}
                      {v.apk_bytes ? ` · ${(v.apk_bytes / 1_048_576).toFixed(1)} MB` : ''}
                    </span>
                  </span>
                  <Chip tono={v.canal === 'estable' ? 'ok' : undefined}>
                    {v.canal === 'estable' ? 'Estable' : 'Prueba'}
                  </Chip>
                </li>
              ))}
            </ul>
          ) : (
            <p className="adm-t3" style={{ fontSize: 12 }}>
              {publicadas.error ?? 'Sin versiones registradas.'}
            </p>
          )}
        </Panel>
      </div>
    </>
  )
}
