package com.gacelas.clima.domain

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Clima que muestra el chip de Inicio: "LOJA 16 °C NUBLADO". */
data class ClimaActual(
    /** Grados Celsius ya redondeados. */
    val temperatura: Int,
    /** Texto en español en minusculas, p. ej. "nublado". */
    val descripcion: String,
    /** Etiqueta del lugar: "Loja", un nombre simple o "Tu zona". */
    val lugar: String,
    /** Momento de la consulta (epoch ms). */
    val consultadoMs: Long,
)

/** Coordenadas (grados decimales). */
data class Coordenadas(val lat: Double, val lng: Double)

/** Respuesta cruda de la fuente: temperatura, codigo WMO y si es de dia. */
data class LecturaClima(val temperatura: Double, val codigoWmo: Int, val esDeDia: Boolean)

/** Puerto de la fuente de datos (Open-Meteo en infraestructura). */
fun interface FuenteClima {
    suspend fun leer(c: Coordenadas): LecturaClima
}

/** Puerto de la cache local del ultimo clima. */
interface CacheClima {
    suspend fun leer(): ClimaActual?
    suspend fun guardar(c: ClimaActual)
}

/** Repositorio que usa Inicio: cache + red con regla de refresco. */
interface ClimaRepositorio {
    /** Ultimo clima guardado (o null). */
    suspend fun enCache(): ClimaActual?

    /** Refresca si toca; ante cualquier fallo devuelve la cache (o null). */
    suspend fun actual(ahoraMs: Long): ClimaActual?
}

object ReglasClima {
    /** Respaldo cuando no hay permiso o ubicacion. */
    val LOJA = Coordenadas(-3.9931, -79.2042)
    const val NOMBRE_LOJA = "Loja"
    const val TU_ZONA = "Tu zona"
    const val RADIO_LOJA_KM = 30.0
    const val REFRESCO_MS = 15 * 60_000L
    const val TIMEOUT_MS = 5_000L

    /** Minimo de 15 min entre llamadas; un reloj que retrocede tambien refresca. */
    fun debeRefrescar(ultimaMs: Long?, ahoraMs: Long): Boolean =
        ultimaMs == null || ahoraMs < ultimaMs || ahoraMs - ultimaMs >= REFRESCO_MS

    /** Privacidad: a Open-Meteo solo van 2 decimales (~1 km). */
    fun aproximar(c: Coordenadas): Coordenadas =
        Coordenadas((c.lat * 100).roundToInt() / 100.0, (c.lng * 100).roundToInt() / 100.0)

    fun distanciaKm(a: Coordenadas, b: Coordenadas): Double {
        fun rad(g: Double) = g * PI / 180.0
        val dLat = rad(b.lat - a.lat); val dLng = rad(b.lng - a.lng)
        val h = sin(dLat / 2).let { it * it } + cos(rad(a.lat)) * cos(rad(b.lat)) * sin(dLng / 2).let { it * it }
        return 2 * 6371.0 * asin(sqrt(h))
    }

    /**
     * "Loja" si es el respaldo (null) o esta a menos de 30 km; si no, el nombre de la
     * geocodificacion inversa de la plataforma o "Tu zona".
     */
    fun etiquetaLugar(c: Coordenadas?, nombrePlataforma: String?): String = when {
        c == null || distanciaKm(c, LOJA) < RADIO_LOJA_KM -> NOMBRE_LOJA
        !nombrePlataforma.isNullOrBlank() -> nombrePlataforma.trim()
        else -> TU_ZONA
    }

    /** Codigo WMO (Open-Meteo) a texto en español. */
    fun descripcion(codigo: Int, esDeDia: Boolean): String = when (codigo) {
        0 -> if (esDeDia) "despejado" else "noche despejada"
        1 -> if (esDeDia) "mayormente despejado" else "noche mayormente despejada"
        2 -> "parcialmente nublado"
        3 -> "nublado"
        45, 48 -> "niebla"
        51, 53, 55, 56, 57 -> "llovizna"
        61, 63, 65, 66, 67 -> "lluvia"
        71, 73, 75, 77, 85, 86 -> "nieve"
        80, 81, 82 -> "chubascos"
        95, 96, 99 -> "tormenta"
        else -> "nublado"
    }

    fun aClima(l: LecturaClima, lugar: String, ahoraMs: Long) =
        ClimaActual(l.temperatura.roundToInt(), descripcion(l.codigoWmo, l.esDeDia), lugar, ahoraMs)
}
