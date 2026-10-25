import { obtenerSupabase } from '../../shared/supabase/client'

export type MotivoReporte = 'spam' | 'ofensivo' | 'acoso' | 'falso' | 'otro'

export const MOTIVOS_REPORTE: [MotivoReporte, string][] = [
  ['spam', 'Spam o publicidad'],
  ['ofensivo', 'Contenido ofensivo'],
  ['acoso', 'Acoso o intimidación'],
  ['falso', 'Carrera falsa o trampa'],
  ['otro', 'Otro motivo'],
]

const CLAVE = 'gacelas:reportes'

export const yaReportada = (postId: string) =>
  (JSON.parse(localStorage.getItem(CLAVE) ?? '[]') as string[]).includes(postId)

/** Inserta el reporte en post_reports. Devuelve false si no hay conexion o sesion. */
export async function reportarPublicacion(
  postId: string,
  motivo: MotivoReporte,
  detalle: string
): Promise<boolean> {
  const sb = obtenerSupabase()
  if (!sb) return false
  const { data } = await sb.auth.getUser()
  if (!data.user) return false
  const { error } = await sb
    .from('post_reports')
    .insert({ post_id: postId, reporter_id: data.user.id, reason: motivo, detail: detalle.trim() })
  // 23505: ya lo habia reportado; para la persona cuenta como enviado.
  if (error && error.code !== '23505') return false
  const prev = JSON.parse(localStorage.getItem(CLAVE) ?? '[]') as string[]
  localStorage.setItem(CLAVE, JSON.stringify([...prev, postId]))
  return true
}
