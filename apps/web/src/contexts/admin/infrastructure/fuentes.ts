import { obtenerSupabase } from '../../../shared/supabase/client'

/*
 * Origen de datos del panel de administracion. Los casos de moderacion y las estadisticas
 * vienen de sus contextos; se reciben como parametros para no depender de su codigo.
 */

/** Casos de moderacion que el panel lista y resuelve. */
export interface CasosAdmin<R, A> {
  pendientes(): Promise<R[]>
  moderar(r: R, accion: A, nota: string): Promise<void>
  cambiarPrioridad(r: R, prioridad: 'alta' | 'media' | 'baja'): Promise<void>
}

/** Estadisticas de uso entre dos fechas. */
export interface AnaliticaAdmin<E> {
  estadisticas(desde: string, hasta: string): Promise<E>
}

export interface Metricas {
  usuarios: number
  usuarios_7d: number
  carreras_7d: number
  km_7d: number
  reportes_pendientes: number
  eventos_proximos: number
  clubes: number
  rutas: number
}
export interface UsuarioAdmin {
  id: string
  username: string
  full_name: string | null
  created_at: string
  suspended_at: string | null
  role: 'admin' | 'moderador' | null
  carreras: number
  km: number
}
export interface EventoAdmin {
  id: string
  title: string
  description: string
  starts_at: string | null
  location: string
  distance_km: number
  published: boolean
}
export interface PuntoGeo {
  lat: number
  lng: number
}
export interface RutaAdmin {
  id: string
  name: string
  distance_km: number
  difficulty: string
  elevation_m: number
  is_official: boolean
  /** Trazado de la ruta (puede venir vacio). */
  path: PuntoGeo[]
}
/** Lo que el escenario del Resumen dibuja: corredores visibles y incidentes sin verificar. */
export interface EscenarioVivo {
  corredores: PuntoGeo[]
  incidentes: PuntoGeo[]
}
/** Origen del escenario en vivo; lo aportan los contextos En vivo y Seguridad de rutas. */
export interface VivoAdmin {
  escenario(): Promise<EscenarioVivo>
}
/** Carga del ECU 911 de un mes: filas agregadas en incidentes_agregados. */
export interface ImportacionEcu {
  anio: number
  mes: number
  agrupaciones: number
}
/** Registro de una carga del ECU 911 (importaciones_ecu911): filas leidas y errores. */
export interface RegistroEcu {
  periodo: string
  archivo: string
  filas: number
  filas_loja: number
  estado: 'ok' | 'error'
  mensaje: string | null
  fecha: string
}
/** Alerta SOS vista por el panel (admin_sos_alertas, 050). Posicion solo si esta activa. */
export interface AlertaSos {
  id: string
  nombre: string
  activa: boolean
  started_at: string
  ended_at: string | null
  last_at: string | null
  lat: number | null
  lng: number | null
  puntos: number
}
/** Ultimo acceso de una cuenta (admin_ultimo_acceso, 050). */
export interface AccesoUsuario {
  id: string
  ultimo_acceso: string | null
  plataforma: string | null
  version_name: string | null
}
export interface VersionPublicada {
  plataforma: string
  version_name: string
  version_code: number
  canal: 'estable' | 'prerelease'
  publicada_en: string
  apk_bytes: number | null
}
export interface VersionEnUso {
  plataforma: string
  version_name: string
  version_code: number | null
  usuarios: number
}
export interface RespuestaRapida {
  id: string | null
  titulo: string
  texto: string
}
export interface ClubAdmin {
  id: string
  name: string
  description: string
}
export interface AjusteApp {
  key: string
  value: unknown
}

