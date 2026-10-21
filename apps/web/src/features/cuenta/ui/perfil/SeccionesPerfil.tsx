import type { Historico, KmMes, Medalla, RecordPersonal } from '../../../../lib/estadisticas'
import { formatearHms, formatearMs, formatearRitmo, Unidades } from '../../../../lib/geo'

const tarjeta = 'bg-surface-container rounded-2xl border border-border-subtle'

function Titulo({ icono, texto, extra }: { icono: string; texto: string; extra?: string }) {
  return (
    <div className="flex items-center justify-between mb-3">
      <h2 className="font-title-md text-title-md text-text-primary flex items-center gap-2">
        <span className="material-symbols-outlined text-primary text-[20px]">{icono}</span>
        {texto}
      </h2>
      {extra && <span className="font-label-md text-label-md text-text-secondary">{extra}</span>}
    </div>
  )
}

export function Contadores({
  carreras,
  seguidores,
  siguiendo,
}: {
  carreras: number
  seguidores: number
  siguiendo: number
}) {
  return (
    <div className={`${tarjeta} grid grid-cols-3 divide-x divide-border-subtle py-3 mb-4`}>
      {[
        ['Carreras', carreras, 'total-carreras'],
        ['Seguidores', seguidores, 'total-seguidores'],
        ['Siguiendo', siguiendo, 'total-siguiendo'],
      ].map(([t, v, id]) => (
        <div key={t} className="text-center">
          <div
            className="font-headline-sm text-headline-sm text-text-primary leading-none"
            data-testid={id}
          >
            {v}
          </div>
          <div className="font-label-md text-label-md text-text-secondary mt-1">{t}</div>
        </div>
      ))}
    </div>
  )
}

export function EstadisticasHistoricas({ h, m }: { h: Historico; m: boolean }) {
  const celdas: [string, string, string, string][] = [
    ['Total distancia', Unidades.texto(h.km, m, 1), Unidades.etiquetaDistancia(m), 'route'],
    ['Tiempo total', formatearHms(h.segundos), '', 'timer'],
    ['Desnivel acum.', `+${h.desnivelM}`, 'm', 'landscape'],
    [
      'Ritmo récord',
      formatearRitmo(Unidades.ritmo(h.mejorRitmoSegKm, m)),
      Unidades.etiquetaRitmo(m),
      'bolt',
    ],
  ]
  return (
    <section className="mb-6" data-testid="estadisticas-historicas">
      <Titulo icono="monitoring" texto="Estadísticas históricas" extra={`${h.carreras} carreras`} />
      <div className="grid grid-cols-2 gap-3">
        {celdas.map(([t, v, u, i]) => (
          <div key={t} className={`${tarjeta} p-4`}>
            <div className="flex justify-between items-center">
              <span className="font-label-md text-label-md text-text-secondary">{t}</span>
              <span className="material-symbols-outlined text-primary text-[18px]">{i}</span>
            </div>
            <div className="mt-2 whitespace-nowrap">
              <span className="font-headline-sm text-headline-sm text-text-primary">{v}</span>
              {u && <span className="text-body-sm text-primary ml-1">{u}</span>}
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}

export function KilometrajeMensual({ meses, m }: { meses: KmMes[]; m: boolean }) {
  const max = Math.max(1, ...meses.map((x) => x.km))
  const actual = meses[meses.length - 1]
  return (
    <section className={`${tarjeta} p-4 mb-6`} data-testid="km-mensual">
      <div className="flex justify-between items-start mb-4">
        <div>
          <h2 className="font-title-md text-title-md text-text-primary">Kilometraje mensual</h2>
          <p className="font-body-sm text-body-sm text-text-secondary">
            {meses[0]?.etiqueta} a {actual?.etiqueta} {actual?.anio}
          </p>
        </div>
        {actual && (
          <span className="rounded-full bg-primary-600/30 text-primary px-3 py-1 font-label-md text-label-md">
            {actual.etiqueta}: {Unidades.texto(actual.km, m, 1)} {Unidades.etiquetaDistancia(m)}
          </span>
        )}
      </div>
      <div className="flex items-end gap-3 h-32">
        {meses.map((x, i) => (
          <div
            key={`${x.anio}-${x.mes}`}
            className="flex-1 flex flex-col items-center gap-1 h-full"
          >
            <div className="flex-1 w-full flex items-end">
              <div
                className={`w-full rounded-t-md ${i === meses.length - 1 ? 'bg-gradient-to-t from-primary-700 to-primary' : 'bg-surface-container-highest'}`}
                style={{ height: `${Math.max(4, (x.km / max) * 100)}%` }}
                title={`${x.etiqueta}: ${x.km.toFixed(1)} km`}
              />
            </div>
            <span className="text-[11px] text-text-secondary">{x.etiqueta}</span>
          </div>
        ))}
      </div>
    </section>
  )
}

export function Records({ lista }: { lista: RecordPersonal[] }) {
  return (
    <section className="mb-6" data-testid="records">
      <Titulo icono="emoji_events" texto="Récords personales" />
      <div className="grid grid-cols-2 gap-3">
        {lista.map((r) => (
          <div key={r.etiqueta} className={`${tarjeta} p-4`}>
            <span className="rounded-full bg-primary-600/30 text-primary px-2 py-0.5 text-[11px] font-bold">
              {r.etiqueta}
            </span>
            <div className="font-headline-sm text-headline-sm text-text-primary mt-2">
              {r.segundos == null
                ? '--'
                : r.segundos >= 3600
                  ? formatearHms(r.segundos)
                  : formatearMs(r.segundos)}
            </div>
            <p className="font-body-sm text-body-sm text-text-secondary">
              {r.segundos == null ? 'Aún sin marca' : 'Mejor tiempo'}
            </p>
          </div>
        ))}
      </div>
    </section>
  )
}

export function Medallero({ lista }: { lista: Medalla[] }) {
  const logradas = lista.filter((x) => x.lograda).length
  return (
    <section className="mb-6" data-testid="medallero">
      <Titulo icono="military_tech" texto="Medallero" extra={`${logradas} de ${lista.length}`} />
      <div className="grid grid-cols-3 gap-3">
        {lista.map((x) => (
          <div
            key={x.id}
            data-lograda={x.lograda}
            className={`${tarjeta} p-3 flex flex-col items-center text-center ${x.lograda ? '' : 'opacity-40'}`}
          >
            <span
              className={`w-12 h-12 rounded-full flex items-center justify-center border-2 ${x.lograda ? 'border-primary bg-primary-600/30 text-primary' : 'border-border-subtle text-text-secondary'}`}
            >
              <span className="material-symbols-outlined">
                {x.lograda ? 'workspace_premium' : 'lock'}
              </span>
            </span>
            <span className="font-label-md text-label-md text-text-primary mt-2">{x.titulo}</span>
            <span className="text-[11px] text-text-secondary">{x.descripcion}</span>
          </div>
        ))}
      </div>
    </section>
  )
}
