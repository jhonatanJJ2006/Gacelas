import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { IconoGacelas } from '../../../../shared/ui/LogoGacelas'

/*
 * Piezas de las pantallas de acceso C4, traduccion 1:1 de Android
 * (contextos/identidad/presentation/.../acceso/AccesoVisual.kt) para que web y app se vean igual.
 */

/** Etiqueta del chip de la cabecera (Android PortadaRuta.etiqueta(): trazo "1" del Parque Jipiro). */
export const ETIQUETA_PORTADA = 'Jipiro · 1.8 KM'

/** Fondo de mapa exportado de Stitch (mismo archivo que Android, drawable-nodpi). */
export function FondoMapaC4({ login }: { login: boolean }) {
  return (
    <div
      role="img"
      aria-label="Mapa de Loja con la ruta del Parque Jipiro"
      data-testid="mapa-portada"
      className="pointer-events-none absolute inset-0 bg-cover bg-top"
      style={{
        backgroundImage: `url('/images/${login ? 'mapa-login-c4' : 'mapa-bienvenida'}.webp')`,
      }}
    />
  )
}

/** Marca de la cabecera: icono 36 + RUNCITY / LOJA RUNNING CO. */
export function MarcaC4() {
  return (
    <div className="flex items-center gap-2" data-testid="logo-gacelas">
      <IconoGacelas tam={36} />
      <div className="flex flex-col">
        <span className="font-display text-[18px] leading-6 tracking-[0.015em] text-white">
          RUNCITY
        </span>
        <span className="-mt-1 whitespace-nowrap text-[9px] leading-3 font-semibold tracking-[0.14em] text-[#A8B0C2]">
          LOJA RUNNING CO.
        </span>
      </div>
    </div>
  )
}

const labelCaps = 'text-[11px] leading-[14px] tracking-[0.12em] font-extrabold uppercase'

export function ChipC4({ texto }: { texto: string }) {
  return (
    <div className="flex items-center gap-1.5 rounded-full border border-white/10 bg-[#232A3A]/75 px-3 py-1.5 shadow-[0_2px_5px_rgba(0,0,0,0.3)]">
      <span className="h-1.5 w-1.5 rounded-full bg-[#C084FC] shadow-[0_0_4px_#C084FC]" />
      <span className="whitespace-nowrap text-[11px] leading-[14px] tracking-[0.06em] font-bold text-[#DFE2EE]">
        {texto}
      </span>
    </div>
  )
}

export function IndicadorZonaC4() {
  return (
    <div className="inline-flex items-center gap-2 rounded-full border border-white/5 bg-[#0C1322]/70 px-2.5 py-1">
      <span className="h-1.5 w-1.5 rounded-full bg-[#C084FC]" />
      <span className="text-[10px] leading-[14px] tracking-[0.1em] text-[#A8B0C2]">
        AV. ORILLAS DEL ZAMORA
      </span>
    </div>
  )
}

/** Tarjeta "aero-glass" r28 con hairline superior. */
export function TarjetaC4({
  children,
  className = '',
  alpha = 0.95,
}: {
  children: ReactNode
  className?: string
  alpha?: number
}) {
  return (
    <div
      className={`relative w-full overflow-hidden rounded-[28px] border border-white/10 p-6 shadow-[0_16px_40px_rgba(0,0,0,0.65)] ${className}`}
      style={{ background: `rgba(25, 31, 47, ${alpha})` }}
    >
      <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-white/20 to-transparent" />
      {children}
    </div>
  )
}

/** Boton principal C4: pildora morada, Anton cursiva y flecha. */
export function BotonC4({
  texto,
  onClick,
  alto = 52,
  tamTexto = 18,
  disabled = false,
  type = 'button',
}: {
  texto: string
  onClick?: () => void
  alto?: number
  tamTexto?: number
  disabled?: boolean
  type?: 'button' | 'submit'
}) {
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      data-testid="boton-principal"
      className="flex w-full items-center justify-center gap-2 rounded-full bg-[#7B1FA2] text-white shadow-[0_4px_8px_rgba(123,31,162,0.38)] transition-transform active:scale-[0.98] disabled:bg-[#7B1FA2]/60"
      style={{ height: alto }}
    >
      <span className="font-display" style={{ fontSize: tamTexto, lineHeight: '24px' }}>
        {texto}
      </span>
      <span className="material-symbols-outlined text-[20px]" aria-hidden="true">
        arrow_forward
      </span>
    </button>
  )
}

export function PieC4({ texto, enlace, a }: { texto: string; enlace: string; a: string }) {
  return (
    <div className="pt-4">
      <div className="h-px w-full bg-white/5" />
      <p className="pt-3 text-center text-[13px] leading-[18px] text-[#A8B0C2]">
        {texto}
        <Link to={a} className="ml-1 font-semibold text-[#C084FC] underline">
          {enlace}
        </Link>
      </p>
    </div>
  )
}

export function SeparadorC4() {
  return (
    <div className="flex items-center py-4" aria-hidden="true">
      <span className="h-px flex-1 bg-white/10" />
      <span className="px-3 text-[12px] text-[#A8B0C2]/60">o</span>
      <span className="h-px flex-1 bg-white/10" />
    </div>
  )
}

export function AtrasC4({ onClick }: { onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label="Atrás"
      className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-white/10 bg-[#232A3A]/80 text-white"
    >
      <span className="material-symbols-outlined text-[19px]" aria-hidden="true">
        arrow_back
      </span>
    </button>
  )
}

/** Clases del campo C4: caja h48 r12, borde blanco 10%, morado con foco, icono a la izquierda. */
export const claseCampoC4 =
  'h-12 w-full rounded-xl border border-white/10 bg-[#232A3A]/90 pl-10 pr-3 text-[13px] text-white placeholder:text-[#A8B0C2]/50 focus:border-[#7B1FA2] focus:outline-none'

export const claseEtiquetaC4 = `${labelCaps} text-[#A8B0C2]`

export function IconoCampoC4({ icono }: { icono: string }) {
  return (
    <span
      aria-hidden="true"
      className="material-symbols-outlined pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-[18px] text-[#A8B0C2]/70"
    >
      {icono}
    </span>
  )
}
