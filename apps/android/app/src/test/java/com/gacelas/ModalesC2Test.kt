package com.gacelas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.gacelas.ui.components.BotonModalC2
import com.gacelas.ui.components.DialogoC2
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import androidx.compose.ui.test.performSemanticsAction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Modal C2 (hoja inferior) usado en cerrar sesion, descartar y eliminar. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class ModalesC2Test {
    @get:Rule val compose = createComposeRule()
    private val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun cerrarSesion() {
        var confirmado = false
        compose.setContent {
            GacelasTheme {
                Box(Modifier.fillMaxSize().background(GColors.C2Bg))
                DialogoC2(
                    onDismissRequest = {},
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
                    title = { Text("¿Cerrar sesión?") },
                    text = { Text("Tus carreras guardadas siguen en tu cuenta.") },
                    confirmButton = { BotonModalC2("Cerrar sesión", { confirmado = true }, peligro = true) },
                    dismissButton = { BotonModalC2("Cancelar", {}, secundario = true) },
                )
            }
        }
        compose.onNodeWithText("¿Cerrar sesión?").assertExists()
        captureScreenRoboImage("$dir/modales/cerrar-sesion.png")
        compose.onNodeWithText("Cerrar sesión").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        assert(confirmado)
    }
}

