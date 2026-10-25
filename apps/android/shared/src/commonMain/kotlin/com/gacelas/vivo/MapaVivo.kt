package com.gacelas.vivo

import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.FechaIso
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

/** Un corredor en el mapa en vivo. */
data class CorredorEnMapa(
    val userId: String,
    val nombre: String,
    val avatarUrl: String?,
    val lat: Double,
    val lng: Double,
    val corriendo: Boolean,
    val actualizadoMs: Long,
)

object MapaVivo {
    /** Posiciones con mas de 2 minutos se ocultan (tambien lo aplica el RLS). */
    const val CADUCIDAD_MS = 2 * 60_000L
    /** Publicar como mucho cada 15 s corriendo y cada 60 s sin correr. */
    const val INTERVALO_CORRIENDO_MS = 15_000L
    const val INTERVALO_QUIETO_MS = 60_000L

    fun visibles(lista: List<CorredorEnMapa>, ahoraMs: Long, yo: String?): List<CorredorEnMapa> =
        lista.filter { it.userId != yo && ahoraMs - it.actualizadoMs <= CADUCIDAD_MS }

    fun tocaPublicar(ultimoMs: Long?, ultimoCorriendo: Boolean?, ahoraMs: Long, corriendo: Boolean): Boolean {
        if (ultimoMs == null || ultimoCorriendo != corriendo) return true
        return ahoraMs - ultimoMs >= if (corriendo) INTERVALO_CORRIENDO_MS else INTERVALO_QUIETO_MS
    }
}

private fun JsonObject.texto(k: String) = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.contentOrNull
private fun JsonObject.numero(k: String) = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.doubleOrNull

/**
 * Mapa en vivo sobre la tabla runner_positions (supabase/migrations/019). Solo se publica con
 * el consentimiento "ubicacion_visible" vigente; el servidor lo vuelve a comprobar.
 */
class MapaVivoRepository(
    private val backend: Backend,
    private val uid: suspend () -> String?,
    private val reloj: () -> Long = { com.gacelas.shared.ahoraMs() },
) {
    private var ultimoMs: Long? = null
    private var ultimoCorriendo: Boolean? = null

    suspend fun publicar(lat: Double, lng: Double, corriendo: Boolean): Boolean {
        val ahora = reloj()
        if (!MapaVivo.tocaPublicar(ultimoMs, ultimoCorriendo, ahora, corriendo)) return false
        ultimoMs = ahora; ultimoCorriendo = corriendo
        return runCatching {
            backend.rpc("mapa_publicar", JsonObject(mapOf("lat" to JsonPrimitive(lat), "lng" to JsonPrimitive(lng), "corriendo" to JsonPrimitive(corriendo))))
            true
        }.getOrDefault(false)
    }

    suspend fun dejarDeCompartir() {
        val id = uid() ?: return
        ultimoMs = null
        runCatching { backend.delete("runner_positions", mapOf("user_id" to id)) }
    }

    suspend fun listar(): List<CorredorEnMapa> = runCatching {
        val filas = backend.select("runner_positions", emptyMap(), "updated_at", descendente = true)
        val lista = filas.mapNotNull { f ->
            val id = f.texto("user_id") ?: return@mapNotNull null
            val p = runCatching { backend.select("profiles", mapOf("id" to id)).firstOrNull() }.getOrNull()
            CorredorEnMapa(
                id, p?.texto("full_name") ?: p?.texto("username") ?: "Corredor", p?.texto("avatar_url"),
                f.numero("lat") ?: return@mapNotNull null, f.numero("lng") ?: return@mapNotNull null,
                (f["running"] as? JsonPrimitive)?.booleanOrNull ?: false,
                f.texto("updated_at")?.let(FechaIso::leer) ?: 0L,
            )
        }
        MapaVivo.visibles(lista, reloj(), uid())
    }.getOrDefault(emptyList())
}
