import { obtenerSupabase } from '../../../shared/supabase/client'
import type { Sugerencia } from '../domain/sugerida'

/* Adaptador de ruta_sugerida_hoy (migracion 035) y cache local para el modo sin red. */

export interface RespuestaSugerida {
  estado: 'ok' | 'sin_ruta'
  dia: string
  rutaId?: string
  nombre?: string
  nivel?: string
  objetivoKm?: number
  motivo?: string
  seguras?: string[]
}

const CLAVE = 'runcity.rutaSugerida'

interface Cache {
  historial: Sugerencia[]
  seguras: string[]
  ultima?: RespuestaSugerida
}

export function leerCacheSugerida(): Cache {
  try {
    const c = JSON.parse(localStorage.getItem(CLAVE) ?? '') as Cache
    return { historial: c.historial ?? [], seguras: c.seguras ?? [], ultima: c.ultima }
  } catch {
    return { historial: [], seguras: [] }
  }
}

export function guardarCacheSugerida(r: RespuestaSugerida) {
  const c = leerCacheSugerida()
  const historial =
    r.estado === 'ok' && r.rutaId
      ? [...c.historial.filter((s) => s.dia !== r.dia), { dia: r.dia, rutaId: r.rutaId }].slice(-14)
      : c.historial
  try {
    localStorage.setItem(
      CLAVE,
      JSON.stringify({ historial, seguras: r.seguras ?? c.seguras, ultima: r } satisfies Cache)
    )
  } catch {
    /* almacenamiento lleno o bloqueado */
  }
}

export async function rutaSugeridaRemota(
  ubicacion?: { lat: number; lng: number } | null
): Promise<RespuestaSugerida | null> {
  const sb = obtenerSupabase()
  if (!sb) return null
  const { data, error } = await sb.rpc('ruta_sugerida_hoy', {
    p_lat: ubicacion?.lat ?? null,
    p_lng: ubicacion?.lng ?? null,
  })
  if (error || !data) return null
  const d = data as {
    estado: 'ok' | 'sin_ruta'
    dia: string
    ruta?: { id: string; nombre: string }
    nivel?: string
    objetivo_km?: number
    motivo?: string
    seguras?: string[]
  }
  return {
    estado: d.estado,
    dia: d.dia,
    rutaId: d.ruta?.id,
    nombre: d.ruta?.nombre,
    nivel: d.nivel,
    objetivoKm: d.objetivo_km,
    motivo: d.motivo,
    seguras: d.seguras,
  }
}
