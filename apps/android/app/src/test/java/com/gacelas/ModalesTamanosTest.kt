package com.gacelas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.gacelas.ui.components.BotonModalC2
import com.gacelas.ui.components.DialogoC2
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Regresion: los modales se cortaban a la mitad (alto fijo de 640dp y sin scroll).
 * En pantalla pequena, normal y con fuente grande el boton principal debe verse completo.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ModalesTamanosTest {
    @get:Rule val compose = createComposeRule()
    private val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"

    @Composable
    private fun ModalLargo(campos: Int) {
        GacelasTheme {
            Box(Modifier.fillMaxSize().background(GColors.C2Bg))
            DialogoC2(
                onDismissRequest = {},
                icon = { Icon(Icons.Filled.Gavel, null) },
                title = { Text("Actualizamos los Términos y la Política de privacidad") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Lee los cambios antes de seguir. Tus carreras siguen guardadas en tu cuenta y puedes exportarlas cuando quieras.")
                        repeat(campos) { i -> OutlinedTextField("", {}, label = { Text("Campo ${i + 1}") }) }
                    }
                },
                confirmButton = { BotonModalC2("Aceptar y continuar", {}) },
                dismissButton = { BotonModalC2("Ahora no", {}, secundario = true) },
            )
        }
    }

    @OptIn(ExperimentalRoborazziApi::class)
    private fun verificar(nombre: String) {
        var altoVentana = 0
        compose.setContent {
            altoVentana = LocalConfiguration.current.screenHeightDp
            ModalLargo(campos = 7)
        }
        compose.waitForIdle()
        captureScreenRoboImage("$dir/modales-tamanos/$nombre.png")
        val boton = compose.onNodeWithText("Aceptar y continuar")
        boton.assertIsDisplayed()
        val b = boton.getBoundsInRoot()
        assert(b.bottom.value <= altoVentana) { "Boton principal fuera de pantalla: ${b.bottom} > $altoVentana" }
        compose.onNodeWithText("Ahora no").assertIsDisplayed()
    }

    @Test @Config(sdk = [34], qualifiers = "w360dp-h640dp-mdpi")
    fun pequena() = verificar("pequena-360x640")

    @Test @Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
    fun normal() = verificar("normal-393x852")

    @Test @Config(sdk = [34], qualifiers = "w360dp-h640dp-mdpi", fontScale = 1.3f)
    fun fuenteGrande() = verificar("fuente-grande-360x640")
}
