import { useEffect, useState } from 'react'
import type { Ruta } from '../../mocks/data'
import type { CarreraGuardada } from '../../shared/kernel/tipos'
import {
  elegirRutaSugerida,
  guardarCacheSugerida,
  leerCacheSugerida,
  rutaSugeridaRemota,
  type RespuestaSugerida,
} from '../../contexts/rutas/api'
import { esVistaPrevia } from '../../contexts/seguridad/application/incidentes'

/**
 * Ruta sugerida para hoy: la decide Supabase (ruta_sugerida_hoy) con las reglas de
 * docs/RUTA-SUGERIDA.md; sin red se usa la misma regla local con la ultima lista de rutas seguras.
 */
export function useRutaSugerida(
  rutas: Ruta[],
  carreras: CarreraGuardada[]
): ResultadoSugerida | null {
  const [remota, setRemota] = useState<RespuestaSugerida | null | undefined>(undefined)
  useEffect(() => {
    let vivo = true
    const pedir = (u?: { lat: number; lng: number } | null) =>
      rutaSugeridaRemota(u)
        .catch(() => null)
        .then((r) => {
          if (!vivo) return
          if (r) guardarCacheSugerida(r)
          setRemota(r)
        })
    // Solo usa la ubicacion si ya hay permiso; nunca pide permiso desde Inicio.
    const permisos = typeof navigator !== 'undefined' ? navigator.permissions : undefined
    if (permisos?.query && navigator.geolocation) {
      permisos
        .query({ name: 'geolocation' as PermissionName })
        .then((p) => {
          if (p.state !== 'granted') return pedir(null)
          navigator.geolocation.getCurrentPosition(
            (pos) => pedir({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
            () => pedir(null),
            { maximumAge: 600_000, timeout: 4000 }
          )
        })
        .catch(() => pedir(null))
    } else pedir(null)
    return () => {
      vivo = false
    }
  }, [])
  if (remota === undefined) return null
  if (remota) {
    if (remota.estado === 'sin_ruta') return { estado: 'sin_ruta', motivo: remota.motivo ?? '' }
    const ruta = rutas.find((r) => r.id === remota.rutaId)
    if (ruta) return { estado: 'ok', ruta, nivel: remota.nivel }
  }
  const cache = leerCacheSugerida()
  // Vista previa de desarrollo (sin Supabase): todas las oficiales cuentan como seguras.
  if (esVistaPrevia()) cache.seguras = rutas.filter((r) => r.oficial).map((r) => r.id)
  const local = elegirRutaSugerida({
    rutas,
    carreras,
    seguras: new Set(cache.seguras),
    historial: cache.historial,
    ahoraMs: Date.now(),
  })
  if (local.estado === 'sin_ruta' && cache.seguras.length === 0) {
    return { estado: 'sin_ruta', motivo: 'Conéctate para ver la ruta segura de hoy.' }
  }
  return local.estado === 'ok'
    ? { estado: 'ok', ruta: local.ruta, offline: true }
    : { estado: 'sin_ruta', motivo: local.motivo }
}

export type ResultadoSugerida =
  | { estado: 'ok'; ruta: Ruta; nivel?: string; offline?: boolean }
  | { estado: 'sin_ruta'; motivo: string }
