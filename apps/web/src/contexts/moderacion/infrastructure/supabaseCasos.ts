import { obtenerSupabase } from '../../../shared/supabase/client'
import type { CasoRepositorio } from '../application/puertos'
import type { EstadoReporte, ReporteAdmin } from '../domain/caso'

function ok<T>(r: { data: T | null; error: { message: string } | null }): T {
  if (r.error) throw new Error(r.error.message)
  return r.data as T
}

/** Cola generica (admin_cola_reportes y moderar_reporte, migracion 023). */
export function casosSupabase(): CasoRepositorio | null {
  const sb = obtenerSupabase()
  if (!sb) return null
  return {
    async pendientes() {
      const r = await sb.rpc('admin_cola_reportes')
      if (!r.error) return r.data as ReporteAdmin[]
      // Sin la migracion 023 se usa la cola anterior de publicaciones.
      const viejos = ok(
        await sb
          .from('post_reports')
          .select('*')
          .order('created_at', { ascending: false })
          .limit(200)
      ) as {
        id: string
        post_id: string
        reason: string
        detail: string
        status: string
        created_at: string
      }[]
      return viejos.map((v) => ({
        ...v,
        target_type: 'post' as const,
        target_id: v.post_id,
        status: v.status as EstadoReporte,
        reportante: '',
        resumen: `Publicación ${v.post_id}`,
        total_reportes: 1,
        acciones: [],
      }))
    },
    async moderar(r, accion, nota) {
      ok(await sb.rpc('moderar_reporte', { reporte: r.id, accion, nota }))
    },
  }
}
