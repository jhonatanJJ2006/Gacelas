import { useEffect, useMemo, useRef, useState } from 'react'
import {
  copiarImagen as copiarImagenPortapapeles,
  compartirNativo,
  type DatosCompartir,
} from '../lib/compartir'
import {
  COLOCACION_INICIAL,
  dibujarComposicion,
  exportarComposicion,
  FORMATOS_FOTO,
  generarSuperposicion,
  HANDLE_RUNCITY,
  lienzoPlantilla,
  normalizarHandle,
  PLANTILLAS,
  rectColocacion,
  type Colocacion,
  type FormatoFoto,
  type Plantilla,
} from '../lib/plantillas'

const chip = (activo: boolean) =>
  `min-h-[40px] px-3 rounded-full text-sm font-semibold whitespace-nowrap ${activo ? 'bg-primary-600 text-on-primary' : 'bg-surface-container text-text-secondary'}`

/** Compositor estilo Strava: tu foto + plantilla transparente que se arrastra y escala. */
export function ComponerFoto({ datos }: { datos: DatosCompartir }) {
  const [foto, setFoto] = useState<HTMLImageElement | null>(null)
  const [plantilla, setPlantilla] = useState<Plantilla>('traza')
  const [formato, setFormato] = useState<FormatoFoto>('historia')
  const [handle, setHandle] = useState(datos.handle ?? HANDLE_RUNCITY)
  const [col, setCol] = useState<Colocacion>(COLOCACION_INICIAL.traza)
  const [aviso, setAviso] = useState<string | null>(null)
  const lienzo = useRef<HTMLCanvasElement>(null)
  const arrastre = useRef<{ x: number; y: number; c: Colocacion } | null>(null)
  const h = normalizarHandle(handle)
  const capa = useMemo(
    () => lienzoPlantilla(datos, plantilla, h, formato),
    [datos, plantilla, h, formato]
  )
  const { ancho: W, alto: H } = FORMATOS_FOTO[formato]

  useEffect(() => {
    const cv = lienzo.current
    if (!cv) return
    dibujarComposicion(cv.getContext('2d')!, W, H, foto, capa, plantilla, col, formato)
  }, [W, H, foto, capa, plantilla, col])

  const elegirPlantilla = (p: Plantilla) => {
    setPlantilla(p)
    setCol(COLOCACION_INICIAL[p])
  }
  const cargarFoto = (f?: File) => {
    if (!f) return
    const img = new Image()
    img.onload = () => setFoto(img)
    img.src = URL.createObjectURL(f)
  }
  const aCoord = (e: React.PointerEvent) => {
    const r = lienzo.current!.getBoundingClientRect()
    return { x: (e.clientX - r.left) / r.width, y: (e.clientY - r.top) / r.height }
  }
  const bajar = (e: React.PointerEvent<HTMLCanvasElement>) => {
    const p = aCoord(e)
    const r = rectColocacion(plantilla, col, 1, 1, formato)
    if (p.x < r.x || p.x > r.x + r.ancho || p.y < r.y || p.y > r.y + r.alto) return
    e.currentTarget.setPointerCapture(e.pointerId)
    arrastre.current = { ...p, c: col }
  }
  const mover = (e: React.PointerEvent) => {
    const a = arrastre.current
    if (!a) return
    const p = aCoord(e)
    setCol({
      ...a.c,
      cx: Math.min(1, Math.max(0, a.c.cx + p.x - a.x)),
      cy: Math.min(1, Math.max(0, a.c.cy + p.y - a.y)),
    })
  }
  const descargar = (b: Blob, nombre: string) => {
    const u = URL.createObjectURL(b)
    const a = document.createElement('a')
    a.href = u
    a.download = nombre
    a.click()
    setTimeout(() => URL.revokeObjectURL(u), 1000)
  }
  const exportar = () => exportarComposicion(foto, capa, plantilla, col, formato)
  const compartir = async () => {
    const b = await exportar()
    if (!(await compartirNativo(datos, b))) {
      descargar(b, `gacelas-${formato}.png`)
      setAviso('Tu navegador no comparte imágenes: la descargamos.')
    }
  }
  const copiarImagen = async () => {
    const copiada = await copiarImagenPortapapeles(await exportar())
    setAviso(copiada ? 'Imagen copiada' : 'No se pudo copiar la imagen en este navegador.')
  }
  const copiarEnlace = async () => {
    try {
      if (!navigator.clipboard?.writeText) throw new Error('Portapapeles no disponible')
      await navigator.clipboard.writeText(datos.enlace)
      setAviso('Enlace copiado')
    } catch {
      setAviso('No se pudo copiar el enlace en este navegador.')
    }
  }

  return (
    <div className="space-y-3" data-testid="componer-foto">
      <div className="flex gap-2">
        <label className="flex-1 h-11 rounded-xl bg-surface-container text-text-primary flex items-center justify-center gap-1 cursor-pointer">
          <span className="material-symbols-outlined">add_photo_alternate</span>
          {foto ? 'Cambiar foto' : 'Elegir foto'}
          <input
            type="file"
            accept="image/*"
            className="sr-only"
            data-testid="foto-input"
            onChange={(e) => cargarFoto(e.target.files?.[0])}
          />
        </label>
        <label
          className="h-11 px-4 rounded-xl bg-surface-container text-text-primary flex items-center justify-center cursor-pointer"
          aria-label="Tomar foto"
        >
          <span className="material-symbols-outlined">photo_camera</span>
          <input
            type="file"
            accept="image/*"
            capture="environment"
            className="sr-only"
            onChange={(e) => cargarFoto(e.target.files?.[0])}
          />
        </label>
      </div>
      <div role="radiogroup" aria-label="Plantilla" className="flex gap-2 overflow-x-auto pb-1">
        {PLANTILLAS.map((p) => (
          <button
            key={p.id}
            role="radio"
            aria-checked={plantilla === p.id}
            onClick={() => elegirPlantilla(p.id)}
            className={chip(plantilla === p.id)}
          >
            {p.etiqueta}
          </button>
        ))}
      </div>
      <div role="radiogroup" aria-label="Tamaño" className="flex gap-2">
        {(Object.keys(FORMATOS_FOTO) as FormatoFoto[]).map((f) => (
          <button
            key={f}
            role="radio"
            aria-checked={formato === f}
            onClick={() => setFormato(f)}
            className={chip(formato === f)}
          >
            {FORMATOS_FOTO[f].etiqueta}
          </button>
        ))}
      </div>
      <canvas
        ref={lienzo}
        width={W}
        height={H}
        onPointerDown={bajar}
        onPointerMove={mover}
        onPointerUp={() => (arrastre.current = null)}
        onWheel={(e) =>
          setCol((c) => ({
            ...c,
            escala: Math.min(1.6, Math.max(0.15, c.escala - e.deltaY * 0.001)),
          }))
        }
        aria-label="Vista previa: arrastra los datos para moverlos"
        data-testid="lienzo-componer"
        className="mx-auto block rounded-2xl touch-none bg-[repeating-conic-gradient(#1f2937_0_25%,#111827_0_50%)] bg-[length:24px_24px]"
        style={{ aspectRatio: `${W}/${H}`, maxHeight: '48vh', maxWidth: '100%' }}
      />
      <label className="flex items-center gap-3 text-sm text-text-secondary">
        Tamaño
        <input
          type="range"
          min={15}
          max={160}
          value={Math.round(col.escala * 100)}
          onChange={(e) => setCol({ ...col, escala: Number(e.target.value) / 100 })}
          className="flex-1"
          aria-label="Tamaño de los datos"
        />
      </label>
      <input
        aria-label="Usuario a mostrar"
        value={handle}
        maxLength={31}
        onChange={(e) => setHandle(e.target.value)}
        placeholder="@usuario (respaldo @runcity)"
        className="w-full h-11 px-3 bg-surface-container border border-border-subtle rounded-lg text-text-primary"
      />
      <div className="grid grid-cols-2 gap-2">
        <button
          onClick={() => void compartir()}
          className="col-span-2 h-12 rounded-xl bg-primary-600 text-on-primary font-semibold flex items-center justify-center gap-2"
        >
          <span className="material-symbols-outlined">ios_share</span>Compartir
        </button>
        <button
          onClick={() => void exportar().then((b) => descargar(b, `gacelas-${formato}.png`))}
          className="h-12 rounded-xl bg-surface-container text-text-primary"
          data-testid="descargar-composicion"
        >
          Guardar imagen
        </button>
        <button
          onClick={() => void copiarImagen()}
          className="h-12 rounded-xl bg-surface-container text-text-primary"
          data-testid="copiar-imagen"
        >
          Copiar imagen
        </button>
        <button
          onClick={() => void copiarEnlace()}
          className="h-12 rounded-xl bg-surface-container text-text-primary"
        >
          Copiar enlace
        </button>
        <button
          onClick={() =>
            void generarSuperposicion(datos, plantilla, h).then((b) =>
              descargar(b, `runcity-${plantilla}-transparente.png`)
            )
          }
          className="col-span-2 h-12 rounded-xl bg-surface-container text-text-primary"
          data-testid="descargar-transparente"
        >
          PNG transparente
        </button>
      </div>
      {aviso && (
        <p role="status" className="text-sm text-text-secondary text-center">
          {aviso}
        </p>
      )}
    </div>
  )
}
