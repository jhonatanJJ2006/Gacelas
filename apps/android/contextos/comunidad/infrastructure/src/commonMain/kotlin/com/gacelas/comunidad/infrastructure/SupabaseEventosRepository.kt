package com.gacelas.comunidad.infrastructure

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.comunidad.domain.modelo.Evento
import com.gacelas.comunidad.domain.repositorio.EventosRepository
import com.gacelas.data.ValorJson
import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.FechaIso
import com.gacelas.data.sync.BorradoSync
import com.gacelas.data.sync.CambioClave
import com.gacelas.data.sync.ConsultaPagina
import com.gacelas.data.sync.bajarTodo
import com.gacelas.data.sync.borradosDe
import com.gacelas.data.sync.fusionarClaves
import com.gacelas.data.sync.fusionarFilas
import com.gacelas.data.sync.marcaMs
import com.gacelas.data.remoto.entero
import com.gacelas.data.remoto.numero
import com.gacelas.data.remoto.texto
import com.gacelas.shared.ahoraMs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/* ---------------- Eventos ---------------- */

/** Oculta borradores (published = false) salvo a su creador; refuerza la RLS de 021. */
fun eventoVisible(f: JsonObject, uid: String? = null): Boolean =
    (f["published"] as? kotlinx.serialization.json.JsonPrimitive)?.content != "false" || (uid != null && f.texto("created_by") == uid)

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

    /**
     * Sin [desde] reemplaza la lista. Con marca, solo eventos e inscripciones posteriores,
     * mas los [borrados]. Un evento que dejo de ser visible se quita igual que un DELETE.
     * El conteo de participantes se refresca solo si vino algun cambio.
     */
    suspend fun descargar(desde: String? = null, borrados: List<BorradoSync> = emptyList()): Boolean {
        val uid = userId() ?: return false
        val completa = desde == null
        val filas = runCatching {
            bajarTodo(backend::selectPagina, ConsultaPagina("events", marcaExclusiva = desde, columnaClave = "id"))
        }.getOrElse { return false }
        val rsvps = runCatching {
            bajarTodo(backend::selectPagina, ConsultaPagina("event_rsvps", mapOf("user_id" to uid), marcaExclusiva = desde, columnaClave = "event_id"))
        }.getOrElse { return false }
        val bajasEventos = if (completa) emptyList() else borradosDe(borrados, "events")
        val hayCambios = completa || filas.isNotEmpty() || rsvps.isNotEmpty() || bajasEventos.isNotEmpty() || borrados.any { it.tabla == "event_rsvps" }
        val otros: Map<String, Int>? = if (!hayCambios) null else {
            runCatching { backend.rpc("participantes_eventos", buildJsonObject { }) }.getOrNull()
                .let { it as? kotlinx.serialization.json.JsonArray }.orEmpty()
                .mapNotNull { it as? JsonObject }
                .mapNotNull { f -> f.texto("event_id")?.let { id -> id to (f.entero("otros") ?: 0) } }.toMap()
        }
        val cambios = filas.filter { eventoVisible(it, uid) }.map { f ->
            val e = eventoDesdeFila(f, uid).let { ev -> otros?.get(ev.id)?.let { ev.copy(participantes = it) } ?: ev }
            e to marcaMs(f)
        }
        val ocultos = filas.filter { !eventoVisible(it, uid) }.mapNotNull { f -> f.texto("id")?.let { CambioClave(it, marcaMs(f)) } }
        creadosValor.actualizar { local ->
            fusionarFilas(local, { it.id }, cambios, if (completa) emptyList() else bajasEventos + ocultos, completa)
                .let { lista -> if (otros == null) lista else lista.map { e -> otros[e.id]?.let { e.copy(participantes = it) } ?: e } }
                .sortedBy { it.inicioMs ?: Long.MAX_VALUE }
        }
        val altas = rsvps.mapNotNull { f -> f.texto("event_id")?.let { CambioClave(it, marcaMs(f)) } }
        inscritosValor.actualizar { fusionarClaves(it, altas, if (completa) emptyList() else borradosDe(borrados, "event_rsvps"), completa) }
        return true
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
