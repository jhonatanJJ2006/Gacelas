package com.gacelas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.gacelas.ui.theme.GColors
import com.gacelas.ui.theme.GacelasTheme
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Privacidad y Notificaciones C2 (figma-proto), reporte con detalle y modal sin conexion. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class AjustesC2Test {
    @get:Rule val compose = createComposeRule()
    private val dir = System.getProperty("gacelas.shots") ?: "build/outputs/roborazzi"

    @Test fun privacidad() {
        compose.setContent { GacelasTheme { com.gacelas.ajustes.PrivacidadUbicacionScreen(onAtras = {}) } }
        compose.onNodeWithText("ZONA OCULTA").assertExists()
        compose.onNodeWithText("SEGUIDORES").assertExists()
        captureScreenRoboImage("$dir/ajustes/privacidad-c2.png")
    }

    @Test fun notificaciones() {
        compose.setContent { GacelasTheme { com.gacelas.ajustes.ConfiguracionNotificacionesScreen(onAtras = {}) } }
        compose.onNodeWithText("Amigos en vivo").assertExists()
        captureScreenRoboImage("$dir/ajustes/notificaciones-c2.png")
    }

    @Test fun reporteConDetalle() {
        compose.setContent {
            GacelasTheme {
                Box(Modifier.fillMaxSize().background(GColors.C2Bg))
                com.gacelas.social.ModalReporte("p-1", com.gacelas.ui.contenedor().social) {}
            }
        }
        compose.onNodeWithTag("reporte-detalle").performTextInput("Usa fotos de otra persona")
        compose.onNodeWithText("Usa fotos de otra persona").assertExists()
        captureScreenRoboImage("$dir/modales/reporte-detalle.png")
    }

    @Test fun sinConexion() {
        compose.setContent {
            GacelasTheme {
                Box(Modifier.fillMaxSize().background(GColors.C2Bg))
                com.gacelas.social.ModalSinConexionContenido {}
            }
        }
        compose.onNodeWithText("Sin conexión").assertExists()
        captureScreenRoboImage("$dir/modales/sin-conexion.png")
    }
}
