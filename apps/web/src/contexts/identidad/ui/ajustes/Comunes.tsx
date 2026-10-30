import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { Interruptor } from '../../../../shared/ui/ui'

export function PantallaAjustes({
  titulo,
  subtitulo,
  volverA,
  children,
}: {
  titulo: string
  subtitulo?: string
  volverA: string | number
  children: ReactNode
}) {
  const navigate = useNavigate()
  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      {/* Android EncabezadoAtras: barra 64dp, flecha simple y titulo titleMd. */}
      <header className="h-16 px-1 bg-[#151B2B] flex items-center">
        <div className="flex items-center gap-1 lg:max-w-3xl lg:mx-auto lg:w-full">
          <button
            onClick={() => (typeof volverA === 'number' ? navigate(volverA) : navigate(volverA))}
            aria-label="Volver"
            className="w-12 h-12 rounded-full flex items-center justify-center text-text-primary"
          >
            <span className="material-symbols-outlined">arrow_back</span>
          </button>
          <h1 className="font-title-md text-title-md tracking-[0.03em] text-text-primary">
            {titulo}
          </h1>
        </div>
      </header>
      <main className="flex-1 px-margin py-4 space-y-3 pb-24 lg:max-w-3xl lg:mx-auto lg:w-full lg:py-10">
        {subtitulo && <p className="text-body-sm text-text-secondary">{subtitulo}</p>}
        {children}
      </main>
    </div>
  )
}

export function Seccion({
  titulo,
  etiqueta,
  children,
}: {
  titulo: string
  etiqueta?: string
  children: ReactNode
}) {
  return (
    <section>
      {/* Android EtiquetaSeccion + Tarjeta(padding = 0): filas juntas en una tarjeta C2. */}
      <div className="flex items-center justify-between pt-3 pb-1">
        <h2 className="text-[12px] font-extrabold uppercase tracking-[0.12em] text-c2-violeta">
          {titulo}
        </h2>
        {etiqueta && <span className="text-body-sm text-text-secondary">{etiqueta}</span>}
      </div>
      <div className="tarjeta-c2">{children}</div>
    </section>
  )
}

export function FilaInterruptor({
  icono,
  color = 'text-text-secondary',
  titulo,
  descripcion,
  activo,
  onCambiar,
}: {
  icono: string
  color?: string
  titulo: string
  descripcion?: string
  activo: boolean
  onCambiar: (v: boolean) => void
}) {
  return (
    <div className="flex items-center justify-between gap-3 px-4 py-3.5">
      <div className="flex items-center gap-3">
        <span className={`material-symbols-outlined text-[20px] ${color}`}>{icono}</span>
        <div>
          <div className="font-body-md text-body-md text-text-primary font-semibold">{titulo}</div>
          {descripcion && (
            <p className="font-body-sm text-body-sm text-text-secondary">{descripcion}</p>
          )}
        </div>
      </div>
      <Interruptor etiqueta={titulo} activo={activo} onCambiar={onCambiar} />
    </div>
  )
}

/** Grupo C2 (figma-proto): etiqueta violeta y tarjeta con filas separadas por lineas. */
export function GrupoC2({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <section aria-label={titulo}>
      <h2 className="mb-2 text-[12px] font-extrabold uppercase tracking-[0.14em] text-c2-violeta">
        {titulo}
      </h2>
      <div className="bg-c2-card divide-y divide-c2-linea">{children}</div>
    </section>
  )
}

/** Fila C2 con interruptor, sin icono. */
export function FilaC2({
  titulo,
  descripcion,
  activo,
  onCambiar,
}: {
  titulo: string
  descripcion?: string
  activo: boolean
  onCambiar: (v: boolean) => void
}) {
  return (
    <div className="flex items-center justify-between gap-3 px-4 py-3.5">
      <div>
        <div className="text-[15px] font-bold text-white">{titulo}</div>
        {descripcion && <p className="text-[13px] text-c2-muted">{descripcion}</p>}
      </div>
      <Interruptor etiqueta={titulo} activo={activo} onCambiar={onCambiar} />
    </div>
  )
}
