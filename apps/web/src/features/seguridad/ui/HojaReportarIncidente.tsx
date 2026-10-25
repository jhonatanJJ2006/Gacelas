import { useEffect, useState } from 'react'
import { obtenerDireccion, reportarIncidente } from '../application/incidentes'
import { MAX_FOTOS, TIPOS_INCIDENTE, type TipoIncidente } from '../domain/incidentes'

export interface Ubicacion {
  lat: number
  lng: number
}

/**
 * Hoja inferior para reportar un incidente. La direccion y la hora se toman solas;
 * el corredor solo elige el tipo. Descripcion y hasta 3 fotos son opcionales.
 */
export function HojaReportarIncidente({
  abierta,
  ubicacion,
  onCerrar,
  onEnviado,
}: {
  abierta: boolean
  ubicacion: Ubicacion | null
  onCerrar: () => void
  onEnviado?: (mensaje: string) => void
}) {
  const [direccion, setDireccion] = useState('Buscando dirección...')
  const [ahora, setAhora] = useState(Date.now())
  const [tipo, setTipo] = useState<TipoIncidente | null>(null)
  const [descripcion, setDescripcion] = useState('')
  const [fotos, setFotos] = useState<File[]>([])
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    if (!abierta) return
    setAhora(Date.now())
    setTipo(null)
    setDescripcion('')
    setFotos([])
    setError(null)
    if (!ubicacion) return setDireccion('Ubicación no disponible')
    setDireccion('Buscando dirección...')
    void obtenerDireccion(ubicacion.lat, ubicacion.lng).then(setDireccion)
  }, [abierta, ubicacion])

  if (!abierta) return null
  const hora = new Date(ahora).toLocaleTimeString('es-EC', { hour: '2-digit', minute: '2-digit' })

  const enviar = async () => {
    if (!tipo || !ubicacion) return
    setEnviando(true)
    setError(null)
    try {
      await reportarIncidente({
        tipo,
        descripcion,
        lat: ubicacion.lat,
        lng: ubicacion.lng,
        direccion,
        ocurridoMs: ahora,
        fotos,
      })
      onEnviado?.('Gracias. Tu reporte queda pendiente de revisión.')
      onCerrar()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo enviar el reporte')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center"
      role="dialog"
      aria-modal="true"
      aria-labelledby="titulo-reportar"
    >
      <button aria-label="Cerrar" className="absolute inset-0 bg-black/60" onClick={onCerrar} />
      <div
        data-testid="hoja-reportar"
        className="relative w-full max-w-lg rounded-t-[14px] bg-c2-card border-t-2 border-c2-violeta px-5 pt-2 pb-[max(env(safe-area-inset-bottom),20px)] space-y-4 max-h-[90vh] overflow-y-auto"
      >
        <div className="mx-auto h-1.5 w-12 rounded-full bg-white/20" />
        <h2
          id="titulo-reportar"
          className="font-display italic uppercase text-[22px] text-text-primary"
        >
          Reportar incidente
        </h2>
        <div className="flex gap-3 text-sm">
          <div className="flex-1 rounded-lg bg-c2-bg p-3 min-w-0">
            <div className="text-[11px] uppercase tracking-wider text-text-secondary">Dónde</div>
            <div data-testid="reporte-direccion" className="text-text-primary truncate">
              {direccion}
            </div>
          </div>
          <div className="rounded-lg bg-c2-bg p-3">
            <div className="text-[11px] uppercase tracking-wider text-text-secondary">Hora</div>
            <div className="text-text-primary tabular-nums">{hora}</div>
          </div>
        </div>
        <fieldset>
          <legend className="text-xs uppercase tracking-wider text-text-secondary mb-2">
            ¿Qué pasó?
          </legend>
          <div className="grid grid-cols-2 gap-2">
            {TIPOS_INCIDENTE.map((t) => (
              <button
                key={t.id}
                type="button"
                aria-pressed={tipo === t.id}
                onClick={() => setTipo(t.id)}
                className={`h-11 px-3 rounded-xl flex items-center gap-2 text-sm text-left border ${
                  tipo === t.id
                    ? 'bg-c2-violeta/20 border-c2-violeta text-white'
                    : 'bg-c2-bg border-c2-linea text-text-primary'
                }`}
              >
                <span className="material-symbols-outlined text-[18px]">{t.icono}</span>
                {t.nombre}
              </button>
            ))}
          </div>
        </fieldset>
        <label className="block">
          <span className="text-xs uppercase tracking-wider text-text-secondary">
            Descripción (opcional)
          </span>
          <textarea
            value={descripcion}
            maxLength={280}
            onChange={(e) => setDescripcion(e.target.value)}
            rows={2}
            placeholder="No incluyas nombres ni datos de otras personas"
            className="mt-1 w-full rounded-xl bg-c2-bg border border-c2-linea p-3 text-text-primary text-sm"
          />
        </label>
        <label className="flex items-center gap-2 text-sm text-text-primary cursor-pointer">
          <span className="material-symbols-outlined">add_a_photo</span>
          Fotos (opcional, máx. {MAX_FOTOS}) {fotos.length > 0 && `· ${fotos.length}`}
          <input
            type="file"
            accept="image/*"
            multiple
            className="sr-only"
            onChange={(e) => setFotos(Array.from(e.target.files ?? []).slice(0, MAX_FOTOS))}
          />
        </label>
        <p className="text-[11px] text-text-secondary">
          Las fotos se suben sin metadatos ni GPS. En el mapa público tu reporte aparece sin tu
          nombre y con la ubicación aproximada.
        </p>
        {error && (
          <p role="alert" className="text-sm text-[#FF8A7A]">
            {error}
          </p>
        )}
        <button
          type="button"
          disabled={!tipo || !ubicacion || enviando}
          onClick={() => void enviar()}
          className="w-full h-12 rounded-xl bg-primary-600 text-on-primary font-semibold disabled:opacity-40"
        >
          {enviando ? 'Enviando...' : 'Enviar reporte'}
        </button>
      </div>
    </div>
  )
}
