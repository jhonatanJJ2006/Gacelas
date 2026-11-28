import type { CarreraGuardada } from '../../../shared/kernel/tipos'
import { calcularRacha } from '../../../shared/kernel/rachas'
import { diaSemana } from '../../../shared/kernel/estadisticas'

/*
 * Catalogo unico de logros (reemplaza al de 4 logros y a las 6 medallas de la web). Misma lista que
 * packages/shared-types/logros-catalogo.json, la tabla logros.catalogo_logros (migracion 049) y el
 * enum Logro de Android; src/__tests__/paridadLogros.test.ts lo comprueba.
 */

export type CodigoLogro =
  | 'km_5'
  | 'km_10'
  | 'km_15'
  | 'km_21'
  | 'km_42'
  | 'total_50'
  | 'total_100'
  | 'total_500'
  | 'total_1000'
  | 'primera_carrera'
  | 'racha_3'
  | 'racha_7'
  | 'racha_30'
  | 'carreras_10'
  | 'carreras_50'
  | 'semanas_meta_4'
  | 'ritmo_6'
  | 'ritmo_5'
  | 'ritmo_430'
  | 'escalador'
  | 'explorador'
  | 'desnivel_1000'
  | 'desnivel_5000'
  | 'madrugador'
  | 'nocturno'
  | 'fin_de_semana'

export type CategoriaLogro = 'distancia' | 'constancia' | 'velocidad' | 'exploracion' | 'horario'

export const CATEGORIAS_LOGRO: { id: CategoriaLogro; titulo: string }[] = [
  { id: 'distancia', titulo: 'Distancia' },
  { id: 'constancia', titulo: 'Constancia' },
  { id: 'velocidad', titulo: 'Velocidad' },
  { id: 'exploracion', titulo: 'Exploración' },
  { id: 'horario', titulo: 'Horario' },
]

export interface DefinicionLogro {
  codigo: CodigoLogro
  titulo: string
  descripcion: string
  comoConseguirlo: string
  categoria: CategoriaLogro
  /** Lo que hay que alcanzar, en la unidad de progresoLogro. */
  meta: number
  unidad: string
  icono: string
}

