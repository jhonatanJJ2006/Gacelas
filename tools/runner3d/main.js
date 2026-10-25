import * as THREE from 'three'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'

// Corredor 3D en WebGL para equipos donde Filament no puede renderizar (p. ej. emulador con SwiftShader).
const q = new URLSearchParams(location.search)
const clipInicial = q.get('clip') || 'Idle'
const cam = (q.get('cam') || '0,1.2,3.5').split(',').map(Number)
const autoRotar = q.get('rotar') === '1'
const renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true })
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
renderer.setClearColor(0x000000, 0)
renderer.outputColorSpace = THREE.SRGBColorSpace
document.body.appendChild(renderer.domElement)
const escena = new THREE.Scene()
const camara = new THREE.PerspectiveCamera(35, 1, 0.1, 100)
camara.position.set(cam[0], cam[1], cam[2])
camara.lookAt(0, 0, 0)
escena.add(new THREE.HemisphereLight(0xffffff, 0x2a1240, 2.2))
const luz = new THREE.DirectionalLight(0xffffff, 2.4)
luz.position.set(2, 4, 3)
escena.add(luz)
const grupo = new THREE.Group()
escena.add(grupo)
let mezclador = null
let acciones = {}
let actual = null
let rumbo = 0
const reloj = new THREE.Clock()

function ajustar() {
  const w = window.innerWidth,
    h = window.innerHeight
  renderer.setSize(w, h, false)
  renderer.domElement.style.width = w + 'px'
  renderer.domElement.style.height = h + 'px'
  camara.aspect = w / Math.max(h, 1)
  camara.updateProjectionMatrix()
}
window.addEventListener('resize', ajustar)
ajustar()

window.gacelasClip = (nombre) => {
  const a = acciones[nombre]
  if (!a || a === actual) return
  a.reset().fadeIn(0.4).play()
  if (actual) actual.fadeOut(0.4)
  actual = a
}
window.gacelasRumbo = (g) => {
  rumbo = g
}

new GLTFLoader().load(q.get('modelo') || '../models/gacelas-runner-animated.glb', (gltf) => {
  const m = gltf.scene
  const caja = new THREE.Box3().setFromObject(m)
  const alto = caja.max.y - caja.min.y || 1
  const s = 1.8 / alto
  m.scale.setScalar(s)
  m.position.y = -((caja.min.y + caja.max.y) / 2) * s
  grupo.add(m)
  mezclador = new THREE.AnimationMixer(m)
  gltf.animations.forEach((c) => {
    acciones[c.name] = mezclador.clipAction(c)
  })
  window.gacelasClip(clipInicial)
  document.body.dataset.listo = '1'
})

let angulo = 0
renderer.setAnimationLoop(() => {
  const dt = reloj.getDelta()
  if (mezclador) mezclador.update(dt)
  if (autoRotar) angulo = (angulo + 0.3) % 360
  grupo.rotation.y = THREE.MathUtils.degToRad(autoRotar ? angulo : -rumbo)
  renderer.render(escena, camara)
})
