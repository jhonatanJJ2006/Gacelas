import { obtenerSupabase } from '../../shared/supabase/client'

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
export type TipoReporte = 'post' | 'usuario' | 'comentario' | 'evento'
export type EstadoReporte = 'pendiente' | 'en_revision' | 'resuelto' | 'descartado'
export type AccionModeracion =
  | 'en_revision'
  | 'resuelto'
  | 'descartado'
  | 'reabierto'
  | 'ocultar_contenido'
  | 'suspender_usuario'
  | 'nota'
export interface AccionReporte {
  accion: AccionModeracion
  nota: string
  fecha: string
  moderador: string
}
/** Fila de la cola ampliada (public.admin_cola_reportes, migracion 023). */
export interface ReporteAdmin {
  id: string
  target_type: TipoReporte
  target_id: string
  /** Compatibilidad con la cola anterior (solo publicaciones). */
  post_id?: string
  reason: string
  detail: string
  status: EstadoReporte
  created_at: string
  reportante: string
  resumen: string
  total_reportes: number
  acciones: AccionReporte[]
}
export interface Estadisticas {
  desde: string
  hasta: string
  totales: {
    usuarios_nuevos: number
    usuarios_activos: number
    carreras: number
    km: number
    eventos: number
    inscripciones: number
    reportes: number
    usuarios_total: number
  }
  serie: { dia: string; nuevos: number; activos: number; carreras: number; km: number }[]
  rutas_top: {
    id: string
    nombre: string
    oficial: boolean
    corridas: number
    corredores: number
  }[]
  eventos_top: { id: string; titulo: string; fecha: string; inscritos: number }[]
  retencion: { previos: number; vuelven: number; porcentaje: number }
  reportes: {
    por_estado: Record<string, number>
    por_tipo: Record<string, number>
    por_motivo: Record<string, number>
  }
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
export interface RutaAdmin {
  id: string
  name: string
  distance_km: number
  difficulty: string
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
export interface FuenteAdmin {
  demo: boolean
  esAdmin(): Promise<boolean>
  metricas(): Promise<Metricas>
  usuarios(): Promise<UsuarioAdmin[]>
  suspender(id: string, si: boolean): Promise<void>
  cambiarRol(id: string, rol: UsuarioAdmin['role']): Promise<void>
  reportes(): Promise<ReporteAdmin[]>
  moderar(r: ReporteAdmin, accion: AccionModeracion, nota: string): Promise<void>
  estadisticas(desde: string, hasta: string): Promise<Estadisticas>
  eventos(): Promise<EventoAdmin[]>
  guardarEvento(e: Partial<EventoAdmin> & { title: string }): Promise<void>
  rutas(): Promise<RutaAdmin[]>
  borrarRuta(id: string): Promise<void>
  clubes(): Promise<ClubAdmin[]>
  borrarClub(id: string): Promise<void>
  ajustes(): Promise<AjusteApp[]>
  guardarAjuste(key: string, value: unknown): Promise<void>
  versionTerminos(): Promise<string | null>
  publicarTerminos(version: string): Promise<void>
}

function ok<T>(r: { data: T | null; error: { message: string } | null }): T {
  if (r.error) throw new Error(r.error.message)
  return r.data as T
}

export function fuenteSupabase(): FuenteAdmin | null {
  const sb = obtenerSupabase()
  if (!sb) return null
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
    async reportes() {
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
    async estadisticas(desde, hasta) {
      return ok(await sb.rpc('admin_estadisticas', { desde, hasta })) as Estadisticas
    },
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
      return ok(
        await sb.from('routes').select('id,name,distance_km,difficulty').order('name')
      ) as RutaAdmin[]
    },
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
  }
}

/** Datos de ejemplo para revisar el panel en desarrollo sin servidor ni rol. */
export function fuenteDemo(): FuenteAdmin {
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
  const reportes: ReporteAdmin[] = (
    [
      [
        'post',
        'p1',
        'ofensivo',
        'Insulta a otro club en la descripción.',
        'Fondo de domingo',
        'pendiente',
        2,
      ],
      [
        'post',
        'p2',
        'falso',
        'Ruta que empieza y termina en un domicilio.',
        '42 km en 1 hora',
        'en_revision',
        1,
      ],
      [
        'usuario',
        'u9',
        'suplantacion',
        'Usa el nombre y la foto de un atleta conocido.',
        'Richard Carapaz',
        'pendiente',
        3,
      ],
      [
        'comentario',
        'c4',
        'acoso',
        'Comentarios repetidos contra la misma corredora.',
        'Eres lentísima, deja de publicar',
        'pendiente',
        1,
      ],
      [
        'evento',
        'e3',
        'peligroso',
        'Convoca a correr de noche por la vía a Cuenca sin permisos.',
        'Reto nocturno vía a Cuenca',
        'pendiente',
        2,
      ],
      [
        'post',
        'p3',
        'spam',
        'Zapatillas a mitad de precio, escribe al número.',
        'Oferta zapatillas',
        'resuelto',
        4,
      ],
      ['comentario', 'c7', 'otro', 'No me gustó.', 'Buen ritmo', 'descartado', 1],
    ] as const
  ).map(([target_type, target_id, reason, detail, resumen, status, total], i) => ({
    id: `r${i}`,
    target_type,
    target_id,
    reason,
    detail,
    status,
    created_at: new Date(Date.now() - (i + 1) * 3_600_000).toISOString(),
    reportante: ['Andrea Montaño', 'Kevin Jaramillo', 'Luis Ordóñez'][i % 3],
    resumen,
    total_reportes: total,
    acciones:
      status === 'pendiente'
        ? []
        : [
            {
              accion: (status === 'resuelto' ? 'ocultar_contenido' : status) as AccionModeracion,
              nota: status === 'descartado' ? 'No incumple las normas.' : 'Revisando el trazado.',
              fecha: new Date(Date.now() - i * 1_800_000).toISOString(),
              moderador: 'Daniela Cueva',
            },
          ],
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
  ]
  let terminos = '2026-10-25'
  const nada = () => Promise.resolve()
  return {
    demo: true,
    esAdmin: () => Promise.resolve(true),
    metricas: () =>
      Promise.resolve({
        usuarios: 2418,
        usuarios_7d: 126,
        carreras_7d: 1094,
        km_7d: 6120.4,
        reportes_pendientes: reportes.filter((r) => r.status === 'pendiente').length,
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
    reportes: () => Promise.resolve(reportes),
    async moderar(r, accion, nota) {
      const estado: Partial<Record<AccionModeracion, EstadoReporte>> = {
        en_revision: 'en_revision',
        resuelto: 'resuelto',
        descartado: 'descartado',
        reabierto: 'pendiente',
        ocultar_contenido: 'resuelto',
        suspender_usuario: 'resuelto',
      }
      r.status = estado[accion] ?? r.status
      r.acciones = [
        ...r.acciones,
        { accion, nota, fecha: new Date().toISOString(), moderador: 'Jhonatan Jara' },
      ]
    },
    estadisticas: (desde, hasta) => Promise.resolve(estadisticasDemo(desde, hasta)),
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
        { id: 'r1', name: 'Parque Jipiro', distance_km: 3.2, difficulty: 'Fácil' },
        { id: 'r2', name: 'Mirador Las Pavas', distance_km: 7.8, difficulty: 'Difícil' },
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
    },
    versionTerminos: () => Promise.resolve(terminos),
    async publicarTerminos(v) {
      terminos = v
    },
  }
}

/** Estadisticas de ejemplo deterministas para /admin?demo. */
export function estadisticasDemo(desde: string, hasta: string): Estadisticas {
  const ini = new Date(`${desde}T00:00:00`)
  const fin = new Date(`${hasta}T00:00:00`)
  const dias = Math.max(1, Math.round((fin.getTime() - ini.getTime()) / 86_400_000) + 1)
  const serie = Array.from({ length: dias }, (_, i) => {
    const d = new Date(ini)
    d.setDate(d.getDate() + i)
    const finde = d.getDay() === 0 || d.getDay() === 6
    const ola = Math.sin(i / 3) * 12
    const carreras = Math.round((finde ? 210 : 140) + ola + (i % 5) * 4)
    return {
      dia: d.toISOString().slice(0, 10),
      nuevos: Math.round(14 + (i % 7) * 2 + ola / 3),
      activos: Math.round(carreras * 0.8),
      carreras,
      km: Math.round(carreras * 6.3 * 10) / 10,
    }
  })
  const suma = (k: 'nuevos' | 'carreras' | 'km') => serie.reduce((s, x) => s + x[k], 0)
  return {
    desde,
    hasta,
    totales: {
      usuarios_nuevos: suma('nuevos'),
      usuarios_activos: Math.round(Math.max(...serie.map((s) => s.activos)) * 2.1),
      carreras: suma('carreras'),
      km: Math.round(suma('km') * 10) / 10,
      eventos: Math.max(1, Math.round(dias / 5)),
      inscripciones: Math.round(dias * 11.5),
      reportes: Math.round(dias * 0.9),
      usuarios_total: 2418,
    },
    serie,
    rutas_top: [
      ['loja-jipiro', 'Parque Recreacional Jipiro', 412],
      ['loja-la-tebaida', 'Parque Lineal La Tebaida', 288],
      ['loja-orillas-zamora', 'Orillas del Zamora', 251],
      ['loja-villonaco', 'Subida al Villonaco', 133],
      ['loja-pucara', 'Parque Pucará Podocarpus', 97],
    ].map(([id, nombre, n]) => ({
      id: id as string,
      nombre: nombre as string,
      oficial: true,
      corridas: Math.round(((n as number) * dias) / 30),
      corredores: Math.round(((n as number) * dias) / 30 / 2.4),
    })),
    eventos_top: [
      {
        id: 'e1',
        titulo: '10K Ciudad de Loja',
        fecha: '2026-11-15T07:00:00-05:00',
        inscritos: 318,
      },
      { id: 'e2', titulo: 'Trail Podocarpus', fecha: '2026-11-29T06:30:00-05:00', inscritos: 96 },
    ],
    retencion: { previos: 1180, vuelven: 802, porcentaje: 68 },
    reportes: {
      por_estado: { pendiente: 4, en_revision: 1, resuelto: 9, descartado: 3 },
      por_tipo: { post: 8, comentario: 5, usuario: 3, evento: 1 },
      por_motivo: { spam: 6, ofensivo: 4, acoso: 3, falso: 2, suplantacion: 1, peligroso: 1 },
    },
  }
}