export const LOGROS: DefinicionLogro[] = [
  {
    codigo: 'km_5',
    titulo: '5 km',
    descripcion: 'Corriste 5 km en una carrera.',
    comoConseguirlo: 'Completa 5 km en una sola carrera.',
    categoria: 'distancia',
    meta: 5,
    unidad: 'km',
    icono: 'flag',
  },
  {
    codigo: 'km_10',
    titulo: '10 km',
    descripcion: 'Corriste 10 km en una carrera.',
    comoConseguirlo: 'Completa 10 km en una sola carrera.',
    categoria: 'distancia',
    meta: 10,
    unidad: 'km',
    icono: 'military_tech',
  },
  {
    codigo: 'km_15',
    titulo: '15 km',
    descripcion: 'Corriste 15 km en una carrera.',
    comoConseguirlo: 'Completa 15 km en una sola carrera.',
    categoria: 'distancia',
    meta: 15,
    unidad: 'km',
    icono: 'emoji_events',
  },
  {
    codigo: 'km_21',
    titulo: 'Media maratón',
    descripcion: 'Corriste 21,1 km en una carrera.',
    comoConseguirlo: 'Completa los 21,1 km de una media maratón en una sola carrera.',
    categoria: 'distancia',
    meta: 21,
    unidad: 'km',
    icono: 'workspace_premium',
  },
  {
    codigo: 'km_42',
    titulo: 'Maratón',
    descripcion: 'Corriste 42,2 km en una carrera.',
    comoConseguirlo: 'Completa los 42,2 km de un maratón en una sola carrera.',
    categoria: 'distancia',
    meta: 42,
    unidad: 'km',
    icono: 'workspace_premium',
  },
  {
    codigo: 'total_50',
    titulo: '50 km acumulados',
    descripcion: 'Sumaste 50 km entre todas tus carreras.',
    comoConseguirlo: 'Suma 50 km entre todas tus carreras guardadas.',
    categoria: 'distancia',
    meta: 50,
    unidad: 'km',
    icono: 'route',
  },
  {
    codigo: 'total_100',
    titulo: 'Centenario',
    descripcion: 'Sumaste 100 km entre todas tus carreras.',
    comoConseguirlo: 'Suma 100 km entre todas tus carreras guardadas.',
    categoria: 'distancia',
    meta: 100,
    unidad: 'km',
    icono: 'route',
  },
  {
    codigo: 'total_500',
    titulo: 'Quinientos',
    descripcion: 'Sumaste 500 km entre todas tus carreras.',
    comoConseguirlo: 'Suma 500 km entre todas tus carreras guardadas.',
    categoria: 'distancia',
    meta: 500,
    unidad: 'km',
    icono: 'route',
  },
  {
    codigo: 'total_1000',
    titulo: 'Mil kilómetros',
    descripcion: 'Sumaste 1000 km entre todas tus carreras.',
    comoConseguirlo: 'Suma 1000 km entre todas tus carreras guardadas.',
    categoria: 'distancia',
    meta: 1000,
    unidad: 'km',
    icono: 'route',
  },
  {
    codigo: 'primera_carrera',
    titulo: 'Primera carrera',
    descripcion: 'Grabaste tu primera carrera.',
    comoConseguirlo: 'Graba y guarda tu primera carrera con el botón Correr.',
    categoria: 'constancia',
    meta: 1,
    unidad: 'carrera',
    icono: 'directions_run',
  },
  {
    codigo: 'racha_3',
    titulo: 'Racha de 3 días',
    descripcion: 'Corriste 3 días seguidos.',
    comoConseguirlo: 'Corre tres días seguidos.',
    categoria: 'constancia',
    meta: 3,
    unidad: 'días',
    icono: 'local_fire_department',
  },
  {
    codigo: 'racha_7',
    titulo: 'Racha de 7 días',
    descripcion: 'Corriste 7 días seguidos.',
    comoConseguirlo: 'Corre al menos una vez cada día durante 7 días seguidos.',
    categoria: 'constancia',
    meta: 7,
    unidad: 'días',
    icono: 'local_fire_department',
  },
  {
    codigo: 'racha_30',
    titulo: 'Racha de 30 días',
    descripcion: 'Corriste 30 días seguidos.',
    comoConseguirlo: 'Corre al menos una vez cada día durante un mes.',
    categoria: 'constancia',
    meta: 30,
    unidad: 'días',
    icono: 'local_fire_department',
  },
  {
    codigo: 'carreras_10',
    titulo: '10 carreras',
    descripcion: 'Guardaste 10 carreras.',
    comoConseguirlo: 'Guarda 10 carreras en tu historial.',
    categoria: 'constancia',
    meta: 10,
    unidad: 'carreras',
    icono: 'repeat',
  },
  {
    codigo: 'carreras_50',
    titulo: '50 carreras',
    descripcion: 'Guardaste 50 carreras.',
    comoConseguirlo: 'Guarda 50 carreras en tu historial.',
    categoria: 'constancia',
    meta: 50,
    unidad: 'carreras',
    icono: 'repeat',
  },
  {
    codigo: 'semanas_meta_4',
    titulo: 'Mes constante',
    descripcion: 'Cumpliste tu meta semanal 4 semanas seguidas.',
    comoConseguirlo: 'Cumple tu meta de carreras por semana durante 4 semanas seguidas.',
    categoria: 'constancia',
    meta: 4,
    unidad: 'semanas',
    icono: 'event_available',
  },
  {
    codigo: 'ritmo_6',
    titulo: 'Bajo 6:00',
    descripcion: 'Corriste 3 km o más a menos de 6:00 por km.',
    comoConseguirlo: 'Corre al menos 3 km con un ritmo medio menor a 6:00 por km.',
    categoria: 'velocidad',
    meta: 1,
    unidad: '',
    icono: 'speed',
  },
  {
    codigo: 'ritmo_5',
    titulo: 'Bajo 5:00',
    descripcion: 'Corriste 3 km o más a menos de 5:00 por km.',
    comoConseguirlo: 'Corre al menos 3 km con un ritmo medio menor a 5:00 por km.',
    categoria: 'velocidad',
    meta: 1,
    unidad: '',
    icono: 'speed',
  },
  {
    codigo: 'ritmo_430',
    titulo: 'Bajo 4:30',
    descripcion: 'Corriste 5 km o más a menos de 4:30 por km.',
    comoConseguirlo: 'Corre al menos 5 km con un ritmo medio menor a 4:30 por km.',
    categoria: 'velocidad',
    meta: 1,
    unidad: '',
    icono: 'speed',
  },
  {
    codigo: 'escalador',
    titulo: 'Escalador',
    descripcion: 'Subiste 100 m en una carrera.',
    comoConseguirlo: 'Acumula 100 m de desnivel positivo en una sola carrera.',
    categoria: 'exploracion',
    meta: 100,
    unidad: 'm',
    icono: 'terrain',
  },
  {
    codigo: 'explorador',
    titulo: 'Explorador',
    descripcion: 'Empezaste carreras en 5 lugares distintos.',
    comoConseguirlo: 'Sal a correr desde 5 lugares distintos de la ciudad.',
    categoria: 'exploracion',
    meta: 5,
    unidad: 'lugares',
    icono: 'explore',
  },
  {
    codigo: 'desnivel_1000',
    titulo: 'Andino',
    descripcion: 'Acumulaste 1000 m de subida entre todas tus carreras.',
    comoConseguirlo: 'Acumula 1000 m de desnivel positivo entre tus carreras.',
    categoria: 'exploracion',
    meta: 1000,
    unidad: 'm',
    icono: 'landscape',
  },
  {
    codigo: 'desnivel_5000',
    titulo: 'Cumbre',
    descripcion: 'Acumulaste 5000 m de subida entre todas tus carreras.',
    comoConseguirlo: 'Acumula 5000 m de desnivel positivo entre tus carreras.',
    categoria: 'exploracion',
    meta: 5000,
    unidad: 'm',
    icono: 'landscape',
  },
  {
    codigo: 'madrugador',
    titulo: 'Madrugador',
    descripcion: 'Saliste a correr 3 veces antes de las 7:00.',
    comoConseguirlo: 'Empieza 3 carreras antes de las 7:00 de la mañana.',
    categoria: 'horario',
    meta: 3,
    unidad: 'salidas',
    icono: 'wb_twilight',
  },
  {
    codigo: 'nocturno',
    titulo: 'Corredor nocturno',
    descripcion: 'Saliste a correr 3 veces después de las 19:00.',
    comoConseguirlo: 'Empieza 3 carreras después de las 19:00.',
    categoria: 'horario',
    meta: 3,
    unidad: 'salidas',
    icono: 'nights_stay',
  },
  {
    codigo: 'fin_de_semana',
    titulo: 'Guerrero de fin de semana',
    descripcion: 'Corriste 5 sábados o domingos.',
    comoConseguirlo: 'Corre 5 veces en sábado o domingo.',
    categoria: 'horario',
    meta: 5,
    unidad: 'salidas',
    icono: 'weekend',
  },
]

