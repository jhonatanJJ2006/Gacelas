import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { BarrasHorizontales, GraficoBarras } from '../../../shared/ui/Graficos'
import { useApp } from '../../../store/app'
import {
  aCsv,
  aPdf,
  descargar,
  duracionTexto,
  etiquetaRango,
  NOMBRE_PERIODO,
  reportePersonal,
  ritmoTexto,
  textoCompartir,
  type Periodo,
  type ReportePersonal,
} from '../api'

function Metrica({ valor, etiqueta, nota }: { valor: string; etiqueta: string; nota?: string }) {
  return (
    <div className="metrica-c2">
      <span className="etiqueta-c2">{etiqueta}</span>
      <span className="text-[17px] font-bold tabular-nums text-white leading-tight">{valor}</span>
      {nota && (
        <span
          className={`text-[11px] ${nota.startsWith('-') ? 'text-c2-peligro' : 'text-c2-menta'}`}
        >
          {nota}
        </span>
      )}
    </div>
  )
}

const dias = (n: number) => `${n} ${n === 1 ? 'día' : 'días'}`

function variacion(a: number, b: number) {
  if (b === 0) return a > 0 ? 'nuevo' : 'igual'
  const p = Math.round(((a - b) / b) * 100)
  return `${p >= 0 ? '+' : ''}${p}% vs. anterior`
}

export function exportarCsv(rep: ReportePersonal) {
  const filas = rep.serie.map((s) => [s.etiqueta, s.km])
  return aCsv(
    ['Periodo', 'Km'],
    [
      ...filas,
      [],
      ['Carreras', rep.actual.carreras],
      ['Km', rep.actual.km],
      ['Tiempo (s)', rep.actual.segundos],
      ['Ritmo medio', ritmoTexto(rep.actual.ritmoSegKm)],
      ['Desnivel (m)', rep.actual.desnivelM],
    ]
  )
}

export function exportarPdf(rep: ReportePersonal) {
  return aPdf(`Mi reporte: ${NOMBRE_PERIODO[rep.periodo]}`, [
    {
      lineas: [
        etiquetaRango(rep),
        `Distancia: ${rep.actual.km.toFixed(1)} km (anterior ${rep.previo.km.toFixed(1)} km)`,
        `Carreras: ${rep.actual.carreras}   Tiempo: ${duracionTexto(rep.actual.segundos)}`,
        `Ritmo medio: ${ritmoTexto(rep.actual.ritmoSegKm)} /km   Desnivel: ${rep.actual.desnivelM} m`,
        `Racha actual: ${rep.racha.actual} dias (mejor ${rep.racha.mejor})`,
      ],
    },
    { titulo: 'Kilometros', barras: rep.serie.map((s) => ({ etiqueta: s.etiqueta, valor: s.km })) },
    {
      titulo: 'Rutas favoritas',
      lineas: rep.rutasFavoritas.length
        ? rep.rutasFavoritas.map((r) => `${r.nombre}: ${r.veces} veces`)
        : ['Sin rutas registradas en este periodo'],
    },
  ])
}

