import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { consultarSeguridadDeRuta, type Nivel } from '../../seguridad'
import { TrazoRuta } from './MapaEscenario'
import { Boton, Cabecera, Chip, Confirmar, Dialogo, ErrorCarga, Vacio, decimal1, entero, useCarga, type Tono } from './kit'
import type { ClubAdmin, EventoAdmin, FuenteAdmin, RutaAdmin } from './fuente'

type Pestana = 'rutas' | 'eventos' | 'clubes'

const hoyISO = () => new Date().toLocaleDateString('en-CA')
const enDias = (n: number) => {
  const d = new Date()
  d.setDate(d.getDate() + n)
  return d.toLocaleDateString('en-CA')
}
const NIVEL_ORDEN: Nivel[] = ['bajo', 'medio', 'alto']
const NIVEL_TONO: Record<Nivel, Tono> = { bajo: 'ok', medio: 'warn', alto: 'bad' }

/** Contenido: rutas, eventos y clubes en pestanas (cada una con su ruta /admin/...). */
export function RutasEventos({ f, pestana, sufijo }: { f: FuenteAdmin; pestana: Pestana; sufijo: string }) {
  const eventos = useCarga<EventoAdmin[]>(() => f.eventos(), [f])
  const [editando, setEditando] = useState<Partial<EventoAdmin> | null>(null)
  return (
    <>
      <Cabecera
        kicker="Contenido"
        extra={
          <>
            {(
              [
                ['rutas', 'Rutas'],
                ['eventos', 'Eventos'],
                ['clubes', 'Clubes'],
              ] as [Pestana, string][]
            ).map(([k, t]) => (
              <Link
                key={k}
                to={`/admin/${k}${sufijo}`}
                className="adm-chip c-neu"
                role="tab"
                aria-selected={pestana === k}
              >
                {t}
              </Link>
            ))}
            <Boton tipo="pri" onClick={() => setEditando({ title: '' })}>
              Crear evento
            </Boton>
          </>
        }
      >
        Rutas y eventos
      </Cabecera>
      {pestana === 'rutas' && <PanelRutas f={f} eventos={eventos.datos} />}
      {pestana === 'eventos' && <ListaEventos f={f} carga={eventos} onEditar={setEditando} />}
      {pestana === 'clubes' && <ListaClubes f={f} />}
      <EditorEvento
        f={f}
        editando={editando}
        setEditando={setEditando}
        alGuardar={() => {
          setEditando(null)
          eventos.recargar()
        }}
      />
    </>
  )
}

/** Nivel de riesgo del peor dia segun el indice de seguridad (ECU 911 y reportes de corredores). */
function SeguridadRuta({ id }: { id: string }) {
  const [nivel, setNivel] = useState<Nivel | null | undefined>(undefined)
  useEffect(() => {
    let vivo = true
    consultarSeguridadDeRuta(id).then(
      (z) => {
        if (!vivo) return
        const niveles = (z?.dias ?? []).map((d) => NIVEL_ORDEN.indexOf(d.nivel))
        setNivel(niveles.length ? NIVEL_ORDEN[Math.max(...niveles)] : null)
      },
      () => vivo && setNivel(null)
    )
    return () => {
      vivo = false
    }
  }, [id])
  if (nivel === undefined) return <span className="adm-t3" style={{ fontSize: 12 }}>Seguridad: cargando</span>
  if (nivel === null)
    return (
      <span className="adm-t3" style={{ fontSize: 12 }} title="La ruta no tiene datos de seguridad del ECU 911 todavía.">
        Seguridad: sin datos
      </span>
    )
  return (
    <span className="adm-fila" style={{ gap: 8, fontSize: 12 }}>
      <span className="adm-t3">Seguridad</span>
      <Chip tono={NIVEL_TONO[nivel]} titulo="Nivel del peor día de la semana. Fuente: ECU 911 y reportes de corredores.">
        Peor día: {nivel}
      </Chip>
    </span>
  )
}

