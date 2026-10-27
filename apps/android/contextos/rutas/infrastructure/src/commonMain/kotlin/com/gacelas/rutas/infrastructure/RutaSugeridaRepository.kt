package com.gacelas.rutas.infrastructure

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.data.ValorJson
import com.gacelas.data.remoto.Backend
import com.gacelas.data.remoto.numero
import com.gacelas.data.remoto.texto
import com.gacelas.rutas.domain.modelo.Dificultad
import com.gacelas.rutas.domain.modelo.Ruta
import com.gacelas.rutas.domain.reglas.CarreraSugerida
import com.gacelas.rutas.domain.reglas.ResultadoSugerida
import com.gacelas.rutas.domain.reglas.SugerenciaPrevia
import com.gacelas.rutas.domain.reglas.elegirRutaSugerida
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Lo que muestra la tarjeta "Ruta sugerida para hoy". */
sealed interface RutaDelDia {
    data class Ok(val ruta: Ruta, val sinConexion: Boolean = false) : RutaDelDia
    data class SinRuta(val motivo: String) : RutaDelDia
}

@Serializable
private data class CacheSugerida(
    val historial: List<Pair<Long, String>> = emptyList(),
    val seguras: Set<String> = emptySet(),
)

/**
 * Adaptador de ruta_sugerida_hoy (migracion 035). Sin red aplica la misma regla en local con la
 * ultima lista de rutas seguras que dio el servidor; si nunca hubo una, no sugiere nada.
 */
class RutaSugeridaRepository(private val backend: Backend?, store: DataStore<Preferences>) {
    private val cache = ValorJson(store, "ruta_sugerida", CacheSugerida.serializer(), CacheSugerida())

    suspend fun hoy(
        rutasLocales: List<Ruta>,
        carreras: List<CarreraSugerida>,
        ahoraMs: Long,
        ubicacion: Pair<Double, Double>? = null,
    ): RutaDelDia {
        val remota = backend?.let { b ->
            runCatching {
                val args = JsonObject(
                    mapOf(
                        "p_lat" to (ubicacion?.first?.let { JsonPrimitive(it) } ?: JsonNull),
                        "p_lng" to (ubicacion?.second?.let { JsonPrimitive(it) } ?: JsonNull),
                    ),
                )
                b.rpc("ruta_sugerida_hoy", args) as? JsonObject
            }.getOrNull()
        }
        if (remota != null) {
            val dia = com.gacelas.rutas.domain.reglas.diaEcuador(ahoraMs)
            val seguras = (remota["seguras"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }?.toSet()
            val ruta = (remota["ruta"] as? JsonObject)?.let { leerRuta(it, rutasLocales) }
            cache.actualizar { c ->
                c.copy(
                    historial = if (ruta != null) (c.historial.filter { it.first != dia } + (dia to ruta.id)).takeLast(14) else c.historial,
                    seguras = seguras ?: c.seguras,
                )
            }
            if (remota.texto("estado") == "sin_ruta") return RutaDelDia.SinRuta(remota.texto("motivo").orEmpty())
            if (ruta != null) return RutaDelDia.Ok(ruta)
        }
        val c = cache.flujo.first()
        if (c.seguras.isEmpty()) return RutaDelDia.SinRuta("Conéctate para ver la ruta segura de hoy.")
        return when (val r = elegirRutaSugerida(rutasLocales, carreras, c.seguras, c.historial.map { SugerenciaPrevia(it.first, it.second) }, ahoraMs, ubicacion)) {
            is ResultadoSugerida.Ok -> RutaDelDia.Ok(r.ruta, sinConexion = true)
            is ResultadoSugerida.SinRuta -> RutaDelDia.SinRuta(r.motivo)
        }
    }

    private fun leerRuta(o: JsonObject, locales: List<Ruta>): Ruta? {
        val id = o.texto("id") ?: return null
        locales.firstOrNull { it.id == id }?.let { return it }
        return Ruta(
            id = id,
            nombre = o.texto("nombre").orEmpty(),
            distanciaKm = o.numero("distancia_km") ?: o.texto("distancia_km")?.toDoubleOrNull() ?: 0.0,
            dificultad = Dificultad.entries.firstOrNull { it.etiqueta == o.texto("dificultad") } ?: Dificultad.Moderado,
            elevacionM = (o.numero("elevacion_m") ?: 0.0).toInt(),
            descripcion = o.texto("descripcion").orEmpty(),
        )
    }
}
