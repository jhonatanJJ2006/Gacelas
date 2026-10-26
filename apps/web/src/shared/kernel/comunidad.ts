import { type CarreraGuardada } from './tipos'

/* Lenguaje publicado de Comunidad: publicaciones, comentarios y corredores visibles. */

export interface PublicacionRemota {
  id: string
  autorId: string
  autor: string
  titulo: string
  inicioMs: number
  distanciaKm: number
  segundos: number
  /** Desnivel positivo (m). */
  desnivelM?: number
  /** URLs publicas de las fotos de la publicacion. */
  imagenes?: string[]
  compartidos?: number
  compartida?: boolean
  /** Km publicos acumulados del autor (nivel). */
  autorKm?: number
  ultimoComentario?: { autor: string; texto: string } | null
  likes: number
  comentarios: number
  puntos: CarreraGuardada['puntos']
  /** id local de la carrera (client_id) para no duplicar la copia local. */
  clienteId?: string | null
  avatar?: string | null
  meGusta?: boolean
  sigo?: boolean
  propia?: boolean
}

export interface ComentarioPublicacion {
  id: string
  autorId: string
  autor: string
  texto: string
  creadoMs: number
}

export interface CorredorPublico {
  id: string
  nombre: string
  username: string
  foto: string | null
  ubicacion: string
}
