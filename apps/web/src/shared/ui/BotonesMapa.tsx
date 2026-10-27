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
