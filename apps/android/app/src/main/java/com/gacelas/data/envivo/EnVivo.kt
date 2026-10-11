package com.gacelas.data.envivo

import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.FechaIso
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

data class Coordenada(val lat: Double, val lng: Double)

/** Sesion en vivo tal como la ve un espectador (misma forma que la web, lib/enVivo.ts). */
data class SesionEnVivo(
    val id: String,
    val userId: String,
    val nombre: String,
    val publica: Boolean,
    val inicioMs: Long,
    val finMs: Long?,
    /** Posicion oculta mientras esta cerca de casa. */
    val oculta: Boolean,
    val ultimaMs: Long?,
    val posicion: Coordenada?,
    val puntos: List<Coordenada>,
    val distanciaKm: Double,
    val segundos: Int,
) {
    fun activa(ahoraMs: Long = System.currentTimeMillis()) = finMs == null && ahoraMs - (ultimaMs ?: inicioMs) < 15 * 60_000L
}

private fun JsonObject.texto(k: String) = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.contentOrNull
private fun JsonObject.numero(k: String) = (this[k] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.doubleOrNull

fun sesionDesdeFila(f: JsonObject, nombre: String = "Corredor"): SesionEnVivo {
    val oculta = (f["hidden"] as? JsonPrimitive)?.booleanOrNull ?: true
    val lat = f.numero("last_lat")
    val lng = f.numero("last_lng")
    val puntos = (f["points"] as? JsonArray)?.mapNotNull { e ->
        val o = e as? JsonObject ?: return@mapNotNull null
        val a = o.numero("lat"); val b = o.numero("lng")
        if (a != null && b != null) Coordenada(a, b) else null
    }.orEmpty()
    return SesionEnVivo(
        id = f.texto("id").orEmpty(),
        userId = f.texto("user_id").orEmpty(),
        nombre = nombre,
        publica = f.texto("audience") == "public",
        inicioMs = f.texto("started_at")?.let(FechaIso::leer) ?: 0L,
        finMs = f.texto("ended_at")?.let(FechaIso::leer),
        oculta = oculta,
        ultimaMs = f.texto("last_at")?.let(FechaIso::leer),
        posicion = if (!oculta && lat != null && lng != null) Coordenada(lat, lng) else null,
        puntos = puntos,
        distanciaKm = (f.numero("distance_m") ?: 0.0) / 1000.0,
        segundos = f.numero("elapsed_s")?.toInt() ?: 0,
    )
}

/**
 * Compartir y ver carreras en vivo. Escribe con las funciones live_* de Supabase
 * (backend/migrations/008_live_rpc.sql); la lectura respeta el RLS de live_sessions.
 */
class EnVivoRepository(private val backend: Backend, private val uid: suspend () -> String?, private val reloj: () -> Long = System::currentTimeMillis) {
    var sesionId: String? = null
        private set
    /** Ultimo envio: true visible, false oculto cerca de casa, null sin envios. */
    var visible: Boolean? = null
        private set
    private var ultimoEnvio = 0L

    suspend fun iniciar(): String? = runCatching {
        val r = backend.rpc("live_iniciar", JsonObject(emptyMap()))
        r.jsonPrimitive.contentOrNull
    }.getOrNull()?.also { sesionId = it; ultimoEnvio = 0L; visible = null }

    /** Envia como mucho cada 5 s. Devuelve true si se envio. */
    suspend fun enviar(lat: Double, lng: Double, distanciaKm: Double, segundos: Int): Boolean {
        val id = sesionId ?: return false
        val ahora = reloj()
        if (ahora - ultimoEnvio < INTERVALO_MS) return false
        ultimoEnvio = ahora
        return runCatching {
            val r = backend.rpc("live_enviar", JsonObject(mapOf(
                "sesion" to JsonPrimitive(id), "lat" to JsonPrimitive(lat), "lng" to JsonPrimitive(lng),
                "distancia_m" to JsonPrimitive(Math.round(distanciaKm * 1000).toDouble()), "segundos" to JsonPrimitive(segundos),
            )))
            visible = r.jsonPrimitive.booleanOrNull
            true
        }.getOrDefault(false)
    }

    suspend fun terminar() {
        val id = sesionId ?: return
        sesionId = null
        visible = null
        runCatching { backend.rpc("live_terminar", JsonObject(mapOf("sesion" to JsonPrimitive(id)))) }
    }

    private suspend fun nombre(userId: String): String = runCatching {
        backend.select("profiles", mapOf("id" to userId)).firstOrNull()?.let { it.texto("full_name") ?: it.texto("username") }
    }.getOrNull() ?: "Corredor"

    /** Sesiones activas que puedo ver, sin la mia. */
    suspend fun listar(): List<SesionEnVivo> = runCatching {
        val yo = uid()
        backend.select("live_sessions", emptyMap(), "started_at", descendente = true)
            .filter { it.texto("ended_at") == null && it.texto("user_id") != yo }
            .map { sesionDesdeFila(it, nombre(it.texto("user_id").orEmpty())) }
            .filter { it.activa(reloj()) }
    }.getOrDefault(emptyList())

    suspend fun leer(id: String): SesionEnVivo? = runCatching {
        backend.select("live_sessions", mapOf("id" to id)).firstOrNull()?.let { sesionDesdeFila(it, nombre(it.texto("user_id").orEmpty())) }
    }.getOrNull()

    companion object { const val INTERVALO_MS = 5000L }
}
