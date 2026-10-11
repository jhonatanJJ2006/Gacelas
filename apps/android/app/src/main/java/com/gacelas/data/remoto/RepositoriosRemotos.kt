package com.gacelas.data.remoto

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.data.ValorJson
import com.gacelas.data.ajustes.Ajustes
import com.gacelas.data.ajustes.AjustesRepository
import com.gacelas.data.ajustes.Visibilidad
import com.gacelas.data.auth.AuthRepository
import com.gacelas.data.auth.Perfil
import com.gacelas.data.auth.ResultadoAuth
import com.gacelas.data.auth.ValidacionAuth
import com.gacelas.data.carreras.CarreraGuardada
import com.gacelas.data.carreras.CarrerasRepository
import com.gacelas.data.carreras.Parcial
import com.gacelas.data.carreras.PuntoGps
import com.gacelas.data.eventos.EventosRepository
import com.gacelas.data.jsonGacelas
import com.gacelas.data.mock.Evento
import com.gacelas.data.rutas.RutasRepository
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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/*
 * Repositorios con Supabase. La interfaz usa siempre la copia local (DataStore) como cache:
 * se escribe primero en el telefono y luego en el servidor; al abrir sesion se descarga todo.
 * Las carreras sin conexion quedan en una cola y se suben al volver la red.
 */

private fun JsonObject.texto(k: String): String? = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.contentOrNull
private fun JsonObject.numero(k: String): Double? = (this[k] as? JsonPrimitive)?.doubleOrNull
private fun JsonObject.entero(k: String): Int? = (this[k] as? JsonPrimitive)?.intOrNull ?: numero(k)?.toInt()
private fun JsonObject.logico(k: String): Boolean? = (this[k] as? JsonPrimitive)?.booleanOrNull

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

/** Fechas ISO 8601 sin java.time (minSdk 24). */
object FechaIso {
    private fun formato() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

    fun formatear(ms: Long): String = formato().format(java.util.Date(ms))

