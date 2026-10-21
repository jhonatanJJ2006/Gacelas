package com.gacelas

import android.graphics.Bitmap
import android.graphics.Color
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
        assertEquals("", Plantillas.normalizarHandle("  "))
        assertEquals(listOf("Distancia" to "5.02 km", "Ritmo" to "5'12\" /km", "Tiempo" to "26:10"), Plantillas.datos(d))
    }

    @Test fun coverYColocacion() {
        val r = Plantillas.ajusteCubrir(4000, 3000, 1080, 1920)
        assertEquals(1156.25f, r.left, 0.01f); assertEquals(1687.5f, r.width(), 0.01f)
        assertEquals(1.6f, Colocacion(2f, -1f, 9f).limitada().escala)
    }

    @Test fun plantillasTransparentesConTrazoMorado() {
        val dir = File("build/plantillas").apply { mkdirs() }
        Plantilla.entries.forEach { p ->
            val b = Plantillas.generar(d, p, HANDLE_GACELAS)
            assertEquals(p.ancho, b.width)
            assertEquals(0, Color.alpha(b.getPixel(0, 0)))
            assertEquals(0, Color.alpha(b.getPixel(b.width - 1, b.height - 1)))
            val px = IntArray(b.width * b.height).also { b.getPixels(it, 0, b.width, 0, 0, b.width, b.height) }
            assertTrue("$p sin trazo", px.count { Color.alpha(it) > 200 && Color.red(it) in 170..215 && Color.green(it) in 110..155 && Color.blue(it) > 230 } > 200)
            assertTrue("$p mayormente transparente", px.count { Color.alpha(it) == 0 } > px.size * 0.7)
            File(dir, "${p.name}.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val foto = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(40, 90, 160)) }
            val c = Plantillas.componer(foto, b, p, Plantillas.inicial(p), FormatoFoto.Historia)
            assertEquals(1080, c.width); assertEquals(1920, c.height)
            assertEquals(255, Color.alpha(c.getPixel(5, 5)))
            File(dir, "foto-${p.name}.png").outputStream().use { c.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
