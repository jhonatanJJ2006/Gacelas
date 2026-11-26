package com.gacelas.mapa

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

/** Espacio (px) que tapan paneles, carruseles o botones sobre el mapa, por lado. */
data class PaddingPx(val arriba: Float, val derecha: Float, val abajo: Float, val izquierda: Float) {
    companion object {
        fun todos(v: Float) = PaddingPx(v, v, v, v)
    }
}

/** Camara que deja una ruta entera a la vista. */
data class CamaraRuta(val centro: Coord, val zoom: Double)

/**
 * Cuentas del encuadre de rutas (fit bounds con padding), puras y sin dependencias de plataforma:
 * las usan MapLibre en Android y las pruebas. El mapa MapLibre usa teselas de 512 px.
 */
object EncuadreMapa {
    const val DURACION_MS = 900
    /** Zoom con el que se centra una ruta de un solo punto. */
    const val ZOOM_UN_PUNTO = 15.5
    const val ZOOM_MAXIMO = 17.0
    const val ZOOM_MINIMO = 3.0
    /** Margen visual por defecto entre la ruta y el borde del mapa (dp). */
    const val PADDING_BASE_DP = 40f
    private const val TESELA = 512.0

    private fun x(lng: Double) = (lng + 180.0) / 360.0
    private fun y(lat: Double): Double {
        val l = lat.coerceIn(-85.0511, 85.0511)
        return 0.5 - ln(tan(PI / 4 + l * PI / 360.0)) / (2 * PI)
    }
    private fun lat(y: Double) = atan(sinh(PI * (1 - 2 * y))) * 180.0 / PI
    private fun lng(x: Double) = x * 360.0 - 180.0

    /**
     * Centro y zoom que dejan TODOS los puntos dentro del area libre (vista menos padding). El
     * centro compensa el padding asimetrico: la ruta queda centrada en la franja visible, por
     * ejemplo encima de un carrusel inferior. Con un solo punto (o puntos casi iguales) centra con
     * [zoomUnPunto]. Devuelve null sin puntos validos.
     */
    fun camaraParaPuntos(
        puntos: List<Coord>,
        anchoPx: Float,
        altoPx: Float,
        padding: PaddingPx,
        zoomMaximo: Double = ZOOM_MAXIMO,
        zoomMinimo: Double = ZOOM_MINIMO,
        zoomUnPunto: Double = ZOOM_UN_PUNTO,
    ): CamaraRuta? {
        val validos = puntos.filter { it.lat.isFinite() && it.lng.isFinite() }
        if (validos.isEmpty() || anchoPx <= 0f || altoPx <= 0f) return null
        // Si los paneles tapan casi todo el mapa se reduce el padding para dejar al menos 25 % libre.
        val sumaH = (padding.izquierda + padding.derecha).toDouble()
        val sumaV = (padding.arriba + padding.abajo).toDouble()
        val libreAncho = max(anchoPx - sumaH, anchoPx * 0.25)
        val libreAlto = max(altoPx - sumaV, altoPx * 0.25)
        val kH = if (sumaH > 0) min(1.0, (anchoPx - libreAncho) / sumaH) else 1.0
        val kV = if (sumaV > 0) min(1.0, (altoPx - libreAlto) / sumaV) else 1.0
        val izq = padding.izquierda * kH
        val der = padding.derecha * kH
        val arr = padding.arriba * kV
        val aba = padding.abajo * kV

        val xs = validos.map { x(it.lng) }
        val ys = validos.map { y(it.lat) }
        val minX = xs.min(); val maxX = xs.max(); val minY = ys.min(); val maxY = ys.max()
        val dx = maxX - minX
        val dy = maxY - minY

        // Menos de ~2 m de extension: es un punto.
        val unPunto = max(dx, dy) * TESELA * 2.0.pow(20) < 8
        val z = if (unPunto) zoomUnPunto else {
            val zx = if (dx > 0) log2((anchoPx - izq - der) / (dx * TESELA)) else Double.POSITIVE_INFINITY
            val zy = if (dy > 0) log2((altoPx - arr - aba) / (dy * TESELA)) else Double.POSITIVE_INFINITY
            min(min(zx, zy), zoomMaximo)
        }
        val zoom = z.coerceIn(zoomMinimo, zoomMaximo)

        // El centro de la ruta va al centro del area libre, desplazada respecto al centro de la vista.
        val mundo = TESELA * 2.0.pow(zoom)
        val cx = (minX + maxX) / 2 - (izq - der) / 2 / mundo
        val cy = (minY + maxY) / 2 - (arr - aba) / 2 / mundo
        return CamaraRuta(Coord(lat(cy), lng(cx)), zoom)
    }

    /** Padding (px) con los dp dados y la densidad de la pantalla. */
    fun paddingDp(
        densidad: Float,
        arriba: Float = PADDING_BASE_DP,
        derecha: Float = PADDING_BASE_DP,
        abajo: Float = PADDING_BASE_DP,
        izquierda: Float = PADDING_BASE_DP,
    ) = PaddingPx(arriba * densidad, derecha * densidad, abajo * densidad, izquierda * densidad)
}
