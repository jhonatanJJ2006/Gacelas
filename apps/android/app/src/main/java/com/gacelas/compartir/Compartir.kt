package com.gacelas.compartir

import java.net.URLEncoder
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class FormatoCompartir(val etiqueta: String, val ancho: Int, val alto: Int) {
    Historia("Historia", 1080, 1920), Post("Post", 1080, 1080), Sticker("Sticker", 900, 900)
}

data class DatosCompartir(
    val tipo: Tipo,
    val titulo: String,
    val puntos: List<Pair<Double, Double>>,
    val distanciaKm: Double,
    val segundos: Int = 0,
    val ritmoSegKm: Int? = null,
    val desnivelM: Int? = null,
    val rachaDias: Int = 0,
    val enlace: String = SITIO,
) {
    enum class Tipo { Carrera, Ruta }
}

const val SITIO = "https://gacelas.vercel.app"
fun enlaceRuta(id: String) = "$SITIO/r/${URLEncoder.encode(id, "UTF-8")}"

/** Logica pura de la imagen para compartir (igual que apps/web/src/lib/compartir.ts). */
object Compartir {
    private fun hv(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
        val p1 = Math.toRadians(a.first); val p2 = Math.toRadians(b.first); val dl = Math.toRadians(b.second - a.second)
        val h = sin((p2 - p1) / 2).let { it * it } + cos(p1) * cos(p2) * sin(dl / 2).let { it * it }
        return 2 * 6_371_000 * asin(sqrt(h))
    }

    /** Privacidad: quita los primeros y ultimos [metros] para no revelar la casa del corredor. */
    fun recortarExtremos(p: List<Pair<Double, Double>>, metros: Double = 250.0): List<Pair<Double, Double>> {
        if (p.size < 3) return emptyList()
        fun corte(l: List<Pair<Double, Double>>): Int {
            var d = 0.0
            for (i in 1 until l.size) { d += hv(l[i - 1], l[i]); if (d >= metros) return i }
            return l.size
        }
        val ini = corte(p)
        val fin = p.size - 1 - corte(p.reversed())
        return if (ini <= fin) p.subList(ini, fin + 1) else emptyList()
    }

    /** Proyecta el trazo en la caja (x, y, ancho, alto), centrado y con el norte arriba. */
    fun proyectar(p: List<Pair<Double, Double>>, x: Float, y: Float, ancho: Float, alto: Float, margen: Float = 0.08f): List<Pair<Float, Float>> {
        if (p.isEmpty()) return emptyList()
        val k0 = cos(Math.toRadians(p[0].first))
        val xs = p.map { it.second * k0 }; val ys = p.map { -it.first }
        val minX = xs.min(); val minY = ys.min()
        val w = maxOf(xs.max() - minX, 1e-9); val h = maxOf(ys.max() - minY, 1e-9)
        val k = minOf(ancho * (1 - 2 * margen) / w, alto * (1 - 2 * margen) / h)
        val ox = x + (ancho - w * k) / 2; val oy = y + (alto - h * k) / 2
        return xs.indices.map { i -> (ox + (xs[i] - minX) * k).toFloat() to (oy + (ys[i] - minY) * k).toFloat() }
    }

    private fun dos(n: Int) = n.toString().padStart(2, '0')
    fun tiempo(s: Int): String { val h = s / 3600; val m = (s % 3600) / 60; return if (h > 0) "$h:${dos(m)}:${dos(s % 60)}" else "$m:${dos(s % 60)}" }
    fun ritmo(s: Int?) = if (s == null || s <= 0) "--" else "${s / 60}'${dos(s % 60)}\""

    fun metricas(d: DatosCompartir): List<Pair<String, String>> = buildList {
        add("Distancia" to "%.2f km".format(java.util.Locale.US, d.distanciaKm))
        if (d.segundos > 0) { add("Tiempo" to tiempo(d.segundos)); add("Ritmo /km" to ritmo(d.ritmoSegKm)) }
        d.desnivelM?.let { add("Desnivel" to "$it m") }
    }

    fun texto(d: DatosCompartir): String {
        val base = if (d.tipo == DatosCompartir.Tipo.Carrera) "Corrí ${"%.2f".format(java.util.Locale.US, d.distanciaKm)} km" + (if (d.segundos > 0) " en ${tiempo(d.segundos)}" else "") + " con Gacelas"
        else "Mira esta ruta en Gacelas: ${d.titulo} (${"%.1f".format(java.util.Locale.US, d.distanciaKm)} km)"
        val racha = if (d.rachaDias > 1) " · racha de ${d.rachaDias} días" else ""
        return "$base$racha 🦌🔥"
    }

    fun enlaceWhatsApp(d: DatosCompartir) = "https://wa.me/?text=" + URLEncoder.encode("${texto(d)}\n${d.enlace}", "UTF-8").replace("+", "%20")

}
