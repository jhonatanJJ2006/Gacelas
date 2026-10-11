package com.gacelas.data.rutas

import com.gacelas.data.mock.Dificultad
import com.gacelas.data.mock.Ruta
import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.ErrorRemoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.cos
import kotlin.math.hypot

/** Etiquetas permitidas (mismas que la web y el CHECK de routes). */
val ETIQUETAS_RUTA = linkedMapOf(
    "panoramica" to "Panorámica", "plana" to "Plana", "urbana" to "Urbana", "sendero" to "Sendero",
    "montana" to "Montaña", "parque" to "Parque", "rio" to "Río",
)

enum class FiltroDistancia(val etiqueta: String) { Todas("Toda distancia"), Hasta5("Hasta 5 km"), De5a10("5-10 km"), Mas10("Más de 10 km") }

data class FiltrosRuta(val dificultad: Dificultad? = null, val distancia: FiltroDistancia = FiltroDistancia.Todas, val etiqueta: String? = null)

fun aplicarFiltros(rutas: List<Ruta>, f: FiltrosRuta) = rutas.filter { r ->
    (f.dificultad == null || r.dificultad == f.dificultad) &&
        when (f.distancia) {
            FiltroDistancia.Todas -> true
            FiltroDistancia.Hasta5 -> r.distanciaKm <= 5
            FiltroDistancia.De5a10 -> r.distanciaKm > 5 && r.distanciaKm <= 10
            FiltroDistancia.Mas10 -> r.distanciaKm > 10
        } &&
        (f.etiqueta == null || f.etiqueta in r.etiquetas)
}

fun ordenarPorCercania(rutas: List<Ruta>, cerca: Map<String, Double>) = rutas.sortedBy { cerca[it.id] ?: Double.MAX_VALUE }

private fun distanciaASegmentoM(p: Pair<Double, Double>, a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
    val kx = 111_320 * cos(Math.toRadians(a.first)); val ky = 110_540.0
    val px = (p.second - a.second) * kx; val py = (p.first - a.first) * ky
    val bx = (b.second - a.second) * kx; val by = (b.first - a.first) * ky
    val l2 = bx * bx + by * by
    val t = if (l2 == 0.0) 0.0 else ((px * bx + py * by) / l2).coerceIn(0.0, 1.0)
    return hypot(px - t * bx, py - t * by)
}

/** Douglas-Peucker (lat, lng): igual que la web (lib/rutas.ts). */
fun simplificar(puntos: List<Pair<Double, Double>>, toleranciaM: Double = 8.0): List<Pair<Double, Double>> {
    if (puntos.size <= 2) return puntos
    val conservar = BooleanArray(puntos.size).also { it[0] = true; it[puntos.lastIndex] = true }
    val pila = ArrayDeque(listOf(0 to puntos.lastIndex))
    while (pila.isNotEmpty()) {
        val (i, j) = pila.removeLast()
        var max = -1.0; var k = -1
        for (x in i + 1 until j) {
            val d = distanciaASegmentoM(puntos[x], puntos[i], puntos[j])
            if (d > max) { max = d; k = x }
        }
        if (max > toleranciaM && k > 0) { conservar[k] = true; pila.add(i to k); pila.add(k to j) }
    }
    return puntos.filterIndexed { i, _ -> conservar[i] }
}

fun trazoParaRuta(puntos: List<Pair<Double, Double>>, max: Int = 500): List<Pair<Double, Double>> {
    var tol = 5.0
    var r = simplificar(puntos, tol)
    while (r.size > max) { tol *= 2; r = simplificar(puntos, tol) }
    return r
}

data class ResenaRuta(val id: String, val usuarioId: String, val nombre: String, val estrellas: Int, val comentario: String)

private fun JsonObject.texto(k: String) = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.contentOrNull
private fun JsonObject.numero(k: String) = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.let { it.doubleOrNull ?: it.contentOrNull?.toDoubleOrNull() }

