import type { PuntoGps } from '../lib/geo'
import type { ConfigIntervalos } from './intervalos'

export type { PuntoGps, ConfigIntervalos }

export interface Parcial {
  numero: number
  distanciaKm: number
  segundos: number
  manual: boolean
}

export interface CarreraGuardada {
  id: string
  titulo: string
  nota: string
  publica: boolean
  inicioMs: number
  segundos: number
  distanciaKm: number
  desnivelM: number
  parciales: Parcial[]
  puntos: PuntoGps[]
}

export interface Perfil {
  email: string
  nombre: string
  username: string
  bio: string
  ubicacion: string
  edad: string
  genero: string
  /** Foto como data URL (se guarda en el navegador). */
  foto: string | null
}

export type ResultadoAuth = { ok: true; perfil: Perfil } | { ok: false; error: string }

export type Visibilidad = 'publico' | 'seguidores' | 'privado'

export interface ConfigAvatar {
  equipacion: number
  estilo: string
  lentes: boolean
  reloj: boolean
  audifonos: boolean
  /** Rasgos guardados; el modelo GLB actual tiene una sola textura y aun no los pinta. */
  complexion?: 'hombre' | 'mujer'
  tonoPiel?: number
  peinado?: 'corto' | 'coleta' | 'rapado'
  colorShorts?: number
}

export interface Ajustes {
  modoOscuro: boolean
  unidadesMetricas: boolean
  visibilidad: Visibilidad
  ubicacionPrecisa: boolean
  ocultarHogar: boolean
  radioHogarM: number
  notifLikes: boolean
  notifComentarios: boolean
  notifSeguidores: boolean
  notifEnVivo: boolean
  notifEventos: boolean
  notifEmail: boolean
  intervalos: ConfigIntervalos
  metronomoBpm: number
  avatar: ConfigAvatar
  notificacionesLeidas: string[]
  likes: string[]
  comentarios: Record<string, string[]>
  ocultas: string[]
  /** Carreras por semana para mantener la racha semanal. */
  metaSemanal: number
  /** Ritmo objetivo del pacer en segundos por km. */
  ritmoObjetivoSeg: number
  autopausa: boolean
  anunciosVoz: boolean
  /** Segundos de cuenta regresiva antes de arrancar el GPS (0 = sin cuenta). */
  cuentaRegresivaSeg: number
  idioma: 'es-EC' | 'en'
  ocultarBiometricos: boolean
  aparecerRankings: boolean
  /** Nivel que el corredor declaró al registrarse. */
  nivelDeclarado: 'principiante' | 'intermedio' | 'avanzado'
  /** Datos fisiologicos privados (solo los ve su dueno en user_settings). */
  pesoKg: number | null
  alturaCm: number | null
  distanciaFavorita: '5K' | '10K' | '21K' | '42K'
  /** Meta de kilometraje semanal. */
  metaKmSemana: number
  /** Compartir la ubicacion en vivo al correr; null = aun no se pregunto (modal la primera vez). */
  compartirEnVivo: boolean | null
}

export const AJUSTES_POR_DEFECTO: Ajustes = {
  modoOscuro: true,
  unidadesMetricas: true,
  visibilidad: 'seguidores',
  ubicacionPrecisa: true,
  ocultarHogar: true,
  radioHogarM: 300,
  notifLikes: true,
  notifComentarios: true,
  notifSeguidores: true,
  notifEnVivo: true,
  notifEventos: true,
  notifEmail: false,
  intervalos: { trabajoSeg: 60, descansoSeg: 30, rondas: 6, vibracion: true, sonido: true },
  metronomoBpm: 168,
  avatar: { equipacion: 0, estilo: 'Dinámico', lentes: false, reloj: true, audifonos: true },
  notificacionesLeidas: [],
  likes: [],
  comentarios: {},
  ocultas: [],
  metaSemanal: 3,
  ritmoObjetivoSeg: 330,
  autopausa: true,
  anunciosVoz: true,
  cuentaRegresivaSeg: 3,
  idioma: 'es-EC',
  ocultarBiometricos: false,
  aparecerRankings: true,
  nivelDeclarado: 'principiante',
  pesoKg: null,
  alturaCm: null,
  distanciaFavorita: '10K',
  metaKmSemana: 25,
  compartirEnVivo: null,
}
