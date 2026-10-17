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
        "signups not allowed" in m || "signup is disabled" in m -> "El registro de cuentas nuevas está desactivado en el servidor"
        "rate limit" in m || "security purposes" in m -> "Demasiados intentos. Espera un momento y vuelve a probar"
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
