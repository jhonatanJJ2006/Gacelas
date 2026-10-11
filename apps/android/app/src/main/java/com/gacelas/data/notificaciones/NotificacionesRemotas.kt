package com.gacelas.data.notificaciones

import com.gacelas.data.mock.Notificacion
import com.gacelas.data.mock.TipoNotificacion
import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.FechaIso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

private fun JsonObject.texto(k: String) = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.contentOrNull

fun tipoDesde(t: String?): TipoNotificacion? = when (t) {
    "like" -> TipoNotificacion.Like
    "comment" -> TipoNotificacion.Comentario
    "follow" -> TipoNotificacion.Seguidor
    "event" -> TipoNotificacion.Evento
    "live" -> TipoNotificacion.EnVivo
    "streak" -> TipoNotificacion.Racha
    else -> null
}

/** "hace 5 min", "hace 3h", "hace 2d" (igual que la web). */
fun haceCuanto(ms: Long?, ahora: Long = System.currentTimeMillis()): String {
    if (ms == null) return ""
    val min = ((ahora - ms) / 60_000).coerceAtLeast(0)
    return when {
        min < 1 -> "ahora"
        min < 60 -> "hace $min min"
        min < 60 * 24 -> "hace ${min / 60}h"
        else -> "hace ${min / (60 * 24)}d"
    }
}

fun notificacionDesdeFila(f: JsonObject, ahora: Long = System.currentTimeMillis()): Notificacion? {
    val tipo = tipoDesde(f.texto("type")) ?: return null
    return Notificacion(
        id = f.texto("id").orEmpty(),
        tipo = tipo,
        usuario = f.texto("actor_name").orEmpty(),
        mensaje = f.texto("message").orEmpty(),
        tiempo = haceCuanto(f.texto("created_at")?.let(FechaIso::leer), ahora),
        leida = (f["read"] as? JsonPrimitive)?.booleanOrNull ?: false,
    )
}

/** Notificaciones reales de la cuenta (tabla notifications, RLS solo el dueño). */
class NotificacionesRepository(private val backend: Backend, private val uid: suspend () -> String?) {
    private val _lista = MutableStateFlow<List<Notificacion>>(emptyList())
    val lista: StateFlow<List<Notificacion>> = _lista

    suspend fun refrescar() {
        val yo = uid() ?: run { _lista.value = emptyList(); return }
        runCatching { backend.select("notifications", mapOf("user_id" to yo), "created_at", descendente = true) }
            .onSuccess { filas -> _lista.value = filas.mapNotNull { notificacionDesdeFila(it) } }
    }

    suspend fun marcarLeidas(ids: Collection<String>) {
        if (ids.isEmpty()) return
        _lista.value = _lista.value.map { if (it.id in ids) it.copy(leida = true) else it }
        ids.forEach { id -> runCatching { backend.update("notifications", JsonObject(mapOf("read" to JsonPrimitive(true))), mapOf("id" to id)) } }
    }
}
