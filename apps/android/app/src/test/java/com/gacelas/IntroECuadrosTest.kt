package com.gacelas

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.gacelas.ui.intro.IntroMarcaE
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Cuadros de la intro "Opcion E" (1080x1920, igual que la referencia opcion-E-secuencia.png). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h640dp-xxhdpi")
class IntroECuadrosTest {
    @get:Rule val compose = createComposeRule()

    private fun dir() = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"

    @Test fun cuadros() {
        val t = androidx.compose.runtime.mutableFloatStateOf(0f)
        compose.setContent { IntroMarcaE(tiempo = { t.floatValue }) }
        for (s in listOf(0.2f, 0.7f, 1.1f, 1.5f, 2.0f, 2.5f, 3.0f)) {
            t.floatValue = s
            compose.waitForIdle()
            compose.onRoot().captureRoboImage("${dir()}/intro-e/t${(s * 10).toInt().toString().padStart(2, '0')}.png")
        }
    }
}
