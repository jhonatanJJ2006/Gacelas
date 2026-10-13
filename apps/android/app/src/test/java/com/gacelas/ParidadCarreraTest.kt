package com.gacelas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.gacelas.correr.CarreraMuestra
import com.gacelas.correr.ContenidoCarreraC4
import com.gacelas.correr.K1
import com.gacelas.correr.TrazadoRuta
import com.gacelas.correr.VistaCarrera
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Carrera V1 al tamaño del iPhone 16, con CarreraMuestra (lo mismo que muestra iOS con --carrera-muestra). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class ParidadCarreraTest {
    @get:Rule val compose = createComposeRule()

    private fun captura(panel: Boolean, nombre: String) {
        val e = CarreraMuestra.estado
        compose.setContent {
            GacelasTheme {
                ContenidoCarreraC4(VistaCarrera(e), onPausa = {}, onStop = {}, onVuelta = {}, onAtras = {}, panelInicial = panel) { _, controles, _ ->
                    Box(Modifier.fillMaxSize().background(K1.Fondo)) {
                        TrazadoRuta(e.puntos, Modifier.fillMaxSize())
                        controles({}, {}, 0f, true)
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(1000)
        val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"
        compose.onRoot().captureRoboImage("$dir/paridad/$nombre.png")
    }

    @Test fun carrera() = captura(false, "carrera")
    @Test fun carreraPanel() = captura(true, "carrera-panel")
}
