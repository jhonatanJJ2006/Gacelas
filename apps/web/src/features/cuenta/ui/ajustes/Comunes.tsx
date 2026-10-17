import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { Interruptor } from '../../../../components/ui'

export function PantallaAjustes({
  titulo,
  volverA,
  children,
}: {
  titulo: string
  volverA: string
  children: ReactNode
}) {
  const navigate = useNavigate()
  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <header className="px-margin py-4 border-b border-border-subtle">
        <div className="flex items-center gap-4 lg:max-w-3xl lg:mx-auto lg:w-full">
          <button
            onClick={() => navigate(volverA)}
            aria-label="Volver"
            className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors"
          >
            <span className="material-symbols-outlined">arrow_back</span>
          </button>
          <h1 className="font-headline-md text-headline-md text-text-primary">{titulo}</h1>
        </div>
      </header>
      <main className="flex-1 px-margin py-6 space-y-6 pb-24 lg:max-w-3xl lg:mx-auto lg:w-full lg:py-10">
        {children}
      </main>
    </div>
  )
}

export function Seccion({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <section>
      <h2 className="font-title-md text-title-md text-text-primary mb-3">{titulo}</h2>
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
