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
import com.gacelas.data.sync.BorradoSync
import com.gacelas.data.sync.CambioCarrera
import com.gacelas.data.sync.ConsultaPagina
import com.gacelas.data.sync.PAGINA_RECORRIDOS
import com.gacelas.data.sync.bajarTodo
import com.gacelas.data.sync.borradosDe
import com.gacelas.data.sync.fusionarCarreras
import com.gacelas.data.sync.marcaMs
import com.gacelas.data.remoto.entero
import com.gacelas.data.remoto.logico
import com.gacelas.data.remoto.numero
import com.gacelas.data.remoto.texto
import com.gacelas.shared.redondear
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

    /** Ids de carreras guardadas en el celular que aun no estan en el servidor. */
    val pendientesDeSubir: Flow<Set<String>> = cola.flujo.map { ops -> ops.mapNotNull { o -> o.carrera?.id.takeIf { o.tipo == "guardar" } }.toSet() }

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

    /**
     * Baja las carreras. Sin [desde] es completa (paginada). Con marca, solo lo posterior,
     * y [borrados] quita lo que ya no esta en el servidor. False si la red fallo: no se toca la cache.
     */
    suspend fun descargar(desde: String? = null, borrados: List<BorradoSync> = emptyList()): Boolean {
        val uid = userId() ?: return false
        sincronizar()
        val completa = desde == null
        val actividades = runCatching {
            bajarTodo(backend::selectPagina, ConsultaPagina("activities", mapOf("user_id" to uid), marcaExclusiva = desde, columnaClave = "id"))
        }.getOrElse { return false }
        val tracks = runCatching {
            bajarTodo(backend::selectPagina, ConsultaPagina(
                "activity_tracks", mapOf("user_id" to uid), marcaExclusiva = desde,
                columnaClave = "activity_id", limite = PAGINA_RECORRIDOS,
            ))
        }.getOrElse { return false }
        val idACliente = actividades.associate { (it.texto("id") ?: "") to idCliente(it) }.toMutableMap()
        val puntos = mutableMapOf<String, List<PuntoGps>>()
        for (t in tracks) {
            val aid = t.texto("activity_id") ?: continue
            val cid = idACliente[aid] ?: clientIdDe(aid)?.also { idACliente[aid] = it } ?: continue
            puntos[cid] = puntosDe(t)
        }
        val cambios = actividades.map { f ->
            val id = idCliente(f)
            val trae = id in puntos
            CambioCarrera(carreraDesdeFila(f, if (trae) puntos.getValue(id) else emptyList()), marcaMs(f), trae)
        }
        val sueltos = puntos.filterKeys { id -> cambios.none { it.carrera.id == id } }
        val bajasTrack = if (completa) emptyList() else borradosDe(borrados, "activity_tracks")
        val ops = cola.flujo.first()
        cache.actualizar { local ->
            fusionarCarreras(
                local, cambios, sueltos + bajasTrack.associate { it.clave to emptyList() },
                if (completa) emptyList() else borradosDe(borrados, "activities"),
                completa, ops.mapNotNull { it.carrera }, ops.filter { it.tipo == "eliminar" }.mapNotNull { it.id }.toSet(),
            )
        }
        return true
    }

    private fun idCliente(f: JsonObject) = f.texto("client_id") ?: f.texto("id").orEmpty()

    private fun puntosDe(t: JsonObject) =
        t["points"]?.let { runCatching { jsonGacelas.decodeFromJsonElement(ListSerializer(PuntoGps.serializer()), it) }.getOrNull() } ?: emptyList()

    private suspend fun clientIdDe(activityId: String): String? {
        val f = runCatching { backend.select("activities", mapOf("id" to activityId)).firstOrNull() }.getOrNull() ?: return null
        return idCliente(f).ifEmpty { null }
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
