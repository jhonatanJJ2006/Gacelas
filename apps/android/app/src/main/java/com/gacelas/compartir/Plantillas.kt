package com.gacelas.compartir

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface

/** Plantillas transparentes (estilo Strava) para poner sobre una foto propia. Igual que la web. */
enum class Plantilla(val etiqueta: String, val ancho: Int, val alto: Int, val cx: Float, val cy: Float, val escala: Float) {
    TrazoDatos("Trazo + datos", 900, 900, 0.62f, 0.62f, 0.62f),
    Trazo("Solo trazo", 900, 900, 0.5f, 0.55f, 0.75f),
    DatosGrandes("Datos grandes", 900, 1000, 0.5f, 0.5f, 0.7f),
    Minimal("Minimal", 1000, 360, 0.5f, 0.86f, 0.85f),
}

enum class FormatoFoto(val etiqueta: String, val ancho: Int, val alto: Int) {
    Historia("Historia 9:16", 1080, 1920), Post45("Post 4:5", 1080, 1350), Cuadrado("Post 1:1", 1080, 1080)
}

data class Colocacion(val cx: Float, val cy: Float, val escala: Float) {
    fun limitada() = Colocacion(cx.coerceIn(0f, 1f), cy.coerceIn(0f, 1f), escala.coerceIn(0.15f, 1.6f))
}

const val HANDLE_GACELAS = "@gacelasrunningteam"
private const val NARANJA = 0xFFFF6A13.toInt()

object Plantillas {
    fun inicial(p: Plantilla) = Colocacion(p.cx, p.cy, p.escala)

    fun normalizarHandle(h: String): String {
        val l = h.trim().trimStart('@').replace(Regex("\\s+"), "").take(30)
        return if (l.isEmpty()) "" else "@$l"
    }

    fun datos(d: DatosCompartir): List<Pair<String, String>> = buildList {
        add("Distancia" to "%.2f km".format(java.util.Locale.US, d.distanciaKm))
        if (d.segundos > 0) { add("Ritmo" to "${Compartir.ritmo(d.ritmoSegKm)} /km"); add("Tiempo" to Compartir.tiempo(d.segundos)) }
        else d.desnivelM?.let { add("Desnivel" to "$it m") }
    }

    /** Recorte tipo cover (x, y, ancho, alto de la parte de la foto que se usa). */
    fun ajusteCubrir(iw: Int, ih: Int, w: Int, h: Int): RectF {
        val k = maxOf(w / iw.toFloat(), h / ih.toFloat())
        val sw = w / k; val sh = h / k
        return RectF((iw - sw) / 2, (ih - sh) / 2, (iw - sw) / 2 + sw, (ih - sh) / 2 + sh)
    }

    fun rect(p: Plantilla, c: Colocacion, w: Int, h: Int): RectF {
        val l = c.limitada()
        val ancho = l.escala * w
        val alto = ancho * p.alto / p.ancho
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

    private fun marca(c: Canvas, x: Float, y: Float, px: Float, a: Paint.Align = Paint.Align.LEFT) = c.drawText("GACELAS", x, y, pintura(px, 700, a, 0.35f))

    private fun handle(c: Canvas, h: String, x: Float, y: Float, px: Float, a: Paint.Align = Paint.Align.LEFT) {
        if (h.isEmpty()) return
        val p = pintura(px, 300, a)
        c.drawText(h, x, y, p)
        val w = p.measureText(h)
        val x0 = when (a) { Paint.Align.CENTER -> x - w / 2; Paint.Align.RIGHT -> x - w; else -> x }
        c.drawRect(x0, y + px * 0.22f, x0 + w, y + px * 0.22f + maxOf(1.5f, px * 0.05f), p)
    }

    /** PNG transparente de la plantilla. */
    fun generar(d: DatosCompartir, p: Plantilla, h: String): Bitmap {
        val bmp = Bitmap.createBitmap(p.ancho, p.alto, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val w = p.ancho.toFloat(); val hh = p.alto.toFloat()
        val datos = datos(d)
        when (p) {
            Plantilla.Trazo -> { linea(c, d, 40f, 30f, w - 80, hh - 150, 11f); marca(c, w / 2, hh - 70, 26f, Paint.Align.CENTER); handle(c, h, w / 2, hh - 22, 26f, Paint.Align.CENTER) }
            Plantilla.TrazoDatos -> {
                marca(c, 20f, 48f, 26f)
                linea(c, d, 10f, 80f, w * 0.62f, hh - 200, 9f)
                var y = 210f
                datos.forEach { (k, v) -> c.drawText(k.uppercase(), w * 0.7f, y, pintura(20f, 400, espaciado = 0.15f)); c.drawText(v, w * 0.7f, y + 46, pintura(40f, 300)); y += 130 }
                handle(c, h, w - 20, hh - 30, 28f, Paint.Align.RIGHT)
            }
            Plantilla.DatosGrandes -> {
                val dist = datos.first().second.removeSuffix(" km")
                c.drawText(dist, w / 2, 230f, pintura(210f, 200, Paint.Align.CENTER, -0.02f))
                c.drawText("KILÓMETROS", w / 2, 290f, pintura(26f, 400, Paint.Align.CENTER, 0.3f))
                val resto = datos.drop(1)
                resto.forEachIndexed { i, (k, v) ->
                    val x = if (resto.size == 1) w / 2 else w * (0.27f + i * 0.46f)
                    c.drawText(v, x, 400f, pintura(52f, 300, Paint.Align.CENTER)); c.drawText(k.uppercase(), x, 440f, pintura(20f, 400, Paint.Align.CENTER, 0.2f))
                }
                linea(c, d, w * 0.18f, 480f, w * 0.64f, 380f, 8f)
                marca(c, w / 2, hh - 70, 24f, Paint.Align.CENTER); handle(c, h, w / 2, hh - 24, 24f, Paint.Align.CENTER)
            }
            Plantilla.Minimal -> {
                linea(c, d, 10f, 20f, 200f, hh - 40, 6f)
                c.drawText(datos.joinToString("   ·   ") { it.second }, 250f, hh / 2 + 6, pintura(42f, 300))
                marca(c, 250f, hh / 2 - 70, 22f); handle(c, h, 250f, hh / 2 + 80, 24f)
            }
        }
        return bmp
    }

    /** Foto (cover) + plantilla colocada. Sin foto queda transparente. */
    fun componer(foto: Bitmap?, capa: Bitmap, p: Plantilla, col: Colocacion, f: FormatoFoto): Bitmap {
        val bmp = Bitmap.createBitmap(f.ancho, f.alto, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val filtro = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        foto?.let {
            val r = ajusteCubrir(it.width, it.height, f.ancho, f.alto)
            c.drawBitmap(it, Rect(r.left.toInt(), r.top.toInt(), r.right.toInt(), r.bottom.toInt()), Rect(0, 0, f.ancho, f.alto), filtro)
        }
        c.drawBitmap(capa, null, rect(p, col, f.ancho, f.alto), filtro)
        return bmp
    }
}