export const logroPorCodigo = (codigo: string) => LOGROS.find((l) => l.codigo === codigo)

const MIN = 60

/** Progreso hacia el logro en su unidad (km, dias, carreras...), sin pasar de la meta. */
export function progresoLogro(
  codigo: CodigoLogro,
  carreras: CarreraGuardada[],
  ahoraMs: number,
  /** Sin redondear los km (para barras de avance); por defecto como Android, en enteros. */
  exacto = false
): number {
  const ent = (n: number) => (exacto ? n : Math.floor(n))
  const max = Math.max(0, ...carreras.map((c) => c.distanciaKm))
  const total = carreras.reduce((s, c) => s + c.distanciaKm, 0)
  const subida = carreras.reduce((s, c) => s + (c.desnivelM || 0), 0)
  const conRitmo = (km: number, segKm: number) =>
    carreras.some((c) => c.distanciaKm >= km && c.segundos / c.distanciaKm < segKm) ? 1 : 0
  const hora = (c: CarreraGuardada) => new Date(c.inicioMs).getHours()
  const meta = logroPorCodigo(codigo)!.meta
  let v: number
  switch (codigo) {
    case 'primera_carrera':
    case 'carreras_10':
    case 'carreras_50':
      v = carreras.length
      break
    case 'km_5':
    case 'km_10':
    case 'km_15':
      v = ent(max)
      break
    case 'km_21':
      v = max >= 21.0975 ? 21 : Math.min(ent(max), exacto ? 20.99 : 20)
      break
    case 'km_42':
      v = max >= 42.195 ? 42 : Math.min(ent(max), exacto ? 41.99 : 41)
      break
    case 'total_50':
    case 'total_100':
    case 'total_500':
    case 'total_1000':
      v = ent(total)
      break
    case 'racha_3':
    case 'racha_7':
    case 'racha_30':
      v = calcularRacha(carreras, ahoraMs).mejorDiaria
      break
    case 'semanas_meta_4':
      v = calcularRacha(carreras, ahoraMs).mejorSemanal
      break
    case 'ritmo_6':
      v = conRitmo(3, 6 * MIN)
      break
    case 'ritmo_5':
      v = conRitmo(3, 5 * MIN)
      break
    case 'ritmo_430':
      v = conRitmo(5, 4.5 * MIN)
      break
    case 'escalador':
      v = Math.max(0, ...carreras.map((c) => c.desnivelM || 0))
      break
    case 'desnivel_1000':
    case 'desnivel_5000':
      v = Math.floor(subida)
      break
    case 'explorador':
      v = new Set(
        carreras
          .filter((c) => c.puntos?.length)
          .map((c) => `${Math.trunc(c.puntos[0].lat * 100)},${Math.trunc(c.puntos[0].lng * 100)}`)
      ).size
      break
    case 'madrugador':
      v = carreras.filter((c) => hora(c) < 7).length
      break
    case 'nocturno':
      v = carreras.filter((c) => hora(c) >= 19).length
      break
    case 'fin_de_semana':
      v = carreras.filter((c) => diaSemana(c.inicioMs) >= 5).length
      break
  }
  return Math.min(Math.max(0, Number.isFinite(v) ? v : 0), meta)
}

