import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { NavLink, Navigate, Route, Routes, useNavigate, useSearchParams } from 'react-router-dom'
import { useApp } from '../../store/app'
import { ModalC2 } from '../../components/modales'
import {
  fuenteDemo,
  fuenteSupabase,
  type AjusteApp,
  type ClubAdmin,
  type EventoAdmin,
  type FuenteAdmin,
  type Metricas,
  type AccionModeracion,
  type EstadoReporte,
  type ReporteAdmin,
  type TipoReporte,
  definicionTipo,
  tiposRegistrados,
  type RutaAdmin,
  type UsuarioAdmin,
} from './api'
import { VERSIONES_LEGALES } from '../legal/textos'
import { Estadisticas } from './Estadisticas'

const SECCIONES: [string, string, string][] = [
  ['', 'Dashboard', 'dashboard'],
  ['usuarios', 'Usuarios', 'group'],
  ['estadisticas', 'Estadísticas', 'monitoring'],
  ['moderacion', 'Moderación', 'shield'],
  ['eventos', 'Eventos', 'event'],
  ['rutas', 'Rutas', 'route'],
  ['clubes', 'Clubes', 'diversity_3'],
  ['push', 'Notificaciones', 'notifications'],
  ['versiones', 'Versiones', 'deployed_code'],
  ['ajustes', 'Ajustes y roles', 'settings'],
]

const MOTIVOS: Record<string, string> = {
  spam: 'Spam o publicidad',
  ofensivo: 'Contenido ofensivo',
  acoso: 'Acoso',
  falso: 'Carrera falsa',
  suplantacion: 'Suplantación',
  peligroso: 'Peligroso',
  otro: 'Otro',
}

/** Carga asincrona con estado de error simple. */
function useCarga<T>(f: () => Promise<T>, deps: unknown[]) {
  const [datos, setDatos] = useState<T | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [n, setN] = useState(0)
  useEffect(() => {
    let vivo = true
    f().then(
      (d) => vivo && setDatos(d),
      (e: Error) => vivo && setError(e.message)
    )
    return () => {
      vivo = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, n])
  const recargar = useCallback(() => setN((x) => x + 1), [])
  return { datos, error, recargar }
}

function Titulo({ children, extra }: { children: ReactNode; extra?: ReactNode }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 mb-6">
      <h1 className="font-display italic uppercase text-[28px] lg:text-[34px] text-white">
        {children}
      </h1>
      {extra}
    </div>
  )
}

function Etiqueta({
  color,
  children,
}: {
  color: 'mint' | 'peligro' | 'gris' | 'violeta'
  children: ReactNode
}) {
  const c = {
    mint: 'bg-[#68DBA9] text-[#0C1322]',
    peligro: 'bg-c2-peligro text-white',
    gris: 'bg-c2-s2 text-c2-muted',
    violeta: 'bg-[#C084FC] text-[#0C1322]',
  }[color]
  return (
    <span
      className={`inline-block -skew-x-12 px-2 py-0.5 text-[11px] font-black italic uppercase ${c}`}
    >
      {children}
    </span>
  )
}

function Boton({
  children,
  onClick,
  tipo = 'primario',
  aria,
}: {
  children: ReactNode
  onClick: () => void
  tipo?: 'primario' | 'secundario' | 'peligro'
  aria?: string
}) {
  const c = {
    primario: 'bg-gradient-to-r from-[#7B1FA2] to-c2-morado text-white',
    secundario: 'bg-c2-s2 text-white',
    peligro: 'bg-c2-peligro text-white',
  }[tipo]
  return (
    <button
      type="button"
      aria-label={aria}
      onClick={onClick}
      className={`h-10 px-4 -skew-x-12 font-black italic uppercase text-[13px] hover:brightness-110 ${c}`}
    >
      {children}
    </button>
  )
}

function ErrorCarga({ mensaje }: { mensaje: string | null }) {
  if (!mensaje) return null
  return (
    <p role="alert" className="mb-4 border border-c2-peligro bg-c2-card p-3 text-sm text-white">
      No se pudo cargar: {mensaje}. Revisa que las migraciones 020 y 023 estén aplicadas.
    </p>
  )
}