/** Origen de datos del panel. En produccion es Supabase; en desarrollo hay datos de ejemplo. */
export interface FuenteAdmin<R, A, E> {
  demo: boolean
  esAdmin(): Promise<boolean>
  metricas(): Promise<Metricas>
  usuarios(): Promise<UsuarioAdmin[]>
  suspender(id: string, si: boolean): Promise<void>
  cambiarRol(id: string, rol: UsuarioAdmin['role']): Promise<void>
  reportes(): Promise<R[]>
  moderar(r: R, accion: A, nota: string): Promise<void>
  estadisticas(desde: string, hasta: string): Promise<E>
  eventos(): Promise<EventoAdmin[]>
  guardarEvento(e: Partial<EventoAdmin> & { title: string }): Promise<void>
  rutas(): Promise<RutaAdmin[]>
  enVivo(): Promise<EscenarioVivo | null>
  importacionesEcu(): Promise<ImportacionEcu[]>
  borrarRuta(id: string): Promise<void>
  clubes(): Promise<ClubAdmin[]>
  borrarClub(id: string): Promise<void>
  ajustes(): Promise<AjusteApp[]>
  guardarAjuste(key: string, value: unknown): Promise<void>
  versionTerminos(): Promise<string | null>
  publicarTerminos(version: string): Promise<void>
  // Migracion 050 (docs/ADMIN.md).
  cambiarPrioridad(r: R, prioridad: 'alta' | 'media' | 'baja'): Promise<void>
  sos(): Promise<AlertaSos[]>
  accesos(): Promise<AccesoUsuario[]>
  registroEcu(): Promise<RegistroEcu[]>
  versionesPublicadas(): Promise<VersionPublicada[]>
  versionesEnUso(): Promise<VersionEnUso[]>
  respuestasRapidas(): Promise<RespuestaRapida[]>
  guardarRespuestaRapida(r: RespuestaRapida): Promise<void>
  borrarRespuestaRapida(id: string): Promise<void>
}

function ok<T>(r: { data: T | null; error: { message: string } | null }): T {
  if (r.error) throw new Error(r.error.message)
  return r.data as T
}

/** Meses con datos del ECU 911, del mas reciente hacia atras (maximo cuatro). */
async function importacionesEcuSupabase(
  sb: NonNullable<ReturnType<typeof obtenerSupabase>>
): Promise<ImportacionEcu[]> {
  const ult = ok(
    await sb
      .from('incidentes_agregados')
      .select('anio,mes')
      .order('anio', { ascending: false })
      .order('mes', { ascending: false })
      .limit(1)
  ) as { anio: number; mes: number }[]
  if (!ult[0]) return []
  const meses: { anio: number; mes: number }[] = []
  let { anio, mes } = ult[0]
  for (let i = 0; i < 4; i++) {
    meses.push({ anio, mes })
    mes -= 1
    if (mes === 0) {
      mes = 12
      anio -= 1
    }
  }
  return Promise.all(
    meses.map(async (m) => {
      const r = await sb
        .from('incidentes_agregados')
        .select('id', { count: 'exact', head: true })
        .eq('anio', m.anio)
        .eq('mes', m.mes)
      if (r.error) throw new Error(r.error.message)
      return { ...m, agrupaciones: r.count ?? 0 }
    })
  )
}

