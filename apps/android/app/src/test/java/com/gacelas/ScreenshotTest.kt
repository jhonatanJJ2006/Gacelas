package com.gacelas

import androidx.compose.ui.test.junit4.createComposeRule
import com.gacelas.navigation.Destinos
import com.gacelas.navigation.GacelasNavHost
import com.gacelas.ui.theme.GacelasTheme
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Captura una imagen PNG de cada pantalla con Roborazzi.
 * Solo graba si se ejecuta con: ./gradlew recordRoborazziDebug
 * Las imagenes quedan en app/build/outputs/roborazzi/ (o en -Pgacelas.shots=<dir>).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-xhdpi")
class ScreenshotTest(private val ruta: String) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun captura() {
        val destino = if (ruta == Destinos.DETALLE_EVENTO) Destinos.detalleEvento("1") else ruta
        compose.setContent {
            GacelasTheme { GacelasNavHost(inicio = Destinos.BIENVENIDA, rutaInicial = destino) }
        }
        compose.mainClock.advanceTimeBy(500)
        val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"
        val nombre = "%02d-%s".format(Destinos.todas.indexOf(ruta) + 1, ruta.replace("/", "_").replace("{id}", "id"))
        compose.onRoot().captureRoboImage("$dir/$nombre.png")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun rutas(): List<Array<Any>> = Destinos.todas.map { arrayOf(it) }
    }
}
