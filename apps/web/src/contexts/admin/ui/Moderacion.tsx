import { useEffect, useMemo, useState } from 'react'
import { useLocation } from 'react-router-dom'
import {
  ESTADOS,
  MOTIVOS,
  estaAbierto,
  idCorto,
  nombreEstado,
  siguienteCaso,
  tonoEstado,
} from './casos'
import {
  Avatar,
  Boton,
  Cabecera,
  Chip,
  Confirmar,
  ErrorCarga,
  Icono,
  Vacio,
  haceCuanto,
  useCarga,
} from './kit'
import {
  definicionTipo,
  tiposRegistrados,
  type AccionModeracion,
  type EstadoReporte,
  type FuenteAdmin,
  type ReporteAdmin,
  type TipoReporte,
} from './fuente'

const HISTORIAL: Record<AccionModeracion, string> = {
  en_revision: 'Tomó el caso',
  resuelto: 'Resolvió',
  descartado: 'Descartó',
  reabierto: 'Reabrió',
  ocultar_contenido: 'Ocultó el contenido',
  suspender_usuario: 'Suspendió al autor',
  nota: 'Nota',
}

const etiquetaTipo = (t: TipoReporte) => definicionTipo(t).etiqueta

type Pendiente = { accion: AccionModeracion; titulo: string; texto: string; boton: string }

