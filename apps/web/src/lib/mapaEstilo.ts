/** Estilo y matematicas del mapa Gacelas (estilo Uber: oscuro, limpio, acentos naranja). */
import type { Coordenada } from './mapas'

export const NARANJA = '#C084FC'
export const VERDE = '#10B981'

/** Paleta: tierra azul marino/carbon, vias apagadas, sin POIs ni transporte, etiquetas sutiles. */
export const ESTILO_GACELAS: google.maps.MapTypeStyle[] = [
  { elementType: 'geometry', stylers: [{ color: '#0d1422' }] },
  { elementType: 'labels.icon', stylers: [{ visibility: 'off' }] },
  { elementType: 'labels.text.fill', stylers: [{ color: '#6b7a90' }] },
  { elementType: 'labels.text.stroke', stylers: [{ color: '#0d1422' }, { weight: 3 }] },
  { featureType: 'administrative', elementType: 'geometry', stylers: [{ visibility: 'off' }] },
  {
    featureType: 'administrative.locality',
    elementType: 'labels.text.fill',
    stylers: [{ color: '#9aa6b8' }],
  },
  {
    featureType: 'administrative.neighborhood',
    elementType: 'labels',
    stylers: [{ visibility: 'off' }],
  },
  { featureType: 'poi', stylers: [{ visibility: 'off' }] },
  { featureType: 'poi.park', stylers: [{ visibility: 'on' }] },
  { featureType: 'poi.park', elementType: 'geometry', stylers: [{ color: '#0f2a24' }] },
  { featureType: 'poi.park', elementType: 'labels', stylers: [{ visibility: 'off' }] },
  { featureType: 'landscape.man_made', elementType: 'geometry', stylers: [{ color: '#111a2b' }] },
  { featureType: 'landscape.natural', elementType: 'geometry', stylers: [{ color: '#0e1726' }] },
  { featureType: 'road', elementType: 'geometry.fill', stylers: [{ color: '#1c2638' }] },
  { featureType: 'road', elementType: 'geometry.stroke', stylers: [{ color: '#0d1422' }] },
  { featureType: 'road', elementType: 'labels.text.fill', stylers: [{ color: '#5d6b80' }] },
  { featureType: 'road.highway', elementType: 'geometry.fill', stylers: [{ color: '#2a3446' }] },
  { featureType: 'road.highway', elementType: 'geometry.stroke', stylers: [{ color: '#3a2a1e' }] },
  { featureType: 'road.local', elementType: 'labels', stylers: [{ visibility: 'off' }] },
  { featureType: 'transit', stylers: [{ visibility: 'off' }] },
  { featureType: 'water', elementType: 'geometry', stylers: [{ color: '#0a2340' }] },
  { featureType: 'water', elementType: 'labels', stylers: [{ visibility: 'off' }] },
]

/** Map ID para el modo vectorial (inclinacion, giro, 3D). Si no hay uno propio se usa el de demostracion. */
export const mapId = () =>
  (import.meta.env.VITE_GOOGLE_MAP_ID as string | undefined) || 'DEMO_MAP_ID'
export const tieneMapIdPropio = () => Boolean(import.meta.env.VITE_GOOGLE_MAP_ID)

const rad = (g: number) => (g * Math.PI) / 180
const grad = (r: number) => (r * 180) / Math.PI

/** Rumbo inicial de a hacia b, en grados 0..360 (0 = norte). */
export function rumbo(a: Coordenada, b: Coordenada) {
  const f1 = rad(a.lat)
  const f2 = rad(b.lat)
  const dl = rad(b.lng - a.lng)
  const y = Math.sin(dl) * Math.cos(f2)
  const x = Math.cos(f1) * Math.sin(f2) - Math.sin(f1) * Math.cos(f2) * Math.cos(dl)
  return (grad(Math.atan2(y, x)) + 360) % 360
}

