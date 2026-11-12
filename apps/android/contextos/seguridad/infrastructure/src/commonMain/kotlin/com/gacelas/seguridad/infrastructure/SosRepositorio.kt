package com.gacelas.seguridad.infrastructure

import com.gacelas.data.remoto.Backend
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Alertas SOS sobre Supabase (migracion 039_sos.sql): sos_iniciar, sos_enviar y sos_terminar. El id
 * devuelto es el enlace privado que se manda a los contactos (https://gacelas.vercel.app/sos/<id>).
 */
class SosRepositorio(private val backend: Backend) {
    var alertaId: String? = null
        private set
    private var ultimoEnvioMs = 0L

    suspend fun activar(lat: Double?, lng: Double?, ahoraMs: Long): String? {
        alertaId?.let { return it }
        val r = backend.rpc("sos_iniciar", buildJsonObject {
            if (lat != null && lng != null) { put("lat", lat); put("lng", lng) } else { put("lat", JsonNull); put("lng", JsonNull) }
        })
        val id = (r as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
        alertaId = id
        ultimoEnvioMs = ahoraMs
        return id
    }

    /** Envia la posicion como mucho cada [com.gacelas.seguridad.domain.Sos.ENVIO_CADA_MS]. */
    suspend fun posicion(lat: Double, lng: Double, ahoraMs: Long) {
        val id = alertaId ?: return
        if (ahoraMs - ultimoEnvioMs < com.gacelas.seguridad.domain.Sos.ENVIO_CADA_MS) return
        ultimoEnvioMs = ahoraMs
        runCatching { backend.rpc("sos_enviar", buildJsonObject { put("alerta", id); put("lat", lat); put("lng", lng) }) }
    }

    suspend fun terminar() {
        val id = alertaId ?: return
        alertaId = null
        runCatching { backend.rpc("sos_terminar", buildJsonObject { put("alerta", id) }) }
    }
}
