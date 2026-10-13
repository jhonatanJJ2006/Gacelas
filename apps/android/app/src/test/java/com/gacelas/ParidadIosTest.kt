package com.gacelas

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.gacelas.navigation.Destinos
import com.gacelas.navigation.GacelasNavHost
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Capturas Android de las pantallas compartidas con iOS, al tamaño del iPhone 16 (393x852 pt @3x),
 * para compararlas lado a lado con las del simulador (workflow iOS). Graba con recordRoborazziDebug.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class ParidadIosTest(private val ruta: String) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun captura() {
        compose.setContent { GacelasTheme { GacelasNavHost(inicio = Destinos.BIENVENIDA, rutaInicial = ruta) } }
        compose.mainClock.advanceTimeBy(500)
        val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"
        compose.onRoot().captureRoboImage("$dir/paridad/${ruta.replace("/", "_")}.png")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun rutas(): List<Array<Any>> = listOf(Destinos.BIENVENIDA, Destinos.INICIO_SESION, Destinos.INICIO, Destinos.AJUSTES, Destinos.RUTAS).map { arrayOf(it) }
    }
}