export function fuenteSupabase<R, A, E>(
  casos: CasosAdmin<R, A> | null,
  analitica: AnaliticaAdmin<E> | null,
  vivo: VivoAdmin | null = null
): FuenteAdmin<R, A, E> | null {
  const sb = obtenerSupabase()
  if (!sb || !casos || !analitica) return null
  return {
    demo: false,
    async esAdmin() {
      const r = await sb.rpc('is_admin')
      return !r.error && r.data === true
    },
    async metricas() {
      return ok(await sb.rpc('admin_metricas')) as Metricas
    },
    async usuarios() {
      return ok(await sb.rpc('admin_usuarios')) as UsuarioAdmin[]
    },
    async suspender(id, si) {
      ok(await sb.rpc('admin_suspender', { uid: id, suspender: si }))
    },
    async cambiarRol(id, rol) {
      if (rol) ok(await sb.from('user_roles').upsert({ user_id: id, role: rol }))
      else ok(await sb.from('user_roles').delete().eq('user_id', id))
    },
    reportes: () => casos.pendientes(),
    moderar: (r, accion, nota) => casos.moderar(r, accion, nota),
    estadisticas: (desde, hasta) => analitica.estadisticas(desde, hasta),
    async eventos() {
      return ok(
        await sb
          .from('events')
          .select('id,title,description,starts_at,location,distance_km,published')
          .order('starts_at', { ascending: false })
      ) as EventoAdmin[]
    },
    async guardarEvento(e) {
      const fila = {
        title: e.title,
        description: e.description ?? '',
        starts_at: e.starts_at,
        location: e.location ?? '',
        distance_km: e.distance_km ?? 0,
        published: e.published ?? false,
        date_label: e.starts_at ? new Date(e.starts_at).toLocaleDateString('es-EC') : '',
        time_label: e.starts_at
          ? new Date(e.starts_at).toLocaleTimeString('es-EC', {
              hour: '2-digit',
              minute: '2-digit',
            })
          : '',
      }
      if (e.id) ok(await sb.from('events').update(fila).eq('id', e.id))
      else ok(await sb.from('events').insert(fila))
    },
    async rutas() {
      const filas = ok(
        await sb
          .from('routes')
          .select('id,name,distance_km,difficulty,elevation_m,is_official,path')
          .order('name')
      ) as (Omit<RutaAdmin, 'path'> & { path: unknown })[]
      return filas.map((f) => ({
        ...f,
        distance_km: Number(f.distance_km ?? 0),
        elevation_m: Number(f.elevation_m ?? 0),
        is_official: Boolean(f.is_official),
        path: Array.isArray(f.path) ? (f.path as PuntoGeo[]) : [],
      }))
    },
    enVivo: () => (vivo ? vivo.escenario() : Promise.resolve(null)),
    importacionesEcu: () => importacionesEcuSupabase(sb),
    async borrarRuta(id) {
      ok(await sb.from('routes').delete().eq('id', id))
    },
    async clubes() {
      return ok(await sb.from('clubs').select('id,name,description').order('name')) as ClubAdmin[]
    },
    async borrarClub(id) {
      ok(await sb.from('clubs').delete().eq('id', id))
    },
    async ajustes() {
      return ok(await sb.from('app_settings').select('key,value').order('key')) as AjusteApp[]
    },
    async guardarAjuste(key, value) {
      ok(await sb.from('app_settings').upsert({ key, value, updated_at: new Date().toISOString() }))
    },
    async versionTerminos() {
      const r = await sb
        .from('legal_versions')
        .select('version')
        .eq('kind', 'terminos')
        .order('published_at', { ascending: false })
        .limit(1)
      return (ok(r) as { version: string }[])[0]?.version ?? null
    },
    async publicarTerminos(version) {
      ok(await sb.from('legal_versions').insert({ kind: 'terminos', version }))
    },
    cambiarPrioridad: (r, p) => casos.cambiarPrioridad(r, p),
    async sos() {
      return ok(await sb.rpc('admin_sos_alertas', { horas: 24 })) as AlertaSos[]
    },
    async accesos() {
      return ok(await sb.rpc('admin_ultimo_acceso')) as AccesoUsuario[]
    },
    async registroEcu() {
      return ok(
        await sb
          .from('importaciones_ecu911')
          .select('periodo,archivo,filas,filas_loja,estado,mensaje,fecha')
          .order('fecha', { ascending: false })
          .limit(12)
      ) as RegistroEcu[]
    },
    async versionesPublicadas() {
      return ok(await sb.rpc('admin_versiones_publicadas')) as VersionPublicada[]
    },
    async versionesEnUso() {
      return ok(await sb.rpc('admin_versiones_en_uso', { dias: 30 })) as VersionEnUso[]
    },
    async respuestasRapidas() {
      return ok(await sb.rpc('admin_respuestas_rapidas')) as RespuestaRapida[]
    },
    async guardarRespuestaRapida(r) {
      ok(
        await sb.rpc('admin_guardar_respuesta_rapida', {
          p_id: r.id,
          p_titulo: r.titulo.trim(),
          p_texto: r.texto.trim(),
        })
      )
    },
    async borrarRespuestaRapida(id) {
      ok(await sb.rpc('admin_borrar_respuesta_rapida', { p_id: id }))
    },
  }
}

/** Trazo de ejemplo (solo para ?demo) alrededor de un punto. */
function trazoEjemplo(lat: number, lng: number, variante: number): PuntoGeo[] {
  return Array.from({ length: 12 }, (_, i) => ({
    lat: lat + Math.sin(i * (0.7 + variante * 0.2)) * 0.004,
    lng: lng + i * 0.0016,
  }))
}

