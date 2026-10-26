package com.gacelas.actividad.infrastructure

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.actividad.domain.modelo.Carrera
import com.gacelas.actividad.domain.modelo.Parcial
import com.gacelas.actividad.domain.modelo.PuntoGps
import com.gacelas.actividad.domain.repositorio.CarrerasRepository
import com.gacelas.data.ValorJson
import com.gacelas.data.jsonGacelas
import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.FechaIso
import com.gacelas.data.remoto.entero
import com.gacelas.data.remoto.logico
import com.gacelas.data.remoto.numero
import com.gacelas.data.remoto.texto
import com.gacelas.shared.redondear
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Carreras en Supabase (activities + activity_tracks) con cache local y cola sin conexion. */
@Serializable
data class OperacionPendiente(val tipo: String, val carrera: Carrera? = null, val id: String? = null)

fun filaDesdeCarrera(c: Carrera, userId: String): JsonObject = buildJsonObject {
    put("user_id", userId)
    put("client_id", c.id)
    put("title", c.titulo)
    put("description", c.nota)
    put("is_public", c.publica)
    put("start_time", FechaIso.formatear(c.inicioMs))
    put("end_time", FechaIso.formatear(c.inicioMs + c.segundos * 1000L))
    put("duration_seconds", c.segundos)
    put("distance_meters", redondear(c.distanciaKm * 1000).toInt())
    c.ritmoSegKm?.let { put("avg_pace_seconds_per_km", it) }
    put("elevation_gain_meters", c.desnivelM)
    put("splits", jsonGacelas.encodeToJsonElement(ListSerializer(Parcial.serializer()), c.parciales))
}

fun carreraDesdeFila(f: JsonObject, puntos: List<PuntoGps>): Carrera = Carrera(
    id = f.texto("client_id") ?: f.texto("id").orEmpty(),
    titulo = f.texto("title").orEmpty(),
    nota = f.texto("description").orEmpty(),
    publica = f.logico("is_public") ?: false,
    inicioMs = f.texto("start_time")?.let(FechaIso::leer) ?: 0L,
    segundos = f.entero("duration_seconds") ?: 0,
    distanciaKm = (f.numero("distance_meters") ?: 0.0) / 1000.0,
    desnivelM = f.entero("elevation_gain_meters") ?: 0,
    parciales = f["splits"]?.let { runCatching { jsonGacelas.decodeFromJsonElement(ListSerializer(Parcial.serializer()), it) }.getOrNull() } ?: emptyList(),
    puntos = puntos,
)

class SupabaseCarrerasRepository(
    private val backend: Backend,
    store: DataStore<Preferences>,
    private val userId: suspend () -> String?,
    /** Se llama si algo queda pendiente (programa SincronizacionWorker). */
    private val alEncolar: () -> Unit = {},
) : CarrerasRepository {
    private val cache = ValorJson(store, "carreras", ListSerializer(Carrera.serializer()), emptyList())
    private val cola = ValorJson(store, "carreras_pendientes", ListSerializer(OperacionPendiente.serializer()), emptyList())
    override val carreras: Flow<List<Carrera>> = cache.flujo
    val pendientes: Flow<List<OperacionPendiente>> = cola.flujo

    private suspend fun subir(op: OperacionPendiente, uid: String) {
        if (op.tipo == "eliminar") {
            backend.delete("activities", mapOf("user_id" to uid, "client_id" to op.id.orEmpty()))
            return
        }
        val c = op.carrera ?: return
        val fila = backend.upsert("activities", listOf(filaDesdeCarrera(c, uid)), "user_id,client_id").first()
        backend.upsert("activity_tracks", listOf(buildJsonObject {
            put("activity_id", fila.texto("id").orEmpty())
            put("user_id", uid)
            put("points", jsonGacelas.encodeToJsonElement(ListSerializer(PuntoGps.serializer()), c.puntos))
        }), "activity_id")
    }

    /** Sube la cola en orden; se detiene en el primer fallo. Devuelve cuantas quedan. */
    suspend fun sincronizar(): Int {
        val uid = userId() ?: return cola.flujo.first().size
        val ops = cola.flujo.first()
        var hechas = 0
        for (op in ops) {
            if (runCatching { subir(op, uid) }.isFailure) break
            hechas++
        }
        if (hechas > 0) cola.actualizar { it.drop(hechas) }
        return ops.size - hechas
    }

    /** Descarga las carreras del servidor y las une con las pendientes. */
    suspend fun descargar() {
        val uid = userId() ?: return
        sincronizar()
        val filas = runCatching { backend.select("activities", mapOf("user_id" to uid), "start_time", descendente = true) }.getOrNull() ?: return
        val tracks = runCatching { backend.select("activity_tracks", mapOf("user_id" to uid)) }.getOrDefault(emptyList())
        val puntos = tracks.associate { t ->
            t.texto("activity_id").orEmpty() to (t["points"]?.let { runCatching { jsonGacelas.decodeFromJsonElement(ListSerializer(PuntoGps.serializer()), it) }.getOrNull() } ?: emptyList())
        }
        val remotas = filas.map { carreraDesdeFila(it, puntos[it.texto("id")] ?: emptyList()) }
        val ops = cola.flujo.first()
        val nuevas = ops.mapNotNull { it.carrera }
        val borradas = ops.filter { it.tipo == "eliminar" }.mapNotNull { it.id }.toSet()
        cache.actualizar {
            (nuevas + remotas.filter { r -> nuevas.none { it.id == r.id } }).filterNot { it.id in borradas }.sortedByDescending { it.inicioMs }
        }
    }

    override suspend fun guardar(carrera: Carrera) {
        cache.actualizar { lista -> listOf(carrera) + lista.filterNot { it.id == carrera.id } }
        cola.actualizar { it.filterNot { o -> o.carrera?.id == carrera.id } + OperacionPendiente("guardar", carrera = carrera) }
        if (sincronizar() > 0) alEncolar()
    }

    override suspend fun eliminar(id: String) {
        cache.actualizar { lista -> lista.filterNot { it.id == id } }
        cola.actualizar { it.filterNot { o -> o.carrera?.id == id } + OperacionPendiente("eliminar", id = id) }
        if (sincronizar() > 0) alEncolar()
    }
}
