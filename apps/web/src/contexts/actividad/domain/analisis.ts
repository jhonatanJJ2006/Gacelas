import type { Parcial } from '../../../shared/kernel/tipos'
import { distanciaPuntos, type PuntoGps } from '../../../shared/lib/geo'

/** Parciales automaticos de un kilometro. Las vueltas manuales no entran al grafico. */
export function parcialesPorKm(parciales: Parcial[]): Parcial[] {
  return parciales.filter((p) => !p.manual && p.distanciaKm > 0)
}

export interface PuntoElevacion {
  distanciaKm: number
  altitudM: number
}

/** Altura contra distancia recorrida. Se remuestrea a pasos iguales de distancia. */
export function serieElevacion(puntos: PuntoGps[], maxPuntos = 80): PuntoElevacion[] {
  const conAltura = puntos.filter((p) => p.altitudM != null)
  if (conAltura.length < 2) return []
  let metros = 0
  const serie: PuntoElevacion[] = [{ distanciaKm: 0, altitudM: conAltura[0].altitudM as number }]
  for (let i = 1; i < conAltura.length; i++) {
    metros += distanciaPuntos(conAltura[i - 1], conAltura[i])
    serie.push({ distanciaKm: metros / 1000, altitudM: conAltura[i].altitudM as number })
  }
  const total = serie[serie.length - 1].distanciaKm
  if (serie.length <= maxPuntos || total <= 0) return serie
  const out: PuntoElevacion[] = []
  for (let i = 0; i < maxPuntos; i++) {
    const d = (total * i) / (maxPuntos - 1)
    let j = 1
    while (j < serie.length - 1 && serie[j].distanciaKm < d) j++
    const a = serie[j - 1]
    const b = serie[j]
    const tramo = b.distanciaKm - a.distanciaKm
    const t = tramo === 0 ? 0 : (d - a.distanciaKm) / tramo
    out.push({ distanciaKm: d, altitudM: a.altitudM + (b.altitudM - a.altitudM) * t })
  }
  return out
}

export function escaparXml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function iso(ms: number): string {
  return new Date(ms).toISOString().replace(/\.\d{3}Z$/, 'Z')
}

export function nombreTcx(titulo: string, inicioMs: number): string {
  const base =
    titulo
      .trim()
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-|-$/g, '') || 'carrera'
  const d = new Date(inicioMs)
  const fecha = `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}${String(d.getDate()).padStart(2, '0')}`
  return `${base}-${fecha}.tcx`
}

export interface DatosTcx {
  titulo: string
  inicioMs: number
  segundos: number
  distanciaKm: number
  puntos: PuntoGps[]
}

/** Actividad de correr en TCX, con un trackpoint por punto GPS. */
export function carreraATcx(c: DatosTcx): string {
  let metros = 0
  const track = c.puntos
    .map((p, i) => {
      if (i > 0) metros += distanciaPuntos(c.puntos[i - 1], p)
      const alt =
        p.altitudM != null ? `<AltitudeMeters>${p.altitudM.toFixed(1)}</AltitudeMeters>` : ''
      const tiempo = iso(p.tiempoMs > 0 ? p.tiempoMs : c.inicioMs)
      return `<Trackpoint><Time>${tiempo}</Time><Position><LatitudeDegrees>${p.lat}</LatitudeDegrees><LongitudeDegrees>${p.lng}</LongitudeDegrees></Position>${alt}<DistanceMeters>${metros.toFixed(1)}</DistanceMeters></Trackpoint>`
    })
    .join('')
  const id = iso(c.inicioMs)
  return (
    `<?xml version="1.0" encoding="UTF-8"?>` +
    `<TrainingCenterDatabase xmlns="http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2">` +
    `<Activities><Activity Sport="Running"><Id>${id}</Id>` +
    `<Lap StartTime="${id}">` +
    `<TotalTimeSeconds>${Math.max(0, Math.round(c.segundos))}</TotalTimeSeconds>` +
    `<DistanceMeters>${(c.distanciaKm * 1000).toFixed(1)}</DistanceMeters>` +
    `<Intensity>Active</Intensity><TriggerMethod>Manual</TriggerMethod>` +
    `<Track>${track}</Track></Lap>` +
    `<Notes>${escaparXml(c.titulo)}</Notes>` +
    `</Activity></Activities></TrainingCenterDatabase>`
  )
}

export interface LadoComparacion {
  id: string
  titulo: string
  distanciaKm: number
  segundos: number
  ritmoSegKm: number | null
  desnivelM: number
}

export function ladoComparacion(c: {
  id: string
  titulo: string
  distanciaKm: number
  segundos: number
  desnivelM: number
}): LadoComparacion {
  return {
    id: c.id,
    titulo: c.titulo,
    distanciaKm: c.distanciaKm,
    segundos: c.segundos,
    ritmoSegKm: c.distanciaKm > 0.01 ? Math.floor(c.segundos / c.distanciaKm) : null,
    desnivelM: c.desnivelM,
  }
}
