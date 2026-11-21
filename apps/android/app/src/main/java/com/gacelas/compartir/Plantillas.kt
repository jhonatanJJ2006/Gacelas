package com.gacelas.compartir

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Plantillas transparentes para poner sobre una foto propia. */
enum class Plantilla(val etiqueta: String, val cx: Float = 0.5f, val cy: Float = 0.5f, val escala: Float = 1f) {
    Estadisticas("Estadísticas"),
    Traza("Traza de ruta"),
    Fecha("Día y fecha"),
    Logro("Logro / racha"),
    Distancia("Distancia"),
    Lateral("Lateral"),
    Tarjeta("Tarjeta"),
}

/** Ajuste solo de la foto de fondo. Los valores por defecto no la cambian. */
data class AjusteFoto(
    val brillo: Float = 0f,
    val contraste: Float = 1f,
    val saturacion: Float = 1f,
    val opacidad: Float = 1f,
) {
    fun esIdentidad() = brillo == 0f && contraste == 1f && saturacion == 1f && opacidad == 1f

    /** Saturación primero; después brillo, contraste y opacidad. */
    fun matriz(): ColorMatrix {
        val m = ColorMatrix()
        m.setSaturation(saturacion.coerceIn(0f, 2f))
        val c = contraste.coerceIn(0f, 2f)
        val t = (1f - c) * 128f + brillo.coerceIn(-1f, 1f) * 255f
        val a = opacidad.coerceIn(0f, 1f)
        m.postConcat(ColorMatrix(floatArrayOf(
            c, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c, 0f, t,
            0f, 0f, 0f, a, 0f,
        )))
        return m
    }
}

enum class FormatoFoto(val etiqueta: String, val ancho: Int, val alto: Int) {
    Historia("Historia 9:16", 1080, 1920), Post45("Post 4:5", 1080, 1350), Cuadrado("Post 1:1", 1080, 1080)
}

data class Colocacion(val cx: Float, val cy: Float, val escala: Float) {
    fun limitada() = Colocacion(cx.coerceIn(0f, 1f), cy.coerceIn(0f, 1f), escala.coerceIn(0.15f, 1.6f))
}

const val HANDLE_GACELAS = HANDLE_RUNCITY
private const val NARANJA = 0xFFC084FC.toInt()

object Plantillas {
    fun inicial(p: Plantilla) = Colocacion(p.cx, p.cy, p.escala)

    fun normalizarHandle(h: String): String {
        val l = h.trim().trimStart('@').replace(Regex("\\s+"), "").take(30)
        return if (l.isEmpty()) HANDLE_RUNCITY else "@$l"
    }

    fun insignia(d: DatosCompartir): String? =
        d.recordLabel?.takeIf { it.isNotBlank() }?.let { "Récord personal · $it" }
            ?: d.rachaDias.takeIf { it > 0 }?.let { "Racha de $it ${if (it == 1) "día" else "días"}" }

    fun datos(d: DatosCompartir): List<Pair<String, String>> = buildList {
        add("Distancia" to "%.2f km".format(java.util.Locale.US, d.distanciaKm))
        if (d.segundos > 0) { add("Ritmo" to "${Compartir.ritmo(d.ritmoSegKm)} /km"); add("Tiempo" to Compartir.tiempo(d.segundos)) }
        d.desnivelM?.let { add("Desnivel" to "$it m") }
    }

    /** Recorte tipo cover (x, y, ancho, alto de la parte de la foto que se usa). */
    fun ajusteCubrir(iw: Int, ih: Int, w: Int, h: Int): RectF {
        val k = maxOf(w / iw.toFloat(), h / ih.toFloat())
        val sw = w / k; val sh = h / k
        return RectF((iw - sw) / 2, (ih - sh) / 2, (iw - sw) / 2 + sw, (ih - sh) / 2 + sh)
    }

    fun rect(c: Colocacion, w: Int, h: Int, f: FormatoFoto): RectF {
        val l = c.limitada()
        val ancho = l.escala * w
        val alto = ancho * f.alto / f.ancho
        return RectF(l.cx * w - ancho / 2, l.cy * h - alto / 2, l.cx * w + ancho / 2, l.cy * h + alto / 2)
    }

    private fun pintura(px: Float, peso: Int, alinear: Paint.Align = Paint.Align.LEFT, espaciado: Float = 0f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = px
        textAlign = alinear
        letterSpacing = espaciado
        typeface = if (android.os.Build.VERSION.SDK_INT >= 28) Typeface.create(Typeface.create("sans-serif", Typeface.NORMAL), peso, false)
        else if (peso >= 600) Typeface.DEFAULT_BOLD else Typeface.create("sans-serif-light", Typeface.NORMAL)
        setShadowLayer(8f, 0f, 1f, Color.argb(115, 0, 0, 0))
    }

