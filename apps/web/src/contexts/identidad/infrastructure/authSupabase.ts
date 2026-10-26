import type { SupabaseClient, User } from '@supabase/supabase-js'
import { ValidacionAuth } from '../../../lib/validacion'
import { configSupabase } from '../../../lib/supabase'
import type { AuthRepository } from '../../../data/repositorios'
import type { Perfil, ResultadoAuth } from '../../../data/tipos'

/* Autenticacion y perfil sobre Supabase (contexto Identidad). */

type Fila = Record<string, unknown>

const origen = () => (typeof window === 'undefined' ? '' : window.location.origin)

export function traducirError(mensaje: string): string {
  const m = mensaje.toLowerCase()
  if (m.includes('invalid login')) return 'Correo o contraseña incorrectos'
  if (m.includes('already registered') || m.includes('already been registered'))
    return 'Ya existe una cuenta con ese correo'
  if (m.includes('not confirmed')) return 'Confirma tu correo antes de iniciar sesión'
  if (m.includes('rate limit') || m.includes('security purposes'))
    return 'Demasiados intentos. Espera un momento y vuelve a probar'
  if (m.includes('fetch') || m.includes('network')) return 'Sin conexión con el servidor'
  if (m.includes('password')) return 'La contraseña no es válida'
  return 'No se pudo completar. Intenta de nuevo'
}

export function perfilDesdeFila(fila: Fila | null, user: User): Perfil {
  const meta = (user.user_metadata ?? {}) as Record<string, string | undefined>
  const email = (user.email ?? '').toLowerCase()
  const nombre = (fila?.full_name as string) || meta.full_name || meta.name || email.split('@')[0]
  return {
    email,
    nombre,
    username: (fila?.username as string) || ValidacionAuth.usernameDesde(nombre, email),
    bio: (fila?.bio as string) ?? '',
    ubicacion: (fila?.location as string) ?? 'Loja, Ecuador',
    edad: (fila?.age as string) ?? '',
    genero: (fila?.gender as string) ?? 'Prefiero no decir',
    foto: (fila?.avatar_url as string) || meta.avatar_url || null,
  }
}

export async function usuario(sb: SupabaseClient): Promise<User | null> {
  const { data } = await sb.auth.getSession()
  return data.session?.user ?? null
}

export class SupabaseAuthRepository implements AuthRepository {
  private sb: SupabaseClient
  constructor(sb: SupabaseClient) {
    this.sb = sb
  }

  private async perfil(user: User): Promise<Perfil> {
    const { data } = await this.sb.from('profiles').select('*').eq('id', user.id).maybeSingle()
    return perfilDesdeFila(data as Fila | null, user)
  }

  async sesionActual() {
    const user = await usuario(this.sb)
    return user ? this.perfil(user) : null
  }

  async iniciarSesion(email: string, password: string): Promise<ResultadoAuth> {
    const errorEmail = ValidacionAuth.email(email)
    if (errorEmail) return { ok: false, error: errorEmail }
    if (!password) return { ok: false, error: 'Ingresa tu contraseña' }
    const { data, error } = await this.sb.auth.signInWithPassword({
      email: email.trim().toLowerCase(),
      password,
    })
    if (error || !data.user) return { ok: false, error: traducirError(error?.message ?? '') }
    return { ok: true, perfil: await this.perfil(data.user) }
  }

  async registrar(nombre: string, email: string, password: string): Promise<ResultadoAuth> {
    const error =
      ValidacionAuth.nombre(nombre) ??
      ValidacionAuth.email(email) ??
      ValidacionAuth.password(password)
    if (error) return { ok: false, error }
    const clave = email.trim().toLowerCase()
    const { data, error: e } = await this.sb.auth.signUp({
      email: clave,
      password,
      options: {
        data: { full_name: nombre.trim(), username: ValidacionAuth.usernameDesde(nombre, clave) },
        emailRedirectTo: `${origen()}/inicio`,
      },
    })
    if (e) return { ok: false, error: traducirError(e.message) }
    // Con la confirmacion de correo activa, Supabase no abre sesion hasta confirmar.
    if (!data.session || !data.user)
      return {
        ok: false,
        error: 'Te enviamos un correo para confirmar tu cuenta. Ábrelo y luego inicia sesión.',
      }
    return { ok: true, perfil: await this.perfil(data.user) }
  }

  async recuperarContrasena(email: string): Promise<ResultadoAuth> {
    const error = ValidacionAuth.email(email)
    if (error) return { ok: false, error }
    const clave = email.trim().toLowerCase()
    const { error: e } = await this.sb.auth.resetPasswordForEmail(clave, {
      redirectTo: `${origen()}/nueva-contrasena`,
    })
    // Igual que en local: no se revela si el correo existe.
    if (e && traducirError(e.message).startsWith('Demasiados'))
      return { ok: false, error: traducirError(e.message) }
    return {
      ok: true,
      perfil: { ...perfilDesdeFila(null, { email: clave } as User), email: clave },
    }
  }

  async cambiarContrasena(password: string): Promise<ResultadoAuth> {
    const error = ValidacionAuth.password(password)
    if (error) return { ok: false, error }
    const { data, error: e } = await this.sb.auth.updateUser({ password })
    if (e || !data.user) return { ok: false, error: e ? traducirError(e.message) : 'Sin sesión' }
    return { ok: true, perfil: await this.perfil(data.user) }
  }

  async actualizarPerfil(perfil: Perfil) {
    const user = await usuario(this.sb)
    if (!user) return perfil
    const { error } = await this.sb
      .from('profiles')
      .update({
        full_name: perfil.nombre,
        username: perfil.username,
        bio: perfil.bio,
        location: perfil.ubicacion,
        age: perfil.edad,
        gender: perfil.genero,
        avatar_url: perfil.foto,
      })
      .eq('id', user.id)
    if (error) throw new Error(traducirError(error.message))
    return perfil
  }

  async cerrarSesion() {
    await this.sb.auth.signOut()
  }

  /** Mira en /auth/v1/settings si Google esta activo, para no mandar al usuario a un error. */
  private async googleActivo(): Promise<boolean> {
    if (!configSupabase.url) return true
    try {
      const r = await fetch(`${configSupabase.url}/auth/v1/settings`, {
        headers: { apikey: configSupabase.anonKey },
      })
      if (!r.ok) return true
      const ajustes = (await r.json()) as { external?: { google?: boolean } }
      return ajustes.external?.google !== false
    } catch {
      return true
    }
  }

  async entrarConGoogle() {
    if (!(await this.googleActivo()))
      return 'El inicio con Google todavía no está activado en el servidor'
    const { error } = await this.sb.auth.signInWithOAuth({
      provider: 'google',
      options: { redirectTo: `${origen()}/inicio` },
    })
    if (error)
      return error.message.toLowerCase().includes('provider is not enabled')
        ? 'El inicio con Google todavía no está activado en el servidor'
        : traducirError(error.message)
    return null
  }

  alCambiarSesion(cb: (perfil: Perfil | null) => void) {
    const { data } = this.sb.auth.onAuthStateChange((evento, sesion) => {
      if (evento === 'INITIAL_SESSION' || evento === 'TOKEN_REFRESHED') return
      // Fuera del callback para no bloquear el cliente de auth.
      setTimeout(() => {
        if (!sesion?.user) cb(null)
        else void this.perfil(sesion.user).then(cb)
      }, 0)
    })
    return () => data.subscription.unsubscribe()
  }
}
