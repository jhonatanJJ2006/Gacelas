import { useEffect, useRef, useState } from 'react'
import { ModalC2 } from '../../components/modales'
import { useApp } from '../../store/app'
import { LOGROS, desbloquearNuevos, logrosDesbloqueados, type DefinicionLogro } from './logros'
import { MOTIVOS_REPORTE, reportarPublicacion, type MotivoReporte } from './moderacion'

/** Modal C2 para reportar una publicacion. */
export function ModalReporte({
  postId,
  onCerrar,
  onEnviado,
}: {
  postId: string | null
  onCerrar: () => void
  onEnviado: (ok: boolean) => void
}) {
  const [motivo, setMotivo] = useState<MotivoReporte>('spam')
  const [detalle, setDetalle] = useState('')
  return (
    <ModalC2
      abierto={postId != null}
      icono="flag"
      titulo="Reportar publicación"
      texto="Cuéntanos qué pasa. El equipo de moderación lo revisará; la persona no sabrá quién la reportó."
      primario="Enviar reporte"
      peligro
      secundario="Cancelar"
      onCerrar={onCerrar}
      onPrimario={() => {
        const id = postId
        if (!id) return
        void reportarPublicacion(id, motivo, detalle).then((ok) => {
          setDetalle('')
          onEnviado(ok)
        })
      }}
    >
      <fieldset className="mt-3 space-y-1" aria-label="Motivo del reporte">
        {MOTIVOS_REPORTE.map(([v, etiqueta]) => (
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
            <span className="text-white text-sm">{etiqueta}</span>
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
        className="mt-3 w-full px-3 py-2 bg-c2-s2 border border-c2-linea text-white resize-none"
      />
    </ModalC2>
  )
}

/** Vigila las carreras guardadas y muestra el modal C2 cuando se desbloquea un logro. */
export function AnfitrionLogros() {
  const carreras = useApp((s) => s.carreras)
  const [cola, setCola] = useState<DefinicionLogro[]>(() =>
    // Solo en desarrollo: ?demo-logro muestra el modal para revisarlo.
    import.meta.env.DEV && location.search.includes('demo-logro') ? [LOGROS[2]] : []
  )
  // Al abrir la app se registran en silencio los logros ya ganados; el modal sale solo
  // cuando una carrera nueva desbloquea uno durante la sesion.
  const previas = useRef<number | null>(null)
  useEffect(() => {
    if (carreras.length === 0) return
    const silencioso = previas.current === null || carreras.length <= previas.current
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

/** Lista de logros en Perfil: desbloqueados en color, pendientes atenuados. */
export function ListaLogros() {
  const carreras = useApp((s) => s.carreras)
  const [hechos, setHechos] = useState(logrosDesbloqueados)
  useEffect(() => setHechos(logrosDesbloqueados()), [carreras])
  return (
    <section aria-label="Logros" className="bg-c2-card border border-c2-linea p-4">
      <h2 className="font-display italic uppercase text-white text-[18px] mb-3">Logros</h2>
      <ul className="grid grid-cols-2 lg:grid-cols-4 gap-2">
        {LOGROS.map((l) => {
          const fecha = hechos[l.codigo]
          return (
            <li
              key={l.codigo}
              data-testid={`logro-${l.codigo}`}
              className={`p-3 border ${fecha ? 'border-c2-violeta bg-c2-s2' : 'border-c2-linea opacity-50'}`}
            >
              <span
                className={`material-symbols-outlined text-[28px] ${fecha ? 'text-c2-violeta' : 'text-c2-muted'}`}
              >
                {l.icono}
              </span>
              <p className="text-white text-sm font-bold">{l.titulo}</p>
              <p className="text-c2-muted text-[12px]">
                {fecha
                  ? `Desbloqueado el ${new Date(fecha).toLocaleDateString('es-EC')}`
                  : l.descripcion}
              </p>
            </li>
          )
        })}
      </ul>
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
