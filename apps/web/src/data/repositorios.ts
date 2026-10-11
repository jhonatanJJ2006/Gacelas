import type { Evento, Notificacion } from '../mocks/data'
import { ValidacionAuth } from '../lib/validacion'
import type { Racha } from '../lib/rachas'
import {
  AJUSTES_POR_DEFECTO,
  type Ajustes,
  type CarreraGuardada,
  type Perfil,
  type ResultadoAuth,
} from './tipos'

/*
 * Repositorios de datos. La app solo conoce estas interfaces; hoy se implementan con
 * localStorage y mas adelante se pueden reemplazar por Supabase sin tocar las pantallas.
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

export interface CarrerasRepository {
  listar(): Promise<CarreraGuardada[]>
  guardar(c: CarreraGuardada): Promise<CarreraGuardada[]>
  eliminar(id: string): Promise<CarreraGuardada[]>
}

export interface RutasRepository {
  guardadas(): Promise<string[]>
  alternarGuardada(id: string): Promise<string[]>
}

export interface AjustesRepository {
  leer(): Promise<Ajustes>
  actualizar(cambios: Partial<Ajustes>): Promise<Ajustes>
}

export interface EventosRepository {
  creados(): Promise<Evento[]>
  inscritos(): Promise<string[]>
  crear(e: Evento): Promise<Evento[]>
  alternarInscripcion(id: string): Promise<string[]>
}

export interface NotificacionesRepository {
  listar(): Promise<Notificacion[]>
  marcarLeidas(ids: string[]): Promise<void>
}

export interface PublicacionRemota {
  id: string
  autorId: string
  autor: string
  titulo: string
  inicioMs: number
  distanciaKm: number
  segundos: number
  likes: number
  comentarios: number
  puntos: CarreraGuardada['puntos']
}

export interface CorredorPublico {
  id: string
  nombre: string
  username: string
  foto: string | null
  ubicacion: string
}

export interface ComunidadRepository {
  /** Busca corredores por nombre o usuario (perfil publico minimo). */
  buscar(consulta: string): Promise<CorredorPublico[]>
  /** Carreras publicas de otros corredores. */
  publicaciones(): Promise<PublicacionRemota[]>
  siguiendo(): Promise<string[]>
  alternarSeguir(id: string): Promise<string[]>
}

export interface RachasRepository {
  /** Racha calculada en el servidor, o null si no se pudo leer. */
  leer(): Promise<Racha | null>
}

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
}

const PREFIJO = 'gacelas:'

export function leer<T>(clave: string, defecto: T): T {
  try {
    const crudo = localStorage.getItem(PREFIJO + clave)
    return crudo == null ? defecto : (JSON.parse(crudo) as T)
  } catch {
    return defecto
  }
}

export function escribir<T>(clave: string, valor: T): T {
  try {
    localStorage.setItem(PREFIJO + clave, JSON.stringify(valor))
  } catch {
    // Almacenamiento lleno o bloqueado: se mantiene el valor en memoria.
  }
  return valor
}

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

export class LocalCarrerasRepository implements CarrerasRepository {
  async listar() {
    return leer<CarreraGuardada[]>('carreras', [])
  }
  async guardar(c: CarreraGuardada) {
    return escribir('carreras', [c, ...(await this.listar()).filter((x) => x.id !== c.id)])
  }
  async eliminar(id: string) {
    return escribir(
      'carreras',
      (await this.listar()).filter((x) => x.id !== id)
    )
  }
}

const alternar = (lista: string[], id: string) =>
  lista.includes(id) ? lista.filter((x) => x !== id) : [...lista, id]

export class LocalRutasRepository implements RutasRepository {
  async guardadas() {
    return leer<string[]>('rutas-guardadas', [])
  }
  async alternarGuardada(id: string) {
    return escribir('rutas-guardadas', alternar(await this.guardadas(), id))
  }
}

export class LocalAjustesRepository implements AjustesRepository {
  async leer() {
    return { ...AJUSTES_POR_DEFECTO, ...leer<Partial<Ajustes>>('ajustes', {}) }
  }
  async actualizar(cambios: Partial<Ajustes>) {
    return escribir('ajustes', { ...(await this.leer()), ...cambios })
  }
}

export class LocalEventosRepository implements EventosRepository {
  async creados() {
    return leer<Evento[]>('eventos-creados', [])
  }
  async inscritos() {
    return leer<string[]>('eventos-inscritos', [])
  }
  async crear(e: Evento) {
    escribir('eventos-inscritos', Array.from(new Set([...(await this.inscritos()), e.id])))
    return escribir('eventos-creados', [e, ...(await this.creados())])
  }
  async alternarInscripcion(id: string) {
    return escribir('eventos-inscritos', alternar(await this.inscritos(), id))
  }
}

export const repositoriosLocales = (): Repositorios => ({
  auth: new LocalAuthRepository(),
  carreras: new LocalCarrerasRepository(),
  rutas: new LocalRutasRepository(),
  ajustes: new LocalAjustesRepository(),
  eventos: new LocalEventosRepository(),
})