    private fun linea(c: Canvas, d: DatosCompartir, x: Float, y: Float, w: Float, h: Float, grosor: Float) {
        val pts = Compartir.proyectar(d.puntos, x, y, w, h, 0.04f)
        if (pts.size < 2) return
        val path = Path().apply { moveTo(pts[0].first, pts[0].second); pts.drop(1).forEach { lineTo(it.first, it.second) } }
        val sombra = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = grosor + 4; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
            color = Color.argb(90, 0, 0, 0); maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
        }
        c.drawPath(path, sombra)
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = grosor; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = NARANJA })
    }

    private fun marca(c: Canvas, x: Float, y: Float, px: Float, a: Paint.Align = Paint.Align.LEFT) = c.drawText("RUNCITY", x, y, pintura(px, 700, a, 0.35f))

    private fun handle(c: Canvas, h: String, x: Float, y: Float, px: Float, a: Paint.Align = Paint.Align.LEFT) {
        if (h.isEmpty()) return
        val p = pintura(px, 300, a)
        c.drawText(h, x, y, p)
        val w = p.measureText(h)
        val x0 = when (a) { Paint.Align.CENTER -> x - w / 2; Paint.Align.RIGHT -> x - w; else -> x }
        c.drawRect(x0, y + px * 0.22f, x0 + w, y + px * 0.22f + maxOf(1.5f, px * 0.05f), p)
    }

    /** PNG transparente de la plantilla en el formato seleccionado. */
    fun generar(d: DatosCompartir, p: Plantilla, h: String, f: FormatoFoto = FormatoFoto.Historia): Bitmap {
        val bmp = Bitmap.createBitmap(f.ancho, f.alto, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val w = f.ancho.toFloat(); val hh = f.alto.toFloat()
        val sx = w / 1080f
        val sy = hh / 1920f
        fun texto(t: String, x: Float, y: Float, px: Float, peso: Int = 500, a: Paint.Align = Paint.Align.LEFT) {
            c.drawText(t, x * sx, y * sy, pintura(px * sx, peso, a))
        }
        fun pie() {
            marca(c, w / 2, 1770f * sy, 36f * sx, Paint.Align.CENTER)
            handle(c, Plantillas.normalizarHandle(h), w / 2, 1840f * sy, 30f * sx, Paint.Align.CENTER)
        }
        fun etiqueta(t: String, x: Float, y: Float, valor: String, a: Paint.Align = Paint.Align.LEFT) {
            texto(t.uppercase(), x, y, 24f, 600, a)
            texto(valor, x, y + 64f, 52f, 700, a)
        }
        when (p) {
            Plantilla.Estadisticas -> {
                texto("DISTANCIA", 72f, 350f, 28f, 600)
                texto("%.2f".format(java.util.Locale.US, d.distanciaKm), 72f, 560f, 172f, 800)
                texto("KILÓMETROS", 80f, 625f, 30f, 600)
                val ritmo = Compartir.ritmo(d.ritmoSegKm)
                etiqueta("Tiempo", 80f, 850f, Compartir.tiempo(d.segundos))
                etiqueta("Ritmo / km", 580f, 850f, ritmo)
                insignia(d)?.let {
                    c.drawRoundRect(
                        RectF(64f * sx, 1130f * sy, w - 64f * sx, 1250f * sy),
                        36f * sx, 36f * sx,
                        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x38C084FC },
                    )
                    texto(it, 540f, 1210f, 38f, 700, Paint.Align.CENTER)
                }
                pie()
            }
            Plantilla.Traza -> {
                linea(c, d, 70f * sx, 250f * sy, w - 140f * sx, 760f * sy, 12f * sx)
                etiqueta("Distancia", 80f, 1170f, "%.2f km".format(java.util.Locale.US, d.distanciaKm))
                etiqueta("Tiempo", 570f, 1170f, Compartir.tiempo(d.segundos))
                etiqueta("Ritmo / km", 80f, 1435f, Compartir.ritmo(d.ritmoSegKm))
                etiqueta("Desnivel", 570f, 1435f, "${d.desnivelM ?: 0} m")
                pie()
            }
            Plantilla.Fecha -> {
                val fecha = SimpleDateFormat("EEEE|d 'de' MMMM yyyy", Locale("es", "EC"))
                    .format(Date(d.fechaMs)).split('|')
                texto(fecha[0].replaceFirstChar { it.uppercase() }, 72f, 720f, 76f, 700)
                texto(fecha.getOrElse(1) { "" }, 72f, 900f, 54f, 400)
                texto("%.2f km".format(java.util.Locale.US, d.distanciaKm), 72f, 1160f, 64f, 700)
                pie()
            }
            Plantilla.Logro -> {
                val logro = insignia(d)
                texto(logro ?: "CADA CARRERA CUENTA", 72f, 600f, 58f, 800)
                texto("%.2f km".format(java.util.Locale.US, d.distanciaKm), 72f, 850f, 88f, 700)
                if (d.segundos > 0) {
                    etiqueta("Tiempo", 80f, 1100f, Compartir.tiempo(d.segundos))
                    etiqueta("Ritmo / km", 570f, 1100f, Compartir.ritmo(d.ritmoSegKm))
                }
                pie()
            }
            Plantilla.Distancia -> {
                texto("%.2f".format(java.util.Locale.US, d.distanciaKm), 540f, 900f, 168f, 800, Paint.Align.CENTER)
                texto("KILÓMETROS", 540f, 990f, 32f, 600, Paint.Align.CENTER)
                pie()
            }
            Plantilla.Lateral -> {
                linea(c, d, 48f * sx, 220f * sy, 560f * sx, 1400f * sy, 14f * sx)
                etiqueta("Distancia", 1020f, 640f, "%.2f km".format(java.util.Locale.US, d.distanciaKm), Paint.Align.RIGHT)
                etiqueta("Tiempo", 1020f, 860f, Compartir.tiempo(d.segundos), Paint.Align.RIGHT)
                etiqueta("Ritmo", 1020f, 1080f, Compartir.ritmo(d.ritmoSegKm), Paint.Align.RIGHT)
                etiqueta("Desnivel", 1020f, 1300f, "${d.desnivelM ?: 0} m", Paint.Align.RIGHT)
                pie()
            }
            Plantilla.Tarjeta -> {
                val left = 160f * sx
                val top = 640f * sy
                val right = w - 160f * sx
                val bottom = 1180f * sy
                val borde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 4f * sx
                    color = Color.WHITE
                }
                c.drawRoundRect(RectF(left, top, right, bottom), 28f * sx, 28f * sx, borde)
                val diente = Path()
                diente.moveTo(left, bottom)
                val paso = (right - left) / 16f
                var x = left
                while (x < right - 0.5f) {
                    val siguiente = minOf(x + paso, right)
                    diente.lineTo((x + siguiente) / 2f, bottom + 22f * sy)
                    diente.lineTo(siguiente, bottom)
                    x = siguiente
                }
                c.drawPath(diente, borde)
                val fecha = SimpleDateFormat("EEEE|d 'de' MMMM", Locale("es", "EC"))
                    .format(Date(d.fechaMs)).split('|')
                texto(fecha[0].replaceFirstChar { it.uppercase() }, 540f, 820f, 64f, 700, Paint.Align.CENTER)
                texto("%.2f km".format(java.util.Locale.US, d.distanciaKm), 540f, 960f, 72f, 800, Paint.Align.CENTER)
                texto(fecha.getOrElse(1) { "" }, 540f, 1060f, 36f, 400, Paint.Align.CENTER)
                pie()
            }
        }
        return bmp
    }

    /** Foto (cover) + plantilla colocada. El ajuste solo tiñe la foto. Sin foto queda transparente. */
    fun componer(
        foto: Bitmap?,
        capa: Bitmap,
        p: Plantilla,
        col: Colocacion,
        f: FormatoFoto,
        ajuste: AjusteFoto = AjusteFoto(),
    ): Bitmap {
        val bmp = Bitmap.createBitmap(f.ancho, f.alto, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val filtro = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        foto?.let {
            val pintura = Paint(filtro)
            if (!ajuste.esIdentidad()) pintura.colorFilter = ColorMatrixColorFilter(ajuste.matriz())
            val r = ajusteCubrir(it.width, it.height, f.ancho, f.alto)
            c.drawBitmap(it, Rect(r.left.toInt(), r.top.toInt(), r.right.toInt(), r.bottom.toInt()), Rect(0, 0, f.ancho, f.alto), pintura)
        }
        c.drawBitmap(capa, null, rect(col, f.ancho, f.alto, f), filtro)
        return bmp
    }
}
