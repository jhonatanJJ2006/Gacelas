import {
  type ComentarioPublicacion,
  type CorredorPublico,
  type PublicacionRemota,
} from '../../../shared/kernel/comunidad'

export interface ComunidadRepository {
  /** Busca corredores por nombre o usuario (perfil publico minimo). */
  buscar(consulta: string): Promise<CorredorPublico[]>
  /** Carreras publicas de otros corredores. */
  publicaciones(): Promise<PublicacionRemota[]>
  alternarMeGusta(postId: string, activo: boolean): Promise<void>
  comentarios(postId: string): Promise<ComentarioPublicacion[]>
  comentar(postId: string, texto: string): Promise<void>
  /** Avisa cuando cambian carreras, likes, comentarios o seguidos. Devuelve la baja. */
  suscribir?(alCambiar: () => void): () => void
  siguiendo(): Promise<string[]>
  alternarSeguir(id: string): Promise<string[]>
  /** Numero de corredores que siguen al usuario. */
  seguidores?(): Promise<number>
  /** Compartir (repost) o dejar de compartir una carrera publica. */
  alternarCompartida?(postId: string, activo: boolean): Promise<void>
  /** Sube fotos a una carrera propia; devuelve las URLs publicas. */
  subirFotos?(postId: string, archivos: File[]): Promise<string[]>
}