function PanelRutas({ f, eventos }: { f: FuenteAdmin; eventos: EventoAdmin[] | null }) {
  const rutas = useCarga<RutaAdmin[]>(() => f.rutas(), [f])
  const uso = useCarga(() => f.estadisticas(enDias(-89), hoyISO()), [f])
  const proximos = useCarga(() => f.estadisticas(hoyISO(), enDias(180)), [f])
  const [todas, setTodas] = useState(false)
  const [borrar, setBorrar] = useState<RutaAdmin | null>(null)

  const usos = useMemo(() => new Map((uso.datos?.rutas_top ?? []).map((r) => [r.id, r.corridas])), [uso.datos])
  const ordenadas = useMemo(
    () =>
      [...(rutas.datos ?? [])].sort(
        (a, b) => (usos.get(b.id) ?? 0) - (usos.get(a.id) ?? 0) || a.name.localeCompare(b.name)
      ),
    [rutas.datos, usos]
  )
  const visibles = todas ? ordenadas : ordenadas.slice(0, 4)
  const inscritos = useMemo(
    () => new Map((proximos.datos?.eventos_top ?? []).map((e) => [e.id, e.inscritos])),
    [proximos.datos]
  )
  const prox = (eventos ?? [])
    .filter((e) => e.starts_at && new Date(e.starts_at).getTime() > Date.now())
    .sort((a, b) => new Date(a.starts_at!).getTime() - new Date(b.starts_at!).getTime())
    .slice(0, 4)

  return (
    <div className="adm-g2 adm-g-rut">
      <section>
        <div className="adm-fila" style={{ justifyContent: 'space-between', marginBottom: 12 }}>
          <h2 className="adm-h2">Rutas más usadas</h2>
          {ordenadas.length > 4 && (
            <button
              type="button"
              onClick={() => setTodas((x) => !x)}
              style={{ background: 'none', border: 0, color: 'hsl(var(--exito-texto))', fontWeight: 600, cursor: 'pointer' }}
            >
              {todas ? 'Ver menos' : `Ver las ${ordenadas.length}`}
            </button>
          )}
        </div>
        <ErrorCarga mensaje={rutas.error} />
        {ordenadas.length === 0 && rutas.datos ? (
          <Vacio titulo="Aún no hay rutas" texto="Las rutas oficiales y las propuestas por corredores aparecerán aquí." />
        ) : (
          <div className="adm-tres" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))' }} data-testid="rutas-lista">
            {visibles.map((r) => (
              <article key={r.id} className="adm-card" style={{ overflow: 'hidden' }}>
                {r.path.length > 1 ? (
                  <TrazoRuta puntos={r.path} etiqueta={`Trazado de ${r.name}`} />
                ) : (
                  <div style={{ height: 120, background: 'hsl(var(--mapa-fondo))', display: 'grid', placeItems: 'center' }} className="adm-t3">
                    Sin trazado guardado
                  </div>
                )}
                <div className="adm-pad" style={{ display: 'grid', gap: 8, padding: 14 }}>
                  <div className="adm-fila" style={{ justifyContent: 'space-between' }}>
                    <p className="adm-ell" style={{ fontWeight: 700 }}>{r.name}</p>
                    <Chip tono={r.is_official ? 'ok' : 'neu'}>{r.is_official ? 'Oficial' : 'Comunitaria'}</Chip>
                  </div>
                  <p className="adm-t2" style={{ fontSize: 13 }}>
                    {decimal1(r.distance_km)} km · desnivel +{entero(r.elevation_m)} m ·{' '}
                    {usos.has(r.id) ? `${entero(usos.get(r.id)!)} recorridos en 90 días` : 'sin recorridos en 90 días'}
                  </p>
                  <div className="adm-fila" style={{ justifyContent: 'space-between' }}>
                    <SeguridadRuta id={r.id} />
                    <Boton chico aria={`Eliminar ${r.id}`} onClick={() => setBorrar(r)}>
                      Eliminar
                    </Boton>
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
      <section>
        <h2 className="adm-h2" style={{ marginBottom: 12 }}>Próximos eventos</h2>
        {prox.length === 0 ? (
          <Vacio titulo="Sin eventos próximos" texto="Crea uno con el botón Crear evento." />
        ) : (
          <div style={{ display: 'grid', gap: 10 }}>
            {prox.map((e) => (
              <TarjetaEvento key={e.id} e={e} inscritos={inscritos.get(e.id)} />
            ))}
          </div>
        )}
      </section>
      <Confirmar
        abierto={borrar != null}
        titulo="¿Eliminar la ruta?"
        texto={`Se elimina ${borrar?.name ?? ''} para todos los usuarios. No se puede deshacer.`}
        confirmar="Eliminar"
        onCerrar={() => setBorrar(null)}
        onConfirmar={() => {
          if (borrar) void f.borrarRuta(borrar.id).then(rutas.recargar)
          setBorrar(null)
        }}
      />
    </div>
  )
}

function TarjetaEvento({ e, inscritos, acciones }: { e: EventoAdmin; inscritos?: number; acciones?: ReactNode }) {
  const fecha = e.starts_at ? new Date(e.starts_at) : null
  return (
    <article className="adm-card adm-pad" style={{ padding: 14 }} data-testid="tarjeta-evento">
      <div className="adm-fila" style={{ alignItems: 'flex-start' }}>
        <div className="adm-grow">
          <p style={{ fontWeight: 700 }}>{e.title}</p>
          <p className="adm-t2" style={{ fontSize: 13 }}>
            {fecha
              ? fecha.toLocaleString('es-EC', { weekday: 'short', day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })
              : 'Sin fecha'}
          </p>
        </div>
        {fecha && (
          <span className="adm-card-el" style={{ textAlign: 'center', padding: '4px 10px', background: 'hsl(var(--primario) / 0.18)' }}>
            <b style={{ display: 'block', fontSize: 20, lineHeight: 1.1 }}>{fecha.getDate()}</b>
            <small style={{ fontSize: 11, fontWeight: 700, textTransform: 'uppercase', color: 'hsl(var(--primario-texto))' }}>
              {fecha.toLocaleDateString('es-EC', { month: 'short' }).replace('.', '')}
            </small>
          </span>
        )}
      </div>
      <div className="adm-fila adm-envuelve" style={{ marginTop: 8, gap: 6 }}>
        {e.location && <Chip>{e.location}</Chip>}
        <Chip>{decimal1(e.distance_km)} km</Chip>
        <Chip tono={e.published ? 'ok' : 'neu'}>{e.published ? 'Publicado' : 'Borrador'}</Chip>
        {inscritos != null && <Chip tono="pri">{entero(inscritos)} inscritos</Chip>}
      </div>
      {acciones && <div className="adm-fila adm-envuelve" style={{ marginTop: 10 }}>{acciones}</div>}
    </article>
  )
}

function ListaEventos({
  f,
  carga,
  onEditar,
}: {
  f: FuenteAdmin
  carga: { datos: EventoAdmin[] | null; error: string | null; recargar: () => void }
  onEditar: (e: EventoAdmin) => void
}) {
  return (
    <>
      <ErrorCarga mensaje={carga.error} />
      {carga.datos?.length === 0 && <Vacio titulo="Aún no hay eventos" texto="Crea el primero con el botón Crear evento." />}
      <div className="adm-tres" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))' }}>
        {(carga.datos ?? []).map((e) => (
          <TarjetaEvento
            key={e.id}
            e={e}
            acciones={
              <>
                <Boton chico onClick={() => onEditar(e)}>Editar</Boton>
                <Boton
                  chico
                  tipo={e.published ? 'sec' : 'pri'}
                  onClick={() => void f.guardarEvento({ ...e, published: !e.published }).then(carga.recargar)}
                >
                  {e.published ? 'Despublicar' : 'Publicar'}
                </Boton>
              </>
            }
          />
        ))}
      </div>
    </>
  )
}

function ListaClubes({ f }: { f: FuenteAdmin }) {
  const c = useCarga<ClubAdmin[]>(() => f.clubes(), [f])
  const [borrar, setBorrar] = useState<ClubAdmin | null>(null)
  return (
    <>
      <ErrorCarga mensaje={c.error} />
      {c.datos?.length === 0 && <Vacio titulo="Aún no hay clubes" />}
      <ul style={{ display: 'grid', gap: 10, listStyle: 'none' }}>
        {(c.datos ?? []).map((x) => (
          <li key={x.id} className="adm-card adm-pad adm-fila" style={{ padding: 14 }}>
            <div className="adm-grow">
              <p style={{ fontWeight: 700 }}>{x.name}</p>
              <p className="adm-t2" style={{ fontSize: 13 }}>{x.description}</p>
            </div>
            <Boton chico aria={`Eliminar ${x.id}`} onClick={() => setBorrar(x)}>Eliminar</Boton>
          </li>
        ))}
      </ul>
      <Confirmar
        abierto={borrar != null}
        titulo="¿Eliminar el club?"
        texto={`Se elimina ${borrar?.name ?? ''} para todos los usuarios. No se puede deshacer.`}
        confirmar="Eliminar"
        onCerrar={() => setBorrar(null)}
        onConfirmar={() => {
          if (borrar) void f.borrarClub(borrar.id).then(c.recargar)
          setBorrar(null)
        }}
      />
    </>
  )
}

function EditorEvento({
  f,
  editando,
  setEditando,
  alGuardar,
}: {
  f: FuenteAdmin
  editando: Partial<EventoAdmin> | null
  setEditando: (e: Partial<EventoAdmin> | null) => void
  alGuardar: () => void
}) {
  const campo = (k: keyof EventoAdmin, etiqueta: string, tipo = 'text') => (
    <label className="adm-label">
      {etiqueta}
      <input
        aria-label={etiqueta}
        type={tipo}
        value={String(editando?.[k] ?? '')}
        onChange={(e) =>
          setEditando({ ...editando, [k]: tipo === 'number' ? Number(e.target.value) : e.target.value })
        }
        className="adm-input"
      />
    </label>
  )
  return (
    <Dialogo
      abierto={editando != null}
      titulo={editando?.id ? 'Editar evento' : 'Nuevo evento'}
      onCerrar={() => setEditando(null)}
      acciones={
        <>
          <Boton onClick={() => setEditando(null)}>Cancelar</Boton>
          <Boton
            tipo="pri"
            onClick={() => {
              if (!editando?.title?.trim()) return
              void f.guardarEvento({ ...editando, title: editando.title.trim() }).then(alGuardar)
            }}
          >
            Guardar
          </Boton>
        </>
      }
    >
      <p className="adm-t2" style={{ fontSize: 14 }}>
        Los eventos publicados aparecen en la app para todos los corredores.
      </p>
      {campo('title', 'Título')}
      {campo('description', 'Descripción')}
      {campo('location', 'Lugar')}
      {campo('starts_at', 'Fecha y hora', 'datetime-local')}
      {campo('distance_km', 'Distancia en km', 'number')}
    </Dialogo>
  )
}