/** Datos de ejemplo para revisar el panel en desarrollo sin servidor ni rol. */
export function fuenteDemo<R extends { status: string }, A, E>(
  casos: CasosAdmin<R, A>,
  estadisticas: (desde: string, hasta: string) => E,
  vivo: VivoAdmin | null = null
): FuenteAdmin<R, A, E> {
  const usuarios: UsuarioAdmin[] = [
    ['Andrea Montaño', 'andre.mont', 214, null],
    ['Kevin Jaramillo', 'kevin.jaramillo', 66, null],
    ['Daniela Cueva', 'dani.cueva', 412, 'moderador'],
    ['Luis Ordóñez', 'luiso.trail', 1020, null],
    ['Jhonatan Jara', 'jhonatan', 880, 'admin'],
  ].map(([n, u, km, role], i) => ({
    id: `u${i}`,
    username: u as string,
    full_name: n as string,
    created_at: new Date(2026, 9, 20 - i * 3).toISOString(),
    suspended_at: null,
    role: role as UsuarioAdmin['role'],
    carreras: Math.round((km as number) / 6),
    km: km as number,
  }))
  const eventos: EventoAdmin[] = [
    {
      id: 'e1',
      title: '10K Ciudad de Loja',
      description: 'Carrera urbana por el centro histórico.',
      starts_at: '2026-11-15T07:00:00-05:00',
      location: 'Parque Central',
      distance_km: 10,
      published: true,
    },
    {
      id: 'e2',
      title: 'Trail Podocarpus',
      description: 'Salida grupal de montaña.',
      starts_at: '2026-11-29T06:30:00-05:00',
      location: 'Cajanuma',
      distance_km: 18,
      published: false,
    },
  ]
  const ajustes: AjusteApp[] = [
    { key: 'mantenimiento', value: false },
    { key: 'registro_abierto', value: true },
    { key: 'version_minima_android', value: '1.0.0' },
    { key: 'sla_horas_moderacion', value: { alta: 4, media: 24, baja: 72 } },
  ]
  let terminos = '2026-10-25'
  const respuestas: RespuestaRapida[] = [
    {
      id: 'q1',
      titulo: 'Contenido retirado',
      texto:
        'Revisamos tu reporte y retiramos el contenido porque incumple las normas de la comunidad. Gracias por avisarnos.',
    },
    {
      id: 'q2',
      titulo: 'Sin infracción',
      texto:
        'Revisamos tu reporte y no encontramos una infracción de las normas de la comunidad. Gracias por ayudarnos a cuidar Runcity.',
    },
  ]
  const nada = () => Promise.resolve()
  return {
    demo: true,
    esAdmin: () => Promise.resolve(true),
    metricas: async () => ({
      usuarios: 2418,
      usuarios_7d: 126,
      carreras_7d: 1094,
      km_7d: 6120.4,
      reportes_pendientes: (await casos.pendientes()).filter((r) => r.status === 'pendiente')
        .length,
      eventos_proximos: 6,
      clubes: 14,
      rutas: 52,
    }),
    usuarios: () => Promise.resolve(usuarios),
    async suspender(id, si) {
      const u = usuarios.find((x) => x.id === id)
      if (u) u.suspended_at = si ? new Date().toISOString() : null
    },
    async cambiarRol(id, rol) {
      const u = usuarios.find((x) => x.id === id)
      if (u) u.role = rol
    },
    reportes: () => casos.pendientes(),
    moderar: (r, accion, nota) => casos.moderar(r, accion, nota),
    estadisticas: (desde, hasta) => Promise.resolve(estadisticas(desde, hasta)),
    eventos: () => Promise.resolve(eventos),
    async guardarEvento(e) {
      const i = eventos.findIndex((x) => x.id === e.id)
      const nuevo = {
        id: e.id ?? `e${eventos.length + 1}`,
        description: '',
        starts_at: null,
        location: '',
        distance_km: 0,
        published: false,
        ...e,
      }
      if (i >= 0) eventos[i] = nuevo
      else eventos.unshift(nuevo)
    },
    rutas: () =>
      Promise.resolve([
        {
          id: 'r1',
          name: 'Parque Jipiro',
          distance_km: 3.2,
          difficulty: 'Fácil',
          elevation_m: 12,
          is_official: true,
          path: trazoEjemplo(-3.9855, -79.2045, 0),
        },
        {
          id: 'r2',
          name: 'Mirador Las Pavas',
          distance_km: 7.8,
          difficulty: 'Difícil',
          elevation_m: 186,
          is_official: false,
          path: trazoEjemplo(-4.0, -79.19, 1),
        },
      ]),
    enVivo: () => (vivo ? vivo.escenario() : Promise.resolve(null)),
    importacionesEcu: () =>
      Promise.resolve([
        { anio: 2026, mes: 10, agrupaciones: 843 },
        { anio: 2026, mes: 9, agrupaciones: 816 },
        { anio: 2026, mes: 8, agrupaciones: 0 },
        { anio: 2026, mes: 7, agrupaciones: 861 },
      ]),
    borrarRuta: nada,
    clubes: () =>
      Promise.resolve([
        { id: 'c1', name: 'Club Gacelas Loja', description: 'Club principal.' },
        { id: 'c2', name: 'Runners UNL', description: 'Universitarios.' },
      ]),
    borrarClub: nada,
    ajustes: () => Promise.resolve(ajustes),
    async guardarAjuste(key, value) {
      const a = ajustes.find((x) => x.key === key)
      if (a) a.value = value
      else ajustes.push({ key, value })
    },
    versionTerminos: () => Promise.resolve(terminos),
    async publicarTerminos(v) {
      terminos = v
    },
    cambiarPrioridad: (r, p) => casos.cambiarPrioridad(r, p),
    sos: () =>
      Promise.resolve([
        {
          id: 's1',
          nombre: 'Andrea Montaño',
          activa: true,
          started_at: new Date(Date.now() - 12 * 60_000).toISOString(),
          ended_at: null,
          last_at: new Date(Date.now() - 3 * 60_000).toISOString(),
          lat: -3.9931,
          lng: -79.2042,
          puntos: 14,
        },
        {
          id: 's2',
          nombre: 'Kevin Jaramillo',
          activa: false,
          started_at: new Date(Date.now() - 5 * 3_600_000).toISOString(),
          ended_at: new Date(Date.now() - 4.6 * 3_600_000).toISOString(),
          last_at: null,
          lat: null,
          lng: null,
          puntos: 21,
        },
      ]),
    accesos: () =>
      Promise.resolve(
        usuarios.slice(0, 4).map((u, i) => ({
          id: u.id,
          ultimo_acceso: new Date(Date.now() - (i + 1) * 3 * 3_600_000).toISOString(),
          plataforma: i % 2 ? 'web' : 'android',
          version_name: i % 2 ? '' : '1.0.2',
        }))
      ),
    registroEcu: () =>
      Promise.resolve([
        {
          periodo: '2026-10',
          archivo: 'incidentes_octubre_2026.csv',
          filas: 454099,
          filas_loja: 9997,
          estado: 'ok' as const,
          mensaje: 'Periodos: 2026-10; agregados: 843',
          fecha: '2026-11-20T14:22:39Z',
        },
      ]),
    versionesPublicadas: () =>
      Promise.resolve([
        {
          plataforma: 'android',
          version_name: '1.0.1',
          version_code: 10001,
          canal: 'prerelease' as const,
          publicada_en: '2026-11-22T00:17:09Z',
          apk_bytes: 39876927,
        },
        {
          plataforma: 'android',
          version_name: '0.9.11',
          version_code: 911,
          canal: 'estable' as const,
          publicada_en: '2026-11-05T03:38:40Z',
          apk_bytes: 51820448,
        },
      ]),
    versionesEnUso: () =>
      Promise.resolve([
        { plataforma: 'android', version_name: '1.0.2', version_code: 10002, usuarios: 2 },
        { plataforma: 'web', version_name: '', version_code: null, usuarios: 1 },
      ]),
    respuestasRapidas: () =>
      Promise.resolve([...respuestas].sort((a, b) => a.titulo.localeCompare(b.titulo))),
    async guardarRespuestaRapida(r) {
      const i = respuestas.findIndex((x) => x.id === r.id && r.id)
      if (i >= 0) respuestas[i] = r
      else respuestas.push({ ...r, id: `q${respuestas.length + 1}` })
    },
    async borrarRespuestaRapida(id) {
      const i = respuestas.findIndex((x) => x.id === id)
      if (i >= 0) respuestas.splice(i, 1)
    },
  }
}
