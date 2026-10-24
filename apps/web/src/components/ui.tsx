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
      className="fixed inset-0 z-[100] flex items-end sm:items-center justify-center bg-black/70 sm:p-4"
      onClick={onCerrar}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={titulo}
        className="w-full max-w-md bg-c2-card border border-c2-linea border-t-2 border-t-c2-violeta rounded-t-[14px] sm:rounded-[14px] px-6 pt-3 pb-6 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mx-auto mb-4 h-1.5 w-12 rounded-full bg-c2-linea" aria-hidden="true" />
        <h2 className="font-display italic text-[24px] leading-tight text-text-primary mb-3">
          {titulo}
        </h2>
        <div className="text-c2-muted text-sm space-y-3">{children}</div>
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
      className={`px-5 h-11 -skew-x-12 font-black italic uppercase text-[14px] transition-colors disabled:opacity-50 ${peligro ? 'bg-c2-peligro text-white hover:brightness-110' : 'bg-c2-s2 text-c2-violeta hover:bg-c2-linea'}`}
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