/** Punto a `metros` de `p` en direccion `rumboGrados`. */
export function desplazar(p: Coordenada, rumboGrados: number, metros: number): Coordenada {
  const d = metros / 6371000
  const r = rad(rumboGrados)
  const f1 = rad(p.lat)
  const l1 = rad(p.lng)
  const f2 = Math.asin(Math.sin(f1) * Math.cos(d) + Math.cos(f1) * Math.sin(d) * Math.cos(r))
  const l2 =
    l1 +
    Math.atan2(Math.sin(r) * Math.sin(d) * Math.cos(f1), Math.cos(d) - Math.sin(f1) * Math.sin(f2))
  return { lat: grad(f2), lng: grad(l2) }
}

/** Diferencia angular mas corta (-180..180). */
export const deltaAngulo = (de: number, a: number) => ((((a - de) % 360) + 540) % 360) - 180

export const interpolar = (a: Coordenada, b: Coordenada, t: number): Coordenada => ({
  lat: a.lat + (b.lat - a.lat) * t,
  lng: a.lng + (b.lng - a.lng) * t,
})

/** Suavizado (ease-out) para animar la posicion entre lecturas del GPS. */
export const suavizar = (t: number) => 1 - Math.pow(1 - Math.min(Math.max(t, 0), 1), 3)

const hex = (c: string) => [1, 3, 5].map((i) => parseInt(c.slice(i, i + 2), 16))
const mezclar = (a: string, b: string, t: number) =>
  '#' +
  hex(a)
    .map((v, i) =>
      Math.round(v + (hex(b)[i] - v) * t)
        .toString(16)
        .padStart(2, '0')
    )
    .join('')

/** Color por ritmo: lento (>= 7:30/km) naranja, rapido (<= 4:30/km) verde. */
export function colorRitmo(segKm: number | null) {
  if (segKm == null || !isFinite(segKm)) return NARANJA
  const t = Math.min(Math.max((450 - segKm) / (450 - 270), 0), 1)
  return mezclar(NARANJA, VERDE, t)
}

/** Tramos del recorrido con su color segun el ritmo de cada tramo. */
export function tramosPorRitmo(
  puntos: { lat: number; lng: number; tiempoMs?: number }[],
  metrosPorTramo = 120
): { camino: Coordenada[]; color: string }[] {
  if (puntos.length < 2) return []
  const tramos: { camino: Coordenada[]; color: string }[] = []
  let actual: Coordenada[] = [puntos[0]]
  let d = 0
  let t0 = puntos[0].tiempoMs
  for (let i = 1; i < puntos.length; i++) {
    const a = puntos[i - 1]
    const b = puntos[i]
    d += distanciaM(a, b)
    actual.push(b)
    if (d >= metrosPorTramo || i === puntos.length - 1) {
      const seg = t0 != null && b.tiempoMs != null ? (b.tiempoMs - t0) / 1000 : null
      tramos.push({ camino: actual, color: colorRitmo(seg && d > 0 ? seg / (d / 1000) : null) })
      actual = [b]
      d = 0
      t0 = b.tiempoMs
    }
  }
  return tramos
}

export function distanciaM(a: Coordenada, b: Coordenada) {
  const R = 6371000
  const dLat = rad(b.lat - a.lat)
  const dLng = rad(b.lng - a.lng)
  const h =
    Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.lat)) * Math.cos(rad(b.lat)) * Math.sin(dLng / 2) ** 2
  return 2 * R * Math.asin(Math.sqrt(h))
}

/** Animacion del corredor 3D segun el ritmo (segundos por km). */
export function animacionPorRitmo(segKm: number | null | undefined): {
  clip: 'idle' | 'jog' | 'run'
  velocidad: number
} {
  if (segKm == null || !isFinite(segKm) || segKm <= 0 || segKm > 900)
    return { clip: 'idle', velocidad: 1 }
  if (segKm > 360) return { clip: 'jog', velocidad: Math.min(1.3, Math.max(0.7, 420 / segKm)) }
  return { clip: 'run', velocidad: Math.min(1.6, Math.max(0.8, 300 / segKm)) }
}

/** Rumbo de los ultimos metros (mas estable que el de los dos ultimos puntos). */
export function rumboReciente(puntos: Coordenada[], metros = 15) {
  if (puntos.length < 2) return null
  const fin = puntos[puntos.length - 1]
  for (let i = puntos.length - 2; i >= 0; i--)
    if (distanciaM(puntos[i], fin) >= metros || i === 0) return rumbo(puntos[i], fin)
  return null
}
