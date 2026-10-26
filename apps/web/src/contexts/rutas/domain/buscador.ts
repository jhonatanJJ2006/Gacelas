import { rutasMock, rutasSugeridasMock } from '../../../mocks/data'

export interface Opcion {
  id: string
  name: string
  distance: number
  duration: number
  elevation: number
  color: 'primary' | 'secondary'
}

/** Rutas sugeridas mas las rutas de Loja, filtradas por el texto buscado. */
export function opcionesBuscador(consulta: string, rutaPreferida: string | null): Opcion[] {
  const sugeridas: Opcion[] = rutasSugeridasMock.map((r) => ({
    id: r.id,
    name: r.nombre,
    distance: r.distanciaKm,
    duration: r.minutos,
    elevation: r.elevacionM,
    color: r.principal ? 'primary' : 'secondary',
  }))
  const deLoja: Opcion[] = rutasMock.map((r) => ({
    id: `ruta-${r.id}`,
    name: r.nombre,
    distance: r.distanciaKm,
    // Estimacion a 5:20 /km.
    duration: Math.round(r.distanciaKm * 5.33),
    elevation: r.elevacionM,
    color: 'secondary',
  }))
  const q = consulta.trim().toLowerCase()
  const todas = [...sugeridas, ...deLoja]
  const filtradas = q ? todas.filter((o) => o.name.toLowerCase().includes(q)) : sugeridas
  const preferida = rutaPreferida ? todas.find((o) => o.id === `ruta-${rutaPreferida}`) : undefined
  return preferida && !filtradas.includes(preferida) ? [preferida, ...filtradas] : filtradas
}

/** Etiqueta de una alternativa: Óptima (la más corta), Plano (menos desnivel) o Trail (más desnivel). */
export function etiquetaOpcion(o: Opcion, todas: Opcion[]): 'Óptima' | 'Plano' | 'Trail' | null {
  if (todas.length < 2) return null
  const min = (f: (x: Opcion) => number) => todas.reduce((a, b) => (f(b) < f(a) ? b : a))
  if (min((x) => x.distance) === o) return 'Óptima'
  if (min((x) => x.elevation) === o) return 'Plano'
  if (min((x) => -x.elevation) === o) return 'Trail'
  return null
}

/** Indicaciones simplificadas segun la distancia recorrida (sin mapas todavia). */
export function indicacion(distanciaKm: number): { metros: number; texto: string } {
  const pasos = [
    { km: 0.5, texto: 'Sigue recto por la Av. Universitaria' },
    { km: 1.2, texto: 'Gira a la derecha en Av. Universitaria' },
    { km: 2.5, texto: 'Continúa junto al río Zamora' },
    { km: 4.0, texto: 'Gira a la izquierda hacia el Parque Jipiro' },
    { km: 6.0, texto: 'Rodea el lago del Parque Jipiro' },
  ]
  const siguiente = pasos.find((p) => p.km > distanciaKm)
  if (!siguiente) return { metros: 0, texto: 'Llegaste al final de la ruta' }
  return { metros: Math.round((siguiente.km - distanciaKm) * 1000), texto: siguiente.texto }
}
