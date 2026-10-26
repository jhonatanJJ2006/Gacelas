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
      <header className="px-margin py-4 border-b border-border-subtle">
        <div className="flex items-center gap-4 lg:max-w-3xl lg:mx-auto lg:w-full">
          <button
            onClick={() => (typeof volverA === 'number' ? navigate(volverA) : navigate(volverA))}
            aria-label="Volver"
            className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors"
          >
            <span className="material-symbols-outlined">arrow_back</span>
          </button>
          <div>
            <h1 className="font-display italic uppercase text-[26px] leading-tight text-text-primary">
              {titulo}
            </h1>
            {subtitulo && <p className="text-body-sm text-text-secondary">{subtitulo}</p>}
          </div>
        </div>
      </header>
      <main className="flex-1 px-margin py-6 space-y-6 pb-24 lg:max-w-3xl lg:mx-auto lg:w-full lg:py-10">
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
      <div className="flex items-baseline justify-between mb-3">
        <h2 className="font-title-md text-title-md text-text-primary">{titulo}</h2>
        {etiqueta && <span className="text-body-sm text-text-secondary">{etiqueta}</span>}
      </div>
      <div className="space-y-2">{children}</div>
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
    <div className="flex items-center justify-between gap-3 px-4 py-3 bg-surface-container rounded-xl">
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
