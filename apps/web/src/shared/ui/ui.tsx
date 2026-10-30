import { createPortal } from 'react-dom'
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
  /** Icono Material del bloque junto al titulo (modales especificos C2). */
  icono?: string
  peligro?: boolean
}

/** Dialogo modal accesible (Escape o clic fuera para cerrar). */
export function Dialogo({
  titulo,
  abierto,
  onCerrar,
  children,
  acciones,
  icono,
  peligro,
}: DialogoProps) {
  useEffect(() => {
    if (!abierto) return
    const alTeclear = (e: KeyboardEvent) => e.key === 'Escape' && onCerrar()
    window.addEventListener('keydown', alTeclear)
    return () => window.removeEventListener('keydown', alTeclear)
  }, [abierto, onCerrar])
  if (!abierto) return null
  return createPortal(
    <div
      className="fixed inset-0 z-[100] flex items-end sm:items-center justify-center bg-black/70 sm:p-4 pt-[max(env(safe-area-inset-top),16px)]"
      onClick={onCerrar}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={titulo}
        className="w-full max-w-md bg-c2-card border border-c2-linea border-t-2 border-t-c2-violeta rounded-t-[14px] sm:rounded-[14px] px-6 pt-3 pb-[max(env(safe-area-inset-bottom),24px)] shadow-2xl flex flex-col max-h-[calc(100dvh-16px)] sm:max-h-[calc(100dvh-32px)]"
        onClick={(e) => e.stopPropagation()}
      >
        <div
          className="mx-auto mb-4 h-1.5 w-12 shrink-0 rounded-full bg-c2-linea"
          aria-hidden="true"
        />
        <div className="flex items-center gap-3 mb-3 shrink-0">
          {icono && (
            <span
              aria-hidden="true"
              className={`w-12 h-12 shrink-0 bg-c2-s2 [clip-path:polygon(0_0,100%_0,100%_75%,75%_100%,0_100%)] flex items-center justify-center material-symbols-outlined ${peligro ? 'text-c2-peligro' : 'text-c2-violeta'}`}
            >
              {icono}
            </span>
          )}
          <h2 className="font-display italic text-[24px] leading-tight text-text-primary">
            {titulo}
          </h2>
        </div>
        <div
          data-testid="dialogo-contenido"
          className="text-c2-muted text-sm space-y-3 min-h-0 flex-1 overflow-y-auto overscroll-contain"
        >
          {children}
        </div>
        {acciones && (
          <div className="flex flex-wrap justify-end gap-2 mt-6 shrink-0">{acciones}</div>
        )}
      </div>
    </div>,
    document.body
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
        className={`relative block w-[52px] h-8 rounded-full border-2 transition-colors ${activo ? 'bg-primary-600 border-primary-600' : 'bg-surface-container-highest border-border-subtle'}`}
      >
        {/* Switch Material 3 como Android: pulgar grande blanco activo, pequeno gris inactivo. */}
        <span
          className={`absolute top-1/2 -translate-y-1/2 rounded-full transition-all ${activo ? 'left-[22px] w-6 h-6 bg-white' : 'left-1.5 w-4 h-4 bg-text-secondary/60'}`}
        />
      </span>
    </button>
  )
}

/** Aviso C2 (toast): banner superior con icono; los mensajes de fallo se pintan como error. */
export function Aviso({ mensaje }: { mensaje: string | null }) {
  if (!mensaje) return null
  const error = /error|no se pudo|no pudimos|fall|sin conexión|inválid|permiso/i.test(mensaje)
  return (
    <div
      role="status"
      className="fixed top-3 inset-x-3 z-[120] flex justify-center pointer-events-none"
    >
      <div
        data-testid={error ? 'toast-error' : 'toast-exito'}
        className={`w-full max-w-md flex items-center gap-3 bg-c2-card border px-4 py-3 shadow-2xl ${error ? 'border-c2-peligro/60' : 'border-c2-menta/60'}`}
      >
        <span
          aria-hidden="true"
          className={`material-symbols-outlined text-[20px] ${error ? 'text-c2-peligro' : 'text-c2-menta'}`}
        >
          {error ? 'error' : 'check_circle'}
        </span>
        <span className="text-sm font-semibold text-white">{mensaje}</span>
      </div>
    </div>
  )
}
