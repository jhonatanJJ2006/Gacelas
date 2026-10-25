import { obtenerSupabase } from '../../shared/supabase/client'

export type MotivoReporte =
  'spam' | 'ofensivo' | 'acoso' | 'falso' | 'suplantacion' | 'peligroso' | 'otro'

/** Que se reporta. Coincide con content_reports.target_type (migracion 023). */
export type TipoContenido = 'post' | 'usuario' | 'comentario' | 'evento'

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

const CLAVE = 'gacelas:reportes'
const clave = (tipo: TipoContenido, id: string) => (tipo === 'post' ? id : `${tipo}:${id}`)
const leidos = () => JSON.parse(localStorage.getItem(CLAVE) ?? '[]') as string[]

export const yaReportado = (tipo: TipoContenido, id: string) => leidos().includes(clave(tipo, id))
export const yaReportada = (postId: string) => yaReportado('post', postId)

/**
 * Envia un reporte. Las publicaciones siguen yendo a post_reports (un trigger las copia a
 * content_reports); el resto va directo a content_reports. Devuelve false sin conexion o sesion.
 */
export async function reportarContenido(
  tipo: TipoContenido,
  id: string,
  motivo: MotivoReporte,
  detalle: string
): Promise<boolean> {
  const sb = obtenerSupabase()
  if (!sb) return false
  const { data } = await sb.auth.getUser()
  if (!data.user) return false
  const { error } =
    tipo === 'post'
      ? await sb.from('post_reports').insert({
          post_id: id,
          reporter_id: data.user.id,
          reason: motivo,
          detail: detalle.trim(),
        })
      : await sb.from('content_reports').insert({
          target_type: tipo,
          target_id: id,
          reporter_id: data.user.id,
          reason: motivo,
          detail: detalle.trim(),
        })
  // 23505: ya lo habia reportado; para la persona cuenta como enviado.
  if (error && error.code !== '23505') return false
  localStorage.setItem(CLAVE, JSON.stringify([...leidos(), clave(tipo, id)]))
  return true
}

export const reportarPublicacion = (postId: string, motivo: MotivoReporte, detalle: string) =>
  reportarContenido('post', postId, motivo, detalle)
