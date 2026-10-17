package com.gacelas.cuenta.data

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
import com.gacelas.comunidad.domain.repositorio.EventosRepository
import com.gacelas.data.jsonGacelas
import com.gacelas.comunidad.domain.modelo.Evento
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
import com.gacelas.data.remoto.*

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

