package com.gacelas

import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.gacelas.compartir.Compartir
import com.gacelas.compartir.DatosCompartir
import com.gacelas.compartir.FormatoCompartir
import com.gacelas.compartir.ImagenCompartir
import com.gacelas.compartir.Intenciones
import com.gacelas.compartir.enlaceRuta
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
class CompartirTest {
    private val linea = (0..20).map { -4.0 + it * 0.0009 to -79.2 }
    private val datos = DatosCompartir(DatosCompartir.Tipo.Carrera, "Vuelta al Jipiro", linea, 5.234, 1712, 327, 48, 6, enlaceRuta("abc"))

    @Test fun ocultaInicioYFin() {
        val r = Compartir.recortarExtremos(linea, 250.0)
        assertEquals(linea[3].first, r.first().first, 1e-9)
        assertEquals(linea[17].first, r.last().first, 1e-9)
        assertTrue(Compartir.recortarExtremos(linea.take(4), 250.0).isEmpty())
    }

    @Test fun proyectaDentroDeLaCaja() {
        val p = Compartir.proyectar(linea, 60f, 100f, 960f, 900f)
        assertTrue(p.all { it.first in 60f..1020f && it.second in 100f..1000f })
        assertTrue(p.first().second > p.last().second)
    }

    @Test fun textosYEnlaces() {
        assertEquals(listOf("Distancia" to "5.23 km", "Tiempo" to "28:32", "Ritmo /km" to "5'27\"", "Desnivel" to "48 m"), Compartir.metricas(datos))
        assertTrue(Compartir.texto(datos).contains("racha de 6 días"))
        assertTrue(java.net.URLDecoder.decode(Compartir.enlaceWhatsApp(datos), "UTF-8").contains("https://gacelas.vercel.app/r/abc"))
    }

    @Test fun intencionHistoriaInstagram() {
        val u = android.net.Uri.parse("content://com.gacelas.archivos/compartir/a.png")
        val i = Intenciones.historiaInstagram(u, u, "", "com.gacelas")
        assertEquals("com.instagram.share.ADD_TO_STORY", i.action)
        assertEquals("com.instagram.android", i.`package`)
        assertEquals("com.gacelas", i.getStringExtra("source_application"))
        assertEquals("image/png", i.type)
    }

    @Test fun generaImagenesDeMarca() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dir = System.getProperty("gacelas.shots")?.let { File(it).apply { mkdirs() } }
        FormatoCompartir.entries.forEach { f ->
            val b = ImagenCompartir.generar(ctx, datos, f)
            assertEquals(f.ancho, b.width)
            assertEquals(f.alto, b.height)
            if (f != FormatoCompartir.Sticker) assertEquals(Color.alpha(b.getPixel(5, 5)), 255)
            dir?.let { d -> File(d, "android-${f.name.lowercase()}.png").outputStream().use { b.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) } }
        }
    }
}
