package com.gacelas.data.remoto

import com.gacelas.shared.nombreClase

import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import com.gacelas.data.sync.ConsultaPagina
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject

/** Usuario autenticado en el servidor. */
data class UsuarioRemoto(val id: String, val email: String, val meta: JsonObject = JsonObject(emptyMap()))

/** Error del servidor con un mensaje ya traducido para la persona usuaria. */
class ErrorRemoto(mensaje: String) : Exception(mensaje)

/**
 * Lo minimo que la app necesita de Supabase (Auth + PostgREST). Los repositorios usan esta
 * interfaz; en produccion la implementa [SupabaseBackend] y en las pruebas un backend en memoria.
 */
interface Backend {
    suspend fun usuarioActual(): UsuarioRemoto?
    suspend fun entrarConEmail(email: String, password: String): UsuarioRemoto
    /** Devuelve el usuario si quedo con sesion, o null si debe confirmar el correo. */
    suspend fun registrar(email: String, password: String, datos: JsonObject): UsuarioRemoto?
    suspend fun recuperarContrasena(email: String)
    suspend fun entrarConIdTokenGoogle(idToken: String, nonce: String?): UsuarioRemoto
    /** Sign in with Apple (iOS): ID token de AuthenticationServices con el nonce original. */
    suspend fun entrarConIdTokenApple(idToken: String, nonce: String?): UsuarioRemoto =
        throw ErrorRemoto("El inicio con Apple no está disponible")
    suspend fun salir()

    suspend fun select(tabla: String, filtros: Map<String, String>, ordenarPor: String? = null, descendente: Boolean = false): List<JsonObject>

    /** Filas cuya [columna] esta en [valores] (una sola consulta en Supabase). */
    suspend fun selectEn(tabla: String, columna: String, valores: Collection<String>): List<JsonObject> =
        valores.distinct().flatMap { select(tabla, mapOf(columna to it)) }

    /**
     * Una pagina ordenada por (marca, clave). Sin marca trae desde el principio;
     * con marca y clave, las filas estrictamente posteriores (no se salta empates).
     */
    suspend fun selectPagina(consulta: ConsultaPagina): List<JsonObject>
    suspend fun insert(tabla: String, filas: List<JsonObject>)
    suspend fun upsert(tabla: String, filas: List<JsonObject>, onConflict: String): List<JsonObject>
    suspend fun update(tabla: String, valores: JsonObject, filtros: Map<String, String>)
    suspend fun delete(tabla: String, filtros: Map<String, String>)
    /** Llama a una funcion SQL (RPC). Devuelve JsonNull si no retorna nada. */
    suspend fun rpc(funcion: String, args: JsonObject): kotlinx.serialization.json.JsonElement
    /** Sube un archivo a Storage y devuelve su URL publica. */
    suspend fun subirArchivo(bucket: String, ruta: String, bytes: ByteArray, tipo: String): String
    fun urlPublica(bucket: String, ruta: String): String
}

/** True si PostgREST rechazo la consulta porque [columna] no existe (SQLSTATE 42703). */
fun esColumnaInexistente(mensaje: String?, columna: String): Boolean {
    val m = mensaje.orEmpty().lowercase()
    return columna.lowercase() in m && ("42703" in m || "does not exist" in m || "could not find" in m)
}

