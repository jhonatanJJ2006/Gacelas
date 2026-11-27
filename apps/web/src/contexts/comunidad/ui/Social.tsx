import { useEffect, useMemo, useRef, useState } from 'react'
import { ModalC2 } from '../../../shared/ui/modales'
import { Progreso } from '../../../shared/ui/Visual'
import { useActividad } from '../../actividad/api'
import {
  CATEGORIAS_LOGRO,
  desbloquearNuevos,
  estadoLogros,
  LOGROS,
  logrosDesbloqueados,
  logrosUnificados,
  textoAvance,
  type DefinicionLogro,
  type EstadoLogro,
} from '../../logros/api'
import {
  definicionTipo,
  reportarContenido,
  type MotivoReporte,
  type TipoObjetivo,
} from '../../moderacion/api'

/** Modal C2 para reportar cualquier tipo registrado en Moderacion (publicacion, evento, grupo...). */
export function ModalReporte({
  postId,
  tipo = 'post',
  onCerrar,
  onEnviado,
}: {
  /** Id del contenido a reportar (null cierra el modal). */
  postId: string | null
  tipo?: TipoObjetivo
  onCerrar: () => void
  onEnviado: (ok: boolean) => void
}) {
  const { motivos, titulo } = definicionTipo(tipo)
  const [motivo, setMotivo] = useState<MotivoReporte>(motivos[0][0])
  const [detalle, setDetalle] = useState('')
  return (
    <ModalC2
      abierto={postId != null}
      icono="flag"
      titulo={titulo}
      texto="Cuéntanos qué pasa. El equipo de moderación lo revisará y te avisará cuando lo resuelva; la persona no sabrá quién la reportó."
      primario="Enviar reporte"
      peligro
      secundario="Cancelar"
      onCerrar={onCerrar}
      onPrimario={() => {
        const id = postId
        if (!id) return
        void reportarContenido(tipo, id, motivo, detalle).then((ok) => {
          setDetalle('')
          onEnviado(ok)
        })
      }}
    >
      <fieldset className="mt-3 space-y-1" aria-label="Motivo del reporte">
        {motivos.map(([v, etiqueta]) => (
          <label
            key={v}
            className={`flex items-center gap-3 px-3 h-11 border cursor-pointer ${motivo === v ? 'border-c2-violeta bg-c2-s2' : 'border-c2-linea'}`}
          >
            <input
              type="radio"
              name="motivo"
              value={v}
              checked={motivo === v}
              onChange={() => setMotivo(v)}
              className="accent-c2-violeta"
            />
            <span className="text-text-primary text-sm">{etiqueta}</span>
          </label>
        ))}
      </fieldset>
      <textarea
        aria-label="Detalle del reporte"
        value={detalle}
        onChange={(e) => setDetalle(e.target.value)}
        maxLength={500}
        rows={2}
        placeholder="Detalle opcional"
        className="mt-3 w-full px-3 py-2 bg-c2-s2 border border-c2-linea text-text-primary resize-none"
      />
    </ModalC2>
  )
}

/** Vigila las carreras guardadas y muestra el modal C2 cuando se desbloquea un logro. */
export function AnfitrionLogros() {
  const carreras = useActividad((s) => s.carreras)
  const [cola, setCola] = useState<DefinicionLogro[]>(() =>
    // Solo en desarrollo: ?demo-logro muestra el modal para revisarlo.
    import.meta.env.DEV && location.search.includes('demo-logro') ? [LOGROS[1]] : []
  )
  // Al abrir la app se registran en silencio los logros ya ganados; el modal sale solo
  // cuando una carrera nueva desbloquea uno durante la sesion. Lo mismo la primera vez que el
  // dispositivo pasa al catalogo unico (antes habia medallas aparte).
  const previas = useRef<number | null>(null)
  useEffect(() => {
    if (carreras.length === 0) return
    const yaUnificado = logrosUnificados()
    const silencioso =
      previas.current === null || carreras.length <= previas.current || !yaUnificado
    previas.current = carreras.length
    const nuevos = desbloquearNuevos(carreras)
    if (nuevos.length && !silencioso) setCola((c) => [...c, ...nuevos])
  }, [carreras])
  const actual = cola[0]
  return (
    <ModalC2
      abierto={actual != null}
      icono={actual?.icono ?? 'military_tech'}
      titulo={`Logro desbloqueado: ${actual?.titulo ?? ''}`}
      texto={actual?.descripcion ?? ''}
      primario="Genial"
      onPrimario={() => setCola((c) => c.slice(1))}
      onCerrar={() => setCola((c) => c.slice(1))}
    />
  )
}

