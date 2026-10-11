package com.gacelas.data.remoto

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
    suspend fun salir()

    suspend fun select(tabla: String, filtros: Map<String, String>, ordenarPor: String? = null, descendente: Boolean = false): List<JsonObject>
    suspend fun insert(tabla: String, filas: List<JsonObject>)
    suspend fun upsert(tabla: String, filas: List<JsonObject>, onConflict: String): List<JsonObject>
    suspend fun update(tabla: String, valores: JsonObject, filtros: Map<String, String>)
    suspend fun delete(tabla: String, filtros: Map<String, String>)
}

/** Traduce mensajes de Supabase a textos para la app (igual que la web). */
fun traducirError(mensaje: String?): String {
    val m = mensaje.orEmpty().lowercase()
    return when {
        "invalid login" in m || "invalid_credentials" in m -> "Correo o contraseña incorrectos"
        "already registered" in m || "already been registered" in m || "user_already_exists" in m -> "Ya existe una cuenta con ese correo"
        "not confirmed" in m -> "Confirma tu correo antes de iniciar sesión"
        "provider is not enabled" in m || "unsupported provider" in m -> "El inicio con Google todavía no está activado en el servidor"
        "rate limit" in m || "security purposes" in m -> "Demasiados intentos. Espera un momento y vuelve a probar"
        "unable to resolve host" in m || "failed to connect" in m || "timeout" in m || "network" in m -> "Sin conexión con el servidor"
        else -> "No se pudo completar. Intenta de nuevo"
    }
}

private suspend fun <T> remoto(bloque: suspend () -> T): T = try {
    bloque()
} catch (e: ErrorRemoto) {
    throw e
} catch (e: Exception) {
    throw ErrorRemoto(traducirError(e.message ?: e.javaClass.simpleName))
}

class SupabaseBackend(url: String, clave: String) : Backend {
    val cliente: SupabaseClient = createSupabaseClient(url, clave) {
        install(Auth)
        install(Postgrest)
    }

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
}
