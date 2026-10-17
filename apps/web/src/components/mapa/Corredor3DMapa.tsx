import { useEffect, useRef } from 'react'
import { useMap } from '@vis.gl/react-google-maps'
import * as THREE from 'three'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'
import type { Coordenada } from '../../lib/mapas'
import { animacionPorRitmo } from '../../lib/mapaEstilo'

const CLIPS = { idle: 'Idle', jog: 'Jog', run: 'Run' } as const

/**
 * Corredor 3D animado DENTRO del mapa vectorial (WebGLOverlayView + three.js, mismo contexto GL).
 * Se coloca en la posicion del usuario, gira con el rumbo y cambia de animacion segun el ritmo.
 */
export function Corredor3DMapa({
  pos,
  rumbo,
  ritmoSegKm,
  escala = 24,
  onFallo,
}: {
  pos: Coordenada
  rumbo: number | null
  ritmoSegKm: number | null
  escala?: number
  onFallo?: () => void
}) {
  const mapa = useMap()
  const estado = useRef({ pos, rumbo: rumbo ?? 0, ritmo: ritmoSegKm })
  estado.current.pos = pos
  if (rumbo != null) estado.current.rumbo = rumbo
  estado.current.ritmo = ritmoSegKm

  useEffect(() => {
    if (!mapa || !(google.maps as { WebGLOverlayView?: unknown }).WebGLOverlayView) {
      onFallo?.()
      return
    }
    const ov = new google.maps.WebGLOverlayView()
    const escena = new THREE.Scene()
    const camara = new THREE.PerspectiveCamera()
    const grupo = new THREE.Group()
    escena.add(grupo)
    escena.add(new THREE.AmbientLight(0xffffff, 1.6))
    const luz = new THREE.DirectionalLight(0xffffff, 1.4)
    luz.position.set(0.5, -1, 1.2)
    escena.add(luz)
    let renderer: THREE.WebGLRenderer | null = null
    let mezclador: THREE.AnimationMixer | null = null
    let acciones: Record<string, THREE.AnimationAction> = {}
    let actual = ''
    let antes = performance.now()
    let rumboVisto = estado.current.rumbo
    let posVista = { ...estado.current.pos }

    new GLTFLoader().load(
      '/models/gacelas-runner-animated.glb',
      (gltf) => {
        const modelo = gltf.scene
        modelo.rotation.x = Math.PI / 2 // Y arriba (three) -> Z arriba (mapa)
        grupo.add(modelo)
        // La matriz del mapa no es una camara normal: sin esto three recorta la malla con huesos.
        modelo.traverse((o) => (o.frustumCulled = false))
        mezclador = new THREE.AnimationMixer(modelo)
        acciones = Object.fromEntries(
          gltf.animations.map((a) => [a.name, mezclador!.clipAction(a)])
        )
        ov.requestRedraw()
      },
      undefined,
      () => onFallo?.()
    )

    ov.onContextRestored = ({ gl }) => {
      renderer = new THREE.WebGLRenderer({
        canvas: gl.canvas,
        context: gl,
        ...gl.getContextAttributes(),
      })
      renderer.autoClear = false
      renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.5))
    }
    ov.onDraw = ({ transformer }) => {
      if (!renderer) return
      const e = estado.current
      // Suavizado de posicion y giro entre lecturas.
      posVista = {
        lat: posVista.lat + (e.pos.lat - posVista.lat) * 0.12,
        lng: posVista.lng + (e.pos.lng - posVista.lng) * 0.12,
      }
      const d = ((((e.rumbo - rumboVisto) % 360) + 540) % 360) - 180
      rumboVisto += d * 0.12
      grupo.rotation.z = Math.PI - (rumboVisto * Math.PI) / 180
      // Tamano constante en pantalla (~120 px) aunque cambie el zoom.
      grupo.scale.setScalar(escala * Math.pow(2, 18 - (mapa.getZoom() ?? 18)))
      const anim = animacionPorRitmo(e.ritmo)
      const nombre = CLIPS[anim.clip]
      if (nombre !== actual && acciones[nombre]) {
        acciones[actual]?.fadeOut(0.4)
        acciones[nombre].reset().fadeIn(0.4).play()
        actual = nombre
      }
      if (mezclador) mezclador.timeScale = anim.velocidad
      const ahora = performance.now()
      mezclador?.update(Math.min((ahora - antes) / 1000, 0.1))
      antes = ahora
      const m = transformer.fromLatLngAltitude({
        lat: posVista.lat,
        lng: posVista.lng,
        altitude: 0,
      })
      camara.projectionMatrix = new THREE.Matrix4().fromArray(m)
      renderer.render(escena, camara)
      renderer.resetState()
      ov.requestRedraw()
    }
    ov.onAdd = () => {}
    ov.onRemove = () => {}
    ov.onContextLost = () => {
      renderer?.dispose()
      renderer = null
    }
    ov.setMap(mapa)
    return () => {
      ov.setMap(null)
      renderer?.dispose()
    }
  }, [mapa, escala, onFallo])
  return null
}
