package com.gacelas.compartir

import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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
    val fechaMs: Long = 0L,
    val recordLabel: String? = null,
    val handle: String = HANDLE_RUNCITY,
) {
    enum class Tipo { Carrera, Ruta }
}

const val SITIO = "https://gacelas.vercel.app"
const val HANDLE_RUNCITY = "@runcity"
fun enlaceRuta(id: String) = "$SITIO/r/${URLEncoder.encode(id, "UTF-8")}"

/** Logica pura de la imagen para compartir (igual que apps/web/src/lib/compartir.ts). */
object Compartir {
    private fun hv(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
        val p1 = Math.toRadians(a.first); val p2 = Math.toRadians(b.first); val dl = Math.toRadians(b.second - a.second)
        val h = sin((p2 - p1) / 2).let { it * it } + cos(p1) * cos(p2) * sin(dl / 2).let { it * it }
        return 2 * 6_371_000 * asin(sqrt(h))
    }

    /** Privacidad: quita los puntos dentro del radio de salida y llegada. */
    fun recortarExtremos(p: List<Pair<Double, Double>>, metros: Double = 300.0): List<Pair<Double, Double>> {
        if (p.size < 3) return emptyList()
        fun corte(l: List<Pair<Double, Double>>): Int {
            val origen = l.first()
            for (i in 1 until l.size) if (hv(origen, l[i]) > metros) return i
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
        val base = if (d.tipo == DatosCompartir.Tipo.Carrera) "Corrí ${"%.2f".format(java.util.Locale.US, d.distanciaKm)} km" + (if (d.segundos > 0) " en ${tiempo(d.segundos)}" else "") + " con Runcity"
        else "Mira esta ruta en Runcity: ${d.titulo} (${"%.1f".format(java.util.Locale.US, d.distanciaKm)} km)"
        val racha = if (d.rachaDias > 1) " · racha de ${d.rachaDias} días" else ""
        return "$base$racha 🦌🔥"
    }

    fun enlaceWhatsApp(d: DatosCompartir) = "https://wa.me/?text=" + URLEncoder.encode("${texto(d)}\n${d.enlace}", "UTF-8").replace("+", "%20")

    /** 1 de enero de 2000 00:00 UTC. Por debajo no es una fecha de carrera creible. */
    const val MIN_FECHA_MS = 946_684_800_000L

    /**
     * Timestamp en milisegundos si es una fecha real.
     * Acepta segundos Unix y rechaza 0 o la epoca Unix.
     */
    fun fechaValidaMs(ms: Long): Long? = when {
        ms in (MIN_FECHA_MS / 1000) until MIN_FECHA_MS -> ms * 1000L
        ms >= MIN_FECHA_MS -> ms
        else -> null
    }

    /** Fecha a mostrar en una plantilla: la de la carrera o un respaldo (nunca 1970). */
    fun fechaPlantillaMs(ms: Long, respaldoMs: Long = System.currentTimeMillis()): Long =
        fechaValidaMs(ms) ?: respaldoMs

    fun textoFechaPlantilla(
        ms: Long,
        respaldoMs: Long = System.currentTimeMillis(),
        patron: String = "EEEE|d 'de' MMMM yyyy",
    ): List<String> {
        val fmt = SimpleDateFormat(patron, Locale("es", "EC")).apply {
            timeZone = TimeZone.getTimeZone("America/Guayaquil")
        }
        return fmt.format(Date(fechaPlantillaMs(ms, respaldoMs))).split('|').mapIndexed { i, parte ->
            if (i == 0) parte.replaceFirstChar { it.uppercase() } else parte
        }
    }
}