/** Moderacion: cola, caso seleccionado y acciones (tres columnas en escritorio). */
export function Moderacion({ f }: { f: FuenteAdmin }) {
  const { datos, error, recargar } = useCarga<ReporteAdmin[]>(() => f.reportes(), [f])
  const { state } = useLocation() as { state: { caso?: string } | null }
  const [filtro, setFiltro] = useState<EstadoReporte>('pendiente')
  const [tipo, setTipo] = useState<TipoReporte | null>(null)
  const [elegido, setElegido] = useState<string | null>(state?.caso ?? null)
  const [nota, setNota] = useState('')
  const [fallo, setFallo] = useState<string | null>(null)
  const [confirmar, setConfirmar] = useState<Pendiente | null>(null)

  const todos = useMemo(() => datos ?? [], [datos])
  const lista = todos.filter((r) => r.status === filtro && (!tipo || r.target_type === tipo))
  const tipos = [...new Set([...tiposRegistrados(), ...todos.map((r) => r.target_type)])]
  const cuenta = (s: EstadoReporte) => todos.filter((r) => r.status === s).length

  // Si el caso llegado desde otra pantalla esta en otro estado, se abre en su lista.
  useEffect(() => {
    const llegado = state?.caso && todos.find((r) => r.id === state.caso)
    if (llegado) setFiltro(llegado.status)
    // Solo al cargar los datos por primera vez.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [datos === null])

  const actual = lista.find((r) => r.id === elegido) ?? lista[0] ?? null
  const siguiente = useMemo(() => siguienteCaso(todos), [todos])

  const hacer = (r: ReporteAdmin, a: AccionModeracion, texto = nota.trim()) => {
    setFallo(null)
    return f.moderar(r, a, texto).then(
      () => {
        setNota('')
        recargar()
      },
      (e: Error) => setFallo(e.message)
    )
  }

  const asignarme = async () => {
    if (!siguiente) return
    await hacer(siguiente, 'en_revision', '')
    setFiltro('en_revision')
    setElegido(siguiente.id)
  }

  const pedir = (r: ReporteAdmin, accion: AccionModeracion) => {
    const def = definicionTipo(r.target_type)
    const usuario = accion === 'suspender_usuario'
    setConfirmar({
      accion,
      titulo: usuario ? 'Suspender al autor' : (def.accionOcultar ?? 'Ocultar contenido'),
      texto: usuario
        ? 'La cuenta del autor queda suspendida y se avisa a quien reportó. Solo un administrador puede hacerlo.'
        : 'El contenido deja de verse para los demás y el caso se marca como resuelto.',
      boton: usuario ? 'Suspender cuenta' : (def.accionOcultar ?? 'Ocultar'),
    })
  }

  return (
    <>
      <Cabecera
        kicker="Cola de reportes"
        extra={ESTADOS.map(([s, t]) => (
          <button
            key={s}
            type="button"
            role="tab"
            aria-selected={filtro === s}
            className={`adm-chip c-${filtro === s ? 'pri' : tonoEstado(s)}`}
            style={{
              cursor: 'pointer',
              border: filtro === s ? '1px solid hsl(var(--primario) / 0.5)' : 0,
            }}
            onClick={() => {
              setFiltro(s)
              setElegido(null)
            }}
          >
            {t} {cuenta(s)}
          </button>
        ))}
      >
        Moderación
      </Cabecera>
      <ErrorCarga mensaje={error} />
      <div
        className="adm-fila adm-envuelve"
        role="group"
        aria-label="Tipo de contenido"
        style={{ marginBottom: 14, gap: 6 }}
      >
        {[null, ...tipos].map((t) => (
          <button
            key={t ?? 'todos'}
            type="button"
            className="adm-chip c-neu"
            aria-pressed={tipo === t}
            onClick={() => {
              setTipo(t)
              setElegido(null)
            }}
          >
            {t ? etiquetaTipo(t) : 'Todo'}
          </button>
        ))}
      </div>

      <div className="adm-g2 adm-g-mod">
        <section className="adm-card adm-pad" aria-label="Cola" style={{ alignSelf: 'start' }}>
          <h2 className="adm-h2" style={{ marginBottom: 12 }}>
            Cola
          </h2>
          {lista.length === 0 ? (
            <Vacio
              titulo="No hay reportes en esta lista"
              texto="Cambia el estado o el tipo para ver otros casos."
            />
          ) : (
            <ul
              style={{
                display: 'grid',
                gridTemplateColumns: 'minmax(0, 1fr)',
                gap: 8,
                listStyle: 'none',
              }}
            >
              {lista.map((r) => (
                <li key={r.id}>
                  <button
                    type="button"
                    className={`adm-caso c-${tonoEstado(r.status)}`}
                    aria-current={actual?.id === r.id}
                    onClick={() => setElegido(r.id)}
                    data-testid={`reporte-${r.id}`}
                  >
                    <span
                      className="adm-fila"
                      style={{ justifyContent: 'space-between', fontSize: 12 }}
                    >
                      <span className="adm-fila" style={{ gap: 8 }}>
                        <span className="adm-t3" style={{ fontWeight: 700 }}>
                          {idCorto(r)}
                        </span>
                        <Chip>{etiquetaTipo(r.target_type)}</Chip>
                      </span>
                      <span className="adm-t3">{haceCuanto(r.created_at)}</span>
                    </span>
                    <span
                      className="adm-ell"
                      style={{ display: 'block', fontWeight: 700, marginTop: 6 }}
                    >
                      {r.resumen}
                    </span>
                    <span className="adm-t3" style={{ display: 'block', fontSize: 12 }}>
                      {MOTIVOS[r.reason] ?? r.reason} · {r.total_reportes}{' '}
                      {r.total_reportes === 1 ? 'reporte' : 'reportes'}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>

        {actual ? (
          <>
            <Detalle r={actual} />
            <Acciones
              r={actual}
              nota={nota}
              setNota={setNota}
              fallo={fallo}
              hacer={hacer}
              pedir={pedir}
              siguiente={siguiente}
              asignarme={asignarme}
            />
          </>
        ) : (
          <section className="adm-card adm-pad" style={{ gridColumn: 'span 2' }}>
            <Vacio
              titulo="Sin caso seleccionado"
              texto="Elige un reporte de la cola para ver su detalle y las acciones."
            />
            {siguiente && (
              <div style={{ marginTop: 12 }}>
                <Boton tipo="pri" onClick={() => void asignarme()}>
                  Asignarme el siguiente caso
                </Boton>
              </div>
            )}
          </section>
        )}
      </div>

      <Confirmar
        abierto={confirmar != null && actual != null}
        titulo={confirmar?.titulo ?? ''}
        texto={confirmar?.texto ?? ''}
        confirmar={confirmar?.boton ?? ''}
        onCerrar={() => setConfirmar(null)}
        onConfirmar={() => {
          if (confirmar && actual) void hacer(actual, confirmar.accion)
          setConfirmar(null)
        }}
      />
    </>
  )
}

function Detalle({ r }: { r: ReporteAdmin }) {
  const [abierto, setAbierto] = useState(true)
  return (
    <section
      className="adm-card adm-pad"
      aria-label="Detalle del caso"
      style={{ alignSelf: 'start' }}
      data-testid="detalle-caso"
    >
      <div className="adm-fila adm-envuelve" style={{ marginBottom: 10 }}>
        <Chip tono="pri">{etiquetaTipo(r.target_type)}</Chip>
        <Chip tono="bad">{MOTIVOS[r.reason] ?? r.reason}</Chip>
        <Chip tono={tonoEstado(r.status)}>{nombreEstado(r.status)}</Chip>
        {r.total_reportes > 1 && <Chip tono="ok">{r.total_reportes} reportes</Chip>}
      </div>
      <h2 style={{ fontSize: 24, fontWeight: 800, letterSpacing: '-0.02em', lineHeight: 1.15 }}>
        {r.resumen}
      </h2>
      <div className="adm-cita" style={{ margin: '16px 0' }}>
        <p className="adm-lbl" style={{ color: 'hsl(var(--primario-texto))' }}>
          Reporte
        </p>
        <p style={{ fontSize: 15, marginTop: 4 }}>
          {r.detail ? `\u201C${r.detail}\u201D` : 'Sin detalle'}
        </p>
      </div>
      <div className="adm-fila">
        <Avatar nombre={r.reportante || 'Anónimo'} />
        <div>
          <p style={{ fontWeight: 700, fontSize: 14 }}>{r.reportante || 'Sin nombre'}</p>
          <p className="adm-t3" style={{ fontSize: 12 }}>
            Reportó {haceCuanto(r.created_at)} · {new Date(r.created_at).toLocaleString('es-EC')}
          </p>
        </div>
      </div>
      {r.acciones.length > 0 && (
        <div style={{ marginTop: 18 }}>
          <button
            type="button"
            onClick={() => setAbierto((x) => !x)}
            aria-expanded={abierto}
            className="adm-fila"
            style={{
              background: 'none',
              border: 0,
              color: 'hsl(var(--primario-texto))',
              fontWeight: 600,
              fontSize: 13,
              cursor: 'pointer',
              padding: 0,
            }}
          >
            <Icono nombre={abierto ? 'expand_less' : 'expand_more'} tam={18} />
            Historial ({r.acciones.length})
          </button>
          {abierto && (
            <ol
              data-testid="historial"
              style={{
                marginTop: 8,
                paddingLeft: 12,
                borderLeft: '2px solid hsl(var(--borde))',
                listStyle: 'none',
                display: 'grid',
                gap: 6,
              }}
            >
              {r.acciones.map((h, i) => (
                <li key={i} className="adm-t2" style={{ fontSize: 13 }}>
                  <strong style={{ color: 'hsl(var(--texto))' }}>
                    {h.moderador || 'Moderación'}
                  </strong>{' '}
                  {HISTORIAL[h.accion] ?? h.accion}
                  {h.nota ? `: ${h.nota}` : ''}{' '}
                  <span className="adm-t3">({new Date(h.fecha).toLocaleString('es-EC')})</span>
                </li>
              ))}
            </ol>
          )}
        </div>
      )}
    </section>
  )
}

function Acciones({
  r,
  nota,
  setNota,
  fallo,
  hacer,
  pedir,
  siguiente,
  asignarme,
}: {
  r: ReporteAdmin
  nota: string
  setNota: (n: string) => void
  fallo: string | null
  hacer: (r: ReporteAdmin, a: AccionModeracion) => Promise<void>
  pedir: (r: ReporteAdmin, a: AccionModeracion) => void
  siguiente: ReporteAdmin | null
  asignarme: () => Promise<void>
}) {
  const def = definicionTipo(r.target_type)
  const incidente = r.target_type === 'incidente'
  const abierto = estaAbierto(r)
  return (
    <aside
      className="adm-card adm-pad"
      aria-label="Acciones"
      style={{ alignSelf: 'start', display: 'flex', flexDirection: 'column', gap: 10 }}
    >
      <h2 className="adm-h2">Acciones</h2>
      <input
        value={nota}
        onChange={(e) => setNota(e.target.value)}
        placeholder="Nota para el historial y el reportante"
        aria-label="Nota de moderación"
        className="adm-input"
      />
      {abierto ? (
        <>
          {r.status === 'pendiente' && (
            <Boton completo onClick={() => void hacer(r, 'en_revision')}>
              Revisar
            </Boton>
          )}
          {incidente ? (
            <Boton tipo="ok" completo onClick={() => void hacer(r, 'descartado')}>
              Verificar incidente
            </Boton>
          ) : (
            <Boton completo onClick={() => void hacer(r, 'descartado')}>
              Descartar
            </Boton>
          )}
          {def.accionOcultar && (
            <Boton
              tipo="bad"
              completo
              onClick={() =>
                pedir(r, r.target_type === 'usuario' ? 'suspender_usuario' : 'ocultar_contenido')
              }
            >
              {def.accionOcultar}
            </Boton>
          )}
          {r.target_type !== 'usuario' && (
            <Boton completo onClick={() => pedir(r, 'suspender_usuario')}>
              Suspender autor
            </Boton>
          )}
        </>
      ) : (
        <Boton completo onClick={() => void hacer(r, 'reabierto')}>
          Reabrir
        </Boton>
      )}
      {nota.trim() && (
        <Boton tipo="gho" completo onClick={() => void hacer(r, 'nota')}>
          Solo nota
        </Boton>
      )}
      {incidente && abierto && (
        <p className="adm-t3" style={{ fontSize: 12 }}>
          Verificar deja el incidente visible en el mapa; rechazarlo lo quita del índice de
          seguridad.
        </p>
      )}
      {fallo && (
        <p role="alert" style={{ fontSize: 13, color: 'hsl(var(--peligro-texto))' }}>
          {fallo}
        </p>
      )}
      <div className="adm-card-el" style={{ padding: 14, marginTop: 'auto' }}>
        <Chip tono="pri">Nuevo</Chip>
        <p style={{ fontWeight: 700, margin: '8px 0 4px' }}>Asignarme el siguiente caso</p>
        <p className="adm-t3" style={{ fontSize: 12, marginBottom: 10 }}>
          Toma el caso pendiente más antiguo y queda en tu historial.
        </p>
        <Boton
          chico
          completo
          deshabilitado={!siguiente}
          onClick={() => void asignarme()}
          testId="asignarme-siguiente"
        >
          {siguiente ? `Tomar ${idCorto(siguiente)}` : 'Sin casos pendientes'}
        </Boton>
      </div>
    </aside>
  )
}
