import { obtenerSupabase } from '../../../shared/supabase/client'
import type { DenunciaRepositorio } from '../application/puertos'

/**
 * Las publicaciones siguen yendo a post_reports (un trigger las copia a content_reports) para
 * no romper apps publicadas; cualquier otro tipo registrado va directo a content_reports.
 */
export function denunciasSupabase(): DenunciaRepositorio | null {
  const sb = obtenerSupabase()
  if (!sb) return null
  return {
    async enviar(o, motivo, detalle) {
      const { data } = await sb.auth.getUser()
      if (!data.user) return 'sin_sesion'
      const { error } =
        o.tipo === 'post'
          ? await sb
              .from('post_reports')
              .insert({ post_id: o.id, reporter_id: data.user.id, reason: motivo, detail: detalle })
          : await sb.from('content_reports').insert({
              target_type: o.tipo,
              target_id: o.id,
              reporter_id: data.user.id,
              reason: motivo,
              detail: detalle,
            })
      // 23505: ya lo habia reportado.
      if (error) return error.code === '23505' ? 'duplicado' : 'error'
      return 'enviado'
    },
  }
}
