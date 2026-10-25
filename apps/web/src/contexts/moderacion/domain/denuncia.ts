export type MotivoReporte =
  'spam' | 'ofensivo' | 'acoso' | 'falso' | 'suplantacion' | 'peligroso' | 'otro'

/** Tipos que registra la migracion 023 en report_target_types. */
export type TipoContenido = 'post' | 'usuario' | 'comentario' | 'evento'

/**
 * Objetivo de una denuncia. La cola es generica: otro contexto (por ejemplo incidentes)
 * registra su tipo en report_target_types y aqui con registrarTipoObjetivo, sin tocar la cola.
 */
export type TipoObjetivo = TipoContenido | (string & {})
export interface ObjetivoDenuncia {
  tipo: TipoObjetivo
  id: string
}

const COMUNES: [MotivoReporte, string][] = [
  ['spam', 'Spam o publicidad'],
  ['ofensivo', 'Contenido ofensivo'],
  ['acoso', 'Acoso o intimidación'],
]

/** Motivos que tienen sentido para cada tipo de contenido. */
export const MOTIVOS_POR_TIPO: Record<TipoContenido, [MotivoReporte, string][]> = {
  post: [...COMUNES, ['falso', 'Carrera falsa o trampa'], ['otro', 'Otro motivo']],
  comentario: [...COMUNES, ['otro', 'Otro motivo']],
  usuario: [
    ...COMUNES,
    ['suplantacion', 'Se hace pasar por otra persona'],
    ['falso', 'Cuenta falsa o trampas'],
    ['otro', 'Otro motivo'],
  ],
  evento: [
    ['falso', 'Evento falso o estafa'],
    ['peligroso', 'Pone en riesgo a los corredores'],
    ['spam', 'Spam o publicidad'],
    ['ofensivo', 'Contenido ofensivo'],
    ['otro', 'Otro motivo'],
  ],
}

/** Compatibilidad: motivos de publicaciones. */
export const MOTIVOS_REPORTE = MOTIVOS_POR_TIPO.post

export const TITULO_REPORTE: Record<TipoContenido, string> = {
  post: 'Reportar publicación',
  comentario: 'Reportar comentario',
  usuario: 'Reportar corredor',
  evento: 'Reportar evento',
}

export interface DefinicionTipo {
  etiqueta: string
  titulo: string
  motivos: [MotivoReporte, string][]
  /** Accion principal del moderador; null si el tipo no se puede ocultar. */
  accionOcultar: string | null
  /** true si se puede suspender al autor del contenido. */
  tieneAutor: boolean
}

const BASE: Record<TipoContenido, DefinicionTipo> = {
  post: {
    etiqueta: 'Publicación',
    titulo: TITULO_REPORTE.post,
    motivos: MOTIVOS_POR_TIPO.post,
    accionOcultar: 'Ocultar publicación',
    tieneAutor: true,
  },
  comentario: {
    etiqueta: 'Comentario',
    titulo: TITULO_REPORTE.comentario,
    motivos: MOTIVOS_POR_TIPO.comentario,
    accionOcultar: 'Ocultar comentario',
    tieneAutor: true,
  },
  evento: {
    etiqueta: 'Evento',
    titulo: TITULO_REPORTE.evento,
    motivos: MOTIVOS_POR_TIPO.evento,
    accionOcultar: 'Ocultar evento',
    tieneAutor: true,
  },
  usuario: {
    etiqueta: 'Usuario',
    titulo: TITULO_REPORTE.usuario,
    motivos: MOTIVOS_POR_TIPO.usuario,
    accionOcultar: 'Suspender usuario',
    tieneAutor: false,
  },
}
const registro = new Map<string, DefinicionTipo>(Object.entries(BASE))

/** Punto de extension para otros contextos. */
export function registrarTipoObjetivo(clave: string, def: DefinicionTipo) {
  registro.set(clave, def)
}

/** Definicion del tipo; los tipos desconocidos tienen una generica para no romper la cola. */
export function definicionTipo(tipo: TipoObjetivo): DefinicionTipo {
  return (
    registro.get(tipo) ?? {
      etiqueta: tipo.charAt(0).toUpperCase() + tipo.slice(1).replace(/_/g, ' '),
      titulo: 'Reportar contenido',
      motivos: [...COMUNES, ['otro', 'Otro motivo']],
      accionOcultar: 'Ocultar contenido',
      tieneAutor: false,
    }
  )
}

export const tiposRegistrados = (): TipoObjetivo[] => [...registro.keys()]

/** Clave local para recordar que ya se reporto; las publicaciones conservan la clave antigua. */
export const claveObjetivo = (o: ObjetivoDenuncia) =>
  o.tipo === 'post' ? o.id : `${o.tipo}:${o.id}`
