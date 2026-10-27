package com.gacelas.mapa

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/** Coordenada del mapa independiente de la plataforma (en Android: LatLng de MapLibre). */
data class Coord(val lat: Double, val lng: Double)

/** Constantes del mapa Runcity (los mismos valores que Mapas/MapaOffline/MapaCompacto en Android). */
object MapaGacelasConst {
    val CENTRO_LOJA = Coord(-3.9931, -79.2042)
    /** Loja y alrededores: mismos limites y zooms (10-16) que MapaOffline.LOJA. */
    const val LOJA_NORTE = -3.92
    const val LOJA_ESTE = -79.15
    const val LOJA_SUR = -4.06
    const val LOJA_OESTE = -79.28
    const val LOJA_ZOOM_MIN = 10.0
    const val LOJA_ZOOM_MAX = 16.0
    const val LOJA_NOMBRE = "Loja, Ecuador"
    const val LOJA_ID = "loja"
    const val TILEJSON = "https://tiles.openfreemap.org/planet"
    const val ESTILO_DESCARGA = "https://tiles.openfreemap.org/styles/dark"
    const val CACHE_AMBIENTAL_BYTES = 150L * 1024 * 1024
    const val SEGUIR_DE_NUEVO_MS = 8_000L
    const val ANIMACION_PUNTO_MS = 1_000
    const val ALTO_COMPACTO_DP = 480f
    const val PADDING_ARRIBA = 0.45
}

/** Estado de la descarga del mapa offline (igual que EstadoDescarga en Android). */
sealed interface EstadoMapaOffline {
    data object SinDescargar : EstadoMapaOffline
    data class Descargando(val porcentaje: Int, val megas: Double) : EstadoMapaOffline
    data class Lista(val megas: Double) : EstadoMapaOffline
    data class Error(val mensaje: String) : EstadoMapaOffline
}

/** Mismos textos que textoEstadoMapa (Android); "%.1f MB" con punto decimal (Locale.US). */
fun textoEstadoMapaOffline(e: EstadoMapaOffline): String {
    fun mb(m: Double): String { val d = (m * 10).roundToLong(); return "${d / 10}.${d % 10} MB" }
    return when (e) {
        EstadoMapaOffline.SinDescargar -> "Descarga las calles de Loja (OpenStreetMap) para correr sin datos"
        is EstadoMapaOffline.Descargando -> "Descargando ${e.porcentaje}% (${mb(e.megas)})"
        is EstadoMapaOffline.Lista -> "Mapa offline listo (${mb(e.megas)})"
        is EstadoMapaOffline.Error -> e.mensaje
    }
}

/**
 * Reglas puras de las teselas offline. OpenFreeMap versiona las teselas (planet/20261004_.../{z}/{x}/{y}.pbf)
 * y cambia la version cada semana: se guarda la plantilla de la version descargada y, sin red, el estilo
 * Runcity se arma con ella (ver MapaOffline.estiloSinRed en Android).
 */
object ReglasTeselas {
    private val json = Json { ignoreUnknownKeys = true }

    /** Primera plantilla "tiles" de una TileJSON. */
    fun plantillaTeselas(tilejson: String): String? = runCatching {
        ((json.parseToJsonElement(tilejson) as JsonObject)["tiles"] as JsonArray).first().let { (it as JsonPrimitive).content }
    }.getOrNull()

    /** Cambia la fuente openmaptiles {url: TileJSON} por {tiles:[plantilla], maxzoom 14}. */
    fun fijarTeselas(estilo: String, plantilla: String): String? = runCatching {
        val e = json.parseToJsonElement(estilo) as JsonObject
        val fuentes = e["sources"] as JsonObject
        val nuevas = JsonObject(fuentes.mapValues { (k, v) ->
            if (k != "openmaptiles") v else buildJsonObject {
                (v as JsonObject).forEach { (kk, vv) -> if (kk != "url") put(kk, vv) }
                put("tiles", JsonArray(listOf(JsonPrimitive(plantilla))))
                put("minzoom", JsonPrimitive(0))
                put("maxzoom", JsonPrimitive(14))
            }
        })
        JsonObject(e + ("sources" to nuevas)).toString()
    }.getOrNull()

