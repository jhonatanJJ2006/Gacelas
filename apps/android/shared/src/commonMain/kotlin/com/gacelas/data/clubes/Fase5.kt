package com.gacelas.data.clubes

import com.gacelas.shared.Fechas
import kotlinx.datetime.plus

import com.gacelas.shared.nuevoUuid
import kotlin.concurrent.Volatile

import com.gacelas.data.mock.Evento
import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.ErrorRemoto
import com.gacelas.data.remoto.eventoDesdeFila
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

enum class RolClub(val etiqueta: String) { owner("Fundador"), admin("Admin"), member("Miembro") }

data class Club(val id: String, val nombre: String, val descripcion: String, val ubicacion: String, val conAprobacion: Boolean, val miembros: Int)
data class MiembroClub(val usuarioId: String, val nombre: String, val rol: RolClub, val activo: Boolean)
data class PostClub(val id: String, val usuarioId: String, val nombre: String, val texto: String)

fun puedeGestionar(rol: RolClub?) = rol == RolClub.owner || rol == RolClub.admin

/** Texto del boton principal segun mi relacion con el club (igual que la web). */
fun accionClub(yo: MiembroClub?, conAprobacion: Boolean): String? = when {
    yo == null -> if (conAprobacion) "Solicitar unirme" else "Unirme"
    !yo.activo -> "Solicitud enviada"
    yo.rol == RolClub.owner -> null
    else -> "Salir del club"
}

fun validarClub(nombre: String, descripcion: String): String? = when {
    nombre.trim().length < 3 -> "El nombre debe tener al menos 3 letras"
    nombre.trim().length > 60 -> "El nombre es demasiado largo"
    descripcion.length > 500 -> "La descripción es demasiado larga"
    else -> null
}

private fun JsonObject.texto(k: String) = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.contentOrNull
private fun JsonObject.numero(k: String) = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.let { it.doubleOrNull ?: it.contentOrNull?.toDoubleOrNull() }
private fun s(v: String) = JsonPrimitive(v)

fun clubDesdeFila(f: JsonObject) = Club(
    f.texto("id").orEmpty(), f.texto("name").orEmpty(), f.texto("description").orEmpty(), f.texto("location").orEmpty(),
    f.texto("requires_approval") == "true", f.numero("members_count")?.toInt() ?: 0,
)

class ClubesRepository(private val backend: Backend, val uid: suspend () -> String?) {
    private suspend fun nombre(id: String) = runCatching {
        backend.select("profiles", mapOf("id" to id)).firstOrNull()?.let { it.texto("full_name")?.ifBlank { null } ?: it.texto("username") }
    }.getOrNull() ?: "Corredor"

    suspend fun listar(): Pair<List<Club>, Set<String>> {
        val todos = backend.select("clubs", emptyMap(), "members_count", descendente = true).map(::clubDesdeFila)
        val yo = uid()
        val mios = if (yo == null) emptySet() else backend.select("club_members", mapOf("user_id" to yo, "status" to "active")).mapNotNull { it.texto("club_id") }.toSet()
        return todos to mios
    }

    suspend fun crear(nombre: String, descripcion: String, ubicacion: String, conAprobacion: Boolean): String {
        val yo = uid() ?: throw ErrorRemoto("Inicia sesión para crear un club")
        validarClub(nombre, descripcion)?.let { throw ErrorRemoto(it) }
        val id = nuevoUuid()
        backend.insert("clubs", listOf(JsonObject(mapOf("id" to s(id), "name" to s(nombre.trim()), "description" to s(descripcion.trim()),
            "location" to s(ubicacion.trim()), "requires_approval" to JsonPrimitive(conAprobacion), "owner_id" to s(yo)))))
        return id
    }

    suspend fun detalle(id: String) = backend.select("clubs", mapOf("id" to id)).firstOrNull()?.let(::clubDesdeFila)

    suspend fun miembros(id: String): List<MiembroClub> = backend.select("club_members", mapOf("club_id" to id), "created_at").map { f ->
        val u = f.texto("user_id").orEmpty()
        MiembroClub(u, nombre(u), runCatching { RolClub.valueOf(f.texto("role").orEmpty()) }.getOrDefault(RolClub.member), f.texto("status") == "active")
    }.sortedBy { it.rol.ordinal }

    suspend fun unirse(id: String) {
        val yo = uid() ?: throw ErrorRemoto("Inicia sesión para unirte")
        backend.insert("club_members", listOf(JsonObject(mapOf("club_id" to s(id), "user_id" to s(yo)))))
    }
    suspend fun salir(id: String, usuario: String? = null) {
        val u = usuario ?: uid() ?: return
        backend.delete("club_members", mapOf("club_id" to id, "user_id" to u))
    }
    suspend fun aprobar(id: String, usuario: String) = backend.update("club_members", JsonObject(mapOf("status" to s("active"))), mapOf("club_id" to id, "user_id" to usuario))
    suspend fun cambiarRol(id: String, usuario: String, rol: RolClub) = backend.update("club_members", JsonObject(mapOf("role" to s(rol.name))), mapOf("club_id" to id, "user_id" to usuario))

