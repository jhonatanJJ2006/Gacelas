package com.gacelas.comunidad.data

import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.ErrorRemoto
import com.gacelas.comunidad.data.eventoDesdeFila
import com.gacelas.shared.Fechas
import com.gacelas.shared.nuevoUuid
import kotlin.concurrent.Volatile
import kotlinx.datetime.plus
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import com.gacelas.comunidad.domain.modelo.*
import com.gacelas.comunidad.domain.reglas.*
import com.gacelas.comunidad.domain.repositorio.ClubesRepository
import com.gacelas.data.remoto.*

private fun JsonObject.texto(k: String) = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.contentOrNull

private fun JsonObject.numero(k: String) = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.let { it.doubleOrNull ?: it.contentOrNull?.toDoubleOrNull() }

private fun s(v: String) = JsonPrimitive(v)

fun clubDesdeFila(f: JsonObject) = Club(
    f.texto("id").orEmpty(), f.texto("name").orEmpty(), f.texto("description").orEmpty(), f.texto("location").orEmpty(),
    f.texto("requires_approval") == "true", f.numero("members_count")?.toInt() ?: 0,
)

class SupabaseClubesRepository(private val backend: Backend, val uid: suspend () -> String?) : ClubesRepository {
    private suspend fun nombre(id: String) = runCatching {
        backend.select("profiles", mapOf("id" to id)).firstOrNull()?.let { it.texto("full_name")?.ifBlank { null } ?: it.texto("username") }
    }.getOrNull() ?: "Corredor"

    override suspend fun miId() = uid()

    override suspend fun listar(): Pair<List<Club>, Set<String>> {
        val todos = backend.select("clubs", emptyMap(), "members_count", descendente = true).map(::clubDesdeFila)
        val yo = uid()
        val mios = if (yo == null) emptySet() else backend.select("club_members", mapOf("user_id" to yo, "status" to "active")).mapNotNull { it.texto("club_id") }.toSet()
        return todos to mios
    }

    override suspend fun crear(nombre: String, descripcion: String, ubicacion: String, conAprobacion: Boolean): String {
        val yo = uid() ?: throw ErrorRemoto("Inicia sesión para crear un club")
        validarClub(nombre, descripcion)?.let { throw ErrorRemoto(it) }
        val id = nuevoUuid()
        backend.insert("clubs", listOf(JsonObject(mapOf("id" to s(id), "name" to s(nombre.trim()), "description" to s(descripcion.trim()),
            "location" to s(ubicacion.trim()), "requires_approval" to JsonPrimitive(conAprobacion), "owner_id" to s(yo)))))
        return id
    }

    override suspend fun detalle(id: String): Club? = backend.select("clubs", mapOf("id" to id)).firstOrNull()?.let(::clubDesdeFila)

    override suspend fun miembros(id: String): List<MiembroClub> = backend.select("club_members", mapOf("club_id" to id), "created_at").map { f ->
        val u = f.texto("user_id").orEmpty()
        MiembroClub(u, nombre(u), runCatching { RolClub.valueOf(f.texto("role").orEmpty()) }.getOrDefault(RolClub.member), f.texto("status") == "active")
    }.sortedBy { it.rol.ordinal }

    override suspend fun unirse(id: String) {
        val yo = uid() ?: throw ErrorRemoto("Inicia sesión para unirte")
        backend.insert("club_members", listOf(JsonObject(mapOf("club_id" to s(id), "user_id" to s(yo)))))
    }
    override suspend fun salir(id: String, usuario: String?) {
        val u = usuario ?: uid() ?: return
        backend.delete("club_members", mapOf("club_id" to id, "user_id" to u))
    }
    override suspend fun aprobar(id: String, usuario: String) { backend.update("club_members", JsonObject(mapOf("status" to s("active"))), mapOf("club_id" to id, "user_id" to usuario)) }
    override suspend fun cambiarRol(id: String, usuario: String, rol: RolClub) { backend.update("club_members", JsonObject(mapOf("role" to s(rol.name))), mapOf("club_id" to id, "user_id" to usuario)) }

    override suspend fun posts(id: String): List<PostClub> = runCatching {
        val filas = backend.select("club_posts", mapOf("club_id" to id), "created_at", descendente = true)
        val nombres = filas.mapNotNull { it.texto("user_id") }.distinct().associateWith { nombre(it) }
        filas.map { PostClub(it.texto("id").orEmpty(), it.texto("user_id").orEmpty(), nombres[it.texto("user_id")] ?: "Corredor", it.texto("body").orEmpty()) }
    }.getOrDefault(emptyList())

    override suspend fun publicar(id: String, texto: String) {
        val yo = uid() ?: return
        backend.insert("club_posts", listOf(JsonObject(mapOf("club_id" to s(id), "user_id" to s(yo), "body" to s(texto.trim())))))
    }

    override suspend fun eventos(id: String): List<Evento> = runCatching { backend.select("events", mapOf("club_id" to id), "starts_at").filter { eventoVisible(it) }.map(::eventoDesdeFila) }.getOrDefault(emptyList())
    override suspend fun evento(id: String): Evento? = runCatching { backend.select("events", mapOf("id" to id)).firstOrNull()?.let(::eventoDesdeFila) }.getOrNull()

    // ---- Eventos: cupo y en vivo ----
    override suspend fun estadoEvento(id: String): EstadoEvento? = runCatching {
        val f = (backend.rpc("evento_estado", JsonObject(mapOf("evento" to s(id)))) as? JsonArray)?.firstOrNull() as? JsonObject ?: return null
        EstadoEvento(f.numero("inscritos")?.toInt() ?: 0, f.numero("en_espera")?.toInt() ?: 0, f.numero("cupo")?.toInt(), f.texto("mi_estado"),
            if (f.texto("mi_estado") == "waitlist") f.numero("mi_posicion")?.toInt() else null)
    }.getOrNull()

    override suspend fun corredores(id: String): List<CorredorEvento> = runCatching {
        (backend.rpc("evento_en_vivo", JsonObject(mapOf("evento" to s(id)))) as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }.map { f ->
            CorredorEvento(
                f.texto("user_id").orEmpty(), f.texto("nombre") ?: "Corredor", f.numero("distance_m") ?: 0.0, f.numero("elapsed_s")?.toInt() ?: 0,
                f.numero("last_lat"), f.numero("last_lng"), f.texto("hidden") == "true", f.texto("ended") == "true",
                (f["points"] as? JsonArray).orEmpty().mapNotNull { p -> (p as? JsonObject)?.let { o -> o.numero("lat")?.let { la -> o.numero("lng")?.let { lo -> la to lo } } } },
            )
        }
    }.getOrDefault(emptyList())
}

/** Club para el que se va a crear un evento (desde el detalle del club). */
object EventoDeClub { @Volatile var id: String? = null }