/** Reglas: que logros cumple el historial de carreras. */
export function logrosCumplidos(carreras: CarreraGuardada[], ahoraMs: number): CodigoLogro[] {
  return LOGROS.filter((l) => progresoLogro(l.codigo, carreras, ahoraMs) >= l.meta).map(
    (l) => l.codigo
  )
}

export interface EstadoLogro {
  logro: DefinicionLogro
  progreso: number
  /** Ganado: ya registrado en el dispositivo o cumplido por las carreras actuales. */
  lograda: boolean
  /** Fecha (ms) en que se registro, si esta registrado. */
  fecha: number | null
}

/** Estado de los 26 logros, en el orden del catalogo. */
export function estadoLogros(
  carreras: CarreraGuardada[],
  hechos: Record<string, number>,
  ahoraMs: number
): EstadoLogro[] {
  return LOGROS.map((logro) => {
    const progreso = progresoLogro(logro.codigo, carreras, ahoraMs)
    const fecha = hechos[logro.codigo] ?? null
    return { logro, progreso, lograda: fecha != null || progreso >= logro.meta, fecha }
  })
}

/** Texto de avance: "8,0/10 km" o "3/7 días". */
export function textoAvance(l: DefinicionLogro, progreso: number): string {
  return `${progreso}/${l.meta}${l.unidad ? ` ${l.unidad}` : ''}`
}
