import { useEffect, useState } from 'react'
import { BarrasHorizontales, GraficoBarras, GraficoLinea } from '../../../shared/ui/Graficos'
import { aCsv, aPdf, descargar } from '../../analitica/api'
import type { Estadisticas as Datos, FuenteAdmin } from './fuente'

const tarjeta = 'bg-c2-card border border-c2-linea'
const hoy = () => new Date().toLocaleDateString('en-CA')
const haceDias = (n: number) => {
  const d = new Date()
  d.setDate(d.getDate() - n)
  return d.toLocaleDateString('en-CA')
}
const RANGOS: [string, number][] = [
  ['7 días', 6],
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

function Kpi({ t, v, s }: { t: string; v: string | number; s?: string }) {
  return (
    <div className={`${tarjeta} p-3`}>
      <p className="text-[10px] font-extrabold uppercase tracking-widest text-[#C084FC]">{t}</p>
      <p className="text-[20px] font-bold tabular-nums text-white leading-tight">
        {typeof v === 'number' ? v.toLocaleString('es-EC') : v}
      </p>
      {s && <p className="text-[11px] text-[#68DBA9]">{s}</p>}
    </div>
  )
}

function Panel({ titulo, children }: { titulo: string; children: React.ReactNode }) {
  return (
    <section className={`${tarjeta} p-4`}>
      <h2 className="font-display italic uppercase text-white text-[16px] mb-3">{titulo}</h2>
      {children}
    </section>
  )
}

/** Estadisticas del panel con rango de fechas, graficos y exportacion CSV y PDF. */
export function Estadisticas({ f }: { f: FuenteAdmin }) {
  const [desde, setDesde] = useState(haceDias(29))
  const [hasta, setHasta] = useState(hoy())
  const [datos, setDatos] = useState<Datos | null>(null)
  const [error, setError] = useState<string | null>(null)
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
  // En rangos largos las barras de carreras se agrupan por semana para que sigan legibles.
  const carreras = datos
    ? semanal
      ? datos.serie.reduce<{ etiqueta: string; valor: number }[]>((acc, s, i) => {
          if (i % 7 === 0) acc.push({ etiqueta: corta(s.dia), valor: 0 })
          acc[acc.length - 1].valor += s.carreras
          return acc
        }, [])
      : datos.serie.map((s) => ({ etiqueta: corta(s.dia), valor: s.carreras }))
    : []

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
        <h1 className="font-display italic uppercase text-[28px] lg:text-[34px] text-white">
          Estadísticas
        </h1>
        <div className="flex gap-2">
          <button
            type="button"
            disabled={!datos}
            data-testid="estadisticas-csv"
            onClick={() =>
              datos && descargar(`${nombre}.csv`, csvEstadisticas(datos), 'text/csv;charset=utf-8')
            }
            className="h-10 px-4 -skew-x-12 font-black italic uppercase text-[13px] bg-c2-s2 text-white disabled:opacity-50"
          >
            CSV
          </button>
          <button
            type="button"
            disabled={!datos}
            data-testid="estadisticas-pdf"
            onClick={() =>
              datos && descargar(`${nombre}.pdf`, pdfEstadisticas(datos), 'application/pdf')
            }
            className="h-10 px-4 -skew-x-12 font-black italic uppercase text-[13px] bg-gradient-to-r from-[#7B1FA2] to-c2-morado text-white disabled:opacity-50"
          >
            PDF
          </button>
        </div>
      </div>
      <div className="flex flex-wrap items-end gap-3 mb-5" data-testid="rango-estadisticas">
        <label className="text-[11px] uppercase font-bold text-c2-muted">
          Desde
          <input
            type="date"
            value={desde}
            max={hasta}
            onChange={(e) => setDesde(e.target.value)}
            className="block mt-1 h-9 bg-c2-s2 px-2 text-white text-[13px] [color-scheme:dark]"
          />
        </label>
        <label className="text-[11px] uppercase font-bold text-c2-muted">
          Hasta
          <input
            type="date"
            value={hasta}
            min={desde}
            max={hoy()}
            onChange={(e) => setHasta(e.target.value)}
            className="block mt-1 h-9 bg-c2-s2 px-2 text-white text-[13px] [color-scheme:dark]"
          />
        </label>
        <div className="flex flex-wrap gap-1">
          {RANGOS.map(([n, d]) => (
            <button
              key={n}
              type="button"
              onClick={() => {
                setDesde(haceDias(d))
                setHasta(hoy())
              }}
              className={`px-3 h-9 text-[12px] font-semibold border ${desde === haceDias(d) && hasta === hoy() ? 'border-[#C084FC] text-[#C084FC]' : 'border-c2-linea text-c2-muted'}`}
            >
              {n}
            </button>
          ))}
        </div>
      </div>
      {error && (
        <p role="alert" className="mb-4 border border-c2-peligro bg-c2-card p-3 text-sm text-white">
          No se pudo cargar: {error}. Revisa que la migración 023 esté aplicada.
        </p>
      )}
      {datos && t && (
        <div className="space-y-4" data-testid="estadisticas">
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-2">
            <Kpi
              t="Usuarios nuevos"
              v={t.usuarios_nuevos}
              s={`${t.usuarios_total.toLocaleString('es-EC')} en total`}
            />
            <Kpi t="Corredores activos" v={t.usuarios_activos} />
            <Kpi t="Carreras" v={t.carreras} s={`${t.km.toLocaleString('es-EC')} km`} />
            <Kpi
              t="Retención"
              v={`${datos.retencion.porcentaje}%`}
              s={`${datos.retencion.vuelven} de ${datos.retencion.previos} volvieron`}
            />
            <Kpi t="Eventos" v={t.eventos} s={`${t.inscripciones} inscripciones`} />
            <Kpi
              t="Reportes"
              v={t.reportes}
              s={`${datos.reportes.por_estado.pendiente ?? 0} pendientes`}
            />
          </div>
          <div className="grid lg:grid-cols-2 gap-4">
            <Panel titulo="Usuarios nuevos por día">
              <GraficoLinea
                testId="grafico-nuevos"
                datos={datos.serie.map((s) => ({ etiqueta: corta(s.dia), valor: s.nuevos }))}
              />
            </Panel>
            <Panel titulo={semanal ? 'Carreras por semana' : 'Carreras por día'}>
              <GraficoBarras testId="grafico-carreras" datos={carreras} resaltar={-1} />
            </Panel>
            <Panel titulo="Rutas más corridas">
              {datos.rutas_top.length ? (
                <BarrasHorizontales
                  testId="rutas-top"
                  datos={datos.rutas_top.map((r) => ({ etiqueta: r.nombre, valor: r.corridas }))}
                />
              ) : (
                <p className="text-c2-muted text-[13px]">Sin recorridos de rutas en el rango.</p>
              )}
            </Panel>
            <Panel titulo="Eventos con más inscritos">
              {datos.eventos_top.length ? (
                <BarrasHorizontales
                  color="#68DBA9"
                  datos={datos.eventos_top.map((e) => ({ etiqueta: e.titulo, valor: e.inscritos }))}
                />
              ) : (
                <p className="text-c2-muted text-[13px]">Sin eventos en el rango.</p>
              )}
            </Panel>
            <Panel titulo="Reportes por estado">
              <BarrasHorizontales
                color="#96CCFF"
                datos={aBarras(datos.reportes.por_estado, ESTADO)}
              />
            </Panel>
            <Panel titulo="Reportes por tipo y motivo">
              <BarrasHorizontales color="#BE185D" datos={aBarras(datos.reportes.por_tipo, TIPO)} />
              <div className="mt-3">
                <BarrasHorizontales color="#A855F7" datos={aBarras(datos.reportes.por_motivo)} />
              </div>
            </Panel>
          </div>
        </div>
      )}
    </>
  )
}
