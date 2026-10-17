export const SITIO: string
export function idValido(id: unknown): boolean
export function htmlRuta(a: {
  id: string
  ruta: { name: string; description?: string; distance_km?: number; elevation_m?: number } | null
  imagen: string
}): string