const tarjeta = 'bg-c2-card border border-c2-linea'

function Dashboard({ f }: { f: FuenteAdmin }) {
  const m = useCarga<Metricas>(() => f.metricas(), [f])
  const u = useCarga(() => f.usuarios(), [f])
  const d = m.datos
  const kpis: [string, string, string][] = d
    ? [
        ['Usuarios', d.usuarios.toLocaleString('es-EC'), `+${d.usuarios_7d} en 7 días`],
        ['Carreras 7 días', d.carreras_7d.toLocaleString('es-EC'), `${d.km_7d} km`],
        ['Reportes pendientes', String(d.reportes_pendientes), 'Por moderar'],
        ['Eventos próximos', String(d.eventos_proximos), `${d.clubes} clubes, ${d.rutas} rutas`],
      ]
    : []
  return (
    <>
      <Titulo>Dashboard</Titulo>
      <ErrorCarga mensaje={m.error} />
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-6" data-testid="kpis">
        {kpis.map(([t, v, s], i) => (
          <div
            key={t}
            className={`p-4 ${i === 0 ? 'bg-gradient-to-br from-[#7B1FA2] to-[#3b1a5c]' : tarjeta}`}
          >
            <p className="text-[11px] font-extrabold uppercase tracking-widest text-[#C084FC]">
              {t}
            </p>
            <p className="font-display italic text-[30px] text-white leading-tight">{v}</p>
            <p className="text-[12px] text-[#68DBA9]">{s}</p>
          </div>
        ))}
      </div>
      <section className={`${tarjeta} p-4`}>
        <h2 className="font-display italic uppercase text-white text-[18px] mb-3">
          Usuarios recientes
        </h2>
        <TablaUsuarios usuarios={(u.datos ?? []).slice(0, 5)} />
      </section>
    </>
  )
}

