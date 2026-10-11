package com.gacelas.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.data.ValorJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import java.security.MessageDigest

@Serializable
data class Perfil(
    val email: String,
    val nombre: String,
    val username: String,
    val bio: String = "",
    val ubicacion: String = "Loja, Ecuador",
    val edad: String = "",
    val genero: String = "Prefiero no decir",
    val fotoPath: String? = null,
)

sealed interface ResultadoAuth {
    data class Ok(val perfil: Perfil) : ResultadoAuth
    data class Error(val mensaje: String) : ResultadoAuth
}

/**
 * Autenticacion de la app. Hoy la implementa [LocalAuthRepository] (local, sin red);
 * mas adelante se puede reemplazar por una implementacion con Supabase Auth.
 */
interface AuthRepository {
    /** Perfil de la sesion activa o null si no hay sesion. */
    val sesion: Flow<Perfil?>
    suspend fun iniciarSesion(email: String, password: String): ResultadoAuth
    suspend fun registrar(nombre: String, email: String, password: String): ResultadoAuth
    suspend fun recuperarContrasena(email: String): ResultadoAuth
    suspend fun actualizarPerfil(perfil: Perfil)
    suspend fun cerrarSesion()
    /** True si la app puede entrar con Google (hay servidor configurado). */
    val googleDisponible: Boolean get() = false
    /** Entra con el ID token de Google obtenido con Credential Manager. */
    suspend fun entrarConGoogle(idToken: String, nonce: String?): ResultadoAuth =
        ResultadoAuth.Error("El inicio con Google no está disponible en esta versión de la app. Usa tu correo por ahora.")
}

/** Reglas de validacion de los formularios de acceso. Devuelven el mensaje de error o null. */
object ValidacionAuth {
    private val regexEmail = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun email(email: String): String? = when {
        email.isBlank() -> "Ingresa tu correo electrónico"
        !regexEmail.matches(email.trim()) -> "El correo no es válido"
        else -> null
    }

    fun password(password: String): String? = when {
        password.length < 8 -> "La contraseña debe tener mínimo 8 caracteres"
        password.none { it.isLetter() } || password.none { it.isDigit() } -> "Usa letras y números"
        else -> null
    }

    fun nombre(nombre: String): String? = if (nombre.trim().length < 2) "Ingresa tu nombre" else null

    fun usernameDesde(nombre: String, email: String): String {
        val base = nombre.lowercase().filter { it.isLetterOrDigit() || it == ' ' }.trim().replace(' ', '_')
        return base.ifBlank { email.substringBefore('@') }.take(20)
    }
}

@Serializable
data class CuentaLocal(val perfil: Perfil, val hash: String)

@Serializable
data class DatosAuth(val cuentas: Map<String, CuentaLocal> = emptyMap(), val sesionEmail: String? = null)

/** Autenticacion local: guarda cuentas y sesion en DataStore. La contrasena se guarda como hash SHA-256. */
class LocalAuthRepository(store: DataStore<Preferences>) : AuthRepository {
    private val datos = ValorJson(store, "auth", DatosAuth.serializer(), DatosAuth())

    override val sesion: Flow<Perfil?> = datos.flujo.map { d -> d.sesionEmail?.let { d.cuentas[it]?.perfil } }

    override suspend fun iniciarSesion(email: String, password: String): ResultadoAuth {
        ValidacionAuth.email(email)?.let { return ResultadoAuth.Error(it) }
        if (password.isEmpty()) return ResultadoAuth.Error("Ingresa tu contraseña")
        val clave = email.trim().lowercase()
        val cuenta = datos.flujo.first().cuentas[clave]
        if (cuenta == null || cuenta.hash != hash(clave, password)) return ResultadoAuth.Error("Correo o contraseña incorrectos")
        datos.actualizar { it.copy(sesionEmail = clave) }
        return ResultadoAuth.Ok(cuenta.perfil)
    }

    override suspend fun registrar(nombre: String, email: String, password: String): ResultadoAuth {
        (ValidacionAuth.nombre(nombre) ?: ValidacionAuth.email(email) ?: ValidacionAuth.password(password))?.let { return ResultadoAuth.Error(it) }
        val clave = email.trim().lowercase()
        if (datos.flujo.first().cuentas.containsKey(clave)) return ResultadoAuth.Error("Ya existe una cuenta con ese correo")
        val perfil = Perfil(clave, nombre.trim(), ValidacionAuth.usernameDesde(nombre, clave))
        datos.actualizar { it.copy(cuentas = it.cuentas + (clave to CuentaLocal(perfil, hash(clave, password))), sesionEmail = clave) }
        return ResultadoAuth.Ok(perfil)
    }

    override suspend fun recuperarContrasena(email: String): ResultadoAuth {
        ValidacionAuth.email(email)?.let { return ResultadoAuth.Error(it) }
        // Sin servidor de correo: se responde igual exista o no la cuenta, para no revelar correos registrados.
        return ResultadoAuth.Ok(Perfil(email.trim().lowercase(), "", ""))
    }

    override suspend fun actualizarPerfil(perfil: Perfil) {
        datos.actualizar { d ->
            val cuenta = d.cuentas[perfil.email] ?: return@actualizar d
            d.copy(cuentas = d.cuentas + (perfil.email to cuenta.copy(perfil = perfil)))
        }
    }

    override suspend fun cerrarSesion() = datos.actualizar { it.copy(sesionEmail = null) }

    private fun hash(email: String, password: String): String =
        MessageDigest.getInstance("SHA-256").digest("gacelas:$email:$password".toByteArray()).joinToString("") { "%02x".format(it) }

}
