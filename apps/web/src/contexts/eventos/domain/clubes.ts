export type RolClub = 'owner' | 'admin' | 'member'

export type EstadoMiembro = 'active' | 'pending'

export interface Club {
  id: string
  nombre: string
  descripcion: string
  ubicacion: string
  conAprobacion: boolean
  miembros: number
  ownerId: string
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

export const puedeGestionar = (rol: RolClub | null | undefined) =>
  rol === 'owner' || rol === 'admin'

/** Texto del boton principal segun mi relacion con el club. */
export function accionClub(yo: MiembroClub | null | undefined, conAprobacion: boolean) {
  if (!yo) return conAprobacion ? 'Solicitar unirme' : 'Unirme'
  if (yo.estado === 'pending') return 'Solicitud enviada'
  return yo.rol === 'owner' ? null : 'Salir del club'
}

export function validarClub(c: { nombre: string; descripcion: string }) {
  if (c.nombre.trim().length < 3) return 'El nombre debe tener al menos 3 letras'
  if (c.nombre.trim().length > 60) return 'El nombre es demasiado largo'
  if (c.descripcion.length > 500) return 'La descripción es demasiado larga'
  return null
}
