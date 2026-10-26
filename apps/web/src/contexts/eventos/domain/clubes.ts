/**
 * Comunidades y grupos de running (ADR 006). Ambos viven en eventos.clubs con un tipo:
 * una comunidad es un colectivo amplio y un grupo es un equipo que entrena junto y puede
 * pertenecer a una comunidad.
 */
export type RolClub = 'owner' | 'admin' | 'member'

export type EstadoMiembro = 'active' | 'pending'

export type TipoClub = 'comunidad' | 'grupo'

export type NivelGrupo = 'todos' | 'principiante' | 'intermedio' | 'avanzado'

export interface PuntoEncuentro {
  lat: number
  lng: number
}

export interface Club {
  id: string
  tipo: TipoClub
  nombre: string
  descripcion: string
  ubicacion: string
  ciudad: string
  conAprobacion: boolean
  miembros: number
  ownerId: string
  comunidadId: string | null
  logo: string | null
  portada: string | null
  contacto: string
  lugarEncuentro: string
  punto: PuntoEncuentro | null
  /** 1 = lunes ... 7 = domingo */
  dias: number[]
  /** HH:MM o null */
  hora: string | null
  nivel: NivelGrupo
  ritmoSegKm: number | null
  cupo: number | null
  oculto: boolean
}

/** Datos que el usuario llena al registrar o editar. */
export interface BorradorClub {
  tipo: TipoClub
  nombre: string
  descripcion: string
  ciudad: string
  conAprobacion: boolean
  comunidadId: string | null
  contacto: string
  logo: string | null
  portada: string | null
  lugarEncuentro: string
  punto: PuntoEncuentro | null
  dias: number[]
  hora: string | null
  nivel: NivelGrupo
  ritmoSegKm: number | null
  cupo: number | null
}

export interface MiembroClub {
  usuarioId: string
  nombre: string
  rol: RolClub
  estado: EstadoMiembro
}

export interface PostClub {
  id: string
  usuarioId: string
  nombre: string
  texto: string
  creadoMs: number
}

export const ETIQUETA_ROL: Record<RolClub, string> = {
  owner: 'Fundador',
  admin: 'Admin',
  member: 'Miembro',
}

export const ETIQUETA_TIPO: Record<TipoClub, string> = {
  comunidad: 'Comunidad',
  grupo: 'Grupo de running',
}

export const ETIQUETA_NIVEL: Record<NivelGrupo, string> = {
  todos: 'Todos los niveles',
  principiante: 'Principiante',
  intermedio: 'Intermedio',
  avanzado: 'Avanzado',
}

export const DIAS_CORTOS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']

/** Limites anti spam; los aplica el servidor (migracion 034), aqui solo se muestran. */
export const LIMITE_CREACION: Record<TipoClub, number> = { comunidad: 3, grupo: 5 }
export const LIMITE_DIARIO = 3

export const borradorVacio = (tipo: TipoClub): BorradorClub => ({
  tipo,
  nombre: '',
  descripcion: '',
  ciudad: 'Loja',
  conAprobacion: false,
  comunidadId: null,
  contacto: '',
  logo: null,
  portada: null,
  lugarEncuentro: '',
  punto: null,
  dias: [],
  hora: null,
  nivel: 'todos',
  ritmoSegKm: null,
  cupo: null,
})

export const borradorDesdeClub = (c: Club): BorradorClub => ({
  tipo: c.tipo,
  nombre: c.nombre,
  descripcion: c.descripcion,
  ciudad: c.ciudad,
  conAprobacion: c.conAprobacion,
  comunidadId: c.comunidadId,
  contacto: c.contacto,
  logo: c.logo,
  portada: c.portada,
  lugarEncuentro: c.lugarEncuentro,
  punto: c.punto,
  dias: c.dias,
  hora: c.hora,
  nivel: c.nivel,
  ritmoSegKm: c.ritmoSegKm,
  cupo: c.cupo,
})

export const puedeGestionar = (rol: RolClub | null | undefined) =>
  rol === 'owner' || rol === 'admin'

/** Nombre normalizado igual que el indice unico del servidor. */
export const normalizarNombre = (n: string) => n.trim().replace(/\s+/g, ' ').toLowerCase()

/** Texto del boton principal segun mi relacion con el grupo o comunidad. */
export function accionClub(
  yo: MiembroClub | null | undefined,
  conAprobacion: boolean,
  tipo: TipoClub = 'grupo',
  lleno = false
) {
  if (!yo) {
    if (lleno) return null
    return conAprobacion ? 'Solicitar unirme' : 'Unirme'
  }
  if (yo.estado === 'pending') return 'Solicitud enviada'
  return yo.rol === 'owner'
    ? null
    : tipo === 'comunidad'
      ? 'Salir de la comunidad'
      : 'Salir del grupo'
}

