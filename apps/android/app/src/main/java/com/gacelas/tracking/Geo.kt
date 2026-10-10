package com.gacelas.tracking

import com.gacelas.data.carreras.PuntoGps
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val RADIO_TIERRA_M = 6_371_000.0

/** Distancia en metros entre dos coordenadas (formula de haversine). */
fun haversineM(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    return 2 * RADIO_TIERRA_M * asin(sqrt(a.coerceIn(0.0, 1.0)))
}

fun haversineM(a: PuntoGps, b: PuntoGps): Double = haversineM(a.lat, a.lng, b.lat, b.lng)

/** Formatea un ritmo en segundos por km como m:ss. */
fun formatearRitmo(segKm: Int?): String = if (segKm == null || segKm <= 0 || segKm > 59 * 60) "--:--" else "%d:%02d".format(segKm / 60, segKm % 60)

fun formatearHms(segundos: Int): String = "%02d:%02d:%02d".format(segundos / 3600, (segundos % 3600) / 60, segundos % 60)

fun formatearMs(segundos: Int): String = "%d:%02d".format(segundos / 60, segundos % 60)

/** Conversion de unidades segun la preferencia metrica o imperial. */
object Unidades {
    private const val KM_POR_MILLA = 1.609344
    fun distancia(km: Double, metricas: Boolean): Double = if (metricas) km else km / KM_POR_MILLA
    fun etiquetaDistancia(metricas: Boolean) = if (metricas) "km" else "mi"
    fun ritmo(segKm: Int?, metricas: Boolean): Int? = segKm?.let { if (metricas) it else (it * KM_POR_MILLA).toInt() }
    fun etiquetaRitmo(metricas: Boolean) = if (metricas) "/km" else "/mi"
    fun texto(km: Double, metricas: Boolean, decimales: Int = 2) = "%.${decimales}f".format(java.util.Locale.US, distancia(km, metricas))
}
