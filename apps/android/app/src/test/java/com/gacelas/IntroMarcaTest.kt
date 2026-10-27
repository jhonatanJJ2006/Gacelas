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
import com.gacelas.intro.IntroVideo
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

    @Test fun controlTerminaUnaSolaVezYSueltaElSplash() {
        var listo = 0; var fin = 0
        val c = ControlIntro({ listo++ }, { fin++ })
        c.cuadroListo(); c.cuadroListo()
        assertEquals(1, listo)
        c.esperaAgotada()            // ya habia cuadro: no cierra
        assertEquals(0, fin)
        c.terminar(); c.terminar()
        assertEquals(1, fin)
        assertEquals(1, listo)
    }

    @Test fun fallbackSinPrimerCuadroCierraYSueltaElSplash() {
        var listo = 0; var fin = 0
        val c = ControlIntro({ listo++ }, { fin++ })
        c.esperaAgotada()
        assertEquals(1, listo)
        assertEquals(1, fin)
        assertTrue(c.terminada)
    }

    @Test fun seMuestraSobreElFondoDeMarcaYSeCierraSolaSiElVideoNoArranca() {
        var fin = 0; var listo = 0
        compose.mainClock.autoAdvance = false
        compose.setContent { IntroVideo(alPrimerCuadro = { listo++ }, alTerminar = { fin++ }) }
        compose.mainClock.advanceTimeBy(50)
        compose.onNodeWithTag("intro-animada").assertExists()
        compose.onRoot().captureRoboImage("${dir()}/intro-primer-cuadro.png")
        // En Robolectric el decodificador no pinta cuadros: a los 1 s el fallback cierra la intro.
        compose.mainClock.advanceTimeBy(IntroArranque.ESPERA_PRIMER_CUADRO_MS + 200)
        assertEquals(1, fin)
        assertEquals(1, listo)
        compose.mainClock.advanceTimeBy(5_000)
        assertEquals(1, fin)
    }

    @Test fun unToqueLaSalta() {
        var fin = 0
        compose.mainClock.autoAdvance = false
        compose.setContent { IntroVideo(alPrimerCuadro = {}, alTerminar = { fin++ }) }
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithTag("intro-animada").performClick()
        compose.mainClock.advanceTimeBy(4000)
        assertEquals(1, fin)
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
