import { useEffect, useState } from 'react'
import {
  compartirNativo,
  enlaceWhatsApp,
  generarImagen,
  TAMANOS,
  type DatosCompartir,
  type FormatoCompartir,
} from '../lib/compartir'
import { ComponerFoto } from './ComponerFoto'

const FORMATOS: { id: FormatoCompartir; etiqueta: string }[] = [
  { id: 'historia', etiqueta: 'Historia' },
  { id: 'post', etiqueta: 'Post' },
  { id: 'sticker', etiqueta: 'Sticker' },
]

/**
 * Hoja para compartir una ruta o carrera: imagen de marca (historia 1080x1920, post 1080x1080
 * o sticker transparente), Web Share con archivos en movil y descarga + enlace + WhatsApp.
 */
export function CompartirDialogo({
  datos,
  onCerrar,
  alGenerarPost,
}: {
  datos: DatosCompartir
  onCerrar: () => void
  /** Se llama con la imagen de post (p. ej. para usarla como vista previa del enlace). */
  alGenerarPost?: (b: Blob) => void
}) {
  const [modo, setModo] = useState<'foto' | 'marca'>('foto')
  const [formato, setFormato] = useState<FormatoCompartir>('historia')
  const [imagen, setImagen] = useState<Blob | null>(null)
  const [url, setUrl] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)

  useEffect(() => {
    let vivo = true
    setImagen(null)
    void generarImagen(datos, formato).then((b) => {
      if (!vivo) return
      setImagen(b)
      if (formato === 'post') alGenerarPost?.(b)
    })
    return () => {
      vivo = false
    }
  }, [datos, formato, alGenerarPost])

  useEffect(() => {
    if (!imagen) return setUrl(null)
    const u = URL.createObjectURL(imagen)
    setUrl(u)
    return () => URL.revokeObjectURL(u)
  }, [imagen])

  useEffect(() => {
    const esc = (e: KeyboardEvent) => e.key === 'Escape' && onCerrar()
    window.addEventListener('keydown', esc)
    return () => window.removeEventListener('keydown', esc)
  }, [onCerrar])

  const compartir = async () => {
    if (!imagen) return
    if (!(await compartirNativo(datos, imagen)))
      setAviso('Tu navegador no comparte imágenes: descárgala o usa el enlace.')
  }
  const copiar = async () => {
    await navigator.clipboard?.writeText(datos.enlace).catch(() => undefined)
    setAviso('Enlace copiado')
  }
  const { ancho, alto } = TAMANOS[formato]

  return (
    <div
      className="fixed inset-0 z-50 bg-black/70 flex items-end sm:items-center justify-center"
      onClick={onCerrar}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="titulo-compartir"
        className="w-full sm:max-w-md bg-surface rounded-t-3xl sm:rounded-3xl p-5 space-y-4 max-h-[95vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between">
          <h2 id="titulo-compartir" className="font-title-md text-title-md text-text-primary">
            Compartir
          </h2>
          <button
            onClick={onCerrar}
            aria-label="Cerrar"
            className="w-11 h-11 rounded-full bg-surface-container text-text-primary flex items-center justify-center"
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>
        <div
          role="tablist"
          className="grid grid-cols-2 gap-1 p-1 rounded-full bg-surface-container"
        >
          {(
            [
              ['foto', 'Sobre tu foto'],
              ['marca', 'Imagen Gacelas'],
            ] as const
          ).map(([m, e]) => (
            <button
              key={m}
              role="tab"
              aria-selected={modo === m}
              onClick={() => setModo(m)}
              className={`min-h-[40px] rounded-full text-sm font-semibold ${modo === m ? 'bg-surface text-text-primary' : 'text-text-secondary'}`}
            >
              {e}
            </button>
          ))}
        </div>
        {modo === 'foto' ? (
          <ComponerFoto datos={datos} />
        ) : (
          <>
            <div role="radiogroup" aria-label="Formato" className="flex gap-2">
              {FORMATOS.map((f) => (
                <button
                  key={f.id}
                  role="radio"
                  aria-checked={formato === f.id}
                  onClick={() => setFormato(f.id)}
                  className={`flex-1 min-h-[40px] rounded-full text-sm font-semibold ${formato === f.id ? 'bg-primary-600 text-on-primary' : 'bg-surface-container text-text-secondary'}`}
                >
                  {f.etiqueta}
                </button>
              ))}
            </div>
            <div
              className="mx-auto rounded-2xl overflow-hidden bg-[repeating-conic-gradient(#1f2937_0_25%,#111827_0_50%)] bg-[length:24px_24px]"
              style={{ aspectRatio: `${ancho}/${alto}`, maxHeight: '52vh' }}
            >
              {url ? (
                <img
                  src={url}
                  alt={`Vista previa (${ancho}×${alto})`}
                  className="w-full h-full object-contain"
                  data-testid="imagen-compartir"
                />
              ) : (
                <div className="w-full h-full flex items-center justify-center text-text-secondary">
                  Generando…
                </div>
              )}
            </div>
            <div className="grid grid-cols-2 gap-2">
              <button
                onClick={() => void compartir()}
                disabled={!imagen}
                className="col-span-2 h-12 rounded-xl bg-primary-600 text-on-primary font-semibold flex items-center justify-center gap-2 disabled:opacity-60"
              >
                <span className="material-symbols-outlined">ios_share</span>Compartir imagen
              </button>
              <a
                href={url ?? undefined}
                download={`gacelas-${formato}.png`}
                aria-disabled={!url}
                className="h-12 rounded-xl bg-surface-container text-text-primary flex items-center justify-center gap-1"
              >
                <span className="material-symbols-outlined">download</span>Descargar
              </a>
              <button
                onClick={() => void copiar()}
                className="h-12 rounded-xl bg-surface-container text-text-primary flex items-center justify-center gap-1"
              >
                <span className="material-symbols-outlined">link</span>Copiar enlace
              </button>
              <a
                href={enlaceWhatsApp(datos)}
                target="_blank"
                rel="noreferrer"
                className="col-span-2 h-12 rounded-xl bg-[#128C4B] text-white font-semibold flex items-center justify-center gap-2"
              >
                <span className="material-symbols-outlined">chat</span>Enviar por WhatsApp
              </a>
            </div>
          </>
        )}
        {aviso && (
          <p role="status" className="text-sm text-text-secondary text-center">
            {aviso}
          </p>
        )}
      </div>
    </div>
  )
}