/** Traduce mensajes de Supabase a textos para la app (igual que la web). */
fun traducirError(mensaje: String?): String {
    val m = mensaje.orEmpty().lowercase()
    return when {
        "invalid login" in m || "invalid_credentials" in m -> "Correo o contraseña incorrectos"
        "already registered" in m || "already been registered" in m || "user_already_exists" in m -> "Ya existe una cuenta con ese correo"
        "not confirmed" in m -> "Confirma tu correo antes de iniciar sesión"
        "provider is not enabled" in m || ("provider" in m && "not enabled" in m) || "unsupported provider" in m -> "El inicio con Google todavía no está activado en el servidor (Supabase › Auth › Providers › Google)"
        "audience" in m -> "El servidor no acepta el ID de cliente de Google de esta app (falta en Supabase › Google › Client IDs)"
        "nonce" in m -> "El servidor rechazó la verificación de Google (nonce). Intenta de nuevo; si sigue, revisa \"Skip nonce check\" en Supabase"
        "bad id token" in m || "id_token" in m || "id token" in m -> "El servidor rechazó el token de Google (Bad ID token). Revisa el Client ID de Google en Supabase"
        "email_address_invalid" in m || "email address is invalid" in m || ("email" in m && "invalid" in m) -> "El correo no es válido o el servidor no lo acepta. Usa un correo real"
        "email_address_not_authorized" in m || "not authorized" in m && "email" in m -> "Ese correo no está autorizado para registrarse"
        "weak_password" in m || "weak password" in m || "password should be" in m || "password is too" in m -> "La contraseña es muy débil: usa mínimo 8 caracteres con letras y números"
        "same_password" in m || "different from the old password" in m -> "La nueva contraseña debe ser distinta de la anterior"
        "database error saving new user" in m || "unexpected_failure" in m -> "El servidor no pudo crear tu perfil. Intenta de nuevo en unos minutos"
        "over_email_send_rate_limit" in m || "email rate limit" in m -> "Se enviaron demasiados correos. Espera unos minutos y vuelve a probar"
        "signups not allowed" in m || "signup is disabled" in m -> "El registro de cuentas nuevas está desactivado en el servidor"
        "rate limit" in m || "security purposes" in m -> "Demasiados intentos. Espera un momento y vuelve a probar"
        "cuenta suspendida" in m -> "Tu cuenta está suspendida. No puedes publicar ni modificar datos."
        "unable to resolve host" in m || "failed to connect" in m || "timeout" in m || "network" in m -> "Sin conexión con el servidor"
        else -> "No se pudo completar. Intenta de nuevo"
    }
}

private suspend fun <T> remoto(bloque: suspend () -> T): T = try {
    bloque()
} catch (e: ErrorRemoto) {
    throw e
} catch (e: kotlinx.coroutines.CancellationException) {
    throw e
} catch (e: Exception) {
    com.gacelas.shared.logAviso("Backend", "Supabase: ${nombreClase(e)}: ${e.message}")
    throw ErrorRemoto(traducirError(e.message ?: nombreClase(e)))
}

class SupabaseBackend(url: String, clave: String) : Backend {
    val cliente: SupabaseClient = createSupabaseClient(url, clave) {
        install(Auth)
        install(Postgrest)
        install(io.github.jan.supabase.storage.Storage)
    }

    override suspend fun subirArchivo(bucket: String, ruta: String, bytes: ByteArray, tipo: String) = remoto {
        cliente.storage.from(bucket).upload(ruta, bytes) { contentType = io.ktor.http.ContentType.parse(tipo) }
        urlPublica(bucket, ruta)
    }

    override fun urlPublica(bucket: String, ruta: String) = cliente.storage.from(bucket).publicUrl(ruta)

    private fun aUsuario(u: io.github.jan.supabase.auth.user.UserInfo) =
        UsuarioRemoto(u.id, u.email.orEmpty().lowercase(), u.userMetadata ?: JsonObject(emptyMap()))

    override suspend fun usuarioActual(): UsuarioRemoto? {
        // Espera a que el cliente cargue la sesion guardada.
        cliente.auth.sessionStatus.first { it !is SessionStatus.Initializing }
        return cliente.auth.currentUserOrNull()?.let(::aUsuario)
    }

    override suspend fun entrarConEmail(email: String, password: String) = remoto {
        cliente.auth.signInWith(Email) { this.email = email; this.password = password }
        aUsuario(cliente.auth.currentUserOrNull() ?: throw ErrorRemoto("Correo o contraseña incorrectos"))
    }

    override suspend fun registrar(email: String, password: String, datos: JsonObject) = remoto {
        cliente.auth.signUpWith(Email) { this.email = email; this.password = password; data = datos }
        cliente.auth.currentSessionOrNull()?.user?.let(::aUsuario)
    }

    override suspend fun recuperarContrasena(email: String) = remoto { cliente.auth.resetPasswordForEmail(email) }

    override suspend fun entrarConIdTokenGoogle(idToken: String, nonce: String?) = remoto {
        cliente.auth.signInWith(IDToken) {
            this.idToken = idToken
            provider = Google
            this.nonce = nonce
        }
        aUsuario(cliente.auth.currentUserOrNull() ?: throw ErrorRemoto("No se pudo iniciar sesión con Google"))
    }

