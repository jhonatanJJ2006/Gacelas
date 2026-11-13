import { useEffect, useMemo, useState } from 'react'
import { create } from 'zustand'
import { useComunidad } from '../contexts/comunidad/api'
import { listarCorredores, type CorredorEnMapa } from '../contexts/envivo/api'
import { filtrarEventos, unirEventos, useEventos } from '../contexts/eventos/api'
import { listarIncidentesRecientes, type IncidentePublico } from '../contexts/seguridad'
import { esVistaPrevia } from '../contexts/seguridad/application/incidentes'
import { eventosMock, type Evento } from '../mocks/data'
import { obtenerSupabase } from '../shared/supabase/client'

/*
 * Datos compartidos por la barra lateral, la barra superior e Inicio del rediseno web
 * (docs/REDISENO-WEB.md). Todo sale de Supabase; sin backend (pruebas, vista previa) se usan
 * los ejemplos locales, que nunca se muestran en produccion con sesion.
 */

/** Eventos visibles: los de Supabase con sesion; los de ejemplo solo sin backend. */
export function useEventosVisibles(): { todos: Evento[]; proximos: Evento[] } {
  const creados = useEventos((s) => s.eventosCreados)
  const inscritos = useEventos((s) => s.inscritos)
  const feedSocial = useComunidad((s) => s.feedSocial)
  return useMemo(() => {
    // En la vista previa de desarrollo tambien se ven los de ejemplo.
    const todos = feedSocial && !esVistaPrevia() ? creados : unirEventos(creados, eventosMock)
    const proximos = filtrarEventos(todos, 'Próximos', inscritos)
      .filter((e) => e.inicioMs == null || e.inicioMs > Date.now() - 3 * 3_600_000)
      .sort((a, b) => (a.inicioMs ?? Infinity) - (b.inicioMs ?? Infinity))
    return { todos, proximos }
  }, [creados, inscritos, feedSocial])
}

interface EstadoEnVivo {
  corredores: CorredorEnMapa[]
  cargando: boolean
  refrescar(): Promise<void>
}

let ultimaConsultaEnVivo = 0

/** Corredores compartiendo su posicion ahora (runner_positions, caducan a los 2 min). */
export const useEnVivo = create<EstadoEnVivo>((set) => ({
  corredores: [],
  cargando: true,
  async refrescar() {
    // Barra lateral e Inicio comparten el sondeo: como mucho una consulta cada 20 s.
    if (Date.now() - ultimaConsultaEnVivo < 20_000) return
    ultimaConsultaEnVivo = Date.now()
    const corredores = await listarCorredores().catch(() => [])
    set({ corredores, cargando: false })
  },
}))

/** Refresca "En vivo" al montar y cada minuto mientras la pestana este visible. */
export function useSondeoEnVivo() {
  const refrescar = useEnVivo((s) => s.refrescar)
  useEffect(() => {
    void refrescar()
    const id = window.setInterval(() => {
      if (document.visibilityState === 'visible') void refrescar()
    }, 60_000)
    return () => window.clearInterval(id)
  }, [refrescar])
}

export interface ResumenSeguridad {
  /** Mes de los datos agregados del ECU 911 (yyyy-mm-01) o null si no hay datos cargados. */
  mesEcu: string | null
  ecu: { servicio: string; conteo: number }[]
  reportes7d: number
}

export type EstadoSeguridadZona =
  | { cargando: true }
  | { cargando: false; incidentes: IncidentePublico[]; resumen: ResumenSeguridad | null }

/** Reportes de corredores de los ultimos dias y agregado ECU 911 (RPC resumen_seguridad_zona). */
export function useSeguridadZona(): EstadoSeguridadZona {
  const [estado, setEstado] = useState<EstadoSeguridadZona>({ cargando: true })
  useEffect(() => {
    let vivo = true
    const sb = esVistaPrevia() ? null : obtenerSupabase()
    const resumen: Promise<ResumenSeguridad | null> = sb
      ? Promise.resolve(sb.rpc('resumen_seguridad_zona'))
          .then(({ data }) =>
            data
              ? {
                  mesEcu: (data.mes_ecu as string | null) ?? null,
                  ecu: ((data.ecu as { servicio: string; conteo: number }[]) ?? []).map((e) => ({
                    servicio: e.servicio,
                    conteo: Number(e.conteo),
                  })),
                  reportes7d: Number(data.reportes_7d ?? 0),
                }
              : null
          )
          .catch(() => null)
      : Promise.resolve(null)
    void Promise.all([listarIncidentesRecientes().catch(() => []), resumen]).then(
      ([incidentes, r]) => {
        if (vivo) setEstado({ cargando: false, incidentes, resumen: r })
      }
    )
    return () => {
      vivo = false
    }
  }, [])
  return estado
}

export interface SeguridadRuta {
  nivel: 'bajo' | 'medio' | 'alto' | string
  indice: number
  reportes7d: number
  reportes30d: number
  /** 0-100 solo con datos ECU 911 cargados; null en otro caso (no se inventa). */
  puntaje: number | null
}

interface EstadoSeguridadRutas {
  porRuta: Map<string, SeguridadRuta> | null
  pedido: boolean
  cargar(): Promise<void>
}

/** Seguridad de todas las rutas publicas para la hora actual (RPC seguridad_rutas, migracion 040). */
export const useSeguridadRutas = create<EstadoSeguridadRutas>((set, get) => ({
  porRuta: null,
  pedido: false,
  async cargar() {
    if (get().pedido) return
    set({ pedido: true })
    const sb = esVistaPrevia() ? null : obtenerSupabase()
    if (!sb) return set({ porRuta: new Map() })
    try {
      const { data } = await sb.rpc('seguridad_rutas')
      const mapa = new Map<string, SeguridadRuta>()
      for (const f of (data ?? []) as Record<string, unknown>[])
        mapa.set(String(f.route_id), {
          nivel: String(f.nivel ?? 'medio'),
          indice: Number(f.indice ?? 1),
          reportes7d: Number(f.reportes_7d ?? 0),
          reportes30d: Number(f.reportes_30d ?? 0),
          puntaje: f.puntaje == null ? null : Number(f.puntaje),
        })
      set({ porRuta: mapa })
    } catch {
      set({ porRuta: new Map(), pedido: false })
    }
  },
}))

/** Carga (una vez) y devuelve la seguridad por ruta; null mientras carga. */
export function useSeguridadDeRutas() {
  const porRuta = useSeguridadRutas((s) => s.porRuta)
  const cargar = useSeguridadRutas((s) => s.cargar)
  useEffect(() => {
    void cargar()
  }, [cargar])
  return porRuta
}

/** Texto honesto del nivel de seguridad. */
export const textoNivelSeguridad = (n: string) =>
  n === 'bajo' ? 'Riesgo bajo' : n === 'alto' ? 'Riesgo alto' : 'Riesgo medio'