fun rutaDesdeFila(f: JsonObject, yo: String? = null) = Ruta(
    id = f.texto("id").orEmpty(),
    nombre = f.texto("name").orEmpty(),
    distanciaKm = f.numero("distance_km") ?: 0.0,
    dificultad = Dificultad.entries.firstOrNull { it.etiqueta == f.texto("difficulty") } ?: Dificultad.Moderado,
    elevacionM = f.numero("elevation_m")?.toInt() ?: 0,
    descripcion = f.texto("description").orEmpty(),
    autorId = f.texto("author_id") ?: "gacelas",
    etiquetas = (f["tags"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty(),
    puntos = (f["path"] as? JsonArray)?.mapNotNull { e ->
        val o = e as? JsonObject ?: return@mapNotNull null
        val a = o.numero("lat"); val b = o.numero("lng")
        if (a != null && b != null) a to b else null
    }.orEmpty(),
    meGusta = f.numero("likes_count")?.toInt() ?: 0,
    valoracion = f.numero("rating_avg"),
    resenas = f.numero("rating_count")?.toInt() ?: 0,
    propia = yo != null && f.texto("author_id") == yo,
    perfilElevacion = (f["elevation_profile"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.doubleOrNull?.toInt() }.orEmpty(),
)

/** Rutas de la comunidad en Supabase (backend/migrations/009_rutas.sql). */
class ExplorarRepository(
    private val backend: Backend,
    /** Copia local y cola de pendientes; null en pruebas antiguas. */
    val offline: RutasOffline? = null,
    /** Se llama al dejar algo pendiente (programa SincronizacionWorker). */
    private val alEncolar: () -> Unit = {},
    private val uid: suspend () -> String?,
) {
    private val _rutas = MutableStateFlow<List<Ruta>?>(null)
    /** null hasta cargar o si el servidor falla (se usan las de ejemplo). */
    val rutas: StateFlow<List<Ruta>?> = _rutas
    private val _meGusta = MutableStateFlow<Set<String>>(emptySet())
    val meGusta: StateFlow<Set<String>> = _meGusta

    suspend fun refrescar() {
        val yo = uid()
        runCatching { backend.select("routes", emptyMap(), "created_at") }.onSuccess { filas ->
            val lista = filas.map { rutaDesdeFila(it, yo) }
            val autores = lista.map { it.autorId }.filter { it != "gacelas" }.distinct()
            val nombres = autores.associateWith { a -> runCatching { backend.select("profiles", mapOf("id" to a)).firstOrNull()?.let { it.texto("full_name") ?: it.texto("username") } }.getOrNull() }
            val conNombre = lista.map { it.copy(autorNombre = nombres[it.autorId]) }
            offline?.guardarDelServidor(conNombre)
            _rutas.value = (offline?.rutasAhora() ?: conNombre).ifEmpty { null }
        }.onFailure {
            // Sin red: se muestran las rutas guardadas en el celular.
            if (_rutas.value == null) _rutas.value = offline?.rutasAhora()?.ifEmpty { null }
        }
        if (yo != null) runCatching { backend.select("route_likes", mapOf("user_id" to yo)) }
            .onSuccess { f -> _meGusta.value = f.mapNotNull { it.texto("route_id") }.toSet() }
    }

    suspend fun cercanas(lat: Double, lng: Double): Map<String, Double> = runCatching {
        val r = backend.rpc("rutas_cercanas", JsonObject(mapOf("lat" to JsonPrimitive(lat), "lng" to JsonPrimitive(lng), "radio_km" to JsonPrimitive(50))))
        (r as? JsonArray)?.mapNotNull { e -> (e as? JsonObject)?.let { o -> o.texto("id")?.let { it to (o.numero("distancia_km") ?: 0.0) } } }?.toMap().orEmpty()
    }.getOrDefault(emptyMap())

    suspend fun crear(nombre: String, descripcion: String, dificultad: Dificultad, etiquetas: List<String>, puntos: List<Pair<Double, Double>>, elevacionM: Int, actividadId: String?, perfil: List<Int> = emptyList(), origen: String = "actividad"): String {
        val yo = uid() ?: throw ErrorRemoto("Inicia sesión para publicar rutas")
        val nueva = RutaNueva(java.util.UUID.randomUUID().toString(), nombre.take(80), descripcion, dificultad, etiquetas, trazoParaRuta(puntos), elevacionM, actividadId, perfil, origen)
        val ok = runCatching { insertar(nueva, yo) }
        if (ok.isFailure) {
            val local = offline ?: throw ok.exceptionOrNull()!!
            // Sin red: queda guardada en el celular y se publica sola al volver la conexion.
            val ruta = nueva.aRuta(yo)
            local.guardarRuta(ruta)
            local.encolar(OperacionRuta("crear", nueva.id, nueva = nueva))
            _rutas.value = listOf(ruta) + (_rutas.value ?: emptyList()).filterNot { it.id == ruta.id }
            alEncolar()
            return nueva.id
        }
        refrescar()
        return nueva.id
    }

    /** true si la ultima ruta creada quedo pendiente de subir. */
    suspend fun pendiente(id: String): Boolean = offline?.pendientes?.first()?.any { it.rutaId == id && it.tipo == "crear" } == true

    private suspend fun insertar(n: RutaNueva, yo: String) {
        backend.insert("routes", listOf(JsonObject(mapOf(
            "name" to JsonPrimitive(n.nombre), "description" to JsonPrimitive(n.descripcion), "difficulty" to JsonPrimitive(n.dificultad.etiqueta),
            "tags" to JsonArray(n.etiquetas.map(::JsonPrimitive)),
            "path" to JsonArray(n.puntos.map { JsonObject(mapOf("lat" to JsonPrimitive(it.first), "lng" to JsonPrimitive(it.second))) }),
            "elevation_m" to JsonPrimitive(n.elevacionM), "distance_km" to JsonPrimitive(0), "author_id" to JsonPrimitive(yo), "is_public" to JsonPrimitive(true),
            "source_activity_id" to (n.actividadId?.let(::JsonPrimitive) ?: JsonNull),
            "elevation_profile" to JsonArray(n.perfil.map(::JsonPrimitive)), "origin" to JsonPrimitive(n.origen),
            "id" to JsonPrimitive(n.id),
        ))))
    }

    /** Sube rutas creadas y corridas registradas sin red. Devuelve cuantas operaciones quedan. */
    suspend fun subirPendientes(): Int {
        val local = offline ?: return 0
        val yo = uid() ?: return local.pendientes.first().size
        val quedan = local.procesar { op ->
            when (op.tipo) {
                "crear" -> op.nueva?.let { insertar(it, yo) }
                "corrida" -> backend.insert("route_runs", listOf(JsonObject(mapOf("route_id" to JsonPrimitive(op.rutaId), "user_id" to JsonPrimitive(yo), "activity_id" to JsonPrimitive(op.actividadId.orEmpty())))))
                "guardar" -> backend.insert("saved_routes", listOf(JsonObject(mapOf("user_id" to JsonPrimitive(yo), "route_id" to JsonPrimitive(op.rutaId)))))
                "quitar" -> backend.delete("saved_routes", mapOf("user_id" to yo, "route_id" to op.rutaId))
            }
        }
        runCatching { refrescar() }
        return quedan
    }

    suspend fun fotos(id: String): List<String> = runCatching {
        backend.select("route_photos", mapOf("route_id" to id), "created_at", descendente = true).mapNotNull { it.texto("path") }.map { backend.urlPublica("route-photos", it) }
    }.getOrDefault(emptyList())

    suspend fun subirFoto(id: String, bytes: ByteArray, tipo: String) {
        val yo = uid() ?: throw ErrorRemoto("Inicia sesión para subir fotos")
        if (bytes.size > 5 * 1024 * 1024) throw ErrorRemoto("La foto pesa más de 5 MB")
        val ext = when (tipo) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
        val ruta = "$yo/${id.filter { it.isLetterOrDigit() || it == '-' }}/${java.util.UUID.randomUUID()}.$ext"
        backend.subirArchivo("route-photos", ruta, bytes, tipo)
        backend.insert("route_photos", listOf(JsonObject(mapOf("route_id" to JsonPrimitive(id), "user_id" to JsonPrimitive(yo), "path" to JsonPrimitive(ruta)))))
    }

    /** (nombre, veces) visibles y total de personas. */
    suspend fun corredores(id: String): Pair<List<Pair<String, Int>>, Int> = runCatching {
        val filas = (backend.rpc("corredores_ruta", JsonObject(mapOf("ruta" to JsonPrimitive(id)))) as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
        filas.map { (it.texto("nombre") ?: it.texto("usuario") ?: "Corredor") to (it.numero("veces")?.toInt() ?: 1) } to (filas.firstOrNull()?.numero("total")?.toInt() ?: 0)
    }.getOrDefault(emptyList<Pair<String, Int>>() to 0)

    suspend fun registrarCorrida(rutaId: String, actividadId: String) {
        val yo = uid() ?: return
        runCatching { backend.insert("route_runs", listOf(JsonObject(mapOf("route_id" to JsonPrimitive(rutaId), "user_id" to JsonPrimitive(yo), "activity_id" to JsonPrimitive(actividadId))))) }
            .onFailure { offline?.let { it.encolar(OperacionRuta("corrida", rutaId, actividadId = actividadId)); alEncolar() } }
    }

    suspend fun alternarMeGusta(id: String) {
        val yo = uid() ?: throw ErrorRemoto("Inicia sesión para dar me gusta")
        val activo = id !in _meGusta.value
        _meGusta.value = if (activo) _meGusta.value + id else _meGusta.value - id
        _rutas.value = _rutas.value?.map { if (it.id == id) it.copy(meGusta = (it.meGusta + if (activo) 1 else -1).coerceAtLeast(0)) else it }
        if (activo) backend.insert("route_likes", listOf(JsonObject(mapOf("user_id" to JsonPrimitive(yo), "route_id" to JsonPrimitive(id)))))
        else backend.delete("route_likes", mapOf("user_id" to yo, "route_id" to id))
    }

    suspend fun resenas(id: String): List<ResenaRuta> = runCatching {
        backend.select("route_reviews", mapOf("route_id" to id), "created_at", descendente = true).map { f ->
            val u = f.texto("user_id").orEmpty()
            val nombre = runCatching { backend.select("profiles", mapOf("id" to u)).firstOrNull()?.let { it.texto("full_name") ?: it.texto("username") } }.getOrNull()
            ResenaRuta(f.texto("id").orEmpty(), u, nombre ?: "Corredor", f.numero("rating")?.toInt() ?: 0, f.texto("comment").orEmpty())
        }
    }.getOrDefault(emptyList())

    suspend fun resenar(id: String, estrellas: Int, comentario: String) {
        val yo = uid() ?: throw ErrorRemoto("Inicia sesión para reseñar")
        require(estrellas in 1..5)
        backend.upsert("route_reviews", listOf(JsonObject(mapOf(
            "route_id" to JsonPrimitive(id), "user_id" to JsonPrimitive(yo), "rating" to JsonPrimitive(estrellas), "comment" to JsonPrimitive(comentario.trim().take(500)),
        ))), "route_id,user_id")
        refrescar()
    }
}

