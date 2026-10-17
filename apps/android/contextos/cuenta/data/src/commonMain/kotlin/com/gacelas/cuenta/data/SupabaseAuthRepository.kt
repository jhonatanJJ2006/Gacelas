package com.gacelas.cuenta.data

import com.gacelas.shared.ahoraMs
import com.gacelas.shared.redondear
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.data.ValorJson
import com.gacelas.cuenta.domain.modelo.Ajustes
import com.gacelas.cuenta.domain.repositorio.AjustesRepository
import com.gacelas.cuenta.domain.modelo.Visibilidad
import com.gacelas.cuenta.domain.repositorio.AuthRepository
import com.gacelas.cuenta.domain.modelo.Perfil
import com.gacelas.cuenta.domain.modelo.ResultadoAuth
import com.gacelas.cuenta.domain.reglas.ValidacionAuth
import com.gacelas.carreras.domain.modelo.Carrera
import com.gacelas.carreras.domain.repositorio.CarrerasRepository
import com.gacelas.carreras.domain.modelo.Parcial
import com.gacelas.carreras.domain.modelo.PuntoGps
import com.gacelas.comunidad.domain.repositorio.EventosRepository
import com.gacelas.data.jsonGacelas
import com.gacelas.comunidad.domain.modelo.Evento
import com.gacelas.rutas.domain.repositorio.RutasRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import com.gacelas.data.remoto.*

fun perfilDesdeFila(fila: JsonObject?, u: UsuarioRemoto): Perfil {
    val nombre = fila?.texto("full_name") ?: u.meta.texto("full_name") ?: u.meta.texto("name") ?: u.email.substringBefore('@')
    return Perfil(
        email = u.email,
        nombre = nombre,
        username = fila?.texto("username") ?: ValidacionAuth.usernameDesde(nombre, u.email),
        bio = fila?.texto("bio").orEmpty(),
        ubicacion = fila?.texto("location") ?: "Loja, Ecuador",
        edad = fila?.texto("age").orEmpty(),
        genero = fila?.texto("gender") ?: "Prefiero no decir",
        fotoPath = fila?.texto("avatar_url") ?: u.meta.texto("avatar_url"),
    )
}

/** Sesion actual: id de usuario remoto y perfil en cache. */
@Serializable
data class SesionRemota(val userId: String, val perfil: Perfil)

class SupabaseAuthRepository(
    private val backend: Backend,
    store: DataStore<Preferences>,
    private val alEntrar: suspend (String) -> Unit = {},
) : AuthRepository {
    private val valor = ValorJson(store, "sesion_remota", SesionRemota.serializer().nullable, null)
    override val sesion: Flow<Perfil?> = kotlinx.coroutines.flow.flow { valor.flujo.collect { emit(it?.perfil) } }

    suspend fun userId(): String? = valor.flujo.first()?.userId

    private suspend fun abrir(u: UsuarioRemoto): ResultadoAuth.Ok {
        val fila = runCatching { backend.select("profiles", mapOf("id" to u.id)).firstOrNull() }.getOrNull()
        val perfil = perfilDesdeFila(fila, u)
        valor.actualizar { SesionRemota(u.id, perfil) }
        runCatching { alEntrar(u.id) }
        return ResultadoAuth.Ok(perfil)
    }

    /** Al abrir la app: confirma la sesion guardada por supabase-kt y refresca el perfil. */
    suspend fun restaurar() {
        val u = runCatching { backend.usuarioActual() }.getOrNull()
        if (u == null) {
            // Sin red se mantiene la sesion en cache; si el servidor dice que no hay sesion, se cierra.
            if (runCatching { backend.usuarioActual() }.isSuccess) valor.actualizar { null }
        } else abrir(u)
    }

    override suspend fun iniciarSesion(email: String, password: String): ResultadoAuth {
        ValidacionAuth.email(email)?.let { return ResultadoAuth.Error(it) }
        if (password.isEmpty()) return ResultadoAuth.Error("Ingresa tu contraseña")
        return try {
            abrir(backend.entrarConEmail(email.trim().lowercase(), password))
        } catch (e: ErrorRemoto) {
            ResultadoAuth.Error(e.message.orEmpty())
        }
    }

    override suspend fun registrar(nombre: String, email: String, password: String): ResultadoAuth {
        (ValidacionAuth.nombre(nombre) ?: ValidacionAuth.email(email) ?: ValidacionAuth.password(password))?.let { return ResultadoAuth.Error(it) }
        val clave = email.trim().lowercase()
        return try {
            val u = backend.registrar(clave, password, buildJsonObject {
                put("full_name", nombre.trim())
                put("username", ValidacionAuth.usernameDesde(nombre, clave))
            }) ?: return ResultadoAuth.Error("Te enviamos un correo para confirmar tu cuenta. Ábrelo y luego inicia sesión.")
            abrir(u)
        } catch (e: ErrorRemoto) {
            ResultadoAuth.Error(e.message.orEmpty())
        }
    }

    override suspend fun recuperarContrasena(email: String): ResultadoAuth {
        ValidacionAuth.email(email)?.let { return ResultadoAuth.Error(it) }
        val clave = email.trim().lowercase()
        return try {
            backend.recuperarContrasena(clave)
            ResultadoAuth.Ok(Perfil(clave, "", ""))
        } catch (e: ErrorRemoto) {
            // No se revela si el correo existe; solo se avisa del limite de envios.
            if (e.message.orEmpty().startsWith("Demasiados")) ResultadoAuth.Error(e.message.orEmpty()) else ResultadoAuth.Ok(Perfil(clave, "", ""))
        }
    }

    override val googleDisponible: Boolean get() = true

    override suspend fun entrarConGoogle(idToken: String, nonce: String?): ResultadoAuth = try {
        abrir(backend.entrarConIdTokenGoogle(idToken, nonce))
    } catch (e: ErrorRemoto) {
        ResultadoAuth.Error(e.message.orEmpty())
    }

    override suspend fun entrarConApple(idToken: String, nonce: String?): ResultadoAuth = try {
        abrir(backend.entrarConIdTokenApple(idToken, nonce))
    } catch (e: ErrorRemoto) {
        ResultadoAuth.Error(e.message.orEmpty())
    }

    override suspend fun actualizarPerfil(perfil: Perfil) {
        val id = userId() ?: return
        valor.actualizar { it?.copy(perfil = perfil) }
        runCatching {
            backend.update("profiles", buildJsonObject {
                put("full_name", perfil.nombre)
                put("username", perfil.username)
                put("bio", perfil.bio)
                put("location", perfil.ubicacion)
                put("age", perfil.edad)
                put("gender", perfil.genero)
            }, mapOf("id" to id))
        }
    }

    override suspend fun cerrarSesion() {
        runCatching { backend.salir() }
        valor.actualizar { null }
    }
}


