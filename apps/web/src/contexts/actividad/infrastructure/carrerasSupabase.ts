import type { SupabaseClient, User } from '@supabase/supabase-js'
import {
  escribir,
  leer,
  LocalCarrerasRepository,
  type CarrerasRepository,
} from '../../../data/repositorios'
import type { CarreraGuardada } from '../../../data/tipos'

/**
 * Adaptador Supabase del repositorio de carreras (tablas activities y activity_tracks) con
 * cola offline en el almacenamiento local: lo que no se puede subir queda pendiente.
 */

type Fila = Record<string, unknown>

async function usuario(sb: SupabaseClient): Promise<User | null> {
  const { data } = await sb.auth.getSession()
  return data.session?.user ?? null
}

export type OperacionPendiente =
  { tipo: 'guardar'; carrera: CarreraGuardada } | { tipo: 'eliminar'; id: string }

const CLAVE_COLA = 'carreras-pendientes'

export const filaDesdeCarrera = (c: CarreraGuardada, userId: string) => ({
  user_id: userId,
  client_id: c.id,
  title: c.titulo,
  description: c.nota,
  is_public: c.publica,
  start_time: new Date(c.inicioMs).toISOString(),
  end_time: new Date(c.inicioMs + c.segundos * 1000).toISOString(),
  duration_seconds: Math.round(c.segundos),
  distance_meters: Math.round(c.distanciaKm * 1000),
  avg_pace_seconds_per_km: c.distanciaKm > 0 ? Math.round(c.segundos / c.distanciaKm) : null,
  elevation_gain_meters: Math.round(c.desnivelM),
  splits: c.parciales,
})

export function carreraDesdeFila(fila: Fila, puntos: CarreraGuardada['puntos']): CarreraGuardada {
  return {
    id: (fila.client_id as string) || (fila.id as string),
    titulo: fila.title as string,
    nota: (fila.description as string) ?? '',
    publica: Boolean(fila.is_public),
    inicioMs: Date.parse(fila.start_time as string),
    segundos: Number(fila.duration_seconds ?? 0),
    distanciaKm: Number(fila.distance_meters ?? 0) / 1000,
    desnivelM: Number(fila.elevation_gain_meters ?? 0),
    parciales: (fila.splits as CarreraGuardada['parciales']) ?? [],
    puntos,
  }
}

export class SupabaseCarrerasRepository implements CarrerasRepository {
  private local = new LocalCarrerasRepository()
  private sb: SupabaseClient
  constructor(sb: SupabaseClient) {
    this.sb = sb
  }

  cola = () => leer<OperacionPendiente[]>(CLAVE_COLA, [])

  private async subir(op: OperacionPendiente, userId: string) {
    if (op.tipo === 'eliminar') {
      const { error } = await this.sb
        .from('activities')
        .delete()
        .eq('user_id', userId)
        .eq('client_id', op.id)
      if (error) throw error
      return
    }
    const { data, error } = await this.sb
      .from('activities')
      .upsert(filaDesdeCarrera(op.carrera, userId), { onConflict: 'user_id,client_id' })
      .select('id')
      .single()
    if (error || !data) throw error ?? new Error('sin id')
    const { error: e2 } = await this.sb
      .from('activity_tracks')
      .upsert(
        { activity_id: (data as Fila).id, user_id: userId, points: op.carrera.puntos },
        { onConflict: 'activity_id' }
      )
    if (e2) throw e2
  }

  /** Sube lo pendiente en orden; lo que falle queda en la cola. Devuelve cuantas quedan. */
  async sincronizar(): Promise<number> {
    const user = await usuario(this.sb)
    const cola = this.cola()
    if (!user || cola.length === 0) return cola.length
    const restantes: OperacionPendiente[] = []
    for (const op of cola) {
      if (restantes.length) {
        restantes.push(op)
        continue
      }
      try {
        await this.subir(op, user.id)
      } catch {
        restantes.push(op)
      }
    }
    escribir(CLAVE_COLA, restantes)
    return restantes.length
  }

  async listar() {
    const user = await usuario(this.sb)
    if (!user) return this.local.listar()
    await this.sincronizar()
    try {
      const [act, tracks] = await Promise.all([
        this.sb
          .from('activities')
          .select('*')
          .eq('user_id', user.id)
          .order('start_time', { ascending: false }),
        this.sb.from('activity_tracks').select('activity_id,points').eq('user_id', user.id),
      ])
      if (act.error || tracks.error) throw act.error ?? tracks.error
      const puntos = new Map(
        ((tracks.data ?? []) as Fila[]).map((t) => [
          t.activity_id,
          t.points as CarreraGuardada['puntos'],
        ])
      )
      const remotas = ((act.data ?? []) as Fila[]).map((f) =>
        carreraDesdeFila(f, puntos.get(f.id) ?? [])
      )
      // Las carreras aun no subidas se muestran igual.
      const pendientes = this.cola().flatMap((o) => (o.tipo === 'guardar' ? [o.carrera] : []))
      const borradas = new Set(this.cola().flatMap((o) => (o.tipo === 'eliminar' ? [o.id] : [])))
      const todas = [
        ...pendientes,
        ...remotas.filter((r) => !pendientes.some((p) => p.id === r.id)),
      ]
        .filter((c) => !borradas.has(c.id))
        .sort((a, b) => b.inicioMs - a.inicioMs)
      return escribir('carreras', todas)
    } catch {
      return this.local.listar()
    }
  }

  async guardar(c: CarreraGuardada) {
    const lista = await this.local.guardar(c)
    escribir(CLAVE_COLA, [
      ...this.cola().filter((o) => !(o.tipo === 'guardar' && o.carrera.id === c.id)),
      { tipo: 'guardar', carrera: c },
    ])
    await this.sincronizar()
    return lista
  }

  async eliminar(id: string) {
    const lista = await this.local.eliminar(id)
    escribir(CLAVE_COLA, [
      ...this.cola().filter((o) => !(o.tipo === 'guardar' && o.carrera.id === id)),
      { tipo: 'eliminar', id },
    ])
    await this.sincronizar()
    return lista
  }
}

/* ---------------- Rutas guardadas ---------------- */