/** Estado de los logros del usuario: ganados (registrados o cumplidos por sus carreras) y avance del resto. */
export function useEstadoLogros(): EstadoLogro[] {
  const carreras = useActividad((s) => s.carreras)
  const [hechos, setHechos] = useState(logrosDesbloqueados)
  useEffect(() => setHechos(logrosDesbloqueados()), [carreras])
  return useMemo(() => estadoLogros(carreras, hechos, Date.now()), [carreras, hechos])
}

function CeldaLogro({ e }: { e: EstadoLogro }) {
  const { logro: l, progreso, lograda, fecha } = e
  return (
    <li
      data-testid={`logro-${l.codigo}`}
      data-lograda={lograda}
      title={l.comoConseguirlo}
      className={`p-3 border min-w-0 ${lograda ? 'border-c2-violeta bg-c2-s2' : 'border-c2-linea opacity-60'}`}
    >
      <span
        aria-hidden="true"
        className={`material-symbols-outlined text-[28px] ${lograda ? 'text-c2-violeta' : 'text-c2-muted'}`}
      >
        {lograda ? l.icono : 'lock'}
      </span>
      <p className="text-text-primary text-sm font-bold leading-tight">{l.titulo}</p>
      {lograda ? (
        <p className="text-c2-muted text-[12px]">
          {fecha ? `Desbloqueado el ${new Date(fecha).toLocaleDateString('es-EC')}` : 'Conseguido'}
        </p>
      ) : (
        <>
          <p className="text-c2-muted text-[12px]">{l.descripcion}</p>
          <div className="mt-1.5">
            <Progreso pct={(progreso / l.meta) * 100} alto={4} etiqueta={l.titulo} />
          </div>
          <p className="text-c2-muted text-[11px] mt-1">{textoAvance(l, progreso)}</p>
        </>
      )}
    </li>
  )
}

/**
 * Logros en Perfil, un solo apartado (antes eran Medallas y Logros): ganados en color, el resto
 * con su avance. "resumen" muestra los 6 mas avanzados; "completo" todos por categoria.
 */
export function ListaLogros({ modo = 'completo' }: { modo?: 'resumen' | 'completo' }) {
  const estados = useEstadoLogros()
  const ganados = estados.filter((e) => e.lograda).length
  if (modo === 'resumen') {
    const primeros = [...estados]
      .sort(
        (a, b) =>
          Number(b.lograda) - Number(a.lograda) ||
          b.progreso / b.logro.meta - a.progreso / a.logro.meta
      )
      .slice(0, 6)
    return (
      <ul className="grid grid-cols-2 gap-2" data-testid="logros-resumen">
        {primeros.map((e) => (
          <CeldaLogro key={e.logro.codigo} e={e} />
        ))}
      </ul>
    )
  }
  return (
    <section aria-label="Logros" className="tarjeta-c2 p-4" data-testid="logros">
      <div className="flex items-baseline justify-between mb-3">
        <h2 className="font-display italic uppercase text-text-primary text-[18px]">Logros</h2>
        <span className="text-c2-muted text-[12px]" data-testid="logros-conteo">
          {ganados} de {estados.length} conseguidos
        </span>
      </div>
      {CATEGORIAS_LOGRO.map((cat) => (
        <div key={cat.id} className="mb-4 last:mb-0">
          <h3 className="rotulo-c2 mb-2">{cat.titulo}</h3>
          <ul className="grid grid-cols-2 lg:grid-cols-4 gap-2">
            {estados
              .filter((e) => e.logro.categoria === cat.id)
              .map((e) => (
                <CeldaLogro key={e.logro.codigo} e={e} />
              ))}
          </ul>
        </div>
      ))}
    </section>
  )
}

/** Modal C2 cuando el dispositivo pierde la conexion. */
export function ModalSinConexion() {
  const [offline, setOffline] = useState(
    () => typeof navigator !== 'undefined' && !navigator.onLine
  )
  const [cerrado, setCerrado] = useState(false)
  useEffect(() => {
    const off = () => {
      setOffline(true)
      setCerrado(false)
    }
    const on = () => setOffline(false)
    window.addEventListener('offline', off)
    window.addEventListener('online', on)
    return () => {
      window.removeEventListener('offline', off)
      window.removeEventListener('online', on)
    }
  }, [])
  return (
    <ModalC2
      abierto={offline && !cerrado}
      icono="wifi_off"
      titulo="Sin conexión"
      texto="Puedes seguir grabando carreras; se guardan en el dispositivo y se sincronizan cuando vuelva la conexión. El feed, el mapa en vivo y los eventos no se actualizarán mientras tanto."
      primario="Entendido"
      onPrimario={() => setCerrado(true)}
      onCerrar={() => setCerrado(true)}
    />
  )
}
