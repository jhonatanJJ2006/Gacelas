import type { PuntoGps } from '../../../shared/lib/geo'

export interface CarreraGpx {
  titulo: string
  inicioMs: number
  puntos: PuntoGps[]
}

const xml = (s: string) =>
  s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')

/** GPX 1.1 de una carrera. No incluye puntos que no existan. */
export function carreraAGpx(c: CarreraGpx): string {
  const pts = c.puntos
    .map((p) => {
      const ele = p.altitudM == null ? '' : `<ele>${p.altitudM}</ele>`
      const time = Number.isFinite(p.tiempoMs)
        ? `<time>${new Date(p.tiempoMs).toISOString()}</time>`
        : ''
      return `<trkpt lat="${p.lat}" lon="${p.lng}">${ele}${time}</trkpt>`
    })
    .join('')
  const cuando = Number.isFinite(c.inicioMs) ? new Date(c.inicioMs).toISOString() : ''
  return `<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="Runcity" xmlns="http://www.topografix.com/GPX/1/1">
<metadata><name>${xml(c.titulo)}</name>${cuando ? `<time>${cuando}</time>` : ''}</metadata>
<trk><name>${xml(c.titulo)}</name><trkseg>${pts}</trkseg></trk>
</gpx>
`
}

export function nombreGpx(titulo: string): string {
  const base = titulo
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
  return `${base || 'carrera'}.gpx`
}
