package com.gacelas.comunidad.infrastructure

import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.ErrorRemoto
import com.gacelas.comunidad.infrastructure.eventoDesdeFila
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
    tipo = if (f.texto("kind") == "comunidad") TipoClub.comunidad else TipoClub.grupo,
    ciudad = f.texto("city") ?: f.texto("location")?.ifBlank { null } ?: "Loja",
    comunidadId = f.texto("community_id"),
    logo = f.texto("logo_path"), portada = f.texto("cover_path"),
    contacto = f.texto("contact").orEmpty(),
    lugarEncuentro = f.texto("meeting_place").orEmpty(),
    lat = f.numero("meeting_lat"), lng = f.numero("meeting_lng"),
    dias = (f["training_days"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.toIntOrNull() },
    hora = f.texto("training_time")?.take(5),
    nivel = runCatching { NivelGrupo.valueOf(f.texto("level") ?: "todos") }.getOrDefault(NivelGrupo.todos),
    ritmoSegKm = f.numero("pace_s_km")?.toInt(),
    cupo = f.numero("max_members")?.toInt(),
    propietarioId = f.texto("owner_id"),
)

private fun n(v: Any?): JsonPrimitive? = when (v) { null -> null; is String -> v.trim().ifBlank { null }?.let(::JsonPrimitive); is Number -> JsonPrimitive(v); is Boolean -> JsonPrimitive(v); else -> null }

/** Fila para insert o update; los campos de grupo quedan nulos en comunidades. */
fun filaDesdeBorrador(b: BorradorClub): Map<String, kotlinx.serialization.json.JsonElement> {
    val g = b.tipo == TipoClub.grupo
    val m = mutableMapOf<String, kotlinx.serialization.json.JsonElement>(
        "name" to s(b.nombre.trim().replace(Regex("\\s+"), " ")), "description" to s(b.descripcion.trim()),
        "city" to s(b.ciudad.trim()), "location" to s(b.ciudad.trim()), "contact" to (n(b.contacto) ?: JsonNull),
        "requires_approval" to JsonPrimitive(b.conAprobacion),
        "community_id" to ((if (g) n(b.comunidadId) else null) ?: JsonNull),
        "meeting_place" to ((if (g) n(b.lugarEncuentro) else null) ?: JsonNull),
        "meeting_lat" to ((if (g) n(b.lat) else null) ?: JsonNull), "meeting_lng" to ((if (g) n(b.lng) else null) ?: JsonNull),
        "training_days" to JsonArray(if (g) b.dias.sorted().map { JsonPrimitive(it) } else emptyList()),
        "training_time" to ((if (g) n(b.hora) else null) ?: JsonNull),
        "level" to s(if (g) b.nivel.name else NivelGrupo.todos.name),
        "pace_s_km" to ((if (g) leerRitmo(b.ritmo)?.let(::JsonPrimitive) else null) ?: JsonNull),
        "max_members" to ((if (g) b.cupo.trim().toIntOrNull()?.let(::JsonPrimitive) else null) ?: JsonNull),
    )
    return m
}

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

    override suspend fun crear(b: BorradorClub): String {
        val yo = uid() ?: throw ErrorRemoto("Inicia sesión para registrar")
        val existentes = runCatching { backend.select("clubs", mapOf("kind" to b.tipo.clave)).map(::clubDesdeFila) }.getOrDefault(emptyList())
        validarClub(b, existentes)?.let { throw ErrorRemoto(it) }
        val id = nuevoUuid()
        runCatching {
            backend.insert("clubs", listOf(JsonObject(filaDesdeBorrador(b) + mapOf("id" to s(id), "kind" to s(b.tipo.clave), "owner_id" to s(yo)))))
        }.onFailure { throw ErrorRemoto(mensajeErrorClub(it.message)) }
        return id
    }

    override suspend fun editar(id: String, b: BorradorClub) {
        val existentes = runCatching { backend.select("clubs", mapOf("kind" to b.tipo.clave)).map(::clubDesdeFila) }.getOrDefault(emptyList())
        validarClub(b, existentes, id)?.let { throw ErrorRemoto(it) }
        runCatching { backend.update("clubs", JsonObject(filaDesdeBorrador(b)), mapOf("id" to id)) }
            .onFailure { throw ErrorRemoto(mensajeErrorClub(it.message)) }
    }

    override suspend fun eliminar(id: String) { backend.delete("clubs", mapOf("id" to id)) }

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
