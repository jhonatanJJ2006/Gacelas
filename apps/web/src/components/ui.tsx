import { useEffect, type ReactNode } from 'react'

export function MensajeError({ mensaje }: { mensaje: string | null }) {
  if (!mensaje) return null
  return (
    <p
      role="alert"
      className="flex items-center gap-2 text-sm text-danger bg-danger/10 border border-danger/30 rounded-lg px-3 py-2"
    >
      <span className="material-symbols-outlined text-[18px]">error</span>
      {mensaje}
    </p>
  )
}

interface DialogoProps {
  titulo: string
  abierto: boolean
  onCerrar: () => void
  children: ReactNode
  acciones?: ReactNode
}

/** Dialogo modal accesible (Escape o clic fuera para cerrar). */
export function Dialogo({ titulo, abierto, onCerrar, children, acciones }: DialogoProps) {
  useEffect(() => {
    if (!abierto) return
    const alTeclear = (e: KeyboardEvent) => e.key === 'Escape' && onCerrar()
    window.addEventListener('keydown', alTeclear)
    return () => window.removeEventListener('keydown', alTeclear)
  }, [abierto, onCerrar])
  if (!abierto) return null
  return (
    <div
      className="fixed inset-0 z-[100] flex items-end sm:items-center justify-center bg-black/60 p-4"
      onClick={onCerrar}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={titulo}
        className="w-full max-w-md bg-bg-secondary border border-border-subtle rounded-2xl p-6 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="font-title-md text-title-md text-text-primary mb-3">{titulo}</h2>
        <div className="text-text-secondary text-sm space-y-3">{children}</div>
        {acciones && <div className="flex justify-end gap-2 mt-6">{acciones}</div>}
      </div>
    </div>
  )
}

export function BotonTexto({
  children,
  onClick,
  peligro = false,
  type = 'button',
  disabled,
}: {
  children: ReactNode
  onClick?: () => void
  peligro?: boolean
  type?: 'button' | 'submit'
  disabled?: boolean
}) {
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={`px-4 h-10 rounded-lg font-label-lg text-label-lg transition-colors disabled:opacity-50 ${peligro ? 'text-danger hover:bg-danger/10' : 'text-primary-500 hover:bg-primary-600/10'}`}
    >
      {children}
    </button>
  )
}

/** Interruptor accesible (role switch). */
export function Interruptor({
  activo,
  onCambiar,
  etiqueta,
}: {
  activo: boolean
  onCambiar: (v: boolean) => void
  etiqueta: string
}) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={activo}
      aria-label={etiqueta}
      onClick={() => onCambiar(!activo)}
      className="relative w-14 h-11 -my-2 flex items-center justify-center shrink-0"
    >
      <span
        aria-hidden="true"
        className={`relative w-12 h-7 rounded-full transition-colors ${activo ? 'bg-primary-600' : 'bg-surface-container-highest'}`}
      >
        <span
          className={`absolute top-1 left-1 w-5 h-5 rounded-full bg-white shadow transition-transform ${activo ? 'translate-x-5' : ''}`}
        />
      </span>
    </button>
  )
}

/** Mensaje breve en la parte inferior de la pantalla. */
export function Aviso({ mensaje }: { mensaje: string | null }) {
  if (!mensaje) return null
  return (
    <div
      role="status"
      className="fixed bottom-28 inset-x-0 z-[90] flex justify-center pointer-events-none"
    >
      <span className="bg-bg-secondary border border-border-subtle text-text-primary text-sm px-4 py-2 rounded-full shadow-xl">
        {mensaje}
      </span>
    </div>
  )
}
