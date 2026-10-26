import { type Ajustes, type Perfil, type ResultadoAuth } from '../../../shared/kernel/tipos'

/*
 * Puertos de Identidad. La app solo conoce estas interfaces; se implementan con
 * localStorage o con Supabase sin tocar las pantallas.
 */

export interface AuthRepository {
  sesionActual(): Promise<Perfil | null>
  iniciarSesion(email: string, password: string): Promise<ResultadoAuth>
  registrar(nombre: string, email: string, password: string): Promise<ResultadoAuth>
  recuperarContrasena(email: string): Promise<ResultadoAuth>
  actualizarPerfil(perfil: Perfil): Promise<Perfil>
  cerrarSesion(): Promise<void>
  /** Abre el inicio de sesion con Google. Si redirige, la promesa devuelve null. */
  entrarConGoogle(): Promise<string | null>
  /** Cambia la contrasena de la sesion actual (tras abrir el enlace de recuperacion). */
  cambiarContrasena(password: string): Promise<ResultadoAuth>
  /** Avisa cuando la sesion cambia fuera de la app (OAuth, token vencido, otra pestana). */
  alCambiarSesion?(cb: (perfil: Perfil | null) => void): () => void
}

export interface AjustesRepository {
  leer(): Promise<Ajustes>
  actualizar(cambios: Partial<Ajustes>): Promise<Ajustes>
}
