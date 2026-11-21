package com.gacelas

import android.graphics.Bitmap
import android.graphics.Color
import com.gacelas.compartir.AjusteFoto
import com.gacelas.compartir.Colocacion
import com.gacelas.compartir.DatosCompartir
import com.gacelas.compartir.FormatoFoto
import com.gacelas.compartir.HANDLE_GACELAS
import com.gacelas.compartir.Plantilla
import com.gacelas.compartir.Plantillas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class PlantillasTest {
    private val d = DatosCompartir(DatosCompartir.Tipo.Carrera, "Rodaje", (0..60).map { val t = it / 60.0; -3.99 + 0.012 * t + 0.002 * Math.sin(t * 9) to -79.2 + 0.006 * Math.sin(t * 4) }, 5.02, 1570, 312)

    @Test fun normalizaHandleYDatos() {
        assertEquals("@lola", Plantillas.normalizarHandle(" @@lo la "))
        assertEquals("@runcity", Plantillas.normalizarHandle("  "))
        assertEquals(listOf("Distancia" to "5.02 km", "Ritmo" to "5'12\" /km", "Tiempo" to "26:10"), Plantillas.datos(d))
    }

    @Test fun insigniaOpcionalYHandleDeRespaldo() {
        assertEquals(null, Plantillas.insignia(d.copy(rachaDias = 0)))
        assertEquals("Racha de 3 días", Plantillas.insignia(d.copy(rachaDias = 3)))
        assertEquals("Récord personal · 5K", Plantillas.insignia(d.copy(recordLabel = "5K")))
        assertEquals("@runcity", Plantillas.normalizarHandle(""))
        val limpia = Plantillas.generar(d.copy(rachaDias = 0), Plantilla.Estadisticas, "@runcity")
        val conRecord = Plantillas.generar(d.copy(recordLabel = "5K"), Plantilla.Estadisticas, "@runcity")
        assertEquals(0, Color.alpha(limpia.getPixel(540, 1190)))
        assertTrue(Color.alpha(conRecord.getPixel(540, 1190)) > 0)
    }

    @Test fun coverYColocacion() {
        val r = Plantillas.ajusteCubrir(4000, 3000, 1080, 1920)
        assertEquals(1156.25f, r.left, 0.01f); assertEquals(1687.5f, r.width(), 0.01f)
        assertEquals(1.6f, Colocacion(2f, -1f, 9f).limitada().escala)
        assertEquals(1080f, Plantillas.rect(Plantillas.inicial(Plantilla.Estadisticas), 1080, 1920, FormatoFoto.Historia).width(), 0.01f)
    }

    @Test fun plantillasTransparentesNueveDieciseisYComposiciones() {
        val dir = File("build/plantillas").apply { mkdirs() }
        Plantilla.entries.forEach { p ->
            val b = Plantillas.generar(d.copy(fechaMs = 1_700_000_000_000L), p, HANDLE_GACELAS)
            assertEquals(1080, b.width)
            assertEquals(1920, b.height)
            assertEquals(0, Color.alpha(b.getPixel(0, 0)))
            assertEquals(0, Color.alpha(b.getPixel(b.width - 1, b.height - 1)))
            val px = IntArray(b.width * b.height).also { b.getPixels(it, 0, b.width, 0, 0, b.width, b.height) }
            assertTrue("$p sin contenido", px.count { Color.alpha(it) > 200 } > 200)
            if (p == Plantilla.Traza || p == Plantilla.Lateral) {
                assertTrue("$p sin trazo", px.count {
                    Color.alpha(it) > 200 && Color.red(it) in 170..215 &&
                        Color.green(it) in 110..155 && Color.blue(it) > 230
                } > 200)
            }
            assertTrue("$p mayormente transparente", px.count { Color.alpha(it) == 0 } > px.size * 0.7)
            File(dir, "${p.name}.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val foto = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(40, 90, 160)) }
            val c = Plantillas.componer(foto, b, p, Plantillas.inicial(p), FormatoFoto.Historia)
            assertEquals(1080, c.width); assertEquals(1920, c.height)
            assertEquals(255, Color.alpha(c.getPixel(5, 5)))
            File(dir, "foto-${p.name}.png").outputStream().use { c.compress(Bitmap.CompressFormat.PNG, 100, it) }
            listOf(FormatoFoto.Post45, FormatoFoto.Cuadrado).forEach { f ->
                val capa = Plantillas.generar(d, p, HANDLE_GACELAS, f)
                val salida = Plantillas.componer(foto, capa, p, Plantillas.inicial(p), f)
                assertEquals(f.ancho, salida.width)
                assertEquals(f.alto, salida.height)
            }
        }
    }

    @Test fun plantillasNuevasYAjusteDeFoto() {
        assertTrue(Plantilla.entries.map { it.name }.containsAll(listOf("Distancia", "Lateral", "Tarjeta")))
        val foto = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(40, 90, 160)) }
        val capa = Plantillas.generar(d.copy(fechaMs = 1_700_000_000_000L), Plantilla.Distancia, "@runcity")
        val col = Plantillas.inicial(Plantilla.Distancia)
        fun armar(a: AjusteFoto) = Plantillas.componer(foto, capa, Plantilla.Distancia, col, FormatoFoto.Historia, a)
        val base = armar(AjusteFoto())
        val brillante = armar(AjusteFoto(brillo = 1f))
        val clara = armar(AjusteFoto(opacidad = 0f))
        fun luz(c: Int) = Color.red(c) + Color.green(c) + Color.blue(c)
        assertTrue(luz(brillante.getPixel(5, 5)) > luz(base.getPixel(5, 5)))
        assertEquals(0, Color.alpha(clara.getPixel(5, 5)))
        var blanco: Pair<Int, Int>? = null
        for (y in 700 until 1000 step 2) {
            for (x in 200 until 880 step 2) {
                val p = base.getPixel(x, y)
                if (Color.alpha(p) > 220 && Color.red(p) > 245 && Color.green(p) > 245 && Color.blue(p) > 245) {
                    blanco = x to y
                    break
                }
            }
            if (blanco != null) break
        }
        val (x, y) = checkNotNull(blanco) { "la plantilla Distancia no dejo un pixel blanco" }
        val pb = brillante.getPixel(x, y)
        val pc = clara.getPixel(x, y)
        assertTrue(Color.red(pb) > 230 && Color.green(pb) > 230 && Color.blue(pb) > 230)
        assertTrue(Color.alpha(pc) > 200 && Color.red(pc) > 230)
    }
}