    /** Acepta "2026-10-10T10:00:00Z", "2026-10-10 10:00:00.123+00" y variantes de Postgres. */
    fun leer(texto: String): Long? {
        var t = texto.trim().replace(' ', 'T').replace(Regex("\\.\\d+"), "")
        if (t.endsWith("Z")) t = t.dropLast(1) + "+00:00"
        if (Regex("[+-]\\d{2}$").containsMatchIn(t)) t += ":00"
        return runCatching { formato().parse(t)?.time }.getOrNull()
    }
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

/* ---------------- Carreras ---------------- */

@Serializable
data class OperacionPendiente(val tipo: String, val carrera: CarreraGuardada? = null, val id: String? = null)

fun filaDesdeCarrera(c: CarreraGuardada, userId: String): JsonObject = buildJsonObject {
    put("user_id", userId)
    put("client_id", c.id)
    put("title", c.titulo)
    put("description", c.nota)
    put("is_public", c.publica)
    put("start_time", FechaIso.formatear(c.inicioMs))
    put("end_time", FechaIso.formatear(c.inicioMs + c.segundos * 1000L))
    put("duration_seconds", c.segundos)
    put("distance_meters", Math.round(c.distanciaKm * 1000).toInt())
    c.ritmoSegKm?.let { put("avg_pace_seconds_per_km", it) }
    put("elevation_gain_meters", c.desnivelM)
    put("splits", jsonGacelas.encodeToJsonElement(ListSerializer(Parcial.serializer()), c.parciales))
}

fun carreraDesdeFila(f: JsonObject, puntos: List<PuntoGps>): CarreraGuardada = CarreraGuardada(
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
) : CarrerasRepository {
    private val cache = ValorJson(store, "carreras", ListSerializer(CarreraGuardada.serializer()), emptyList())
    private val cola = ValorJson(store, "carreras_pendientes", ListSerializer(OperacionPendiente.serializer()), emptyList())
    override val carreras: Flow<List<CarreraGuardada>> = cache.flujo
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

    override suspend fun guardar(carrera: CarreraGuardada) {
        cache.actualizar { lista -> listOf(carrera) + lista.filterNot { it.id == carrera.id } }
        cola.actualizar { it.filterNot { o -> o.carrera?.id == carrera.id } + OperacionPendiente("guardar", carrera = carrera) }
        sincronizar()
    }

    override suspend fun eliminar(id: String) {
        cache.actualizar { lista -> lista.filterNot { it.id == id } }
        cola.actualizar { it.filterNot { o -> o.carrera?.id == id } + OperacionPendiente("eliminar", id = id) }
        sincronizar()
    }
}

/* ---------------- Rutas ---------------- */

class SupabaseRutasRepository(
    private val backend: Backend,
    store: DataStore<Preferences>,
    private val userId: suspend () -> String?,
) : RutasRepository {
    private val valor = ValorJson(store, "rutas_guardadas", SetSerializer(String.serializer()), emptySet())
    override val guardadas: Flow<Set<String>> = valor.flujo

    suspend fun descargar() {
        val uid = userId() ?: return
        val filas = runCatching { backend.select("saved_routes", mapOf("user_id" to uid)) }.getOrNull() ?: return
        valor.actualizar { filas.mapNotNull { it.texto("route_id") }.toSet() }
    }

    override suspend fun alternarGuardada(id: String) {
        val estaba = id in valor.flujo.first()
        valor.actualizar { if (estaba) it - id else it + id }
        val uid = userId() ?: return
        runCatching {
            if (estaba) backend.delete("saved_routes", mapOf("user_id" to uid, "route_id" to id))
            else backend.insert("saved_routes", listOf(buildJsonObject { put("user_id", uid); put("route_id", id) }))
        }
    }
}

/* ---------------- Ajustes, likes y comentarios ---------------- */

private val VIS_A_DB = mapOf(Visibilidad.Publico to "public", Visibilidad.Seguidores to "followers", Visibilidad.Privado to "private")

class SupabaseAjustesRepository(
    private val backend: Backend,
    store: DataStore<Preferences>,
    private val userId: suspend () -> String?,
) : AjustesRepository {
    private val valor = ValorJson(store, "ajustes", Ajustes.serializer(), Ajustes())
    override val ajustes: Flow<Ajustes> = valor.flujo

    suspend fun descargar() {
        val uid = userId() ?: return
        val fila = runCatching { backend.select("user_settings", mapOf("user_id" to uid)).firstOrNull() }.getOrNull()
        val likes = runCatching { backend.select("post_likes", mapOf("user_id" to uid)) }.getOrNull() ?: return
        val comentarios = runCatching { backend.select("post_comments", mapOf("user_id" to uid), "created_at") }.getOrNull() ?: return
        val prefs = fila?.get("prefs")?.let { runCatching { jsonGacelas.decodeFromJsonElement(Ajustes.serializer(), it) }.getOrNull() }
        valor.actualizar { actual ->
            val base = prefs ?: actual
            base.copy(
                visibilidad = VIS_A_DB.entries.firstOrNull { it.value == fila?.texto("visibility") }?.key ?: base.visibilidad,
                ocultarHogar = fila?.logico("hide_home") ?: base.ocultarHogar,
                radioHogarM = fila?.entero("home_radius_m") ?: base.radioHogarM,
                ubicacionPrecisa = fila?.logico("precise_location") ?: base.ubicacionPrecisa,
                metaSemanal = fila?.entero("weekly_goal_runs") ?: base.metaSemanal,
                likes = likes.mapNotNull { it.texto("post_id") }.toSet(),
                comentarios = comentarios.groupBy({ it.texto("post_id").orEmpty() }, { it.texto("body").orEmpty() }),
            )
        }
    }

    override suspend fun actualizar(transformar: (Ajustes) -> Ajustes) {
        val antes = valor.flujo.first()
        valor.actualizar(transformar)
        val nuevos = valor.flujo.first()
        val uid = userId() ?: return
        runCatching {
            val poner = nuevos.likes - antes.likes
            val quitar = antes.likes - nuevos.likes
            if (poner.isNotEmpty()) backend.insert("post_likes", poner.map { buildJsonObject { put("user_id", uid); put("post_id", it) } })
            quitar.forEach { backend.delete("post_likes", mapOf("user_id" to uid, "post_id" to it)) }
            val filas = nuevos.comentarios.flatMap { (post, lista) ->
                lista.drop(antes.comentarios[post]?.size ?: 0).map { body -> buildJsonObject { put("user_id", uid); put("post_id", post); put("body", body) } }
            }
            if (filas.isNotEmpty()) backend.insert("post_comments", filas)
            val prefs: JsonElement = jsonGacelas.encodeToJsonElement(Ajustes.serializer(), nuevos.copy(likes = emptySet(), comentarios = emptyMap()))
            backend.upsert("user_settings", listOf(buildJsonObject {
                put("user_id", uid)
                put("visibility", VIS_A_DB.getValue(nuevos.visibilidad))
                put("hide_home", nuevos.ocultarHogar)
                put("home_radius_m", nuevos.radioHogarM)
                put("precise_location", nuevos.ubicacionPrecisa)
                put("weekly_goal_runs", nuevos.metaSemanal)
                put("prefs", prefs)
            }), "user_id")
        }
    }
}

/* ---------------- Eventos ---------------- */

fun eventoDesdeFila(f: JsonObject): Evento {
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
        pasado = inicio?.let { it < System.currentTimeMillis() } ?: false,
        creadoPorUsuario = true,
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
        runCatching { backend.select("events", mapOf("created_by" to uid), "created_at") }.getOrNull()?.let { filas ->
            creadosValor.actualizar { filas.map(::eventoDesdeFila) }
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
internal fun fila(vararg pares: Pair<String, Any?>): JsonObject = JsonObject(pares.associate { (k, v) ->
    k to when (v) {
        null -> JsonNull
        is String -> JsonPrimitive(v)
        is Number -> JsonPrimitive(v)
        is Boolean -> JsonPrimitive(v)
        is JsonElement -> v
        else -> JsonPrimitive(v.toString())
    }
})

