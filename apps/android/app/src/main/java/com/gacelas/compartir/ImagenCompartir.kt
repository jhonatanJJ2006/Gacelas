package com.gacelas.compartir

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.gacelas.R

/** Dibuja la imagen de marca (misma composicion que la web) en un Bitmap. */
object ImagenCompartir {
    private const val NARANJA = 0xFFA855F7.toInt()
    private const val GRIS = 0xFF94A3B8.toInt()
    private const val BLANCO = 0xFFFFFFFF.toInt()

    fun generar(context: Context, d: DatosCompartir, f: FormatoCompartir): Bitmap {
        val w = f.ancho.toFloat(); val h = f.alto.toFloat()
        val bmp = Bitmap.createBitmap(f.ancho, f.alto, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.compartir_logo)
        val texto = { peso: Int, px: Float, color: Int -> Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = px; this.color = color; typeface = Typeface.create(Typeface.SANS_SERIF, if (peso >= 700) Typeface.BOLD else Typeface.NORMAL) } }

        if (f == FormatoCompartir.Sticker) {
            c.drawRoundRect(RectF(0f, 0f, w, h), 64f, 64f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xD10B1220.toInt() })
            trazo(c, Compartir.proyectar(d.puntos, 60f, 140f, w - 120, 470f), 14f)
            cabecera(c, logo, 60f, 50f, 44f, texto)
            val m = Compartir.metricas(d).take(3)
            m.forEachIndexed { i, (e, v) ->
                val x = 60 + i * (w - 120) / m.size
                c.drawText(v, x, 720f, texto(800, 54f, BLANCO)); c.drawText(e.uppercase(), x, 770f, texto(600, 30f, GRIS))
            }
            if (d.rachaDias > 0) c.drawText("🔥 ${d.rachaDias} días de racha", 60f, 850f, texto(800, 40f, NARANJA))
            return bmp
        }

        c.drawRect(0f, 0f, w, h, Paint().apply { shader = LinearGradient(0f, 0f, 0f, h, 0xFF0B1220.toInt(), 0xFF111827.toInt(), Shader.TileMode.CLAMP) })
        val historia = f == FormatoCompartir.Historia
        val mapa = if (historia) RectF(60f, 260f, w - 60, 1160f) else RectF(60f, 170f, 680f, 730f)
        c.save()
        c.clipPath(Path().apply { addRoundRect(mapa, 40f, 40f, Path.Direction.CW) })
        c.drawRect(mapa, Paint().apply { color = 0xFF0F1A2E.toInt() })
        val calle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1F2A3D.toInt(); strokeWidth = 6f }
        var i = -mapa.height()
        while (i < mapa.width()) { c.drawLine(mapa.left + i, mapa.top, mapa.left + i + mapa.height() * 0.45f, mapa.bottom, calle); i += 90 }
        var j = 40f
        while (j < mapa.height()) { c.drawLine(mapa.left, mapa.top + j, mapa.right, mapa.top + j + 30, calle); j += 120 }
        c.drawOval(RectF(mapa.left + mapa.width() * 0.75f - 120, mapa.top + mapa.height() * 0.3f - 80, mapa.left + mapa.width() * 0.75f + 120, mapa.top + mapa.height() * 0.3f + 80), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x8C064E3B.toInt() })
        trazo(c, Compartir.proyectar(d.puntos, mapa.left, mapa.top, mapa.width(), mapa.height(), 0.12f), if (historia) 16f else 12f)
        c.restore()

        BitmapFactory.decodeResource(context.resources, R.drawable.compartir_corredor)?.let { r ->
            val alto = if (historia) 520f else 580f
            val ancho = r.width.toFloat() / r.height * alto
            val x = if (historia) w - ancho - 20 else w - ancho + 30
            val y = if (historia) 730f else 120f
            c.drawBitmap(r, null, RectF(x, y, x + ancho, y + alto), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        cabecera(c, logo, 60f, if (historia) 110f else 50f, if (historia) 60f else 48f, texto)
        c.drawText(recortar(d.titulo, texto(800, if (historia) 64f else 50f, BLANCO), w - 120), 60f, if (historia) 220f else 145f, texto(800, if (historia) 64f else 50f, BLANCO))

        val m = Compartir.metricas(d)
        val filaY = if (historia) 1260f else 820f
        val col = (w - 120) / minOf(m.size, if (historia) 2 else 4)
        m.forEachIndexed { k, (e, v) ->
            val fila = if (historia) k / 2 else 0
            val x = 60 + (if (historia) k % 2 else k) * col
            val y = filaY + fila * 200
            c.drawText(v, x, y + if (historia) 80 else 50, texto(800, if (historia) 92f else 50f, if (k == 0) NARANJA else BLANCO))
            c.drawText(e.uppercase(), x, y + if (historia) 130 else 90, texto(600, if (historia) 34f else 26f, GRIS))
        }
        val pieY = if (historia) 1760f else 1010f
        if (d.rachaDias > 0) {
            val alto = if (historia) 96f else 70f
            c.drawRoundRect(RectF(60f, pieY - (if (historia) 70 else 52), 60f + (if (historia) 520 else 400), pieY - (if (historia) 70 else 52) + alto), 48f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x29F59E0B })
            c.drawText("🔥 ${d.rachaDias} ${if (d.rachaDias == 1) "día" else "días"} de racha", 90f, pieY - (if (historia) 7 else 6), texto(800, if (historia) 44f else 34f, NARANJA))
        }
        c.drawText("gacelas.vercel.app", w - 60, pieY - 10, texto(600, if (historia) 32f else 26f, GRIS).apply { textAlign = Paint.Align.RIGHT })
        return bmp
    }

    private fun cabecera(c: Canvas, logo: Bitmap?, x: Float, y: Float, px: Float, texto: (Int, Float, Int) -> Paint) {
        logo?.let { c.drawBitmap(it, null, RectF(x - 10, y - px * 1.1f, x - 10 + px * 1.6f, y - px * 1.1f + px * 1.6f), Paint(Paint.FILTER_BITMAP_FLAG)) }
        c.drawText("RUNCITY", x + px * 1.6f, y + px * 0.25f, texto(900, px, BLANCO))
    }

    private fun trazo(c: Canvas, pts: List<Pair<Float, Float>>, ancho: Float) {
        if (pts.size < 2) return
        val path = Path().apply { moveTo(pts[0].first, pts[0].second); pts.drop(1).forEach { lineTo(it.first, it.second) } }
        val base = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = NARANJA; strokeWidth = ancho }
        c.drawPath(path, Paint(base).apply { strokeWidth = ancho * 2.2f; alpha = 90; maskFilter = BlurMaskFilter(24f, BlurMaskFilter.Blur.NORMAL) })
        c.drawPath(path, base)
        fun punto(p: Pair<Float, Float>, color: Int) {
            c.drawCircle(p.first, p.second, ancho * 1.3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
            c.drawCircle(p.first, p.second, ancho * 1.3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = BLANCO; style = Paint.Style.STROKE; strokeWidth = 5f })
        }
        punto(pts.first(), 0xFF10B981.toInt()); punto(pts.last(), 0xFFEF4444.toInt())
    }

    private fun recortar(t: String, p: Paint, max: Float): String {
        if (p.measureText(t) <= max) return t
        var s = t
        while (s.length > 1 && p.measureText("$s…") > max) s = s.dropLast(1)
        return "$s…"
    }
}
