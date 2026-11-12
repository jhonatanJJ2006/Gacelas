import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'

/** Vuelve a la pantalla anterior; sin historial dentro de la app, va a Inicio. */
export function useVolver(respaldo = '/inicio') {
  const navigate = useNavigate()
  return () => {
    const idx = (window.history.state as { idx?: number } | null)?.idx ?? 0
    if (idx > 0) navigate(-1)
    else navigate(respaldo)
  }
}

/** Flecha de retroceso para pantallas de mapa: arriba a la izquierda, respeta la safe area. */
export function BotonVolverMapa({
  onVolver,
  className = '',
}: {
  onVolver?: () => void
  className?: string
}) {
  const volver = useVolver()
  return (
    <button
      type="button"
      onClick={onVolver ?? volver}
      aria-label="Volver"
      data-testid="volver-mapa"
      className={`w-[42px] h-[42px] shrink-0 rounded-full bg-c2-card/95 border border-c2-linea text-white flex items-center justify-center shadow-lg backdrop-blur-md ${className}`}
    >
      <span className="material-symbols-outlined text-[22px]">arrow_back</span>
    </button>
  )
}

/**
 * Boton lateral de reportar incidente: color de alerta C2 e icono de advertencia, pensado para ir
 * en el borde derecho del mapa, nunca en el centro.
 */
export function BotonReportarMapa({
  onClick,
  className = '',
}: {
  onClick: () => void
  className?: string
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label="Reportar incidente"
      title="Reportar incidente"
      data-testid="reportar-incidente"
      className={`w-[42px] h-[42px] rounded-[6px] bg-c2-peligro border border-[#F472B6]/60 text-white flex items-center justify-center shadow-lg ${className}`}
    >
      <span className="material-symbols-outlined text-[22px]">warning</span>
    </button>
  )
}

/** Control del mapa con icono y texto corto debajo; el texto completo va en title y aria-label. */
export function ControlEtiquetado({
  icono,
  etiqueta,
  descripcion,
  onClick,
  presionado,
  testId,
}: {
  icono: string
  etiqueta: string
  descripcion: string
  onClick: () => void
  presionado?: boolean
  testId?: string
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={descripcion}
      title={descripcion}
      aria-pressed={presionado}
      data-testid={testId}
      className={`w-[56px] h-[50px] flex flex-col items-center justify-center gap-0.5 text-white hover:bg-white/10 ${presionado === false ? 'text-c2-muted' : ''}`}
    >
      <span className="material-symbols-outlined text-[20px] leading-none" aria-hidden="true">
        {icono}
      </span>
      <span className="text-[9px] font-bold uppercase tracking-wide leading-none">{etiqueta}</span>
    </button>
  )
}

/** Grupo de controles con su titulo (Mapa, Carrera...), en una tarjeta C2 vertical. */
export function GrupoControles({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <div
      role="group"
      aria-label={titulo}
      className="flex flex-col items-center bg-c2-card/95 border border-c2-linea rounded-[8px] shadow-lg backdrop-blur-md overflow-hidden divide-y divide-c2-linea"
    >
      <span className="w-full text-center text-[8px] font-extrabold uppercase tracking-[0.14em] text-c2-violeta py-1">
        {titulo}
      </span>
      {children}
    </div>
  )
}

/**
 * Selector de vista 2D / 3D: muestra las dos opciones y resalta la que se ve ahora, asi la
 * etiqueta nunca contradice al mapa.
 */
export function SelectorVista({
  vista3d,
  onCambiar,
}: {
  vista3d: boolean
  onCambiar: (tresD: boolean) => void
}) {
  const op = (tresD: boolean) => {
    const activa = vista3d === tresD
    return (
      <button
        type="button"
        onClick={() => onCambiar(tresD)}
        aria-pressed={activa}
        aria-label={tresD ? 'Ver mapa en 3D' : 'Ver mapa en 2D'}
        title={tresD ? 'Vista 3D inclinada' : 'Vista 2D desde arriba'}
        data-testid={tresD ? 'vista-3d-on' : 'vista-2d-on'}
        className={`w-full h-[28px] text-[12px] font-black italic ${activa ? 'bg-c2-violeta text-white' : 'text-c2-muted hover:text-white'}`}
      >
        {tresD ? '3D' : '2D'}
      </button>
    )
  }
  return (
    <div
      className="w-[56px] flex flex-col"
      data-testid="vista-3d"
      data-vista={vista3d ? '3d' : '2d'}
    >
      {op(true)}
      {op(false)}
    </div>
  )
}
