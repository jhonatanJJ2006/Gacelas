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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import com.gacelas.ui.intro.IntroAnimada
import com.gacelas.ui.intro.IntroCuadro
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Intro de marca "Ruta y destello" e icono adaptativo (docs/MARCA.md). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
class IntroMarcaTest {
    @get:Rule val compose = createComposeRule()

    private fun dir() = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"

    @Test fun cuadrosClaveDeLaIntro() {
        var t by androidx.compose.runtime.mutableFloatStateOf(0f)
        compose.setContent { IntroCuadro(t) }
        listOf(0.6f, 1.2f, 1.45f, 1.9f, 2.4f, 2.75f).forEach { s ->
            t = s
            compose.waitForIdle()
            compose.onRoot().captureRoboImage("${dir()}/intro-%.2f.png".format(java.util.Locale.ROOT, s))
        }
    }

    @Test fun terminaSolaUnaVezAlAcabar() {
        var fin = 0
        compose.mainClock.autoAdvance = false
        compose.setContent { IntroAnimada(alTerminar = { fin++ }) }
        compose.mainClock.advanceTimeBy(1500)
        assertEquals(0, fin)
        compose.mainClock.advanceTimeBy(2000)
        assertEquals(1, fin)
        compose.mainClock.advanceTimeBy(1000)
        assertEquals(1, fin)
    }

    @Test fun unToqueLaSalta() {
        var fin = 0
        compose.mainClock.autoAdvance = false
        compose.setContent { IntroAnimada(alTerminar = { fin++ }) }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("intro-animada").performClick()
        compose.mainClock.advanceTimeBy(4000)
        assertEquals(1, fin)
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
