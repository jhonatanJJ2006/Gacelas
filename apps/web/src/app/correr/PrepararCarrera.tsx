import { Correr } from '../../contexts/actividad/ui/Correr'
import { useEsEscritorio } from '../../shared/lib/useEsEscritorio'
import { PanelClima, PanelRutaSegura, PanelSeguridad } from '../inicio/PanelesInicio'

/**
 * Preparar carrera. En escritorio: ruta segura del dia, clima para correr y seguridad de la zona
 * junto al panel de GPS y el boton de empezar. En movil es la pantalla de Correr de siempre.
 */
export function PrepararCarrera() {
  const escritorio = useEsEscritorio()
  if (!escritorio) return <Correr />
  return (
    <div className="w-full px-8 max-w-[1440px] mx-auto pt-6 pb-10">
      <div className="grid grid-cols-[minmax(0,1fr)_400px] gap-5 items-start">
        <div className="space-y-5 min-w-0">
          <PanelRutaSegura />
          <div className="grid gap-5">
            <PanelClima />
            <PanelSeguridad />
          </div>
        </div>
        <section
          aria-label="Empezar carrera"
          className="panel-c2 bg-c2-card border border-c2-linea p-4 sticky top-[88px]"
        >
          <Correr incrustado />
        </section>
      </div>
    </div>
  )
}
