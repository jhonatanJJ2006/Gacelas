package com.gacelas

import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.ErrorRemoto
import com.gacelas.data.remoto.UsuarioRemoto
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Supabase falso en memoria para las pruebas (sin red ni secretos). */
class FakeBackend : Backend {
    val tablas = mutableMapOf<String, MutableList<JsonObject>>()
    val usuarios = mutableListOf<Triple<String, String, String>>() // id, email, password
    var actual: UsuarioRemoto? = null
    var sinRed = false
    var confirmarCorreo = false
    var googleActivo = true
    val tokensGoogle = mutableListOf<Pair<String, String?>>()
    private var n = 0

    private val claves = mapOf(
        "activities" to listOf("user_id", "client_id"),
        "activity_tracks" to listOf("activity_id"),
        "user_settings" to listOf("user_id"),
    )

    private fun red() { if (sinRed) throw ErrorRemoto("Sin conexión con el servidor") }
    private fun tabla(t: String) = tablas.getOrPut(t) { mutableListOf() }
    private fun JsonObject.valor(k: String) = (this[k] as? JsonPrimitive)?.contentOrNull
    private fun coincide(f: JsonObject, filtros: Map<String, String>) = filtros.all { (k, v) -> f.valor(k) == v }

    private fun crearUsuario(email: String, password: String, nombre: String?): UsuarioRemoto {
        val id = "u-${++n}"
        usuarios += Triple(id, email, password)
        tabla("profiles") += JsonObject(mapOf("id" to JsonPrimitive(id), "email" to JsonPrimitive(email), "full_name" to JsonPrimitive(nombre ?: email)))
        tabla("user_settings") += JsonObject(mapOf("user_id" to JsonPrimitive(id), "visibility" to JsonPrimitive("followers")))
        return UsuarioRemoto(id, email)
    }

    override suspend fun usuarioActual(): UsuarioRemoto? { red(); return actual }

    override suspend fun entrarConEmail(email: String, password: String): UsuarioRemoto {
        red()
        val u = usuarios.firstOrNull { it.second == email && it.third == password } ?: throw ErrorRemoto("Correo o contraseña incorrectos")
        return UsuarioRemoto(u.first, u.second).also { actual = it }
    }

    override suspend fun registrar(email: String, password: String, datos: JsonObject): UsuarioRemoto? {
        red()
        if (usuarios.any { it.second == email }) throw ErrorRemoto("Ya existe una cuenta con ese correo")
        val u = crearUsuario(email, password, datos.valor("full_name"))
        return if (confirmarCorreo) null else u.also { actual = it }
    }

    override suspend fun recuperarContrasena(email: String) { red() }

    override suspend fun entrarConIdTokenGoogle(idToken: String, nonce: String?): UsuarioRemoto {
        red()
        if (!googleActivo) throw ErrorRemoto("El inicio con Google todavía no está activado en el servidor")
        tokensGoogle += idToken to nonce
        val email = "google@gmail.com"
        val u = usuarios.firstOrNull { it.second == email }?.let { UsuarioRemoto(it.first, it.second) } ?: crearUsuario(email, "", "Cuenta Google")
        return u.also { actual = it }
    }

    override suspend fun salir() { actual = null }

    override suspend fun select(tabla: String, filtros: Map<String, String>, ordenarPor: String?, descendente: Boolean): List<JsonObject> {
        red()
        val filas = tabla(tabla).filter { coincide(it, filtros) }
        return if (ordenarPor == null) filas else filas.sortedBy { it.valor(ordenarPor) }.let { if (descendente) it.reversed() else it }
    }

    override suspend fun insert(tabla: String, filas: List<JsonObject>) {
        red()
        filas.forEach { tabla(tabla) += JsonObject(mapOf("id" to JsonPrimitive("f-${++n}"), "created_at" to JsonPrimitive("%06d".format(n))) + it) }
    }

    override suspend fun upsert(tabla: String, filas: List<JsonObject>, onConflict: String): List<JsonObject> {
        red()
        val k = onConflict.split(",")
        return filas.map { f ->
            val i = tabla(tabla).indexOfFirst { r -> k.all { r.valor(it) == f.valor(it) } }
            if (i >= 0) JsonObject(tabla(tabla)[i] + f).also { tabla(tabla)[i] = it }
            else JsonObject(mapOf("id" to JsonPrimitive("f-${++n}")) + f).also { tabla(tabla) += it }
        }
    }

    override suspend fun update(tabla: String, valores: JsonObject, filtros: Map<String, String>) {
        red()
        val t = tabla(tabla)
        t.indices.filter { coincide(t[it], filtros) }.forEach { t[it] = JsonObject(t[it] + valores) }
    }

    override suspend fun delete(tabla: String, filtros: Map<String, String>) {
        red()
        tabla(tabla).removeAll { coincide(it, filtros) }
    }

    /** Funciones live_* en memoria (sin difuminado: eso se prueba contra Supabase real). */
    override suspend fun rpc(funcion: String, args: JsonObject): kotlinx.serialization.json.JsonElement {
        red()
        val yo = actual?.id ?: throw ErrorRemoto("no autenticado")
        val t = tabla("live_sessions")
        return when (funcion) {
            "live_iniciar" -> {
                val id = "s-${++n}"
                t += JsonObject(mapOf("id" to JsonPrimitive(id), "user_id" to JsonPrimitive(yo), "audience" to JsonPrimitive("followers"),
                    "started_at" to JsonPrimitive("2026-10-11T05:00:00Z"), "hidden" to JsonPrimitive(true)))
                JsonPrimitive(id)
            }
            "live_enviar" -> {
                val id = args.valor("sesion")
                val i = t.indexOfFirst { it.valor("id") == id && it.valor("user_id") == yo }
                if (i < 0) throw ErrorRemoto("sesion no valida")
                t[i] = JsonObject(t[i] + mapOf("hidden" to JsonPrimitive(false), "last_lat" to args.getValue("lat"), "last_lng" to args.getValue("lng"),
                    "distance_m" to args.getValue("distancia_m"), "elapsed_s" to args.getValue("segundos")))
                JsonPrimitive(true)
            }
            "live_terminar" -> {
                val i = t.indexOfFirst { it.valor("id") == args.valor("sesion") && it.valor("user_id") == yo }
                if (i >= 0) t[i] = JsonObject(t[i] + ("ended_at" to JsonPrimitive("2026-10-11T05:30:00Z")))
                kotlinx.serialization.json.JsonNull
            }
            else -> throw ErrorRemoto("funcion desconocida $funcion")
        }
    }
}