function TablaUsuarios({
  usuarios,
  onAbrir,
}: {
  usuarios: UsuarioAdmin[]
  onAbrir?: (u: UsuarioAdmin) => void
}) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="text-left text-[11px] uppercase tracking-widest text-c2-muted border-b border-c2-linea">
            <th className="py-2">Usuario</th>
            <th>Km totales</th>
            <th>Rol</th>
            <th>Estado</th>
          </tr>
        </thead>
        <tbody>
          {usuarios.map((u) => (
            <tr
              key={u.id}
              onClick={() => onAbrir?.(u)}
              className={`border-b border-c2-linea ${onAbrir ? 'cursor-pointer hover:bg-c2-s2' : ''}`}
            >
              <td className="py-3">
                <p className="text-white font-semibold">{u.full_name ?? u.username}</p>
                <p className="text-c2-muted text-[12px]">@{u.username}</p>
              </td>
              <td className="font-display italic text-white">{u.km} km</td>
              <td>
                {u.role ? (
                  <Etiqueta color="violeta">{u.role}</Etiqueta>
                ) : (
                  <span className="text-c2-muted">Corredor</span>
                )}
              </td>
              <td>
                {u.suspended_at ? (
                  <Etiqueta color="peligro">Suspendida</Etiqueta>
                ) : (
                  <Etiqueta color="mint">Activa</Etiqueta>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function Usuarios({ f }: { f: FuenteAdmin }) {
  const { datos, error, recargar } = useCarga(() => f.usuarios(), [f])
  const [q, setQ] = useState('')
  const [abierto, setAbierto] = useState<UsuarioAdmin | null>(null)
  const lista = (datos ?? []).filter((u) =>
    `${u.username} ${u.full_name ?? ''}`.toLowerCase().includes(q.toLowerCase())
  )
  const actual = abierto && (datos ?? []).find((u) => u.id === abierto.id)
  return (
    <>
      <Titulo
        extra={
          <input
            aria-label="Buscar usuarios"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Buscar usuarios"
            className="h-10 px-3 bg-c2-card border border-c2-linea text-white"
          />
        }
      >
        Usuarios
      </Titulo>
      <ErrorCarga mensaje={error} />
      <section className={`${tarjeta} p-4`}>
        <TablaUsuarios usuarios={lista} onAbrir={setAbierto} />
      </section>
      {actual && (
        <ModalC2
          abierto
          icono="person"
          titulo={actual.full_name ?? actual.username}
          texto={`@${actual.username}. ${actual.carreras} carreras, ${actual.km} km. Alta el ${new Date(actual.created_at).toLocaleDateString('es-EC')}.`}
          primario={actual.suspended_at ? 'Reactivar cuenta' : 'Suspender cuenta'}
          peligro={!actual.suspended_at}
          onPrimario={() => void f.suspender(actual.id, !actual.suspended_at).then(recargar)}
          secundario="Cerrar"
          onCerrar={() => setAbierto(null)}
        >
          <label className="mt-4 block text-[11px] uppercase tracking-widest text-c2-muted">
            Rol
            <select
              aria-label="Rol del usuario"
              value={actual.role ?? ''}
              onChange={(e) =>
                void f
                  .cambiarRol(actual.id, (e.target.value || null) as UsuarioAdmin['role'])
                  .then(recargar)
              }
              className="mt-1 block w-full h-10 bg-c2-s2 border border-c2-linea text-white px-2 normal-case tracking-normal text-sm"
            >
              <option value="">Corredor</option>
              <option value="moderador">Moderador</option>
              <option value="admin">Administrador</option>
            </select>
          </label>
        </ModalC2>
      )}
    </>
  )
}

const etiqueta = (t: TipoReporte) => definicionTipo(t).etiqueta
const ESTADOS: [EstadoReporte, string][] = [
  ['pendiente', 'Pendientes'],
  ['en_revision', 'En revisión'],
  ['resuelto', 'Resueltos'],
  ['descartado', 'Descartados'],
]
const ACCIONES: Record<AccionModeracion, string> = {
  en_revision: 'Tomó el caso',
  resuelto: 'Resolvió',
  descartado: 'Descartó',
  reabierto: 'Reabrió',
  ocultar_contenido: 'Ocultó el contenido',
  suspender_usuario: 'Suspendió al autor',
  nota: 'Nota',
}

function TarjetaReporte({
  r,
  f,
  recargar,
}: {
  r: ReporteAdmin
  f: FuenteAdmin
  recargar: () => void
}) {
  const [nota, setNota] = useState('')
  const [abierto, setAbierto] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const hacer = (a: AccionModeracion) =>
    void f.moderar(r, a, nota.trim()).then(
      () => {
        setNota('')
        recargar()
      },
      (e: Error) => setError(e.message)
    )
  const abiertoCaso = r.status === 'pendiente' || r.status === 'en_revision'
  const ocultar = definicionTipo(r.target_type).accionOcultar
  return (
    <li className={`${tarjeta} p-4`} data-testid={`reporte-${r.id}`}>
      <div className="flex flex-col lg:flex-row gap-4">
        <div className="flex-1 min-w-0">
          <div className="flex flex-wrap items-center gap-1.5 mb-1">
            <Etiqueta color="violeta">{etiqueta(r.target_type)}</Etiqueta>
            <Etiqueta color="peligro">{MOTIVOS[r.reason] ?? r.reason}</Etiqueta>
            <Etiqueta color="gris">
              {ESTADOS.find(([e]) => e === r.status)?.[1] ?? r.status}
            </Etiqueta>
            {r.total_reportes > 1 && <Etiqueta color="mint">{r.total_reportes} reportes</Etiqueta>}
          </div>
          <p className="text-white font-semibold truncate">{r.resumen}</p>
          <p className="text-white/80 text-[14px] my-1">{r.detail || 'Sin detalle'}</p>
          <p className="text-c2-muted text-[12px]">
            {r.reportante ? `Reportado por ${r.reportante}, ` : 'Reportado el '}
            {new Date(r.created_at).toLocaleString('es-EC')}
          </p>
          {r.acciones.length > 0 && (
            <button
              type="button"
              onClick={() => setAbierto((x) => !x)}
              className="mt-2 text-[12px] text-[#C084FC] underline"
              aria-expanded={abierto}
            >
              Historial ({r.acciones.length})
            </button>
          )}
          {abierto && (
            <ol className="mt-2 border-l border-c2-linea pl-3 space-y-1" data-testid="historial">
              {r.acciones.map((h, i) => (
                <li key={i} className="text-[12px] text-c2-muted">
                  <span className="text-white">{h.moderador || 'Moderación'}</span>{' '}
                  {ACCIONES[h.accion] ?? h.accion}
                  {h.nota ? `: ${h.nota}` : ''} ({new Date(h.fecha).toLocaleString('es-EC')})
                </li>
              ))}
            </ol>
          )}
        </div>
        <div className="lg:w-64 flex flex-col gap-2">
          <input
            value={nota}
            onChange={(e) => setNota(e.target.value)}
            placeholder="Nota para el historial y el reportante"
            aria-label="Nota de moderación"
            className="h-9 bg-c2-s2 px-3 text-[13px] text-white placeholder:text-c2-muted"
          />
          <div className="flex flex-wrap gap-2">
            {abiertoCaso ? (
              <>
                {r.status === 'pendiente' && (
                  <Boton tipo="secundario" onClick={() => hacer('en_revision')}>
                    Revisar
                  </Boton>
                )}
                {ocultar && (
                  <Boton
                    tipo="peligro"
                    onClick={() =>
                      hacer(r.target_type === 'usuario' ? 'suspender_usuario' : 'ocultar_contenido')
                    }
                  >
                    {ocultar}
                  </Boton>
                )}
                <Boton tipo="secundario" onClick={() => hacer('descartado')}>
                  Descartar
                </Boton>
              </>
            ) : (
              <Boton tipo="secundario" onClick={() => hacer('reabierto')}>
                Reabrir
              </Boton>
            )}
            {nota.trim() && (
              <Boton tipo="secundario" onClick={() => hacer('nota')}>
                Solo nota
              </Boton>
            )}
          </div>
          {error && (
            <p role="alert" className="text-[12px] text-c2-peligro">
              {error}
            </p>
          )}
        </div>
      </div>
    </li>
  )
}

function Moderacion({ f }: { f: FuenteAdmin }) {
  const { datos, error, recargar } = useCarga<ReporteAdmin[]>(() => f.reportes(), [f])
  const [filtro, setFiltro] = useState<EstadoReporte>('pendiente')
  const [tipo, setTipo] = useState<TipoReporte | null>(null)
  const lista = (datos ?? []).filter(
    (r) => r.status === filtro && (!tipo || r.target_type === tipo)
  )
  // Tipos conocidos mas los que lleguen de otros contextos (por ejemplo incidentes).
  const tipos = [...new Set([...tiposRegistrados(), ...(datos ?? []).map((r) => r.target_type)])]
  const cuenta = (s: EstadoReporte) => (datos ?? []).filter((r) => r.status === s).length
  return (
    <>
      <Titulo>Moderación</Titulo>
      <ErrorCarga mensaje={error} />
      <div className="flex flex-wrap gap-2 mb-3" role="tablist" aria-label="Estado">
        {ESTADOS.map(([s, t]) => (
          <button
            key={s}
            role="tab"
            aria-selected={filtro === s}
            onClick={() => setFiltro(s)}
            className={`-skew-x-12 px-3 h-8 text-[12px] font-black italic uppercase ${filtro === s ? 'bg-[#C084FC] text-[#0C1322]' : 'bg-c2-s2 text-white'}`}
          >
            {t} {cuenta(s)}
          </button>
        ))}
      </div>
      <div className="flex flex-wrap gap-2 mb-4" role="group" aria-label="Tipo de contenido">
        {[null, ...tipos].map((t) => (
          <button
            key={t ?? 'todos'}
            aria-pressed={tipo === t}
            onClick={() => setTipo(t)}
            className={`px-3 h-7 text-[12px] font-semibold border ${tipo === t ? 'border-[#C084FC] text-[#C084FC]' : 'border-c2-linea text-c2-muted'}`}
          >
            {t ? etiqueta(t) : 'Todo'}
          </button>
        ))}
      </div>
      {lista.length === 0 && <p className="text-c2-muted">No hay reportes en esta lista.</p>}
      <ul className="space-y-3">
        {lista.map((r) => (
          <TarjetaReporte key={r.id} r={r} f={f} recargar={recargar} />
        ))}
      </ul>
    </>
  )
}

function Eventos({ f }: { f: FuenteAdmin }) {
  const { datos, error, recargar } = useCarga<EventoAdmin[]>(() => f.eventos(), [f])
  const [editando, setEditando] = useState<Partial<EventoAdmin> | null>(null)
  const campo = (k: keyof EventoAdmin, etiqueta: string, tipo = 'text') => (
    <label className="block mt-3 text-[11px] uppercase tracking-widest text-c2-muted">
      {etiqueta}
      <input
        aria-label={etiqueta}
        type={tipo}
        value={String(editando?.[k] ?? '')}
        onChange={(e) =>
          setEditando((x) => ({
            ...x,
            [k]: tipo === 'number' ? Number(e.target.value) : e.target.value,
          }))
        }
        className="mt-1 block w-full h-10 bg-c2-s2 border border-c2-linea text-white px-2 normal-case tracking-normal text-sm"
      />
    </label>
  )
  return (
    <>
      <Titulo extra={<Boton onClick={() => setEditando({ published: false })}>Nuevo evento</Boton>}>
        Eventos
      </Titulo>
      <ErrorCarga mensaje={error} />
      <ul className="space-y-3">
        {(datos ?? []).map((e) => (
          <li key={e.id} className={`${tarjeta} p-4 flex flex-wrap items-center gap-3`}>
            <div className="flex-1 min-w-[200px]">
              <p className="text-white font-semibold">{e.title}</p>
              <p className="text-c2-muted text-[12px]">
                {e.starts_at ? new Date(e.starts_at).toLocaleString('es-EC') : 'Sin fecha'} ·{' '}
                {e.location} · {e.distance_km} km
              </p>
            </div>
            {e.published ? (
              <Etiqueta color="mint">Publicado</Etiqueta>
            ) : (
              <Etiqueta color="gris">Borrador</Etiqueta>
            )}
            <Boton tipo="secundario" onClick={() => setEditando(e)}>
              Editar
            </Boton>
            <Boton
              tipo={e.published ? 'secundario' : 'primario'}
              onClick={() => void f.guardarEvento({ ...e, published: !e.published }).then(recargar)}
            >
              {e.published ? 'Despublicar' : 'Publicar'}
            </Boton>
          </li>
        ))}
      </ul>
      <ModalC2
        abierto={editando != null}
        icono="event"
        titulo={editando?.id ? 'Editar evento' : 'Nuevo evento'}
        texto="Los eventos publicados aparecen en la app para todos los corredores."
        primario="Guardar"
        secundario="Cancelar"
        onCerrar={() => setEditando(null)}
        onPrimario={() => {
          if (!editando?.title?.trim()) return
          void f.guardarEvento({ ...editando, title: editando.title.trim() }).then(() => {
            setEditando(null)
            recargar()
          })
        }}
      >
        {campo('title', 'Título')}
        {campo('description', 'Descripción')}
        {campo('location', 'Lugar')}
        {campo('starts_at', 'Fecha y hora', 'datetime-local')}
        {campo('distance_km', 'Distancia en km', 'number')}
      </ModalC2>
    </>
  )
}

function ListaSimple<T extends { id: string }>({
  titulo,
  carga,
  borrar,
  fila,
}: {
  titulo: string
  carga: { datos: T[] | null; error: string | null; recargar: () => void }
  borrar: (id: string) => Promise<void>
  fila: (x: T) => ReactNode
}) {
  const [confirmar, setConfirmar] = useState<string | null>(null)
  return (
    <>
      <Titulo>{titulo}</Titulo>
      <ErrorCarga mensaje={carga.error} />
      <ul className="space-y-2">
        {(carga.datos ?? []).map((x) => (
          <li key={x.id} className={`${tarjeta} p-4 flex items-center gap-3`}>
            <div className="flex-1">{fila(x)}</div>
            <Boton tipo="secundario" aria={`Eliminar ${x.id}`} onClick={() => setConfirmar(x.id)}>
              Eliminar
            </Boton>
          </li>
        ))}
      </ul>
      <ModalC2
        abierto={confirmar != null}
        icono="delete"
        titulo="¿Eliminar?"
        texto="Se elimina para todos los usuarios. No se puede deshacer."
        primario="Eliminar"
        peligro
        secundario="Cancelar"
        onCerrar={() => setConfirmar(null)}
        onPrimario={() =>
          void borrar(confirmar!).then(() => {
            setConfirmar(null)
            carga.recargar()
          })
        }
      />
    </>
  )
}

function Rutas({ f }: { f: FuenteAdmin }) {
  const c = useCarga<RutaAdmin[]>(() => f.rutas(), [f])
  return (
    <ListaSimple
      titulo="Rutas"
      carga={c}
      borrar={(id) => f.borrarRuta(id)}
      fila={(r) => (
        <>
          <p className="text-white font-semibold">{r.name}</p>
          <p className="text-c2-muted text-[12px]">
            {r.distance_km} km · {r.difficulty}
          </p>
        </>
      )}
    />
  )
}

function Clubes({ f }: { f: FuenteAdmin }) {
  const c = useCarga<ClubAdmin[]>(() => f.clubes(), [f])
  return (
    <ListaSimple
      titulo="Clubes"
      carga={c}
      borrar={(id) => f.borrarClub(id)}
      fila={(x) => (
        <>
          <p className="text-white font-semibold">{x.name}</p>
          <p className="text-c2-muted text-[12px]">{x.description}</p>
        </>
      )}
    />
  )
}

function Push() {
  return (
    <>
      <Titulo>Notificaciones</Titulo>
      <section className={`${tarjeta} p-6 max-w-2xl`} data-testid="push-no-disponible">
        <Etiqueta color="gris">No disponible</Etiqueta>
        <h2 className="font-display italic uppercase text-white text-[20px] mt-3">
          Envío de notificaciones push
        </h2>
        <p className="text-c2-muted mt-2">
          Gacelas todavía no tiene un servicio de envío push (FCM o APNs) conectado al servidor.
          Cuando exista, desde aquí se podrán redactar avisos y elegir el público. Mientras tanto
          las notificaciones de la app son locales: recordatorios de eventos y estado de la carrera.
        </p>
      </section>
    </>
  )
}

function Versiones({ f }: { f: FuenteAdmin }) {
  const { datos, recargar } = useCarga<AjusteApp[]>(() => f.ajustes(), [f])
  const min = (datos ?? []).find((a) => a.key === 'version_minima_android')?.value as
    string | undefined
  const [v, setV] = useState('')
  return (
    <>
      <Titulo>Versiones</Titulo>
      <section className={`${tarjeta} p-4 max-w-2xl space-y-3`}>
        <p className="text-white">
          Versión mínima de Android:{' '}
          <strong className="font-display italic">{min ?? 'sin definir'}</strong>
        </p>
        <p className="text-c2-muted text-sm">
          Los APK se publican con scripts/publicar-android.sh en el bucket app-releases
          (android/latest.json). Las apps por debajo de la versión mínima piden actualizar.
        </p>
        <div className="flex gap-2">
          <input
            aria-label="Nueva versión mínima"
            value={v}
            onChange={(e) => setV(e.target.value)}
            placeholder="1.2.0"
            className="h-10 px-3 bg-c2-s2 border border-c2-linea text-white"
          />
          <Boton
            onClick={() => {
              if (/^\d+\.\d+\.\d+$/.test(v))
                void f.guardarAjuste('version_minima_android', v).then(recargar)
            }}
          >
            Guardar
          </Boton>
        </div>
      </section>
    </>
  )
}

function Ajustes({ f }: { f: FuenteAdmin }) {
  const aj = useCarga<AjusteApp[]>(() => f.ajustes(), [f])
  const ter = useCarga<string | null>(() => f.versionTerminos(), [f])
  const us = useCarga(() => f.usuarios(), [f])
  const [nueva, setNueva] = useState('')
  const [confirmar, setConfirmar] = useState(false)
  const equipo = (us.datos ?? []).filter((u) => u.role)
  return (
    <>
      <Titulo>Ajustes y roles</Titulo>
      <div className="grid lg:grid-cols-2 gap-4">
        <section className={`${tarjeta} p-4`}>
          <h2 className="font-display italic uppercase text-white text-[18px] mb-3">
            Documentos legales
          </h2>
          <p className="text-white" data-testid="version-terminos">
            Versión vigente de los Términos:{' '}
            <strong className="font-display italic">{ter.datos ?? '...'}</strong>
          </p>
          <p className="text-c2-muted text-sm mt-1">
            Versión incluida en esta build: {VERSIONES_LEGALES.terminos}. Al publicar una versión
            nueva, las apps vuelven a pedir la aceptación.
          </p>
          <div className="flex gap-2 mt-3">
            <input
              aria-label="Nueva versión de los Términos"
              value={nueva}
              onChange={(e) => setNueva(e.target.value)}
              placeholder="AAAA-MM-DD"
              className="h-10 px-3 bg-c2-s2 border border-c2-linea text-white"
            />
            <Boton onClick={() => nueva.trim() && setConfirmar(true)}>Publicar</Boton>
          </div>
        </section>
        <section className={`${tarjeta} p-4`}>
          <h2 className="font-display italic uppercase text-white text-[18px] mb-3">
            Ajustes de la app
          </h2>
          <ul className="space-y-2">
            {(aj.datos ?? [])
              .filter((a) => typeof a.value === 'boolean')
              .map((a) => (
                <li key={a.key} className="flex items-center justify-between">
                  <span className="text-white text-sm">{a.key.replace(/_/g, ' ')}</span>
                  <button
                    role="switch"
                    aria-checked={a.value === true}
                    aria-label={a.key}
                    onClick={() => void f.guardarAjuste(a.key, !a.value).then(aj.recargar)}
                    className={`w-12 h-7 rounded-full p-1 transition-colors ${a.value ? 'bg-c2-morado' : 'bg-c2-s2'}`}
                  >
                    <span
                      className={`block w-5 h-5 rounded-full bg-white transition-transform ${a.value ? 'translate-x-5' : ''}`}
                    />
                  </button>
                </li>
              ))}
          </ul>
        </section>
        <section className={`${tarjeta} p-4 lg:col-span-2`}>
          <h2 className="font-display italic uppercase text-white text-[18px] mb-3">Equipo</h2>
          <TablaUsuarios usuarios={equipo} />
          <p className="text-c2-muted text-sm mt-2">
            Asigna roles desde Usuarios, abriendo el detalle de cada persona.
          </p>
        </section>
      </div>
      <ModalC2
        abierto={confirmar}
        icono="gavel"
        titulo="Publicar nueva versión de los Términos"
        texto={`Todos los usuarios deberán aceptar la versión ${nueva} al abrir la app. Actualiza antes los textos en la app.`}
        primario="Publicar"
        secundario="Cancelar"
        onCerrar={() => setConfirmar(false)}
        onPrimario={() =>
          void f.publicarTerminos(nueva.trim()).then(() => {
            setConfirmar(false)
            ter.recargar()
          })
        }
      />
    </>
  )
}

function LoginAdmin() {
  const iniciar = useApp((s) => s.iniciarSesion)
  const [email, setEmail] = useState('')
  const [pass, setPass] = useState('')
  const [error, setError] = useState<string | null>(null)
  return (
    <div className="min-h-screen bg-[#0C1322] flex items-center justify-center p-4">
      <form
        onSubmit={(e) => {
          e.preventDefault()
          void iniciar(email, pass).then((r) => {
            if (!r.ok) setError(r.error)
          })
        }}
        className={`${tarjeta} p-6 w-full max-w-sm space-y-4`}
      >
        <p className="font-display italic uppercase text-white text-[26px]">
          Gacelas <Etiqueta color="violeta">Admin</Etiqueta>
        </p>
        <input
          aria-label="Correo"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="Correo"
          className="w-full h-11 px-3 bg-c2-s2 border border-c2-linea text-white"
        />
        <input
          aria-label="Contraseña"
          type="password"
          value={pass}
          onChange={(e) => setPass(e.target.value)}
          placeholder="Contraseña"
          className="w-full h-11 px-3 bg-c2-s2 border border-c2-linea text-white"
        />
        {error && <p className="text-sm text-[#f472b6]">{error}</p>}
        <button className="w-full h-11 -skew-x-12 bg-gradient-to-r from-[#7B1FA2] to-c2-morado text-white font-black italic uppercase">
          Entrar al panel
        </button>
        <p className="text-c2-muted text-[12px]">Solo cuentas con rol de administrador.</p>
      </form>
    </div>
  )
}

/** Panel /admin: solo para quien tenga rol admin en user_roles (verificado en el servidor con RLS). */
export function Admin() {
  const sesion = useApp((s) => s.sesion)
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const demo = import.meta.env.DEV && params.has('demo')
  const f = useMemo(() => (demo ? fuenteDemo() : fuenteSupabase()), [demo])
  const [permitido, setPermitido] = useState<boolean | null>(null)
  useEffect(() => {
    if (!f || (!sesion && !f.demo)) return
    void f.esAdmin().then(setPermitido)
  }, [f, sesion])

  if (!f?.demo && !sesion) return <LoginAdmin />
  if (!f || permitido === false)
    return (
      <div className="min-h-screen bg-[#0C1322] flex items-center justify-center p-4">
        <div className={`${tarjeta} p-6 max-w-md text-center`} data-testid="admin-sin-acceso">
          <span className="material-symbols-outlined text-[48px] text-c2-peligro">lock</span>
          <h1 className="font-display italic uppercase text-white text-[24px]">
            Acceso restringido
          </h1>
          <p className="text-c2-muted mt-2">
            {f
              ? 'Tu cuenta no tiene rol de administrador.'
              : 'El panel necesita conexión con el servidor.'}
          </p>
          <div className="mt-4 flex justify-center">
            <Boton tipo="secundario" onClick={() => navigate('/')}>
              Volver a la app
            </Boton>
          </div>
        </div>
      </div>
    )
  if (permitido === null) return <div className="min-h-screen bg-[#0C1322]" />
  const sufijo = demo ? '?demo' : ''
  return (
    <div className="min-h-screen bg-[#0C1322] lg:flex">
      <nav
        aria-label="Secciones del panel"
        className="lg:w-[220px] lg:min-h-screen lg:border-r border-b lg:border-b-0 border-c2-linea p-3 lg:p-4 flex lg:flex-col gap-1 overflow-x-auto no-scrollbar"
      >
        <p className="hidden lg:block font-display italic uppercase text-white text-[22px] mb-4">
          Gacelas <Etiqueta color="violeta">Admin</Etiqueta>
        </p>
        {SECCIONES.map(([ruta, t, icono]) => (
          <NavLink
            key={ruta}
            end
            to={`/admin/${ruta}${sufijo}`}
            className={({ isActive }) =>
              `shrink-0 flex items-center gap-2 px-3 h-10 text-[13px] font-black italic uppercase ${isActive ? 'bg-[#C084FC] text-[#0C1322]' : 'text-c2-muted hover:text-white'}`
            }
          >
            <span className="material-symbols-outlined text-[18px]">{icono}</span>
            {t}
          </NavLink>
        ))}
        {f.demo && (
          <p className="hidden lg:block mt-auto text-[11px] text-c2-muted">
            Datos de ejemplo (solo desarrollo)
          </p>
        )}
      </nav>
      <main className="flex-1 p-4 lg:p-8 max-w-6xl">
        <Routes>
          <Route index element={<Dashboard f={f} />} />
          <Route path="usuarios" element={<Usuarios f={f} />} />
          <Route path="estadisticas" element={<Estadisticas f={f} />} />
          <Route path="moderacion" element={<Moderacion f={f} />} />
          <Route path="eventos" element={<Eventos f={f} />} />
          <Route path="rutas" element={<Rutas f={f} />} />
          <Route path="clubes" element={<Clubes f={f} />} />
          <Route path="push" element={<Push />} />
          <Route path="versiones" element={<Versiones f={f} />} />
          <Route path="ajustes" element={<Ajustes f={f} />} />
          <Route path="*" element={<Navigate to={`/admin${sufijo}`} replace />} />
        </Routes>
      </main>
    </div>
  )
}