    /** Se descarga sola si falta o quedo incompleta, y solo con red permitida. */
    fun descargarSola(existe: Boolean, completa: Boolean, wifi: Boolean): Boolean = wifi && (!existe || !completa)
}

/** Matematicas del mapa (mismas formulas que MapaMat y ZoomNavegacion de Android). */
object MapaMatComun {
    private fun rad(g: Double) = g * PI / 180
    private fun grados(r: Double) = r * 180 / PI
    fun rumbo(a: Coord, b: Coord): Float {
        val f1 = rad(a.lat); val f2 = rad(b.lat); val dl = rad(b.lng - a.lng)
        val y = sin(dl) * cos(f2); val x = cos(f1) * sin(f2) - sin(f1) * cos(f2) * cos(dl)
        return ((grados(atan2(y, x)) + 360) % 360).toFloat()
    }
    fun distanciaM(a: Coord, b: Coord): Double {
        val dLat = rad(b.lat - a.lat); val dLng = rad(b.lng - a.lng)
        val h = sin(dLat / 2).pow(2) + cos(rad(a.lat)) * cos(rad(b.lat)) * sin(dLng / 2).pow(2)
        return 2 * 6371000 * asin(sqrt(h))
    }
    fun desplazar(p: Coord, rumboGrados: Double, metros: Double): Coord {
        val d = metros / 6371000; val r = rad(rumboGrados); val f1 = rad(p.lat); val l1 = rad(p.lng)
        val f2 = asin(sin(f1) * cos(d) + cos(f1) * sin(d) * cos(r))
        val l2 = l1 + atan2(sin(r) * sin(d) * cos(f1), cos(d) - sin(f1) * sin(f2))
        return Coord(grados(f2), grados(l2))
    }
    fun rumboReciente(p: List<Coord>, metros: Double = 15.0): Float? {
        if (p.size < 2) return null
        val fin = p.last()
        for (i in p.size - 2 downTo 0) if (distanciaM(p[i], fin) >= metros || i == 0) return rumbo(p[i], fin)
        return null
    }
    /** Zoom de la vista de navegacion segun la velocidad (ZoomNavegacion.para). */
    fun zoomNavegacion(velocidadMs: Double?): Double {
        val lento = 19.5; val rapido = 18.3; val vl = 8 / 3.6; val vr = 12 / 3.6
        val t = (((velocidadMs ?: 0.0) - vl) / (vr - vl)).coerceIn(0.0, 1.0)
        return lento + (rapido - lento) * t
    }
}

/** Trazo y circulo como GeoJSON (mismas propiedades que las capas de MapaLibre en Android). */
data class TrazoComun(val puntos: List<Coord>, val argb: Long, val anchoDp: Float, val opacidad: Float = 1f)
data class CirculoComun(val centro: Coord, val radioDp: Float, val relleno: Long, val borde: Long = 0xFFFFFFFF, val bordeDp: Float = 2f, val id: String? = null)

private fun hex(argb: Long) = "#" + (argb and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')
private fun alfa(argb: Long) = ((argb shr 24) and 0xFF) / 255.0

fun geojsonTrazos(t: List<TrazoComun>): String = t.filter { it.puntos.size >= 2 }.joinToString(",", "{\"type\":\"FeatureCollection\",\"features\":[", "]}") { tr ->
    val coords = tr.puntos.joinToString(",") { "[${it.lng},${it.lat}]" }
    "{\"type\":\"Feature\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[$coords]}," +
        "\"properties\":{\"color\":\"${hex(tr.argb)}\",\"ancho\":${tr.anchoDp},\"op\":${tr.opacidad * alfa(tr.argb)}}}"
}

fun geojsonCirculos(c: List<CirculoComun>): String = c.joinToString(",", "{\"type\":\"FeatureCollection\",\"features\":[", "]}") { ci ->
    val id = ci.id?.let { ",\"id\":${JsonPrimitive(it)}" } ?: ""
    "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[${ci.centro.lng},${ci.centro.lat}]}," +
        "\"properties\":{\"color\":\"${hex(ci.relleno)}\",\"r\":${ci.radioDp},\"op\":${alfa(ci.relleno)}," +
        "\"borde\":\"${hex(ci.borde)}\",\"bw\":${ci.bordeDp}$id}}"
}
