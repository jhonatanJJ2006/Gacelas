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