export const estaLleno = (c: Pick<Club, 'cupo' | 'miembros'>) =>
  c.cupo != null && c.miembros >= c.cupo

/** Valida el borrador. Devuelve el primer error o null. */
export function validarClub(
  c: Partial<BorradorClub> & { nombre: string; descripcion: string },
  existentes: Pick<Club, 'id' | 'tipo' | 'nombre'>[] = [],
  editandoId?: string
) {
  const tipo = c.tipo ?? 'grupo'
  const nombre = c.nombre.trim()
  if (nombre.length < 3) return 'El nombre debe tener al menos 3 letras'
  if (nombre.length > 60) return 'El nombre es demasiado largo'
  if (c.descripcion.length > 500) return 'La descripción es demasiado larga'
  if ((c.ciudad ?? '').length > 60) return 'La ciudad es demasiado larga'
  if ((c.contacto ?? '').length > 200) return 'El contacto es demasiado largo'
  if (
    existentes.some(
      (e) =>
        e.tipo === tipo &&
        e.id !== editandoId &&
        normalizarNombre(e.nombre) === normalizarNombre(nombre)
    )
  )
    return `Ya existe ${tipo === 'comunidad' ? 'una comunidad' : 'un grupo'} con ese nombre`
  if (tipo === 'grupo') {
    if ((c.lugarEncuentro ?? '').length > 120) return 'El punto de encuentro es demasiado largo'
    if (c.hora && !/^([01]\d|2[0-3]):[0-5]\d$/.test(c.hora)) return 'La hora no es válida'
    if ((c.dias ?? []).some((d) => d < 1 || d > 7)) return 'Los días no son válidos'
    if (c.cupo != null && (c.cupo < 2 || c.cupo > 1000))
      return 'El cupo debe estar entre 2 y 1000 personas'
    if (c.ritmoSegKm != null && (c.ritmoSegKm < 150 || c.ritmoSegKm > 900))
      return 'El ritmo debe estar entre 2:30 y 15:00 min/km'
  }
  return null
}

/** "Mar y Jue · 06:00" */
export function textoHorario(dias: number[], hora: string | null) {
  const d = [...dias].sort((a, b) => a - b).map((x) => DIAS_CORTOS[x - 1])
  const lista = d.length <= 1 ? d.join('') : `${d.slice(0, -1).join(', ')} y ${d[d.length - 1]}`
  return [lista, hora].filter(Boolean).join(' · ')
}

/** 330 -> "5:30 min/km" */
export function textoRitmo(seg: number | null) {
  if (seg == null) return ''
  return `${Math.floor(seg / 60)}:${String(seg % 60).padStart(2, '0')} min/km`
}

/** "5:30" -> 330; null si no es valido. */
export function leerRitmo(t: string): number | null {
  const m = /^(\d{1,2}):([0-5]\d)$/.exec(t.trim())
  return m ? Number(m[1]) * 60 + Number(m[2]) : null
}

export interface FiltroClubes {
  tipo: TipoClub
  consulta?: string
  nivel?: NivelGrupo | null
  comunidadId?: string | null
}

/** Listado y buscador: por tipo, texto (nombre, ciudad, lugar, descripcion), nivel y comunidad. */
export function filtrarClubes(lista: Club[], f: FiltroClubes) {
  const q = (f.consulta ?? '').trim().toLowerCase()
  return lista.filter(
    (c) =>
      c.tipo === f.tipo &&
      !c.oculto &&
      (!q ||
        `${c.nombre} ${c.ciudad} ${c.ubicacion} ${c.lugarEncuentro} ${c.descripcion}`
          .toLowerCase()
          .includes(q)) &&
      (!f.nivel || f.nivel === 'todos' || c.nivel === f.nivel || c.nivel === 'todos') &&
      (!f.comunidadId || c.comunidadId === f.comunidadId)
  )
}

/** Mensaje claro para los errores del servidor. */
export function mensajeErrorClub(mensaje: string, tipo: TipoClub = 'grupo') {
  if (/clubs_nombre_unico|duplicate key/i.test(mensaje))
    return `Ya existe ${tipo === 'comunidad' ? 'una comunidad' : 'un grupo'} con ese nombre`
  if (/limite|intenta mañana|lleno|no existe|no está disponible/i.test(mensaje)) return mensaje
  return mensaje || 'Algo salió mal'
}
