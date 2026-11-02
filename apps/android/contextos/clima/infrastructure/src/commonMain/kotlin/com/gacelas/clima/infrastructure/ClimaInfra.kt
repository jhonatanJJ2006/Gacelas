package com.gacelas.clima.infrastructure

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.clima.domain.CacheClima
import com.gacelas.clima.domain.ClimaActual
import com.gacelas.clima.domain.ClimaRepositorio
import com.gacelas.clima.domain.Coordenadas
import com.gacelas.clima.domain.FuenteClima
import com.gacelas.clima.domain.LecturaClima
import com.gacelas.clima.domain.ReglasClima
import com.gacelas.data.ValorJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Open-Meteo (gratis, sin clave, CC BY 4.0). [texto] hace el GET (Red.texto por defecto). */
class OpenMeteoFuenteClima(
    private val texto: suspend (String) -> String = { com.gacelas.shared.Red.texto(it) },
) : FuenteClima {
    override suspend fun leer(c: Coordenadas): LecturaClima = leerJson(texto(url(c)))

    companion object {
        fun url(c: Coordenadas) = "https://api.open-meteo.com/v1/forecast?latitude=${c.lat}&longitude=${c.lng}" +
            "&current=temperature_2m,weather_code,is_day"

        fun leerJson(json: String): LecturaClima {
            val actual = Json.parseToJsonElement(json).jsonObject.getValue("current").jsonObject
            return LecturaClima(
                temperatura = actual.getValue("temperature_2m").jsonPrimitive.double,
                codigoWmo = actual.getValue("weather_code").jsonPrimitive.int,
                esDeDia = actual["is_day"]?.jsonPrimitive?.let { runCatching { it.int == 1 }.getOrElse { _ -> it.boolean } } ?: true,
            )
        }
    }
}

@Serializable
private data class ClimaGuardado(val t: Int = 0, val d: String = "", val l: String = "", val ms: Long = 0L, val hay: Boolean = false)

/** Ultimo clima en DataStore (sobrevive sin red y entre aperturas). */
class CacheClimaDataStore(store: DataStore<Preferences>) : CacheClima {
    private val valor = ValorJson(store, "clima_actual", ClimaGuardado.serializer(), ClimaGuardado())
    override suspend fun leer(): ClimaActual? = valor.flujo.first().takeIf { it.hay }?.let { ClimaActual(it.t, it.d, it.l, it.ms) }
    override suspend fun guardar(c: ClimaActual) = valor.actualizar { ClimaGuardado(c.temperatura, c.descripcion, c.lugar, c.consultadoMs, true) }
}

/**
 * Cache primero; a la red como mucho cada 15 min (tambien tras un fallo) con timeout de 5 s.
 * [ubicacion] da la ultima ubicacion conocida o null (sin permiso: respaldo Loja) y
 * [nombreLugar] la geocodificacion inversa de la plataforma (null si no hay).
 */
class ClimaRepositorioOpenMeteo(
    private val fuente: FuenteClima,
    private val cache: CacheClima,
    private val ubicacion: suspend () -> Coordenadas? = { null },
    private val nombreLugar: suspend (Coordenadas) -> String? = { null },
    private val timeoutMs: Long = ReglasClima.TIMEOUT_MS,
) : ClimaRepositorio {
    private var ultimoIntentoMs: Long? = null

    override suspend fun enCache(): ClimaActual? = runCatching { cache.leer() }.getOrNull()

    override suspend fun actual(ahoraMs: Long): ClimaActual? {
        val guardado = enCache()
        if (guardado != null && !ReglasClima.debeRefrescar(guardado.consultadoMs, ahoraMs)) return guardado
        if (!ReglasClima.debeRefrescar(ultimoIntentoMs, ahoraMs)) return guardado
        ultimoIntentoMs = ahoraMs
        val coords = seguro { ubicacion() }
        val lectura = seguro { withTimeoutOrNull(timeoutMs) { fuente.leer(ReglasClima.aproximar(coords ?: ReglasClima.LOJA)) } }
            ?: return guardado
        val cercaDeLoja = ReglasClima.etiquetaLugar(coords, null) == ReglasClima.NOMBRE_LOJA
        val nombre = if (coords != null && !cercaDeLoja) seguro { withTimeoutOrNull(timeoutMs) { nombreLugar(coords) } } else null
        val clima = ReglasClima.aClima(lectura, ReglasClima.etiquetaLugar(coords, nombre), ahoraMs)
        seguro { cache.guardar(clima) }
        return clima
    }

    private inline fun <T> seguro(bloque: () -> T?): T? = try { bloque() } catch (e: CancellationException) { throw e } catch (_: Throwable) { null }
}
