import { lazy, Suspense } from 'react'
import { useEsEscritorio } from '../../../../shared/lib/useEsEscritorio'
import { Runner3DFallback } from '../../../../shared/ui/Runner3D'

const Runner3D = lazy(() =>
  import('../../../../shared/ui/Runner3D').then((m) => ({ default: m.Runner3D }))
)

/** Corredor 3D de la columna izquierda en las pantallas de acceso (solo escritorio). */
export function PanelCorredorAcceso() {
  const escritorio = useEsEscritorio()
  if (!escritorio) return null
  return (
    <div
      data-testid="panel-corredor-acceso"
      className="relative mt-8 h-[min(46vh,380px)] aspect-[4/5] rounded-2xl overflow-hidden border border-border-subtle/80 bg-surface-container-low/60 shadow-lg"
    >
      <div className="absolute inset-x-0 bottom-0 h-1/3 bg-gradient-to-t from-primary-600/20 to-transparent" />
      <Suspense fallback={<Runner3DFallback />}>
        <Runner3D animation="Jog" cameraPosition={[0, 1.35, 3.9]} target={[0, 1.2, 0]} />
      </Suspense>
    </div>
  )
}
