package com.gacelas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.gacelas.intro.ControlIntro
import com.gacelas.intro.FondoIntro
import com.gacelas.intro.IntroAnimada
import com.gacelas.intro.FUNDIDO_SALIDA_MS
import com.gacelas.ui.intro.IntroArranque
import androidx.compose.ui.graphics.toArgb
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Intro de marca en video ("Ruta y destello v2b") e icono adaptativo (docs/MARCA.md). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class IntroMarcaTest {
    @get:Rule val compose = createComposeRule()

    private fun dir() = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"

    @Test fun controlTerminaUnaSolaVez() {
        var fin = 0
        val c = ControlIntro { fin++ }
        c.terminar(); c.terminar()
        assertEquals(1, fin)
        assertTrue(c.terminada)
    }

    /** El vector solo anima propiedades que Android corre en el RenderThread (si una no lo es, todo cae al hilo principal). */
    @Test fun soloPropiedadesDelRenderThreadYDuracionCompleta() {
        val xml = java.io.File("src/main/res/drawable/intro_marca.xml").readText()
        val props = Regex("propertyName=\"(\\w+)\"").findAll(xml).map { it.groupValues[1] }.toSet()
        val permitidas = setOf("trimPathStart", "trimPathEnd", "fillAlpha", "strokeAlpha", "scaleX", "scaleY", "translateX", "translateY")
        assertTrue("propiedades: $props", permitidas.containsAll(props))
        val fin = Regex("startOffset=\"(\\d+)\" android:duration=\"(\\d+)\"").findAll(xml).maxOf { it.groupValues[1].toInt() + it.groupValues[2].toInt() }
        assertEquals((IntroArranque.DURACION_S * 1000).toInt(), fin)
        assertTrue(xml.contains("RUNCITY") || xml.contains("Runcity"))
        assertTrue(!xml.contains("GACELAS"))
    }

    private fun actividad() = org.robolectric.Robolectric.buildActivity(androidx.activity.ComponentActivity::class.java).setup().get()

    @Test fun seMuestraEnteraYSeRetiraConFundido() {
        val act = actividad(); var fin = 0
        val vista = IntroAnimada.mostrar(act) { fin++ }
        assertEquals(IntroAnimada.ETIQUETA, vista.tag)
        assertTrue(vista.isAttachedToWindow)
        vista.captureRoboImage("${dir()}/intro-primer-cuadro.png")
        // A mitad de la intro sigue ahi aunque la app ya este lista.
        org.robolectric.shadows.ShadowLooper.idleMainLooper(1_000, java.util.concurrent.TimeUnit.MILLISECONDS)
        assertEquals(0, fin)
        org.robolectric.shadows.ShadowLooper.idleMainLooper(IntroArranque.TOPE_MS + 500, java.util.concurrent.TimeUnit.MILLISECONDS)
        assertEquals(1, fin)
        assertTrue(vista.parent == null)
    }

    @Test fun unToqueLaSaltaConFundido() {
        val act = actividad(); var fin = 0
        val vista = IntroAnimada.mostrar(act) { fin++ }
        vista.performClick(); vista.performClick()
        org.robolectric.shadows.ShadowLooper.idleMainLooper(FUNDIDO_SALIDA_MS + 100, java.util.concurrent.TimeUnit.MILLISECONDS)
        assertEquals(1, fin)
        assertTrue(vista.parent == null)
    }

    @Test fun fondoIgualAlSplash() {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        assertEquals(ctx.getColor(R.color.fondo_marca), FondoIntro.toArgb())
    }

    @Test fun iconoAdaptativo() {
        @androidx.compose.runtime.Composable
        fun capa(forma: Shape) = Box(Modifier.size(86.dp).clip(forma)) {
            Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.size(86.dp))
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(86.dp))
        }
        compose.setContent {
            Row(Modifier.background(Color(0xFF070E1D)).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                capa(CircleShape)
                capa(RoundedCornerShape(30))
                Box(Modifier.size(86.dp).clip(CircleShape).background(Color(0xFFE9D5FF))) {
                    Image(painterResource(R.drawable.ic_launcher_monochrome), null, Modifier.size(86.dp), colorFilter = ColorFilter.tint(Color(0xFF5E1781)))
                }
                Box(Modifier.size(86.dp).background(Color(0xFF0C1322))) {
                    Image(painterResource(R.drawable.splash_icono), null, Modifier.size(86.dp))
                }
            }
        }
        compose.onRoot().captureRoboImage("${dir()}/icono-adaptativo.png")
    }
}
