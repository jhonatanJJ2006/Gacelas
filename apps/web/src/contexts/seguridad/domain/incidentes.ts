/*
 * Incidentes de seguridad: datos abiertos del ECU 911 agregados por parroquia (sin hora ni
 * coordenadas) y reportes de corredores. Ver docs/INCIDENTES-ECU911.md y docs/INCIDENTES.md.
 */

export const TIPOS_INCIDENTE = [
  { id: 'robo_asalto', nombre: 'Robo o asalto', icono: 'report' },
  { id: 'acoso', nombre: 'Acoso', icono: 'front_hand' },
  { id: 'perros_agresivos', nombre: 'Perros agresivos', icono: 'pets' },
  { id: 'iluminacion_deficiente', nombre: 'Iluminación deficiente', icono: 'lightbulb' },
  { id: 'trafico_peligroso', nombre: 'Tráfico peligroso', icono: 'traffic' },
  { id: 'via_mal_estado', nombre: 'Vía en mal estado', icono: 'construction' },
  { id: 'accidente', nombre: 'Accidente', icono: 'car_crash' },
  { id: 'otro', nombre: 'Otro', icono: 'more_horiz' },
] as const
export type TipoIncidente = (typeof TIPOS_INCIDENTE)[number]['id']
export const nombreTipo = (t: string) => TIPOS_INCIDENTE.find((x) => x.id === t)?.nombre ?? t

export type Nivel = 'bajo' | 'medio' | 'alto'
export interface SeguridadZona {
  dias: { dia: number; nivel: Nivel; indice: number }[]
  tipos: { servicio: string; subtipo: string; conteo: number }[]
  actualizadoHasta: string | null
  fuente: string
}
export interface IncidentePublico {
  id: string
  tipo: TipoIncidente
  lat: number
  lng: number
  ocurridoMs: number
  confirmaciones: number
  estado: 'pendiente' | 'verificado'
  fotos: string[]
}

export const DIAS_CORTOS = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb']
export const AVISO_SEGURIDAD =
  'Indicador orientativo con datos agregados del ECU 911 por parroquia (sin hora exacta) y reportes de corredores. Llega con meses de retraso y no garantiza tu seguridad: mantente atento y, ante una emergencia, llama al 911.'
export const MAX_FOTOS = 3
export const MAX_BYTES_FOTO = 3 * 1024 * 1024

/** Datos de ejemplo (ficticios) para ?vista-previa. */
export const SEGURIDAD_EJEMPLO: SeguridadZona = {
  dias: [0.6, 0.9, 0.8, 0.9, 1.1, 1.6, 1.3].map((indice, dia) => ({
    dia,
    indice,
    nivel: indice < 0.75 ? 'bajo' : indice < 1.5 ? 'medio' : 'alto',
  })),
  tipos: [
    { servicio: 'Tránsito y Movilidad', subtipo: 'Ejemplo: siniestro de tránsito', conteo: 18 },
    { servicio: 'Seguridad Ciudadana', subtipo: 'Ejemplo: alerta de seguridad', conteo: 11 },
    { servicio: 'Gestión Sanitaria', subtipo: 'Ejemplo: atención prehospitalaria', conteo: 7 },
  ],
  actualizadoHasta: '2026-03',
  fuente: 'Ejemplo de vista previa. Fuente real: SIS ECU 911 (CC-BY) y reportes de corredores',
}

export function incidentesEjemplo(ahora = Date.now()): IncidentePublico[] {
  return [
    {
      id: 'vp-i1',
      tipo: 'perros_agresivos',
      lat: -3.994,
      lng: -79.203,
      ocurridoMs: ahora - 2 * 3600e3,
      confirmaciones: 3,
      estado: 'verificado',
      fotos: [],
    },
    {
      id: 'vp-i2',
      tipo: 'iluminacion_deficiente',
      lat: -3.986,
      lng: -79.199,
      ocurridoMs: ahora - 26 * 3600e3,
      confirmaciones: 1,
      estado: 'pendiente',
      fotos: [],
    },
    {
      id: 'vp-i3',
      tipo: 'trafico_peligroso',
      lat: -4.001,
      lng: -79.207,
      ocurridoMs: ahora - 50 * 3600e3,
      confirmaciones: 5,
      estado: 'verificado',
      fotos: [],
    },
  ]
}

export function nivelDeIndice(i: number): Nivel {
  return i < 0.75 ? 'bajo' : i < 1.5 ? 'medio' : 'alto'
}

export function colorNivel(n: Nivel) {
  return n === 'bajo' ? '#68DBA9' : n === 'medio' ? '#F5C451' : '#FF8A7A'
}

export function haceTexto(ms: number, ahora = Date.now()) {
  const h = Math.round((ahora - ms) / 3600e3)
  if (h < 1) return 'hace menos de 1 h'
  if (h < 24) return `hace ${h} h`
  return `hace ${Math.round(h / 24)} d`
}
