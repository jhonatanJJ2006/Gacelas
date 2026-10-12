package com.gacelas.ui.components

import com.gacelas.data.carreras.PuntoGps
import com.gacelas.ui.three.ClipCorredor
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Configuracion de mapas (MapLibre + teselas vectoriales de OpenStreetMap) y trazados de ejemplo. */
object Mapas {
    val CENTRO_LOJA = LatLng(-3.9931, -79.2042)

    /** Estilos Gacelas en assets/mapas (teselas OpenFreeMap con datos de OpenStreetMap, sin clave). */
    const val ESTILO_OSCURO = "asset://mapas/gacelas-oscuro.json"
    const val ESTILO_CLARO = "asset://mapas/gacelas-claro.json"
    /** Alias: todas las pantallas usan el estilo Gacelas. */
    const val ESTILO_GACELAS = ESTILO_OSCURO

    /**
     * MapLibre no necesita clave. En las pruebas JVM (Robolectric) no hay codigo nativo, asi que
     * ahi se muestran las alternativas dibujadas en Compose.
     */
    fun configurado(): Boolean = android.os.Build.FINGERPRINT != "robolectric"

    val trazos: Map<String, List<LatLng>> =
        com.gacelas.data.rutas.TrazosRutas.puntos.mapValues { (_, p) -> p.map { (lat, lng) -> LatLng(lat, lng) } }

    fun trazo(id: String?): List<LatLng> = id?.removePrefix("ruta-")?.let { trazos[it] } ?: emptyList()
}

fun List<PuntoGps>.aLatLng() = map { LatLng(it.lat, it.lng) }

/**
 * Zoom de la vista 3D segun la velocidad. Caminando o trotando suave (5-8 km/h) el avance por
 * actualizacion GPS es de pocos metros, asi que se acerca mas la camara para que el suelo se
 * vea correr; a ritmo de carrera se aleja un poco para ver por delante.
 */
object ZoomNavegacion {
    const val ZOOM_LENTO = 19.5
    const val ZOOM_RAPIDO = 18.3
    const val VEL_LENTA_MS = 8 / 3.6
    const val VEL_RAPIDA_MS = 12 / 3.6

    fun para(velocidadMs: Double?): Double {
        val v = velocidadMs ?: 0.0
        val t = ((v - VEL_LENTA_MS) / (VEL_RAPIDA_MS - VEL_LENTA_MS)).coerceIn(0.0, 1.0)
        return ZOOM_LENTO + (ZOOM_RAPIDO - ZOOM_LENTO) * t
    }

    /** Metros por punto de pantalla (dp) en MapLibre (teselas de 512) a esa latitud y zoom. */
    fun metrosPorDp(latitud: Double, zoom: Double): Double = 40_075_016.686 * cos(Math.toRadians(latitud)) / (512 * 2.0.pow(zoom))

    /** Cuantos dp por segundo se desplaza el suelo bajo el corredor (sin contar la inclinacion). */
    fun avanceDpPorSegundo(velocidadMs: Double, latitud: Double = -4.0): Double = velocidadMs / metrosPorDp(latitud, para(velocidadMs))
}

/** Matematicas del mapa (compartidas con las pruebas). */
object MapaMat {
    private fun rad(g: Double) = Math.toRadians(g)
    fun rumbo(a: LatLng, b: LatLng): Float {
        val f1 = rad(a.latitude); val f2 = rad(b.latitude); val dl = rad(b.longitude - a.longitude)
        val y = sin(dl) * cos(f2); val x = cos(f1) * sin(f2) - sin(f1) * cos(f2) * cos(dl)
        return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
    }
    fun distanciaM(a: LatLng, b: LatLng): Double {
        val dLat = rad(b.latitude - a.latitude); val dLng = rad(b.longitude - a.longitude)
        val h = sin(dLat / 2).pow(2) + cos(rad(a.latitude)) * cos(rad(b.latitude)) * sin(dLng / 2).pow(2)
        return 2 * 6371000 * asin(sqrt(h))
    }
    fun desplazar(p: LatLng, rumboGrados: Double, metros: Double): LatLng {
        val d = metros / 6371000; val r = rad(rumboGrados); val f1 = rad(p.latitude); val l1 = rad(p.longitude)
        val f2 = asin(sin(f1) * cos(d) + cos(f1) * sin(d) * cos(r))
        val l2 = l1 + atan2(sin(r) * sin(d) * cos(f1), cos(d) - sin(f1) * sin(f2))
        return LatLng(Math.toDegrees(f2), Math.toDegrees(l2))
    }
    fun rumboReciente(p: List<LatLng>, metros: Double = 15.0): Float? {
        if (p.size < 2) return null
        val fin = p.last()
        for (i in p.size - 2 downTo 0) if (distanciaM(p[i], fin) >= metros || i == 0) return rumbo(p[i], fin)
        return null
    }
    fun clipPorRitmo(segKm: Int?): ClipCorredor = when {
        segKm == null || segKm <= 0 || segKm > 900 -> ClipCorredor.Idle
        segKm > 360 -> ClipCorredor.Jog
        else -> ClipCorredor.Run
    }
    fun limites(p: List<LatLng>): LatLngBounds = LatLngBounds.Builder().apply { p.forEach { include(it) } }.build()
}
