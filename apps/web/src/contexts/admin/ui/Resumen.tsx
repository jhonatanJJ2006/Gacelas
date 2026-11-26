import { useMemo } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { MapaEscenario } from './MapaEscenario'
import { siguienteCaso, MOTIVOS, idCorto } from './casos'
import { Avatar, Boton, Cabecera, Chip, ErrorCarga, Icono, Vacio, entero, haceCuanto, useCarga } from './kit'
import { definicionTipo, type FuenteAdmin } from './fuente'

const MESES = ['', 'ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']

function saludo(h = new Date().getHours()) {
  return h < 12 ? 'Buenos días' : h < 19 ? 'Buenas tardes' : 'Buenas noches'
}

/** Resumen: escenario de Loja en vivo, accion rapida, cifras y cola de moderacion. */
export function Resumen({ f, nombre, sufijo }: { f: FuenteAdmin; nombre: string; sufijo: string }) {
  const navigate = useNavigate()
  const m = useCarga(() => f.metricas(), [f])
  const cola = useCarga(() => f.reportes(), [f])
  const vivo = useCarga(() => f.enVivo(), [f])
  const ecu = useCarga(() => f.importacionesEcu(), [f])
  const siguiente = useMemo(() => siguienteCaso(cola.datos ?? []), [cola.datos])
  const abiertos = useMemo(
    () => (cola.datos ?? []).filter((r) => r.status === 'pendiente').slice(0, 3),
    [cola.datos]
  )
  const d = m.datos
  const ultimo = ecu.datos?.[0]
  const hoy = new Date().toLocaleDateString('es-EC', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
  const irACaso = (id?: string) =>
    navigate(`/admin/moderacion${sufijo}`, { state: id ? { caso: id } : undefined })

  return (
    <>
      <Cabecera kicker="Loja, Ecuador" extra={<Chip>{hoy}</Chip>}>
        {saludo()}, {nombre}
      </Cabecera>
      <ErrorCarga mensaje={m.error} />
      <div className="adm-g2 adm-g-resumen">
        <section className="adm-card" style={{ overflow: 'hidden' }} aria-label="Escenario de Loja">
          <div className="adm-mapa" style={{ height: 300 }}>
            <MapaEscenario
              corredores={vivo.datos?.corredores ?? []}
              incidentes={vivo.datos?.incidentes ?? []}
              etiqueta="Esquema de Loja con corredores visibles e incidentes por revisar"
            />
            <span className="adm-mapa-chip" style={{ left: 14 }}>
              <span className={`adm-dot ${vivo.datos ? 'c-ok' : 'c-neu'}`} />
              {vivo.datos ? 'EN VIVO' : 'SIN FUENTE'}
            </span>
            <span
              className="adm-mapa-chip adm-t2"
              style={{ right: 14 }}
              title="Las alertas SOS no tienen lectura para el panel: la tabla seguridad.sos_alertas no concede acceso a usuarios autenticados."
              data-testid="sos-pendiente"
            >
              <Icono nombre="warning" tam={16} /> SOS: fuente pendiente
            </span>
          </div>
          <div
            className="adm-sep"
            style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', textAlign: 'center', padding: '14px 8px' }}
            data-testid="escenario-cifras"
          >
            <Cifra
              valor={vivo.datos ? entero(vivo.datos.corredores.length) : '-'}
              etiqueta="Corredores en vivo"
              tono="ok"
            />
            <Cifra
              valor={vivo.datos ? entero(vivo.datos.incidentes.length) : '-'}
              etiqueta="Incidentes por revisar"
              tono="warn"
            />
            <Cifra
              valor={d ? entero(d.reportes_pendientes) : '-'}
              etiqueta="Casos en la cola"
              tono="pri"
            />
          </div>
        </section>

        <div className="adm-g2" style={{ alignContent: 'start' }}>
          <button
            type="button"
            className="adm-amb"
            disabled={!siguiente}
            onClick={() => irACaso(siguiente?.id)}
            data-testid="atender-siguiente"
          >
            <span className="adm-grow">
              <span style={{ display: 'block', fontSize: 11, fontWeight: 700, letterSpacing: '0.12em', textTransform: 'uppercase', opacity: 0.85 }}>
                Acción rápida
              </span>
              <span style={{ display: 'block', fontSize: 24, fontWeight: 800, letterSpacing: '-0.02em' }}>
                {siguiente ? 'Atender siguiente caso' : 'Sin casos pendientes'}
              </span>
              <span style={{ display: 'block', fontSize: 13, opacity: 0.9 }}>
                {siguiente
                  ? `${idCorto(siguiente)} · ${definicionTipo(siguiente.target_type).etiqueta} · ${haceCuanto(siguiente.created_at)}`
                  : 'La cola está al día'}
              </span>
            </span>
            {siguiente && (
              <span className="adm-amb-play">
                <Icono nombre="play_arrow" tam={30} />
              </span>
            )}
          </button>
          <div className="adm-tres" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }} data-testid="kpis">
            <Kpi icono="group" valor={d ? entero(d.usuarios) : '-'} etiqueta="Usuarios" />
            <Kpi icono="map" valor={d ? entero(d.rutas) : '-'} etiqueta="Rutas" />
            <Kpi icono="event" valor={d ? entero(d.eventos_proximos) : '-'} etiqueta="Eventos" />
          </div>
          <section className="adm-card adm-pad" style={{ background: 'linear-gradient(135deg, hsl(var(--realce-primario)), hsl(var(--superficie)))' }}>
            <div className="adm-fila" style={{ justifyContent: 'space-between' }}>
              <p className="adm-lbl" style={{ color: 'hsl(var(--primario-texto))' }}>
                Importación ECU 911
              </p>
              {ultimo && (
                <span className="adm-t2" style={{ fontSize: 13 }}>
                  Periodo {ultimo.anio}-{String(ultimo.mes).padStart(2, '0')}
                </span>
              )}
            </div>
            {ultimo ? (
              <>
                <p className="adm-num" style={{ fontSize: 28, margin: '6px 0 2px' }}>
                  {entero(ultimo.agrupaciones)} agrupaciones
                </p>
                <p className="adm-t3" style={{ fontSize: 12 }}>
                  {MESES[ultimo.mes]} {ultimo.anio}, filas agregadas por parroquia y día
                </p>
              </>
            ) : (
              <div style={{ marginTop: 8 }}>
                <Vacio
                  testId="ecu-vacio"
                  titulo={ecu.error ? 'No se pudo leer la carga del ECU 911' : 'Sin cargas del ECU 911'}
                  texto={ecu.error ?? 'Todavía no hay datos en incidentes_agregados.'}
                />
              </div>
            )}
          </section>
        </div>
      </div>

      <div className="adm-fila" style={{ justifyContent: 'space-between', margin: '28px 0 12px' }}>
        <h2 className="adm-h2">Cola de moderación</h2>
        <Link to={`/admin/moderacion${sufijo}`} style={{ color: 'hsl(var(--exito-texto))', fontWeight: 600, fontSize: 14 }}>
          Ver toda la cola
        </Link>
      </div>
      {abiertos.length === 0 ? (
        <Vacio titulo="No hay casos pendientes" texto="Cuando alguien reporte contenido aparecerá aquí." />
      ) : (
        <div className="adm-tres">
          {abiertos.map((r) => (
            <article key={r.id} className="adm-card adm-pad" style={{ display: 'flex', flexDirection: 'column', gap: 8 }} data-testid="caso-resumen">
              <div className="adm-fila adm-envuelve">
                <Chip tono="pri">{definicionTipo(r.target_type).etiqueta}</Chip>
                <Chip tono="bad">{MOTIVOS[r.reason] ?? r.reason}</Chip>
                <span className="adm-t3" style={{ marginLeft: 'auto', fontSize: 12 }}>
                  {haceCuanto(r.created_at)}
                </span>
              </div>
              <p className="adm-ell" style={{ fontWeight: 700 }}>
                {r.resumen}
              </p>
              <p className="adm-t2" style={{ fontSize: 14, minHeight: 40 }}>
                {r.detail || 'Sin detalle'}
              </p>
              <div className="adm-fila" style={{ marginTop: 'auto' }}>
                <Avatar nombre={r.reportante || 'Anónimo'} tam={28} />
                <span className="adm-t2 adm-ell adm-grow" style={{ fontSize: 12 }}>
                  {r.reportante || 'Sin nombre'}
                  {r.total_reportes > 1 ? ` · ${r.total_reportes} reportes` : ''}
                </span>
                <Boton chico onClick={() => irACaso(r.id)}>
                  Revisar
                </Boton>
              </div>
            </article>
          ))}
        </div>
      )}
      {f.demo && (
        <p className="adm-t3" style={{ fontSize: 11, marginTop: 24, letterSpacing: '0.1em', textTransform: 'uppercase' }}>
          Datos de ejemplo (solo desarrollo)
        </p>
      )}
    </>
  )
}

function Cifra({ valor, etiqueta, tono }: { valor: string; etiqueta: string; tono: 'ok' | 'warn' | 'pri' }) {
  return (
    <div className={`c-${tono}`}>
      <p className="adm-num" style={{ fontSize: 28, color: 'hsl(var(--tt))' }}>
        {valor}
      </p>
      <p className="adm-t2" style={{ fontSize: 12 }}>
        {etiqueta}
      </p>
    </div>
  )
}

function Kpi({ icono, valor, etiqueta }: { icono: string; valor: string; etiqueta: string }) {
  return (
    <div className="adm-card adm-pad" style={{ textAlign: 'center', padding: 14 }}>
      <span className="adm-t2" style={{ color: 'hsl(var(--primario-texto))' }}>
        <Icono nombre={icono} tam={18} />
      </span>
      <p className="adm-num" style={{ fontSize: 26 }}>
        {valor}
      </p>
      <p className="adm-t2" style={{ fontSize: 12 }}>
        {etiqueta}
      </p>
    </div>
  )
}
