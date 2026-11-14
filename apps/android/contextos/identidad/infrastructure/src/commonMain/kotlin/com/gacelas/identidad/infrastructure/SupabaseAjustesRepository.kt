package com.gacelas.identidad.infrastructure

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.identidad.domain.modelo.Ajustes
import com.gacelas.identidad.domain.modelo.Visibilidad
import com.gacelas.identidad.domain.repositorio.AjustesRepository
import com.gacelas.data.ValorJson
import com.gacelas.data.jsonGacelas
import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.entero
import com.gacelas.data.remoto.logico
import com.gacelas.data.remoto.texto
import com.gacelas.data.sync.BorradoSync
import com.gacelas.data.sync.CambioClave
import com.gacelas.data.sync.ConsultaPagina
import com.gacelas.data.sync.bajarTodo
import com.gacelas.data.sync.borradosDe
import com.gacelas.data.sync.fusionarClaves
import com.gacelas.data.sync.marcaMs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/* ---------------- Ajustes, likes y comentarios ---------------- */

private val VIS_A_DB = mapOf(Visibilidad.Publico to "public", Visibilidad.Seguidores to "followers", Visibilidad.Privado to "private")

class SupabaseAjustesRepository(
    private val backend: Backend,
    store: DataStore<Preferences>,
    private val userId: suspend () -> String?,
) : AjustesRepository {
    private val valor = ValorJson(store, "ajustes", Ajustes.serializer(), Ajustes())
    override val ajustes: Flow<Ajustes> = valor.flujo

    /**
     * Sin [desde] trae ajustes, me gusta y comentarios enteros. Con marca, solo lo que cambio.
     * Un comentario borrado no trae id en la cache local: si hubo bajas, se releen los textos.
     */
    suspend fun descargar(desde: String? = null, borrados: List<BorradoSync> = emptyList()): Boolean {
        val uid = userId() ?: return false
        val completa = desde == null
        val ajustesFilas = runCatching {
            bajarTodo(backend::selectPagina, ConsultaPagina("user_settings", mapOf("user_id" to uid), marcaExclusiva = desde, columnaClave = "user_id", limite = 5))
        }.getOrElse { return false }
        val likesFilas = runCatching {
            bajarTodo(backend::selectPagina, ConsultaPagina("post_likes", mapOf("user_id" to uid), marcaExclusiva = desde, columnaClave = "post_id"))
        }.getOrElse { return false }
        val comentariosNuevos = runCatching {
            bajarTodo(backend::selectPagina, ConsultaPagina("post_comments", mapOf("user_id" to uid), marcaExclusiva = desde, columnaClave = "id"))
        }.getOrElse { return false }
        val bajasComentarios = if (completa) emptyList() else borradosDe(borrados, "post_comments")
        val filasComentarios = when {
            !completa && comentariosNuevos.isEmpty() && bajasComentarios.isEmpty() -> null
            completa -> comentariosNuevos
            else -> runCatching {
                bajarTodo(backend::selectPagina, ConsultaPagina("post_comments", mapOf("user_id" to uid), columnaMarca = "created_at", columnaClave = "id"))
            }.getOrElse { return false }
        }
        val fila = ajustesFilas.firstOrNull()
        val altasLikes = likesFilas.mapNotNull { f -> f.texto("post_id")?.let { CambioClave(it, marcaMs(f)) } }
        valor.actualizar { actual ->
            val base = if (fila == null) actual else {
                val prefs = fila["prefs"]?.let { ajustesTolerantes(it) } ?: actual
                prefs.copy(
                    visibilidad = VIS_A_DB.entries.firstOrNull { it.value == fila.texto("visibility") }?.key ?: prefs.visibilidad,
                    ocultarHogar = fila.logico("hide_home") ?: prefs.ocultarHogar,
                    radioHogarM = fila.entero("home_radius_m") ?: prefs.radioHogarM,
                    ubicacionPrecisa = fila.logico("precise_location") ?: prefs.ubicacionPrecisa,
                    metaSemanal = fila.entero("weekly_goal_runs") ?: prefs.metaSemanal,
                )
            }
            base.copy(
                likes = fusionarClaves(actual.likes, altasLikes, if (completa) emptyList() else borradosDe(borrados, "post_likes"), completa),
                comentarios = filasComentarios?.groupBy({ it.texto("post_id").orEmpty() }, { it.texto("body").orEmpty() }) ?: actual.comentarios,
            )
        }
        return true
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

/**
 * Lee prefs aunque las haya escrito la web (otros nombres o tipos en algunos campos, p. ej.
 * visibilidad en minusculas): se aplican clave por clave y se ignora solo la que no encaja. Antes un
 * solo campo distinto descartaba todo y la app volvia a preguntar cosas ya respondidas.
 */
fun ajustesTolerantes(remoto: JsonElement): Ajustes? {
    val obj = remoto as? kotlinx.serialization.json.JsonObject ?: return null
    runCatching { return jsonGacelas.decodeFromJsonElement(Ajustes.serializer(), obj) }
    var base = jsonGacelas.encodeToJsonElement(Ajustes.serializer(), Ajustes()) as kotlinx.serialization.json.JsonObject
    for ((k, v) in obj) {
        val valor = if (k == "visibilidad" && v is kotlinx.serialization.json.JsonPrimitive && v.isString)
            kotlinx.serialization.json.JsonPrimitive(v.content.replaceFirstChar { it.uppercase() }) else v
        val prueba = kotlinx.serialization.json.JsonObject(base + (k to valor))
        if (runCatching { jsonGacelas.decodeFromJsonElement(Ajustes.serializer(), prueba) }.isSuccess) base = prueba
    }
    return runCatching { jsonGacelas.decodeFromJsonElement(Ajustes.serializer(), base) }.getOrNull()
}
