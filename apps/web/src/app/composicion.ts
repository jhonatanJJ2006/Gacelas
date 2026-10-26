import {
  LocalCarrerasRepository,
  SupabaseCarrerasRepository,
  SupabaseRachasRepository,
  type CarrerasRepository,
  type RachasRepository,
} from '../contexts/actividad/api'
import {
  SupabaseNotificacionesRepository,
  type NotificacionesRepository,
} from '../contexts/avisos/api'
import { SupabaseComunidadRepository, type ComunidadRepository } from '../contexts/comunidad/api'
import {
  LocalEventosRepository,
  SupabaseEventosRepository,
  type EventosRepository,
} from '../contexts/eventos/api'
import {
  LocalAjustesRepository,
  LocalAuthRepository,
  SupabaseAjustesRepository,
  SupabaseAuthRepository,
  type AjustesRepository,
  type AuthRepository,
} from '../contexts/identidad/api'
import {
  LocalRutasRepository,
  SupabaseExplorarRepository,
  SupabaseRutasRepository,
  type ExplorarRepository,
  type RutasRepository,
} from '../contexts/rutas/api'
import { obtenerSupabase } from '../shared/supabase/client'

/*
 * Raiz de composicion: elige la implementacion de cada repositorio. La app solo conoce los
 * puertos de cada contexto; hoy se implementan con localStorage o con Supabase.
 */

type ClienteSupabase = NonNullable<ReturnType<typeof obtenerSupabase>>

export interface Repositorios {
  auth: AuthRepository
  carreras: CarrerasRepository
  rutas: RutasRepository
  ajustes: AjustesRepository
  eventos: EventosRepository
  /** Notificaciones reales; sin este repositorio se usan las de ejemplo. */
  notificaciones?: NotificacionesRepository
  comunidad?: ComunidadRepository
  rachas?: RachasRepository
  /** Rutas de la comunidad (Fase 4); sin este repositorio se usan las de ejemplo. */
  explorar?: ExplorarRepository
}

export const repositoriosLocales = (): Repositorios => ({
  auth: new LocalAuthRepository(),
  carreras: new LocalCarrerasRepository(),
  rutas: new LocalRutasRepository(),
  ajustes: new LocalAjustesRepository(),
  eventos: new LocalEventosRepository(),
})

export const repositoriosSupabase = (sb: ClienteSupabase): Repositorios => ({
  auth: new SupabaseAuthRepository(sb),
  carreras: new SupabaseCarrerasRepository(sb),
  rutas: new SupabaseRutasRepository(sb),
  ajustes: new SupabaseAjustesRepository(sb),
  eventos: new SupabaseEventosRepository(sb),
  notificaciones: new SupabaseNotificacionesRepository(sb),
  comunidad: new SupabaseComunidadRepository(sb),
  rachas: new SupabaseRachasRepository(sb),
  explorar: new SupabaseExplorarRepository(sb),
})

/** Con Supabase configurado se usa el servidor; si no, todo queda en el dispositivo. */
export function repositoriosPorDefecto(): Repositorios {
  const sb = obtenerSupabase()
  return sb ? repositoriosSupabase(sb) : repositoriosLocales()
}
