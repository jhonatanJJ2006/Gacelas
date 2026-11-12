import { animacionPorRitmo } from '../../lib/mapaEstilo'
import type { Coordenada } from '../../lib/mapas'
import { Runner3D } from '../Runner3D'
import { MarcadorHtml } from './MapaBase'
import { usePosicionSuave } from './usePosicionSuave'

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
  fijo = false,
}: {
  /** En la vista 3D inclinada por CSS el corredor va de pie en el centro, fuera de la capa del mapa. */
  fijo?: boolean
  pos: Coordenada
  rumbo: number | null
  ritmoSegKm: number | null
  lado?: number
}) {
  const suave = usePosicionSuave(pos)
  if (!suave) return null
  const { clip } = animacionPorRitmo(ritmoSegKm)
  if (fijo)
    return (
      <div
        className="pointer-events-none absolute left-1/2 top-1/2 z-[5]"
        style={{ width: lado, height: lado, transform: `translate(-50%, -80%)` }}
        data-testid="corredor-3d-html"
      >
        <span className="absolute left-1/2 bottom-[12%] -translate-x-1/2 w-14 h-5 rounded-[50%] bg-c2-violeta/40 blur-[3px]" />
        <Runner3D
          animation={CLIPS[clip]}
          cameraPosition={[0, 3.2, 3.6]}
          target={[0, 2.1, 0]}
          giroY={0}
          interactivo={false}
        />
      </div>
    )
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
