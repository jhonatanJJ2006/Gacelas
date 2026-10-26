import type { ClienteSupabase } from '../../../shared/supabase'
import type { IncidentePublico, SeguridadZona } from '../domain/indice'
import type { IncidenteRepositorio, IndiceRepositorio } from '../domain/puertos'

/** Indice por ruta con la RPC seguridad_zona_ruta. */
export function indiceSupabase(sb: ClienteSupabase): IndiceRepositorio {
  return {
    async zonaDeRuta(rutaId) {
      const { data, error } = await sb.rpc('seguridad_zona_ruta', { p_ruta_id: rutaId })
      if (error) throw new Error(error.message)
      if (!data) return null
      const d = data as {
        dias?: SeguridadZona['dias']
        tipos?: SeguridadZona['tipos']
        actualizado_hasta?: string | null
        fuente?: string
      }
      return {
        dias: d.dias ?? [],
        tipos: d.tipos ?? [],
        actualizadoHasta: d.actualizado_hasta ?? null,
        fuente: d.fuente ?? 'SIS ECU 911 (CC-BY)',
      }
    },
  }
}

interface FilaIncidente {
  id: string
  tipo: string
  lat: number
  lng: number
  ocurrido_hora: string
  confirmaciones: number | null
  estado: string | null
}

/** Incidentes reportados por corredores, desde la vista anonima incidentes_publicos. */
export function incidentesSupabase(sb: ClienteSupabase): IncidenteRepositorio {
  return {
    async recientes(limite) {
      const { data, error } = await sb
        .from('incidentes_publicos')
        .select('id,tipo,lat,lng,ocurrido_hora,confirmaciones,estado')
        .order('ocurrido_hora', { ascending: false })
        .limit(limite)
      if (error) throw new Error(error.message)
      return ((data ?? []) as FilaIncidente[]).map((r): IncidentePublico => ({
        id: r.id,
        tipo: r.tipo,
        lat: r.lat,
        lng: r.lng,
        ocurridoMs: new Date(r.ocurrido_hora).getTime(),
        confirmaciones: r.confirmaciones ?? 0,
        verificado: r.estado === 'verificado',
      }))
    },
  }
}
