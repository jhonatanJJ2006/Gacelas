import { OrbitControls, useAnimations, useGLTF } from '@react-three/drei'
import { Canvas } from '@react-three/fiber'
import {
  Component,
  Suspense,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import * as THREE from 'three'
import { clone as cloneSkinned } from 'three/examples/jsm/utils/SkeletonUtils.js'
import { isWebGLAvailable } from '../lib/webgl'

interface Runner3DProps {
  animation?: 'Idle' | 'Jog' | 'Run'
  className?: string
  autoRotate?: boolean
  cameraPosition?: [number, number, number]
  /** Punto al que mira la camara (por defecto el origen). */
  target?: [number, number, number]
  /** Giro del modelo en grados (sentido horario visto desde arriba). */
  giroY?: number
  /** Falso quita los controles de orbita (corredor sobre el mapa). */
  interactivo?: boolean
}

/** Imagen estatica del corredor: se ve mientras carga el 3D o si WebGL/el modelo fallan. */
export const RUNNER_POSTER = '/models/gacelas-runner-poster.png'

function RunnerModel({
  animation = 'Idle',
  onReady,
  giroY = 0,
}: {
  animation: string
  onReady?: () => void
  giroY?: number
}) {
  const group = useRef<THREE.Group>(null)
  const { scene, animations } = useGLTF('/models/gacelas-runner-animated.glb')
  // SkeletonUtils.clone mantiene los huesos enlazados (scene.clone() rompe el skinning)
  const modelo = useMemo(() => cloneSkinned(scene), [scene])
  const { actions } = useAnimations(animations, group)

  useEffect(() => {
    onReady?.()
  }, [onReady])

  useEffect(() => {
    if (actions[animation]) {
      actions[animation]?.reset().fadeIn(0.5).play()
      return () => {
        actions[animation]?.fadeOut(0.5)
      }
    }
  }, [actions, animation])

  return (
    <primitive
      ref={group}
      object={modelo}
      scale={1.5}
      position={[0, 0, 0]}
      rotation={[0, (-giroY * Math.PI) / 180, 0]}
    />
  )
}

export function Runner3DFallback({ className = '' }: { className?: string }) {
  const [poster, setPoster] = useState(true)
  return (
    <div
      data-testid="runner3d-fallback"
      role="img"
      aria-label="Corredor de Runcity"
      className={`w-full h-full flex items-center justify-center ${className}`}
    >
      {poster ? (
        <img
          src={RUNNER_POSTER}
          alt=""
          aria-hidden="true"
          className="w-full h-full object-contain"
          onError={() => setPoster(false)}
        />
      ) : (
        <svg
          viewBox="0 0 64 64"
          className="w-24 h-24 text-amber-500"
          fill="currentColor"
          aria-hidden="true"
        >
          <circle cx="38" cy="10" r="6" />
          <path d="M30 20l10 2 6 10 8 2-1 4-10-2-4-5-3 9 7 6-2 14h-5l1-11-8-6-5 9-10 1v-4l7-1 8-16-4-1-5 6-3-2 7-9z" />
        </svg>
      )}
    </div>
  )
}

class Runner3DErrorBoundary extends Component<
  { fallback: ReactNode; children: ReactNode },
  { hasError: boolean }
> {
  state = { hasError: false }

  static getDerivedStateFromError() {
    return { hasError: true }
  }

  componentDidCatch(error: unknown) {
    console.warn('Runner3D: no se pudo renderizar el modelo 3D', error)
  }

  render() {
    return this.state.hasError ? this.props.fallback : this.props.children
  }
}

export function Runner3D({
  animation = 'Idle',
  className = '',
  autoRotate = false,
  cameraPosition = [0, 1.2, 3],
  target,
  giroY = 0,
  interactivo = true,
}: Runner3DProps) {
  const [webgl] = useState(isWebGLAvailable)
  const [menosMovimiento] = useState(
    () =>
      typeof window !== 'undefined' &&
      typeof window.matchMedia === 'function' &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches
  )
  const [listo, setListo] = useState(false)
  const marcarListo = useCallback(() => setListo(true), [])
  const fallback = <Runner3DFallback className={className} />
  if (!webgl) return fallback
  return (
    <Runner3DErrorBoundary fallback={fallback}>
      <div className={`relative w-full h-full ${className}`}>
        {!listo && (
          <div className="absolute inset-0 pointer-events-none">
            <Runner3DFallback />
          </div>
        )}
        <Canvas camera={{ position: cameraPosition, fov: 50 }} shadows>
          <Suspense fallback={null}>
            <ambientLight intensity={0.5} />
            <directionalLight position={[5, 5, 5]} intensity={1} castShadow />
            <RunnerModel animation={animation} onReady={marcarListo} giroY={giroY} />
            {interactivo && (
              <OrbitControls
                enableZoom={false}
                enablePan={false}
                target={target}
                autoRotate={autoRotate && !menosMovimiento}
                autoRotateSpeed={2}
                maxPolarAngle={Math.PI / 2}
                minPolarAngle={Math.PI / 4}
              />
            )}
          </Suspense>
        </Canvas>
      </div>
    </Runner3DErrorBoundary>
  )
}

useGLTF.preload('/models/gacelas-runner-animated.glb')
