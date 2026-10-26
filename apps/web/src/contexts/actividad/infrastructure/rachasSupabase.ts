import type { SupabaseClient } from '@supabase/supabase-js'
import { type RachasRepository } from '../../../data/repositorios'
import type { Racha } from '../../../shared/kernel/rachas'
import { usuario } from '../../../shared/supabase/sesion'

/* Rachas calculadas en Supabase (contexto Actividad). */

type Fila = Record<string, unknown>

export function rachaDesdeFila(f: Fila): Racha {
  const fecha = f.last_active_date as string | null
  let ultimoDiaMs: number | null = null
  if (fecha) {
    const [a, m, d] = fecha.split('-').map(Number)
    ultimoDiaMs = new Date(a, m - 1, d).getTime()
  }
  return {
    diaria: Number(f.current_daily ?? 0),
    mejorDiaria: Number(f.best_daily ?? 0),
    ultimoDiaMs,
    semanal: Number(f.current_weekly ?? 0),
    mejorSemanal: Number(f.best_weekly ?? 0),
    metaSemanal: Number(f.weekly_goal_runs ?? 3),
    carrerasSemana: Number(f.runs_this_week ?? 0),
  }
}

export class SupabaseRachasRepository implements RachasRepository {
  private sb: SupabaseClient
  constructor(sb: SupabaseClient) {
    this.sb = sb
  }
  async leer() {
    const user = await usuario(this.sb)
    if (!user) return null
    const { data, error } = await this.sb.rpc('mi_racha')
    if (error || !data) return null
    const fila = (Array.isArray(data) ? data[0] : data) as Fila | undefined
    return fila ? rachaDesdeFila(fila) : null
  }
}
