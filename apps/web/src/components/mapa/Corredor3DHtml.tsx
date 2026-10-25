import { MarcadorHtml } from './MapaBase'
import { Runner3D } from '../Runner3D'
import { usePosicionSuave } from './usePosicionSuave'
import type { Coordenada } from '../../lib/mapas'
import { animacionPorRitmo } from '../../lib/mapaEstilo'

const CLIPS = { idle: 'Idle', jog: 'Jog', run: 'Run' } as const

/**
 * Corredor 3D (mismo GLB) dibujado sobre el mapa raster en la posicion del usuario, con los pies en
 * el punto y girado segun el rumbo. Se usa cuando no hay mapa vectorial con Map ID propio.
 */
export function Corredor3DHtml({
  pos,
  rumbo,
  ritmoSegKm,
  lado = 150,
}: {
  pos: Coordenada
  rumbo: number | null
  ritmoSegKm: number | null
  lado?: number
}) {
  const suave = usePosicionSuave(pos)
  if (!suave) return null
  const { clip } = animacionPorRitmo(ritmoSegKm)
  return (
    <MarcadorHtml pos={suave} z={60}>
      <div
        className="pointer-events-none relative"
        style={{ width: lado, height: lado, transform: `translateY(${lado * 0.2}px)` }}
        data-testid="corredor-3d-html"
      >
        <span className="absolute left-1/2 bottom-[12%] -translate-x-1/2 w-14 h-5 rounded-[50%] bg-c2-violeta/40 blur-[3px]" />
        <Runner3D
          animation={CLIPS[clip]}
          cameraPosition={[0, 3.2, 3.6]}
          target={[0, 2.1, 0]}
          giroY={rumbo ?? 0}
          interactivo={false}
        />
      </div>
    </MarcadorHtml>
  )
}
