/** Configuracion de Google Maps y trazados de las rutas de ejemplo en Loja. */

export const claveMapas = () =>
  import.meta.env.MODE === 'test'
    ? ''
    : ((import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string | undefined) ?? '')

export const CENTRO_LOJA = { lat: -3.9931, lng: -79.2042 }

export interface Coordenada {
  lat: number
  lng: number
}

/** Mismos trazados que backend/migrations/003_seed_loja.sql. */
export const TRAZOS_RUTAS: Record<string, Coordenada[]> = {
  '1': [
    { lat: -3.9746, lng: -79.2017 },
    { lat: -3.9712, lng: -79.2001 },
    { lat: -3.969, lng: -79.203 },
    { lat: -3.9718, lng: -79.2058 },
    { lat: -3.9746, lng: -79.2017 },
  ],
  '2': [
    { lat: -3.9931, lng: -79.2005 },
    { lat: -3.999, lng: -79.1985 },
    { lat: -4.006, lng: -79.197 },
    { lat: -4.0125, lng: -79.195 },
  ],
  '3': [
    { lat: -3.995, lng: -79.225 },
    { lat: -3.999, lng: -79.24 },
    { lat: -4.001, lng: -79.255 },
    { lat: -4.0002, lng: -79.264 },
  ],
  primary: [
    { lat: -3.9746, lng: -79.2017 },
    { lat: -3.9845, lng: -79.2035 },
    { lat: -3.9931, lng: -79.2005 },
    { lat: -4.0045, lng: -79.1958 },
  ],
  alt: [
    { lat: -3.9746, lng: -79.2017 },
    { lat: -3.988, lng: -79.2075 },
    { lat: -3.999, lng: -79.201 },
  ],
}

export const trazoDeOpcion = (id: string | null): Coordenada[] =>
  id ? (TRAZOS_RUTAS[id.replace(/^ruta-/, '')] ?? []) : []

/** Estilo oscuro de docs/DESIGN.md (carreteras #374151, parques #064E3B, agua #1E3A8A). */
export const ESTILO_OSCURO = [
  { elementType: 'geometry', stylers: [{ color: '#0b1220' }] },
  { elementType: 'labels.text.fill', stylers: [{ color: '#94a3b8' }] },
  { elementType: 'labels.text.stroke', stylers: [{ color: '#0b1220' }] },
  { featureType: 'road', elementType: 'geometry', stylers: [{ color: '#374151' }] },
  { featureType: 'road', elementType: 'geometry.stroke', stylers: [{ color: '#1f2937' }] },
  { featureType: 'poi', elementType: 'labels', stylers: [{ visibility: 'off' }] },
  { featureType: 'poi.park', elementType: 'geometry', stylers: [{ color: '#064E3B' }] },
  { featureType: 'water', elementType: 'geometry', stylers: [{ color: '#1E3A8A' }] },
  { featureType: 'transit', stylers: [{ visibility: 'off' }] },
]

/** Estilo claro (carreteras #E5E7EB, parques #D1FAE5, agua #BFDBFE). */
export const ESTILO_CLARO = [
  { featureType: 'road', elementType: 'geometry', stylers: [{ color: '#E5E7EB' }] },
  { featureType: 'poi.park', elementType: 'geometry', stylers: [{ color: '#D1FAE5' }] },
  { featureType: 'water', elementType: 'geometry', stylers: [{ color: '#BFDBFE' }] },
  { featureType: 'poi', elementType: 'labels', stylers: [{ visibility: 'off' }] },
]
