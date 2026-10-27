package com.gacelas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.gacelas.ui.theme.GacelasTheme
import com.gacelas.ui.three.ClipCorredor
import com.gacelas.ui.three.Runner3DWeb
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.sceneview.math.Position
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * v0.9.4: el corredor de Inicio (WebView) no puede dejar el escenario vacio. Mientras runner3d.js no
 * confirma un cuadro con pixeles (aqui nunca: Robolectric no ejecuta WebGL, igual que un WebView que
 * carga pero pinta transparente) el respaldo tiene que verse. Con v0.9.1-v0.9.3 este test falla.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class Runner3DWebRespaldoTest {
    @get:Rule val compose = createComposeRule()
    private val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"

    @Test
    fun sinPrimerCuadroSeVeElRespaldo() {
        compose.setContent {
            GacelasTheme {
                Box(Modifier.width(360.dp).height(300.dp).background(Color(0xFF2A0E3D))) {
                    Runner3DWeb(ClipCorredor.Idle, autoRotar = true, camara = Position(0f, 0.3f, 4.4f))
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("corredor-3d-respaldo", useUnmergedTree = true).assertIsDisplayed()
        compose.onRoot().captureRoboImage("$dir/android-corredor-respaldo-sin-cuadro.png")
    }
}
