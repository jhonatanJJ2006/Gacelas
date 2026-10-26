import { ValidacionAuth } from '../../../shared/lib/validacion'
import {
  AJUSTES_POR_DEFECTO,
  type Ajustes,
  type Perfil,
  type ResultadoAuth,
} from '../../../shared/kernel/tipos'
import { escribir, leer } from '../../../shared/lib/almacenLocal'
import { type AjustesRepository, type AuthRepository } from '../domain/puertos'

/** SHA-256 en hexadecimal; si no hay WebCrypto (contexto no seguro) usa un hash simple. */
export async function hashPassword(password: string): Promise<string> {
  const datos = new TextEncoder().encode(`gacelas:${password}`)
  if (globalThis.crypto?.subtle) {
    const buf = await globalThis.crypto.subtle.digest('SHA-256', datos)
    return Array.from(new Uint8Array(buf), (b) => b.toString(16).padStart(2, '0')).join('')
  }
  let h = 5381
  for (const b of datos) h = ((h << 5) + h + b) | 0
  return `djb2-${(h >>> 0).toString(16)}`
}

interface CuentaLocal {
  perfil: Perfil
  hash: string
}

interface DatosAuth {
  cuentas: Record<string, CuentaLocal>
  sesionEmail: string | null
}

export class LocalAuthRepository implements AuthRepository {
  private datos = () => leer<DatosAuth>('auth', { cuentas: {}, sesionEmail: null })

  async sesionActual() {
    const d = this.datos()
    return d.sesionEmail ? (d.cuentas[d.sesionEmail]?.perfil ?? null) : null
  }

  async iniciarSesion(email: string, password: string): Promise<ResultadoAuth> {
    const errorEmail = ValidacionAuth.email(email)
    if (errorEmail) return { ok: false, error: errorEmail }
    if (!password) return { ok: false, error: 'Ingresa tu contraseña' }
    const clave = email.trim().toLowerCase()
    const d = this.datos()
    const cuenta = d.cuentas[clave]
    if (!cuenta || cuenta.hash !== (await hashPassword(password)))
      return { ok: false, error: 'Correo o contraseña incorrectos' }
    escribir('auth', { ...d, sesionEmail: clave })
    return { ok: true, perfil: cuenta.perfil }
  }

  async registrar(nombre: string, email: string, password: string): Promise<ResultadoAuth> {
    const error =
      ValidacionAuth.nombre(nombre) ??
      ValidacionAuth.email(email) ??
      ValidacionAuth.password(password)
    if (error) return { ok: false, error }
    const clave = email.trim().toLowerCase()
    const d = this.datos()
    if (d.cuentas[clave]) return { ok: false, error: 'Ya existe una cuenta con ese correo' }
    const perfil: Perfil = {
      email: clave,
      nombre: nombre.trim(),
      username: ValidacionAuth.usernameDesde(nombre, clave),
      bio: '',
      ubicacion: 'Loja, Ecuador',
      edad: '',
      genero: 'Prefiero no decir',
      foto: null,
    }
    escribir('auth', {
      cuentas: { ...d.cuentas, [clave]: { perfil, hash: await hashPassword(password) } },
      sesionEmail: clave,
    })
    return { ok: true, perfil }
  }

  async recuperarContrasena(email: string): Promise<ResultadoAuth> {
    const error = ValidacionAuth.email(email)
    if (error) return { ok: false, error }
    // Sin servidor de correo: se responde igual exista o no la cuenta, para no revelar correos registrados.
    return { ok: true, perfil: { ...perfilVacio(), email: email.trim().toLowerCase() } }
  }

  async actualizarPerfil(perfil: Perfil) {
    const d = this.datos()
    const cuenta = d.cuentas[perfil.email]
    if (cuenta)
      escribir('auth', { ...d, cuentas: { ...d.cuentas, [perfil.email]: { ...cuenta, perfil } } })
    return perfil
  }

  async cerrarSesion() {
    escribir('auth', { ...this.datos(), sesionEmail: null })
  }

  async entrarConGoogle() {
    return 'El inicio con Google necesita conexión con el servidor. Usa tu correo por ahora.'
  }

  async cambiarContrasena(password: string): Promise<ResultadoAuth> {
    const error = ValidacionAuth.password(password)
    if (error) return { ok: false, error }
    const d = this.datos()
    const cuenta = d.sesionEmail ? d.cuentas[d.sesionEmail] : undefined
    if (!cuenta) return { ok: false, error: 'Abre el enlace de recuperación de nuevo' }
    escribir('auth', {
      ...d,
      cuentas: {
        ...d.cuentas,
        [cuenta.perfil.email]: { ...cuenta, hash: await hashPassword(password) },
      },
    })
    return { ok: true, perfil: cuenta.perfil }
  }
}

export const perfilVacio = (): Perfil => ({
  email: '',
  nombre: '',
  username: '',
  bio: '',
  ubicacion: '',
  edad: '',
  genero: '',
  foto: null,
})

export class LocalAjustesRepository implements AjustesRepository {
  async leer() {
    return { ...AJUSTES_POR_DEFECTO, ...leer<Partial<Ajustes>>('ajustes', {}) }
  }
  async actualizar(cambios: Partial<Ajustes>) {
    return escribir('ajustes', { ...(await this.leer()), ...cambios })
  }
}
