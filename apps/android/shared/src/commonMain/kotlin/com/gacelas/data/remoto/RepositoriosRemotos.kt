package com.gacelas.data.remoto

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
import com.gacelas.data.eventos.EventosRepository
import com.gacelas.data.jsonGacelas
import com.gacelas.data.mock.Evento
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

/*
 * Repositorios con Supabase. La interfaz usa siempre la copia local (DataStore) como cache:
 * se escribe primero en el telefono y luego en el servidor; al abrir sesion se descarga todo.
 * Las carreras sin conexion quedan en una cola y se suben al volver la red.
 */

internal fun JsonObject.texto(k: String): String? = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.contentOrNull
internal fun JsonObject.numero(k: String): Double? = (this[k] as? JsonPrimitive)?.doubleOrNull
internal fun JsonObject.entero(k: String): Int? = (this[k] as? JsonPrimitive)?.intOrNull ?: numero(k)?.toInt()
internal fun JsonObject.logico(k: String): Boolean? = (this[k] as? JsonPrimitive)?.booleanOrNull

/** Fechas ISO 8601 con kotlinx-datetime (multiplataforma). */
object FechaIso {
    fun formatear(ms: Long): String = com.gacelas.shared.isoUtc(ms)

    /** Acepta "2026-10-10T10:00:00Z", "2026-10-10 10:00:00.123+00" y variantes de Postgres. */
    fun leer(texto: String): Long? {
        var t = texto.trim().replace(' ', 'T').replace(Regex("\\.\\d+"), "")
        if (t.endsWith("Z")) t = t.dropLast(1) + "+00:00"
        if (Regex("[+-]\\d{2}$").containsMatchIn(t)) t += ":00"
        return runCatching { kotlinx.datetime.Instant.parse(t).toEpochMilliseconds() }.getOrNull()
    }
}

/* ---------------- Eventos ---------------- */

fun eventoDesdeFila(f: JsonObject, uid: String? = null): Evento {
    val inicio = f.texto("starts_at")?.let(FechaIso::leer)
    return Evento(
        id = f.texto("id").orEmpty(),
        titulo = f.texto("title").orEmpty(),
        descripcion = f.texto("description").orEmpty(),
        fecha = f.texto("date_label").orEmpty(),
        hora = f.texto("time_label").orEmpty(),
        ubicacion = f.texto("location").orEmpty(),
        participantes = f.entero("base_participants") ?: 0,
        distanciaKm = f.numero("distance_km")?.toInt() ?: 0,
        pasado = inicio?.let { it < ahoraMs() } ?: false,
        creadoPorUsuario = uid == null || f.texto("created_by") == uid,
        inicioMs = inicio,
        cupo = f.entero("max_participants"),
        tipo = f.texto("kind") ?: "grupal",
        clubId = f.texto("club_id"),
    )
}

class SupabaseEventosRepository(
    private val backend: Backend,
    store: DataStore<Preferences>,
    private val userId: suspend () -> String?,
) : EventosRepository {
    private val creadosValor = ValorJson(store, "eventos_creados", ListSerializer(Evento.serializer()), emptyList())
    private val inscritosValor = ValorJson(store, "eventos_inscritos", SetSerializer(String.serializer()), emptySet())
    override val creados: Flow<List<Evento>> = creadosValor.flujo
    override val inscritos: Flow<Set<String>> = inscritosValor.flujo

    suspend fun descargar() {
        val uid = userId() ?: return
        // Igual que la web: todos los eventos creados por la comunidad (los de ejemplo vienen en MockData).
        runCatching { backend.select("events", emptyMap(), "starts_at") }.getOrNull()?.let { filas ->
            creadosValor.actualizar { filas.filter { it.texto("created_by") != null }.map { eventoDesdeFila(it, uid) } }
        }
        runCatching { backend.select("event_rsvps", mapOf("user_id" to uid)) }.getOrNull()?.let { filas ->
            inscritosValor.actualizar { filas.mapNotNull { it.texto("event_id") }.toSet() }
        }
    }

    override suspend fun crear(evento: Evento) {
        creadosValor.actualizar { it + evento }
        inscritosValor.actualizar { it + evento.id }
        val uid = userId() ?: return
        runCatching {
            backend.insert("events", listOf(buildJsonObject {
                put("id", evento.id)
                put("title", evento.titulo)
                put("description", evento.descripcion)
                put("date_label", evento.fecha)
                put("time_label", evento.hora)
                put("location", evento.ubicacion)
                put("distance_km", evento.distanciaKm)
                put("base_participants", evento.participantes)
                put("created_by", uid)
                evento.inicioMs?.let { put("starts_at", kotlinx.datetime.Instant.fromEpochMilliseconds(it).toString()) }
                evento.cupo?.let { put("max_participants", it) }
                put("kind", evento.tipo)
                evento.clubId?.let { put("club_id", it) }
            }))
            backend.insert("event_rsvps", listOf(buildJsonObject { put("event_id", evento.id); put("user_id", uid) }))
        }
    }

    override suspend fun alternarInscripcion(id: String) {
        val estaba = id in inscritosValor.flujo.first()
        inscritosValor.actualizar { if (estaba) it - id else it + id }
        val uid = userId() ?: return
        runCatching {
            if (estaba) backend.delete("event_rsvps", mapOf("user_id" to uid, "event_id" to id))
            else backend.insert("event_rsvps", listOf(buildJsonObject { put("event_id", id); put("user_id", uid) }))
        }
    }
}

/** Utilidad para pruebas: arma un JsonObject desde pares. */
fun fila(vararg pares: Pair<String, Any?>): JsonObject = JsonObject(pares.associate { (k, v) ->
    k to when (v) {
        null -> JsonNull
        is String -> JsonPrimitive(v)
        is Number -> JsonPrimitive(v)
        is Boolean -> JsonPrimitive(v)
        is JsonElement -> v
        else -> JsonPrimitive(v.toString())
    }
})