    suspend fun posts(id: String): List<PostClub> = runCatching {
        val filas = backend.select("club_posts", mapOf("club_id" to id), "created_at", descendente = true)
        val nombres = filas.mapNotNull { it.texto("user_id") }.distinct().associateWith { nombre(it) }
        filas.map { PostClub(it.texto("id").orEmpty(), it.texto("user_id").orEmpty(), nombres[it.texto("user_id")] ?: "Corredor", it.texto("body").orEmpty()) }
    }.getOrDefault(emptyList())

    suspend fun publicar(id: String, texto: String) {
        val yo = uid() ?: return
        backend.insert("club_posts", listOf(JsonObject(mapOf("club_id" to s(id), "user_id" to s(yo), "body" to s(texto.trim())))))
    }

    suspend fun eventos(id: String): List<Evento> = runCatching { backend.select("events", mapOf("club_id" to id), "starts_at").map(::eventoDesdeFila) }.getOrDefault(emptyList())
    suspend fun evento(id: String): Evento? = runCatching { backend.select("events", mapOf("id" to id)).firstOrNull()?.let(::eventoDesdeFila) }.getOrNull()

    // ---- Eventos: cupo y en vivo ----
    suspend fun estadoEvento(id: String): EstadoEvento? = runCatching {
        val f = (backend.rpc("evento_estado", JsonObject(mapOf("evento" to s(id)))) as? JsonArray)?.firstOrNull() as? JsonObject ?: return null
        EstadoEvento(f.numero("inscritos")?.toInt() ?: 0, f.numero("en_espera")?.toInt() ?: 0, f.numero("cupo")?.toInt(), f.texto("mi_estado"),
            if (f.texto("mi_estado") == "waitlist") f.numero("mi_posicion")?.toInt() else null)
    }.getOrNull()

    suspend fun corredores(id: String): List<CorredorEvento> = runCatching {
        (backend.rpc("evento_en_vivo", JsonObject(mapOf("evento" to s(id)))) as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }.map { f ->
            CorredorEvento(
                f.texto("user_id").orEmpty(), f.texto("nombre") ?: "Corredor", f.numero("distance_m") ?: 0.0, f.numero("elapsed_s")?.toInt() ?: 0,
                f.numero("last_lat"), f.numero("last_lng"), f.texto("hidden") == "true", f.texto("ended") == "true",
                (f["points"] as? JsonArray).orEmpty().mapNotNull { p -> (p as? JsonObject)?.let { o -> o.numero("lat")?.let { la -> o.numero("lng")?.let { lo -> la to lo } } } },
            )
        }
    }.getOrDefault(emptyList())
}

data class EstadoEvento(val inscritos: Int, val enEspera: Int, val cupo: Int?, val miEstado: String?, val miPosicion: Int?)

fun textoCupo(e: EstadoEvento): String {
    val cupo = e.cupo ?: return "${e.inscritos} inscritos"
    val libres = maxOf(cupo - e.inscritos, 0)
    return if (libres > 0) "${e.inscritos}/$cupo · $libres ${if (libres == 1) "cupo libre" else "cupos libres"}" else "Lleno · ${e.enEspera} en espera"
}

data class CorredorEvento(
    val usuarioId: String, val nombre: String, val distanciaM: Double, val segundos: Int,
    val lat: Double?, val lng: Double?, val oculto: Boolean, val terminado: Boolean, val puntos: List<Pair<Double, Double>>,
)

fun clasificacion(c: List<CorredorEvento>) = c.sortedWith(compareByDescending<CorredorEvento> { it.distanciaM }.thenBy { it.segundos })

fun posicionRepeticion(c: CorredorEvento, t: Float): Pair<Double, Double>? =
    if (c.puntos.isEmpty()) null else c.puntos[minOf(c.puntos.lastIndex, (t * c.puntos.lastIndex).toInt())]

data class DiaCalendario(val dia: Int, val delMes: Boolean, val eventos: List<Evento>, val inicioMs: Long)

/** Semanas (lunes a domingo) del mes con los eventos de cada dia. */
fun calendarioMes(anio: Int, mes: Int, eventos: List<Evento>, zona: kotlinx.datetime.TimeZone = kotlinx.datetime.TimeZone.currentSystemDefault()): List<List<DiaCalendario>> {
    // mes base 0 (como Calendar.MONTH).
    val porDia = eventos.filter { it.inicioMs != null }.groupBy { e -> Fechas.fecha(e.inicioMs!!, zona) }
    var d = Fechas.lunes(kotlinx.datetime.LocalDate(anio, mes + 1, 1))
    val semanas = mutableListOf<List<DiaCalendario>>()
    do {
        semanas += (0 until 7).map {
            DiaCalendario(d.dayOfMonth, d.monthNumber == mes + 1, porDia[d].orEmpty(), Fechas.inicio(d, zona))
                .also { d = d.plus(kotlinx.datetime.DatePeriod(days = 1)) }
        }
    } while (d.monthNumber == mes + 1)
    return semanas
}

/** Club para el que se va a crear un evento (desde el detalle del club). */
object EventoDeClub { @Volatile var id: String? = null }
