package com.gacelas.comunidad.data

import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.FechaIso
import com.gacelas.shared.ahoraMs
import com.gacelas.shared.redondear
import kotlin.concurrent.Volatile
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import com.gacelas.comunidad.domain.modelo.*
import com.gacelas.comunidad.domain.reglas.*
import com.gacelas.comunidad.domain.repositorio.*
import com.gacelas.data.remoto.*

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
class SupabaseEnVivoRepository(private val backend: Backend, private val uid: suspend () -> String?, private val reloj: () -> Long = { com.gacelas.shared.ahoraMs() }) : EnVivoRepository {
    override var sesionId: String? = null
        private set
    override var visible: Boolean? = null
        private set
    private var ultimoEnvio = 0L

    /** Evento elegido para correr en vivo (Fase 5): la sesion se asocia a el. */
    @Volatile override var eventoId: String? = null

    override suspend fun iniciar(): String? = runCatching {
        val ev = eventoId
        val r = if (ev != null) backend.rpc("live_iniciar_evento", JsonObject(mapOf("evento" to kotlinx.serialization.json.JsonPrimitive(ev))))
        else backend.rpc("live_iniciar", JsonObject(emptyMap()))
        r.jsonPrimitive.contentOrNull
    }.getOrNull()?.also { sesionId = it; ultimoEnvio = 0L; visible = null }

    /** Envia como mucho cada 5 s. Devuelve true si se envio. */
    override suspend fun enviar(lat: Double, lng: Double, distanciaKm: Double, segundos: Int): Boolean {
        val id = sesionId ?: return false
        val ahora = reloj()
        if (ahora - ultimoEnvio < INTERVALO_MS) return false
        ultimoEnvio = ahora
        return runCatching {
            val r = backend.rpc("live_enviar", JsonObject(mapOf(
                "sesion" to JsonPrimitive(id), "lat" to JsonPrimitive(lat), "lng" to JsonPrimitive(lng),
                "distancia_m" to JsonPrimitive(redondear(distanciaKm * 1000).toDouble()), "segundos" to JsonPrimitive(segundos),
            )))
            visible = r.jsonPrimitive.booleanOrNull
            true
        }.getOrDefault(false)
    }

    override suspend fun terminar() {
        val id = sesionId ?: return
        sesionId = null
        eventoId = null
        visible = null
        runCatching { backend.rpc("live_terminar", JsonObject(mapOf("sesion" to JsonPrimitive(id)))) }
    }

    private suspend fun nombre(userId: String): String = runCatching {
        backend.select("profiles", mapOf("id" to userId)).firstOrNull()?.let { it.texto("full_name") ?: it.texto("username") }
    }.getOrNull() ?: "Corredor"

    /** Sesiones activas que puedo ver, sin la mia. */
    override suspend fun listar(): List<SesionEnVivo> = runCatching {
        val yo = uid()
        backend.select("live_sessions", emptyMap(), "started_at", descendente = true)
            .filter { it.texto("ended_at") == null && it.texto("user_id") != yo }
            .map { sesionDesdeFila(it, nombre(it.texto("user_id").orEmpty())) }
            .filter { it.activa(reloj()) }
    }.getOrDefault(emptyList())

    override suspend fun leer(id: String): SesionEnVivo? = runCatching {
        backend.select("live_sessions", mapOf("id" to id)).firstOrNull()?.let { sesionDesdeFila(it, nombre(it.texto("user_id").orEmpty())) }
    }.getOrNull()

    companion object { const val INTERVALO_MS = 5000L }
}
