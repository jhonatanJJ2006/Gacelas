package com.gacelas.clima.infrastructure

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gacelas.clima.domain.CacheClima
import com.gacelas.clima.domain.ClimaActual
import com.gacelas.clima.domain.ClimaRepositorio
import com.gacelas.clima.domain.Coordenadas
import com.gacelas.clima.domain.FuenteClima
import com.gacelas.clima.domain.HoraClima
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/** Open-Meteo (gratis, sin clave, CC BY 4.0). [texto] hace el GET (Red.texto por defecto). */
class OpenMeteoFuenteClima(
    private val texto: suspend (String) -> String = { com.gacelas.shared.Red.texto(it) },
) : FuenteClima {
    override suspend fun leer(c: Coordenadas): LecturaClima = leerJson(texto(url(c)))

    companion object {
        fun url(c: Coordenadas) = "https://api.open-meteo.com/v1/forecast?latitude=${c.lat}&longitude=${c.lng}" +
            "&current=temperature_2m,weather_code,is_day&hourly=temperature_2m,weather_code&forecast_hours=$HORAS&timezone=auto"

        /** Horas de pronostico que pide el widget de clima. */
        const val HORAS = 8

        fun leerJson(json: String): LecturaClima {
            val raiz = Json.parseToJsonElement(json).jsonObject
            val actual = raiz.getValue("current").jsonObject
            return LecturaClima(
                temperatura = actual.getValue("temperature_2m").jsonPrimitive.double,
                codigoWmo = actual.getValue("weather_code").jsonPrimitive.int,
                esDeDia = actual["is_day"]?.jsonPrimitive?.let { runCatching { it.int == 1 }.getOrElse { _ -> it.boolean } } ?: true,
                horas = runCatching { horas(raiz["hourly"]?.jsonObject) }.getOrElse { emptyList() },
            )
        }

        /** hourly.time ("2026-11-13T07:00"), temperature_2m y weather_code en paralelo. */
        private fun horas(h: kotlinx.serialization.json.JsonObject?): List<HoraClima> {
            if (h == null) return emptyList()
            val t = h["time"]?.jsonArray ?: return emptyList()
            val temp = h["temperature_2m"]?.jsonArray ?: return emptyList()
            val cod = h["weather_code"]?.jsonArray ?: return emptyList()
            return t.indices.mapNotNull { i ->
                val hora = t[i].jsonPrimitive.content.substringAfter('T').take(2).toIntOrNull() ?: return@mapNotNull null
                val g = temp.getOrNull(i)?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                val c = cod.getOrNull(i)?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
                HoraClima(hora, kotlin.math.round(g).toInt(), c)
            }.take(HORAS)
        }
    }
}

@Serializable
private data class ClimaGuardado(val t: Int = 0, val d: String = "", val l: String = "", val ms: Long = 0L, val hay: Boolean = false, val h: List<HoraGuardada> = emptyList())

@Serializable
private data class HoraGuardada(val h: Int, val t: Int, val c: Int)

/** Ultimo clima en DataStore (sobrevive sin red y entre aperturas). */
class CacheClimaDataStore(store: DataStore<Preferences>) : CacheClima {
    private val valor = ValorJson(store, "clima_actual", ClimaGuardado.serializer(), ClimaGuardado())
    override suspend fun leer(): ClimaActual? = valor.flujo.first().takeIf { it.hay }?.let { g -> ClimaActual(g.t, g.d, g.l, g.ms, g.h.map { HoraClima(it.h, it.t, it.c) }) }
    override suspend fun guardar(c: ClimaActual) = valor.actualizar { ClimaGuardado(c.temperatura, c.descripcion, c.lugar, c.consultadoMs, true, c.horas.map { HoraGuardada(it.hora, it.temperatura, it.codigoWmo) }) }
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