/** Reporte personal del corredor: semana, mes o año con comparacion y exportacion. */
export function MisReportes() {
  const navigate = useNavigate()
  const carreras = useApp((s) => s.carreras)
  const rutas = useApp((s) => s.rutas)
  const [periodo, setPeriodo] = useState<Periodo>('semana')
  const [aviso, setAviso] = useState<string | null>(null)
  const rep = useMemo(
    () => reportePersonal(carreras, periodo, Date.now(), rutas),
    [carreras, periodo, rutas]
  )
  const nombre = `gacelas-reporte-${periodo}-${new Date(rep.desdeMs).toISOString().slice(0, 10)}`

  const compartir = async () => {
    const texto = textoCompartir(rep)
    try {
      if (navigator.share) await navigator.share({ title: 'Mi reporte en Gacelas', text: texto })
      else {
        await navigator.clipboard.writeText(texto)
        setAviso('Resumen copiado al portapapeles')
      }
    } catch {
      /* cancelado */
    }
  }

  return (
    <div className="contenedor-escritorio max-w-3xl pb-space-3xl" data-testid="mis-reportes">
      <header className="flex items-center gap-2 px-margin pt-space-md pb-space-sm lg:px-0">
        <button
          onClick={() => navigate(-1)}
          aria-label="Atrás"
          className="w-11 h-11 rounded-full flex items-center justify-center text-text-primary hover:bg-surface-container"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </button>
        <h1 className="font-display italic uppercase text-[24px] text-white">Mis reportes</h1>
      </header>
      <div className="px-margin lg:px-0 space-y-4">
        <div className="flex gap-2" role="tablist" aria-label="Periodo">
          {(['semana', 'mes', 'anio'] as const).map((p) => (
            <button
              key={p}
              role="tab"
              aria-selected={periodo === p}
              onClick={() => setPeriodo(p)}
              className={`-skew-x-12 px-4 h-9 text-[12px] font-black italic uppercase ${periodo === p ? 'bg-c2-violeta text-c2-bg' : 'bg-c2-s2 text-white'}`}
            >
              {NOMBRE_PERIODO[p]}
            </button>
          ))}
        </div>
        <p className="text-[12px] text-c2-muted" data-testid="rango-reporte">
          {etiquetaRango(rep)}
        </p>

        <section className="tarjeta-c2 p-4">
          <span className="etiqueta-c2">Distancia</span>
          <p className="flex items-baseline gap-2">
            <span className="valor-c2 text-white" data-testid="km-reporte">
              {rep.actual.km.toFixed(1)}
            </span>
            <span className="text-c2-muted text-[13px]">km</span>
            <span
              className={`ml-auto text-[12px] ${rep.actual.km < rep.previo.km ? 'text-c2-peligro' : 'text-c2-menta'}`}
              data-testid="variacion-reporte"
            >
              {variacion(rep.actual.km, rep.previo.km)}
            </span>
          </p>
          <div className="mt-3">
            <GraficoBarras
              testId="grafico-reporte"
              datos={rep.serie.map((s) => ({ etiqueta: s.etiqueta, valor: s.km }))}
              unidad=" km"
              alto={120}
            />
          </div>
        </section>

        <div className="grid grid-cols-2 lg:grid-cols-4 gap-2">
          <Metrica
            etiqueta="Carreras"
            valor={String(rep.actual.carreras)}
            nota={variacion(rep.actual.carreras, rep.previo.carreras)}
          />
          <Metrica etiqueta="Tiempo" valor={duracionTexto(rep.actual.segundos)} />
          <Metrica
            etiqueta="Ritmo medio"
            valor={`${ritmoTexto(rep.actual.ritmoSegKm)} /km`}
            nota={rep.previo.ritmoSegKm ? `antes ${ritmoTexto(rep.previo.ritmoSegKm)}` : undefined}
          />
          <Metrica etiqueta="Desnivel" valor={`${rep.actual.desnivelM} m`} />
        </div>

        <section className="tarjeta-c2 p-4">
          <h2 className="etiqueta-c2 mb-2">Mejores marcas del periodo</h2>
          <div className="grid grid-cols-3 gap-2">
            <Metrica etiqueta="Más larga" valor={`${rep.mejores.masLargaKm.toFixed(1)} km`} />
            <Metrica etiqueta="Mejor ritmo" valor={ritmoTexto(rep.mejores.mejorRitmoSegKm)} />
            <Metrica etiqueta="Más desnivel" valor={`${Math.round(rep.mejores.masDesnivelM)} m`} />
          </div>
        </section>

        <section className="tarjeta-c2 p-4 grid grid-cols-2 gap-2">
          <Metrica etiqueta="Racha actual" valor={dias(rep.racha.actual)} />
          <Metrica etiqueta="Mejor racha" valor={dias(rep.racha.mejor)} />
        </section>

        <section className="tarjeta-c2 p-4">
          <h2 className="etiqueta-c2 mb-2">Rutas favoritas</h2>
          {rep.rutasFavoritas.length ? (
            <BarrasHorizontales
              testId="rutas-favoritas"
              datos={rep.rutasFavoritas.map((r) => ({ etiqueta: r.nombre, valor: r.veces }))}
            />
          ) : (
            <p className="text-[13px] text-c2-muted">Corre una ruta guardada para verla aquí.</p>
          )}
        </section>

        <div className="grid grid-cols-3 gap-2">
          <button className="boton-c2-secundario" onClick={() => void compartir()}>
            Compartir
          </button>
          <button
            className="boton-c2-secundario"
            data-testid="exportar-csv"
            onClick={() => descargar(`${nombre}.csv`, exportarCsv(rep), 'text/csv;charset=utf-8')}
          >
            CSV
          </button>
          <button
            className="boton-c2-secundario"
            data-testid="exportar-pdf"
            onClick={() => descargar(`${nombre}.pdf`, exportarPdf(rep), 'application/pdf')}
          >
            PDF
          </button>
        </div>
        {aviso && (
          <p role="status" className="text-[12px] text-c2-menta">
            {aviso}
          </p>
        )}
        <p className="text-[11px] text-c2-muted">
          Solo tú ves este reporte: se calcula con tus carreras.
        </p>
      </div>
    </div>
  )
}