    override suspend fun entrarConIdTokenApple(idToken: String, nonce: String?) = remoto {
        cliente.auth.signInWith(IDToken) {
            this.idToken = idToken
            provider = io.github.jan.supabase.auth.providers.Apple
            this.nonce = nonce
        }
        aUsuario(cliente.auth.currentUserOrNull() ?: throw ErrorRemoto("No se pudo iniciar sesión con Apple"))
    }

    override suspend fun salir() = remoto { cliente.auth.signOut() }

    override suspend fun select(tabla: String, filtros: Map<String, String>, ordenarPor: String?, descendente: Boolean) = remoto {
        cliente.from(tabla).select {
            filter { filtros.forEach { (k, v) -> eq(k, v) } }
            if (ordenarPor != null) order(ordenarPor, if (descendente) Order.DESCENDING else Order.ASCENDING)
        }.decodeList<JsonObject>()
    }

    override suspend fun selectEn(tabla: String, columna: String, valores: Collection<String>) = remoto {
        val lista = valores.distinct()
        if (lista.isEmpty()) emptyList()
        else cliente.from(tabla).select { filter { isIn(columna, lista) } }.decodeList<JsonObject>()
    }

    override suspend fun selectPagina(consulta: ConsultaPagina) = remoto {
        try {
            cliente.from(consulta.tabla).select {
                filter {
                    consulta.igual.forEach { (k, v) -> eq(k, v) }
                    consulta.nulos.forEach { exact(it, null) }
                    val marca = consulta.marcaExclusiva
                    val clave = consulta.claveExclusiva
                    if (marca != null && clave != null) {
                        or {
                            gt(consulta.columnaMarca, marca)
                            and {
                                eq(consulta.columnaMarca, marca)
                                gt(consulta.columnaClave, clave)
                            }
                        }
                    } else if (marca != null) {
                        gt(consulta.columnaMarca, marca)
                    } else if (clave != null) {
                        gt(consulta.columnaClave, clave)
                    }
                }
                order(consulta.columnaMarca, Order.ASCENDING)
                order(consulta.columnaClave, Order.ASCENDING)
                limit(consulta.limite.toLong())
            }.decodeList<JsonObject>()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!esColumnaInexistente(e.message, consulta.columnaMarca)) throw e
            // La base aun no tiene la marca de sync (migracion 044): se baja la tabla completa,
            // paginada por clave, para que la descarga no falle y deje la app sin eventos ni recorridos.
            com.gacelas.shared.logAviso("Backend", "Sin columna ${consulta.columnaMarca} en ${consulta.tabla}: descarga completa por clave")
            cliente.from(consulta.tabla).select {
                filter {
                    consulta.igual.forEach { (k, v) -> eq(k, v) }
                    consulta.nulos.forEach { exact(it, null) }
                    consulta.claveExclusiva?.let { gt(consulta.columnaClave, it) }
                }
                order(consulta.columnaClave, Order.ASCENDING)
                limit(consulta.limite.toLong())
            }.decodeList<JsonObject>()
        }
    }

    override suspend fun insert(tabla: String, filas: List<JsonObject>) = remoto {
        if (filas.isNotEmpty()) cliente.from(tabla).insert(filas)
        Unit
    }

    override suspend fun upsert(tabla: String, filas: List<JsonObject>, onConflict: String) = remoto {
        cliente.from(tabla).upsert(filas) {
            this.onConflict = onConflict
            select()
        }.decodeList<JsonObject>()
    }

    override suspend fun update(tabla: String, valores: JsonObject, filtros: Map<String, String>) = remoto {
        cliente.from(tabla).update(valores) { filter { filtros.forEach { (k, v) -> eq(k, v) } } }
        Unit
    }

    override suspend fun delete(tabla: String, filtros: Map<String, String>) = remoto {
        cliente.from(tabla).delete { filter { filtros.forEach { (k, v) -> eq(k, v) } } }
        Unit
    }

    override suspend fun rpc(funcion: String, args: JsonObject) = remoto {
        val datos = cliente.postgrest.rpc(funcion, args).data
        if (datos.isBlank()) kotlinx.serialization.json.JsonNull else kotlinx.serialization.json.Json.parseToJsonElement(datos)
    }
}
